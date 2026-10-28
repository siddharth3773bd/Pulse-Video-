@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import kotlinx.coroutines.coroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import com.example.ui.icons.Symbols
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.widthIn
import android.os.SystemClock
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
 * screen stays on and the system bars hide while it is open. Double-tap the artwork to play/pause
 * and swipe it to change song (anywhere on screen when the artwork is hidden). A double tap on empty
 * space shows the play controls for a few seconds. The top corners hold "back to Now Playing" and ⋮.
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
    // A tap shows the controls; they fade away 4 seconds after the last touch.
    var controlsTouchedAt by remember { mutableLongStateOf(0L) }
    var controlsShown by remember { mutableStateOf(false) }
    LaunchedEffect(controlsTouchedAt) {
        if (controlsTouchedAt == 0L) return@LaunchedEffect
        controlsShown = true
        delay(4_000L)
        controlsShown = false
    }
    val touchControls = { controlsTouchedAt = SystemClock.uptimeMillis() }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = nowPlayingColors(background)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Taps on empty space stop here (they used to reach Now Playing underneath). A double
            // tap shows or hides the controls; a single tap does nothing, so touching the song
            // name or artist by accident no longer pops them up.
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (controlsShown) controlsShown = false else touchControls()
                })
            }
            .testTag("ambient_mode")
    ) {
        // The colours move very slowly, so the screen feels alive without drawing attention.
        NowPlayingBackgroundLayer(style = background, track = track, drift = true, moving = isPlaying, modifier = Modifier.matchParentSize())

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
                        settings = lyricsSettings.copy(
                            align = if (align == TextAlign.Center) com.example.lyrics.LyricsAlign.CENTER
                            else com.example.lyrics.LyricsAlign.LEFT
                        ),
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
                // How much of the height the artwork takes: all of it when nothing sits under it,
                // less when the song name / artist do.
                val targetFraction = when {
                    showTitle && showArtist -> 0.64f
                    showTitle -> 0.72f
                    showArtist -> 0.84f
                    else -> 1f
                }
                // Smooth by design: ONLY the artwork animates (one non-bouncy spring, read while
                // laying out, so nothing is recomposed per frame). The text area under it simply
                // takes the leftover height, so the group never wobbles. The lyrics don't follow the
                // artwork frame by frame (that made them re-wrap and shake): they fade out, jump to
                // their final place unseen, and fade back in once the artwork has settled.
                val artFraction = remember { Animatable(targetFraction) }
                var slotFraction by remember { mutableFloatStateOf(targetFraction) }
                val lyricsAlpha = remember { Animatable(1f) }
                val sizeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
                LaunchedEffect(targetFraction) {
                    if (artFraction.targetValue == targetFraction) return@LaunchedEffect
                    coroutineScope {
                        launch { artFraction.animateTo(targetFraction, sizeSpec) }
                        launch {
                            lyricsAlpha.animateTo(0f, tween(90))
                            slotFraction = targetFraction
                            delay(170)
                            lyricsAlpha.animateTo(1f, tween(240))
                        }
                    }
                }
                BoxWithConstraints(
                    modifier = safe.padding(start = 36.dp, end = 48.dp, top = 32.dp, bottom = 32.dp)
                ) {
                    val hPx = constraints.maxHeight
                    val wPx = constraints.maxWidth
                    fun sideFor(f: Float) = minOf(hPx * f, wPx * 0.5f).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = if (showLyrics) Arrangement.Start else Arrangement.Center
                    ) {
                        // The artwork's slot: with lyrics it already has its final width (so the
                        // lyrics don't move while the artwork animates); without lyrics it follows it.
                        Box(
                            modifier = Modifier.layout { measurable, c ->
                                val w = sideFor(if (showLyrics) slotFraction else artFraction.value)
                                val placeable = measurable.measure(Constraints.fixed(w, c.maxHeight))
                                layout(w, c.maxHeight) { placeable.placeRelative(0, 0) }
                            }
                        ) {
                            Column(modifier = Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)) {
                                art(
                                    Modifier.layout { measurable, _ ->
                                        val side = sideFor(artFraction.value)
                                        val placeable = measurable.measure(Constraints.fixed(side, side))
                                        layout(side, side) { placeable.placeRelative(0, 0) }
                                    }
                                )
                                // The song name and artist: they take whatever height the artwork leaves.
                                Box(
                                    modifier = Modifier
                                        .layout { measurable, _ ->
                                            val side = sideFor(artFraction.value)
                                            val h = (hPx - side).coerceAtLeast(0)
                                            val placeable = measurable.measure(Constraints.fixed(side, h))
                                            layout(side, h) { placeable.placeRelative(0, 0) }
                                        }
                                        .clipToBounds()
                                ) {
                                    val appear = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                                        expandVertically(MaterialTheme.motionScheme.defaultEffectsSpec())
                                    val disappear = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                                        shrinkVertically(MaterialTheme.motionScheme.defaultEffectsSpec())
                                    Column {
                                        androidx.compose.animation.AnimatedVisibility(visible = showTitle, enter = appear, exit = disappear) {
                                            Text(
                                                text = track.title,
                                                color = colors.content,
                                                style = MaterialTheme.typography.titleLargeEmphasized,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 14.dp)
                                            )
                                        }
                                        androidx.compose.animation.AnimatedVisibility(visible = showArtist, enter = appear, exit = disappear) {
                                            Text(
                                                text = track.artist,
                                                color = colors.contentVariant,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = if (showTitle) 2.dp else 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // The lyrics get the full height of the screen.
                        if (showLyrics) {
                            Spacer(modifier = Modifier.width(40.dp))
                            lyrics(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .graphicsLayer { alpha = lyricsAlpha.value },
                                TextAlign.Start
                            )
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

        // Play controls, shown for a moment after a tap.
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 3 },
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                modifier = Modifier.widthIn(max = 360.dp)
            ) {
                NowPlayingControls(
                    isPlaying = isPlaying,
                    colors = colors,
                    onPrevious = { touchControls(); onSkipPrevious() },
                    onPlayPause = { touchControls(); onTogglePlayPause() },
                    onNext = { touchControls(); onSkipNext() },
                    compact = true,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Back to Now Playing: always there, but quiet.
        IconButton(
            onClick = onClose,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.content.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(8.dp)
                .testTag("ambient_exit_btn")
        ) {
            Icon(Symbols.CloseFullscreen, contentDescription = "Back to Now Playing")
        }

        // ⋮ with a small menu of options.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(8.dp)
        ) {
            IconButton(
                onClick = { showOptions = true },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.iconButtonColors(contentColor = colors.content.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("ambient_options_btn")
            ) {
                Icon(Symbols.MoreVert, contentDescription = "Ambient mode options")
            }
            AmbientMenu(
                expanded = showOptions,
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

/**
 * Ambient mode options as the official Expressive menu (small, next to ⋮, instead of a sheet that
 * covered the whole screen sideways): what to show, a black background for the night, and Exit.
 * Switching an option keeps the menu open so several can be changed in a row.
 */
@Composable
private fun AmbientMenu(
    expanded: Boolean,
    settings: AmbientSettings,
    onChange: (AmbientSettings) -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ambient_options_menu")
    ) {
        // Artwork and lyrics can't both be off, or the screen would be empty.
        val show = listOf(
            AmbientOption("Artwork", Symbols.Image, settings.showArtwork, enabled = !settings.showArtwork || settings.showLyrics) {
                onChange(settings.copy(showArtwork = it))
            },
            AmbientOption("Song name", Symbols.MusicNote, settings.showTitle && settings.showArtwork, enabled = settings.showArtwork) {
                onChange(settings.copy(showTitle = it))
            },
            AmbientOption("Artist name", Symbols.Person, settings.showArtist && settings.showArtwork, enabled = settings.showArtwork) {
                onChange(settings.copy(showArtist = it))
            },
            AmbientOption("Lyrics", Symbols.Lyrics, settings.showLyrics, enabled = !settings.showLyrics || settings.showArtwork) {
                onChange(settings.copy(showLyrics = it))
            }
        )
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 3)) {
            show.forEachIndexed { index, option ->
                // The official menu puts items edge to edge, so several ticked items in a row
                // would melt into one block. The Material menu gap keeps each one its own shape.
                if (index > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                CheckableDropdownMenuItem(
                    checked = option.checked,
                    onCheckedChange = option.onCheckedChange,
                    enabled = option.enabled,
                    text = { Text(option.title) },
                    shapes = MenuDefaults.itemShape(index = index, count = show.size),
                    leadingIcon = { Icon(option.icon, contentDescription = null) },
                    checkedLeadingIcon = { Icon(Symbols.Check, contentDescription = null) },
                    modifier = Modifier.testTag("ambient_show_${option.title.lowercase().replace(' ', '_')}")
                )
            }
        }
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 3)) {
            CheckableDropdownMenuItem(
                checked = settings.black,
                onCheckedChange = { onChange(settings.copy(black = it)) },
                text = { Text("Black background") },
                supportingText = { Text("For the night; off follows Now Playing") },
                shapes = MenuDefaults.itemShape(index = 0, count = 1),
                leadingIcon = { Icon(Symbols.DarkMode, contentDescription = null) },
                checkedLeadingIcon = { Icon(Symbols.Check, contentDescription = null) },
                modifier = Modifier.testTag("ambient_black")
            )
        }
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 2, count = 3)) {
            DropdownMenuItem(
                onClick = onExit,
                text = { Text("Exit ambient mode") },
                shape = MenuDefaults.itemShape(index = 0, count = 1).shape,
                leadingIcon = { Icon(Symbols.ArrowBack, contentDescription = null) },
                modifier = Modifier.testTag("ambient_exit")
            )
        }
    }
}

private class AmbientOption(
    val title: String,
    val icon: ImageVector,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit
)

/** Keeps the screen awake and hides the status and navigation bars while shown. */
@Composable
private fun KeepScreenOnAndHideSystemBars() {
    KeepScreenOn()
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

