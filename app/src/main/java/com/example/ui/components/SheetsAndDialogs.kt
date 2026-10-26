@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.formatSizeLabel
import com.example.model.formatCompactDate
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
    ExpressiveMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        groups = listOf(
            listOf(
                MenuEntry("Add to playlist", Symbols.PlaylistAdd, "menu_action_add_to_playlist", onClick = onAddToPlaylist),
                MenuEntry("Rename", Symbols.Edit, "menu_action_rename", onClick = onRename),
                MenuEntry("Share", Symbols.Share, "menu_action_share", onClick = onShare),
                MenuEntry("Details", Symbols.Info, "menu_action_details", onClick = onDetails)
            ),
            listOf(
                MenuEntry("Delete", Symbols.DeleteOutline, "menu_action_delete", destructive = true, onClick = onDelete)
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
                ChoiceButtonGroup(
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
            val layoutRow: @Composable () -> Unit = {
                CompactRow("Layout") {
                    ChoiceButtonGroup(
                        options = listOf(
                            LayoutMode.LIST to "List",
                            LayoutMode.GRID to "Grid"
                        ),
                        selected = layoutMode,
                        onSelect = onSelectLayoutMode,
                        tag = { "layout_mode_btn_${it.id}" }
                    )
                }
            }
            val orderRow: @Composable () -> Unit = {
                CompactRow("Order") {
                    val natural = sortBy.naturalDir
                    val dirs = listOf(natural, if (natural == SortDir.ASC) SortDir.DESC else SortDir.ASC)
                    ChoiceButtonGroup(
                        options = dirs.map { it to if (it == SortDir.ASC) sortBy.ascFull else sortBy.descFull },
                        selected = sortDir,
                        onSelect = onSelectSortDir,
                        tag = { "sort_dir_${it.name.lowercase()}" }
                    )
                }
            }
            val sortRow: @Composable () -> Unit = {
                CompactRow("Sort by") {
                    ChoiceButtonGroup(
                        options = SortBy.entries.map { it to it.label },
                        selected = sortBy,
                        onSelect = onSelectSortField,
                        tag = { "sort_option_btn_${it.id}" }
                    )
                }
            }
            if (currentWindowLayout().compactHeight) {
                // Sideways: the two-choice rows share a line, so everything fits without scrolling.
                sortRow()
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { layoutRow() }
                    Box(Modifier.weight(1f)) { orderRow() }
                }
            } else {
                layoutRow()
                sortRow()
                orderRow()
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
                Icon(Symbols.AutoAwesome, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Group series together", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = smartGroups, onCheckedChange = null)
            }
        }
    }
}

/** A small label above one row of choices. */
@Composable
internal fun CompactRow(label: String, content: @Composable () -> Unit) {
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
 * Pick one of a few options: the official Material 3 Expressive standard button group. Separate
 * rounded buttons sharing the width equally; the pressed one stretches and squeezes its
 * neighbours, and the selected one turns into a rounded square.
 */
@Composable
internal fun <T> ChoiceButtonGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    tag: (T) -> String
) {
    val scheme = MaterialTheme.colorScheme
    val colors = ToggleButtonDefaults.colors(
        containerColor = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        checkedContainerColor = scheme.primary,
        checkedContentColor = scheme.onPrimary
    )
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (value, label) ->
            customItem(
                buttonGroupContent = {
                    val interaction = remember { MutableInteractionSource() }
                    ToggleButton(
                        checked = value == selected,
                        onCheckedChange = { if (value != selected) onSelect(value) },
                        colors = colors,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        interactionSource = interaction,
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interaction)
                            .semantics { role = Role.RadioButton }
                            .testTag(tag(value))
                    ) {
                        Text(label, maxLines = 1, softWrap = false)
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(value)
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Details
// ---------------------------------------------------------------------------------------------

/** "720p", "1080p", "4K"… from a video's size, for the details screen. */
private fun qualityName(width: Int, height: Int): String? {
    val short = minOf(width, height)
    return when {
        short <= 0 -> null
        short >= 2000 -> "4K"
        short >= 1400 -> "1440p"
        short >= 1000 -> "1080p"
        short >= 700 -> "720p"
        short >= 470 -> "480p"
        else -> "${short}p"
    }
}

/**
 * Video details: the thumbnail, name and date on top, then one grouped Material 3 Expressive list
 * (the same grouped style as the ⋮ menu) with the basics, each with its icon.
 */
@Composable
fun VideoDetailsSheet(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("video_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                VideoThumbnailBox(
                    video = video,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f)
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
                        formatCompactDate(video.dateStr),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            val resolution = if (video.width > 0 && video.height > 0) {
                listOfNotNull("${video.width} × ${video.height}", qualityName(video.width, video.height)).joinToString(" · ")
            } else "Unknown"
            val rows = listOf(
                Triple(Symbols.Timer, "Length", video.durationLabel),
                Triple(Symbols.HighQuality, "Resolution", resolution),
                Triple(Symbols.Storage, "Size", formatSizeLabel(video.sizeMB)),
                Triple(Symbols.Description, "Format", video.ext.uppercase().ifBlank { "Unknown" }),
                Triple(Symbols.Folder, "Folder", friendlyFolderPath(video.folderPath))
            )
            InfoRows(rows = rows, tagPrefix = "detail_row_")
        }
    }
}

/**
 * The rows of a details sheet (an icon, a small label, the value) as one joined Expressive list.
 * Sideways there's little height, so they split into two joined lists side by side.
 */
@Composable
internal fun InfoRows(rows: List<Triple<ImageVector, String, String>>, tagPrefix: String) {
    val scheme = MaterialTheme.colorScheme
    val group: @Composable (List<Triple<ImageVector, String, String>>, Modifier) -> Unit = { part, modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            part.forEachIndexed { i, (icon, label, value) ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = i, count = part.size),
                    leadingContent = { Icon(icon, contentDescription = null) },
                    overlineContent = { Text(label) },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = scheme.surfaceContainerHigh,
                        leadingContentColor = scheme.onSurfaceVariant,
                        overlineContentColor = scheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("$tagPrefix${label.lowercase()}")
                ) {
                    Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (currentWindowLayout().compactHeight && rows.size > 2) {
        val split = (rows.size + 1) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            group(rows.take(split), Modifier.weight(1f))
            group(rows.drop(split), Modifier.weight(1f))
        }
    } else {
        group(rows, Modifier)
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
        icon = { Icon(Symbols.Edit, contentDescription = null) },
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
                            leadingContent = { Icon(Symbols.PlaylistAdd, contentDescription = null) },
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
                Icon(Symbols.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Create and add")
            }
        }
    }
}
