@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferencesRepository
import com.example.model.AudioTrackItem
import com.example.model.DecoderMode
import com.example.model.LibraryPreferences
import com.example.model.LibraryTab
import com.example.model.NowPlayingAction
import com.example.model.NowPlayingBackground
import com.example.model.NowPlayingColorSource
import com.example.model.PlaybackSettings
import com.example.model.ScreenOrientationMode
import com.example.model.SidewaysMode
import com.example.model.ThemeMode
import com.example.ui.icons.Symbols
import com.example.ui.theme.AppColorPresets
import com.example.ui.theme.CustomColorId
import com.example.ui.theme.colorToHex
import com.example.ui.theme.hueOf
import com.example.ui.theme.hueRainbow
import com.example.ui.theme.parseHexColorOrNull
import com.example.ui.theme.schemeFromSeed
import com.example.ui.theme.seedForHue
import com.example.viewmodel.VideoUiState
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Appearance
// ---------------------------------------------------------------------------------------------

/** Theme (system / light / dark, pure black) and colours (wallpaper, a preset or your own). */
@Composable
internal fun AppearancePage(
    uiState: VideoUiState,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSelectColorTheme: (String) -> Unit,
    onSelectCustomColor: (Int) -> Unit,
    onToggleAmoledBlack: () -> Unit
) {
    var showCustomColour by remember { mutableStateOf(false) }

    SettingsPage(title = "Appearance", onBack = onBack, modifier = Modifier.testTag("me_subpage_appearance")) {
        settingsGroup(
            key = "theme",
            title = "Theme",
            rows = buildList {
                add(
                    SettingRow.Custom("theme_mode") {
                        LabelledControl(icon = Symbols.Contrast, title = "Theme") {
                            ChoiceButtonGroup(
                                options = ThemeMode.entries.map { it to it.label },
                                selected = uiState.themeMode,
                                onSelect = onSelectThemeMode,
                                tag = { "me_theme_mode_${it.id}" }
                            )
                        }
                    }
                )
                // Pure black only matters when the app can be dark.
                if (uiState.themeMode != ThemeMode.LIGHT) {
                    add(
                        SettingRow.Toggle(
                            key = "amoled",
                            icon = Symbols.DarkMode,
                            title = "Pure black",
                            summary = "Black background in dark theme",
                            checked = uiState.amoledBlack,
                            onCheckedChange = { onToggleAmoledBlack() }
                        )
                    )
                }
            }
        )
        settingsGroup(
            key = "colours",
            title = "Colours",
            rows = buildList {
                add(
                    SettingRow.Toggle(
                        key = "dynamic",
                        icon = Symbols.Wallpaper,
                        title = "Wallpaper colours",
                        summary = "The app takes its colours from your wallpaper (Material You)",
                        checked = uiState.dynamicColor,
                        onCheckedChange = onSetDynamicColor
                    )
                )
                if (!uiState.dynamicColor) {
                    add(
                        SettingRow.Custom("swatches") {
                            ColourSwatches(
                                selectedId = uiState.colorTheme,
                                customArgb = uiState.customColorArgb,
                                onSelect = onSelectColorTheme,
                                onCustom = { showCustomColour = true }
                            )
                        }
                    )
                }
            }
        )
    }

    if (showCustomColour) {
        CustomColourDialog(
            initialArgb = uiState.customColorArgb,
            onApply = { argb ->
                onSelectCustomColor(argb)
                showCustomColour = false
            },
            onDismiss = { showCustomColour = false }
        )
    }
}

/** An icon and title, with a control (button group) underneath, lined up like the list rows. */
@Composable
private fun LabelledControl(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    control: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        control()
    }
}

/**
 * The colour choices as round swatches, like Android's own "Wallpaper & style": each one is split
 * into the main colours the app will really get from it. The selected one has a ring and a tick.
 * "Custom" is a rainbow: tap it to pick any colour.
 */
