@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableFloatStateOf
import java.util.Locale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.model.AudioTrackItem
import com.example.model.NowPlayingAction
import com.example.model.formatDurationMs
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily

/** Simple "lines of text" icon for the Lyrics button (no extra icon dependency needed). */
val LyricsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Lyrics",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 7f); lineTo(4f, 7f); close()
            moveTo(4f, 9f); lineTo(20f, 9f); lineTo(20f, 11f); lineTo(4f, 11f); close()
            moveTo(4f, 13f); lineTo(20f, 13f); lineTo(20f, 15f); lineTo(4f, 15f); close()
            moveTo(4f, 17f); lineTo(14f, 17f); lineTo(14f, 19f); lineTo(4f, 19f); close()
        }
    }.build()
}

fun nowPlayingActionIcon(action: NowPlayingAction): ImageVector = when (action) {
    NowPlayingAction.SHUFFLE -> Icons.Default.Shuffle
    NowPlayingAction.REPEAT -> Icons.Default.Repeat
    NowPlayingAction.LYRICS -> LyricsIcon
    NowPlayingAction.SLEEP -> Icons.Default.Bedtime
}

/**
 * Full-screen Now Playing page. Layout is inspired by Echo Music (centered "Now Playing" header,
 * big cover) with Pixel Player style controls and a customizable bottom button tray.
 */
