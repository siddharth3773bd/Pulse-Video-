@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.model.AudioTrackItem
import com.example.model.NowPlayingAction
import com.example.model.NowPlayingBackground
import com.example.model.formatDurationMs
import com.example.ui.icons.Symbols
import com.example.ui.theme.LocalAlbumArtGradient
import java.util.Locale

fun nowPlayingActionIcon(action: NowPlayingAction): ImageVector = when (action) {
    NowPlayingAction.SHUFFLE -> Symbols.Shuffle
    NowPlayingAction.REPEAT -> Symbols.Repeat
    NowPlayingAction.SLEEP -> Symbols.Bedtime
    NowPlayingAction.QUEUE -> Symbols.QueueMusic
    NowPlayingAction.AMBIENT -> Symbols.Landscape
}

// ---------------------------------------------------------------------------------------------
// One colour rule for Now Playing, Lyrics and Ambient mode
// ---------------------------------------------------------------------------------------------

/**
 * Colours of the Now Playing screens, following Material 3 colour roles so nothing clashes.
 * The colours come from "Colours from" (album art or the app's wallpaper / theme colours):
 * - Play and the seek bar use the primary colour (the most important action). For album-art
 *   colours in a dark theme that primary is already kept deep enough to stay colourful.
 * - Previous / Next use the tonal secondary container.
 * - The bottom tray (shuffle, repeat, sleep timer, queue, ambient) sits one level lower: switched
 *   off it is a quiet surface container, switched on it is the secondary container, so the
 *   two rows are told apart by lightness while keeping the same hue.
 * - Lyrics uses the secondary container too, so it matches Previous / Next (the tertiary colour is
 *   the opposite hue of the cover and clashes). The audio-info pill is a quiet surface container.
 * - Black is a pitch-black screen with the same colours as the Colour background, with the
 *   switched-off tray buttons and the info pill on a very dark surface.
 * - Gradient and Blur backgrounds are dark, so the dark version of the colours is used
 *   there (light, soft accents) and the text is white. On the "Colour" background (the primary
 *   container) the text uses its matching on-colour and the other buttons a tint of the primary.
 */
@Immutable
data class NowPlayingColors(
    val background: NowPlayingBackground,
    /** Text and plain icons. */
    val content: Color,
    /** Secondary text (artist, times). */
    val contentVariant: Color,
    /** Play button, seek bar and other primary-coloured controls. */
    val main: Color,
    val onMain: Color,
    /** Previous / Next, and other soft buttons. */
    val soft: Color,
    val onSoft: Color,
    /** Switched-off buttons of the bottom tray. */
    val tray: Color,
    val onTray: Color,
    /** Switched-on buttons of the bottom tray. */
    val trayChecked: Color,
    val onTrayChecked: Color,
    /** Lyrics button. */
    val accent: Color,
    val onAccent: Color,
    /** The audio-info pill (kHz, kbps, format): information, not a button. */
    val badge: Color,
    val onBadge: Color
)

