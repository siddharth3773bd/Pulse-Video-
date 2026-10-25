@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrackItem
import com.example.ui.theme.LocalM3Palette
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * The play queue: tap a song to play it, drag the handle to reorder, ✕ to remove.
 * Songs that already played are dimmed; the current one is highlighted.
 */
@Composable
fun QueueSheet(
    queue: List<AudioTrackItem>,
    currentIndex: Int,
    shuffleEnabled: Boolean,
    onPlayItem: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val haptic = LocalHapticFeedback.current

    // Local copy so a drag reorders instantly; it is re-synced from the player afterwards.
    val items = remember { mutableStateListOf<AudioTrackItem>().apply { addAll(queue) } }
    var current by remember { mutableIntStateOf(currentIndex) }
    var dragging by remember { mutableStateOf(false) }
    var lastEditAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(queue, currentIndex) {
        val same = queue.size == items.size && queue.indices.all { queue[it].id == items[it].id }
        if (same) {
            current = currentIndex
            return@LaunchedEffect
        }
        // Give our own edit time to reach the player before treating the difference as external.
        val wait = 900L - (System.currentTimeMillis() - lastEditAt)
        if (wait > 0L) delay(wait)
        if (!dragging) {
            items.clear()
            items.addAll(queue)
            current = currentIndex
        }
    }

    // Same song twice in the queue still needs unique keys.
    val keys = remember(items.toList()) {
        val seen = HashMap<String, Int>()
        items.map { track ->
            val n = seen.getOrDefault(track.id, 0)
            seen[track.id] = n + 1
            "${track.id}#$n"
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0)
    )
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val a = from.index
        val b = to.index
        if (a in items.indices && b in items.indices && a != b) {
            items.add(b, items.removeAt(a))
            current = when {
                a == current -> b
                a < current && b >= current -> current - 1
                a > current && b <= current -> current + 1
                else -> current
            }
            lastEditAt = System.currentTimeMillis()
            onMove(a, b)
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = !dragging,
        modifier = Modifier.testTag("queue_sheet")
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.92f)) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge
                )
                val count = if (items.size == 1) "1 song" else "${items.size} songs"
                Text(
                    text = if (shuffleEnabled) "$count · Shuffle is on, so songs play in a mixed order"
                    else count,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .testTag("queue_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(items, key = { index, _ -> keys.getOrElse(index) { "q$index" } }) { index, track ->
                    ReorderableItem(reorderState, key = keys.getOrElse(index) { "q$index" }) { isDragging ->
                        val isCurrent = index == current
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "queueRowElevation"
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            shadowElevation = elevation,
                            onClick = { onPlayItem(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (index < current) 0.6f else 1f)
                                .testTag("queue_row_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    AudioArt(
                                        uri = track.contentUri,
                                        placeholderBackground = palette.surfaceContainerHighest,
                                        placeholderTint = palette.primary,
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Symbols.BarChart, contentDescription = "Playing")
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LocalContentColorVariant(isCurrent),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index in items.indices && items.size > 1) {
                                            items.removeAt(index)
                                            current = when {
                                                index < current -> current - 1
                                                else -> current.coerceAtMost(items.lastIndex)
                                            }
                                            lastEditAt = System.currentTimeMillis()
                                            onRemove(index)
                                        }
                                    },
                                    enabled = items.size > 1,
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier.testTag("queue_remove_$index")
                                ) {
                                    Icon(Symbols.Close, contentDescription = "Remove from queue")
                                }
                                IconButton(
                                    onClick = {},
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .draggableHandle(
                                            onDragStarted = {
                                                dragging = true
                                                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                            },
                                            onDragStopped = {
                                                dragging = false
                                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            }
                                        )
                                        .testTag("queue_handle_$index")
                                ) {
                                    Icon(Symbols.DragHandle, contentDescription = "Reorder")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalContentColorVariant(isCurrent: Boolean) =
    if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurfaceVariant
