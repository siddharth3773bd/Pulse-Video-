package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrackItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Square album art you can swipe through (a HorizontalPager over the queue):
 * swipe left/right to change song, double-tap to play or pause.
 *
 * Feedback: a double-tap flashes a play/pause icon over the art (with a haptic tick), and while
 * the music is paused the art stays slightly dimmed.
 */
@Composable
fun ArtworkPager(
    track: AudioTrackItem,
    queue: List<AudioTrackItem>,
    queueIndex: Int,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipToQueueItem: (Int) -> Unit,
    placeholderBackground: Color,
    placeholderTint: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    testTag: String = "now_playing_cover",
    /** Applied to the current song's cover only (Now Playing: the cover the mini player's grows into). */
    currentCoverModifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isPlayingLatest by rememberUpdatedState(isPlaying)
    val toggleLatest by rememberUpdatedState(onTogglePlayPause)

    // The play/pause flash shown after a double-tap.
    var flashIcon by remember { mutableStateOf<ImageVector?>(null) }
    val flashAlpha = remember { Animatable(0f) }
    val flashScale = remember { Animatable(0.7f) }
    fun doubleTap() {
        val wasPlaying = isPlayingLatest
        toggleLatest()
        haptic.performHapticFeedback(
            if (wasPlaying) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
        )
        flashIcon = if (wasPlaying) Symbols.Pause else Symbols.PlayArrow
        scope.launch {
            flashAlpha.snapTo(1f)
            flashScale.snapTo(0.7f)
            launch { flashScale.animateTo(1f, FlashPopSpring) }
            delay(450)
            flashAlpha.animateTo(0f, tween(250))
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val shape = RoundedCornerShape(cornerRadius)
        val coverTracks = if (queue.isEmpty()) listOf(track) else queue
        val coverIndex = if (queue.isEmpty()) 0 else queueIndex.coerceIn(0, coverTracks.lastIndex)
        val pagerState = rememberPagerState(initialPage = coverIndex, pageCount = { coverTracks.size })
        val skipLatest by rememberUpdatedState(onSkipToQueueItem)
        val coverIndexLatest by rememberUpdatedState(coverIndex)
        val hasQueue by rememberUpdatedState(queue.isNotEmpty())
        val glideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        // Only a swipe by the user picks a song; covers that glide on their own never do.
        var userSwiped by remember { mutableStateOf(false) }
        LaunchedEffect(pagerState) {
            pagerState.interactionSource.interactions.collect { if (it is DragInteraction.Start) userSwiped = true }
        }

        // Each cover is keyed by its song, so when the queue is reordered (shuffle on/off, a drag in
        // the queue) the pager stays on the same song instead of showing another cover. The same
        // song can be queued twice, so repeats get a number.
        val pageKeys = remember(coverTracks) {
            val seen = HashMap<String, Int>()
            coverTracks.map { t ->
                val n = seen.getOrDefault(t.id, 0)
                seen[t.id] = n + 1
                "${t.id}#$n"
            }
        }
        val currentSongId = coverTracks.getOrNull(coverIndex)?.id
        var shownSongId by remember { mutableStateOf(currentSongId) }

        // The song changed some other way (buttons, auto-advance, the queue): glide the art along.
        // A far jump glides just the last page, like Next, instead of racing through every cover.
        LaunchedEffect(coverIndex, currentSongId) {
            val sameSong = currentSongId == shownSongId
            shownSongId = currentSongId
            val from = pagerState.currentPage
            if (from != coverIndex && coverIndex < coverTracks.size) {
                userSwiped = false
                // Same song, new place in the queue: just be there, nothing to animate.
                if (sameSong) {
                    pagerState.scrollToPage(coverIndex)
                    return@LaunchedEffect
                }
                if (kotlin.math.abs(coverIndex - from) > 1) {
                    pagerState.scrollToPage(if (coverIndex > from) coverIndex - 1 else coverIndex + 1)
                }
                pagerState.animateScrollToPage(coverIndex, animationSpec = glideSpec)
            }
        }
        // The user swiped to another cover: play that song. It starts as soon as the finger lets
        // go past the halfway point (or, for a quick flick, as soon as the cover glides past it),
        // so the title, background and seek bar change together with the motion, not after it.
        // [committedPage] makes sure the same song isn't started twice (now and again at rest).
        val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
        var committedPage by remember { mutableIntStateOf(-1) }
        LaunchedEffect(coverIndex) { committedPage = -1 }
        val commit: (Int) -> Unit = { page ->
            if (hasQueue && page != coverIndexLatest && page != committedPage) {
                committedPage = page
                skipLatest(page)
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage to isDragged }.collect { (page, dragged) ->
                if (userSwiped && !dragged) commit(page)
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                if (userSwiped) {
                    userSwiped = false
                    commit(page)
                }
            }
        }

        // A small tick each time the swipe passes halfway to another song, so you feel it commit.
        LaunchedEffect(pagerState) {
            var last = pagerState.currentPage
            snapshotFlow { pagerState.currentPage }.collect { page ->
                if (page != last) {
                    last = page
                    if (userSwiped) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            }
        }
        // The cover settles on the same Expressive spring as the glide above and the rest of the app.
        val flingBehavior = PagerDefaults.flingBehavior(state = pagerState, snapAnimationSpec = glideSpec)

        HorizontalPager(
            state = pagerState,
            flingBehavior = flingBehavior,
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
            key = { page -> pageKeys.getOrElse(page) { "page#$page" } },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Depth: the cover that's leaving shrinks a little while the next one grows to
                    // full size, following your finger. At rest it's full size.
                    .graphicsLayer {
                        val distance = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue.coerceIn(0f, 1f)
                        val scale = 0.92f + 0.08f * (1f - distance)
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { doubleTap() }) }
                    .testTag(testTag),
                contentAlignment = Alignment.Center
            ) {
                val isCurrent = page == coverIndex
                Box(modifier = (if (isCurrent) currentCoverModifier else Modifier).size(side)) {
                    AudioArt(
                        uri = coverTracks[page].contentUri,
                        placeholderBackground = placeholderBackground,
                        placeholderTint = placeholderTint,
                        modifier = Modifier.fillMaxSize(),
                        shape = shape,
                        iconSize = (side.value * 0.25f).dp
                    )
                    // Paused: dim the art a little (the big Play button already says it's paused).
                    val paused = isCurrent && !isPlaying
                    val dim by animateFloatAsState(
                        targetValue = if (paused) 0.3f else 0f,
                        animationSpec = tween(300),
                        label = "artPausedDim"
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(Color.Black.copy(alpha = dim))
                    )
                    // Double-tap flash.
                    val icon = flashIcon
                    if (isCurrent && icon != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size((side.value * 0.26f).coerceIn(56f, 112f).dp)
                                .graphicsLayer {
                                    alpha = flashAlpha.value
                                    scaleX = flashScale.value
                                    scaleY = flashScale.value
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A quick, lightly bouncy spring for the flash pop-in. */
private val FlashPopSpring =
    androidx.compose.animation.core.spring<Float>(dampingRatio = 0.6f, stiffness = 800f)
