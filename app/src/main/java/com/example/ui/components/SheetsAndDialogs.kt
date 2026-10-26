@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlaylistItem
import com.example.model.VideoItem
import com.example.model.formatSizeLabel
import com.example.model.formatCompactDate

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
 * Video details: the thumbnail, name and date, then the basics, each with its icon.
 * See [InfoSheet] for how it looks upright and sideways.
 */
@Composable
fun VideoDetailsSheet(
    video: VideoItem,
    onDismiss: () -> Unit
) {
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
    InfoSheet(
        title = video.title,
        subtitle = formatCompactDate(video.dateStr),
        rows = rows,
        artAspectRatio = 16f / 9f,
        art = { modifier -> VideoThumbnailBox(video = video, modifier = modifier) },
        tagPrefix = "detail_row_",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("video_details_sheet")
    )
}

/**
 * The frame shared by video Details and Song info.
 * - Upright: a bottom sheet with the picture, name and line below on top, then one joined
 *   Material 3 Expressive list of facts.
 * - Sideways (little height): a centred dialog instead, the picture and name on the left and the
 *   facts as small tiles in two columns on the right, so nothing needs scrolling.
 */
@Composable
internal fun InfoSheet(
    title: String,
    subtitle: String,
    rows: List<Triple<ImageVector, String, String>>,
    artAspectRatio: Float,
    art: @Composable (Modifier) -> Unit,
    tagPrefix: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    if (currentWindowLayout().compactHeight) {
        // Wider than a normal 560dp dialog, so the facts fit in two columns.
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = scheme.surfaceContainerHigh,
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 760.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp)) {
                    Column(modifier = Modifier.width(if (artAspectRatio > 1f) 220.dp else 176.dp)) {
                        art(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(artAspectRatio)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rows.chunked(2).forEach { pair ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(IntrinsicSize.Min)
                                ) {
                                    pair.forEach { (icon, label, value) ->
                                        InfoTile(icon, label, value, Modifier.weight(1f).fillMaxHeight().testTag("$tagPrefix${label.lowercase()}"))
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        TextButton(
                            onClick = onDismiss,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp)
                        ) { Text("Close") }
                    }
                }
            }
        }
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                art(
                    Modifier
                        .width(if (artAspectRatio > 1f) 120.dp else 72.dp)
                        .aspectRatio(artAspectRatio)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoRows(rows = rows, tagPrefix = tagPrefix)
        }
    }
}

/** One fact as a small tile (sideways details). */
@Composable
private fun InfoTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = scheme.surfaceContainerHighest, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * The rows of a details sheet (an icon, a small label, the value) as one joined Expressive list.
 */
@Composable
internal fun InfoRows(rows: List<Triple<ImageVector, String, String>>, tagPrefix: String) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { i, (icon, label, value) ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = i, count = rows.size),
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
