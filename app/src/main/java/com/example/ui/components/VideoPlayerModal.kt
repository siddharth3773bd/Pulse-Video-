@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.icons.Symbols
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon as PlatformIcon
import android.media.AudioManager
import android.provider.OpenableColumns
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.example.R
import com.example.data.MediaStoreScanner
import com.example.model.PlaybackSettings
import com.example.model.ScreenOrientationMode
import com.example.model.SubtitleSize
import com.example.model.VideoAspectMode
import com.example.model.VideoItem
import com.example.model.formatDurationMs
import com.example.player.PlayerController
import com.example.player.PlayerTrackOption
import kotlin.math.abs
import kotlinx.coroutines.delay

private const val ACTION_PIP_CONTROL = "app.doppler.player.PIP_CONTROL"
private const val EXTRA_PIP_CMD = "cmd"
private const val PIP_PLAY_PAUSE = 1
private const val PIP_NEXT = 2
private const val PIP_PREVIOUS = 3


/**
 * Full-screen video player.
 * - Hides the status and navigation bars (swipe from an edge to peek at them).
 * - The video is drawn with the official Media3 Compose ContentFrame; subtitles with Media3's
 *   SubtitleView, so their own styling and position are kept.
 * - Pinch out to fill the screen (crop), pinch in to fit; the aspect button cycles Fit/Crop/Stretch.
 * - Picture-in-picture (if on in settings) when leaving the app, or from the PiP button.
 * - When a video ends, the next one in its folder plays (if on in settings).
 */