@Composable
fun nowPlayingColors(background: NowPlayingBackground): NowPlayingColors {
    val scheme = MaterialTheme.colorScheme
    return if (background == NowPlayingBackground.FOLLOW_THEME) {
        NowPlayingColors(
            background = background,
            // Material's media-player roles: a calm tinted surface, primary for play, secondary
            // container for the rest.
            content = scheme.onSurface,
            contentVariant = scheme.onSurfaceVariant,
            main = scheme.primary,
            onMain = scheme.onPrimary,
            soft = scheme.secondaryContainer,
            onSoft = scheme.onSecondaryContainer,
            // Highest, not High: on a light screen High is almost the same as the background.
            tray = scheme.surfaceContainerHighest,
            onTray = scheme.onSurfaceVariant,
            trayChecked = scheme.secondaryContainer,
            onTrayChecked = scheme.onSecondaryContainer,
            accent = scheme.secondaryContainer,
            onAccent = scheme.onSecondaryContainer,
            badge = scheme.surfaceContainerHigh,
            onBadge = scheme.onSurfaceVariant
        )
    } else if (background == NowPlayingBackground.AMOLED) {
        // Pitch-black screen, the cover's colours on the controls (the same roles as Colour).
        // Switched-off tray buttons and the info pill sit quietly on a very dark surface.
        NowPlayingColors(
            background = background,
            content = Color.White,
            contentVariant = Color.White.copy(alpha = 0.72f),
            main = scheme.primary,
            onMain = scheme.onPrimary,
            soft = scheme.secondaryContainer,
            onSoft = scheme.onSecondaryContainer,
            tray = scheme.surfaceContainerHigh,
            onTray = scheme.onSurfaceVariant,
            trayChecked = scheme.secondaryContainer,
            onTrayChecked = scheme.onSecondaryContainer,
            accent = scheme.secondaryContainer,
            onAccent = scheme.onSecondaryContainer,
            badge = scheme.surfaceContainerHigh,
            onBadge = scheme.onSurfaceVariant
        )
    } else {
        // Gradient / Blur: the secondary container is dark too and nearly melts into the
        // background, so the soft buttons are the on-surface colour at 20%: a light veil that
        // shows over any cover colour.
        val soft = scheme.onSurface.copy(alpha = 0.20f)
        NowPlayingColors(
            background = background,
            content = Color.White,
            contentVariant = Color.White.copy(alpha = 0.72f),
            main = scheme.primary,
            onMain = scheme.onPrimary,
            soft = soft,
            onSoft = scheme.onSurface,
            // Same two levels on Gradient and Blur, where the surface and secondary containers
            // would melt into the cover colours. Off: a faint veil (10%). On: a veil as dense as
            // Previous / Next (20% white, about as bright as 28% of the light secondary) with the
            // cover's tint, and the same icon colour as Previous / Next. Never brighter than them.
            tray = scheme.onSurface.copy(alpha = 0.10f),
            onTray = Color.White.copy(alpha = 0.72f),
            trayChecked = scheme.secondary.copy(alpha = 0.28f),
            onTrayChecked = scheme.onSurface,
            accent = soft,
            onAccent = scheme.tertiary,
            badge = soft,
            onBadge = Color.White.copy(alpha = 0.72f)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Now Playing
// ---------------------------------------------------------------------------------------------

/**
 * Full-screen Now Playing. Top: close and ⋮ (song actions). Then the cover (swipe to change song,
 * double-tap to play/pause), title and artist with Lyrics, the wavy seek bar, the official
 * Expressive button group for the main controls, and the customisable tray of toggles.
 */
@Composable
fun NowPlayingScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    trayActions: List<NowPlayingAction>,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onLyrics: () -> Unit,
    onOpenAmbient: () -> Unit,
    sleepTimerRemainingMs: Long,
    sleepAtEndOfSong: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAtEndOfSong: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    queue: List<AudioTrackItem>,
    queueIndex: Int,
    onSkipToQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onSongInfo: () -> Unit,
    onGoToAlbum: (() -> Unit)?,
    onGoToArtist: (() -> Unit)?,
    modifier: Modifier = Modifier,
    background: NowPlayingBackground = NowPlayingBackground.FOLLOW_THEME
) {
    val colors = nowPlayingColors(background)
    val context = LocalContext.current
    val closeLatest by rememberUpdatedState(onClose)
    val density = LocalDensity.current
    val closeThresholdPx = with(density) { 120.dp.toPx() }
    val flingClosePx = with(density) { 900.dp.toPx() }
    val motion = MaterialTheme.motionScheme
    val settleSpec = motion.defaultSpatialSpec<Float>()

    // Predictive back: the screen shrinks and follows the finger, then goes into the mini player.
    val back = rememberPredictiveBack(onBack = { closeLatest() })
    // Swipe down: the screen follows the finger, then closes or springs back.
    var dragOffset by remember { mutableFloatStateOf(0f) }

    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // The old song's sound details stay until the new ones are known (no blink on every change).
    var techLabel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(track.id) {
        techLabel = try {
            audioTechLabel(context, track)
        } catch (_: Throwable) {
            null
        }
    }
    var showMenu by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    val safeDuration = if (durationMs > 0L) durationMs else track.durationMs
    val liveFraction = if (safeDuration > 0L) {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = dragFraction ?: liveFraction
    val shownPositionMs = (fraction * safeDuration).toLong()
    val sleepActive = sleepTimerRemainingMs > 0L || sleepAtEndOfSong

    Box(
        modifier = modifier
            .fillMaxSize()
            // The drag is read outside the moving layer, so the finger and the screen stay together.
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> dragOffset = (dragOffset + delta).coerceAtLeast(0f) },
                onDragStopped = { velocity ->
                    if (dragOffset > closeThresholdPx || velocity > flingClosePx) {
                        closeLatest()
                    } else {
                        animate(dragOffset, 0f, velocity, settleSpec) { value, _ -> dragOffset = value }
                    }
                }
            )
            .graphicsLayer {
                val pull = (dragOffset / size.height).coerceIn(0f, 1f)
                applyPredictiveBack(
                    state = back,
                    cornerRadius = MiniPlayerHeight / 2,
                    extraScale = 1f - 0.1f * pull,
                    extraTranslationY = dragOffset,
                    extraCornerFraction = pull * 6f
                )
            }
    ) {
        NowPlayingBackgroundLayer(
            style = background,
            track = track,
            modifier = Modifier.matchParentSize()
        )
        // The pieces of the screen; upright they stack, sideways the cover goes on the left.
        val moreMenuButton: @Composable () -> Unit = {
                    Box {
                        TooltipIconButton(
                            label = "More options",
                            icon = Symbols.MoreVert,
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("now_playing_more")
                        )
                        ExpressiveMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            groups = listOf(
                                listOf(
                                    MenuEntry("Add to playlist", Symbols.PlaylistAdd, "np_menu_add_playlist", onClick = onAddToPlaylist),
                                    MenuEntry("Share", Symbols.Share, "np_menu_share", onClick = onShare)
                                ),
                                listOfNotNull(
                                    onGoToAlbum?.let { MenuEntry("Go to album", Symbols.Album, "np_menu_album", onClick = it) },
                                    onGoToArtist?.let { MenuEntry("Go to artist", Symbols.Person, "np_menu_artist", onClick = it) },
                                    MenuEntry("Song info", Symbols.Info, "np_menu_info", onClick = onSongInfo)
                                )
                            )
                        )
                    }
        }
        val topBar: @Composable () -> Unit = {
            NowPlayingTopBar(
                colors = colors,
                title = track.realAlbum,
                subtitle = null,
                onClose = onClose,
                closeLabel = "Close Now Playing",
                action = moreMenuButton
            )
        }
        val cover: @Composable (Modifier) -> Unit = { coverModifier ->
            ArtworkPager(
                track = track,
                queue = queue,
                queueIndex = queueIndex,
                isPlaying = isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onSkipToQueueItem = onSkipToQueueItem,
                placeholderBackground = colors.soft,
                placeholderTint = colors.content,
                // The mini player's cover grows into this one (and back).
                currentCoverModifier = Modifier.playerCover(),
                modifier = coverModifier
            )
        }
        // Title and artist, with Lyrics on the right.
        val titleRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Material's lateral transition: the names slide the same way as the cover.
                val queueLatest by rememberUpdatedState(queue)
                val slideSpec = motion.defaultSpatialSpec<IntOffset>()
                val fadeInSpec = motion.defaultEffectsSpec<Float>()
                val fadeOutSpec = motion.fastEffectsSpec<Float>()
                val shiftPx = with(density) { 48.dp.roundToPx() }
                AnimatedContent(
                    targetState = track,
                    contentKey = { it.id },
                    transitionSpec = {
                        val direction = skipDirection(queueLatest, initialState, targetState)
                        (slideInHorizontally(slideSpec) { direction * shiftPx } + fadeIn(fadeInSpec)) togetherWith
                            (slideOutHorizontally(slideSpec) { -direction * shiftPx } + fadeOut(fadeOutSpec)) using
                            SizeTransform(clip = false)
                    },
                    modifier = Modifier.weight(1f),
                    label = "nowPlayingTitle"
                ) { shown ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = shown.title,
                            style = MaterialTheme.typography.headlineSmallEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("now_playing_title")
                        )
                        Text(
                            text = shown.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.contentVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                FilledTonalIconButton(
                    onClick = onLyrics,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier
                        .size(IconButtonDefaults.mediumContainerSize())
                        .testTag("now_playing_lyrics")
                ) {
                    Icon(Symbols.Lyrics, contentDescription = "Lyrics")
                }
            }
        }
        // Wavy seek bar, times and the sound details in between (left out sideways, to keep it calm).
        val seekBlock: @Composable (showTech: Boolean) -> Unit = { showTech ->
            Column(modifier = Modifier.fillMaxWidth()) {
                PlaybackSeekBar(
                    progress = fraction,
                    onSeekPreview = { dragFraction = it },
                    onSeekFinished = { target ->
                        dragFraction = null
                        onSeekTo(target)
                    },
                    isPlaying = isPlaying,
                    activeColor = colors.main,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_seek_slider")
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDurationMs(shownPositionMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.contentVariant
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = techLabel.takeIf { showTech },
                            animationSpec = motion.defaultEffectsSpec(),
                            label = "nowPlayingTechChip"
                        ) { chip ->
                            if (!chip.isNullOrBlank()) {
                                // A quiet pill so it reads as information, not a button.
                                Surface(shape = CircleShape, color = colors.badge, contentColor = colors.onBadge) {
                                    Text(
                                        text = chip,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = formatDurationMs(safeDuration),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.contentVariant
                    )
                }
            }
        }
        val controls: @Composable (compact: Boolean) -> Unit = { compact ->
            NowPlayingControls(
                isPlaying = isPlaying,
                colors = colors,
                onPrevious = onSkipPrevious,
                onPlayPause = onTogglePlayPause,
                onNext = onSkipNext,
                compact = compact
            )
        }
        val tray: @Composable (height: Dp) -> Unit = { height ->
            NowPlayingTray(
                actions = trayActions,
                colors = colors,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                sleepActive = sleepActive,
                sleepLabel = if (sleepAtEndOfSong) {
                    formatCountdown((safeDuration - positionMs).coerceAtLeast(0L))
                } else {
                    formatCountdown(sleepTimerRemainingMs)
                },
                height = height,
                onAction = { action ->
                    when (action) {
                        NowPlayingAction.SHUFFLE -> onToggleShuffle()
                        NowPlayingAction.REPEAT -> onCycleRepeat()
                        NowPlayingAction.QUEUE -> showQueueSheet = true
                        NowPlayingAction.AMBIENT -> onOpenAmbient()
                        NowPlayingAction.SLEEP -> showSleepSheet = true
                    }
                }
            )
        }

        val window = currentWindowLayout()
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            if (window.width > window.height) {
                // Sideways: the cover on the left at full height, everything else beside it, so
                // nothing is squeezed or cut off.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp)
                        .testTag("audio_now_playing"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cover(
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Close, then the tray's other buttons as small icons, then ⋮.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TooltipIconButton(
                                label = "Close Now Playing",
                                icon = Symbols.KeyboardArrowDown,
                                onClick = onClose,
                                modifier = Modifier.testTag("now_playing_close")
                            )
                            Spacer(Modifier.weight(1f))
                            trayActions.filter { it != NowPlayingAction.SHUFFLE && it != NowPlayingAction.REPEAT }
                                .forEach { action ->
                                    val active = action == NowPlayingAction.SLEEP && sleepActive
                                    TooltipIconButton(
                                        label = if (active) "Sleep timer: ${formatCountdown(sleepTimerRemainingMs)}" else action.label,
                                        icon = nowPlayingActionIcon(action),
                                        onClick = {
                                            when (action) {
                                                NowPlayingAction.QUEUE -> showQueueSheet = true
                                                NowPlayingAction.AMBIENT -> onOpenAmbient()
                                                NowPlayingAction.SLEEP -> showSleepSheet = true
                                                else -> Unit
                                            }
                                        },
                                        modifier = Modifier.testTag("now_playing_tray_${action.id}")
                                    )
                                }
                            moreMenuButton()
                        }
                        titleRow()
                        seekBlock(false)
                        // Shuffle · previous · play · next · repeat, all in one row.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (NowPlayingAction.SHUFFLE in trayActions) {
                                TrayToggle(
                                    checked = shuffleEnabled,
                                    icon = if (shuffleEnabled) Symbols.ShuffleOn else Symbols.Shuffle,
                                    label = "Shuffle",
                                    colors = colors,
                                    onClick = onToggleShuffle
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Box(Modifier.weight(1f)) { controls(true) }
                            if (NowPlayingAction.REPEAT in trayActions) {
                                Spacer(Modifier.width(8.dp))
                                TrayToggle(
                                    checked = repeatMode != Player.REPEAT_MODE_OFF,
                                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) Symbols.RepeatOne else Symbols.Repeat,
                                    label = "Repeat",
                                    colors = colors,
                                    onClick = onCycleRepeat
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp)
                        .testTag("audio_now_playing")
                ) {
                    topBar()
                    // Cover: takes the room that's left, always square.
                    cover(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    titleRow()
                    Spacer(modifier = Modifier.height(16.dp))
                    seekBlock(true)
                    Spacer(modifier = Modifier.height(20.dp))
                    controls(false)
                    if (trayActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        tray(56.dp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showQueueSheet) {
            QueueSheet(
                queue = queue.ifEmpty { listOf(track) },
                currentIndex = if (queue.isEmpty()) 0 else queueIndex,
                shuffleEnabled = shuffleEnabled,
                isPlaying = isPlaying,
                onPlayItem = onSkipToQueueItem,
                onMove = onMoveQueueItem,
                onRemove = onRemoveQueueItem,
                onDismiss = { showQueueSheet = false }
            )
        }
        if (showSleepSheet) {
            SleepTimerSheet(
                active = sleepActive,
                atEndActive = sleepAtEndOfSong,
                remainingMs = sleepTimerRemainingMs,
                endLabel = "At the end of this song",
                endShortLabel = "End of song",
                endIcon = Symbols.MusicNote,
                onStart = { minutes ->
                    onStartSleepTimer(minutes)
                    showSleepSheet = false
                },
                onAtEnd = {
                    onSleepAtEndOfSong()
                    showSleepSheet = false
                },
                onTurnOff = {
                    onCancelSleepTimer()
                    showSleepSheet = false
                },
                onDismiss = { showSleepSheet = false }
            )
        }
    }
}

/**
 * Top bar of the Now Playing screens: close on the left, an optional [title] / [subtitle] in the
 * middle and one [action] on the right. Same height and buttons everywhere.
 */
@Composable
internal fun NowPlayingTopBar(
    colors: NowPlayingColors,
    title: String?,
    subtitle: String?,
    onClose: () -> Unit,
    closeLabel: String,
    action: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipIconButton(
                label = closeLabel,
                icon = Symbols.KeyboardArrowDown,
                onClick = onClose,
                modifier = Modifier.testTag("now_playing_close")
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title != null) {
                    // The album name cross-fades when the song changes.
                    Crossfade(
                        targetState = title,
                        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                        label = "nowPlayingTopTitle"
                    ) { shownTitle ->
                        Text(
                            text = shownTitle,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.contentVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { action() }
        }
    }
}

/**
 * Previous / Play-Pause / Next: the official Expressive button group. The pressed button springs
 * wider and squeezes its neighbours; corners morph on press. [compact] is for Lyrics.
 */
@Composable
internal fun NowPlayingControls(
    isPlaying: Boolean,
    colors: NowPlayingColors,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val sideHeight: Dp = if (compact) 56.dp else 72.dp
    val mainHeight: Dp = if (compact) 64.dp else 80.dp
    val sideIcon: Dp = if (compact) 26.dp else 30.dp
    val mainIcon: Dp = if (compact) 30.dp else 36.dp
    val previousInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val sideColors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.soft, contentColor = colors.onSoft)
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = onPrevious,
                    shapes = IconButtonDefaults.shapes(),
                    colors = sideColors,
                    interactionSource = previousInteraction,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(previousInteraction)
                        .height(sideHeight)
                        .testTag("now_playing_previous")
                ) { Icon(Symbols.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(sideIcon)) }
            },
            menuContent = { menuState ->
                DropdownMenuItem(text = { Text("Previous") }, onClick = { onPrevious(); menuState.dismiss() })
            }
        )
        customItem(
            buttonGroupContent = {
                FilledIconButton(
                    onClick = onPlayPause,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.main, contentColor = colors.onMain),
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .weight(1.3f)
                        .animateWidth(playInteraction)
                        .height(mainHeight)
                        .testTag("now_playing_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Symbols.Pause else Symbols.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(mainIcon)
                    )
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(text = { Text(if (isPlaying) "Pause" else "Play") }, onClick = { onPlayPause(); menuState.dismiss() })
            }
        )
        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = onNext,
                    shapes = IconButtonDefaults.shapes(),
                    colors = sideColors,
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(nextInteraction)
                        .height(sideHeight)
                        .testTag("now_playing_next")
                ) { Icon(Symbols.SkipNext, contentDescription = "Next", modifier = Modifier.size(sideIcon)) }
            },
            menuContent = { menuState ->
                DropdownMenuItem(text = { Text("Next") }, onClick = { onNext(); menuState.dismiss() })
            }
        )
    }
}

