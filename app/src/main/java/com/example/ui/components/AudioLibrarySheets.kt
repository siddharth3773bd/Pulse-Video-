@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import sh.calvin.reorderable.ReorderableColumn
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.key
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrackItem
import com.example.model.BlacklistedSong
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import com.example.model.LibraryPreferences
import com.example.model.LibrarySongSort
import com.example.model.LibraryTab
import com.example.model.NowPlayingAction
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Shared sheet frame
// ---------------------------------------------------------------------------------------------

@Composable
private fun LibrarySheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            content = content
        )
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    val palette = LocalM3Palette.current
    Text(
        text = title,
        color = palette.onSurface,
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    if (subtitle != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = palette.onSurfaceVariant,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

// ---------------------------------------------------------------------------------------------
// Now Playing tray buttons
// ---------------------------------------------------------------------------------------------

@Composable
fun NowPlayingTraySheet(
    order: List<NowPlayingAction>,
    hidden: Set<NowPlayingAction>,
    onChange: (List<NowPlayingAction>, Set<NowPlayingAction>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    LibrarySheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Now Playing buttons",
            subtitle = "Drag the handle to reorder. Switch buttons on or off. The tray holds up to 5 buttons."
        )

        ReorderableToggleList(
            items = order,
            hidden = hidden,
            label = { it.label },
            icon = { nowPlayingActionIcon(it) },
            canToggle = { _, _ -> true },
            onChange = onChange
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onReset) {
                Text(
                    text = "Reset to default",
                    color = palette.primary,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Manage tabs
// ---------------------------------------------------------------------------------------------

@Composable
fun ManageTabsSheet(
    prefs: LibraryPreferences,
    onChange: (List<LibraryTab>, Set<LibraryTab>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val order = prefs.tabOrder
    val hidden = prefs.hiddenTabs
    val visibleCount = order.count { it !in hidden }

    LibrarySheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Manage tabs",
            subtitle = "Drag the handle to reorder. Switch tabs on or off. One tab always stays visible."
        )

        ReorderableToggleList(
            items = order,
            hidden = hidden,
            label = { it.label },
            icon = { it.icon },
            // One tab always stays visible.
            canToggle = { tab, isVisible -> !(isVisible && visibleCount == 1 && tab !in hidden) },
            onChange = onChange
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onReset) {
                Text(
                    text = "Reset to default",
                    color = palette.primary,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Song info sheet (technical details only)
// ---------------------------------------------------------------------------------------------

private data class AudioTech(val sampleRateHz: Int?, val bitrateBps: Int?)

/** "48.0 kHz • 320 kbps • MP3" style label for the Now Playing screen. */
suspend fun audioTechLabel(context: Context, track: AudioTrackItem): String {
    val tech = readAudioTech(context, track.contentUri)
    val rate = tech.sampleRateHz?.let { String.format(Locale.US, "%.1f kHz", it / 1000.0) }
    val kbps = tech.bitrateBps?.let { "${it / 1000} kbps" }
    return listOfNotNull(rate, kbps, track.ext.uppercase(Locale.US)).joinToString(" • ")
}

private suspend fun readAudioTech(context: Context, uri: Uri): AudioTech = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        var sampleRate: Int? = null
        var bitrate: Int? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                }
                break
            }
        }
        AudioTech(sampleRate, bitrate)
    } catch (_: Exception) {
        AudioTech(null, null)
    } finally {
        try {
            extractor.release()
        } catch (_: Exception) {
        }
    }
}

private fun formatDurationLabel(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
fun SongInfoSheet(
    track: AudioTrackItem,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val context = LocalContext.current
    var tech by remember(track.id) { mutableStateOf<AudioTech?>(null) }
    var techLoaded by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        tech = readAudioTech(context, track.contentUri)
        techLoaded = true
    }

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
        android.widget.Toast.makeText(context, "$label copied", android.widget.Toast.LENGTH_SHORT).show()
    }

    val pending = if (techLoaded) "—" else "…"
    val fileName = track.path.substringAfterLast('/').takeIf { it.isNotBlank() }
    val folderName = track.path.substringBeforeLast('/', "").substringAfterLast('/').takeIf { it.isNotBlank() }

    LibrarySheet(onDismiss = onDismiss) {
        // Cover + full title and artist
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHigh,
                placeholderTint = palette.primary,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                iconSize = 30.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist.ifBlank { "Unknown artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Sound details: small label on top, the value large
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecCell("Format", track.ext.uppercase(Locale.US).ifBlank { "—" }, Modifier.weight(1f))
                    SpecCell("Duration", formatDurationLabel(track.durationMs), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecCell(
                        "Bitrate",
                        tech?.bitrateBps?.let { "${it / 1000} kbps" } ?: pending,
                        Modifier.weight(1f)
                    )
                    SpecCell(
                        "Sample rate",
                        tech?.sampleRateHz?.let { String.format(Locale.US, "%.1f kHz", it / 1000.0) } ?: pending,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        // File details: plain text, long-press a value to copy it. Nothing the header already says.
        val details = listOfNotNull(
            ("Album" to track.album).takeIf {
                track.album.isNotBlank() && track.album != "Unknown album" &&
                    !track.album.equals(track.title, ignoreCase = true)
            },
            ("Size" to String.format(Locale.US, "%.1f MB", track.sizeMB)).takeIf { track.sizeBytes > 0L },
            folderName?.let { "Folder" to it },
            fileName?.let { "File" to it }
        )
        Column(modifier = Modifier.padding(top = 20.dp, start = 4.dp, end = 4.dp)) {
            details.forEach { (label, value) ->
                DetailLine(label = label, value = value, onCopy = { copy(label, value) })
            }
        }
    }
}

@Composable
private fun SpecCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(value) { detectTapGestures(onLongPress = { onCopy() }) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Rows with an icon, a label, an on/off switch and a drag handle (≡). Press and drag the handle to
 * reorder (Reorderable library); the new order is saved when the drag settles.
 */
@Composable
private fun <T> ReorderableToggleList(
    items: List<T>,
    hidden: Set<T>,
    label: (T) -> String,
    icon: (T) -> androidx.compose.ui.graphics.vector.ImageVector,
    canToggle: (item: T, isVisible: Boolean) -> Boolean,
    onChange: (List<T>, Set<T>) -> Unit
) {
    val palette = LocalM3Palette.current
    // Local copy so rows move live while dragging.
    var rows by remember(items) { mutableStateOf(items) }
    val haptic = LocalHapticFeedback.current

    ReorderableColumn(
        list = rows,
        onSettle = { fromIndex, toIndex ->
            val next = rows.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            rows = next
            onChange(next, hidden)
        },
        onMove = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) { _, item, isDragging ->
        key(item) {
            ReorderableItem {
                val isVisible = item !in hidden
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 6.dp else 0.dp,
                    label = "rowDragElevation"
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDragging) palette.surfaceContainerHighest else palette.surfaceContainerHigh,
                    shadowElevation = elevation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon(item),
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label(item),
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isVisible,
                            enabled = canToggle(item, isVisible),
                            onCheckedChange = { checked ->
                                val nextHidden = if (checked) hidden - item else hidden + item
                                onChange(rows, nextHidden)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = palette.primary,
                                checkedThumbColor = palette.onPrimary
                            )
                        )
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder ${label(item)}",
                                tint = palette.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Blacklist (Me > Music > Hide from library)
// ---------------------------------------------------------------------------------------------

@Composable
private fun blacklistRowColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
)

/** Songs hidden with ⋮ › Blacklist song. "Restore" puts a song back in the library. */
@Composable
fun BlacklistedSongsSheet(
    songs: List<BlacklistedSong>,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LibrarySheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Blacklisted songs",
            subtitle = "Hidden from your library. The files stay on your device."
        )
        if (songs.isEmpty()) {
            Text(
                text = "Nothing here yet. Use ⋮ › Blacklist song on any song, or long-press several.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            songs.forEachIndexed { index, song ->
                key(song.id) {
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = songs.size),
                        leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                        supportingContent = {
                            Text(song.artist.ifBlank { "Unknown artist" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            TextButton(
                                onClick = { onRestore(song.id) },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text("Restore")
                            }
                        },
                        colors = blacklistRowColors()
                    ) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** A folder that holds music, with how many songs are in it. */
data class MusicFolder(val path: String, val songCount: Int)

/** Shorter, readable folder path: drops the "/storage/emulated/0/" prefix. */
private fun displayFolderPath(path: String): String =
    path.removePrefix("/storage/emulated/0/").removePrefix("/storage/emulated/0")

/**
 * Every folder that holds music, each with a switch. A blacklisted folder hides its songs and the
 * songs of its sub-folders, including ones added later.
 */
@Composable
fun BlacklistedFoldersSheet(
    folders: List<MusicFolder>,
    blacklisted: Set<String>,
    onSetBlacklisted: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // Blacklisted folders first, then the busiest folders. Keep folders that no longer hold songs.
    val known = folders.map { it.path }.toSet()
    val all = (folders + blacklisted.filter { it !in known }.map { MusicFolder(it, 0) })
        .sortedWith(compareBy<MusicFolder>({ it.path !in blacklisted }, { -it.songCount }, { it.path.lowercase() }))
    LibrarySheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Blacklisted folders",
            subtitle = "Songs in a blacklisted folder, and its sub-folders, are hidden from your library. " +
                "Handy for WhatsApp audio and voice notes."
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            all.forEachIndexed { index, folder ->
                val hidden = folder.path in blacklisted
                key(folder.path) {
                    SegmentedListItem(
                        checked = hidden,
                        onCheckedChange = { onSetBlacklisted(folder.path, it) },
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = all.size),
                        leadingContent = {
                            Icon(
                                imageVector = if (hidden) Icons.Default.FolderOff else Icons.Default.Folder,
                                contentDescription = null
                            )
                        },
                        supportingContent = {
                            val count = if (folder.songCount == 1) "1 song" else "${folder.songCount} songs"
                            Text(
                                text = "${displayFolderPath(folder.path)} · $count",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingContent = { Switch(checked = hidden, onCheckedChange = null) },
                        colors = blacklistRowColors()
                    ) {
                        Text(
                            text = folder.path.substringAfterLast('/').ifBlank { folder.path },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
