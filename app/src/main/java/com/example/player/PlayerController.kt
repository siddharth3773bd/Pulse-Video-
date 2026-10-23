package com.example.player

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
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
    val activeSubtitleText: String = ""
)

/**
 * Engine-agnostic player interface.
 * UI components interact exclusively with PlayerController and never reference ExoPlayer directly.
 */
interface PlayerController {
    val state: StateFlow<PlayerState>
    val tracks: StateFlow<PlayerTracks>

    fun prepareMedia(
        uri: Uri,
        filePath: String? = null,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
        decoderMode: DecoderMode = DecoderMode.AUTO
    )

    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun selectAudioTrack(option: PlayerTrackOption)
    fun selectSubtitleTrack(option: PlayerTrackOption)
    fun setSubtitlesEnabled(enabled: Boolean)
    fun release()

    @Composable
    fun RenderVideoSurface(modifier: Modifier)
}
