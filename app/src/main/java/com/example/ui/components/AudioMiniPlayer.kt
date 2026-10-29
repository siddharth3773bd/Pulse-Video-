@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrackItem
import com.example.ui.icons.Symbols
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.ln
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mini player: a floating pill above the navigation bar while a song is loaded, styled like the
 * official Expressive floating toolbar (pill shape, calm container, soft shadow) in the app's own
 * colours. Play/Pause is the one filled button; Previous and Next are plain icons.
 * Tap it, or drag it up, to open Now Playing.
 *
 * Swipe the whole pill left (next song) or right (previous song): it follows your finger with a
 * rubber-band feel, ticks when you've pulled far enough, and on release the song changes while the
 * pill springs back on Material's Expressive spring. The cover and names slide to the new song the
 * way they do on Now Playing, starting the moment you let go. With nothing to go to in that
 * direction the pill only stretches a little and bounces back.
 * A thin progress ring around the round cover shows how far the song is.
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
    /** The play queue, in order. Swiping needs it to know if there is a song before/after. */
    queue: List<AudioTrackItem> = emptyList(),
    queueIndex: Int = 0,
    onSkipToQueueItem: (Int) -> Unit = {},
    /** How far the song is, 0..1. */
    progress: () -> Float = { 0f }
) {
    val scheme = MaterialTheme.colorScheme
    val openLatest by rememberUpdatedState(onOpen)
    val density = LocalDensity.current
    val thresholdPx = with(density) { 36.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val motion = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()

    val queueLatest by rememberUpdatedState(queue)
    val queueIndexLatest by rememberUpdatedState(queueIndex)
    val skipNextLatest by rememberUpdatedState(onSkipNext)
    val skipToLatest by rememberUpdatedState(onSkipToQueueItem)

    // ---- Swipe: the whole pill follows the finger. ----
    var widthPx by remember { mutableFloatStateOf(0f) }
    var offsetPx by remember { mutableFloatStateOf(0f) }   // where the pill is drawn
    var rawDrag by remember { mutableFloatStateOf(0f) }    // how far the finger went (before the rubber band)
    var armed by remember { mutableStateOf(false) }        // pulled far enough to change song
    var settleJob by remember { mutableStateOf<Job?>(null) }

    // The song we're about to show. Set the instant you let go (the real song follows a moment
    // later), so the cover and names start sliding together with the pill instead of after it.
    var pending by remember { mutableStateOf<AudioTrackItem?>(null) }
    LaunchedEffect(track.id) { pending = null }               // the real song caught up
    LaunchedEffect(pending) { if (pending != null) { delay(1500); pending = null } }
    val shownTrack = pending ?: track

    val minCommitPx = with(density) { 56.dp.toPx() }
    val flingVelocityPx = with(density) { 700.dp.toPx() }
    val minFlingDistancePx = with(density) { 20.dp.toPx() }
    val rejectDistancePx = with(density) { 24.dp.toPx() }

    fun canGoNext() = queueLatest.size > 1 && queueIndexLatest < queueLatest.lastIndex
    fun canGoPrevious() = queueLatest.size > 1 && queueIndexLatest > 0
    fun canGo(towardNext: Boolean) = if (towardNext) canGoNext() else canGoPrevious()
    fun commitPx() = maxOf(widthPx * 0.2f, minCommitPx)
    fun limitFor(towardNext: Boolean) = widthPx * (if (canGo(towardNext)) 0.45f else 0.12f)
    fun goNext() {
        val q = queueLatest
        if (q.size > 1) pending = q[(queueIndexLatest + 1) % q.size]
        skipNextLatest()
    }
    fun swipeToPrevious() {
        val index = queueIndexLatest - 1
        queueLatest.getOrNull(index)?.let { pending = it }
        skipToLatest(index)
    }
    // 1:1 at first, then it gets heavier. Where there is no song to go to it stops much sooner.
    fun displayed(raw: Float): Float {
        val limit = limitFor(towardNext = raw < 0f)
        return if (limit <= 0f) 0f else rubberBand(raw, limit)
    }

    val dragState = rememberDraggableState { delta ->
        rawDrag += delta
        offsetPx = displayed(rawDrag)
        val nowArmed = canGo(towardNext = rawDrag < 0f) && abs(offsetPx) >= commitPx()
        if (nowArmed != armed) {
            armed = nowArmed
            if (nowArmed) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    Surface(
        onClick = onOpen,
        shape = CircleShape,
        color = scheme.surfaceContainerHighest,
        contentColor = scheme.onSurface,
        shadowElevation = 4.dp,
        // The drag sits outside the moving layer, so the finger and the touch area stay in step.
        // Grows into Now Playing and back (container transform); size modifiers come after it.
        modifier = modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    settleJob?.cancel()
                    // Picking it up mid-spring: continue from exactly where it is (no jump).
                    val limit = limitFor(towardNext = offsetPx < 0f)
                    rawDrag = if (limit > 0f) unRubberBand(offsetPx, limit) else 0f
                    armed = false
                },
                onDragStopped = { velocity ->
                    val raw = rawDrag
                    val towardNext = raw < 0f
                    val allowed = canGo(towardNext)
                    val distance = abs(offsetPx)
                    val flung = abs(velocity) > flingVelocityPx &&
                        distance > minFlingDistancePx &&
                        (velocity < 0f) == towardNext
                    val commit = allowed && (distance >= commitPx() || flung)
                    // The finger's speed, as the pill itself moves (the rubber band slows it), and
                    // only the part heading back to the middle: it never gets pushed further out.
                    val limit = limitFor(towardNext)
                    val slope = if (limit > 0f) exp(-abs(raw) / limit) else 0f
                    val awayFromMiddle = offsetPx != 0f && (velocity > 0f) == (offsetPx > 0f)
                    val releaseVelocity = if (awayFromMiddle) 0f else velocity * slope
                    armed = false
                    rawDrag = 0f
                    if (commit) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        if (towardNext) goNext() else swipeToPrevious()
                    } else if (!allowed && distance > rejectDistancePx) {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    }
                    // Back to the middle on Material's Expressive spring.
                    settleJob = scope.launch {
                        Animatable(offsetPx).animateTo(
                            targetValue = 0f,
                            animationSpec = motion.defaultSpatialSpec<Float>(),
                            initialVelocity = releaseVelocity
                        ) { offsetPx = value }
                    }
                }
            )
            .graphicsLayer { translationX = offsetPx }
            .playerContainer()
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .testTag("audio_mini_player")
            // Swiping isn't visible to screen readers, so offer the same thing as actions.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next song") { goNext(); true },
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
            // Material's lateral transition, the same as the title on Now Playing: the cover (with
            // its ring) and the names slide out together and the new ones slide in from the side
            // the song comes from. It also plays for the Next button.
            val slideSpec = motion.defaultSpatialSpec<IntOffset>()
            val fadeInSpec = motion.defaultEffectsSpec<Float>()
            val fadeOutSpec = motion.fastEffectsSpec<Float>()
            val shiftPx = with(density) { 32.dp.roundToPx() }
            AnimatedContent(
                targetState = shownTrack,
                contentKey = { it.id },
                transitionSpec = {
                    val direction = miniSkipDirection(queueLatest, initialState, targetState)
                    (slideInHorizontally(slideSpec) { direction * shiftPx } + fadeIn(fadeInSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { -direction * shiftPx } + fadeOut(fadeOutSpec)) using
                        SizeTransform(clip = false)
                },
                // Keeps the sliding content from drawing over the buttons.
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds(),
                label = "miniPlayerSong"
            ) { shown ->
                // Only the song that stays is the one whose cover grows into Now Playing.
                val isShown = shown.id == shownTrack.id
                // Thin progress ring around the round cover: a faint full circle with the song's
                // progress drawn over it from 12 o'clock (same look as Echo Music).
                val ring = rememberRingProgress(progress, active = isShown)
                val ringColor = scheme.primary
                val ringTrackColor = scheme.outline.copy(alpha = 0.2f)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(RingBoxSize)
                            .drawWithContent {
                                drawContent()
                                val stroke = Stroke(width = RingStrokeWidth.toPx(), cap = StrokeCap.Round)
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
                                    sweepAngle = 360f * ring.value,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = stroke
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AudioArt(
                            uri = shown.contentUri,
                            placeholderBackground = scheme.secondaryContainer,
                            placeholderTint = scheme.onSecondaryContainer,
                            modifier = (if (isShown) Modifier.playerCover() else Modifier)
                                .size(CoverSize)
                                .border(1.dp, scheme.outline.copy(alpha = 0.3f), CircleShape),
                            shape = CircleShape
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shown.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shown.artist,
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
                                // 42dp leaves 11dp above and below in the 64dp pill, about the
                                // 12dp the round cover has (48dp nearly touched the edges).
                                .size(42.dp)
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
                            onClick = { goNext() },
                            shapes = IconButtonDefaults.shapes(),
                            interactionSource = nextInteraction,
                            modifier = Modifier
                                .animateWidth(nextInteraction)
                                .testTag("mini_player_next")
                        ) { Icon(Symbols.SkipNext, contentDescription = "Next") }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(text = { Text("Next") }, onClick = { goNext(); menuState.dismiss() })
                    }
                )
            }
        }
    }
}

