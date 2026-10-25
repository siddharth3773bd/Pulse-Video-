@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lyrics.LrcParser
import com.example.lyrics.LyricsNetworkException
import com.example.lyrics.LyricsRepository
import com.example.lyrics.LyricsSearchResult
import com.example.lyrics.LyricsSettings
import com.example.lyrics.LyricsSource
import com.example.lyrics.LyricsState
import com.example.lyrics.LyricsTextSize
import com.example.model.AudioTrackItem
import com.example.model.NowPlayingBackground
import com.example.model.formatDurationMs
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Full-screen lyrics. One tap on Lyrics shows lyrics: the song's own, saved ones, or (with
 * "Always search online") LRCLIB automatically. When there's nothing yet it asks once
 * (Search online / Load lyrics file). Every fix (search again, own file, sync) is in ⚙ here.
 */
@Composable
fun LyricsScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    positionMs: Long,
    settings: LyricsSettings,
    amoled: Boolean,
    onSettingsChange: (LyricsSettings) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handle = rememberLyricsState(track, settings)
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = LyricsRepository.readTextFile(context, uri)
                if (text == null || LrcParser.parse(text).isEmpty) {
                    Toast.makeText(context, "Couldn't read lyrics from that file", Toast.LENGTH_SHORT).show()
                } else {
                    LyricsRepository.setUserFile(context, track, text)
                    handle.reload(false)
                }
            }
        }
    }
    val loadFile: () -> Unit = { filePicker.launch(arrayOf("*/*")) }

    Box(modifier = Modifier.fillMaxSize().testTag("lyrics_screen")) {
        NowPlayingBackgroundLayer(
            style = if (amoled) NowPlayingBackground.AMOLED else NowPlayingBackground.GRADIENT,
            track = track,
            followThemeColor = Color.Black,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ---- Top bar: close, song, settings ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("lyrics_close")
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close lyrics")
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { showSettings = true },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("lyrics_settings_btn")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Lyrics settings")
                }
            }

            // ---- Lyrics or a message ----
            AnimatedContent(
                targetState = handle.state,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "lyricsState",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { state ->
                when (state) {
                    LyricsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator(color = Color.White, modifier = Modifier.testTag("lyrics_loading"))
                    }
                    is LyricsState.Loaded -> LyricsView(
                        lyrics = state.lyrics,
                        positionMs = positionMs,
                        isPlaying = isPlaying,
                        offsetMs = state.offsetMs,
                        settings = settings,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.fillMaxSize()
                    )
                    LyricsState.NeedsChoice -> ChoicePanel(
                        alwaysSearch = settings.autoSearch,
                        onAlwaysSearchChange = { onSettingsChange(settings.copy(autoSearch = it)) },
                        onSearchOnline = { handle.reload(true) },
                        onLoadFile = loadFile
                    )
                    LyricsState.NotFound -> MessagePanel(
                        icon = Icons.Default.Search,
                        title = "No lyrics found",
                        body = "LRCLIB doesn't have this song under its current name. Try Search again to fix the title or artist, or load your own file.",
                        primary = "Search again" to { showSearch = true },
                        secondary = "Load file" to loadFile
                    )
                    LyricsState.NoInternet -> MessagePanel(
                        icon = Icons.Default.WifiOff,
                        title = "No internet connection",
                        body = "Connect and try again, or load your own lyrics file.",
                        primary = "Retry" to { handle.reload(true) },
                        secondary = "Load file" to loadFile
                    )
                    LyricsState.Instrumental -> MessagePanel(
                        icon = LyricsIcon,
                        title = "Instrumental",
                        body = "This song has no lyrics.",
                        primary = "Search again" to { showSearch = true },
                        secondary = null
                    )
                }
            }

            // ---- Small controls ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.width(20.dp))
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.size(64.dp).testTag("lyrics_play_pause")
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(20.dp))
                IconButton(
                    onClick = onSkipNext,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(30.dp)) }
            }
        }
    }

    if (showSettings) {
        LyricsSettingsSheet(
            state = handle.state,
            settings = settings,
            onSettingsChange = onSettingsChange,
            onUseOnline = { use ->
                scope.launch {
                    LyricsRepository.setPreferOnline(context, track, use)
                    handle.reload(use)
                }
            },
            onSearchAgain = {
                showSettings = false
                showSearch = true
            },
            onLoadFile = {
                showSettings = false
                loadFile()
            },
            onRemoveFile = {
                scope.launch {
                    LyricsRepository.setUserFile(context, track, null)
                    handle.reload(false)
                }
            },
            onOffset = { offset ->
                scope.launch {
                    LyricsRepository.setOffset(context, track, offset)
                    handle.reload(false)
                }
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showSearch) {
        LyricsSearchSheet(
            track = track,
            onPick = { result ->
                showSearch = false
                scope.launch {
                    LyricsRepository.useResult(context, track, result)
                    handle.reload(false)
                }
            },
            onDismiss = { showSearch = false }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Messages
// ---------------------------------------------------------------------------------------------

/** First time a song has no lyrics (and auto-search is off): pick once. */
@Composable
private fun ChoicePanel(
    alwaysSearch: Boolean,
    onAlwaysSearchChange: (Boolean) -> Unit,
    onSearchOnline: () -> Unit,
    onLoadFile: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            modifier = Modifier.widthIn(max = 440.dp).testTag("lyrics_choice")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(LyricsIcon, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text("Find lyrics for this song", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Search LRCLIB, a free online lyrics library, or load your own .lrc or .txt file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onSearchOnline,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().testTag("lyrics_search_online")
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Search online")
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onLoadFile,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("lyrics_load_file")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Load lyrics file")
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlwaysSearchChange(!alwaysSearch) }
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Always search online", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Don't ask again; find lyrics automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Switch(checked = alwaysSearch, onCheckedChange = onAlwaysSearchChange)
                }
            }
        }
    }
}

@Composable
private fun MessagePanel(
    icon: ImageVector,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 380.dp)
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = primary.second,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) { Text(primary.first) }
            if (secondary != null) {
                FilledTonalButton(
                    onClick = secondary.second,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    )
                ) { Text(secondary.first) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

@Composable
private fun LyricsSettingsSheet(
    state: LyricsState,
    settings: LyricsSettings,
    onSettingsChange: (LyricsSettings) -> Unit,
    onUseOnline: (Boolean) -> Unit,
    onSearchAgain: () -> Unit,
    onLoadFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onOffset: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val loaded = state as? LyricsState.Loaded
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("lyrics_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Lyrics", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            if (loaded != null) {
                Text(
                    text = "From: ${loaded.source.label}" + if (loaded.lyrics.synced) " · Synced" else " · Not synced",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }

            SheetSectionTitle("This song")
            val songRows = buildList<@Composable (Int, Int) -> Unit> {
                if (loaded != null && loaded.hasEmbedded && !loaded.hasUserFile) {
                    add { i, n ->
                        val useOnline = loaded.source == LyricsSource.ONLINE
                        SegmentedListItem(
                            checked = useOnline,
                            onCheckedChange = onUseOnline,
                            shapes = ListItemDefaults.segmentedShapes(index = i, count = n),
                            leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                            supportingContent = { Text("Skip the lyrics inside the song file") },
                            trailingContent = { Switch(checked = useOnline, onCheckedChange = null) },
                            colors = sheetRowColors()
                        ) { Text("Use online lyrics instead") }
                    }
                }
                add { i, n ->
                    ActionRow(i, n, Icons.Default.Search, "Search again", "Fix the title or artist and pick a match", onSearchAgain)
                }
                add { i, n ->
                    ActionRow(i, n, Icons.Default.FileOpen, "Load lyrics file", "Your own .lrc or .txt; it takes priority", onLoadFile)
                }
                if (loaded?.hasUserFile == true) {
                    add { i, n ->
                        ActionRow(i, n, Icons.Default.DeleteOutline, "Remove my file", "Go back to the other lyrics", onRemoveFile)
                    }
                }
                if (loaded?.lyrics?.synced == true) {
                    add { i, n -> SyncRow(i, n, loaded.offsetMs, onOffset) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                songRows.forEachIndexed { i, row -> row(i, songRows.size) }
            }

            SheetSectionTitle("All songs")
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                SegmentedListItem(
                    checked = settings.autoSearch,
                    onCheckedChange = { onSettingsChange(settings.copy(autoSearch = it)) },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 4),
                    leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
                    supportingContent = { Text("Find lyrics automatically when a song has none") },
                    trailingContent = { Switch(checked = settings.autoSearch, onCheckedChange = null) },
                    colors = sheetRowColors()
                ) { Text("Always search online") }
                ChoiceRow(
                    index = 1, count = 4, icon = Icons.Default.TextFields, title = "Text size",
                    options = LyricsTextSize.entries.map { it.label },
                    selected = settings.textSize.ordinal,
                    onSelect = { onSettingsChange(settings.copy(textSize = LyricsTextSize.entries[it])) }
                )
                ChoiceRow(
                    index = 2, count = 4,
                    icon = if (settings.alignCenter) Icons.Default.FormatAlignCenter else Icons.AutoMirrored.Filled.FormatAlignLeft,
                    title = "Alignment",
                    options = listOf("Left", "Centre"),
                    selected = if (settings.alignCenter) 1 else 0,
                    onSelect = { onSettingsChange(settings.copy(alignCenter = it == 1)) }
                )
                SegmentedListItem(
                    checked = settings.blur,
                    onCheckedChange = { onSettingsChange(settings.copy(blur = it)) },
                    shapes = ListItemDefaults.segmentedShapes(index = 3, count = 4),
                    leadingContent = { Icon(Icons.Default.BlurOn, contentDescription = null) },
                    supportingContent = { Text("Soften lines far from the one being sung") },
                    trailingContent = { Switch(checked = settings.blur, onCheckedChange = null) },
                    colors = sheetRowColors()
                ) { Text("Blur") }
            }
        }
    }
}

@Composable
private fun SheetSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun sheetRowColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
)

