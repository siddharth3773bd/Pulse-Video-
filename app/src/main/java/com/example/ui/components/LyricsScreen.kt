@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Full-screen lyrics, with the same background, top bar and controls as Now Playing.
 * Lyrics come from the song itself, a saved choice, your own file, or LRCLIB online (searched
 * automatically unless "Always search online" is off). Every fix (search again, own file, sync)
 * is in the settings button at the top.
 */
@Composable
fun LyricsScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    positionMs: Long,
    settings: LyricsSettings,
    background: NowPlayingBackground,
    onSettingsChange: (LyricsSettings) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClose: () -> Unit
) {
    // Predictive back: Lyrics shrinks and follows the finger, showing Now Playing behind it.
    val back = rememberPredictiveBack(onBack = onClose)
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

    val colors = nowPlayingColors(background)
    // The controls step aside while the lyrics play; any touch brings them back.
    var controlsShown by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    val showingLyrics = handle.state is LyricsState.Loaded
    LaunchedEffect(lastTouch, isPlaying, showingLyrics) {
        controlsShown = true
        if (isPlaying && showingLyrics) {
            delay(4_000L)
            controlsShown = false
        }
    }
    val controlsInset by animateDpAsState(
        targetValue = if (controlsShown) 88.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "lyricsControlsInset"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPreview(back)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    lastTouch = System.currentTimeMillis()
                }
            }
            .testTag("lyrics_screen")
    ) {
        NowPlayingBackgroundLayer(
            style = background,
            track = track,
            drift = true,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NowPlayingTopBar(
                    colors = colors,
                    title = track.title,
                    subtitle = track.artist,
                    onClose = onClose,
                    closeLabel = "Close lyrics",
                    action = {
                        CompositionLocalProvider(LocalContentColor provides colors.content) {
                            TooltipIconButton(
                                label = "Lyrics settings",
                                icon = Symbols.Tune,
                                onClick = { showSettings = true },
                                modifier = Modifier.testTag("lyrics_settings_btn")
                            )
                        }
                    }
                )
            }

            // Lyrics, or a short message with what to do next.
            AnimatedContent(
                targetState = handle.state,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "lyricsState",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { state ->
                when (state) {
                    LyricsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator(color = colors.content, modifier = Modifier.testTag("lyrics_loading"))
                    }
                    is LyricsState.Loaded -> LyricsView(
                        lyrics = state.lyrics,
                        positionMs = positionMs,
                        isPlaying = isPlaying,
                        offsetMs = state.offsetMs,
                        settings = settings,
                        onSeekTo = onSeekTo,
                        contentColor = colors.content,
                        bottomInset = controlsInset,
                        modifier = Modifier.fillMaxSize()
                    )
                    // "Always search online" is off and the song has none: offer both ways.
                    LyricsState.NeedsChoice -> MessagePanel(
                        colors = colors,
                        icon = Symbols.Lyrics,
                        title = "No lyrics yet",
                        body = "Search LRCLIB, a free online lyrics library, or load your own .lrc or .txt file.",
                        primary = "Search online" to { handle.reload(true) },
                        secondary = "Load file" to loadFile
                    )
                    LyricsState.NotFound -> MessagePanel(
                        colors = colors,
                        icon = Symbols.Search,
                        title = "No lyrics found",
                        body = "LRCLIB doesn't have this song under this name. Fix the title or artist with Search again, or load your own file.",
                        primary = "Search again" to { showSearch = true },
                        secondary = "Load file" to loadFile
                    )
                    LyricsState.NoInternet -> MessagePanel(
                        colors = colors,
                        icon = Symbols.WifiOff,
                        title = "No internet connection",
                        body = "Connect and try again, or load your own lyrics file.",
                        primary = "Retry" to { handle.reload(true) },
                        secondary = "Load file" to loadFile
                    )
                    LyricsState.Instrumental -> MessagePanel(
                        colors = colors,
                        icon = Symbols.MusicNote,
                        title = "Instrumental",
                        body = "This song has no lyrics.",
                        primary = "Search again" to { showSearch = true },
                        secondary = null
                    )
                }
            }

        }

        // Controls float over the bottom of the lyrics (the lyrics leave room there anyway).
        AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 2 },
            exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) +
                slideOutVertically(MaterialTheme.motionScheme.slowSpatialSpec()) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NowPlayingControls(
                isPlaying = isPlaying,
                colors = colors,
                onPrevious = onSkipPrevious,
                onPlayPause = onTogglePlayPause,
                onNext = onSkipNext,
                compact = true,
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                    .testTag("lyrics_controls")
            )
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