@Composable
fun NowPlayingScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    liked: Boolean,
    trayActions: List<NowPlayingAction>,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onLyrics: () -> Unit,
    sleepTimerRemainingMs: Long,
    sleepAtEndOfSong: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAtEndOfSong: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val context = LocalContext.current
    val closeLatest by rememberUpdatedState(onClose)
    val closeThresholdPx = with(LocalDensity.current) { 90.dp.toPx() }

    BackHandler(onBack = onClose)

    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var techLabel by remember(track.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(track.id) {
        techLabel = try {
            audioTechLabel(context, track)
        } catch (_: Throwable) {
            null
        }
    }

    val safeDuration = if (durationMs > 0L) durationMs else track.durationMs
    val liveFraction = if (safeDuration > 0L) {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = dragFraction ?: liveFraction
    val shownPositionMs = (fraction * safeDuration).toLong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.primaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .pointerInput(closeThresholdPx) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > closeThresholdPx) closeLatest()
                        total = 0f
                    },
                    onDragCancel = { total = 0f }
                ) { _, dragAmount ->
                    total += dragAmount
                }
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp)
            .testTag("audio_now_playing")
    ) {
        // ---- Header: collapse button, centered "Now Playing" + album, empty right side ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = palette.onPrimaryContainer.copy(alpha = 0.12f),
                    contentColor = palette.onPrimaryContainer
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close Now Playing")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Now Playing",
                    color = palette.onPrimaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = track.album.uppercase(),
                    color = palette.onPrimaryContainer.copy(alpha = 0.7f),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        // ---- Cover (takes whatever vertical room is left, always square) ----
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHighest,
                placeholderTint = palette.primary,
                modifier = Modifier.size(side),
                shape = RoundedCornerShape(28.dp),
                iconSize = 72.dp
            )
        }

        // ---- Title / artist + Like button ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = palette.onPrimaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = palette.onPrimaryContainer.copy(alpha = 0.75f),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FilledIconToggleButton(
                checked = liked,
                onCheckedChange = { onToggleLike() },
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = palette.onPrimaryContainer.copy(alpha = 0.12f),
                    contentColor = palette.onPrimaryContainer,
                    checkedContainerColor = palette.primary,
                    checkedContentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("now_playing_like")
            ) {
                Icon(
                    imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (liked) "Remove from liked songs" else "Add to liked songs"
                )
            }
        }

        // ---- Seek bar (official Slider + wavy track) + times + technical chip ----
        Spacer(modifier = Modifier.height(12.dp))
        PlaybackSeekBar(
            progress = fraction,
            onSeekPreview = { dragFraction = it },
            onSeekFinished = { target ->
                dragFraction = null
                onSeekTo(target)
            },
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_seek_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDurationMs(shownPositionMs),
                color = palette.onPrimaryContainer.copy(alpha = 0.85f),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val chip = techLabel
                if (!chip.isNullOrBlank()) {
                    // Slim, low-contrast pill so it reads as metadata, not a button.
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.onPrimaryContainer.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = chip,
                            color = palette.onPrimaryContainer.copy(alpha = 0.8f),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Text(
                text = formatDurationMs(safeDuration),
                color = palette.onPrimaryContainer.copy(alpha = 0.85f),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ---- Main controls: official Expressive ButtonGroup ----
        // ButtonGroup + animateWidth springs the pressed button wider and squeezes its
        // neighbours; IconButtonDefaults shapes morph the corners on press.
        Spacer(modifier = Modifier.height(16.dp))
        val previousInteraction = remember { MutableInteractionSource() }
        val playPauseInteraction = remember { MutableInteractionSource() }
        val nextInteraction = remember { MutableInteractionSource() }
        ButtonGroup(
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            customItem(
                buttonGroupContent = {
                    FilledTonalIconButton(
                        onClick = onSkipPrevious,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = palette.secondaryContainer,
                            contentColor = palette.onSecondaryContainer
                        ),
                        interactionSource = previousInteraction,
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(previousInteraction)
                            .height(72.dp)
                            .testTag("now_playing_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Previous") },
                        leadingIcon = { Icon(Icons.Default.SkipPrevious, contentDescription = null) },
                        onClick = {
                            onSkipPrevious()
                            menuState.dismiss()
                        }
                    )
                }
            )
            // Official icon button shapes: stays a round pill, squishes on press.
            customItem(
                buttonGroupContent = {
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        interactionSource = playPauseInteraction,
                        modifier = Modifier
                            .weight(1.25f)
                            .animateWidth(playPauseInteraction)
                            .height(80.dp)
                            .testTag("now_playing_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(if (isPlaying) "Pause" else "Play") },
                        leadingIcon = {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                    FilledTonalIconButton(
                        onClick = onSkipNext,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = palette.secondaryContainer,
                            contentColor = palette.onSecondaryContainer
                        ),
                        interactionSource = nextInteraction,
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(nextInteraction)
                            .height(72.dp)
                            .testTag("now_playing_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Next") },
                        leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                        onClick = {
                            onSkipNext()
                            menuState.dismiss()
                        }
                    )
                }
            )
        }

        // ---- Bottom tray: official connected ButtonGroup ----
        // Same official container as the main controls: pressing a toggle springs it wider
        // and squeezes its neighbours (ButtonGroup + animateWidth).
        var showSleepSheet by remember { mutableStateOf(false) }
        val sleepActive = sleepTimerRemainingMs > 0L || sleepAtEndOfSong
        // The running sleep timer morphs into a wider pill that shows the countdown.
        val sleepWeight by animateFloatAsState(
            targetValue = if (sleepActive) 2f else 1f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "trayWeight_sleep"
        )
        val trayInteractions = remember { mutableMapOf<NowPlayingAction, MutableInteractionSource>() }
        if (trayActions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trayActions.forEachIndexed { index, action ->
                    val interaction = trayInteractions.getOrPut(action) { MutableInteractionSource() }
                    val active = when (action) {
                        NowPlayingAction.SHUFFLE -> shuffleEnabled
                        NowPlayingAction.REPEAT -> repeatMode != Player.REPEAT_MODE_OFF
                        NowPlayingAction.SLEEP -> sleepActive
                        NowPlayingAction.LYRICS -> false
                    }
                    val icon = when {
                        action == NowPlayingAction.REPEAT && repeatMode == Player.REPEAT_MODE_ONE ->
                            Icons.Default.RepeatOne
                        else -> nowPlayingActionIcon(action)
                    }
                    val onTrayAction: () -> Unit = {
                        when (action) {
                            NowPlayingAction.SHUFFLE -> onToggleShuffle()
                            NowPlayingAction.REPEAT -> onCycleRepeat()
                            NowPlayingAction.LYRICS -> onLyrics()
                            NowPlayingAction.SLEEP -> showSleepSheet = true
                        }
                    }
                    customItem(
                        buttonGroupContent = {
                            val shapes = when {
                                trayActions.size == 1 -> ToggleButtonDefaults.shapesFor(buttonHeight = 60.dp)
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == trayActions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            ToggleButton(
                                checked = active,
                                onCheckedChange = { onTrayAction() },
                                shapes = shapes,
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = palette.onPrimaryContainer.copy(alpha = 0.10f),
                                    contentColor = palette.onPrimaryContainer,
                                    checkedContainerColor = palette.primary,
                                    checkedContentColor = palette.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                interactionSource = interaction,
                                modifier = Modifier
                                    .weight(if (action == NowPlayingAction.SLEEP) sleepWeight else 1f)
                                    .animateWidth(interaction)
                                    .height(60.dp)
                                    .testTag("now_playing_tray_${action.id}")
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = action.label,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (action == NowPlayingAction.SLEEP && sleepActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (sleepAtEndOfSong) "End" else formatCountdown(sleepTimerRemainingMs),
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(action.label) },
                                leadingIcon = { Icon(icon, contentDescription = null) },
                                onClick = {
                                    onTrayAction()
                                    menuState.dismiss()
                                }
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (showSleepSheet) {
            SleepTimerSheet(
                isActive = sleepActive,
                remainingMs = sleepTimerRemainingMs,
                atEndOfSong = sleepAtEndOfSong,
                onStart = { minutes ->
                    onStartSleepTimer(minutes)
                    showSleepSheet = false
                },
                onEndOfSong = {
                    onSleepAtEndOfSong()
                    showSleepSheet = false
                },
                onCancel = {
                    onCancelSleepTimer()
                    showSleepSheet = false
                },
                onDismiss = { showSleepSheet = false }
            )
        }
    }
}

/** "14:56" style countdown; shows hours as "1:05:00" when needed. */
private fun formatCountdown(ms: Long): String {
    val totalSec = ((ms + 999L) / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

/** Sleep timer picker: 5 to 120 minutes in 5-minute steps, or stop at the end of the song. */
@Composable
private fun SleepTimerSheet(
    isActive: Boolean,
    remainingMs: Long,
    atEndOfSong: Boolean,
    onStart: (Int) -> Unit,
    onEndOfSong: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var minutes by remember { mutableFloatStateOf(30f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .testTag("sleep_timer_sheet")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sleep timer",
                    color = palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (atEndOfSong) "Stops at the end of this song" else "Stops in ${formatCountdown(remainingMs)}",
                    color = palette.primary,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${minutes.toInt()} min",
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Official Material 3 slider, snapping in 5-minute steps (5..120 = 23 stops).
            val sleepSliderState = remember(22) { SliderState(value = minutes, steps = 22, trackRange = 5f..120f) }
            sleepSliderState.value = minutes
            Slider(
                state = sleepSliderState,
                onValueChange = { minutes = (it / 5f).let { v -> Math.round(v) * 5f }.coerceIn(5f, 120f) },
                colors = SliderDefaults.colors(
                    thumbColor = palette.primary,
                    activeTrackColor = palette.primary,
                    inactiveTrackColor = palette.surfaceContainerHighest,
                    activeTickColor = palette.onPrimary.copy(alpha = 0.5f),
                    inactiveTickColor = palette.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_timer_slider")
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("5 min", color = palette.onSurfaceVariant, fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("120 min", color = palette.onSurfaceVariant, fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onStart(minutes.toInt()) },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Start ${minutes.toInt()} min timer",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            FilledTonalButton(
                onClick = onEndOfSong,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.secondaryContainer,
                    contentColor = palette.onSecondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "End of song",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Turn off timer",
                        color = palette.error,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
