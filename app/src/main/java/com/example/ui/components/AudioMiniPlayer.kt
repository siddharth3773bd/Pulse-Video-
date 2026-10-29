@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlin.math.absoluteValue
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
 *
 * Swipe the cover and song name left/right to change song (the same idea as the cover on Now
 * Playing). A thin progress ring around the round cover shows how far the song is.
 */
@Composable
fun AudioMiniPlayer(
    track: AudioTrackItem,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
    /** The play queue, in order. With it, swiping changes song. Empty: no swiping. */
    queue: List<AudioTrackItem> = emptyList(),
    queueIndex: Int = 0,
    onSkipToQueueItem: (Int) -> Unit = {},
    /** How far the song is, 0..1. */
    progress: () -> Float = { 0f }
) {
    val scheme = MaterialTheme.colorScheme
    val openLatest by rememberUpdatedState(onOpen)
    val thresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val motion = MaterialTheme.motionScheme

    // ---- Swipe to change song: a pager over the queue (same logic as ArtworkPager on Now Playing).
    val tracks = if (queue.isEmpty()) listOf(track) else queue
    val currentIndex = if (queue.isEmpty()) 0 else queueIndex.coerceIn(0, tracks.lastIndex)
    val pagerState = rememberPagerState(initialPage = currentIndex, pageCount = { tracks.size })
    val skipLatest by rememberUpdatedState(onSkipToQueueItem)
    val currentIndexLatest by rememberUpdatedState(currentIndex)
    val hasQueueLatest by rememberUpdatedState(queue.isNotEmpty())
    val glideSpec = motion.defaultSpatialSpec<Float>()
    // Snappy settle without a bounce (a bouncy snap can make a pager flip back).
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    )
    // Only a swipe by the user picks a song; covers that glide on their own never do.
    var userSwiped by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { if (it is DragInteraction.Start) userSwiped = true }
    }
    // A small tick each time the swipe passes halfway to another song.
    LaunchedEffect(pagerState) {
        var last = pagerState.currentPage
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != last) {
                last = page
                if (userSwiped) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
    }
    // Each page is keyed by its song, so reordering the queue keeps the pager on the same song.
    val pageKeys = remember(tracks) {
        val seen = HashMap<String, Int>()
        tracks.map { t ->
            val n = seen.getOrDefault(t.id, 0)
            seen[t.id] = n + 1
            "${t.id}#$n"
        }
    }
    val currentSongId = tracks.getOrNull(currentIndex)?.id
    var shownSongId by remember { mutableStateOf(currentSongId) }
    // The song changed some other way (buttons, auto-advance): glide the pill along.
    LaunchedEffect(currentIndex, currentSongId) {
        val sameSong = currentSongId == shownSongId
        shownSongId = currentSongId
        val from = pagerState.currentPage
        if (from != currentIndex && currentIndex < tracks.size) {
            userSwiped = false
            if (sameSong) {
                pagerState.scrollToPage(currentIndex)
                return@LaunchedEffect
            }
            if (kotlin.math.abs(currentIndex - from) > 1) {
                pagerState.scrollToPage(if (currentIndex > from) currentIndex - 1 else currentIndex + 1)
            }
            pagerState.animateScrollToPage(currentIndex, animationSpec = glideSpec)
        }
    }
    // The user swiped to another song and it settled: play that song.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (userSwiped) {
                userSwiped = false
                if (hasQueueLatest && page != currentIndexLatest) skipLatest(page)
            }
        }
    }

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
            // Swiping isn't visible to screen readers, so offer the same thing as actions.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next song") { onSkipNext(); true },
                    CustomAccessibilityAction("Previous song") { onSkipPrevious(); true }
                )
            }
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
            // 7dp on the left keeps the 50dp ring centred in the pill's round end.
            modifier = Modifier.padding(start = 7.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalPager(
                state = pagerState,
                flingBehavior = flingBehavior,
                beyondViewportPageCount = 1,
                key = { page -> pageKeys.getOrElse(page) { "page#$page" } },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { page ->
                val pageTrack = tracks[page]
                val isCurrent = page == currentIndex
                // The song that's leaving fades and shrinks a little, the arriving one grows in.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val distance = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                .absoluteValue.coerceIn(0f, 1f)
                            val scale = 0.9f + 0.1f * (1f - distance)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.7f * distance
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thin progress ring around the round cover: a faint full circle with the
                    // song's progress drawn over it from 12 o'clock (same look as Echo Music).
                    val ringProgress = animateFloatAsState(
                        targetValue = if (isCurrent) progress().coerceIn(0f, 1f) else 0f,
                        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
                        label = "miniPlayerRing"
                    )
                    val ringColor = scheme.primary
                    val ringTrackColor = scheme.outline.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .drawWithContent {
                                drawContent()
                                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                // Inset by the line width so the ring stays inside its box.
                                val diameter = size.minDimension - stroke.width
                                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                drawArc(
                                    color = ringTrackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = stroke
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * ringProgress.value,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = stroke
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AudioArt(
                            uri = pageTrack.contentUri,
                            placeholderBackground = scheme.secondaryContainer,
                            placeholderTint = scheme.onSecondaryContainer,
                            // Only the current song's cover is the one that grows into Now Playing.
                            modifier = (if (isCurrent) Modifier.playerCover() else Modifier)
                                .size(40.dp)
                                .border(1.dp, scheme.outline.copy(alpha = 0.3f), CircleShape),
                            shape = CircleShape
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pageTrack.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pageTrack.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
