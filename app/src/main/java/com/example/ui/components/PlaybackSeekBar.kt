@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * App-wide playback seek bar (music Now Playing, audio popup and video player), built only from
 * official Material 3 Expressive pieces — the same recipe Pixel Player and Echo Music use:
 *
 * - [Slider] handles touch, dragging and accessibility.
 * - Track: Google's [LinearWavyProgressIndicator]. It waves while playing and flattens on
 *   pause / while seeking using Google's own amplitude animation.
 * - Thumb: the official [SliderDefaults.Thumb]. A small round dot at rest that springs into a
 *   short vertical line while the user is touching it (Pixel Player style).
 *
 * Seeking happens once on release; [onSeekPreview] reports the position while dragging.
 */
@Composable
fun PlaybackSeekBar(
    progress: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isTouching = isPressed || isDragged

    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shownValue = (dragValue ?: progress).coerceIn(0f, 1f)
    val waving = isPlaying && dragValue == null && !isTouching

    // Dot (16 x 16) at rest -> slim line (4 x 24) while touching, with the theme's spring.
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val thumbWidth by animateDpAsState(
        targetValue = if (isTouching) 4.dp else 16.dp,
        animationSpec = spatialSpec,
        label = "seekThumbWidth"
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (isTouching) 24.dp else 16.dp,
        animationSpec = spatialSpec,
        label = "seekThumbHeight"
    )
    // The thumb's own press handling would halve its width instantly; give it a separate
    // source so only the animation above changes its size.
    val thumbVisualSource = remember { MutableInteractionSource() }

    val activeColor = WavyProgressIndicatorDefaults.indicatorColor
    val sliderColors = SliderDefaults.colors(thumbColor = activeColor)

    Slider(
        value = shownValue,
        onValueChange = { v ->
            dragValue = v
            onSeekPreview(v)
        },
        onValueChangeFinished = {
            val target = dragValue ?: shownValue
            dragValue = null
            onSeekFinished(target)
        },
        valueRange = 0f..1f,
        interactionSource = interactionSource,
        colors = sliderColors,
        thumb = {
            // Fixed slot so the track width (and thumb travel) never changes while it morphs.
            Box(
                modifier = Modifier.size(width = 16.dp, height = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                SliderDefaults.Thumb(
                    interactionSource = thumbVisualSource,
                    colors = sliderColors,
                    thumbSize = DpSize(thumbWidth, thumbHeight)
                )
            }
        },
        track = { sliderState ->
            LinearWavyProgressIndicator(
                progress = { sliderState.value },
                modifier = Modifier
                    .fillMaxWidth()
                    // The Slider already reports the position to accessibility services.
                    .clearAndSetSemantics { },
                color = activeColor,
                gapSize = 12.dp,
                amplitude = { p -> if (waving && p > 0f) 1f else 0f }
            )
        },
        modifier = modifier
    )
}
