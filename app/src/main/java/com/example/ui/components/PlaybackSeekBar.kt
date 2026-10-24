/*
 * The squiggly drawing below (SquigglyTrack) is copied from Echo Music's "Squiggly" seek bar:
 *   https://github.com/EchoMusicApp/Echo-Music  —  ui/component/SquigglySlider.kt  (GPL-3.0)
 * which itself is a Compose port of Android's SystemUI media seek bar:
 *   AOSP frameworks/base  —  media/controls/ui/drawable/SquigglyProgress.kt  (Apache-2.0)
 *   Copyright (C) 2022 The Android Open Source Project
 * Personal use only.
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * App-wide playback seek bar (music Now Playing, audio popup and video player).
 *
 * Look and motion: Echo Music's "Squiggly" style (Android's own media-player seek bar) — a wavy
 * played part that flattens on pause / while seeking, a flat unplayed part and a vertical pill
 * handle, all drawn by [SquigglyTrack].
 *
 * Touch and accessibility: the official Material 3 [Slider] with an invisible thumb (the pill is
 * part of the drawing), the same way Echo Music hosts its own "Slim" style.
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

    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shownValue = (dragValue ?: progress).coerceIn(0f, 1f)
    val isDragging = isPressed || isDragged || dragValue != null

    val primaryColor = MaterialTheme.colorScheme.primary

    // New Material 3 Slider API: the slider reads its position from a SliderState.
    val sliderState = remember { SliderState(value = shownValue) }
    sliderState.value = shownValue

    Slider(
        state = sliderState,
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
        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
        track = { sliderState ->
            SquigglyTrack(
                progress = sliderState.value.coerceIn(0f, 1f),
                isPlaying = isPlaying,
                isDragging = isDragging,
                primaryColor = primaryColor
            )
        },
        modifier = modifier
    )
}

/** Echo Music's SquigglySlider drawing, unchanged except that progress comes in as 0..1. */
@Composable
private fun SquigglyTrack(
    progress: Float,
    isPlaying: Boolean,
    isDragging: Boolean,
    primaryColor: Color
) {
    var phaseOffset by remember { mutableFloatStateOf(0f) }
    var heightFraction by remember { mutableFloatStateOf(if (isPlaying) 1f else 0f) }

    val scope = rememberCoroutineScope()

    val waveLength = 80f
    val lineAmplitude = 6f
    val phaseSpeed = 24f
    val transitionPeriods = 1.5f
    val minWaveEndpoint = 0f
    val matchedWaveEndpoint = 1f
    val transitionEnabled = true

    LaunchedEffect(isPlaying, isDragging) {
        scope.launch {
            val shouldFlatten = !isPlaying || isDragging
            val targetHeight = if (shouldFlatten) 0f else 1f
            val animDuration = if (shouldFlatten) 150 else 200
            val startDelay = if (shouldFlatten) 0L else 30L

            delay(startDelay)

            val animator = Animatable(heightFraction)
            animator.animateTo(
                targetValue = targetHeight,
                animationSpec = tween(
                    durationMillis = animDuration,
                    easing = LinearEasing,
                ),
            ) {
                heightFraction = this.value
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        var lastFrameTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { frameTimeMillis ->
                val deltaTime = (frameTimeMillis - lastFrameTime) / 1000f
                phaseOffset += deltaTime * phaseSpeed
                phaseOffset %= waveLength
                lastFrameTime = frameTimeMillis
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        val strokeWidth = 5.dp.toPx()
        val totalWidth = size.width
        val totalProgressPx = totalWidth * progress
        val centerY = size.height / 2f

        val waveProgressPx =
            if (!transitionEnabled || progress > matchedWaveEndpoint) {
                totalWidth * progress
            } else {
                val t = (progress / matchedWaveEndpoint).coerceIn(0f, 1f)
                totalWidth * (minWaveEndpoint + (matchedWaveEndpoint - minWaveEndpoint) * t)
            }

        fun computeAmplitude(x: Float, sign: Float): Float {
            return if (transitionEnabled) {
                val length = transitionPeriods * waveLength
                val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                sign * heightFraction * lineAmplitude * coeff
            } else {
                sign * heightFraction * lineAmplitude
            }
        }

        val path = Path()
        val waveStart = -phaseOffset - waveLength / 2f
        val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

        path.moveTo(waveStart, centerY)

        var currentX = waveStart
        var waveSign = 1f
        var currentAmp = computeAmplitude(currentX, waveSign)
        val dist = waveLength / 2f

        while (currentX < waveEnd) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2f
            val nextAmp = computeAmplitude(nextX, waveSign)

            path.cubicTo(
                midX,
                centerY + currentAmp,
                midX,
                centerY + nextAmp,
                nextX,
                centerY + nextAmp,
            )

            currentAmp = nextAmp
            currentX = nextX
        }

        val clipTop = lineAmplitude + strokeWidth

        val disabledAlpha = 77f / 255f
        val inactiveTrackColor = primaryColor.copy(alpha = disabledAlpha)
        val capRadius = strokeWidth / 2f

        fun drawPathSegment(startX: Float, endX: Float, color: Color) {
            if (endX <= startX) return
            clipRect(
                left = startX,
                top = centerY - clipTop,
                right = endX,
                bottom = centerY + clipTop,
            ) {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        drawPathSegment(0f, totalProgressPx, primaryColor)

        drawPathSegment(totalProgressPx, totalWidth, inactiveTrackColor)

        fun getWaveY(x: Float): Float {
            val phase = (x - waveStart) / waveLength
            val waveCycle = phase - kotlin.math.floor(phase)
            val waveValue = kotlin.math.cos(waveCycle * 2f * kotlin.math.PI.toFloat())

            val ampCoeff =
                if (transitionEnabled) {
                    val length = transitionPeriods * waveLength
                    ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                } else {
                    1f
                }

            return centerY + waveValue * lineAmplitude * heightFraction * ampCoeff
        }

        drawCircle(
            color = primaryColor,
            radius = capRadius,
            center = Offset(0f, getWaveY(0f)),
        )

        val endWaveY = getWaveY(totalWidth)
        clipRect(
            left = totalWidth,
            top = centerY - clipTop,
            right = totalWidth + capRadius,
            bottom = centerY + clipTop,
        ) {
            drawCircle(
                color = inactiveTrackColor,
                radius = capRadius,
                center = Offset(totalWidth, endWaveY),
            )
        }

        val barHalfHeight = (lineAmplitude + strokeWidth)
        val barWidth = 5.dp.toPx()

        if (barHalfHeight > 0.5f) {
            drawLine(
                color = primaryColor,
                start = Offset(totalProgressPx, centerY - barHalfHeight),
                end = Offset(totalProgressPx, centerY + barHalfHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
