package com.example.player

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.text.Cue
import com.example.model.DecoderMode
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class PlayerTrackOption(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

@Immutable
data class PlayerTracks(
    val audioTracks: List<PlayerTrackOption> = emptyList(),
    val subtitleTracks: List<PlayerTrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = true
)

@Immutable
data class PlayerState(
    val currentMediaUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val activeSubtitleText: String = "",
    /** Current subtitle cues with their styling and position, for Media3's SubtitleView. */
    val cues: List<Cue> = emptyList(),
    /** Goes up each time a new picture is first shown (a new video, or after a seek). */
    val firstFrameCount: Int = 0
)

/**
 * Engine-agnostic player interface.
 * UI components interact exclusively with PlayerController and never reference ExoPlayer directly.
 */
interface PlayerController {
    val state: StateFlow<PlayerState>
    val tracks: StateFlow<PlayerTracks>

    /** Emits the uri of the next video when the player moved on to it by itself (autoplay). */
    val autoAdvanced: kotlinx.coroutines.flow.SharedFlow<String>

    /**
     * Lines up the next video inside the player, so it starts the moment this one ends (no
     * loading pause). Null clears it.
     */
    fun queueNext(uri: Uri?, filePath: String? = null, title: String? = null)

    /**
     * Jumps straight to the video lined up by [queueNext], if it is the one with this [uri]: it is
     * already waiting inside the player, so it starts almost instantly. False if it isn't queued
     * (the caller then loads it the normal way).
     */
    fun skipToQueuedNext(uri: Uri): Boolean

    fun prepareMedia(
        uri: Uri,
        filePath: String? = null,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
        decoderMode: DecoderMode = DecoderMode.AUTO,
        title: String? = null
    )

    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun selectAudioTrack(option: PlayerTrackOption)
    fun selectSubtitleTrack(option: PlayerTrackOption)
    fun setSubtitlesEnabled(enabled: Boolean)
    /** Adds a subtitle file the user picked (.srt, .vtt, .ass, .ssa, .ttml) and turns it on. */
    fun addSubtitleFile(uri: Uri, name: String)
    fun release()

    /** Audio only: stops drawing the picture (the sound and position carry on). */
    fun setVideoEnabled(enabled: Boolean)
    /** Keeps the sound playing after leaving the app, with a media notification. */
    fun setBackgroundAudio(enabled: Boolean)
    /**
     * Languages to pick automatically when a video has several audio or subtitle tracks
     * (ISO codes like "hi", "en"; null or blank = the video's own default).
     */
    fun setPreferredLanguages(audio: String?, subtitles: String?)

    /**
     * Scrubbing mode (Media3): while on, seeks are optimised for many quick jumps (dragging the
     * seek bar or swiping), so the picture follows the finger. Turn off when the finger lifts.
     */
    fun setScrubbing(enabled: Boolean)

    /** 0..1, used by the sleep timer's fade-out. */
    fun setVolume(volume: Float)

    /**
     * The video, drawn with the official Media3 Compose ContentFrame. [placeholder] (the video's
     * thumbnail) shows until the first frame is ready, instead of a black box.
     */
    @Composable
    fun RenderVideoSurface(
        modifier: Modifier,
        contentScale: ContentScale,
        placeholder: @Composable () -> Unit = {}
    )
}
