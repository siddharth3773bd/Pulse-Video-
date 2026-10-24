package com.example.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrackItem
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily

/**
 * Pill-shaped mini player shown above the bottom navigation while a song is loaded.
 * Tap it, or drag it upwards, to open the Now Playing screen.
 */
@Composable
fun AudioMiniPlayer(
    track: AudioTrackItem,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val openLatest by rememberUpdatedState(onOpen)
    val thresholdPx = with(LocalDensity.current) { 36.dp.toPx() }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(50),
        color = palette.primaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("audio_mini_player")
            .pointerInput(thresholdPx) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -thresholdPx) openLatest()
                        total = 0f
                    },
                    onDragCancel = { total = 0f }
                ) { _, dragAmount ->
                    total += dragAmount
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHighest,
                placeholderTint = palette.primary,
                modifier = Modifier.size(52.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = palette.onPrimaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = palette.onPrimaryContainer.copy(alpha = 0.75f),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = palette.onPrimaryContainer.copy(alpha = 0.12f),
                        contentColor = palette.onPrimaryContainer
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = palette.onPrimaryContainer.copy(alpha = 0.12f),
                        contentColor = palette.onPrimaryContainer
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
