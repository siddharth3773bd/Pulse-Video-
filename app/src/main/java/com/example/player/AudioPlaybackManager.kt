package com.example.player

import com.example.model.cleanArtistName
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
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
import kotlinx.coroutines.flow.first
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
    val sleepAtEndOfSong: Boolean = false,
    /** Every song in the play queue, in queue order. */
    val queue: List<AudioTrackItem> = emptyList(),
    /** Position of the current song inside [queue]. */
    val queueIndex: Int = 0
)

/** A song that couldn't play: shown in a dialog with the full reason, which can be copied. */
data class PlaybackErrorReport(
    val title: String,
    /** True when playback moved on to the next song in the queue. */
    val skipped: Boolean,
    /** Error code plus every underlying reason, one per line. */
    val details: String
)

@androidx.annotation.OptIn(UnstableApi::class)
class AudioPlaybackManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<PlaybackErrorReport?>(null)
    /** The last song that couldn't play, until the UI has shown it ([clearError]). */
    val lastError: StateFlow<PlaybackErrorReport?> = _lastError.asStateFlow()

    /** Songs that failed in a row; stops the skipping when every song in the queue fails. */
    private var failuresInARow = 0

    // Last session (see saveSession / restoreSessionIfIdle).
    private val sessionPrefs = appContext.getSharedPreferences("audio_session", Context.MODE_PRIVATE)
    private var restoreChecked = false
    private var lastSavedAt = 0L
    /** False while the pop-up player plays a file from another app: that isn't the user's queue. */
    private var rememberSession = true

    // Shuffle reorders the queue itself (the current song first, the rest in random order), so the
    // Queue, the cover strip and Next/Previous all follow the same order you can see. The player's
    // own hidden shuffle mode stays off. Turning shuffle off puts the songs back in the order they
    // had before, carrying on from the current song.
    private var shuffleOn = false
    /** Media ids in the order they had before shuffling; null while not shuffled. */
    private var orderBeforeShuffle: List<String>? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionPollJob: Job? = null
    private var queueBuildJob: Job? = null
    private var pendingControllerAction: ((MediaController) -> Unit)? = null
    private var isLoadingQueue = false

    private val tracksById = LinkedHashMap<String, AudioTrackItem>()

    /** Cached queue, rebuilt only when the playlist itself changes (not on every position tick). */
    private var queueSnapshot: List<AudioTrackItem> = emptyList()

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
            saveSession()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            rebuildQueueSnapshot()
            syncFromController()
            saveSession()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncFromController()
            // Pausing saves the exact spot.
            saveSession()
        }

        // Say exactly why a song couldn't play, then move on to the next one like other music apps.
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val title = _state.value.currentTrack?.title ?: "This song"
            android.util.Log.e("Doppler", "Playback error for $title: ${error.errorCodeName}", error)
            val controller = mediaController
            failuresInARow++
            val canSkip = controller != null && controller.hasNextMediaItem() &&
                failuresInARow < controller.mediaItemCount
            _lastError.value = PlaybackErrorReport(
                title = title,
                skipped = canSkip,
                details = describeError(error)
            )
            if (canSkip && controller != null) {
                controller.seekToNextMediaItem()
                controller.prepare()
                controller.play()
            }
            syncFromController()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) failuresInARow = 0
            syncFromController()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            syncFromController()
        }

        // Shuffle asked for from outside the app (headphones, the system's media controls):
        // do it our way, by reordering the queue, and keep the player's own shuffle mode off.
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (!shuffleModeEnabled) return
            mediaController?.shuffleModeEnabled = false
            if (!shuffleOn) setShuffle(true)
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
                    if (controller.shuffleModeEnabled) controller.shuffleModeEnabled = false
                    rebuildQueueSnapshot()
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
        restoreSessionIfIdle()
    }

    // ---- Last session ------------------------------------------------------------------------
    // The queue, song and spot are saved as they change, so after the app was closed (or Android
    // ended it) Music shows the last song again, paused where it was. Without this it only came
    // back while the app happened to still be in memory.

    private fun saveSession() {
        if (!rememberSession) return
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count == 0 || isLoadingQueue) return // Stopped: keep the last session for next time.
        val ids = ArrayList<String>(count)
        for (i in 0 until count) {
            val id = controller.getMediaItemAt(i).mediaId
            val track = tracksById[id] ?: return
            // A file opened from another app isn't part of the library: keep the library session.
            if (track.mediaId == 0L) return
            ids += id
        }
        sessionPrefs.edit()
            .putString(SESSION_IDS, ids.joinToString("\n"))
            .putInt(SESSION_INDEX, controller.currentMediaItemIndex.coerceAtLeast(0))
            .putLong(SESSION_POSITION, controller.currentPosition.coerceAtLeast(0L))
            .putBoolean(SESSION_SHUFFLED, shuffleOn)
            .putString(SESSION_IDS_BEFORE_SHUFFLE, orderBeforeShuffle?.joinToString("\n"))
            .putInt(SESSION_REPEAT, controller.repeatMode)
            .apply()
        lastSavedAt = SystemClock.elapsedRealtime()
    }

    /** Once per app start, when nothing is loaded: put the last session back, paused. */
    private fun restoreSessionIfIdle() {
        if (restoreChecked) return
        restoreChecked = true
        val ids = sessionPrefs.getString(SESSION_IDS, null)
            ?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        if (ids.isEmpty()) return
        val savedId = ids.getOrNull(sessionPrefs.getInt(SESSION_INDEX, 0))
        // Songs deleted since then are skipped.
        val tracks = ids.mapNotNull { tracksById[it] }
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.id == savedId }
        val startIndex = index.coerceAtLeast(0)
        val startPosition = if (index >= 0) sessionPrefs.getLong(SESSION_POSITION, 0L) else 0L
        withController { controller ->
            if (controller.mediaItemCount > 0 || _state.value.currentTrack != null) return@withController
            scope.launch {
                val items = withContext(Dispatchers.IO) {
                    tracks.mapIndexed { i, track ->
                        buildMediaItem(track, if (abs(i - startIndex) <= 1) loadTrackArtworkBytes(track) else null)
                    }
                }
                if (controller.mediaItemCount > 0) return@launch
                controller.shuffleModeEnabled = false
                shuffleOn = sessionPrefs.getBoolean(SESSION_SHUFFLED, false) && tracks.size > 1
                orderBeforeShuffle = if (shuffleOn) {
                    sessionPrefs.getString(SESSION_IDS_BEFORE_SHUFFLE, null)
                        ?.split('\n')?.filter { it.isNotBlank() }
                } else null
                controller.repeatMode = sessionPrefs.getInt(SESSION_REPEAT, Player.REPEAT_MODE_OFF)
                // Not prepared on purpose: nothing plays and no notification appears until Play.
                controller.setMediaItems(items, startIndex, startPosition)
                rebuildQueueSnapshot()
                syncFromController()
            }
        }
    }

    fun playQueue(
        tracks: List<AudioTrackItem>,
        startIndex: Int,
        shuffle: Boolean = false,
        /** False for the pop-up player: what it plays doesn't replace the remembered queue. */
        remember: Boolean = true
    ) {
        if (tracks.isEmpty()) return
        rememberSession = remember
        val safeStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
        tracks.forEach { tracksById[it.id] = it }

        val targetTrack = tracks[safeStartIndex]
        // Tapping a song plays the list in its own order; Shuffle starts with the chosen song and
        // plays every other song once, in random order (the queue itself is reordered).
        val ordered = if (shuffle && tracks.size > 1) {
            listOf(targetTrack) + tracks.filterIndexed { i, _ -> i != safeStartIndex }.shuffled()
        } else {
            tracks
        }
        val orderedStart = if (shuffle && tracks.size > 1) 0 else safeStartIndex
        shuffleOn = shuffle && tracks.size > 1
        orderBeforeShuffle = if (shuffleOn) tracks.map { it.id } else null
        isLoadingQueue = true
        failuresInARow = 0
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
                ordered.mapIndexed { idx, track ->
                    val preloadNow = abs(idx - orderedStart) <= 2 || artworkCache.get(track.id) != null
                    val art = if (preloadNow) loadTrackArtworkBytes(track) else null
                    buildMediaItem(track, art)
                }
            }

            withController { controller ->
                // Keep the user's repeat choice. Shuffle is already in the order of the songs.
                if (controller.shuffleModeEnabled) controller.shuffleModeEnabled = false
                controller.setMediaItems(initialMediaItems, orderedStart, 0L)
                controller.prepare()
                controller.play()
                isLoadingQueue = false
                syncFromController()
            }

            // Populate artwork for any remaining tracks in the queue in the background
            ordered.forEachIndexed { idx, track ->
                if (!isActive) return@launch
                if (abs(idx - orderedStart) > 2 && artworkCache.get(track.id) == null) {
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

    /** Inserts [track] right after the current song. Starts playback if nothing is loaded. */
    fun playNext(track: AudioTrackItem) = insertIntoQueue(listOf(track), next = true)

    /** Appends [track] to the end of the queue. Starts playback if nothing is loaded. */
    fun addToQueue(track: AudioTrackItem) = insertIntoQueue(listOf(track), next = false)

    /** Inserts [tracks], in order, right after the current song. */
    fun playNext(tracks: List<AudioTrackItem>) = insertIntoQueue(tracks, next = true)

    /** Appends [tracks], in order, to the end of the queue. */
    fun addToQueue(tracks: List<AudioTrackItem>) = insertIntoQueue(tracks, next = false)

    private fun insertIntoQueue(tracks: List<AudioTrackItem>, next: Boolean) {
        if (tracks.isEmpty()) return
        tracks.forEach { tracksById[it.id] = it }
        if (_state.value.currentTrack == null) {
            playQueue(tracks, 0)
            return
        }
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                tracks.map { track -> buildMediaItem(track, loadTrackArtworkBytes(track)) }
            }
            withController { controller ->
                if (controller.mediaItemCount == 0) {
                    controller.setMediaItems(items)
                    controller.prepare()
                    controller.play()
                } else if (next) {
                    controller.addMediaItems(controller.currentMediaItemIndex + 1, items)
                } else {
                    controller.addMediaItems(items)
                }
                syncFromController()
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

    private fun rebuildQueueSnapshot() {
        val controller = mediaController ?: return
        queueSnapshot = (0 until controller.mediaItemCount).mapNotNull { i ->
            val item = controller.getMediaItemAt(i)
            tracksById[item.mediaId] ?: reconstructTrackFromMediaItem(item)
        }
    }

    /** Jumps to the song at [index] in the queue and plays it. */
    fun skipToQueueItem(index: Int) {
        withController { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.seekTo(index, 0L)
                controller.prepareIfIdle()
                controller.play()
            }
            syncFromController()
        }
    }

    /** Moves a song inside the queue (drag to reorder). The current song keeps playing. */
    fun moveQueueItem(from: Int, to: Int) {
        withController { controller ->
            val count = controller.mediaItemCount
            if (from in 0 until count && to in 0 until count && from != to) {
                controller.moveMediaItem(from, to)
            }
        }
    }

    /** Removes a song from the queue. Removing the current song moves on to the next one. */
    fun removeQueueItem(index: Int) {
        withController { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.removeMediaItem(index)
            }
        }
    }

    fun togglePlayPause() {
        withController { controller ->
            // Same rule as the button: while the next song loads it still counts as playing.
            if (!Util.shouldShowPlayButton(controller)) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_ENDED) {
                    controller.seekTo(0L)
                }
                controller.prepareIfIdle()
                controller.play()
            }
            syncFromController()
        }
    }

    fun toggleShuffle() = setShuffle(!shuffleOn)

    /**
     * On: the current song stays, every other song in the queue follows once in random order.
     * Off: the songs go back to the order they had before, carrying on from the current song.
     * The current song keeps playing either way.
     */
    fun setShuffle(on: Boolean) {
        withController { controller ->
            if (controller.shuffleModeEnabled) controller.shuffleModeEnabled = false
            val count = controller.mediaItemCount
            val current = controller.currentMediaItemIndex
            if (count <= 1 || current !in 0 until count || on == shuffleOn) {
                shuffleOn = on && count > 1
                if (!shuffleOn) orderBeforeShuffle = null
                syncFromController()
                saveSession()
                return@withController
            }
            val items = (0 until count).map { controller.getMediaItemAt(it) }
            val before: List<MediaItem>
            val after: List<MediaItem>
            if (on) {
                orderBeforeShuffle = items.map { it.mediaId }
                before = emptyList()
                after = items.filterIndexed { i, _ -> i != current }.shuffled()
            } else {
                val restored = orderBefore(items, current, orderBeforeShuffle)
                before = restored.first
                after = restored.second
                orderBeforeShuffle = null
            }
            shuffleOn = on
            replaceAroundCurrent(controller, before, after)
            rebuildQueueSnapshot()
            syncFromController()
            saveSession()
        }
    }

    /**
     * The queue's songs (all but the current one) split into the ones before and after the current
     * song, in [original] order. Songs removed meanwhile stay removed; songs added while shuffled
     * (Play next / Add to queue) follow the current song.
     */
    private fun orderBefore(
        items: List<MediaItem>,
        current: Int,
        original: List<String>?
    ): Pair<List<MediaItem>, List<MediaItem>> {
        val others = items.filterIndexed { i, _ -> i != current }
        val currentId = items[current].mediaId
        if (original == null || currentId !in original) return emptyList<MediaItem>() to others
        val pool = HashMap<String, ArrayDeque<MediaItem>>()
        others.forEach { pool.getOrPut(it.mediaId) { ArrayDeque() }.addLast(it) }
        val before = ArrayList<MediaItem>()
        val after = ArrayList<MediaItem>()
        var passedCurrent = false
        for (id in original) {
            if (!passedCurrent && id == currentId) {
                passedCurrent = true
                continue
            }
            val item = pool[id]?.removeFirstOrNull() ?: continue
            if (passedCurrent) after += item else before += item
        }
        // Same object, not just the same song: the queue can hold a song twice.
        val leftover = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<MediaItem, Boolean>())
        pool.values.forEach { leftover.addAll(it) }
        after.addAll(0, others.filter { it in leftover })
        return before to after
    }

    /** Puts [before] and [after] around the current song without interrupting it. */
    private fun replaceAroundCurrent(controller: MediaController, before: List<MediaItem>, after: List<MediaItem>) {
        val current = controller.currentMediaItemIndex
        val count = controller.mediaItemCount
        if (current + 1 < count) controller.removeMediaItems(current + 1, count)
        if (current > 0) controller.removeMediaItems(0, current)
        if (before.isNotEmpty()) controller.addMediaItems(0, before)
        if (after.isNotEmpty()) controller.addMediaItems(controller.mediaItemCount, after)
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
                controller.prepareIfIdle()
                controller.play()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
                controller.prepareIfIdle()
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
            controller.prepareIfIdle()
            controller.play()
            syncFromController()
        }
    }

    /**
     * Swaps the song that's playing for [track] (the same song, from the library) at the same
     * spot. Used when the pop-up player hands a file from another app over to Doppler: that app's
     * permission to read the file ends with the pop-up, the library copy keeps working.
     */
    suspend fun continueWith(track: AudioTrackItem) {
        tracksById[track.id] = track
        rememberSession = true
        shuffleOn = false
        orderBeforeShuffle = null
        val controller = mediaController ?: return
        val position = controller.currentPosition
        val keepPlaying = controller.playWhenReady
        val art = withContext(Dispatchers.IO) { loadTrackArtworkBytes(track) }
        controller.setMediaItems(listOf(buildMediaItem(track, art)), 0, position)
        controller.prepare()
        controller.playWhenReady = keepPlaying
        syncFromController()
    }

    /**
     * When only one song is loaded (it came from the pop-up player), puts the library songs around
     * it without interrupting it, so Next and Previous move through the library like a song
     * started from the Songs tab. A song that isn't in [library] gets the library after it.
     */
    fun surroundSingleSongWith(library: List<AudioTrackItem>) {
        if (library.isEmpty()) return
        val controller = mediaController ?: return
        if (controller.mediaItemCount != 1) return
        val currentId = controller.currentMediaItem?.mediaId ?: return
        val index = library.indexOfFirst { it.id == currentId }
        val before = if (index > 0) library.subList(0, index) else emptyList()
        val after = if (index >= 0) library.subList(index + 1, library.size) else library
        library.forEach { tracksById[it.id] = it }
        scope.launch {
            // Covers for the songs right next to it now; the notification fills in the rest later.
            val beforeItems = withContext(Dispatchers.IO) {
                before.mapIndexed { i, t -> buildMediaItem(t, if (i == before.lastIndex) loadTrackArtworkBytes(t) else null) }
            }
            val afterItems = withContext(Dispatchers.IO) {
                after.mapIndexed { i, t -> buildMediaItem(t, if (i == 0) loadTrackArtworkBytes(t) else null) }
            }
            if (controller.mediaItemCount != 1 || controller.currentMediaItem?.mediaId != currentId) return@launch
            // Like tapping that song in the Songs tab: the list in its own order.
            shuffleOn = false
            orderBeforeShuffle = null
            if (beforeItems.isNotEmpty()) controller.addMediaItems(0, beforeItems)
            if (afterItems.isNotEmpty()) controller.addMediaItems(controller.mediaItemCount, afterItems)
            rebuildQueueSnapshot()
            syncFromController()
            saveSession()
        }
    }

    /** The pop-up's song became the app's queue ("Open in Doppler"): remember it from now on. */
    fun rememberCurrentQueue() {
        rememberSession = true
        saveSession()
    }

    /** Hides the error dialog. */
    fun clearError() {
        _lastError.value = null
    }

    /** After an error the player is idle; it has to be prepared again before it can play. */
    private fun MediaController.prepareIfIdle() {
        if (playbackState == Player.STATE_IDLE && mediaItemCount > 0) prepare()
    }

    /** "ERROR_CODE_… (3001)" followed by every underlying reason, so nothing is cut off. */
    private fun describeError(error: androidx.media3.common.PlaybackException): String = buildString {
        append(error.errorCodeName).append(" (").append(error.errorCode).append(')')
        error.message?.takeIf { it.isNotBlank() }?.let { append('\n').append(it) }
        generateSequence(error.cause) { it.cause }.take(6).forEach { cause ->
            append('\n').append(cause.javaClass.simpleName.ifBlank { "Cause" })
            cause.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
        }
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                checkSleepTimer()
                syncFromController()
                val playing = _state.value.isPlaying
                // While playing, the spot is saved every 10 seconds too.
                if (playing && SystemClock.elapsedRealtime() - lastSavedAt > 10_000L) saveSession()
                // Battery: the position is read 4 times a second only while a screen shows it.
                // Playing in the background, once a second is plenty (sleep timer, saving).
                // Paused, nothing moves: wait until it plays again or a sleep timer starts
                // (player events keep the state up to date meanwhile).
                when {
                    playing && _state.subscriptionCount.value > 0 -> delay(250L)
                    playing || sleepTimerEndAtMs > 0L -> delay(1_000L)
                    else -> _state.first { it.isPlaying || sleepTimerEndAtMs > 0L }
                }
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
                // Media3's own rule for the Play/Pause button: still "playing" while the next song
                // loads, so the button, the cover and the wave don't blink on every skip.
                isPlaying = !Util.shouldShowPlayButton(controller),
                positionMs = pos,
                durationMs = dur,
                isEnded = ended,
                shuffleEnabled = shuffleOn,
                repeatMode = controller.repeatMode,
                queue = queueSnapshot,
                queueIndex = controller.currentMediaItemIndex.coerceAtLeast(0)
            )
        }
    }

    private fun reconstructTrackFromMediaItem(item: MediaItem): AudioTrackItem? {
        val uri = item.localConfiguration?.uri ?: return null
        val meta = item.mediaMetadata
        val title = meta.title?.toString()?.takeIf { it.isNotBlank() } ?: "Audio Track"
        val artist = cleanArtistName(meta.artist?.toString())
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
        private const val SESSION_IDS = "ids"
        private const val SESSION_INDEX = "index"
        private const val SESSION_POSITION = "position"
        private const val SESSION_SHUFFLED = "shuffled"
        private const val SESSION_IDS_BEFORE_SHUFFLE = "ids_before_shuffle"
        private const val SESSION_REPEAT = "repeat"

        @Volatile
        private var instance: AudioPlaybackManager? = null

        /** The manager if the app has already made it (never creates one). */
        fun existingInstance(): AudioPlaybackManager? = instance

        fun getInstance(context: Context): AudioPlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlaybackManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
