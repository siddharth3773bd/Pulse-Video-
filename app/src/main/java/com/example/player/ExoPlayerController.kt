package com.example.player

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import kotlinx.coroutines.withContext
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ScrubbingModeParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.example.model.DecoderMode
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** How far (as a share of the video's length) a drag-seek may land from the finger: 1%. */
private const val SCRUB_SEEK_TOLERANCE = 0.01

/**
 * The single class in the codebase that references Media3 ExoPlayer.
 */
@OptIn(UnstableApi::class)
class ExoPlayerController(
    context: Context
) : PlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _autoAdvanced = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val autoAdvanced: kotlinx.coroutines.flow.SharedFlow<String> = _autoAdvanced

    private val _tracks = MutableStateFlow(PlayerTracks())
    override val tracks: StateFlow<PlayerTracks> = _tracks.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var activePlayerInstance by mutableStateOf<ExoPlayer?>(null)
    private var currentDecoderMode: DecoderMode = DecoderMode.AUTO
    private var positionPollJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncStateFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncStateFromPlayer()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _state.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            parseAvailableTracks(tracks)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            // Moves on by itself (autoplay), or because the next button jumped to the queued video.
            val auto = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            if (!auto && !skippingToQueued) return
            skippingToQueued = false
            val player = exoPlayer ?: return
            // The queued video is now playing: it becomes "the" video.
            val config = mediaItem.localConfiguration
            currentUri = config?.uri
            currentTitle = mediaItem.mediaMetadata.title?.toString()
            baseSubtitles = config?.subtitleConfigurations ?: emptyList()
            extraSubtitles.clear()
            pendingNext = null
            if (player.currentMediaItemIndex > 0) {
                player.removeMediaItems(0, player.currentMediaItemIndex)
            }
            _state.update {
                it.copy(
                    currentMediaUri = mediaItem.mediaId,
                    positionMs = 0L,
                    isEnded = false,
                    activeSubtitleText = "",
                    cues = emptyList()
                )
            }
            // (When the next button caused it, the view model does its own bookkeeping.)
            if (auto) _autoAdvanced.tryEmit(mediaItem.mediaId)
        }

        override fun onRenderedFirstFrame() {
            _state.update { it.copy(firstFrameCount = it.firstFrameCount + 1) }
        }

        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues
                .mapNotNull { it.text?.toString()?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
            _state.update { it.copy(activeSubtitleText = text, cues = cueGroup.cues) }
        }
    }

    private fun ensurePlayer(decoderMode: DecoderMode): ExoPlayer {
        val existing = exoPlayer
        if (existing != null && currentDecoderMode == decoderMode) {
            return existing
        }
        releaseInternalPlayer()
        currentDecoderMode = decoderMode

        val renderersFactory = DefaultRenderersFactory(appContext).apply {
            setEnableDecoderFallback(true)
            val selector = when (decoderMode) {
                DecoderMode.AUTO -> MediaCodecSelector.DEFAULT
                DecoderMode.HARDWARE -> MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                    val hw = all.filter { it.hardwareAccelerated }
                    hw.ifEmpty { all }
                }
                DecoderMode.SOFTWARE -> MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                    val sw = all.filter { it.softwareOnly || !it.hardwareAccelerated }
                    sw.ifEmpty { all }
                }
            }
            setMediaCodecSelector(selector)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val created = ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            // While the seek bar (or a sideways swipe) is being dragged, jump to the nearest
            // keyframe instead of decoding the exact frame every time, so the picture follows the
            // finger straight away. Letting go does one exact seek.
            .setScrubbingModeParameters(
                ScrubbingModeParameters.Builder()
                    .setFractionalSeekTolerance(SCRUB_SEEK_TOLERANCE, SCRUB_SEEK_TOLERANCE)
                    .build()
            )
            .build()
            .apply {
                addListener(playerListener)
            }

        exoPlayer = created
        activePlayerInstance = created
        if (videoDisabled) applyVideoDisabled(created)
        applyLanguagePrefs(created)
        if (backgroundAudio) {
            created.setWakeMode(C.WAKE_MODE_LOCAL)
            VideoAudioService.swapPlayer(created)
        }
        startPositionPolling()
        return created
    }

    override fun prepareMedia(
        uri: Uri,
        filePath: String?,
        startPositionMs: Long,
        autoPlay: Boolean,
        decoderMode: DecoderMode,
        title: String?
    ) {
        val player = ensurePlayer(decoderMode)
        skippingToQueued = false
        currentTitle = title
        _state.update {
            it.copy(
                currentMediaUri = uri.toString(),
                playbackSpeed = 1.0f,
                activeSubtitleText = "",
                isEnded = false
            )
        }

        extraSubtitles.clear()
        prepareJob?.cancel()
        // Looking for subtitle files next to the video touches the disk, so it runs off the main
        // thread; the video then starts with them attached.
        prepareJob = scope.launch {
            val subtitleConfigs = withContext(Dispatchers.IO) { discoverExternalSubtitles(filePath) }
            baseSubtitles = subtitleConfigs
            currentUri = uri
            val mediaItem = MediaItem.Builder()
                .setMediaId(uri.toString())
                .setUri(uri)
                .setSubtitleConfigurations(subtitleConfigs)
                .setMediaMetadata(metadata())
                .build()
            pendingNext = null
            player.setPlaybackParameters(PlaybackParameters(1.0f))
            player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = autoPlay
            syncStateFromPlayer()
        }
    }

    private var prepareJob: Job? = null
    private var currentTitle: String? = null
    private var pendingNext: MediaItem? = null
    private var queueJob: Job? = null
    private var skippingToQueued = false

    override fun skipToQueuedNext(uri: Uri): Boolean {
        val player = exoPlayer ?: return false
        val queued = pendingNext ?: return false
        if (queued.mediaId != uri.toString() || !player.hasNextMediaItem()) return false
        skippingToQueued = true
        extraSubtitles.clear()
        player.setPlaybackParameters(PlaybackParameters(1.0f))
        player.seekToNextMediaItem()
        player.playWhenReady = true
        syncStateFromPlayer()
        return true
    }

    override fun queueNext(uri: Uri?, filePath: String?, title: String?) {
        queueJob?.cancel()
        queueJob = scope.launch {
            // Wait until the current video has been handed to the player.
            prepareJob?.join()
            val player = exoPlayer ?: return@launch
            val item = if (uri == null) null else {
                val subs = withContext(Dispatchers.IO) { discoverExternalSubtitles(filePath) }
                MediaItem.Builder()
                    .setMediaId(uri.toString())
                    .setUri(uri)
                    .setSubtitleConfigurations(subs)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setDisplayTitle(title)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            }
            pendingNext = item
            val after = player.currentMediaItemIndex + 1
            if (player.mediaItemCount > after) player.removeMediaItems(after, player.mediaItemCount)
            if (item != null) player.addMediaItem(item)
        }
    }
    private var videoDisabled = false
    private var backgroundAudio = false

    /** Title shown in the media notification while the sound plays in the background. */
    private fun metadata(): MediaMetadata = MediaMetadata.Builder()
        .setTitle(currentTitle)
        .setDisplayTitle(currentTitle)
        .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
        .build()

    private fun applyVideoDisabled(player: ExoPlayer) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, videoDisabled)
            .build()
    }

    override fun setVideoEnabled(enabled: Boolean) {
        videoDisabled = !enabled
        exoPlayer?.let { applyVideoDisabled(it) }
    }

    override fun setBackgroundAudio(enabled: Boolean) {
        if (backgroundAudio == enabled) return
        backgroundAudio = enabled
        val player = exoPlayer
        if (enabled && player != null) {
            // Keeps the CPU awake while the screen is off so the sound doesn't stop.
            player.setWakeMode(C.WAKE_MODE_LOCAL)
            VideoAudioService.start(appContext, player)
        } else {
            player?.setWakeMode(C.WAKE_MODE_NONE)
            VideoAudioService.stop(appContext)
        }
    }

    private var preferredAudio: String? = null
    private var preferredText: String? = null

    override fun setPreferredLanguages(audio: String?, subtitles: String?) {
        preferredAudio = audio?.takeIf { it.isNotBlank() }
        preferredText = subtitles?.takeIf { it.isNotBlank() }
        exoPlayer?.let { applyLanguagePrefs(it) }
    }

    private fun applyLanguagePrefs(player: ExoPlayer) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguages(*listOfNotNull(preferredAudio).toTypedArray())
            .setPreferredTextLanguages(*listOfNotNull(preferredText).toTypedArray())
            .build()
    }

    override fun setScrubbing(enabled: Boolean) {
        val player = exoPlayer ?: return
        if (player.isScrubbingModeEnabled != enabled) player.setScrubbingModeEnabled(enabled)
    }

    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    private var currentUri: Uri? = null
    private var baseSubtitles: List<MediaItem.SubtitleConfiguration> = emptyList()
    private val extraSubtitles = mutableListOf<MediaItem.SubtitleConfiguration>()

    override fun addSubtitleFile(uri: Uri, name: String) {
        val player = exoPlayer ?: return
        val videoUri = currentUri ?: return
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        val mime = when (ext) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "ttml", "xml", "dfxp" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        extraSubtitles += MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime)
            .setLanguage("und")
            .setLabel(name)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        val item = MediaItem.Builder()
            .setUri(videoUri)
            .setMediaId(videoUri.toString())
            .setSubtitleConfigurations(baseSubtitles + extraSubtitles)
            .setMediaMetadata(metadata())
            .build()
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage("und")
            .build()
        player.setMediaItem(item, position)
        pendingNext?.let { player.addMediaItem(it) }
        player.prepare()
        player.playWhenReady = wasPlaying
        _tracks.update { it.copy(subtitlesEnabled = true) }
    }

    private fun discoverExternalSubtitles(
        videoFilePath: String?
    ): List<MediaItem.SubtitleConfiguration> {
        val configs = mutableListOf<MediaItem.SubtitleConfiguration>()
        if (videoFilePath.isNullOrBlank()) return configs

        try {
            val videoFile = File(videoFilePath)
            val baseName = videoFile.nameWithoutExtension
            val parentDir = videoFile.parentFile
            if (parentDir != null && parentDir.exists() && parentDir.canRead()) {
                parentDir.listFiles()?.forEach { file ->
                    val ext = file.extension.lowercase(Locale.US)
                    if ((ext == "srt" || ext == "vtt") &&
                        file.nameWithoutExtension.startsWith(baseName, ignoreCase = true)
                    ) {
                        val mimeType = if (ext == "vtt") MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                        configs.add(
                            MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(file))
                                .setMimeType(mimeType)
                                .setLanguage("und")
                                .setLabel(file.name)
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (configs.isEmpty()) {
            try {
                val baseName = File(videoFilePath).nameWithoutExtension
                val filesUri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME
                )
                val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("$baseName%.srt", "$baseName%.vtt")
                appContext.contentResolver.query(
                    filesUri,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                        if (ext == "srt" || ext == "vtt") {
                            val subUri = ContentUris.withAppendedId(filesUri, id)
                            val mimeType = if (ext == "vtt") MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                            configs.add(
                                MediaItem.SubtitleConfiguration.Builder(subUri)
                                    .setMimeType(mimeType)
                                    .setLanguage("und")
                                    .setLabel(name)
                                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                    .build()
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return configs
    }

    private fun parseAvailableTracks(currentTracks: Tracks) {
        val player = exoPlayer ?: return
        val audioOptions = mutableListOf<PlayerTrackOption>()
        val subtitleOptions = mutableListOf<PlayerTrackOption>()
        val isTextDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)

        currentTracks.groups.forEachIndexed { groupIdx, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (trackIdx in 0 until group.length) {
                        val format = group.getTrackFormat(trackIdx)
                        val langCode = format.language
                        val langDisplay = langCode?.takeIf { it != "und" }?.let {
                            Locale.forLanguageTag(it).displayLanguage.takeIf { d -> d.isNotBlank() && d != it }
                        }
                        val labelText = buildString {
                            append(format.label ?: langDisplay ?: "Track ${audioOptions.size + 1}")
                            if (!langDisplay.isNullOrBlank() && format.label != null && !format.label!!.contains(langDisplay, ignoreCase = true)) {
                                append(" ($langDisplay)")
                            }
                            if (format.channelCount > 0) {
                                append(" · ${format.channelCount}ch")
                            }
                        }
                        audioOptions.add(
                            PlayerTrackOption(
                                id = "audio_${groupIdx}_$trackIdx",
                                groupIndex = groupIdx,
                                trackIndex = trackIdx,
                                label = labelText,
                                language = langCode,
                                isSelected = group.isTrackSelected(trackIdx)
                            )
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (trackIdx in 0 until group.length) {
                        val format = group.getTrackFormat(trackIdx)
                        val langCode = format.language
                        val langDisplay = langCode?.takeIf { it != "und" }?.let {
                            Locale.forLanguageTag(it).displayLanguage.takeIf { d -> d.isNotBlank() && d != it }
                        }
                        val labelText = format.label
                            ?: langDisplay
                            ?: "Subtitle ${subtitleOptions.size + 1}"
                        subtitleOptions.add(
                            PlayerTrackOption(
                                id = "sub_${groupIdx}_$trackIdx",
                                groupIndex = groupIdx,
                                trackIndex = trackIdx,
                                label = labelText,
                                language = langCode,
                                isSelected = !isTextDisabled && group.isTrackSelected(trackIdx)
                            )
                        )
                    }
                }
            }
        }

        // Sound the phone can't decode (Dolby / DTS on many phones): the picture would play in
        // silence, so say so. Only when no sound track at all can be played.
        val audioGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.length > 0 }
        val unsupportedAudio = if (audioGroups.isNotEmpty() && audioGroups.none { it.isSupported(false) }) {
            audioFormatName(audioGroups.first().getTrackFormat(0).sampleMimeType)
        } else null

        _tracks.value = PlayerTracks(
            audioTracks = audioOptions,
            subtitleTracks = subtitleOptions,
            subtitlesEnabled = !isTextDisabled,
            unsupportedAudio = unsupportedAudio
        )
    }

    private fun audioFormatName(mime: String?): String = when {
        mime == null -> "unknown"
        mime == "audio/ac3" -> "Dolby Digital (AC3)"
        mime.startsWith("audio/eac3") -> "Dolby Digital Plus (EAC3)"
        mime == "audio/true-hd" -> "Dolby TrueHD"
        mime.startsWith("audio/vnd.dts") -> "DTS"
        else -> mime.substringAfter('/').uppercase()
    }

    override fun selectAudioTrack(option: PlayerTrackOption) {
        val player = exoPlayer ?: return
        val groups = player.currentTracks.groups
        if (option.groupIndex !in groups.indices) return
        val mediaGroup = groups[option.groupIndex].mediaTrackGroup
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(mediaGroup, option.trackIndex))
            .build()
    }

    override fun selectSubtitleTrack(option: PlayerTrackOption) {
        val player = exoPlayer ?: return
        val groups = player.currentTracks.groups
        if (option.groupIndex !in groups.indices) return
        val mediaGroup = groups[option.groupIndex].mediaTrackGroup
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(mediaGroup, option.trackIndex))
            .build()
        _tracks.update { it.copy(subtitlesEnabled = true) }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        val player = exoPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        if (!enabled) {
            _state.update { it.copy(activeSubtitleText = "", cues = emptyList()) }
        }
        _tracks.update { it.copy(subtitlesEnabled = enabled) }
    }

    override fun play() {
        val player = exoPlayer ?: return
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }
        player.play()
        syncStateFromPlayer()
    }

    override fun pause() {
        exoPlayer?.pause()
        syncStateFromPlayer()
    }

    override fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, dur)
        player.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        exoPlayer?.setPlaybackParameters(PlaybackParameters(clamped))
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                syncStateFromPlayer()
                if (_state.value.isPlaying) {
                    delay(250L)
                } else {
                    // Paused (or loading): nothing moves, so don't wake up at all. Seeking updates
                    // the position straight away, and the player's own events (play, ready, ended)
                    // update the state; this resumes the moment playing starts.
                    _state.first { it.isPlaying }
                }
            }
        }
    }

    private fun syncStateFromPlayer() {
        val player = exoPlayer ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it > 0L } ?: _state.value.durationMs
        val buffering = player.playbackState == Player.STATE_BUFFERING
        val ended = player.playbackState == Player.STATE_ENDED
        _state.update {
            it.copy(
                isPlaying = player.isPlaying,
                positionMs = pos,
                durationMs = dur,
                isBuffering = buffering,
                isEnded = ended
            )
        }
    }

    private fun releaseInternalPlayer() {
        queueJob?.cancel()
        pendingNext = null
        positionPollJob?.cancel()
        prepareJob?.cancel()
        try {
            exoPlayer?.removeListener(playerListener)
            exoPlayer?.release()
        } catch (_: Exception) {
        }
        exoPlayer = null
        activePlayerInstance = null
    }

    override fun release() {
        if (backgroundAudio) {
            backgroundAudio = false
            VideoAudioService.stop(appContext)
        }
        videoDisabled = false
        releaseInternalPlayer()
        _state.value = PlayerState()
        _tracks.value = PlayerTracks()
    }

    fun destroy() {
        release()
        scope.cancel()
    }

    @Composable
    override fun RenderVideoSurface(
        modifier: Modifier,
        contentScale: ContentScale,
        placeholder: @Composable () -> Unit
    ) {
        ContentFrame(
            player = activePlayerInstance,
            modifier = modifier,
            // Android's recommended surface: the video goes straight to the display hardware
            // (less battery, full HDR, protected content). It used to be a TextureView, which is
            // drawn like a normal view (easy to fade and clip) but copies every frame and loses HDR.
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = contentScale,
            keepContentOnReset = true,
            shutter = {
                Box(Modifier.fillMaxSize().background(Color.Black)) { placeholder() }
            }
        )
    }
}
