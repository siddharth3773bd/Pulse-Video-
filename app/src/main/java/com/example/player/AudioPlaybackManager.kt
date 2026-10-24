package com.example.player

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.SystemClock
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.model.AudioTrackItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AudioPlaybackState(
    val currentTrack: AudioTrackItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isEnded: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** Time left on the sleep timer in ms, 0 when no countdown is running. */
    val sleepTimerRemainingMs: Long = 0L,
    /** True when playback should pause at the end of the current song. */
    val sleepAtEndOfSong: Boolean = false
)

class AudioPlaybackManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionPollJob: Job? = null
    private var queueBuildJob: Job? = null
    private var pendingControllerAction: ((MediaController) -> Unit)? = null
    private var isLoadingQueue = false

    private val tracksById = LinkedHashMap<String, AudioTrackItem>()

    // Sleep timer: lives here (not in the UI) so it keeps counting while the screen is off.
    private var sleepTimerEndAtMs: Long = 0L
    private var sleepAtEndOfSongArmed: Boolean = false

    private val artworkCache = object : LruCache<String, ByteArray>(60) {
        override fun sizeOf(key: String, value: ByteArray): Int = 1
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // "End of song" sleep timer: the song finished on its own, so stop here.
            if (sleepAtEndOfSongArmed &&
                (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
            ) {
                fireSleepTimer(seekToStart = true)
            }
            syncFromController()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncFromController()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFromController()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            syncFromController()
        }
    }

    init {
        ensureControllerConnected()
    }

    private fun ensureControllerConnected() {
        if (mediaController != null || controllerFuture != null) return
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, AudioPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    startPositionPolling()
                    val pending = pendingControllerAction
                    pendingControllerAction = null
                    if (pending != null) {
                        pending(controller)
                    } else {
                        syncFromController()
                    }
                } catch (_: Throwable) {
                    controllerFuture = null
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun withController(action: (MediaController) -> Unit) {
        val current = mediaController
        if (current != null) {
            action(current)
        } else {
            pendingControllerAction = action
            ensureControllerConnected()
        }
    }

    fun registerTracksCatalog(tracks: List<AudioTrackItem>) {
        tracks.forEach { tracksById[it.id] = it }
        syncFromController()
    }

    fun playQueue(
        tracks: List<AudioTrackItem>,
        startIndex: Int,
        shuffle: Boolean = false
    ) {
        if (tracks.isEmpty()) return
        val safeStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
        tracks.forEach { tracksById[it.id] = it }

        val targetTrack = tracks[safeStartIndex]
        isLoadingQueue = true
        _state.update {
            it.copy(
                currentTrack = targetTrack,
                isPlaying = true,
                positionMs = 0L,
                durationMs = targetTrack.durationMs,
                isEnded = false
            )
        }

        queueBuildJob?.cancel()
        queueBuildJob = scope.launch {
            // Preload artwork for the starting track and its immediate neighbors first so playback starts fast
            val initialMediaItems = withContext(Dispatchers.IO) {
                tracks.mapIndexed { idx, track ->
                    val preloadNow = abs(idx - safeStartIndex) <= 2 || artworkCache.get(track.id) != null
                    val art = if (preloadNow) loadTrackArtworkBytes(track) else null
                    buildMediaItem(track, art)
                }
            }

            withController { controller ->
                // Keep the user's repeat / shuffle choice; only force shuffle on when asked to.
                if (shuffle) controller.shuffleModeEnabled = true
                controller.setMediaItems(initialMediaItems, safeStartIndex, 0L)
                controller.prepare()
                controller.play()
                isLoadingQueue = false
                syncFromController()
            }

            // Populate artwork for any remaining tracks in the queue in the background
            tracks.forEachIndexed { idx, track ->
                if (!isActive) return@launch
                if (abs(idx - safeStartIndex) > 2 && artworkCache.get(track.id) == null) {
                    val art = withContext(Dispatchers.IO) { loadTrackArtworkBytes(track) }
                    if (art != null && art.isNotEmpty()) {
                        val updatedItem = buildMediaItem(track, art)
                        mediaController?.let { controller ->
                            if (idx < controller.mediaItemCount &&
                                controller.getMediaItemAt(idx).mediaId == track.id &&
                                controller.currentMediaItemIndex != idx
                            ) {
                                controller.replaceMediaItem(idx, updatedItem)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildMediaItem(track: AudioTrackItem, artworkBytes: ByteArray?): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsBrowsable(false)
            .setIsPlayable(true)

        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.contentUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun loadTrackArtworkBytes(track: AudioTrackItem): ByteArray? {
        val cacheKey = track.id
        artworkCache.get(cacheKey)?.let { cached ->
            return if (cached.isNotEmpty()) cached else null
        }

        var bitmap: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap = appContext.contentResolver.loadThumbnail(
                    track.contentUri,
                    Size(512, 512),
                    null
                )
            } catch (_: Throwable) {
            }
        }

        if (bitmap == null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(appContext, track.contentUri)
                val embedded = retriever.embeddedPicture
                if (embedded != null && embedded.isNotEmpty()) {
                    val decoded = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                    if (decoded != null) {
                        bitmap = Bitmap.createScaledBitmap(decoded, 512, 512, true)
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {
                }
            }
        }

        val compressed: ByteArray? = try {
            bitmap?.let { bmp ->
                ByteArrayOutputStream().use { bos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                    bos.toByteArray()
                }
            }
        } catch (_: Throwable) {
            null
        }

        artworkCache.put(cacheKey, compressed ?: ByteArray(0))
        return compressed
    }

    fun togglePlayPause() {
        withController { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_ENDED) {
                    controller.seekTo(0L)
                }
                controller.play()
            }
            syncFromController()
        }
    }

    fun toggleShuffle() {
        withController { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            syncFromController()
        }
    }

    /** Cycles repeat: off -> repeat all -> repeat one -> off. */
    fun cycleRepeat() {
        withController { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            syncFromController()
        }
    }

    fun pause() {
        mediaController?.let { controller ->
            if (controller.isPlaying || controller.playWhenReady) {
                controller.pause()
            }
            syncFromController()
        }
    }

    /** Stops playback, clears the queue and asks the service to remove its notification and stop. */
    fun stopAndClear() {
        queueBuildJob?.cancel()
        pendingControllerAction = null
        isLoadingQueue = false
        mediaController?.let { controller ->
            try {
                controller.pause()
                controller.stop()
                controller.clearMediaItems()
                controller.sendCustomCommand(
                    SessionCommand(AudioPlaybackService.ACTION_STOP_PLAYBACK, Bundle.EMPTY),
                    Bundle.EMPTY
                )
            } catch (_: Throwable) {
            }
        }
        sleepTimerEndAtMs = 0L
        sleepAtEndOfSongArmed = false
        _state.value = AudioPlaybackState()
    }

    fun seekTo(ms: Long) {
        withController { controller ->
            val dur = controller.duration.takeIf { it > 0L }
                ?: _state.value.currentTrack?.durationMs
                ?: Long.MAX_VALUE
            val clampedMs = ms.coerceIn(0L, dur)
            controller.seekTo(clampedMs)
            _state.update { it.copy(positionMs = clampedMs) }
            syncFromController()
        }
    }

    fun skipNext() {
        withController { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
                controller.play()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
                controller.play()
            }
            syncFromController()
        }
    }

    fun skipPrevious() {
        withController { controller ->
            if (controller.currentPosition > 3_000L || !controller.hasPreviousMediaItem()) {
                controller.seekTo(0L)
            } else {
                controller.seekToPreviousMediaItem()
            }
            controller.play()
            syncFromController()
        }
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                checkSleepTimer()
                syncFromController()
                delay(250L)
            }
        }
    }

    /** Starts a countdown; playback pauses when it reaches zero. Replaces any running timer. */
    fun startSleepTimer(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 24 * 60)
        sleepAtEndOfSongArmed = false
        sleepTimerEndAtMs = SystemClock.elapsedRealtime() + safeMinutes * 60_000L
        _state.update {
            it.copy(sleepTimerRemainingMs = safeMinutes * 60_000L, sleepAtEndOfSong = false)
        }
        ensureControllerConnected()
    }

    /** Pauses playback when the current song ends. Replaces any running countdown. */
    fun startSleepAtEndOfSong() {
        sleepTimerEndAtMs = 0L
        sleepAtEndOfSongArmed = true
        _state.update { it.copy(sleepTimerRemainingMs = 0L, sleepAtEndOfSong = true) }
        ensureControllerConnected()
    }

    fun cancelSleepTimer() {
        sleepTimerEndAtMs = 0L
        sleepAtEndOfSongArmed = false
        _state.update { it.copy(sleepTimerRemainingMs = 0L, sleepAtEndOfSong = false) }
    }

    private fun checkSleepTimer() {
        if (sleepTimerEndAtMs > 0L) {
            val remaining = sleepTimerEndAtMs - SystemClock.elapsedRealtime()
            if (remaining <= 0L) {
                fireSleepTimer(seekToStart = false)
            } else {
                _state.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
        }
        if (sleepAtEndOfSongArmed) {
            val controller = mediaController ?: return
            val dur = controller.duration
            if (controller.playbackState == Player.STATE_ENDED ||
                (dur > 0L && controller.currentPosition >= dur - 300L)
            ) {
                fireSleepTimer(seekToStart = false)
            }
        }
    }

    private fun fireSleepTimer(seekToStart: Boolean) {
        sleepTimerEndAtMs = 0L
        sleepAtEndOfSongArmed = false
        mediaController?.let { controller ->
            try {
                controller.pause()
                if (seekToStart) controller.seekTo(0L)
            } catch (_: Throwable) {
            }
        }
        _state.update { it.copy(sleepTimerRemainingMs = 0L, sleepAtEndOfSong = false, isPlaying = false) }
    }

    private fun syncFromController() {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem
        val mediaId = currentItem?.mediaId
        val matchedTrack = if (mediaId != null) {
            tracksById[mediaId] ?: reconstructTrackFromMediaItem(currentItem)
        } else if (!isLoadingQueue && controller.mediaItemCount == 0) {
            null
        } else {
            _state.value.currentTrack
        }

        val pos = controller.currentPosition.coerceAtLeast(0L)
        val dur = controller.duration.takeIf { it > 0L }
            ?: matchedTrack?.durationMs
            ?: _state.value.durationMs
        val ended = controller.playbackState == Player.STATE_ENDED && !controller.hasNextMediaItem()

        _state.update {
            it.copy(
                currentTrack = matchedTrack,
                isPlaying = controller.isPlaying,
                positionMs = pos,
                durationMs = dur,
                isEnded = ended,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }
    }

    private fun reconstructTrackFromMediaItem(item: MediaItem): AudioTrackItem? {
        val uri = item.localConfiguration?.uri ?: return null
        val meta = item.mediaMetadata
        val title = meta.title?.toString()?.takeIf { it.isNotBlank() } ?: "Audio Track"
        val artist = meta.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown artist"
        val album = meta.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown album"
        return AudioTrackItem(
            mediaId = item.mediaId.substringAfterLast('/', "0").toLongOrNull() ?: 0L,
            id = item.mediaId,
            contentUri = uri,
            title = title,
            artist = artist,
            album = album,
            ext = "mp3",
            durationMs = 0L,
            durationSec = 0,
            durationLabel = "0:00",
            sizeBytes = 0L,
            sizeMB = 0.0,
            folder = "Music"
        )
    }

    companion object {
        @Volatile
        private var instance: AudioPlaybackManager? = null

        fun getInstance(context: Context): AudioPlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlaybackManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