@Composable
private fun ActionRow(index: Int, count: Int, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(subtitle) },
        colors = sheetRowColors()
    ) { Text(title) }
}

/** Sync adjustment for this song in half-second steps. */
@Composable
private fun SyncRow(index: Int, count: Int, offsetMs: Long, onOffset: (Long) -> Unit) {
    val label = when {
        offsetMs == 0L -> "In time"
        offsetMs > 0 -> String.format(Locale.US, "%.1f s later", offsetMs / 1000.0)
        else -> String.format(Locale.US, "%.1f s earlier", -offsetMs / 1000.0)
    }
    SegmentedListItem(
        onClick = { if (offsetMs != 0L) onOffset(0L) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
        supportingContent = { Text(if (offsetMs == 0L) label else "$label · tap to reset") },
        trailingContent = {
            Row {
                IconButton(onClick = { onOffset(offsetMs - 500L) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Default.Remove, contentDescription = "Show lyrics earlier")
                }
                IconButton(onClick = { onOffset(offsetMs + 500L) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Default.Add, contentDescription = "Show lyrics later")
                }
            }
        },
        colors = sheetRowColors()
    ) { Text("Sync") }
}

/** A row with an Expressive connected button group for picking one option. */
@Composable
private fun ChoiceRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                options.forEachIndexed { i, option ->
                    ToggleButton(
                        checked = i == selected,
                        onCheckedChange = { if (i != selected) onSelect(i) },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(option, maxLines = 1) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Search again
// ---------------------------------------------------------------------------------------------

/** "Search again": edit the title / artist / album (pre-filled) and pick the right match. */
@Composable
private fun LyricsSearchSheet(
    track: AudioTrackItem,
    onPick: (LyricsSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(LyricsRepository.cleanTitle(track.title)) }
    var artist by remember {
        mutableStateOf(track.artist.takeUnless { it.equals("Unknown artist", true) }.orEmpty())
    }
    var album by remember {
        mutableStateOf(track.album.takeUnless { it.equals("Unknown album", true) }.orEmpty())
    }
    var searching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<LyricsSearchResult>>(emptyList()) }

    fun search() {
        if (title.isBlank()) return
        searching = true
        message = null
        scope.launch {
            try {
                results = LyricsRepository.searchManually(title, artist, album)
                if (results.isEmpty()) message = "No matches. Try a shorter title or just the main artist."
            } catch (_: LyricsNetworkException) {
                results = emptyList()
                message = "No internet connection."
            } catch (_: Exception) {
                results = emptyList()
                message = "Something went wrong. Try again."
            } finally {
                searching = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("lyrics_search_sheet")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            item {
                Column {
                    Text("Search lyrics", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                    Text(
                        "Messy song names are the usual reason nothing is found. Fix them and search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = title, onValueChange = { title = it }, label = { Text("Song title") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("lyrics_search_title"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = artist, onValueChange = { artist = it }, label = { Text("Artist") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("lyrics_search_artist"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = album, onValueChange = { album = it }, label = { Text("Album (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() })
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { search() },
                        enabled = !searching && title.isNotBlank(),
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().testTag("lyrics_search_go")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Search")
                    }
                    if (searching) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                    message?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                    if (results.isNotEmpty()) Spacer(Modifier.height(12.dp))
                }
            }
            itemsIndexed(results, key = { _, r -> r.id }) { i, r ->
                SegmentedListItem(
                    onClick = { onPick(r) },
                    shapes = ListItemDefaults.segmentedShapes(index = i, count = results.size),
                    supportingContent = {
                        val length = if (r.durationSec > 0) " · ${formatDurationMs(r.durationSec * 1000L)}" else ""
                        Text(
                            listOf(r.artistName, r.albumName).filter { it.isNotBlank() }.joinToString(" · ") + length,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        Text(
                            when {
                                r.hasSynced -> "Synced"
                                r.hasAnyLyrics -> "Plain"
                                else -> "Instrumental"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (r.hasSynced) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = sheetRowColors()
                ) { Text(r.trackName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}