@Composable
private fun ColourSwatches(
    selectedId: String,
    customArgb: Int,
    onSelect: (String) -> Unit,
    onCustom: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Symbols.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text("Colour", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 6
        ) {
            AppColorPresets.forEach { preset ->
                val seed = preset.seed.toArgb()
                val scheme = remember(seed) { schemeFromSeed(seed, dark = true) }
                Swatch(
                    label = preset.label,
                    selected = selectedId == preset.id,
                    onClick = { onSelect(preset.id) },
                    tag = "appearance_swatch_${preset.id}"
                ) { drawSplit(scheme.primary, scheme.secondary, scheme.tertiary) }
            }
            val customScheme = remember(customArgb) { schemeFromSeed(customArgb, dark = true) }
            val rainbow = remember { hueRainbow() }
            val customSelected = selectedId == CustomColorId
            Swatch(
                label = "Custom",
                selected = customSelected,
                onClick = onCustom,
                tag = "appearance_swatch_custom"
            ) {
                if (customSelected) {
                    drawSplit(customScheme.primary, customScheme.secondary, customScheme.tertiary)
                } else {
                    drawCircle(brush = Brush.sweepGradient(rainbow))
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    draw: DrawScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val inner by animateDpAsState(
        targetValue = if (selected) 40.dp else 48.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "swatchSize"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .then(if (selected) Modifier.border(2.dp, scheme.primary, CircleShape) else Modifier)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .semantics { this.selected = selected }
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(inner)) { draw() }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Symbols.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Top half [top], bottom-left [bottomLeft], bottom-right [bottomRight]. */
private fun DrawScope.drawSplit(top: Color, bottomLeft: Color, bottomRight: Color) {
    drawArc(color = top, startAngle = 180f, sweepAngle = 180f, useCenter = true)
    drawArc(color = bottomLeft, startAngle = 90f, sweepAngle = 90f, useCenter = true)
    drawArc(color = bottomRight, startAngle = 0f, sweepAngle = 90f, useCenter = true)
}

/**
 * Pick your own colour. Material You uses only the colour's hue (it chooses how colourful and how
 * light the app's colours are itself), so there is one hue slider, a preview of the colours the
 * app will really get, and a hex box for an exact colour.
 */
@Composable
private fun CustomColourDialog(initialArgb: Int, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    var hue by remember { mutableFloatStateOf(hueOf(initialArgb)) }
    var hex by remember { mutableStateOf(colorToHex(Color(initialArgb))) }
    val seed = seedForHue(hue)
    val preview = remember(seed, dark) { schemeFromSeed(seed, dark) }
    val rainbow = remember { hueRainbow() }
    val sliderState = remember { SliderState(value = hue, trackRange = 0f..360f) }
    sliderState.value = hue

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Symbols.Palette, contentDescription = null) },
        title = { Text("Custom colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // What the app will look like: its main, soft and accent colours.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PreviewDot(preview.primary, preview.onPrimary, "Main")
                    PreviewDot(preview.secondaryContainer, preview.onSecondaryContainer, "Soft")
                    PreviewDot(preview.tertiary, preview.onTertiary, "Accent")
                }
                Slider(
                    state = sliderState,
                    onValueChange = { value ->
                        hue = value
                        hex = colorToHex(Color(seedForHue(value)))
                    },
                    colors = SliderDefaults.colors(thumbColor = preview.primary),
                    track = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(rainbow))
                        )
                    },
                    modifier = Modifier.testTag("custom_color_hue_slider")
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { next ->
                        hex = next.uppercase().take(7)
                        parseHexColorOrNull(hex)?.let { hue = hueOf(it.toArgb()) }
                    },
                    singleLine = true,
                    label = { Text("Hex code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_color_hex_input")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(seed) }, shapes = ButtonDefaults.shapes()) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
        }
    )
}

@Composable
private fun PreviewDot(color: Color, onColor: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", style = MaterialTheme.typography.labelLarge, color = onColor)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current)
    }
}

// ---------------------------------------------------------------------------------------------
// Video
// ---------------------------------------------------------------------------------------------

