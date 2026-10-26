package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The window's size, sorted by Google's window size classes (the breakpoints Material 3 uses to
 * adapt layouts): width under 600 dp is compact (a phone held upright), height under 480 dp is
 * compact (a phone held sideways).
 */
@Immutable
internal data class WindowLayout(val width: Dp, val height: Dp) {
    val compactWidth: Boolean get() = width < 600.dp
    val compactHeight: Boolean get() = height < 480.dp

    /**
     * A navigation rail on the side instead of the bar at the bottom: on wider screens, and on a
     * phone held sideways, where the bar would take a big slice of the little height there is.
     */
    val useRail: Boolean get() = !compactWidth || compactHeight

    /** Wide enough for two panes side by side (sideways phones, tablets, unfolded foldables). */
    val twoPane: Boolean get() = !compactWidth && width > height * 1.2f || width >= 840.dp
}

/** The current window's [WindowLayout]; changes when the phone turns or the window is resized. */
@Composable
internal fun currentWindowLayout(): WindowLayout {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { WindowLayout(size.width.toDp(), size.height.toDp()) }
}
