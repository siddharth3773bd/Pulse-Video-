@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lyrics.LyricsSettings
import com.example.lyrics.LyricsState
import com.example.model.AmbientSettings
import com.example.model.AudioTrackItem
import com.example.model.NowPlayingBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ambient mode: a calm, full-screen, always-on view of the current song, on the same background
 * as Now Playing. It opens in landscape when started from the button ([forceLandscape]); the
 * screen stays on and the system bars hide while it is open. There are no buttons on screen:
 * double-tap the artwork to play/pause and swipe it to change song (anywhere on screen when the
 * artwork is hidden). The ⋮ in the corner opens the options.
 */
@Composable
fun AmbientModeScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    queue: List<AudioTrackItem>,
    queueIndex: Int,
    settings: AmbientSettings,
    background: NowPlayingBackground,
    forceLandscape: Boolean,
    onSettingsChange: (AmbientSettings) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipToQueueItem: (Int) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClose: () -> Unit,
    positionMs: Long,
    lyricsSettings: LyricsSettings,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    KeepScreenOnAndHideSystemBars()
    LockLandscape(enabled = forceLandscape)

    var showOptions by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = nowPlayingColors(background)

    Box(modifier = modifier.fillMaxSize().testTag("ambient_mode")) {
        NowPlayingBackgroundLayer(style = background, track = track, modifier = Modifier.matchParentSize())

        val showArt = settings.showArtwork
        val showTitle = showArt && settings.showTitle
        val showArtist = showArt && settings.showArtist
        val showLyrics = settings.showLyrics

        val art: @Composable (Modifier) -> Unit = { mod ->
            ArtworkPager(
                track = track,
                queue = queue,
                queueIndex = queueIndex,
                isPlaying = isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onSkipToQueueItem = onSkipToQueueItem,
                placeholderBackground = colors.soft,
                placeholderTint = colors.content,
                modifier = mod,
                cornerRadius = 28.dp,
                testTag = "ambient_artwork"
            )
        }
        val info: @Composable () -> Unit = {
            if (showTitle) {
                Text(
                    text = track.title,
                    color = colors.content,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showArtist) {
                Text(
                    text = track.artist,
                    color = colors.contentVariant,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        // Ambient never asks anything: it follows the saved lyrics choice quietly.
        val lyricsHandle = if (showLyrics) rememberLyricsState(track, lyricsSettings) else null
        val lyrics: @Composable (Modifier, TextAlign) -> Unit = { mod, align ->
            Box(modifier = mod, contentAlignment = Alignment.CenterStart) {
                when (val st = lyricsHandle?.state) {
                    is LyricsState.Loaded -> LyricsView(
                        lyrics = st.lyrics,
                        positionMs = positionMs,
                        isPlaying = isPlaying,
                        offsetMs = st.offsetMs,
                        settings = lyricsSettings.copy(alignCenter = align == TextAlign.Center),
                        onSeekTo = onSeekTo,
                        contentColor = colors.content,
                        modifier = Modifier.fillMaxSize(),
                        horizontalPadding = 0
                    )
                    null, LyricsState.Loading -> Unit
                    else -> Text(
                        text = if (st == LyricsState.Instrumental) "Instrumental" else "No lyrics",
                        color = colors.content.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Room around everything, clear of the camera cut-out.
        val safe = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout)
        when {
            // Lyrics only: they fill the screen and the gestures work anywhere.
            !showArt -> LyricsOnlyGestures(
                isPlaying = isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                modifier = safe
            ) {
                lyrics(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 72.dp, vertical = 40.dp),
                    TextAlign.Center
                )
            }
            landscape -> {
                val sideContent = showTitle || showArtist || showLyrics
                Row(
                    modifier = safe.padding(horizontal = 48.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (sideContent) Arrangement.Start else Arrangement.Center
                ) {
                    // Square artwork as tall as the screen allows.
                    art(
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    )
                    if (sideContent) {
                        Spacer(modifier = Modifier.width(48.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = if (showLyrics) Arrangement.Top else Arrangement.Center
                        ) {
                            info()
                            if (showLyrics) {
                                if (showTitle || showArtist) Spacer(modifier = Modifier.height(24.dp))
                                lyrics(Modifier.weight(1f).fillMaxWidth(), TextAlign.Start)
                            }
                        }
                    }
                }
            }
            else -> Column(
                modifier = safe.padding(horizontal = 32.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.Start
            ) {
                art(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                if (showTitle || showArtist) {
                    Spacer(modifier = Modifier.height(28.dp))
                    info()
                }
                if (showLyrics) {
                    Spacer(modifier = Modifier.height(24.dp))
                    lyrics(Modifier.weight(1f).fillMaxWidth(), TextAlign.Start)
                }
            }
        }

        // The only thing on screen besides the song: ⋮ for the options.
        IconButton(
            onClick = { showOptions = true },
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.content.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(8.dp)
                .testTag("ambient_options_btn")
        ) {
            Icon(Symbols.MoreVert, contentDescription = "Ambient mode options")
        }

        if (showOptions) {
            AmbientOptionsSheet(
                settings = settings,
                onChange = onSettingsChange,
                onExit = {
                    showOptions = false
                    onClose()
                },
                onDismiss = { showOptions = false }
            )
        }
    }
}

/** Turns the screen sideways while shown (when [enabled]) and gives the old orientation back after. */
@Composable
private fun LockLandscape(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val activity = view.context.findActivity()
        if (!enabled || activity == null) return@DisposableEffect onDispose { }
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity.requestedOrientation = previous }
    }
}

/** With the artwork hidden, the whole screen takes the gestures, with the same flash feedback. */
@Composable
private fun LyricsOnlyGestures(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isPlayingLatest by rememberUpdatedState(isPlaying)
    val toggle by rememberUpdatedState(onTogglePlayPause)
    val next by rememberUpdatedState(onSkipNext)
    val previous by rememberUpdatedState(onSkipPrevious)
    val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    var flashIcon by remember { mutableStateOf<ImageVector?>(null) }
    val flashAlpha = remember { Animatable(0f) }

    fun flash(icon: ImageVector) {
        flashIcon = icon
        scope.launch {
            flashAlpha.snapTo(1f)
            delay(450)
            flashAlpha.animateTo(0f, tween(250))
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    val wasPlaying = isPlayingLatest
                    toggle()
                    haptic.performHapticFeedback(
                        if (wasPlaying) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                    )
                    flash(if (wasPlaying) Symbols.Pause else Symbols.PlayArrow)
                })
            }
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -swipeThreshold -> next()
                            total > swipeThreshold -> previous()
                        }
                        total = 0f
                    },
                    onDragCancel = { total = 0f }
                ) { _, amount -> total += amount }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
        val icon = flashIcon
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { alpha = flashAlpha.value }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

/** Ambient mode options: what to show, AMOLED black, and Exit. */
@Composable
private fun AmbientOptionsSheet(
    settings: AmbientSettings,
    onChange: (AmbientSettings) -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("ambient_options_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Ambient mode",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
            // Artwork and lyrics can't both be off, or the screen would be empty.
            val rows = listOf(
                AmbientOption(
                    "Show artwork", "The album art; swipe it to change song",
                    Symbols.Image, settings.showArtwork,
                    enabled = !settings.showArtwork || settings.showLyrics
                ) { onChange(settings.copy(showArtwork = it)) },
                AmbientOption(
                    "Show song name", if (settings.showArtwork) "Next to the artwork" else "Needs the artwork",
                    Symbols.MusicNote, settings.showTitle && settings.showArtwork,
                    enabled = settings.showArtwork
                ) { onChange(settings.copy(showTitle = it)) },
                AmbientOption(
                    "Show artist name", if (settings.showArtwork) "Under the song name" else "Needs the artwork",
                    Symbols.Person, settings.showArtist && settings.showArtwork,
                    enabled = settings.showArtwork
                ) { onChange(settings.copy(showArtist = it)) },
                AmbientOption(
                    "Show lyrics", "Synced lyrics, when the song has them",
                    Symbols.Lyrics, settings.showLyrics,
                    enabled = !settings.showLyrics || settings.showArtwork
                ) { onChange(settings.copy(showLyrics = it)) }
            )
            OptionGroup(rows)
            Text(
                text = "The background follows Now Playing (Me › Music › Background).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SegmentedListItem(
                onClick = onExit,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                leadingContent = { Icon(Symbols.ArrowBack, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.testTag("ambient_exit")
            ) {
                Text("Exit ambient mode")
            }
        }
    }
}

private class AmbientOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
private fun OptionGroup(options: List<AmbientOption>) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        options.forEachIndexed { index, option ->
            SegmentedListItem(
                checked = option.checked,
                onCheckedChange = option.onCheckedChange,
                enabled = option.enabled,
                shapes = ListItemDefaults.segmentedShapes(index = index, count = options.size),
                leadingContent = { Icon(option.icon, contentDescription = null) },
                supportingContent = { Text(option.subtitle) },
                trailingContent = {
                    Switch(checked = option.checked, onCheckedChange = null, enabled = option.enabled)
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Text(option.title)
            }
        }
    }
}

/** Keeps the screen awake and hides the status and navigation bars while shown. */
@Composable
private fun KeepScreenOnAndHideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