/** A short message in the middle of the lyrics screen with one or two buttons. */
@Composable
private fun MessagePanel(
    colors: NowPlayingColors,
    icon: ImageVector,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("lyrics_message"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.content, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.contentVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 380.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = primary.second,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.main, contentColor = colors.onMain),
                modifier = Modifier.testTag("lyrics_primary_action")
            ) { Text(primary.first) }
            if (secondary != null) {
                FilledTonalButton(
                    onClick = secondary.second,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.soft, contentColor = colors.onSoft),
                    modifier = Modifier.testTag("lyrics_secondary_action")
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
                    text = "From ${loaded.source.label}" + if (loaded.lyrics.synced) " · Synced" else " · Not synced",
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
                            leadingContent = { Icon(Symbols.Cloud, contentDescription = null) },
                            supportingContent = { Text("Skip the lyrics inside the song file") },
                            trailingContent = { Switch(checked = useOnline, onCheckedChange = null) },
                            colors = sheetRowColors()
                        ) { Text("Use online lyrics instead") }
                    }
                }
                add { i, n ->
                    ActionRow(i, n, Symbols.Search, "Search again", "Fix the title or artist and pick a match", onSearchAgain)
                }
                add { i, n ->
                    ActionRow(i, n, Symbols.FileOpen, "Load lyrics file", "Your own .lrc or .txt; it takes priority", onLoadFile)
                }
                if (loaded?.hasUserFile == true) {
                    add { i, n ->
                        ActionRow(i, n, Symbols.DeleteOutline, "Remove my file", "Go back to the other lyrics", onRemoveFile)
                    }
                }
                if (loaded?.lyrics?.synced == true) {
                    add { i, n -> SyncRow(i, n, loaded.offsetMs, onOffset) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                songRows.forEachIndexed { i, row -> row(i, songRows.size) }
            }

            SheetSectionTitle("Look")
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ChoiceRow(
                    index = 0, count = 4, icon = Symbols.TextFields, title = "Text size",
                    options = LyricsTextSize.entries.map { it.label },
                    selected = settings.textSize.ordinal,
                    onSelect = { onSettingsChange(settings.copy(textSize = LyricsTextSize.entries[it])) }
                )
                ChoiceRow(
                    index = 1, count = 4,
                    icon = if (settings.alignCenter) Symbols.FormatAlignCenter else Symbols.FormatAlignLeft,
                    title = "Alignment",
                    options = listOf("Left", "Centre"),
                    selected = if (settings.alignCenter) 1 else 0,
                    onSelect = { onSettingsChange(settings.copy(alignCenter = it == 1)) }
                )
                SwitchRow(
                    index = 2, count = 4, icon = Symbols.BlurOn, title = "Blur",
                    subtitle = "Soften lines far from the one being sung",
                    checked = settings.blur,
                    onCheckedChange = { onSettingsChange(settings.copy(blur = it)) }
                )
                SwitchRow(
                    index = 3, count = 4, icon = Symbols.Translate, title = "Show in English letters",
                    subtitle = "Hindi, Urdu, Punjabi and other scripts, under each line",
                    checked = settings.romanize,
                    onCheckedChange = { onSettingsChange(settings.copy(romanize = it)) }
                )
            }

            SheetSectionTitle("Online")
            SwitchRow(
                index = 0, count = 1, icon = Symbols.Cloud, title = "Always search online",
                subtitle = "Find lyrics on LRCLIB when a song has none of its own",
                checked = settings.autoSearch,
                onCheckedChange = { onSettingsChange(settings.copy(autoSearch = it)) }
            )
        }
    }
}

/** A grouped row with an on/off switch; the whole row toggles it. */
@Composable
private fun SwitchRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = sheetRowColors(),
        modifier = Modifier.testTag("lyrics_switch_${title.lowercase().replace(' ', '_')}")
    ) { Text(title) }
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
        leadingContent = { Icon(Symbols.Sync, contentDescription = null) },
        supportingContent = { Text(if (offsetMs == 0L) label else "$label · tap to reset") },
        trailingContent = {
            Row {
                IconButton(onClick = { onOffset(offsetMs - 500L) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Symbols.Remove, contentDescription = "Show lyrics earlier")
                }
                IconButton(onClick = { onOffset(offsetMs + 500L) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Symbols.Add, contentDescription = "Show lyrics later")
                }
            }
        },
        colors = sheetRowColors()
    ) { Text("Sync") }
}

/** A grouped row with the Expressive standard button group for picking one option. */
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
            Spacer(Modifier.height(12.dp))
            ChoiceButtonGroup(
                options = options.indices.map { it to options[it] },
                selected = selected,
                onSelect = { if (it != selected) onSelect(it) },
                tag = { "lyrics_choice_${title.lowercase().replace(' ', '_')}_$it" }
            )
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
                        Icon(Symbols.Search, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
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