/** Video player settings in four groups: gestures, playback, display, advanced. */
@Composable
internal fun VideoSettingsPage(
    settings: PlaybackSettings,
    onBack: () -> Unit,
    onUpdateSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit
) {
    var sheet by remember { mutableStateOf<String?>(null) }

    SettingsPage(title = "Video", onBack = onBack, modifier = Modifier.testTag("me_subpage_video_settings")) {
        settingsGroup(
            key = "gestures",
            title = "Gestures",
            rows = buildList {
                add(
                    SettingRow.Toggle(
                        key = "double_tap_seek",
                        icon = Symbols.FastForward,
                        title = "Double tap to skip",
                        summary = "Left side goes back, right side forward",
                        checked = settings.doubleTapToSeek,
                        onCheckedChange = { on -> onUpdateSettings { it.copy(doubleTapToSeek = on) } }
                    )
                )
                // How far a double tap skips: only there while double tap to skip is on.
                if (settings.doubleTapToSeek) {
                    add(
                        SettingRow.Custom("skip_time") {
                            LabelledControl(
                                icon = Symbols.Timer,
                                title = "Skip time",
                                summary = "How far each double tap goes"
                            ) {
                                ChoiceButtonGroup(
                                    options = listOf(5, 10, 15, 20, 30).map { it to "${it}s" },
                                    selected = settings.skipDurationSec,
                                    onSelect = { sec -> onUpdateSettings { it.copy(skipDurationSec = sec) } },
                                    tag = { "setting_skip_$it" }
                                )
                            }
                        }
                    )
                }
                addAll(
                    listOf(
                        SettingRow.Toggle(
                            key = "double_tap_pause",
                            icon = Symbols.TouchApp,
                            title = "Double tap to pause",
                            summary = "In the middle of the screen",
                            checked = settings.doubleTapToPause,
                            onCheckedChange = { on -> onUpdateSettings { it.copy(doubleTapToPause = on) } }
                        ),
                        SettingRow.Toggle(
                            key = "hold_2x",
                            icon = Symbols.Speed,
                            title = "Hold for 2× speed",
                            summary = "Plays faster while you hold",
                            checked = settings.longPressFor2xSpeed,
                            onCheckedChange = { on -> onUpdateSettings { it.copy(longPressFor2xSpeed = on) } }
                        ),
                        SettingRow.Toggle(
                            key = "swipe_seek",
                            icon = Symbols.Swipe,
                            title = "Swipe to seek",
                            summary = "Slide sideways across the video",
                            checked = settings.horizontalSwipeToSeek,
                            onCheckedChange = { on -> onUpdateSettings { it.copy(horizontalSwipeToSeek = on) } }
                        ),
                        SettingRow.Toggle(
                            key = "swipe_brightness",
                            icon = Symbols.Brightness,
                            title = "Brightness swipe",
                            summary = "Up and down on the left half",
                            checked = settings.leftSwipeForBrightness,
                            onCheckedChange = { on -> onUpdateSettings { it.copy(leftSwipeForBrightness = on) } }
                        ),
                        SettingRow.Toggle(
                            key = "swipe_volume",
                            icon = Symbols.VolumeUp,
                            title = "Volume swipe",
                            summary = "Up and down on the right half",
                            checked = settings.rightSwipeForVolume,
                            onCheckedChange = { on -> onUpdateSettings { it.copy(rightSwipeForVolume = on) } }
                        )
                    )
                )
            }
        )
        settingsGroup(
            key = "playback",
            title = "Playback",
            rows = listOf(
                SettingRow.Toggle(
                    key = "remember_position",
                    icon = Symbols.History,
                    title = "Carry on where you stopped",
                    summary = "With a Start over button",
                    checked = settings.rememberPosition,
                    onCheckedChange = { on -> onUpdateSettings { it.copy(rememberPosition = on) } }
                ),
                SettingRow.Toggle(
                    key = "autoplay",
                    icon = Symbols.Autoplay,
                    title = "Play next automatically",
                    summary = "The next video in the list",
                    checked = settings.autoplayNext,
                    onCheckedChange = { on -> onUpdateSettings { it.copy(autoplayNext = on) } }
                ),
                SettingRow.Toggle(
                    key = "pip",
                    icon = Symbols.PictureInPictureAlt,
                    title = "Picture-in-picture",
                    summary = "Keeps playing in a small window when you leave",
                    checked = settings.pictureInPicture,
                    onCheckedChange = { on -> onUpdateSettings { it.copy(pictureInPicture = on) } }
                ),
                SettingRow.Choice(
                    key = "audio_language",
                    icon = Symbols.Language,
                    title = "Audio language",
                    value = languageLabel(settings.preferredAudioLanguage, "Video's own"),
                    onClick = { sheet = "audio" }
                ),
                SettingRow.Choice(
                    key = "subtitle_language",
                    icon = Symbols.Subtitles,
                    title = "Subtitles",
                    value = languageLabel(settings.preferredSubtitleLanguage, "Off"),
                    onClick = { sheet = "subtitles" }
                )
            )
        )
        settingsGroup(
            key = "display",
            title = "Display",
            rows = listOf(
                SettingRow.Toggle(
                    key = "time_left",
                    icon = Symbols.Timer,
                    title = "Show time left",
                    summary = "Instead of the full length",
                    checked = settings.showRemainingTime,
                    onCheckedChange = { on -> onUpdateSettings { it.copy(showRemainingTime = on) } }
                ),
                SettingRow.Toggle(
                    key = "colourful_controls",
                    icon = Symbols.ColorLens,
                    title = "Colourful controls",
                    summary = "Play button and seek bar in your theme colour",
                    checked = settings.colorfulControls,
                    onCheckedChange = { on -> onUpdateSettings { it.copy(colorfulControls = on) } }
                ),
                SettingRow.Choice(
                    key = "orientation",
                    icon = Symbols.ScreenRotation,
                    title = "Screen orientation",
                    value = settings.forceOrientation.label,
                    onClick = { sheet = "orientation" }
                )
            )
        )
        settingsGroup(
            key = "advanced",
            title = "Advanced",
            rows = listOf(
                SettingRow.Choice(
                    key = "decoder",
                    icon = Symbols.Memory,
                    title = "Decoder",
                    value = decoderLabel(settings.defaultDecoder),
                    onClick = { sheet = "decoder" }
                )
            )
        )
    }

    when (sheet) {
        "audio" -> ChoiceSheet(
            title = "Audio language",
            options = listOf(ChoiceOption("", "Video's own", "Whatever the video plays first")) +
                PreferredLanguages.map { (code, name) -> ChoiceOption(code, name) },
            selected = settings.preferredAudioLanguage,
            onSelect = { code ->
                onUpdateSettings { it.copy(preferredAudioLanguage = code) }
                sheet = null
            },
            onDismiss = { sheet = null }
        )
        "subtitles" -> ChoiceSheet(
            title = "Subtitles",
            options = listOf(ChoiceOption("", "Off", "Turn them on in the player when you want")) +
                PreferredLanguages.map { (code, name) -> ChoiceOption(code, name) },
            selected = settings.preferredSubtitleLanguage,
            onSelect = { code ->
                onUpdateSettings { it.copy(preferredSubtitleLanguage = code) }
                sheet = null
            },
            onDismiss = { sheet = null }
        )
        "orientation" -> ChoiceSheet(
            title = "Screen orientation",
            options = ScreenOrientationMode.entries.map { mode ->
                ChoiceOption(
                    mode,
                    mode.label,
                    when (mode) {
                        ScreenOrientationMode.SYSTEM -> "Follows how you hold the phone"
                        ScreenOrientationMode.PORTRAIT -> "Always upright"
                        ScreenOrientationMode.LANDSCAPE -> "Always sideways"
                    }
                )
            },
            selected = settings.forceOrientation,
            onSelect = { mode ->
                onUpdateSettings { it.copy(forceOrientation = mode) }
                sheet = null
            },
            onDismiss = { sheet = null }
        )
        "decoder" -> ChoiceSheet(
            title = "Decoder",
            options = DecoderMode.entries.map { mode ->
                ChoiceOption(
                    mode,
                    decoderLabel(mode),
                    when (mode) {
                        DecoderMode.AUTO -> "Your phone's chip when it can, otherwise software"
                        DecoderMode.HARDWARE -> "Only your phone's chip: smooth and battery-friendly"
                        DecoderMode.SOFTWARE -> "Only Android's built-in software decoders: try it if a video shows glitches"
                    }
                )
            },
            selected = settings.defaultDecoder,
            onSelect = { mode ->
                onUpdateSettings { it.copy(defaultDecoder = mode) }
                sheet = null
            },
            onDismiss = { sheet = null }
        )
    }
}