/**
 * The customisable tray: official connected button group of toggles. A running sleep timer turns
 * into a wider pill with the countdown.
 */
@Composable
private fun NowPlayingTray(
    actions: List<NowPlayingAction>,
    colors: NowPlayingColors,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepActive: Boolean,
    sleepLabel: String,
    onAction: (NowPlayingAction) -> Unit,
    height: Dp = 56.dp
) {
    val sleepWeight by animateFloatAsState(
        targetValue = if (sleepActive) 2f else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "traySleepWeight"
    )
    val interactions = remember { mutableMapOf<NowPlayingAction, MutableInteractionSource>() }
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            val interaction = interactions.getOrPut(action) { MutableInteractionSource() }
            val checked = when (action) {
                NowPlayingAction.SHUFFLE -> shuffleEnabled
                NowPlayingAction.REPEAT -> repeatMode != Player.REPEAT_MODE_OFF
                NowPlayingAction.SLEEP -> sleepActive
                NowPlayingAction.QUEUE, NowPlayingAction.AMBIENT -> false
            }
            val icon = if (action == NowPlayingAction.REPEAT && repeatMode == Player.REPEAT_MODE_ONE) {
                Symbols.RepeatOne
            } else {
                nowPlayingActionIcon(action)
            }
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = checked,
                        onCheckedChange = { onAction(action) },
                        shapes = when {
                            actions.size == 1 -> ToggleButtonDefaults.shapesFor(buttonHeight = height)
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = colors.tray,
                            contentColor = colors.onTray,
                            checkedContainerColor = colors.trayChecked,
                            checkedContentColor = colors.onTrayChecked
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        interactionSource = interaction,
                        modifier = Modifier
                            .weight(if (action == NowPlayingAction.SLEEP) sleepWeight else 1f)
                            .animateWidth(interaction)
                            .height(height)
                            .testTag("now_playing_tray_${action.id}")
                    ) {
                        Icon(icon, contentDescription = action.label, modifier = Modifier.size(24.dp))
                        if (action == NowPlayingAction.SLEEP && sleepActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sleepLabel, style = MaterialTheme.typography.labelLargeEmphasized, maxLines = 1)
                        }
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        leadingIcon = { Icon(icon, contentDescription = null) },
                        onClick = {
                            onAction(action)
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}

/** Shuffle or repeat as a round toggle beside the play buttons (sideways Now Playing). */
@Composable
private fun TrayToggle(
    checked: Boolean,
    icon: ImageVector,
    label: String,
    colors: NowPlayingColors,
    onClick: () -> Unit
) {
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = IconButtonDefaults.toggleableShapes(),
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            containerColor = colors.tray,
            contentColor = colors.onTray,
            checkedContainerColor = colors.trayChecked,
            checkedContentColor = colors.onTrayChecked
        ),
        modifier = Modifier
            .size(IconButtonDefaults.mediumContainerSize())
            .semantics { contentDescription = label }
            .testTag("now_playing_tray_${label.lowercase()}")
    ) {
        Icon(icon, contentDescription = null)
    }
}

