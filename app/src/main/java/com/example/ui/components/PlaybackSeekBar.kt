package com.example.ui.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Seek bar for the Now Playing screen: the stock Material 3 Expressive [Slider] with its
 * default track (16 dp, gap around the handle, stop dot), default 44 dp handle and default
 * theme colors — nothing custom drawn.
 *
 * Seeking happens once on release; [onSeekPreview] reports the position while dragging.
 */
@Composable
fun PlaybackSeekBar(
    progress: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shownValue = (dragValue ?: progress).coerceIn(0f, 1f)

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
        modifier = modifier
    )
}
