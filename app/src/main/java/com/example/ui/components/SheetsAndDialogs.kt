@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LayoutMode
import com.example.model.PlaylistItem
import com.example.model.SortBy
import com.example.model.SortDir
import com.example.model.VideoItem
import com.example.model.ViewMode
import com.example.model.formatDurationMs
import com.example.model.formatSizeLabel
import com.example.model.naturalDir

// ---------------------------------------------------------------------------------------------
// Video ⋮ menu (anchored to the button that opened it)
// ---------------------------------------------------------------------------------------------

/** The video ⋮ menu: Expressive grouped menu, with Delete on its own at the end. */
@Composable
fun VideoActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    AudioGroupedMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        groups = listOf(
            listOf(
                AudioMenuEntry("Add to playlist", Icons.AutoMirrored.Filled.PlaylistAdd, "menu_action_add_to_playlist", onClick = onAddToPlaylist),
                AudioMenuEntry("Rename", Icons.Default.Edit, "menu_action_rename", onClick = onRename),
                AudioMenuEntry("Share", Icons.Default.Share, "menu_action_share", onClick = onShare),
                AudioMenuEntry("Details", Icons.Default.Info, "menu_action_details", onClick = onDetails)
            ),
            listOf(
                AudioMenuEntry("Delete", Icons.Default.DeleteOutline, "menu_action_delete", destructive = true, onClick = onDelete)
            )
        )
    )
}

// ---------------------------------------------------------------------------------------------
// Sort & view
// ---------------------------------------------------------------------------------------------

/**
 * View & sort, kept compact: each part is one short row (Show, Layout, Sort by, Order) plus a
 * slim Smart groups switch, so the panel only takes the space it needs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortAndViewBottomSheet(
    smartGroups: Boolean,
    viewMode: ViewMode,
    layoutMode: LayoutMode,
    onSelectViewMode: (ViewMode) -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit,
    sortBy: SortBy,
    sortDir: SortDir,
    onToggleSmartGroups: () -> Unit,
    onSelectSortField: (SortBy) -> Unit,
    onSelectSortDir: (SortDir) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Opens fully, so nothing ends up under the gesture bar.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("sort_and_view_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactRow("Show") {
                ConnectedChoice(
                    options = listOf(
                        ViewMode.VIDEOS to "Videos",
                        ViewMode.FOLDERS to "Folders",
                        ViewMode.TREE to "Tree"
                    ),
                    selected = viewMode,
                    onSelect = onSelectViewMode,
                    tag = { "view_mode_btn_${it.id}" }
                )
            }
            CompactRow("Layout") {
                ConnectedChoice(
                    options = listOf(
                        LayoutMode.LIST to "List",
                        LayoutMode.GRID to "Grid"
                    ),
                    selected = layoutMode,
                    onSelect = onSelectLayoutMode,
                    tag = { "layout_mode_btn_${it.id}" }
                )
            }
            CompactRow("Sort by") {
                ConnectedChoice(
                    options = SortBy.entries.map { it to it.label },
                    selected = sortBy,
                    onSelect = onSelectSortField,
                    tag = { "sort_option_btn_${it.id}" }
                )
            }
            CompactRow("Order") {
                val natural = sortBy.naturalDir
                val dirs = listOf(natural, if (natural == SortDir.ASC) SortDir.DESC else SortDir.ASC)
                ConnectedChoice(
                    options = dirs.map { it to if (it == SortDir.ASC) sortBy.ascFull else sortBy.descFull },
                    selected = sortDir,
                    onSelect = onSelectSortDir,
                    tag = { "sort_dir_${it.name.lowercase()}" }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .toggleable(value = smartGroups, onValueChange = { onToggleSmartGroups() })
                    .padding(vertical = 4.dp)
                    .testTag("smart_groups_toggle_card")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Group series together", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = smartGroups, onCheckedChange = null)
            }
        }
    }
}

/** A small label above one row of choices. */
@Composable
private fun CompactRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        content()
    }
}

/**
 * An Expressive connected button group for picking one of a few options. Every button has the
 * same width and the official tonal colours, so picking one never moves anything.
 */
@Composable
private fun <T> ConnectedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    tag: (T) -> String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, (value, label) ->
            FilledTonalToggleButton(
                checked = value == selected,
                onCheckedChange = { if (value != selected) onSelect(value) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton }
                    .testTag(tag(value))
            ) {
                Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Details
// ---------------------------------------------------------------------------------------------

/**
 * Video details, kept small: the thumbnail and name with where it lives, then one card with the
 * basics (length, size, format, dimensions).
 */
@Composable
fun VideoDetailsSheet(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("video_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VideoThumbnailBox(
                    video = video,
                    modifier = Modifier
                        .width(112.dp)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        video.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${friendlyFolderPath(video.folderPath)} · ${video.dateStr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row {
                        DetailCell("Length", video.durationLabel, Modifier.weight(1f))
                        DetailCell("Size", formatSizeLabel(video.sizeMB), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row {
                        DetailCell("Format", video.ext.uppercase(), Modifier.weight(1f))
                        DetailCell(
                            "Dimensions",
                            if (video.width > 0 && video.height > 0) "${video.width} × ${video.height}" else "—",
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------------------------
// Rename
// ---------------------------------------------------------------------------------------------

@Composable
fun RenameVideoDialog(
    video: VideoItem,
    renameValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue(renameValue, TextRange(0, renameValue.length))) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Rename video") },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it
                    onValueChange(it.text)
                },
                singleLine = true,
                label = { Text("Name") },
                suffix = { Text(".${video.ext}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (field.text.isNotBlank()) onConfirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .testTag("rename_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = field.text.isNotBlank(),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("rename_save_btn")
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
        },
        modifier = Modifier.testTag("rename_video_dialog")
    )
}

// ---------------------------------------------------------------------------------------------
// Add to playlist
// ---------------------------------------------------------------------------------------------

/** Pick a video playlist, or make a new one (bottom sheet, like Music). */
@Composable
fun AddVideosToPlaylistSheet(
    playlists: List<PlaylistItem>,
    onSelectPlaylistName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("add_to_playlist_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Add to playlist", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            if (playlists.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    playlists.forEachIndexed { i, pl ->
                        SegmentedListItem(
                            onClick = { onSelectPlaylistName(pl.name) },
                            shapes = ListItemDefaults.segmentedShapes(index = i, count = playlists.size),
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                            supportingContent = {
                                Text(if (pl.mediaUris.size == 1) "1 video" else "${pl.mediaUris.size} videos")
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.testTag("playlist_option_${pl.name}")
                        ) { Text(pl.name) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text("New playlist") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (newName.isNotBlank()) onSelectPlaylistName(newName.trim()) }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
                onClick = { onSelectPlaylistName(newName.trim()) },
                enabled = newName.isNotBlank(),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Create and add")
            }
        }
    }
}
