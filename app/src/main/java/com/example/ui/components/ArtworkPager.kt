package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    testTag: String = "now_playing_cover"
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

        // The song changed some other way (buttons, auto-advance): glide the art along.
        LaunchedEffect(coverIndex, coverTracks.size) {
            if (pagerState.currentPage != coverIndex && coverIndex < coverTracks.size) {
                pagerState.animateScrollToPage(coverIndex)
            }
        }
        // The user swiped to another cover: play that song.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                if (hasQueue && page != coverIndexLatest) skipLatest(page)
            }
        }

        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { doubleTap() }) }
                    .testTag(testTag),
                contentAlignment = Alignment.Center
            ) {
                val isCurrent = page == coverIndex
                Box(modifier = Modifier.size(side)) {
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
