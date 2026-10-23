package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavySeekBar(
    progress: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val motionScheme = MaterialTheme.motionScheme
    val colorScheme = MaterialTheme.colorScheme

    val playedColor = colorScheme.primary
    val trackColor = colorScheme.outline.copy(alpha = 0.38f)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }

    val currentOnSeekPreview by rememberUpdatedState(onSeekPreview)
    val currentOnSeekFinished by rememberUpdatedState(onSeekFinished)

    val activeProgress = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    // Smoothly animate wave amplitude between 1f (playing) and 0f (paused/stopped)
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "wavySeekBarAmplitude"
    )

    // Tall, narrow vertical pill handle: 4.dp wide at rest, 2.5.dp wide while pressed/dragging
    val handleWidthDp by animateDpAsState(
        targetValue = if (isDragging) 2.5.dp else 4.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "wavySeekBarHandleWidth"
    )

    // Continuous phase shift in radians; advances only while playing or flattening out, freezes when flat
    var phaseRadians by remember { mutableFloatStateOf(0f) }
    val shouldAnimateWave = isPlaying || animatedAmplitude > 0.005f

    LaunchedEffect(shouldAnimateWave) {
        if (!shouldAnimateWave) return@LaunchedEffect
        var lastFrameNanos = -1L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos >= 0L) {
                    val deltaSec = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                    // Gentle, natural Android 16 / Echo Music wave drift (~1.15 cycles per second)
                    val angularVelocity = (2f * PI.toFloat() * 1.15f)
                    phaseRadians = (phaseRadians + deltaSec * angularVelocity) % (2f * PI.toFloat())
                }
                lastFrameNanos = frameTimeNanos
            }
        }
    }

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(activeProgress, 0f..1f)
                setProgress { targetValue ->
                    val clamped = targetValue.coerceIn(0f, 1f)
                    currentOnSeekPreview(clamped)
                    currentOnSeekFinished(clamped)
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val width = size.width.coerceAtLeast(1).toFloat()
                    var currentFraction = (down.position.x / width).coerceIn(0f, 1f)

                    isDragging = true
                    dragProgress = currentFraction
                    currentOnSeekPreview(currentFraction)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            break
                        }
                        change.consume()
                        currentFraction = (change.position.x / width).coerceIn(0f, 1f)
                        dragProgress = currentFraction
                        currentOnSeekPreview(currentFraction)
                    }

                    isDragging = false
                    currentOnSeekFinished(currentFraction)
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val centerY = canvasHeight / 2f
        val strokeWidthPx = 4.dp.toPx()
        val halfStrokePx = strokeWidthPx / 2f

        val wavelengthPx = 22.dp.toPx()
        val maxWaveAmplitudePx = 3.5.dp.toPx()
        val currentAmplitudePx = maxWaveAmplitudePx * animatedAmplitude

        val handleWidthPx = handleWidthDp.toPx()
        val halfHandleWidthPx = handleWidthPx / 2f
        val handleHeightPx = 28.dp.toPx()
        val gapPx = 5.dp.toPx()

        // Position of the vertical pill handle along the bar
        val minHandleCenterX = halfHandleWidthPx
        val maxHandleCenterX = (canvasWidth - halfHandleWidthPx).coerceAtLeast(minHandleCenterX)
        val handleCenterX = minHandleCenterX + activeProgress * (maxHandleCenterX - minHandleCenterX)

        // 1. Played part (left of the handle minus the small gap)
        val waveStartX = halfStrokePx
        val waveEndX = handleCenterX - halfHandleWidthPx - gapPx - halfStrokePx

        if (waveEndX > waveStartX) {
            if (currentAmplitudePx <= 0.05f) {
                // Perfectly flat straight line when paused or stopped
                drawLine(
                    color = playedColor,
                    start = Offset(waveStartX, centerY),
                    end = Offset(waveEndX, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            } else {
                wavePath.reset()
                val stepPx = 1.5.dp.toPx().coerceAtLeast(1f)
                val taperPx = 6.dp.toPx()
                val segmentLength = waveEndX - waveStartX
                val twoPiOverWavelength = (2.0 * PI / wavelengthPx.toDouble()).toFloat()

                var x = waveStartX
                var isFirstPoint = true
                while (x < waveEndX) {
                    val distFromStart = x - waveStartX
                    val distFromEnd = waveEndX - x
                    val startEnvelope = if (segmentLength > taperPx * 2f) {
                        (distFromStart / taperPx).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    val endEnvelope = (distFromEnd / taperPx).coerceIn(0f, 1f)
                    val envelope = startEnvelope * endEnvelope

                    val y = centerY + sin(x * twoPiOverWavelength - phaseRadians) * currentAmplitudePx * envelope
                    if (isFirstPoint) {
                        wavePath.moveTo(x, y)
                        isFirstPoint = false
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += stepPx
                }

                // Final endpoint at waveEndX (tapered smoothly to centerY)
                wavePath.lineTo(waveEndX, centerY)

                drawPath(
                    path = wavePath,
                    color = playedColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // 2. Unplayed part (flat rounded track to the right of the handle plus the small gap)
        val trackStartX = handleCenterX + halfHandleWidthPx + gapPx + halfStrokePx
        val trackEndX = canvasWidth - halfStrokePx
        if (trackEndX > trackStartX) {
            drawLine(
                color = trackColor,
                start = Offset(trackStartX, centerY),
                end = Offset(trackEndX, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // 3. Tall, narrow, fully rounded vertical pill handle in front
        drawRoundRect(
            color = playedColor,
            topLeft = Offset(
                x = handleCenterX - halfHandleWidthPx,
                y = centerY - handleHeightPx / 2f
            ),
            size = Size(width = handleWidthPx, height = handleHeightPx),
            cornerRadius = CornerRadius(halfHandleWidthPx, halfHandleWidthPx)
        )
    }
}
