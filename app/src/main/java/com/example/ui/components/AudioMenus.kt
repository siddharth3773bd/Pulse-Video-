@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.model.AudioTrackItem
import com.example.ui.theme.LocalM3Palette
import kotlinx.coroutines.launch
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

/**
 * The song ⋮ panel (a bottom sheet): a header with the song, four quick actions, then the
 * rest as grouped rows. The actions that hide or delete a song sit in their own group at the end.
 */
@Composable
fun SongActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    track: AudioTrackItem,
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
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    if (!expanded) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val palette = LocalM3Palette.current

    // Slide the panel away first, then run the action.
    fun act(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("song_actions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioArt(
                    uri = track.contentUri,
                    placeholderBackground = palette.surfaceContainerHighest,
                    placeholderTint = palette.primary,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist.ifBlank { "Unknown artist" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAction(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next", "song_action_play_next") { act(onPlayNext) }
                QuickAction(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue", "song_action_add_queue") { act(onAddToQueue) }
                QuickAction(
                    icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (liked) "Liked" else "Like",
                    tag = "song_action_like",
                    highlighted = liked
                ) { act(onToggleLike) }
                QuickAction(Icons.Default.Share, "Share", "song_action_share") { act(onShare) }
            }

            // Library actions
            val main = listOfNotNull(
                AudioMenuEntry("Add to playlist", Icons.AutoMirrored.Filled.PlaylistAdd, "song_action_add_playlist", onClick = onAddToPlaylist),
                AudioMenuEntry("Go to album", Icons.Default.Album, "song_action_album", onClick = onGoToAlbum)
                    .takeIf { canGoToAlbum },
                AudioMenuEntry("Go to artist", Icons.Default.Person, "song_action_artist", onClick = onGoToArtist)
                    .takeIf { canGoToArtist },
                AudioMenuEntry("Song info", Icons.Default.Info, "song_action_info", onClick = onSongInfo),
                onRemoveFromPlaylist?.let {
                    AudioMenuEntry("Remove from playlist", Icons.Default.PlaylistRemove, "song_action_remove_playlist", onClick = it)
                }
            )
            SheetActionGroup(main, onAct = { act(it) })
            Spacer(modifier = Modifier.height(12.dp))
            SheetActionGroup(
                listOf(
                    AudioMenuEntry("Blacklist song", Icons.Default.Block, "song_action_blacklist", onClick = onBlacklistSong),
                    AudioMenuEntry("Delete from device", Icons.Default.DeleteOutline, "song_action_delete", destructive = true, onClick = onDelete)
                ),
                onAct = { act(it) }
            )
        }
    }
}

/** A big tonal button with the icon above the label. */
@Composable
private fun RowScope.QuickAction(
    icon: ImageVector,
    label: String,
    tag: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .weight(1f)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Rows joined into one rounded group (official SegmentedListItem). */
@Composable
private fun SheetActionGroup(entries: List<AudioMenuEntry>, onAct: (() -> Unit) -> Unit) {
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        entries.forEachIndexed { index, entry ->
            val error = MaterialTheme.colorScheme.error
            SegmentedListItem(
                onClick = { onAct(entry.onClick) },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = entries.size),
                leadingContent = {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = if (entry.destructive) error else LocalContentColor.current
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.testTag(entry.tag)
            ) {
                Text(
                    text = entry.label,
                    color = if (entry.destructive) error else Color.Unspecified
                )
            }
        }
    }
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
 * Sort panel (a bottom sheet): "Sort by" chips and an Ascending / Descending connected button
 * group. Changes apply right away, so the list can be watched re-ordering as you pick.
 * [field] is null for albums and artists, which only sort A to Z / Z to A.
 */
@OptIn(ExperimentalLayoutApi::class)
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Sort",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (field != null) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    LibrarySongSort.entries.forEach { option ->
                        FilterChip(
                            selected = option == field,
                            onClick = { if (option != field) onSelectField(option) },
                            label = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (option == field) Icons.Default.Check else option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            modifier = Modifier.testTag("sort_field_${option.id}")
                        )
                    }
                }
            }
            Text(
                text = "Order",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ToggleButton(
                    checked = ascending,
                    onCheckedChange = { if (!ascending) onSelectAscending(true) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_order_asc")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(field?.ascendingLabel ?: "A to Z", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ToggleButton(
                    checked = !ascending,
                    onCheckedChange = { if (ascending) onSelectAscending(false) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_order_desc")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(field?.descendingLabel ?: "Z to A", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
