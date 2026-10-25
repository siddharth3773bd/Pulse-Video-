@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.AmbientSettings
import com.example.model.AudioTrackItem
import com.example.model.NowPlayingBackground
import com.example.ui.theme.AlbumArtTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ambient mode: a calm, full-screen, always-on view of the current song.
 * The screen stays on and the system bars hide while it is open. There are no buttons on
 * screen: double-tap the artwork to play/pause and swipe it to change song (anywhere on screen
 * when the artwork is hidden). The ⋮ in the corner opens the options.
 */
@Composable
fun AmbientModeScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    queue: List<AudioTrackItem>,
    queueIndex: Int,
    settings: AmbientSettings,
    onSettingsChange: (AmbientSettings) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipToQueueItem: (Int) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    KeepScreenOnAndHideSystemBars()

    var showOptions by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val textColor = Color.White

    AlbumArtTheme(track = track, applyScheme = false, extractColors = !settings.amoled) {
        Box(modifier = modifier.fillMaxSize().testTag("ambient_mode")) {
            NowPlayingBackgroundLayer(
                style = if (settings.amoled) NowPlayingBackground.AMOLED else NowPlayingBackground.GRADIENT,
                track = track,
                followThemeColor = Color.Black,
                modifier = Modifier.matchParentSize()
            )

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
                    placeholderBackground = Color.White.copy(alpha = 0.08f),
                    placeholderTint = Color.White.copy(alpha = 0.7f),
                    modifier = mod,
                    cornerRadius = 24.dp,
                    testTag = "ambient_artwork"
                )
            }
            val info: @Composable () -> Unit = {
                if (showTitle) {
                    Text(
                        text = track.title,
                        color = textColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showArtist) {
                    Text(
                        text = track.artist,
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            val lyrics: @Composable (Modifier, TextAlign) -> Unit = { mod, align ->
                // Lyrics are not built yet; this is where they will appear.
                Box(modifier = mod, contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = "Lyrics will show here",
                        color = textColor.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            when {
                // Lyrics only: they fill the screen and the gestures work anywhere.
                !showArt -> {
                    LyricsOnlyGestures(
                        isPlaying = isPlaying,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        lyrics(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 64.dp, vertical = 48.dp),
                            TextAlign.Center
                        )
                    }
                }
                landscape -> Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 56.dp, vertical = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sideContent = showTitle || showArtist || showLyrics
                    art(
                        Modifier
                            .fillMaxHeight()
                            .weight(if (sideContent) 1f else 2f)
                    )
                    if (sideContent) {
                        Spacer(modifier = Modifier.width(40.dp))
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            info()
                            if (showLyrics) {
                                if (showTitle || showArtist) Spacer(modifier = Modifier.height(24.dp))
                                lyrics(Modifier.weight(1f).fillMaxWidth(), TextAlign.Start)
                            }
                        }
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 64.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    art(
                        Modifier
                            .fillMaxWidth()
                            .weight(if (showLyrics) 1f else 2f)
                    )
                    if (showTitle || showArtist) {
                        Spacer(modifier = Modifier.height(24.dp))
                        info()
                    }
                    if (showLyrics) {
                        Spacer(modifier = Modifier.height(24.dp))
                        lyrics(Modifier.weight(0.8f).fillMaxWidth(), TextAlign.Start)
                    }
                }
            }

            // The only thing on screen besides the song: ⋮ for the options.
            IconButton(
                onClick = { showOptions = true },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.iconButtonColors(contentColor = textColor.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .testTag("ambient_options_btn")
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Ambient mode options")
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
                    flash(if (wasPlaying) Icons.Default.Pause else Icons.Default.PlayArrow)
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
                    Icons.Default.Image, settings.showArtwork,
                    enabled = !settings.showArtwork || settings.showLyrics
                ) { onChange(settings.copy(showArtwork = it)) },
                AmbientOption(
                    "Show song name", if (settings.showArtwork) "Next to the artwork" else "Needs the artwork",
                    Icons.Default.MusicNote, settings.showTitle && settings.showArtwork,
                    enabled = settings.showArtwork
                ) { onChange(settings.copy(showTitle = it)) },
                AmbientOption(
                    "Show artist name", if (settings.showArtwork) "Under the song name" else "Needs the artwork",
                    Icons.Default.Person, settings.showArtist && settings.showArtwork,
                    enabled = settings.showArtwork
                ) { onChange(settings.copy(showArtist = it)) },
                AmbientOption(
                    "Show lyrics", "Synced lyrics, when the song has them",
                    LyricsIcon, settings.showLyrics,
                    enabled = !settings.showLyrics || settings.showArtwork
                ) { onChange(settings.copy(showLyrics = it)) }
            )
            OptionGroup(rows)
            Spacer(modifier = Modifier.height(12.dp))
            OptionGroup(
                listOf(
                    AmbientOption(
                        "AMOLED black", "Black background and white text, no album colours",
                        Icons.Default.DarkMode, settings.amoled
                    ) { onChange(settings.copy(amoled = it)) }
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            SegmentedListItem(
                onClick = onExit,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
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
