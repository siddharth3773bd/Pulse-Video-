@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
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
data class MenuEntry(
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
fun ExpressiveMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    groups: List<List<MenuEntry>>,
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

/**
 * A song's ⋮ menu: the official Material 3 Expressive pop-up menu (the same as a video's ⋮),
 * anchored to the button. Three groups: play/share, go to/info, then blacklist and delete.
 */
@Composable
fun SongActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canGoToAlbum: Boolean,
    canGoToArtist: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onSongInfo: () -> Unit,
    onBlacklistSong: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    ExpressiveMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        groups = listOf(
            listOf(
                MenuEntry("Play next", Symbols.PlaylistPlay, "song_action_play_next", onClick = onPlayNext),
                MenuEntry("Add to queue", Symbols.QueueMusic, "song_action_add_queue", onClick = onAddToQueue),
                MenuEntry("Add to playlist", Symbols.PlaylistAdd, "song_action_add_playlist", onClick = onAddToPlaylist),
                MenuEntry("Share", Symbols.Share, "song_action_share", onClick = onShare)
            ),
            listOfNotNull(
                MenuEntry("Go to album", Symbols.Album, "song_action_album", onClick = onGoToAlbum)
                    .takeIf { canGoToAlbum },
                MenuEntry("Go to artist", Symbols.Person, "song_action_artist", onClick = onGoToArtist)
                    .takeIf { canGoToArtist },
                MenuEntry("Song info", Symbols.Info, "song_action_info", onClick = onSongInfo),
                onRemoveFromPlaylist?.let {
                    MenuEntry("Remove from playlist", Symbols.PlaylistRemove, "song_action_remove_playlist", onClick = it)
                }
            ),
            listOf(
                MenuEntry("Blacklist song", Symbols.Block, "song_action_blacklist", onClick = onBlacklistSong),
                MenuEntry("Delete from device", Symbols.DeleteOutline, "song_action_delete", destructive = true, onClick = onDelete)
            )
        )
    )
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
    ExpressiveMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        groups = listOf(
            listOf(
                MenuEntry("Play next", Symbols.PlaylistPlay, "playlist_action_play_next", onClick = onPlayNext),
                MenuEntry("Add to queue", Symbols.QueueMusic, "playlist_action_add_queue", onClick = onAddToQueue)
            ),
            listOf(
                MenuEntry("Rename", Symbols.Edit, "playlist_action_rename", onClick = onRename),
                MenuEntry("Delete playlist", Symbols.DeleteOutline, "playlist_action_delete", destructive = true, onClick = onDelete)
            )
        )
    )
}

// ---------------------------------------------------------------------------------------------
// Sort menu
// ---------------------------------------------------------------------------------------------

/**
 * Sort panel (a bottom sheet), styled like the Videos one: "Sort by" and "Order" as the official
 * Expressive standard button group. Changes apply right away. [field] is null for albums and
 * artists, which only sort A to Z / Z to A.
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
    if (!expanded) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("library_sort_menu")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (field != null) {
                CompactRow("Sort by") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LibrarySongSort.entries.chunked(3).forEach { row ->
                            ChoiceButtonGroup(
                                options = row.map { it to it.label },
                                selected = field.takeIf { it in row },
                                onSelect = { option -> if (option != null) onSelectField(option) },
                                tag = { "sort_field_${it?.id}" }
                            )
                        }
                    }
                }
            }
            // The usual choice for this field comes first: Newest, Longest, A to Z.
            val naturalAscending = field?.naturalAscending ?: true
            CompactRow("Order") {
                ChoiceButtonGroup(
                    options = listOf(naturalAscending, !naturalAscending).map { isAscending ->
                        isAscending to if (isAscending) field?.ascendingLabel ?: "A to Z" else field?.descendingLabel ?: "Z to A"
                    },
                    selected = ascending,
                    onSelect = { onSelectAscending(it) },
                    tag = { if (it) "sort_order_asc" else "sort_order_desc" }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Selection toolbar (long-press)
// ---------------------------------------------------------------------------------------------

/**
 * Actions for the selected songs: the official floating toolbar in its vibrant colours (same as
 * Videos), icon buttons with their names in tooltips.
 */
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
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier.testTag("audio_selection_toolbar")
    ) {
        TooltipIconButton("Play next", Symbols.PlaylistPlay, onPlayNext, Modifier.testTag("selection_play_next"), tooltipAbove = true)
        TooltipIconButton("Add to queue", Symbols.QueueMusic, onAddToQueue, Modifier.testTag("selection_add_queue"), tooltipAbove = true)
        TooltipIconButton("Add to playlist", Symbols.PlaylistAdd, onAddToPlaylist, Modifier.testTag("selection_add_playlist"), tooltipAbove = true)
        TooltipIconButton("Blacklist", Symbols.Block, onBlacklist, Modifier.testTag("selection_blacklist"), tooltipAbove = true)
        TooltipIconButton("Delete from device", Symbols.DeleteOutline, onDelete, Modifier.testTag("selection_delete"), tooltipAbove = true)
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
                            Icon(Symbols.Add, contentDescription = null)
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
                                Icon(Symbols.PlaylistPlay, contentDescription = null)
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
