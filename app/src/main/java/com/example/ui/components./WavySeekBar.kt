@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Playback seek bar built on the official Material 3 [Slider]:
 * - the official thumb (tall narrow pill) that shrinks while pressed so the finger can see the position,
 * - a value bubble above the thumb while dragging (when [valueLabel] is given),
 * - a custom wavy active track that waves while playing and flattens smoothly when paused,
 * - a flat inactive track with the official end stop dot.
 *
 * Seeking happens once on release; [onSeekPreview] reports the position while dragging.
 */
@Composable
fun WavySeekBar(
    progress: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    valueLabel: ((Float) -> String)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme

    val activeColor = colorScheme.primary
    val inactiveColor = colorScheme.onSurfaceVariant.copy(alpha = 0.28f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isTouching = isPressed || isDragged

    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shownValue = (dragValue ?: progress).coerceIn(0f, 1f)

    // Wave height: full while playing, smoothly flat when paused or while the user drags.
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying && dragValue == null) 1f else 0f,
        animationSpec = motionScheme.slowEffectsSpec(),
        label = "seekWaveAmplitude"
    )

    // Phase moves only while there is a wave to show.
    var phase by remember { mutableFloatStateOf(0f) }
    val animateWave = isPlaying || amplitude > 0.01f
    LaunchedEffect(animateWave) {
        if (!animateWave) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    // About 0.6 waves per second: calm, like Android's media controls.
                    phase = (phase + dt * 2f * PI.toFloat() * 0.6f) % (2f * PI.toFloat())
                }
                last = now
            }
        }
    }

    val wavePath = remember { Path() }

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
        interactionSource = interactionSource,
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(
            thumbColor = activeColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor
        ),
        thumb = {
            Box(contentAlignment = Alignment.Center) {
                // Official Material 3 thumb: narrows while pressed/dragged.
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(thumbColor = activeColor),
                    thumbSize = DpSize(4.dp, 32.dp)
                )
                if (valueLabel != null && isTouching) {
                    // Value indicator above the finger. Takes no layout space so the
                    // slider's thumb measurement is unchanged.
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.inverseSurface,
                        contentColor = colorScheme.inverseOnSurface,
                        modifier = Modifier.layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints())
                            layout(0, 0) {
                                placeable.place(
                                    x = -placeable.width / 2,
                                    y = -placeable.height - 30.dp.roundToPx()
                                )
                            }
                        }
                    ) {
                        Text(
                            text = valueLabel(shownValue),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        track = { sliderState ->
            val fraction = sliderState.value.coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                val w = size.width
                val cy = size.height / 2f
                if (w <= 0f) return@Canvas

                val stroke = 5.dp.toPx()
                val halfStroke = stroke / 2f
                val gap = 7.dp.toPx() // clearance on each side of the thumb
                val thumbX = fraction * w

                // Inactive part: flat track after the thumb.
                val inactiveStart = (thumbX + gap + halfStroke).coerceAtMost(w)
                val inactiveEnd = w - halfStroke
                if (inactiveEnd > inactiveStart) {
                    drawLine(
                        color = inactiveColor,
                        start = Offset(inactiveStart, cy),
                        end = Offset(inactiveEnd, cy),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                // Official-style stop dot at the end of the track.
                drawCircle(
                    color = activeColor,
                    radius = 2.dp.toPx(),
                    center = Offset(w - halfStroke, cy)
                )

                // Active part: smooth sine wave up to just before the thumb.
                val activeStart = halfStroke
                val activeEnd = thumbX - gap - halfStroke
                if (activeEnd > activeStart) {
                    val amp = 3.dp.toPx() * amplitude
                    if (amp < 0.1f) {
                        drawLine(
                            color = activeColor,
                            start = Offset(activeStart, cy),
                            end = Offset(activeEnd, cy),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    } else {
                        val wavelength = 34.dp.toPx()
                        val k = (2.0 * PI / wavelength).toFloat()
                        val step = 1.dp.toPx().coerceAtLeast(1f)
                        // Ease the wave into the line near the thumb so it meets it cleanly.
                        val easeLen = wavelength / 2f
                        wavePath.reset()
                        var x = activeStart
                        var first = true
                        while (x <= activeEnd) {
                            val ease = ((activeEnd - x) / easeLen).coerceIn(0f, 1f)
                            val y = cy + sin(x * k - phase) * amp * ease
                            if (first) {
                                wavePath.moveTo(x, y)
                                first = false
                            } else {
                                wavePath.lineTo(x, y)
                            }
                            x += step
                        }
                        wavePath.lineTo(activeEnd, cy)
                        drawPath(
                            path = wavePath,
                            color = activeColor,
                            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
