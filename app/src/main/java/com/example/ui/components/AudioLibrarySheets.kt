@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrackItem
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
// Library settings (gear)
// ---------------------------------------------------------------------------------------------

@Composable
fun LibrarySettingsSheet(
    onOpenManageTabs: () -> Unit,
    onOpenTray: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    LibrarySheet(onDismiss = onDismiss) {
        SheetTitle(title = "Library settings")

        Surface(
            onClick = onOpenManageTabs,
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Manage tabs",
                    color = palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reorder, show or hide Library tabs.",
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            onClick = onOpenTray,
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Now Playing buttons",
                    color = palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose and reorder the buttons in the bottom tray.",
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp
                )
            }
        }
    }
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
            subtitle = "Use the arrows to reorder. Switch buttons on or off. The tray holds up to 5 buttons."
        )

        order.forEachIndexed { index, action ->
            val isVisible = action !in hidden
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val next = order.toMutableList()
                                    val moved = next.removeAt(index)
                                    next.add(index - 1, moved)
                                    onChange(next, hidden)
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move ${action.label} up",
                                tint = palette.onSurfaceVariant.copy(alpha = if (index > 0) 1f else 0.3f)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < order.lastIndex) {
                                    val next = order.toMutableList()
                                    val moved = next.removeAt(index)
                                    next.add(index + 1, moved)
                                    onChange(next, hidden)
                                }
                            },
                            enabled = index < order.lastIndex,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move ${action.label} down",
                                tint = palette.onSurfaceVariant.copy(alpha = if (index < order.lastIndex) 1f else 0.3f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = nowPlayingActionIcon(action),
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = action.label,
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { checked ->
                            val nextHidden = if (checked) hidden - action else hidden + action
                            onChange(order, nextHidden)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.primary,
                            checkedThumbColor = palette.onPrimary
                        )
                    )
                }
            }
        }

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
            subtitle = "Use the arrows to reorder. Switch tabs on or off. One tab always stays visible."
        )

        order.forEachIndexed { index, tab ->
            val isVisible = tab !in hidden
            val isLastVisible = isVisible && visibleCount == 1
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val next = order.toMutableList()
                                    val moved = next.removeAt(index)
                                    next.add(index - 1, moved)
                                    onChange(next, hidden)
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move ${tab.label} up",
                                tint = palette.onSurfaceVariant.copy(alpha = if (index > 0) 1f else 0.3f)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < order.lastIndex) {
                                    val next = order.toMutableList()
                                    val moved = next.removeAt(index)
                                    next.add(index + 1, moved)
                                    onChange(next, hidden)
                                }
                            },
                            enabled = index < order.lastIndex,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move ${tab.label} down",
                                tint = palette.onSurfaceVariant.copy(alpha = if (index < order.lastIndex) 1f else 0.3f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tab.label,
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isVisible,
                        enabled = !isLastVisible,
                        onCheckedChange = { checked ->
                            val nextHidden = if (checked) hidden - tab else hidden + tab
                            onChange(order, nextHidden)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.primary,
                            checkedThumbColor = palette.onPrimary
                        )
                    )
                }
            }
        }

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
// Sort sheet
// ---------------------------------------------------------------------------------------------

@Composable
fun LibrarySortSheet(
    showFields: Boolean,
    field: LibrarySongSort,
    ascending: Boolean,
    onSelectField: (LibrarySongSort) -> Unit,
    onToggleOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    LibrarySheet(onDismiss = onDismiss) {
        SheetTitle(title = "Sort by")

        Surface(
            onClick = onToggleOrder,
            shape = RoundedCornerShape(24.dp),
            color = palette.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.onSecondaryContainer.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = palette.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Order",
                        color = palette.onSecondaryContainer.copy(alpha = 0.8f),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (ascending) "Ascending" else "Descending",
                        color = palette.onSecondaryContainer,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showFields) {
            Spacer(modifier = Modifier.height(14.dp))
            LibrarySongSort.entries.forEach { option ->
                val isSelected = option == field
                Surface(
                    onClick = { onSelectField(option) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) palette.secondaryContainer else palette.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            color = if (isSelected) palette.onSecondaryContainer else palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectField(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = palette.primary,
                                unselectedColor = palette.onSurfaceVariant
                            )
                        )
                    }
                }
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

    LaunchedEffect(track.id) {
        tech = readAudioTech(context, track.contentUri)
    }

    val songInfoText = tech?.let { t ->
        val rate = t.sampleRateHz?.let { String.format(Locale.US, "%.1f kHz", it / 1000.0) } ?: "-"
        val rateKbps = t.bitrateBps?.let { "${it / 1000} kbps" } ?: "-"
        "$rate · $rateKbps · ${track.ext.uppercase(Locale.US)}"
    } ?: "Loading…"

    LibrarySheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHigh,
                placeholderTint = palette.primary,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(18.dp),
                iconSize = 30.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        InfoCard(label = "Duration", value = formatDurationLabel(track.durationMs), bg = palette.surfaceContainerHigh)
        InfoCard(label = "Album", value = track.album, bg = palette.surfaceContainerHigh)
        InfoCard(label = "Artist", value = track.artist, bg = palette.surfaceContainerHigh)
        InfoCard(label = "Song info", value = songInfoText, bg = palette.surfaceContainerHigh)
        InfoCard(label = "Path", value = track.path.ifBlank { "-" }, bg = palette.surfaceContainerHigh)
    }
}

@Composable
private fun InfoCard(label: String, value: String, bg: Color) {
    val palette = LocalM3Palette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                text = label,
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