// Round cover and its progress ring. The ring sits inside its box, so the free space between the
// ring's inner edge and the cover is: (box - 2 x stroke - cover) / 2 = 2.5dp. That keeps bright
// cover edges from touching the progress arc.
private val RingBoxSize = 50.dp
private val RingStrokeWidth = 3.dp
private val RingCoverGap = 2.5.dp
private val CoverSize = RingBoxSize - RingStrokeWidth * 2 - RingCoverGap * 2

/** Follows the finger 1:1 at first, then slows down towards [limit] (it never goes past it). */
private fun rubberBand(raw: Float, limit: Float): Float =
    sign(raw) * limit * (1f - exp(-abs(raw) / limit))

/** +1 when [to] comes after [from] in the queue (Next), -1 for an earlier song (Previous). */
private fun miniSkipDirection(queue: List<AudioTrackItem>, from: AudioTrackItem, to: AudioTrackItem): Int {
    val a = queue.indexOfFirst { it.id == from.id }
    val b = queue.indexOfFirst { it.id == to.id }
    return if (a >= 0 && b >= 0 && b < a) -1 else 1
}

/** The reverse of [rubberBand]: how far the finger would have to be for the pill to be at [shown]. */
private fun unRubberBand(shown: Float, limit: Float): Float {
    val ratio = (abs(shown) / limit).coerceIn(0f, 0.999f)
    return sign(shown) * -limit * ln(1f - ratio)
}

private class FloatHolder(var value: Float)

/**
 * How far the ring is drawn. It follows the song's progress smoothly, but a big step back (a new
 * song starts) jumps instead of sweeping backwards; a song that is sliding out keeps the progress
 * it had instead of rewinding.
 */
@Composable
private fun rememberRingProgress(progress: () -> Float, active: Boolean): State<Float> {
    val live = progress().coerceIn(0f, 1f)
    val last = remember { FloatHolder(live) }
    if (active) last.value = live
    val target = last.value
    val animated = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (target < animated.value - 0.05f) {
            animated.snapTo(target)
        } else {
            animated.animateTo(target, tween(durationMillis = 250, easing = LinearEasing))
        }
    }
    return animated.asState()
}
