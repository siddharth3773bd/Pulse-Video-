package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

/** How many screens are asking each window to stay awake right now. */
private val keepOnRequests = WeakHashMap<Window, Int>()

/**
 * Keeps the screen from turning off while this is shown and [enabled]. Several screens can ask at
 * once (Now Playing, ambient mode, the video player): the screen may sleep again only once none of
 * them is asking any more.
 */
@Composable
internal fun KeepScreenOn(enabled: Boolean = true) {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window, enabled) {
        if (window == null || !enabled) return@DisposableEffect onDispose {}
        keepOnRequests[window] = (keepOnRequests[window] ?: 0) + 1
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            val left = (keepOnRequests[window] ?: 1) - 1
            if (left <= 0) {
                keepOnRequests.remove(window)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                keepOnRequests[window] = left
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