/** +1 when [to] comes after [from] in the queue (Next, a later song), -1 for an earlier one. */
private fun skipDirection(queue: List<AudioTrackItem>, from: AudioTrackItem, to: AudioTrackItem): Int {
    val a = queue.indexOfFirst { it.id == from.id }
    val b = queue.indexOfFirst { it.id == to.id }
    return if (a >= 0 && b >= 0 && b < a) -1 else 1
}

/** "14:56" style countdown; shows hours as "1:05:00" when needed. */
private fun formatCountdown(ms: Long): String {
    val totalSec = ((ms + 999L) / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/**
 * Background behind the Now Playing screens: Colour (solid primary container), Gradient
 * (album-art colours), Blur (album art, blurred) or Black. Image backgrounds get a dark scrim so
 * white text stays readable; changes crossfade.
 */
@Composable
internal fun NowPlayingBackgroundLayer(
    style: NowPlayingBackground,
    track: AudioTrackItem,
    modifier: Modifier = Modifier,
    /** Lyrics and ambient mode: the background drifts very slowly, so the screen feels alive. */
    drift: Boolean = false,
    /** Drifting only while the music plays; paused, it holds still (and the screen stops redrawing). */
    moving: Boolean = true
) {
    val gradient = LocalAlbumArtGradient.current
    // One slow sway (about half a minute each way). Read only while drawing: redraws, never recomposes.
    val sway = if (drift) remember { Animatable(0f) } else null
    if (sway != null) {
        val forward = remember { booleanArrayOf(true) }
        LaunchedEffect(moving) {
            if (!moving) return@LaunchedEffect
            while (true) {
                // Carries on from where it stopped, in the same direction.
                val target = if (forward[0]) 1f else 0f
                val remaining = (kotlin.math.abs(target - sway.value) * 28_000).toInt().coerceAtLeast(1)
                sway.animateTo(target, tween(remaining, easing = LinearEasing))
                forward[0] = !forward[0]
            }
        }
    }
    when (style) {
        NowPlayingBackground.FOLLOW_THEME -> Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer))
        NowPlayingBackground.AMOLED -> Box(modifier.background(Color.Black))
        NowPlayingBackground.GRADIENT -> {
            // The colours glide to the next song's; the layer itself never fades, so the screen
            // behind can't show through while songs change.
            val spec = tween<Color>(durationMillis = 800)
            val top by animateColorAsState(gradient.getOrElse(0) { Color(0xFF1C1C1C) }, spec, label = "gradientTop")
            val middle by animateColorAsState(gradient.getOrElse(1) { Color(0xFF111111) }, spec, label = "gradientMiddle")
            val bottom by animateColorAsState(gradient.getOrElse(2) { Color.Black }, spec, label = "gradientBottom")
            if (sway != null) {
                Box(
                    modifier.drawBehind {
                        val w = size.width
                        val h = size.height
                        val t = sway.value
                        drawRect(
                            Brush.linearGradient(
                                0f to top, 0.5f to middle, 1f to bottom,
                                start = Offset(w * (0.1f + 0.5f * t), 0f),
                                end = Offset(w * (0.9f - 0.5f * t), h)
                            )
                        )
                        drawRect(Color.Black.copy(alpha = 0.2f))
                    }
                )
            } else {
                Box(
                    modifier
                        .background(Brush.verticalGradient(0f to top, 0.5f to middle, 1f to bottom))
                        .background(Color.Black.copy(alpha = 0.2f))
                )
            }
        }
        NowPlayingBackground.BLUR -> {
            val bitmap = rememberAudioArtBitmap(track.contentUri)
            Crossfade(
                targetState = bitmap,
                animationSpec = tween(800),
                label = "nowPlayingBlur",
                // Solid black underneath, so nothing shows through while two covers cross-fade.
                modifier = modifier.background(Color.Black)
            ) { bmp ->
                Box(Modifier.fillMaxSize()) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // Drift: the blurred cover slowly pans, slightly zoomed so no edge shows.
                                    if (sway != null) {
                                        scaleX = 1.15f
                                        scaleY = 1.15f
                                        translationX = (sway.value - 0.5f) * size.width * 0.1f
                                    }
                                }
                                .blur(120.dp)
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}