@Composable
fun VideoPlayerModal(
    video: VideoItem,
    playerController: PlayerController,
    playbackSettings: PlaybackSettings,
    resumedFromMs: Long,
    nextVideo: VideoItem?,
    onSavePosition: (positionMs: Long, durationMs: Long, forcePersist: Boolean) -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onPlaybackEnded: () -> Boolean,
    onUpdatePlaybackSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** True while the player is shrinking away after being closed. */
    closing: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val playerState by playerController.state.collectAsStateWithLifecycle()
    val playerTracks by playerController.tracks.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var gestureHud by remember { mutableStateOf<Pair<ImageVector, String>?>(null) }
    // Volume / brightness level (0..1), drawn as a bar in the gesture feedback.
    var hudLevel by remember { mutableStateOf<Float?>(null) }
    var isLongPressing2x by remember { mutableStateOf(false) }
    var speedBeforeLongPress by remember { mutableFloatStateOf(1.0f) }
    var aspectMode by remember { mutableStateOf(VideoAspectMode.FIT) }

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }

    // Audio only: the picture is switched off, the sound keeps going (also after leaving the app).
    var audioOnly by remember { mutableStateOf(false) }
    // Rotate button: null = follow the setting / sensor.
    var rotationOverride by remember { mutableStateOf<Int?>(null) }
    // Sleep timer: a deadline (elapsed-realtime clock) or "at the end of this video".
    var sleepDeadline by remember { mutableStateOf<Long?>(null) }
    var sleepEndOfVideo by remember { mutableStateOf(false) }
    var sleepRemainingMs by remember { mutableLongStateOf(0L) }

    // ---- Next / previous: Material's fade-through ----
    // The picture fades out quickly, and the new video fades up once its first picture is
    // really there (no thumbnail, no stretched frame, no sliding).
    // A video that follows on by itself (autoplay) just carries on.
    var switchDirection by remember { mutableIntStateOf(0) }
    var switchFrameCount by remember { mutableIntStateOf(0) }
    val goPrevious: () -> Unit = {
        switchDirection = -1
        switchFrameCount = playerController.state.value.firstFrameCount
        onPreviousVideo()
    }
    val goNext: () -> Unit = {
        switchDirection = 1
        switchFrameCount = playerController.state.value.firstFrameCount
        onNextVideo()
    }
    val surfaceAlpha = remember { Animatable(1f) }
    val motion = MaterialTheme.motionScheme
    val fadeOutSpec = motion.fastEffectsSpec<Float>()
    val fadeInSpec = motion.defaultEffectsSpec<Float>()
    var shownVideoId by remember { mutableStateOf(video.id) }
    LaunchedEffect(video.id) {
        if (video.id == shownVideoId) return@LaunchedEffect
        shownVideoId = video.id
        val direction = switchDirection
        switchDirection = 0
        if (direction == 0 || audioOnly) {
            surfaceAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        surfaceAlpha.animateTo(0f, fadeOutSpec)
        withTimeoutOrNull(1_500L) {
            playerController.state.first { it.firstFrameCount > switchFrameCount }
        }
        surfaceAlpha.animateTo(1f, fadeInSpec)
    }

    // "Resumed from 0:10 · Start over" pill, shown for a few seconds after resuming.
    var showResumedPill by remember(video.id) { mutableStateOf(resumedFromMs > 0L) }
    LaunchedEffect(video.id, showResumedPill) {
        if (showResumedPill) {
            delay(4_000L)
            showResumedPill = false
        }
    }

    var surfaceWidthPx by remember { mutableIntStateOf(1) }
    var surfaceHeightPx by remember { mutableIntStateOf(1) }

    val inPip = rememberIsInPictureInPicture(activity)
    val poke: () -> Unit = { interactionTick++ }

    // ---- Window: screen on while playing, bars hidden, forced orientation ----
    KeepScreenOn(enabled = playerState.isPlaying && !audioOnly)
    val view = LocalView.current
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    // The bars return as the player starts to shrink away (not at the very end of the flight),
    // so the status/navigation bars don't pop in after the animation is over.
    LaunchedEffect(closing) {
        if (closing) {
            activity?.window?.let { WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
    DisposableEffect(activity) {
        val prev = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { activity?.requestedOrientation = prev }
    }
    LaunchedEffect(activity, playbackSettings.forceOrientation, rotationOverride) {
        activity?.requestedOrientation = rotationOverride ?: when (playbackSettings.forceOrientation) {
            ScreenOrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // ---- Audio only / background sound ----
    LaunchedEffect(audioOnly) {
        playerController.setVideoEnabled(!audioOnly)
        playerController.setBackgroundAudio(audioOnly)
    }
    DisposableEffect(Unit) {
        onDispose {
            playerController.setBackgroundAudio(false)
            playerController.setVideoEnabled(true)
            // (Volume isn't reset here: closing fades it out, the next video sets it back.)
            playerController.setScrubbing(false)
        }
    }

    // ---- Sleep timer: counts down, then fades the sound out and pauses ----
    LaunchedEffect(sleepDeadline) {
        val deadline = sleepDeadline ?: return@LaunchedEffect
        while (true) {
            val left = deadline - SystemClock.elapsedRealtime()
            sleepRemainingMs = left.coerceAtLeast(0L)
            if (left <= 0L) break
            delay(left.coerceAtMost(1_000L))
        }
        try {
            for (step in 15 downTo 0) {
                playerController.setVolume(step / 15f)
                delay(250L)
            }
            playerController.pause()
        } finally {
            playerController.setVolume(1f)
        }
        sleepDeadline = null
        controlsVisible = true
        gestureHud = Symbols.Bedtime to "Sleep timer: paused"
    }

    // ---- Save the position every few seconds (not on every tick) ----
    val saveLatest by rememberUpdatedState(onSavePosition)
    LaunchedEffect(video.id) {
        while (true) {
            delay(5_000L)
            val st = playerController.state.value
            val dur = st.durationMs.takeIf { it > 0L } ?: video.durationMs
            // Paused, nothing changes (closing the player saves the spot anyway).
            if (st.isPlaying && st.positionMs > 0L && dur > 0L) saveLatest(st.positionMs, dur, false)
        }
    }

    // ---- Autoplay: the next video waits inside the player, so it starts without a pause ----
    LaunchedEffect(video.id, nextVideo?.id, playbackSettings.autoplayNext, sleepEndOfVideo) {
        val next = nextVideo
        if (playbackSettings.autoplayNext && !sleepEndOfVideo && next != null) {
            playerController.queueNext(next.contentUri, next.path, next.title)
        } else {
            playerController.queueNext(null)
        }
    }
    val endedLatest by rememberUpdatedState(onPlaybackEnded)
    LaunchedEffect(playerState.isEnded, video.id) {
        if (playerState.isEnded) {
            if (sleepEndOfVideo) {
                sleepEndOfVideo = false
                controlsVisible = true
                gestureHud = Symbols.Bedtime to "Sleep timer: stopped"
            } else if (playbackSettings.autoplayNext) {
                // Normally the queued video has already taken over; this is only a fallback.
                if (!endedLatest()) controlsVisible = true
            } else {
                controlsVisible = true
            }
        }
    }

    // ---- Controls hide by themselves while playing ----
    LaunchedEffect(controlsVisible, playerState.isPlaying, interactionTick, controlsLocked, showMoreMenu) {
        if (controlsVisible && playerState.isPlaying && !audioOnly && !showMoreMenu) {
            delay(if (controlsLocked) 1_500L else 3_500L)
            controlsVisible = false
        }
    }
    LaunchedEffect(gestureHud) {
        if (gestureHud != null && !isLongPressing2x) {
            delay(900L)
            gestureHud = null
            hudLevel = null
        }
    }

    val closeAndPersist = {
        val dur = playerState.durationMs.takeIf { it > 0L } ?: video.durationMs
        onSavePosition(playerState.positionMs, dur, true)
        playerController.setBackgroundAudio(false)
        onClose()
    }
    // Android's predictive back: while the gesture is held the player shrinks and follows the
    // finger with the library showing behind it; letting go closes it.
    val back = rememberPredictiveBack(enabled = !closing, settleAfterBack = true, onBack = closeAndPersist)
    // While the back gesture is held or the player is closing, the buttons step aside so just the
    // picture shrinks away.
    val backInProgress by remember { derivedStateOf { back.progress > 0.01f } }

    // ---- Picture-in-picture ----
    PictureInPictureSupport(
        activity = activity,
        enabled = playbackSettings.pictureInPicture && !audioOnly,
        isPlaying = playerState.isPlaying,
        videoWidth = video.width,
        videoHeight = video.height,
        onPlayPause = { playerController.togglePlayPause() },
        onNext = goNext,
        onPrevious = goPrevious
    )
    // Leaving the app without PiP (or closing the PiP window) pauses the video.
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioOnlyLatest by rememberUpdatedState(audioOnly)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isChangingConfigurations != true && !audioOnlyLatest) {
                playerController.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    val effectiveDurationMs = playerState.durationMs.takeIf { it > 0L } ?: video.durationMs.coerceAtLeast(1000L)
    val currentPositionMs = playerState.positionMs.coerceIn(0L, effectiveDurationMs)
    val progressFraction = (currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    var seekPreviewFraction by remember { mutableStateOf<Float?>(null) }
    // The loading indicator only appears when loading really takes a while (a second), and never
    // while the seek bar is being dragged: the picture already shows where you are. The wait starts
    // over for every new video, so tapping next/previous quickly doesn't add up to a "long" load.
    var showBuffering by remember { mutableStateOf(false) }
    val dragging = seekPreviewFraction != null
    LaunchedEffect(playerState.isBuffering, dragging, video.id) {
        if (playerState.isBuffering && !dragging) {
            delay(1_000L)
            showBuffering = true
        } else {
            showBuffering = false
        }
    }
    val displayedPositionMs = seekPreviewFraction?.let { (it.coerceIn(0f, 1f) * effectiveDurationMs).toLong() }
        ?: currentPositionMs
    val rightTimeLabel = if (playbackSettings.showRemainingTime) {
        "-${formatDurationMs((effectiveDurationMs - displayedPositionMs).coerceAtLeast(0L))}"
    } else formatDurationMs(effectiveDurationMs)
    val configuration = LocalConfiguration.current
    // Sideways: play controls sit in the middle of the video and the bottom keeps just one row,
    // so the picture isn't covered by a tall block of buttons.
    val wide = surfaceWidthPx > surfaceHeightPx
    val skipMs = playbackSettings.skipDurationSec * 1000L
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    // Android's own gesture areas (home bar at the bottom, back at the sides, status at the top).
    // Swipes that start there belong to the system, so the player ignores them. Without this, the
    // first swipe up for Home (which only brings the hidden bars back) also changed the volume.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gestureInsets = WindowInsets.systemGestures
    val edgeTopPx = maxOf(gestureInsets.getTop(density), with(density) { 40.dp.roundToPx() })
    val edgeBottomPx = maxOf(gestureInsets.getBottom(density), with(density) { 48.dp.roundToPx() })
    val layoutDir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val edgeLeftPx = maxOf(gestureInsets.getLeft(density, layoutDir), with(density) { 24.dp.roundToPx() })
    val edgeRightPx = maxOf(gestureInsets.getRight(density, layoutDir), with(density) { 24.dp.roundToPx() })

    var dragMode by remember { mutableStateOf("NONE") }
    var dragStartSeekMs by remember { mutableLongStateOf(0L) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartVolume by remember { mutableIntStateOf(0) }
    var dragStartBrightness by remember { mutableFloatStateOf(0.5f) }

    fun setAspect(mode: VideoAspectMode) {
        aspectMode = mode
        gestureHud = Symbols.AspectRatio to mode.label
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { applyPredictiveBack(back, cornerRadius = 28.dp) }
            .background(Color.Black)
            .onSizeChanged { size ->
                surfaceWidthPx = size.width.coerceAtLeast(1)
                surfaceHeightPx = size.height.coerceAtLeast(1)
            }
            // Pinch: out = fill the screen (crop), in = fit the whole picture.
            .pointerInput(controlsLocked) {
                awaitEachGesture {
                    var zoom = 1f
                    var pinched = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!controlsLocked && event.changes.count { it.pressed } >= 2) {
                            pinched = true
                            zoom *= event.calculateZoom()
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (pinched) {
                        if (zoom > 1.12f && aspectMode != VideoAspectMode.CROP) setAspect(VideoAspectMode.CROP)
                        else if (zoom < 0.9f && aspectMode != VideoAspectMode.FIT) setAspect(VideoAspectMode.FIT)
                    }
                }
            }
            .pointerInput(controlsLocked, playbackSettings, effectiveDurationMs) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        poke()
                    },
                    onDoubleTap = { offset ->
                        if (controlsLocked) return@detectTapGestures
                        val width = surfaceWidthPx.toFloat()
                        when {
                            playbackSettings.doubleTapToSeek && offset.x < width * 0.35f -> {
                                val target = (playerController.state.value.positionMs - skipMs).coerceAtLeast(0L)
                                playerController.seekTo(target)
                                gestureHud = Symbols.FastRewind to "-${playbackSettings.skipDurationSec}s"
                            }
                            playbackSettings.doubleTapToSeek && offset.x > width * 0.65f -> {
                                val dur = playerController.state.value.durationMs.takeIf { it > 0L } ?: effectiveDurationMs
                                val target = (playerController.state.value.positionMs + skipMs).coerceAtMost(dur)
                                playerController.seekTo(target)
                                gestureHud = Symbols.FastForward to "+${playbackSettings.skipDurationSec}s"
                            }
                            playbackSettings.doubleTapToPause -> {
                                val wasPlaying = playerController.state.value.isPlaying
                                playerController.togglePlayPause()
                                gestureHud = if (wasPlaying) Symbols.Pause to "Paused" else Symbols.PlayArrow to "Playing"
                            }
                        }
                    },
                    onLongPress = {
                        if (controlsLocked || !playbackSettings.longPressFor2xSpeed) return@detectTapGestures
                        speedBeforeLongPress = playerController.state.value.playbackSpeed
                        isLongPressing2x = true
                        playerController.setSpeed(2.0f)
                        gestureHud = Symbols.Speed to "2× speed"
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (isLongPressing2x) {
                            isLongPressing2x = false
                            playerController.setSpeed(speedBeforeLongPress)
                            gestureHud = null
                        }
                    }
                )
            }
            .pointerInput(controlsLocked, playbackSettings, effectiveDurationMs, edgeTopPx, edgeBottomPx, edgeLeftPx, edgeRightPx) {
                detectDragGestures(
                    onDragStart = { start ->
                        val fromSystemEdge = start.y < edgeTopPx || start.y > surfaceHeightPx - edgeBottomPx ||
                            start.x < edgeLeftPx || start.x > surfaceWidthPx - edgeRightPx
                        dragMode = if (fromSystemEdge) "IGNORE" else "NONE"
                        dragAccumulatedX = 0f
                        dragAccumulatedY = 0f
                        dragStartX = start.x
                        dragStartSeekMs = playerController.state.value.positionMs
                        dragStartVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        val current = activity?.window?.attributes?.screenBrightness ?: -1f
                        dragStartBrightness = if (current in 0.01f..1.0f) current else 0.5f
                    },
                    onDragEnd = {
                        if (dragMode == "SEEK") playerController.setScrubbing(false)
                        dragMode = "NONE"
                    },
                    onDragCancel = {
                        if (dragMode == "SEEK") playerController.setScrubbing(false)
                        dragMode = "NONE"
                    },
                    onDrag = { change, amount ->
                        if (controlsLocked || dragMode == "IGNORE") return@detectDragGestures
                        change.consume()
                        dragAccumulatedX += amount.x
                        dragAccumulatedY += amount.y
                        if (dragMode == "NONE") {
                            if (abs(dragAccumulatedX) > 24f && abs(dragAccumulatedX) > abs(dragAccumulatedY)) {
                                if (playbackSettings.horizontalSwipeToSeek) {
                                    dragMode = "SEEK"
                                    playerController.setScrubbing(true)
                                }
                            } else if (abs(dragAccumulatedY) > 24f && abs(dragAccumulatedY) > abs(dragAccumulatedX)) {
                                val right = dragStartX >= surfaceWidthPx * 0.5f
                                if (right && playbackSettings.rightSwipeForVolume) dragMode = "VOLUME"
                                else if (!right && playbackSettings.leftSwipeForBrightness) dragMode = "BRIGHTNESS"
                            }
                        }
                        when (dragMode) {
                            "SEEK" -> {
                                val dur = playerController.state.value.durationMs.takeIf { it > 0L } ?: effectiveDurationMs
                                val delta = ((dragAccumulatedX / surfaceWidthPx.toFloat()) * 90_000f).toLong()
                                val target = (dragStartSeekMs + delta).coerceIn(0L, dur)
                                playerController.seekTo(target)
                                val sign = if (delta >= 0) "+" else "-"
                                gestureHud = (if (delta >= 0) Symbols.FastForward else Symbols.FastRewind) to
                                    "${formatDurationMs(target)} ($sign${abs(delta / 1000L)}s)"
                            }
                            "VOLUME" -> {
                                val am = audioManager ?: return@detectDragGestures
                                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val ratio = -dragAccumulatedY / (surfaceHeightPx * 0.65f)
                                val target = (dragStartVolume + (ratio * maxVol).toInt()).coerceIn(0, maxVol)
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                hudLevel = target.toFloat() / maxVol
                                gestureHud = Symbols.VolumeUp to "${target * 100 / maxVol}%"
                            }
                            "BRIGHTNESS" -> {
                                val win = activity?.window ?: return@detectDragGestures
                                val ratio = -dragAccumulatedY / (surfaceHeightPx * 0.65f)
                                val target = (dragStartBrightness + ratio).coerceIn(0.02f, 1.0f)
                                win.attributes = win.attributes.also { it.screenBrightness = target }
                                hudLevel = target
                                gestureHud = Symbols.BrightnessMedium to "${(target * 100).toInt()}%"
                            }
                        }
                    }
                )
            }
            .testTag("video_player_modal")
    ) {
        // 1. Video (official Media3 Compose ContentFrame)
        playerController.RenderVideoSurface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .graphicsLayer { alpha = surfaceAlpha.value },
            contentScale = when (aspectMode) {
                VideoAspectMode.FIT -> ContentScale.Fit
                VideoAspectMode.CROP -> ContentScale.Crop
                VideoAspectMode.STRETCH -> ContentScale.FillBounds
            },
            // The video's own thumbnail until its first frame is ready (not a black box).
            placeholder = {
                val thumb = remember(video.id) { MediaStoreScanner.getCachedThumbnail(video.id) }
                if (thumb != null) {
                    val image = remember(thumb) { thumb.asImageBitmap() }
                    androidx.compose.foundation.Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )

        if (audioOnly) {
            AudioOnlyCover(
                title = video.title,
                wide = wide,
                onShowVideo = { audioOnly = false; poke() },
                // Sideways the play buttons are part of the card (beside it), so nothing overlaps.
                controls = if (wide) {
                    {
                        PlayerCentreControls(
                            isPlaying = playerState.isPlaying,
                            buffering = false,
                            colorful = playbackSettings.colorfulControls,
                            onPrevious = { goPrevious(); poke() },
                            onPlayPause = { playerController.togglePlayPause(); poke() },
                            onNext = { goNext(); poke() }
                        )
                    }
                } else null,
                modifier = Modifier.fillMaxSize()
            )
        }

        val centreControlsShown = wide && controlsVisible && !controlsLocked && !inPip
        if (showBuffering && !audioOnly && !centreControlsShown) {
            ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center).size(56.dp))
        }

        // 2. Subtitles with their own styling (Media3 SubtitleView)
        if (playerTracks.subtitlesEnabled) {
            StyledSubtitles(
                playerState.cues,
                size = playbackSettings.subtitleSize,
                background = playbackSettings.subtitleBackground,
                liftForControls = controlsVisible && !controlsLocked && !inPip,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Gesture feedback. Volume and brightness show a thin upright level bar on the side
        //    being swiped; everything else is a small chip in the middle.
        val hud = gestureHud
        val level = hudLevel
        if (hud != null && !inPip) {
            val isLevel = level != null && (hud.first === Symbols.VolumeUp || hud.first === Symbols.BrightnessMedium)
            if (isLevel && level != null) {
                LevelHud(
                    icon = hud.first,
                    label = hud.second,
                    level = level,
                    modifier = Modifier
                        .align(if (hud.first === Symbols.VolumeUp) Alignment.CenterEnd else Alignment.CenterStart)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 28.dp)
                        .testTag("player_level_hud")
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.Black.copy(alpha = 0.72f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.Center).testTag("player_gesture_hud")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(hud.first, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text(hud.second, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // 4. Resumed pill (Material 3 snackbar)
        AnimatedVisibility(
            visible = showResumedPill && !inPip && !controlsLocked,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                androidx.compose.animation.slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 2 },
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                androidx.compose.animation.slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = when {
                        !controlsVisible -> 24.dp
                        wide -> 88.dp
                        else -> 148.dp
                    }
                )
        ) {
            androidx.compose.material3.Snackbar(
                action = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            playerController.seekTo(0L)
                            showResumedPill = false
                        },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                    ) { Text("Start over") }
                },
                dismissAction = {
                    IconButton(onClick = { showResumedPill = false }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Symbols.Close, contentDescription = "Dismiss")
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.widthIn(max = 480.dp).testTag("player_resumed_pill")
            ) {
                Text("Resumed from ${formatDurationMs(resumedFromMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // 5. Controls
        AnimatedVisibility(
            visible = controlsVisible && !inPip && !closing && !backInProgress,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                if (controlsLocked) {
                    FilledTonalButton(
                        onClick = {
                            controlsLocked = false
                            poke()
                        },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(bottom = 32.dp)
                    ) {
                        Icon(Symbols.Lock, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Tap to unlock")
                    }
                } else {
                    PlayerTopBar(
                        title = video.title,
                        subtitlesOn = playerTracks.subtitlesEnabled && playerTracks.subtitleTracks.any { it.isSelected },
                        showSubtitles = !audioOnly,
                        speed = playerState.playbackSpeed,
                        sleepLabel = when {
                            sleepEndOfVideo -> "End"
                            sleepDeadline != null -> formatDurationMs(sleepRemainingMs + 999L)
                            else -> null
                        },
                        onBack = closeAndPersist,
                        onSubtitles = { showSubtitleSheet = true; poke() },
                        onSpeed = { showSpeedSheet = true; poke() },
                        onSleep = { showSleepSheet = true; poke() },
                        onLock = { controlsLocked = true; poke() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    PlayerBottomBar(
                        isPlaying = playerState.isPlaying,
                        positionLabel = formatDurationMs(displayedPositionMs),
                        rightLabel = rightTimeLabel,
                        progress = progressFraction,
                        rotationLocked = rotationOverride != null,
                        showRotate = !audioOnly,
                        onSeekPreview = { f ->
                            // Scrubbing mode (Media3): quick, frequent seeks while dragging, so
                            // the picture follows the finger.
                            if (seekPreviewFraction == null) playerController.setScrubbing(true)
                            seekPreviewFraction = f
                            playerController.seekTo((f.coerceIn(0f, 1f) * effectiveDurationMs).toLong())
                            poke()
                        },
                        onSeekFinished = { f ->
                            seekPreviewFraction = null
                            playerController.setScrubbing(false)
                            playerController.seekTo((f * effectiveDurationMs).toLong())
                            poke()
                        },
                        onPlayPause = { playerController.togglePlayPause(); poke() },
                        onPrevious = goPrevious,
                        onNext = goNext,
                        onRotate = {
                            val landscapeNow = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                            rotationOverride = if (landscapeNow) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            gestureHud = Symbols.ScreenRotation to (if (landscapeNow) "Portrait" else "Landscape")
                            poke()
                        },
                        onRotateLongPress = {
                            rotationOverride = null
                            gestureHud = Symbols.ScreenRotation to "Auto-rotate"
                            poke()
                        },
                        onMore = { showMoreMenu = true; poke() },
                        compact = wide,
                        moreMenu = {
                            PlayerMoreMenu(
                                expanded = showMoreMenu,
                                onDismiss = { showMoreMenu = false; poke() },
                                audioOnly = audioOnly,
                                onAudioOnly = { on -> audioOnly = on },
                                sleepStatus = when {
                                    sleepEndOfVideo -> "At the end of this video"
                                    sleepDeadline != null -> "${formatDurationMs(sleepRemainingMs + 999L)} left"
                                    else -> "Off"
                                },
                                onSleep = { showSleepSheet = true },
                                aspectLabel = if (audioOnly) null else aspectMode.label,
                                onAspect = {
                                    setAspect(VideoAspectMode.entries[(aspectMode.ordinal + 1) % VideoAspectMode.entries.size])
                                },
                                showPip = playbackSettings.pictureInPicture && activity != null && !audioOnly,
                                onPip = { enterPip(activity, video.width, video.height, playerState.isPlaying) },
                                audioTrackLabel = playerTracks.audioTracks.takeIf { it.size > 1 }
                                    ?.let { tracks -> tracks.firstOrNull { it.isSelected }?.label ?: "Choose" },
                                onAudioTrack = { showAudioSheet = true },
                                onDetails = { showInfoSheet = true }
                            )
                        },

                        isPlayingForSeekBar = playerState.isPlaying && !playerState.isEnded,
                        colorful = playbackSettings.colorfulControls,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    if (wide && !audioOnly) {
                        PlayerCentreControls(
                            isPlaying = playerState.isPlaying,
                            buffering = showBuffering && !audioOnly,
                            colorful = playbackSettings.colorfulControls,
                            onPrevious = { goPrevious(); poke() },
                            onPlayPause = { playerController.togglePlayPause(); poke() },
                            onNext = { goNext(); poke() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    // ---- Sheets (on top of the playing video) ----
    if (showSpeedSheet) {
        SpeedSheet(
            speed = playerState.playbackSpeed,
            onSpeed = { playerController.setSpeed(it) },
            onDismiss = { showSpeedSheet = false }
        )
    }
    if (showAudioSheet) {
        TrackSheet(
            title = "Audio",
            icon = Symbols.Audiotrack,
            tracks = playerTracks.audioTracks,
            onSelect = { playerController.selectAudioTrack(it); showAudioSheet = false },
            onDismiss = { showAudioSheet = false }
        )
    }
    if (showSubtitleSheet) {
        SubtitleSheet(
            tracks = playerTracks.subtitleTracks,
            enabled = playerTracks.subtitlesEnabled,
            settings = playbackSettings,
            onSelect = { playerController.selectSubtitleTrack(it) },
            onOff = { playerController.setSubtitlesEnabled(false) },
            onLoadFile = { uri, name -> playerController.addSubtitleFile(uri, name) },
            onUpdateSettings = onUpdatePlaybackSettings,
            onDismiss = { showSubtitleSheet = false }
        )
    }
    if (showSleepSheet) {
        SleepTimerSheet(
            active = sleepDeadline != null || sleepEndOfVideo,
            atEndActive = sleepEndOfVideo,
            remainingMs = sleepRemainingMs,
            endLabel = "At the end of this video",
            endShortLabel = "End of video",
            endIcon = Symbols.Movie,
            onStart = { minutes ->
                sleepEndOfVideo = false
                sleepRemainingMs = minutes * 60_000L
                sleepDeadline = SystemClock.elapsedRealtime() + minutes * 60_000L
                gestureHud = Symbols.Bedtime to "Sleep in $minutes min"
                showSleepSheet = false
            },
            onAtEnd = {
                sleepDeadline = null
                sleepEndOfVideo = true
                gestureHud = Symbols.Bedtime to "Sleep at the end of this video"
                showSleepSheet = false
            },
            onTurnOff = {
                sleepDeadline = null
                sleepEndOfVideo = false
                gestureHud = Symbols.Bedtime to "Sleep timer off"
                showSleepSheet = false
            },
            onDismiss = { showSleepSheet = false }
        )
    }
    if (showInfoSheet) {
        VideoDetailsSheet(
            video = video,
            onDismiss = { showInfoSheet = false }
        )
    }
}

/**
 * Upright level bar for volume / brightness: percentage, a thin bar, then the icon. The bar is the
 * official (thin, 4 dp) Material 3 progress indicator stood on its end, filling from the bottom.
 */
@Composable
private fun LevelHud(icon: ImageVector, label: String, level: Float, modifier: Modifier = Modifier) {
    val barLength = 150.dp
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(width = 24.dp, height = barLength), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    progress = { level.coerceIn(0f, 1f) },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .requiredWidth(barLength)
                        .graphicsLayer { rotationZ = -90f }
                        .clearAndSetSemantics {}
                )
            }
            Spacer(Modifier.height(12.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bars
// ---------------------------------------------------------------------------------------------

/** See-through dark fill for buttons over the video: readable on bright and dark pictures alike. */
private val OverlayContainer = Color.Black.copy(alpha = 0.45f)

/** Translucent tonal colours for the buttons drawn over the video. */
@Composable
private fun overlayButtonColors(active: Boolean = false) = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else OverlayContainer,
    contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
)

/**
 * An Expressive tonal icon button for the player (the round shape morphs to a squircle when
 * pressed). [onLongClick], if given, fires after a long press instead of the normal click.
 */
@Composable
private fun PlayerIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    var longFired by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val longLatest by rememberUpdatedState(onLongClick)
    if (onLongClick != null) {
        LaunchedEffect(interaction) {
            var job: kotlinx.coroutines.Job? = null
            interaction.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> {
                        longFired = false
                        job?.cancel()
                        job = launch {
                            delay(500L)
                            longFired = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            longLatest?.invoke()
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> job?.cancel()
                }
            }
        }
    }
    FilledTonalIconButton(
        onClick = { if (longFired) longFired = false else onClick() },
        shapes = IconButtonDefaults.shapes(),
        colors = overlayButtonColors(active),
        interactionSource = interaction,
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) { content() }
}

@Composable
private fun PlayerTopBar(
    title: String,
    subtitlesOn: Boolean,
    showSubtitles: Boolean,
    speed: Float,
    sleepLabel: String?,
    onBack: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val white = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes(), colors = white, modifier = Modifier.testTag("player_close_btn")) {
            Icon(Symbols.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sleepLabel != null) {
                AssistChip(
                    onClick = onSleep,
                    label = { Text(sleepLabel, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(Symbols.Bedtime, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = OverlayContainer,
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp).testTag("player_sleep_chip")
                )
            }
        }
        // Order: subtitles, speed, lock (lock up here so it's a deliberate tap).
        if (showSubtitles) {
            PlayerIconButton(onClick = onSubtitles, contentDescription = "Subtitles", active = subtitlesOn, modifier = Modifier.testTag("player_subtitles_btn")) {
                Icon(if (subtitlesOn) Symbols.ClosedCaption else Symbols.ClosedCaptionDisabled, contentDescription = null)
            }
        }
        PlayerIconButton(onClick = onSpeed, contentDescription = "Playback speed", active = speed != 1.0f, modifier = Modifier.testTag("player_speed_btn")) {
            if (speed != 1.0f) {
                Text(speedLabel(speed), style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Symbols.Speed, contentDescription = null)
            }
        }
        PlayerIconButton(onClick = onLock, contentDescription = "Lock controls", modifier = Modifier.testTag("player_lock_btn")) {
            Icon(Symbols.LockOpen, contentDescription = null)
        }
    }
}

@Composable
private fun PlayerBottomBar(
    isPlaying: Boolean,
    positionLabel: String,
    rightLabel: String,
    progress: Float,
    rotationLocked: Boolean,
    showRotate: Boolean,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRotate: () -> Unit,
    onRotateLongPress: () -> Unit,
    onMore: () -> Unit,
    moreMenu: @Composable () -> Unit,
    isPlayingForSeekBar: Boolean,
    colorful: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // Rotate: tap flips portrait/landscape, hold goes back to auto-rotate.
    val rotateButton: @Composable () -> Unit = {
        PlayerIconButton(
            onClick = onRotate,
            onLongClick = onRotateLongPress,
            contentDescription = "Rotate (hold for auto-rotate)",
            active = rotationLocked,
            modifier = Modifier.testTag("player_rotate_btn")
        ) {
            Icon(Symbols.ScreenRotation, contentDescription = null)
        }
    }
    // ⋮ at the bottom right, under the thumb; its menu opens upwards.
    val moreButton: @Composable () -> Unit = {
        Box {
            PlayerIconButton(onClick = onMore, contentDescription = "More", modifier = Modifier.testTag("player_more_btn")) {
                Icon(Symbols.MoreVert, contentDescription = null)
            }
            moreMenu()
        }
    }
    val scrim = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
    if (compact) {
        // Sideways: one slim row (rotate, time, seek bar, time, ⋮). Play, previous and next are
        // in the middle of the video instead.
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(scrim)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRotate) {
                rotateButton()
                Spacer(Modifier.width(12.dp))
            }
            Text(positionLabel, color = Color.White, style = MaterialTheme.typography.labelLarge)
            PlaybackSeekBar(
                progress = progress,
                onSeekPreview = onSeekPreview,
                onSeekFinished = onSeekFinished,
                isPlaying = isPlayingForSeekBar,
                activeColor = if (colorful) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).testTag("player_seek_slider")
            )
            Text(rightLabel, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(12.dp))
            moreButton()
        }
        return
    }
    // "Colourful player controls" (Me › Appearance): theme colours; otherwise plain white.
    val scheme = MaterialTheme.colorScheme
    val tonal = if (colorful) {
        IconButtonDefaults.filledTonalIconButtonColors()
    } else {
        IconButtonDefaults.filledTonalIconButtonColors(containerColor = OverlayContainer, contentColor = Color.White)
    }
    val playColors = if (colorful) {
        IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            checkedContainerColor = scheme.primary,
            checkedContentColor = scheme.onPrimary
        )
    } else {
        IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            checkedContainerColor = Color.White,
            checkedContentColor = Color.Black
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scrim)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(positionLabel, color = Color.White, style = MaterialTheme.typography.labelLarge)
            PlaybackSeekBar(
                progress = progress,
                onSeekPreview = onSeekPreview,
                onSeekFinished = onSeekFinished,
                isPlaying = isPlayingForSeekBar,
                activeColor = if (colorful) scheme.primary else Color.White,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).testTag("player_seek_slider")
            )
            Text(rightLabel, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Same width kept in audio-only mode, so the play buttons don't move.
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (showRotate) rotateButton()
            }
            // Official Expressive button group: the pressed button springs wider and squeezes
            // its neighbours; the shapes morph on press.
            val prevInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            ButtonGroup(
                overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                customItem(
                    buttonGroupContent = {
                        FilledTonalIconButton(
                            onClick = onPrevious,
                            shapes = IconButtonDefaults.shapes(),
                            colors = tonal,
                            interactionSource = prevInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .animateWidth(prevInteraction)
                                .height(56.dp)
                        ) { Icon(Symbols.SkipPrevious, contentDescription = "Previous video", modifier = Modifier.size(28.dp)) }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(text = { Text("Previous") }, onClick = { onPrevious(); menuState.dismiss() })
                    }
                )
                customItem(
                    buttonGroupContent = {
                        // Toggle button: keeps its round shape (play and pause look the same); it
                        // squishes to the official pressed shape while touched.
                        FilledIconToggleButton(
                            checked = !isPlaying,
                            onCheckedChange = { onPlayPause() },
                            shapes = IconButtonDefaults.toggleableShapes(
                                shape = IconButtonDefaults.mediumRoundShape,
                                pressedShape = IconButtonDefaults.mediumPressedShape,
                                checkedShape = IconButtonDefaults.mediumRoundShape
                            ),
                            colors = playColors,
                            interactionSource = playInteraction,
                            modifier = Modifier
                                .weight(1.4f)
                                .animateWidth(playInteraction)
                                .height(64.dp)
                                .testTag("player_play_pause_btn")
                        ) {
                            Icon(
                                if (isPlaying) Symbols.Pause else Symbols.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
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
                            colors = tonal,
                            interactionSource = nextInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .animateWidth(nextInteraction)
                                .height(56.dp)
                        ) { Icon(Symbols.SkipNext, contentDescription = "Next video", modifier = Modifier.size(28.dp)) }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(text = { Text("Next") }, onClick = { onNext(); menuState.dismiss() })
                    }
                )
            }
            moreButton()
        }
    }
}

/**
 * Sideways: previous, play/pause and next in the middle of the video, like Google's own players.
 * Official Expressive icon buttons in a button group: a large round play button (squishing when
 * pressed, the pressed button springs wider and squeezes its neighbours) between two medium ones. While the video loads, the play
 * button shows Material's loading indicator.
 */
@Composable
private fun PlayerCentreControls(
    isPlaying: Boolean,
    buffering: Boolean,
    colorful: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val sideColors = if (colorful) {
        IconButtonDefaults.filledTonalIconButtonColors()
    } else {
        IconButtonDefaults.filledTonalIconButtonColors(containerColor = OverlayContainer, contentColor = Color.White)
    }
    val playColors = if (colorful) {
        IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            checkedContainerColor = scheme.primary,
            checkedContentColor = scheme.onPrimary
        )
    } else {
        IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            checkedContainerColor = Color.White,
            checkedContentColor = Color.Black
        )
    }
    val sideShapes = IconButtonDefaults.shapes(
        shape = IconButtonDefaults.mediumRoundShape,
        pressedShape = IconButtonDefaults.mediumPressedShape
    )
    // Official Expressive button group, like the portrait bar: the pressed button springs wider
    // and squeezes its neighbours (that's the "tactile" feel), and the shapes morph on press.
    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = onPrevious,
                    shapes = sideShapes,
                    colors = sideColors,
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .animateWidth(prevInteraction)
                        .size(IconButtonDefaults.mediumContainerSize())
                        .testTag("player_previous_btn")
                ) { Icon(Symbols.SkipPrevious, contentDescription = "Previous video", modifier = Modifier.size(IconButtonDefaults.mediumIconSize)) }
            },
            menuContent = { menuState ->
                DropdownMenuItem(text = { Text("Previous") }, onClick = { onPrevious(); menuState.dismiss() })
            }
        )
        customItem(
            buttonGroupContent = {
                FilledIconToggleButton(
                    checked = !isPlaying,
                    onCheckedChange = { onPlayPause() },
                    shapes = IconButtonDefaults.toggleableShapes(
                        shape = IconButtonDefaults.largeRoundShape,
                        pressedShape = IconButtonDefaults.largePressedShape,
                        checkedShape = IconButtonDefaults.largeRoundShape
                    ),
                    colors = playColors,
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .animateWidth(playInteraction)
                        .size(IconButtonDefaults.largeContainerSize())
                        .testTag("player_play_pause_btn")
                ) {
                    if (buffering) {
                        LoadingIndicator(
                            color = if (colorful) scheme.onPrimary else Color.Black,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(
                            if (isPlaying) Symbols.Pause else Symbols.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(IconButtonDefaults.largeIconSize)
                        )
                    }
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
                    shapes = sideShapes,
                    colors = sideColors,
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .animateWidth(nextInteraction)
                        .size(IconButtonDefaults.mediumContainerSize())
                        .testTag("player_next_btn")
                ) { Icon(Symbols.SkipNext, contentDescription = "Next video", modifier = Modifier.size(IconButtonDefaults.mediumIconSize)) }
            },
            menuContent = { menuState ->
                DropdownMenuItem(text = { Text("Next") }, onClick = { onNext(); menuState.dismiss() })
            }
        )
    }
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "${("%.2f".format(java.util.Locale.US, speed)).trimEnd('0').trimEnd('.')}×"

// ---------------------------------------------------------------------------------------------
// Subtitles
// ---------------------------------------------------------------------------------------------

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StyledSubtitles(
    cues: List<androidx.media3.common.text.Cue>,
    size: SubtitleSize,
    background: Boolean,
    liftForControls: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            SubtitleView(ctx).apply {
                setApplyEmbeddedStyles(true)
                setApplyEmbeddedFontSizes(false)
            }
        },
        update = { view ->
            view.setStyle(
                CaptionStyleCompat(
                    Color.White.toArgb(),
                    if (background) Color.Black.copy(alpha = 0.6f).toArgb() else Color.Transparent.toArgb(),
                    Color.Transparent.toArgb(),
                    if (background) CaptionStyleCompat.EDGE_TYPE_NONE else CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    Color.Black.toArgb(),
                    null
                )
            )
            view.setFractionalTextSize(size.fraction)
            view.setBottomPaddingFraction(if (liftForControls) 0.2f else 0.08f)
            view.setCues(cues)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtitleSheet(
    tracks: List<PlayerTrackOption>,
    enabled: Boolean,
    settings: PlaybackSettings,
    onSelect: (PlayerTrackOption) -> Unit,
    onOff: () -> Unit,
    onLoadFile: (android.net.Uri, String) -> Unit,
    onUpdateSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Subtitles.srt"
            onLoadFile(uri, name)
            onDismiss()
        }
    }
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("player_subtitle_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Subtitles", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            val offSelected = !enabled || tracks.none { it.isSelected }
            val count = tracks.size + 2
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                SegmentedListItem(
                    selected = offSelected,
                    onClick = { onOff(); onDismiss() },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = count),
                    leadingContent = { RadioButton(selected = offSelected, onClick = null) }
                ) { Text("Off") }
                tracks.forEachIndexed { i, t ->
                    val sel = enabled && t.isSelected
                    SegmentedListItem(
                        selected = sel,
                        onClick = { onSelect(t); onDismiss() },
                        shapes = ListItemDefaults.segmentedShapes(index = i + 1, count = count),
                        leadingContent = { RadioButton(selected = sel, onClick = null) }
                    ) { Text(t.label) }
                }
                SegmentedListItem(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    shapes = ListItemDefaults.segmentedShapes(index = count - 1, count = count),
                    leadingContent = { Icon(Symbols.FileOpen, contentDescription = null) },
                    supportingContent = { Text(".srt, .vtt, .ass, .ssa or .ttml") },
                    modifier = Modifier.testTag("subtitle_load_file")
                ) { Text("Load subtitle file") }
            }
            Spacer(Modifier.height(16.dp))
            CompactRow("Text size") {
                ChoiceButtonGroup(
                    options = SubtitleSize.entries.map { it to it.label },
                    selected = settings.subtitleSize,
                    onSelect = { size -> onUpdateSettings { it.copy(subtitleSize = size) } },
                    tag = { "subtitle_size_${it.name.lowercase()}" }
                )
            }
            Spacer(Modifier.height(10.dp))
            SegmentedListItem(
                checked = settings.subtitleBackground,
                onCheckedChange = { on -> onUpdateSettings { it.copy(subtitleBackground = on) } },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                supportingContent = { Text("Dark box behind the text; off shows outlined text") },
                trailingContent = { Switch(checked = settings.subtitleBackground, onCheckedChange = null) },
                colors = ListItemDefaults.colors(
                    containerColor = scheme.surfaceContainerHigh,
                    selectedContainerColor = scheme.surfaceContainerHigh
                )
            ) { Text("Background") }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Speed & audio
// ---------------------------------------------------------------------------------------------

/** Speed: two rows of the official Expressive standard button group. */
@Composable
private fun SpeedSheet(speed: Float, onSpeed: (Float) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val rows = listOf(listOf(0.5f, 0.75f, 1f, 1.25f), listOf(1.5f, 1.75f, 2f, 3f))
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("player_speed_sheet")
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(speedLabel(speed), style = MaterialTheme.typography.titleLarge, color = scheme.primary)
                if (abs(speed - 1f) > 0.01f) {
                    androidx.compose.material3.TextButton(onClick = { onSpeed(1f) }, shapes = ButtonDefaults.shapes()) { Text("Reset") }
                }
            }
            rows.forEach { row ->
                ChoiceButtonGroup(
                    options = row.map { it to speedLabel(it) },
                    selected = row.firstOrNull { abs(it - speed) < 0.01f },
                    onSelect = { v -> if (v != null) onSpeed(v) },
                    tag = { "speed_${it}" }
                )
            }
        }
    }
}

@Composable
private fun TrackSheet(
    title: String,
    icon: ImageVector,
    tracks: List<PlayerTrackOption>,
    onSelect: (PlayerTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                tracks.forEachIndexed { i, t ->
                    SegmentedListItem(
                        selected = t.isSelected,
                        onClick = { onSelect(t) },
                        shapes = ListItemDefaults.segmentedShapes(index = i, count = tracks.size),
                        leadingContent = { RadioButton(selected = t.isSelected, onClick = null) }
                    ) { Text(t.label) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Audio only, More, Sleep timer
// ---------------------------------------------------------------------------------------------

/** Shown instead of the picture while only the sound plays. */
@Composable
private fun AudioOnlyCover(
    title: String,
    wide: Boolean,
    onShowVideo: () -> Unit,
    controls: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val details: @Composable (Alignment.Horizontal, androidx.compose.ui.text.style.TextAlign, androidx.compose.ui.unit.Dp) -> Unit =
        { alignment, textAlign, badgeSize ->
            Column(horizontalAlignment = alignment) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                    modifier = Modifier.size(badgeSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Symbols.Headphones, contentDescription = null, modifier = Modifier.size(badgeSize * 0.46f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign
                )
                Spacer(Modifier.height(4.dp))
                Text("Playing audio only", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onShowVideo,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("player_show_video_btn")
                ) {
                    Icon(Symbols.Videocam, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Show video")
                }
            }
        }
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (wide && controls != null) {
            // Sideways: the song card on the left, the play buttons on the right.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 48.dp)
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    details(Alignment.Start, androidx.compose.ui.text.style.TextAlign.Start, 80.dp)
                }
                Spacer(Modifier.width(32.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { controls() }
            }
        } else {
            Box(Modifier.padding(horizontal = 32.dp)) {
                details(Alignment.CenterHorizontally, androidx.compose.ui.text.style.TextAlign.Center, 112.dp)
            }
        }
    }
}

/**
 * The ⋮ menu: the official Material 3 Expressive vertical menu (same as the ⋮ on a video in the
 * list), anchored to the button. Everyday options in one group, Details on its own.
 */
@Composable
private fun PlayerMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    audioOnly: Boolean,
    onAudioOnly: (Boolean) -> Unit,
    sleepStatus: String,
    onSleep: () -> Unit,
    aspectLabel: String?,
    onAspect: () -> Unit,
    showPip: Boolean,
    onPip: () -> Unit,
    audioTrackLabel: String?,
    onAudioTrack: () -> Unit,
    onDetails: () -> Unit
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("player_more_menu")
    ) {
        val count = 2 + (if (aspectLabel != null) 1 else 0) + (if (showPip) 1 else 0) + (if (audioTrackLabel != null) 1 else 0)
        var i = 0
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
            CheckableDropdownMenuItem(
                checked = audioOnly,
                onCheckedChange = { on -> onDismiss(); onAudioOnly(on) },
                text = { Text("Audio only") },
                supportingText = { Text("Keeps playing when you leave") },
                shapes = MenuDefaults.itemShape(index = i++, count = count),
                leadingIcon = { Icon(Symbols.Headphones, contentDescription = null) },
                checkedLeadingIcon = { Icon(Symbols.Check, contentDescription = null) },
                modifier = Modifier.testTag("player_audio_only_item")
            )
            DropdownMenuItem(
                onClick = { onDismiss(); onSleep() },
                text = { Text("Sleep timer") },
                supportingText = { Text(sleepStatus) },
                shape = MenuDefaults.itemShape(index = i++, count = count).shape,
                leadingIcon = { Icon(Symbols.Bedtime, contentDescription = null) }
            )
            if (aspectLabel != null) {
                DropdownMenuItem(
                    onClick = { onDismiss(); onAspect() },
                    text = { Text("Aspect ratio") },
                    supportingText = { Text(aspectLabel) },
                    shape = MenuDefaults.itemShape(index = i++, count = count).shape,
                    leadingIcon = { Icon(Symbols.AspectRatio, contentDescription = null) }
                )
            }
            if (showPip) {
                DropdownMenuItem(
                    onClick = { onDismiss(); onPip() },
                    text = { Text("Picture-in-picture") },
                    shape = MenuDefaults.itemShape(index = i++, count = count).shape,
                    leadingIcon = { Icon(Symbols.PictureInPictureAlt, contentDescription = null) }
                )
            }
            if (audioTrackLabel != null) {
                DropdownMenuItem(
                    onClick = { onDismiss(); onAudioTrack() },
                    text = { Text("Audio track") },
                    supportingText = { Text(audioTrackLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    shape = MenuDefaults.itemShape(index = i, count = count).shape,
                    leadingIcon = { Icon(Symbols.Audiotrack, contentDescription = null) }
                )
            }
        }
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
            DropdownMenuItem(
                onClick = { onDismiss(); onDetails() },
                text = { Text("Details") },
                shape = MenuDefaults.itemShape(index = 0, count = 1).shape,
                leadingIcon = { Icon(Symbols.Info, contentDescription = null) }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Picture-in-picture
// ---------------------------------------------------------------------------------------------

@Composable
private fun rememberIsInPictureInPicture(activity: Activity?): Boolean {
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    val component = activity as? ComponentActivity
    DisposableEffect(component) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        component?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { component?.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPip
}

private fun pipRatio(width: Int, height: Int): Rational {
    if (width <= 0 || height <= 0) return Rational(16, 9)
    val r = width.toFloat() / height.toFloat()
    return when {
        r > 2.39f -> Rational(239, 100)
        r < 1f / 2.39f -> Rational(100, 239)
        else -> Rational(width, height)
    }
}

private fun pipParams(
    context: Context,
    width: Int,
    height: Int,
    isPlaying: Boolean,
    autoEnter: Boolean
): PictureInPictureParams {
    fun action(cmd: Int, icon: Int, label: String) = RemoteAction(
        PlatformIcon.createWithResource(context, icon),
        label,
        label,
        PendingIntent.getBroadcast(
            context,
            cmd,
            Intent(ACTION_PIP_CONTROL).setPackage(context.packageName).putExtra(EXTRA_PIP_CMD, cmd),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    )
    return PictureInPictureParams.Builder()
        .setAspectRatio(pipRatio(width, height))
        .setAutoEnterEnabled(autoEnter)
        .setSeamlessResizeEnabled(true)
        .setActions(
            listOf(
                action(PIP_PREVIOUS, R.drawable.ic_pip_previous, "Previous"),
                if (isPlaying) action(PIP_PLAY_PAUSE, R.drawable.ic_pip_pause, "Pause")
                else action(PIP_PLAY_PAUSE, R.drawable.ic_pip_play, "Play"),
                action(PIP_NEXT, R.drawable.ic_pip_next, "Next")
            )
        )
        .build()
}

private fun enterPip(activity: Activity?, width: Int, height: Int, isPlaying: Boolean) {
    activity ?: return
    runCatching { activity.enterPictureInPictureMode(pipParams(activity, width, height, isPlaying, autoEnter = true)) }
}

/** Keeps the PiP window's buttons and "auto-enter when leaving the app" in step with playback. */
@Composable
private fun PictureInPictureSupport(
    activity: Activity?,
    enabled: Boolean,
    isPlaying: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    if (activity == null) return
    LaunchedEffect(enabled, isPlaying, videoWidth, videoHeight) {
        runCatching {
            activity.setPictureInPictureParams(
                if (enabled) pipParams(activity, videoWidth, videoHeight, isPlaying, autoEnter = isPlaying)
                else PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
            )
        }
    }
    DisposableEffect(activity) {
        onDispose {
            runCatching {
                activity.setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
            }
        }
    }
    val playPause by rememberUpdatedState(onPlayPause)
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    DisposableEffect(activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.getIntExtra(EXTRA_PIP_CMD, 0)) {
                    PIP_PLAY_PAUSE -> playPause()
                    PIP_NEXT -> next()
                    PIP_PREVIOUS -> previous()
                }
            }
        }
        ContextCompat.registerReceiver(
            activity, receiver, IntentFilter(ACTION_PIP_CONTROL), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { activity.unregisterReceiver(receiver) } }
    }
}
