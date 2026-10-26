@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

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
import com.example.ui.icons.Symbols

/**
 * Mini player: a floating pill above the navigation bar while a song is loaded, styled like the
 * official Expressive floating toolbar (pill shape, calm container, soft shadow) in the app's own
 * colours. Play/Pause is the one filled button; Previous and Next are plain icons.
 * Tap it, or drag it up, to open Now Playing.
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
    val scheme = MaterialTheme.colorScheme
    val openLatest by rememberUpdatedState(onOpen)
    val thresholdPx = with(LocalDensity.current) { 36.dp.toPx() }

    Surface(
        onClick = onOpen,
        shape = CircleShape,
        color = scheme.surfaceContainerHighest,
        contentColor = scheme.onSurface,
        shadowElevation = 4.dp,
        // Grows into Now Playing and back (container transform); size modifiers come after it.
        modifier = modifier
            .playerContainer()
            .fillMaxWidth()
            .height(MiniPlayerHeight)
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
                ) { _, dragAmount -> total += dragAmount }
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = scheme.secondaryContainer,
                placeholderTint = scheme.onSecondaryContainer,
                modifier = Modifier
                    .playerCover()
                    .size(48.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Official Expressive button group: the pressed button springs wider.
            val previousInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            ButtonGroup(
                overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                customItem(
                    buttonGroupContent = {
                        IconButton(
                            onClick = onSkipPrevious,
                            shapes = IconButtonDefaults.shapes(),
                            interactionSource = previousInteraction,
                            modifier = Modifier
                                .animateWidth(previousInteraction)
                                .testTag("mini_player_previous")
                        ) { Icon(Symbols.SkipPrevious, contentDescription = "Previous") }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(text = { Text("Previous") }, onClick = { onSkipPrevious(); menuState.dismiss() })
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
                            interactionSource = playInteraction,
                            modifier = Modifier
                                .animateWidth(playInteraction)
                                .size(48.dp)
                                .testTag("mini_player_play_pause")
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
                            onClick = { onTogglePlayPause(); menuState.dismiss() }
                        )
                    }
                )
                customItem(
                    buttonGroupContent = {
                        IconButton(
                            onClick = onSkipNext,
                            shapes = IconButtonDefaults.shapes(),
                            interactionSource = nextInteraction,
                            modifier = Modifier
                                .animateWidth(nextInteraction)
                                .testTag("mini_player_next")
                        ) { Icon(Symbols.SkipNext, contentDescription = "Next") }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(text = { Text("Next") }, onClick = { onSkipNext(); menuState.dismiss() })
                    }
                )
            }
        }
    }
}
