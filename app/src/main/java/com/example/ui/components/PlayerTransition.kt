package com.example.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
// Mini player ⇄ Now Playing: Material's "container transform" motion pattern, built with Compose's
// official shared element transitions. The pill grows into the full screen (and shrinks back into
// it), its small cover flies to the big cover, and the content cross-fades inside. Movement uses
// the Material 3 Expressive spatial spring, fades the effects springs.
// ---------------------------------------------------------------------------------------------

/** What the shared modifiers need: the shared transition and the side (pill or full screen). */
class PlayerTransitionScopes(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope
)

/** Set around the mini player and around Now Playing; null everywhere else (no transition). */
val LocalPlayerTransition = compositionLocalOf<PlayerTransitionScopes?> { null }

private const val PlayerContainerKey = "player_container"
private const val PlayerCoverKey = "player_cover"

/** Height of the mini player pill; its corners are half of it. */
internal val MiniPlayerHeight = 64.dp

/**
 * Android's predictive back curve for in-app surfaces (decelerate: quick at first, then easing
 * in), as given in Google's predictive back design guidance.
 */
internal val PredictiveBackEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

/**
 * The container that morphs between the pill and the full screen. Put it first in the modifier
 * chain of the pill and of Now Playing (size modifiers after it).
 */
@Composable
fun Modifier.playerContainer(): Modifier {
    val scopes = LocalPlayerTransition.current ?: return this
    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<Rect>()
    val fadeInSpec = motion.slowEffectsSpec<Float>()
    val fadeOutSpec = motion.fastEffectsSpec<Float>()
    val density = LocalDensity.current
    val screenHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    val shape = remember(density, screenHeight) {
        with(density) {
            PlayerMorphShape(
                pillHeightPx = MiniPlayerHeight.toPx(),
                fullHeightPx = screenHeight,
                cornerPx = (MiniPlayerHeight / 2).toPx()
            )
        }
    }
    return with(scopes.shared) {
        this@playerContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(PlayerContainerKey),
            animatedVisibilityScope = scopes.visibility,
            enter = fadeIn(fadeInSpec),
            exit = fadeOut(fadeOutSpec),
            boundsTransform = BoundsTransform { _, _ -> spatial },
            // Scale (don't re-lay out) the content; the bottom stays put, where the pill is.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.BottomCenter),
            clipInOverlayDuringTransition = OverlayClip(shape)
        )
    }
}

/** The cover: the pill's small one and Now Playing's big one are the same element. */
@Composable
fun Modifier.playerCover(): Modifier {
    val scopes = LocalPlayerTransition.current ?: return this
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    return with(scopes.shared) {
        this@playerCover.sharedElement(
            sharedContentState = rememberSharedContentState(PlayerCoverKey),
            animatedVisibilityScope = scopes.visibility,
            boundsTransform = BoundsTransform { _, _ -> spatial },
            zIndexInOverlay = 1f
        )
    }
}

/**
 * Round like the pill while small, square at full screen. The corners follow the container's
 * height, so they change smoothly with the size and need no animation of their own.
 */
private class PlayerMorphShape(
    private val pillHeightPx: Float,
    private val fullHeightPx: Float,
    private val cornerPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val range = (fullHeightPx - pillHeightPx).coerceAtLeast(1f)
        val grown = ((size.height - pillHeightPx) / range).coerceIn(0f, 1f)
        // Stays round most of the way, then squares off as it reaches the screen edges.
        val radius = (cornerPx * (1f - grown * grown)).coerceAtMost(min(size.width, size.height) / 2f)
        return Outline.Rounded(RoundRect(rect = size.toRect(), cornerRadius = CornerRadius(radius)))
    }
}

// ---------------------------------------------------------------------------------------------
// Video thumbnail ⇄ video player: the same container transform. The player grows out of the
// thumbnail you tapped, and when it closes the live picture shrinks back into that thumbnail.
// ---------------------------------------------------------------------------------------------

/** The shared transition for videos, and which video is open in the player (null = none). */
class VideoTransitionScope(val shared: SharedTransitionScope, val openVideoId: String?)

/** Set around the Videos list and grid; null elsewhere (those thumbnails don't take part). */
val LocalVideoTransition = compositionLocalOf<VideoTransitionScope?> { null }

private fun videoTransitionKey(videoId: String) = "video_player_$videoId"

/** Corners of the moving picture: the thumbnail's own. */
private val VideoMorphShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

/** The video player's side of the transform. Put it first in the player's modifier chain. */
@Composable
fun Modifier.videoPlayerContainer(
    shared: SharedTransitionScope,
    visibility: AnimatedVisibilityScope,
    /** The video whose thumbnail it's linked to; null while just playing (linked to none). */
    videoId: String?
): Modifier {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    return with(shared) {
        this@videoPlayerContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(
                if (videoId != null) videoTransitionKey(videoId) else "video_player_unlinked"
            ),
            animatedVisibilityScope = visibility,
            // The fades come from the player's own enter/exit animation.
            enter = androidx.compose.animation.EnterTransition.None,
            exit = androidx.compose.animation.ExitTransition.None,
            boundsTransform = BoundsTransform { _, _ -> spatial },
            // The picture is scaled into the thumbnail's frame (centre crop), not re-laid out.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.Center),
            clipInOverlayDuringTransition = OverlayClip(VideoMorphShape)
        )
    }
}

/**
 * A thumbnail's side of the transform. While its video is open in the player the thumbnail steps
 * aside (the player covers it anyway); when the player closes, the picture lands here.
 */
@Composable
internal fun VideoThumbnailTransition(videoId: String, content: @Composable (Modifier) -> Unit) {
    val scope = LocalVideoTransition.current
    if (scope == null) {
        content(Modifier)
        return
    }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    androidx.compose.animation.AnimatedVisibility(
        visible = scope.openVideoId != videoId,
        // Comes back at the end of the flight, as the moving picture fades into it.
        enter = fadeIn(androidx.compose.animation.core.tween(durationMillis = 140, delayMillis = 260)),
        exit = fadeOut(androidx.compose.animation.core.tween(durationMillis = 120))
    ) {
        with(scope.shared) {
            content(
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(videoTransitionKey(videoId)),
                    animatedVisibilityScope = this@AnimatedVisibility,
                    enter = androidx.compose.animation.EnterTransition.None,
                    exit = androidx.compose.animation.ExitTransition.None,
                    boundsTransform = BoundsTransform { _, _ -> spatial },
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.Center),
                    clipInOverlayDuringTransition = OverlayClip(VideoMorphShape)
                )
            )
        }
    }
}
