@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioPlaylist
import com.example.model.LibrarySongSort

// ---------------------------------------------------------------------------------------------
// Grouped menu (Material 3 Expressive: DropdownMenuPopup + DropdownMenuGroup)
// ---------------------------------------------------------------------------------------------

/** One action in a grouped menu. */
data class AudioMenuEntry(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Official Expressive grouped menu: each group is a [DropdownMenuGroup] with position-aware
 * shapes, separated by [MenuDefaults.GroupSpacing]. Picking an item closes the menu first.
 */
@Composable
fun AudioGroupedMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    groups: List<List<AudioMenuEntry>>,
    modifier: Modifier = Modifier
) {
    val visibleGroups = groups.filter { it.isNotEmpty() }
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        visibleGroups.forEachIndexed { groupIndex, group ->
            if (groupIndex > 0) Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = groupIndex, count = visibleGroups.size)) {
                group.forEachIndexed { index, entry ->
                    val error = MaterialTheme.colorScheme.error
                    DropdownMenuItem(
                        onClick = {
                            onDismiss()
                            entry.onClick()
                        },
                        text = { Text(entry.label) },
                        shape = MenuDefaults.itemShape(index = index, count = group.size).shape,
                        leadingIcon = { Icon(entry.icon, contentDescription = null) },
                        enabled = entry.enabled,
                        colors = if (entry.destructive) {
                            MenuDefaults.itemColors(textColor = error, leadingIconColor = error)
                        } else {
                            MenuDefaults.itemColors()
                        },
                        modifier = Modifier.testTag(entry.tag)
                    )
                }
            }
        }
    }
}

/** The song ⋮ menu: queue, playlist and library actions, then the actions that hide or delete. */
@Composable
fun SongActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    liked: Boolean,
    canGoToAlbum: Boolean,
    canGoToArtist: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLike: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onSongInfo: () -> Unit,
    onBlacklistSong: () -> Unit,
    onBlacklistFolder: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val groups = listOf(
        listOf(
            AudioMenuEntry("Play next", Icons.AutoMirrored.Filled.PlaylistPlay, "song_action_play_next", onClick = onPlayNext),
            AudioMenuEntry("Add to queue", Icons.AutoMirrored.Filled.QueueMusic, "song_action_add_queue", onClick = onAddToQueue),
            AudioMenuEntry("Add to playlist", Icons.AutoMirrored.Filled.PlaylistAdd, "song_action_add_playlist", onClick = onAddToPlaylist)
        ),
        listOfNotNull(
            AudioMenuEntry(
                if (liked) "Unlike" else "Like",
                if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "song_action_like",
                onClick = onToggleLike
            ),
            AudioMenuEntry("Go to album", Icons.Default.Album, "song_action_album", onClick = onGoToAlbum)
                .takeIf { canGoToAlbum },
            AudioMenuEntry("Go to artist", Icons.Default.Person, "song_action_artist", onClick = onGoToArtist)
                .takeIf { canGoToArtist },
            AudioMenuEntry("Share", Icons.Default.Share, "song_action_share", onClick = onShare),
            AudioMenuEntry("Song info", Icons.Default.Info, "song_action_info", onClick = onSongInfo)
        ),
        listOfNotNull(
            onRemoveFromPlaylist?.let {
                AudioMenuEntry("Remove from playlist", Icons.Default.PlaylistRemove, "song_action_remove_playlist", onClick = it)
            },
            AudioMenuEntry("Blacklist song", Icons.Default.Block, "song_action_blacklist", onClick = onBlacklistSong),
            AudioMenuEntry("Blacklist folder", Icons.Default.FolderOff, "song_action_blacklist_folder", onClick = onBlacklistFolder),
            AudioMenuEntry("Delete from device", Icons.Default.DeleteOutline, "song_action_delete", destructive = true, onClick = onDelete)
        )
    )
    AudioGroupedMenu(expanded = expanded, onDismiss = onDismiss, groups = groups)
}

/** A playlist's ⋮ menu. */
@Composable
fun PlaylistActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    AudioGroupedMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        groups = listOf(
            listOf(
                AudioMenuEntry("Play next", Icons.AutoMirrored.Filled.PlaylistPlay, "playlist_action_play_next", onClick = onPlayNext),
                AudioMenuEntry("Add to queue", Icons.AutoMirrored.Filled.QueueMusic, "playlist_action_add_queue", onClick = onAddToQueue)
            ),
            listOf(
                AudioMenuEntry("Rename", Icons.Default.Edit, "playlist_action_rename", onClick = onRename),
                AudioMenuEntry("Delete playlist", Icons.Default.DeleteOutline, "playlist_action_delete", destructive = true, onClick = onDelete)
            )
        )
    )
}

// ---------------------------------------------------------------------------------------------
// Sort menu
// ---------------------------------------------------------------------------------------------

