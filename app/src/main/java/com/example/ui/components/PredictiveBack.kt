package com.example.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Android's predictive back for screens that sit on top of another one (Now Playing, Lyrics):
// while the back gesture is held the screen shrinks to 90%, moves a little with the finger and
// rounds its corners (Google's predictive back design guidance). Let go and the screen leaves;
// change your mind and it springs back.
// ---------------------------------------------------------------------------------------------

/** Where a held back gesture is. Read it while drawing (in a graphics layer), never to compose. */
@Stable
class PredictiveBackState internal constructor() {
    internal val raw = Animatable(0f)

    /** 0 = no gesture, 1 = fully pulled; eased with Android's predictive back curve. */
    val progress: Float get() = PredictiveBackEasing.transform(raw.value)

    /** True when the gesture started at the left edge of the screen. */
    var fromLeftEdge by mutableStateOf(true)
        internal set

    /** How far the finger moved up (negative) or down (positive) since the gesture started, in px. */
    var verticalPull by mutableFloatStateOf(0f)
        internal set

    /** Back to no preview (for a screen that stays and is shown again later). */
    suspend fun reset() {
        raw.snapTo(0f)
        verticalPull = 0f
    }
}

/**
 * Handles back with Android's predictive back gesture. [onBack] runs when the gesture is finished
 * (or on a plain back press). The returned state drives the preview, see [predictiveBackPreview].
 * [settleAfterBack]: after letting go the preview eases back to full size while the screen's own
 * closing animation takes over (for a screen that flies into something, like the video player
 * into its thumbnail).
 */
@Composable
fun rememberPredictiveBack(
    enabled: Boolean = true,
    settleAfterBack: Boolean = false,
    onBack: () -> Unit
): PredictiveBackState {
    val state = remember { PredictiveBackState() }
    val scope = rememberCoroutineScope()
    val onBackLatest by rememberUpdatedState(onBack)
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    PredictiveBackHandler(enabled = enabled) { events ->
        var startY = Float.NaN
        try {
            events.collect { event ->
                if (startY.isNaN()) startY = event.touchY
                state.fromLeftEdge = event.swipeEdge != BackEventCompat.EDGE_RIGHT
                state.verticalPull = event.touchY - startY
                state.raw.snapTo(event.progress)
            }
            // The screen leaves from where the gesture left it; nothing jumps back first.
            onBackLatest()
            if (settleAfterBack) scope.launch { state.raw.animateTo(0f, settleSpec) }
        } catch (cancelled: CancellationException) {
            scope.launch { state.raw.animateTo(0f, settleSpec) }
            throw cancelled
        }
    }
    return state
}

/** The predictive back preview: shrink to 90%, follow the finger a little, round the corners. */
fun Modifier.predictiveBackPreview(state: PredictiveBackState, cornerRadius: Dp = 32.dp): Modifier =
    graphicsLayer { applyPredictiveBack(state, cornerRadius) }

/**
 * Applies the preview inside a graphics layer. [extraScale], [extraTranslationY] and
 * [extraCornerFraction] let a screen add its own movement (Now Playing's swipe down).
 */
internal fun GraphicsLayerScope.applyPredictiveBack(
    state: PredictiveBackState,
    cornerRadius: Dp,
    extraScale: Float = 1f,
    extraTranslationY: Float = 0f,
    extraCornerFraction: Float = 0f
) {
    val back = state.progress
    val scale = (1f - 0.1f * back) * extraScale
    scaleX = scale
    scaleY = scale
    // Google's values: at most (width / 20 - 8 dp) sideways and (height / 20 - 8 dp) up or down.
    val margin = 8.dp.toPx()
    val maxShiftX = (size.width / 20f - margin).coerceAtLeast(0f)
    val maxShiftY = (size.height / 20f - margin).coerceAtLeast(0f)
    translationX = (if (state.fromLeftEdge) 1f else -1f) * maxShiftX * back
    translationY = extraTranslationY +
        (state.verticalPull / (size.height / 2f)).coerceIn(-1f, 1f) * maxShiftY * back
    val corner = cornerRadius.toPx() * maxOf(back, extraCornerFraction.coerceIn(0f, 1f))
    if (corner > 0f) {
        shape = RoundedCornerShape(corner)
        clip = true
    } else {
        clip = false
    }
}