private fun decoderLabel(mode: DecoderMode): String = when (mode) {
    DecoderMode.AUTO -> "Automatic (recommended)"
    DecoderMode.HARDWARE -> "Hardware"
    DecoderMode.SOFTWARE -> "Software"
}

// ---------------------------------------------------------------------------------------------
// Music
// ---------------------------------------------------------------------------------------------

/** Music settings: library tabs and Now Playing buttons, its look, ambient mode, hidden items. */
@Composable
internal fun MusicSettingsPage(audioTracks: List<AudioTrackItem>, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by remember(context) {
        AppPreferencesRepository.observeLibraryPreferences(context)
    }.collectAsState(initial = LibraryPreferences())

    var showTabsSheet by remember { mutableStateOf(false) }
    var showTraySheet by remember { mutableStateOf(false) }
    var lookChoice by remember { mutableStateOf<String?>(null) }
    var showHiddenSongs by remember { mutableStateOf(false) }
    var showHiddenFolders by remember { mutableStateOf(false) }
    val musicFolders = remember(audioTracks) {
        audioTracks.groupBy { it.folderPath }
            .filterKeys { it.isNotBlank() }
            .map { (path, list) -> MusicFolder(path = path, songCount = list.size) }
    }

    SettingsPage(title = "Music", onBack = onBack, modifier = Modifier.testTag("me_subpage_music_settings")) {
        settingsGroup(
            key = "library",
            title = "Library",
            rows = listOf(
                SettingRow.Link(
                    key = "library_tabs",
                    icon = Symbols.ViewCarousel,
                    title = "Library tabs",
                    summary = "Show, hide and reorder Songs, Albums, Artists, Playlists",
                    onClick = { showTabsSheet = true }
                ),
                SettingRow.Link(
                    key = "now_playing_buttons",
                    icon = Symbols.Tune,
                    title = "Now Playing buttons",
                    summary = "Pick and order the buttons under the controls",
                    onClick = { showTraySheet = true }
                )
            )
        )
        settingsGroup(
            key = "look",
            title = "Now Playing",
            rows = listOf(
                SettingRow.Choice(
                    key = "colours_from",
                    icon = Symbols.ColorLens,
                    title = "Colours from",
                    value = prefs.nowPlayingColorSource.label,
                    onClick = { lookChoice = "colors" }
                ),
                SettingRow.Choice(
                    key = "background",
                    icon = Symbols.Gradient,
                    title = "Background",
                    value = prefs.nowPlayingBackground.label,
                    onClick = { lookChoice = "background" }
                ),
                SettingRow.Toggle(
                    key = "keep_screen_on_lyrics",
                    icon = Symbols.Lyrics,
                    title = "Keep screen on in lyrics",
                    summary = "The screen won't turn off while you read along",
                    checked = prefs.keepScreenOnLyrics,
                    onCheckedChange = { on -> scope.launch { AppPreferencesRepository.setLyricsKeepScreenOn(context, on) } }
                ),
                SettingRow.Toggle(
                    key = "keep_screen_on",
                    icon = Symbols.LightMode,
                    title = "Keep screen on in Now Playing",
                    summary = "Otherwise the screen turns off as usual",
                    checked = prefs.keepScreenOnNowPlaying,
                    onCheckedChange = { on -> scope.launch { AppPreferencesRepository.setNowPlayingKeepScreenOn(context, on) } }
                ),
                SettingRow.Choice(
                    key = "ambient_rotate",
                    icon = Symbols.ScreenRotation,
                    title = "When the phone turns sideways",
                    value = prefs.ambient.whenSideways.label,
                    onClick = { lookChoice = "sideways" }
                )
            )
        )
        settingsGroup(
            key = "hide",
            title = "Hide from library",
            rows = listOf(
                SettingRow.Toggle(
                    key = "hide_short",
                    icon = Symbols.Timer,
                    title = "Short clips",
                    summary = "Under 30 seconds, like ringtones and notification sounds",
                    checked = prefs.hideShortAudio,
                    onCheckedChange = { on -> scope.launch { AppPreferencesRepository.setLibraryHideShortAudio(context, on) } }
                ),
                SettingRow.Toggle(
                    key = "hide_recordings",
                    icon = Symbols.AudioFile,
                    title = "Voice recordings",
                    summary = "From recorder and call-recording apps",
                    checked = prefs.hideRecordings,
                    onCheckedChange = { on -> scope.launch { AppPreferencesRepository.setLibraryHideRecordings(context, on) } }
                ),
                SettingRow.Link(
                    key = "hidden_songs",
                    icon = Symbols.Block,
                    title = "Hidden songs",
                    summary = when (val n = prefs.blacklistedSongs.size) {
                        0 -> "None. Hide a song from its ⋮ menu"
                        1 -> "1 song"
                        else -> "$n songs"
                    },
                    onClick = { showHiddenSongs = true }
                ),
                SettingRow.Link(
                    key = "hidden_folders",
                    icon = Symbols.FolderOff,
                    title = "Hidden folders",
                    summary = when (val n = prefs.blacklistedFolders.size) {
                        0 -> "None. Hide whole folders, like WhatsApp audio"
                        1 -> "1 folder"
                        else -> "$n folders"
                    },
                    onClick = { showHiddenFolders = true }
                )
            )
        )
    }

    if (showHiddenSongs) {
        BlacklistedSongsSheet(
            songs = prefs.blacklistedSongs,
            onRestore = { id -> scope.launch { AppPreferencesRepository.removeBlacklistedSong(context, id) } },
            onDismiss = { showHiddenSongs = false }
        )
    }
    if (showHiddenFolders) {
        BlacklistedFoldersSheet(
            folders = musicFolders,
            blacklisted = prefs.blacklistedFolders,
            onSetBlacklisted = { path, hidden ->
                scope.launch { AppPreferencesRepository.setFolderBlacklisted(context, path, hidden) }
            },
            onDismiss = { showHiddenFolders = false }
        )
    }
    when (lookChoice) {
        "colors" -> ChoiceSheet(
            title = "Colours from",
            options = NowPlayingColorSource.entries.map { ChoiceOption(it, it.label, it.description) },
            selected = prefs.nowPlayingColorSource,
            onSelect = { value ->
                scope.launch { AppPreferencesRepository.setNowPlayingColorSource(context, value) }
                lookChoice = null
            },
            onDismiss = { lookChoice = null }
        )
        "sideways" -> ChoiceSheet(
            title = "When the phone turns sideways",
            options = SidewaysMode.entries.map { ChoiceOption(it, it.label, it.description) },
            selected = prefs.ambient.whenSideways,
            onSelect = { value ->
                scope.launch {
                    AppPreferencesRepository.updateAmbientSettings(context, prefs.ambient.copy(whenSideways = value))
                }
                lookChoice = null
            },
            onDismiss = { lookChoice = null }
        )
        "background" -> ChoiceSheet(
            title = "Background",
            options = NowPlayingBackground.entries.map { ChoiceOption(it, it.label, it.description) },
            selected = prefs.nowPlayingBackground,
            onSelect = { value ->
                scope.launch { AppPreferencesRepository.setNowPlayingBackground(context, value) }
                lookChoice = null
            },
            onDismiss = { lookChoice = null }
        )
    }
    if (showTabsSheet) {
        ManageTabsSheet(
            prefs = prefs,
            onChange = { order, hidden ->
                scope.launch { AppPreferencesRepository.updateLibraryTabs(context, order, hidden) }
            },
            onReset = {
                scope.launch { AppPreferencesRepository.updateLibraryTabs(context, LibraryTab.entries, emptySet()) }
            },
            onDismiss = { showTabsSheet = false }
        )
    }
    if (showTraySheet) {
        NowPlayingTraySheet(
            order = prefs.trayOrder,
            hidden = prefs.hiddenTray,
            onChange = { order, hidden ->
                scope.launch { AppPreferencesRepository.updateLibraryTray(context, order, hidden) }
            },
            onReset = {
                scope.launch { AppPreferencesRepository.updateLibraryTray(context, NowPlayingAction.entries, emptySet()) }
            },
            onDismiss = { showTraySheet = false }
        )
    }
}

/** Languages offered for the preferred audio / subtitle language (ISO 639-1 codes). */
private val PreferredLanguages: List<Pair<String, String>> = listOf(
    "en" to "English",
    "hi" to "Hindi",
    "mr" to "Marathi",
    "bn" to "Bengali",
    "ta" to "Tamil",
    "te" to "Telugu",
    "kn" to "Kannada",
    "ml" to "Malayalam",
    "gu" to "Gujarati",
    "pa" to "Punjabi",
    "ur" to "Urdu",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese",
    "ru" to "Russian",
    "ar" to "Arabic",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese"
)

private fun languageLabel(code: String, blankLabel: String): String =
    if (code.isBlank()) blankLabel else PreferredLanguages.firstOrNull { it.first == code }?.second ?: code
