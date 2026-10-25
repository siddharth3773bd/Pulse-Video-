@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrackItem

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
    // Inside AlbumArtTheme, so these are the album-art colours when that option is on.
    val scheme = MaterialTheme.colorScheme
    val openLatest by rememberUpdatedState(onOpen)
    val thresholdPx = with(LocalDensity.current) { 36.dp.toPx() }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(50),
        color = scheme.primaryContainer,
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
                placeholderBackground = scheme.surfaceContainerHighest,
                placeholderTint = scheme.primary,
                modifier = Modifier.size(52.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = scheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Official Expressive ButtonGroup: shape-morphing icon buttons that spring wider on press.
            val previousInteraction = remember { MutableInteractionSource() }
            val playPauseInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                customItem(
                    buttonGroupContent = {
                        IconButton(
                            onClick = onSkipPrevious,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                                contentColor = scheme.onPrimaryContainer
                            ),
                            interactionSource = previousInteraction,
                            modifier = Modifier
                                .animateWidth(previousInteraction)
                                .size(42.dp)
                        ) {
                            Icon(Symbols.SkipPrevious, contentDescription = "Previous")
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text("Previous") },
                            leadingIcon = { Icon(Symbols.SkipPrevious, contentDescription = null) },
                            onClick = {
                                onSkipPrevious()
                                menuState.dismiss()
                            }
                        )
                    }
                )
                customItem(
                    buttonGroupContent = {
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            ),
                            interactionSource = playPauseInteraction,
                            modifier = Modifier
                                .animateWidth(playPauseInteraction)
                                .size(52.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Symbols.Pause else Symbols.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(if (isPlaying) "Pause" else "Play") },
                            leadingIcon = {
                                Icon(
                                    if (isPlaying) Symbols.Pause else Symbols.PlayArrow,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onTogglePlayPause()
                                menuState.dismiss()
                            }
                        )
                    }
                )
                customItem(
                    buttonGroupContent = {
                        IconButton(
                            onClick = onSkipNext,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                                contentColor = scheme.onPrimaryContainer
                            ),
                            interactionSource = nextInteraction,
                            modifier = Modifier
                                .animateWidth(nextInteraction)
                                .size(42.dp)
                        ) {
                            Icon(Symbols.SkipNext, contentDescription = "Next")
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text("Next") },
                            leadingIcon = { Icon(Symbols.SkipNext, contentDescription = null) },
                            onClick = {
                                onSkipNext()
                                menuState.dismiss()
                            }
                        )
                    }
                )
            }
        }
    }
}