private val LibrarySongSort.icon: ImageVector
    get() = when (this) {
        LibrarySongSort.DEFAULT -> Icons.AutoMirrored.Filled.Sort
        LibrarySongSort.TITLE -> Icons.Default.SortByAlpha
        LibrarySongSort.ARTIST -> Icons.Default.Person
        LibrarySongSort.ALBUM -> Icons.Default.Album
        LibrarySongSort.DATE_ADDED -> Icons.Default.CalendarToday
        LibrarySongSort.DURATION -> Icons.Default.Timer
    }

/**
 * Expressive grouped menu for sorting. "Sort by" (only for songs) and "Order" are separate groups;
 * the current choice uses the selectable item's selected state with a check mark.
 * [field] is null for albums and artists, which only sort A to Z / Z to A.
 */
@Composable
fun LibrarySortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    field: LibrarySongSort?,
    ascending: Boolean,
    onSelectField: (LibrarySongSort) -> Unit,
    onSelectAscending: (Boolean) -> Unit
) {
    val groupCount = if (field != null) 2 else 1
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("library_sort_menu")
    ) {
        if (field != null) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = groupCount)) {
                MenuDefaults.DropdownMenuGroupLabel { Text("Sort by") }
                val fields = LibrarySongSort.entries
                fields.forEachIndexed { index, option ->
                    SelectableDropdownMenuItem(
                        selected = option == field,
                        onClick = {
                            onDismiss()
                            if (option != field) onSelectField(option)
                        },
                        text = { Text(option.label) },
                        shapes = MenuDefaults.itemShape(index = index, count = fields.size),
                        leadingIcon = { Icon(option.icon, contentDescription = null) },
                        selectedLeadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        modifier = Modifier.testTag("sort_field_${option.id}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
        }
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = groupCount - 1, count = groupCount)) {
            MenuDefaults.DropdownMenuGroupLabel { Text("Order") }
            val labels = listOf(
                true to (field?.ascendingLabel ?: "A to Z"),
                false to (field?.descendingLabel ?: "Z to A")
            )
            labels.forEachIndexed { index, (isAscending, label) ->
                SelectableDropdownMenuItem(
                    selected = ascending == isAscending,
                    onClick = {
                        onDismiss()
                        if (ascending != isAscending) onSelectAscending(isAscending)
                    },
                    text = { Text(label) },
                    shapes = MenuDefaults.itemShape(index = index, count = labels.size),
                    leadingIcon = {
                        Icon(
                            if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null
                        )
                    },
                    selectedLeadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                    modifier = Modifier.testTag(if (isAscending) "sort_order_asc" else "sort_order_desc")
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Selection toolbar (long-press)
// ---------------------------------------------------------------------------------------------

/** Official floating toolbar with the actions for the selected songs. */
@Composable
fun AudioSelectionToolbar(
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onBlacklist: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            toolbarContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.testTag("audio_selection_toolbar")
    ) {
        SelectionToolbarButton(Icons.AutoMirrored.Filled.PlaylistPlay, "Next", onPlayNext, "selection_play_next")
        SelectionToolbarButton(Icons.AutoMirrored.Filled.QueueMusic, "Queue", onAddToQueue, "selection_add_queue")
        SelectionToolbarButton(Icons.AutoMirrored.Filled.PlaylistAdd, "Playlist", onAddToPlaylist, "selection_add_playlist")
        SelectionToolbarButton(Icons.Default.Block, "Blacklist", onBlacklist, "selection_blacklist")
        SelectionToolbarButton(
            Icons.Default.DeleteOutline,
            "Delete",
            onDelete,
            "selection_delete",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun SelectionToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String,
    tint: Color = LocalContentColor.current
) {
    TextButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = Modifier
            .widthIn(min = 60.dp)
            .height(54.dp)
            .testTag(tag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Playlists: add-to sheet and name dialog
// ---------------------------------------------------------------------------------------------

/** Pick a playlist for [songCount] songs, or start a new one. */
@Composable
fun AddToPlaylistSheet(
    playlists: List<AudioPlaylist>,
    songCount: Int,
    onCreateNew: () -> Unit,
    onPick: (AudioPlaylist) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("add_to_playlist_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
            val rows = playlists.size + 1
            val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            SegmentedListItem(
                onClick = onCreateNew,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = rows),
                leadingContent = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                },
                colors = colors,
                modifier = Modifier.testTag("add_to_playlist_new")
            ) {
                Text("New playlist")
            }
            playlists.forEachIndexed { index, playlist ->
                SegmentedListItem(
                    onClick = { onPick(playlist) },
                    shapes = ListItemDefaults.segmentedShapes(index = index + 1, count = rows),
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
                            }
                        }
                    },
                    supportingContent = {
                        val n = playlist.trackIds.size
                        Text(if (n == 1) "1 song" else "$n songs")
                    },
                    colors = colors,
                    modifier = Modifier.testTag("add_to_playlist_${playlist.id}")
                ) {
                    Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Official dialog with a text field, used to name a new playlist or rename one. */
@Composable
fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTextFieldState(initialName)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val name = state.text.toString().trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                state = state,
                label = { Text("Name") },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("playlist_name_field")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotEmpty(),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("playlist_name_confirm")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text("Cancel")
            }
        }
    )
}
