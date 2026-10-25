@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrackItem
import com.example.ui.icons.Symbols
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * The play queue: the same rows as the Music list. The current song is tinted and has the playing
 * badge; songs that already played are dimmed. Drag the handle to reorder, swipe a song sideways
 * to remove it (official swipe-to-dismiss), tap one to play it.
 */
@Composable
fun QueueSheet(
    queue: List<AudioTrackItem>,
    currentIndex: Int,
    shuffleEnabled: Boolean,
    isPlaying: Boolean,
    onPlayItem: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Local copy so a drag or a swipe updates at once; it is re-synced from the player afterwards.
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
    // The same song twice in the queue still needs unique keys.
    val keys = remember(items.toList()) {
        val seen = HashMap<String, Int>()
        items.map { track ->
            val n = seen.getOrDefault(track.id, 0)
            seen[track.id] = n + 1
            "${track.id}#$n"
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0))
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
    fun removeAt(index: Int) {
        if (index in items.indices && items.size > 1) {
            items.removeAt(index)
            current = if (index < current) current - 1 else current.coerceAtMost(items.lastIndex)
            lastEditAt = System.currentTimeMillis()
            onRemove(index)
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
                Text(text = "Queue", style = MaterialTheme.typography.titleLarge)
                val count = if (items.size == 1) "1 song" else "${items.size} songs"
                Text(
                    text = buildString {
                        append(count)
                        if (shuffleEnabled) append(" · Shuffle is on")
                        if (items.size > 1) append(" · Swipe a song to remove it")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .testTag("queue_list"),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                itemsIndexed(items, key = { index, _ -> keys.getOrElse(index) { "q$index" } }) { index, track ->
                    ReorderableItem(reorderState, key = keys.getOrElse(index) { "q$index" }) { isDragging ->
                        val isCurrent = index == current
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 6.dp else 0.dp,
                            label = "queueRowElevation"
                        )
                        val threshold = SwipeToDismissBoxDefaults.positionalThreshold
                        val swipeState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
                        SwipeToDismissBox(
                            state = swipeState,
                            gesturesEnabled = items.size > 1 && !dragging,
                            onDismiss = {
                                removeAt(index)
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                // A reused row (same song queued twice) must not stay swiped away.
                                scope.launch { swipeState.reset() }
                            },
                            backgroundContent = {
                                val toStart = swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.large)
                                        .background(
                                            if (swipeState.dismissDirection == SwipeToDismissBoxValue.Settled) Color.Transparent
                                            else scheme.errorContainer
                                        )
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = if (toStart) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Icon(
                                        Symbols.PlaylistRemove,
                                        contentDescription = null,
                                        tint = scheme.onErrorContainer
                                    )
                                }
                            },
                            modifier = Modifier.testTag("queue_row_$index")
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = if (isDragging) scheme.surfaceContainerHighest else scheme.surfaceContainerLow,
                                shadowElevation = elevation
                            ) {
                                ListItem(
                                    selected = isCurrent,
                                    onClick = { onPlayItem(index) },
                                    leadingContent = {
                                        SongArt(
                                            track = track,
                                            playing = isCurrent,
                                            animatePlaying = isCurrent && isPlaying,
                                            selection = ThumbSelection.NONE
                                        )
                                    },
                                    supportingContent = {
                                        Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    },
                                    trailingContent = {
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
                                            Icon(
                                                Symbols.DragHandle,
                                                contentDescription = "Reorder",
                                                tint = scheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    shapes = ListItemDefaults.shapes(
                                        shape = MaterialTheme.shapes.large,
                                        selectedShape = MaterialTheme.shapes.large
                                    ),
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        contentColor = scheme.onSurface,
                                        supportingContentColor = scheme.onSurfaceVariant,
                                        selectedContainerColor = scheme.secondaryContainer,
                                        selectedContentColor = scheme.onSecondaryContainer,
                                        selectedSupportingContentColor = scheme.onSecondaryContainer
                                    ),
                                    contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(if (index < current) 0.6f else 1f)
                                ) {
                                    Text(
                                        track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
