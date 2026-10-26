@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
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
 * - Play, the seek bar and switched-on buttons use the primary colour (the most important action).
 * - Previous / Next and switched-off buttons use the tonal secondary container.
 * - Gradient, Blur and Black backgrounds are dark, so the dark version of the colours is used
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
    /** Play button, seek bar, switched-on toggles. */
    val main: Color,
    val onMain: Color,
    /** Previous / Next and switched-off toggles. */
    val soft: Color,
    val onSoft: Color
)

@Composable
fun nowPlayingColors(background: NowPlayingBackground): NowPlayingColors {
    val scheme = MaterialTheme.colorScheme
    return if (background == NowPlayingBackground.FOLLOW_THEME) {
        NowPlayingColors(
            background = background,
            content = scheme.onPrimaryContainer,
            contentVariant = scheme.onPrimaryContainer.copy(alpha = 0.72f),
            main = scheme.primary,
            onMain = scheme.onPrimary,
            soft = scheme.primary.copy(alpha = 0.18f),
            onSoft = scheme.onPrimaryContainer
        )
    } else {
        NowPlayingColors(
            background = background,
            content = Color.White,
            contentVariant = Color.White.copy(alpha = 0.72f),
            main = scheme.primary,
            onMain = scheme.onPrimary,
            soft = scheme.secondaryContainer,
            onSoft = scheme.onSecondaryContainer
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
    val scope = rememberCoroutineScope()

    // Predictive back (Google's guidance for in-app surfaces): while the back gesture is held the
    // screen shrinks to 90%, moves a little with the finger and rounds its corners. Let go and it
    // shrinks into the mini player; change your mind and it springs back.
    val backProgress = remember { Animatable(0f) }
    var backFromLeft by remember { mutableStateOf(true) }
    var backPull by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler { events ->
        var startY = Float.NaN
        try {
            events.collect { event ->
                if (startY.isNaN()) startY = event.touchY
                backFromLeft = event.swipeEdge != BackEventCompat.EDGE_RIGHT
                backPull = event.touchY - startY
                backProgress.snapTo(event.progress)
            }
            closeLatest()
        } catch (cancelled: CancellationException) {
            scope.launch { backProgress.animateTo(0f, settleSpec) }
            throw cancelled
        }
    }
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
                val back = PredictiveBackEasing.transform(backProgress.value)
                val pull = (dragOffset / size.height).coerceIn(0f, 1f)
                val scale = (1f - 0.1f * back) * (1f - 0.1f * pull)
                scaleX = scale
                scaleY = scale
                val margin = 8.dp.toPx()
                val maxShiftX = (size.width / 20f - margin).coerceAtLeast(0f)
                val maxShiftY = (size.height / 20f - margin).coerceAtLeast(0f)
                translationX = (if (backFromLeft) 1f else -1f) * maxShiftX * back
                translationY = dragOffset +
                    (backPull / (size.height / 2f)).coerceIn(-1f, 1f) * maxShiftY * back
                val corner = (MiniPlayerHeight / 2).toPx() * maxOf(back, (pull * 6f).coerceAtMost(1f))
                if (corner > 0f) {
                    shape = RoundedCornerShape(corner)
                    clip = true
                } else {
                    clip = false
                }
            }
    ) {
        NowPlayingBackgroundLayer(
            style = background,
            track = track,
            modifier = Modifier.matchParentSize()
        )
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp)
                    .testTag("audio_now_playing")
            ) {
                NowPlayingTopBar(
                    colors = colors,
                    title = track.realAlbum,
                    subtitle = null,
                    onClose = onClose,
                    closeLabel = "Close Now Playing",
                    action = {
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
                )

                // Cover: takes the room that's left, always square.
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Title and artist, with Lyrics on the right.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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
                            containerColor = colors.soft,
                            contentColor = colors.onSoft
                        ),
                        modifier = Modifier
                            .size(IconButtonDefaults.mediumContainerSize())
                            .testTag("now_playing_lyrics")
                    ) {
                        Icon(Symbols.Lyrics, contentDescription = "Lyrics")
                    }
                }

                // Wavy seek bar, times and the sound details in between.
                Spacer(modifier = Modifier.height(16.dp))
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
                            targetState = techLabel,
                            animationSpec = motion.defaultEffectsSpec(),
                            label = "nowPlayingTechChip"
                        ) { chip ->
                            if (!chip.isNullOrBlank()) {
                                // A quiet pill so it reads as information, not a button.
                                Surface(shape = CircleShape, color = colors.soft, contentColor = colors.contentVariant) {
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

                Spacer(modifier = Modifier.height(20.dp))
                NowPlayingControls(
                    isPlaying = isPlaying,
                    colors = colors,
                    onPrevious = onSkipPrevious,
                    onPlayPause = onTogglePlayPause,
                    onNext = onSkipNext
                )

                if (trayActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
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
    onAction: (NowPlayingAction) -> Unit
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
                            actions.size == 1 -> ToggleButtonDefaults.shapesFor(buttonHeight = 56.dp)
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = colors.soft,
                            contentColor = colors.onSoft,
                            checkedContainerColor = colors.main,
                            checkedContentColor = colors.onMain
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        interactionSource = interaction,
                        modifier = Modifier
                            .weight(if (action == NowPlayingAction.SLEEP) sleepWeight else 1f)
                            .animateWidth(interaction)
                            .height(56.dp)
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
    drift: Boolean = false
) {
    val gradient = LocalAlbumArtGradient.current
    // One slow sway (about half a minute each way). Read only while drawing: redraws, never recomposes.
    val sway = if (drift) {
        rememberInfiniteTransition(label = "backgroundDrift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "backgroundSway"
        )
    } else null
    when (style) {
        NowPlayingBackground.FOLLOW_THEME -> Box(modifier.background(MaterialTheme.colorScheme.primaryContainer))
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
