@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlaylistItem
import com.example.model.VideoHistoryEntry
import com.example.model.VideoItem
import com.example.model.formatDurationMs
import com.example.ui.icons.Symbols
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Video playlists
// ---------------------------------------------------------------------------------------------

/**
 * Video playlists: one "New playlist" button (official extended FAB). Each playlist is a card;
 * tap it to see its videos, ⋮ to add videos, rename or delete (deleting asks first).
 */
@Composable
internal fun VideoPlaylistsPage(
    playlists: List<PlaylistItem>,
    allVideos: List<VideoItem>,
    videoById: Map<String, VideoItem>,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (playlistId: String, newName: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onAddVideosToPlaylist: (playlistId: String, videoIds: List<String>) -> Unit,
    onRemoveVideoFromPlaylist: (playlistId: String, videoId: String) -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    var addTarget by remember { mutableStateOf<PlaylistItem?>(null) }

    SettingsPage(
        title = "Video playlists",
        subtitle = when (playlists.size) {
            0 -> null
            1 -> "1 playlist"
            else -> "${playlists.size} playlists"
        },
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New playlist") },
                icon = { Icon(Symbols.Add, contentDescription = null) },
                onClick = { showCreate = true },
                modifier = Modifier.testTag("me_create_playlist_btn")
            )
        },
        modifier = Modifier.testTag("me_subpage_video_playlist")
    ) {
        if (playlists.isEmpty()) {
            item(key = "empty") {
                SettingsEmptyState(
                    icon = Symbols.PlaylistPlay,
                    title = "No playlists yet",
                    body = "Tap New playlist to make one, then add videos to it."
                )
            }
        } else {
            items(playlists, key = { "pl_${it.id}" }) { playlist ->
                val videos = playlist.mediaUris.mapNotNull { videoById[it] }
                PlaylistCard(
                    playlist = playlist,
                    videos = videos,
                    expanded = expandedId == playlist.id,
                    onToggle = { expandedId = if (expandedId == playlist.id) null else playlist.id },
                    menuOpen = menuFor == playlist.id,
                    onOpenMenu = { menuFor = playlist.id },
                    onDismissMenu = { menuFor = null },
                    onAddVideos = { addTarget = playlist },
                    onRename = { renameTarget = playlist },
                    onDelete = { deleteTarget = playlist },
                    onPlayVideo = onPlayVideo,
                    onRemoveVideo = { video -> onRemoveVideoFromPlaylist(playlist.id, video.id) },
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = "Rename playlist",
            initial = target.name,
            confirmLabel = "Save",
            onConfirm = { name ->
                onRenamePlaylist(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Symbols.DeleteOutline, contentDescription = null) },
            title = { Text("Delete “${target.name}”?") },
            text = { Text("The playlist goes away. The videos stay on your phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(target.id)
                        if (expandedId == target.id) expandedId = null
                        deleteTarget = null
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("confirm_delete_playlist")
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
            }
        )
    }
    addTarget?.let { target ->
        AddVideosSheet(
            playlistName = target.name,
            videos = allVideos.filter { it.id !in target.mediaUris },
            hasAnyVideos = allVideos.isNotEmpty(),
            onAdd = { ids ->
                if (ids.isNotEmpty()) onAddVideosToPlaylist(target.id, ids)
                addTarget = null
            },
            onDismiss = { addTarget = null }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistItem,
    videos: List<VideoItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    menuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onAddVideos: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onRemoveVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val chevronTurn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "playlistChevron"
    )
    Card(
        onClick = onToggle,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .testTag("me_playlist_card_${playlist.name}")
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val first = videos.firstOrNull()
            if (first != null) {
                VideoThumbnailBox(
                    video = first,
                    modifier = Modifier
                        .width(96.dp)
                        .height(54.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(54.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Symbols.PlaylistPlay, contentDescription = null, tint = scheme.onSurfaceVariant)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (videos.size == 1) "1 video" else "${videos.size} videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            Icon(
                Symbols.ExpandMore,
                contentDescription = if (expanded) "Hide videos" else "Show videos",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = chevronTurn }
            )
            Box {
                TooltipIconButton(
                    label = "More options",
                    icon = Symbols.MoreVert,
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag("me_playlist_more_${playlist.name}")
                )
                ExpressiveMenu(
                    expanded = menuOpen,
                    onDismiss = onDismissMenu,
                    groups = listOf(
                        listOf(
                            MenuEntry("Add videos", Symbols.Add, "pl_menu_add", onClick = onAddVideos),
                            MenuEntry("Rename", Symbols.Edit, "pl_menu_rename", onClick = onRename)
                        ),
                        listOf(
                            MenuEntry("Delete", Symbols.DeleteOutline, "pl_menu_delete", destructive = true, onClick = onDelete)
                        )
                    )
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec())
        ) {
            Column(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                if (videos.isEmpty()) {
                    Text(
                        "No videos yet. Use ⋮ › Add videos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                } else {
                    videos.forEachIndexed { index, video ->
                        SegmentedListItem(
                            onClick = { onPlayVideo(video) },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = videos.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = scheme.surfaceContainerHigh),
                            leadingContent = {
                                VideoThumbnailBox(
                                    video = video,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(45.dp)
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onRemoveVideo(video) },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(Symbols.RemoveCircleOutline, contentDescription = "Remove from playlist")
                                }
                            }
                        ) {
                            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** Name a new playlist or rename one. */
@Composable
private fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_field")
            )
        },
        confirmButton = {
            Button(
                onClick = { name.trim().takeIf { it.isNotEmpty() }?.let(onConfirm) },
                enabled = name.isNotBlank(),
                shapes = ButtonDefaults.shapes()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
        }
    )
}

/** Pick several videos to add to a playlist, then Add. */
@Composable
private fun AddVideosSheet(
    playlistName: String,
    videos: List<VideoItem>,
    hasAnyVideos: Boolean,
    onAdd: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var picked by remember { mutableStateOf(emptySet<String>()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("add_videos_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add videos", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "to $playlistName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = { onAdd(videos.filter { it.id in picked }.map { it.id }) },
                    enabled = picked.isNotEmpty(),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("add_videos_confirm")
                ) { Text(if (picked.isEmpty()) "Add" else "Add ${picked.size}") }
            }
            if (videos.isEmpty()) {
                SettingsEmptyState(
                    icon = Symbols.VideoLibrary,
                    title = if (hasAnyVideos) "Everything's already in" else "No videos found",
                    body = if (hasAnyVideos) "All your videos are in this playlist." else "Videos on your phone show up here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                        val checked = video.id in picked
                        SegmentedListItem(
                            checked = checked,
                            onCheckedChange = { on -> picked = if (on) picked + video.id else picked - video.id },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = videos.size),
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = scheme.surfaceContainerHigh,
                                selectedContainerColor = scheme.secondaryContainer
                            ),
                            leadingContent = {
                                VideoThumbnailBox(
                                    video = video,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(45.dp)
                                )
                            },
                            trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                            supportingContent = { Text(video.durationLabel) }
                        ) {
                            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Watch history
// ---------------------------------------------------------------------------------------------

/**
 * Watch history, newest first, grouped by day (Today, Yesterday, then dates). Each video shows how
 * far you got. Swipe one away to remove it; the bin in the top bar clears everything (asks first).
 */
@Composable
internal fun WatchHistoryPage(
    historyPairs: List<Pair<VideoHistoryEntry, VideoItem>>,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onClearHistory: () -> Unit,
    onRemove: (videoId: String) -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    val days = remember(historyPairs) {
        historyPairs.groupBy { (entry, _) -> dayLabel(entry.lastPlayedTimestamp) }.toList()
    }
    SettingsPage(
        title = "History",
        subtitle = when (historyPairs.size) {
            0 -> null
            1 -> "1 video"
            else -> "${historyPairs.size} videos"
        },
        onBack = onBack,
        actions = {
            if (historyPairs.isNotEmpty()) {
                TooltipIconButton(
                    label = "Clear history",
                    icon = Symbols.DeleteSweep,
                    onClick = { confirmClear = true },
                    modifier = Modifier.testTag("clear_history_btn")
                )
            }
        },
        modifier = Modifier.testTag("me_subpage_video_history")
    ) {
        if (historyPairs.isEmpty()) {
            item(key = "empty") {
                SettingsEmptyState(
                    icon = Symbols.History,
                    title = "Nothing watched yet",
                    body = "Videos you play show up here, so you can carry on where you left off."
                )
            }
        } else {
            days.forEach { (day, pairs) ->
                item(key = "day_$day") {
                    SettingsSectionTitle(day, modifier = Modifier.animateItem())
                }
                itemsIndexed(pairs, key = { _, (entry, _) -> "hist_${entry.mediaUri}" }) { index, (entry, video) ->
                    HistoryRow(
                        entry = entry,
                        video = video,
                        index = index,
                        count = pairs.size,
                        onPlay = { onPlayVideo(video) },
                        onRemove = { onRemove(video.id) },
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = if (index < pairs.lastIndex) ListItemDefaults.SegmentedGap else 12.dp)
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Symbols.DeleteSweep, contentDescription = null) },
            title = { Text("Clear watch history?") },
            text = { Text("All ${historyPairs.size} videos leave the list, and where you stopped in them is forgotten.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        confirmClear = false
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("confirm_clear_history")
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: VideoHistoryEntry,
    video: VideoItem,
    index: Int,
    count: Int,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val swipeState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    SwipeToDismissBox(
        state = swipeState,
        onDismiss = {
            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
            onRemove()
            scope.launch { swipeState.reset() }
        },
        backgroundContent = {
            val toStart = swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.shape)
                    .background(
                        if (swipeState.dismissDirection == SwipeToDismissBoxValue.Settled) Color.Transparent
                        else scheme.errorContainer
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (toStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(Symbols.DeleteOutline, contentDescription = null, tint = scheme.onErrorContainer)
            }
        },
        modifier = modifier.testTag("history_item_${video.mediaId}")
    ) {
        SegmentedListItem(
            onClick = onPlay,
            shapes = shapes,
            colors = ListItemDefaults.segmentedColors(containerColor = scheme.surfaceContainer),
            leadingContent = {
                VideoThumbnailBox(
                    video = video,
                    watchedFraction = entry.progress,
                    modifier = Modifier
                        .width(112.dp)
                        .height(63.dp)
                )
            },
            supportingContent = { Text(historyDetail(entry), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        ) {
            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "1:35 pm · 12:40 left" or "1:35 pm · Watched". */
private fun historyDetail(entry: VideoHistoryEntry): String {
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.lastPlayedTimestamp))
    val left = (entry.durationMs - entry.positionMs).coerceAtLeast(0L)
    val where = if (entry.progress >= 0.97f || left < 5_000L) "Watched" else "${formatDurationMs(left)} left"
    return "$time · $where"
}

/** "Today", "Yesterday", "Mon, 20 Oct", or with the year when it isn't this year. */
private fun dayLabel(timestampMs: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val today = Calendar.getInstance()
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(day, today) -> "Today"
        sameDay(day, yesterday) -> "Yesterday"
        day.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(timestampMs))
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestampMs))
    }
}
