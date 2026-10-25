@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon as PlatformIcon
import android.media.AudioManager
import android.provider.OpenableColumns
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val playerState by playerController.state.collectAsStateWithLifecycle()
    val playerTracks by playerController.tracks.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var gestureHud by remember { mutableStateOf<Pair<ImageVector, String>?>(null) }
    var isLongPressing2x by remember { mutableStateOf(false) }
    var speedBeforeLongPress by remember { mutableFloatStateOf(1.0f) }
    var aspectMode by remember { mutableStateOf(VideoAspectMode.FIT) }

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }

    // Audio only: the picture is switched off, the sound keeps going (also after leaving the app).
    var audioOnly by remember { mutableStateOf(false) }
    // Rotate button: null = follow the setting / sensor.
    var rotationOverride by remember { mutableStateOf<Int?>(null) }
    // Sleep timer: a deadline (elapsed-realtime clock) or "at the end of this video".
    var sleepDeadline by remember { mutableStateOf<Long?>(null) }
    var sleepEndOfVideo by remember { mutableStateOf(false) }
    var sleepRemainingMs by remember { mutableLongStateOf(0L) }

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
    val keepScreenOn = playerState.isPlaying && !audioOnly
    DisposableEffect(activity, keepScreenOn) {
        if (keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val view = LocalView.current
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
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
            playerController.setVolume(1f)
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
            if (st.positionMs > 0L && dur > 0L) saveLatest(st.positionMs, dur, false)
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
    LaunchedEffect(controlsVisible, playerState.isPlaying, interactionTick, controlsLocked) {
        if (controlsVisible && playerState.isPlaying && !audioOnly) {
            delay(if (controlsLocked) 1_500L else 3_500L)
            controlsVisible = false
        }
    }
    LaunchedEffect(gestureHud) {
        if (gestureHud != null && !isLongPressing2x) {
            delay(900L)
            gestureHud = null
        }
    }

    val closeAndPersist = {
        val dur = playerState.durationMs.takeIf { it > 0L } ?: video.durationMs
        onSavePosition(playerState.positionMs, dur, true)
        playerController.setBackgroundAudio(false)
        onClose()
    }
    BackHandler(onBack = closeAndPersist)

    // ---- Picture-in-picture ----
    PictureInPictureSupport(
        activity = activity,
        enabled = playbackSettings.pictureInPicture && !audioOnly,
        isPlaying = playerState.isPlaying,
        videoWidth = video.width,
        videoHeight = video.height,
        onPlayPause = { playerController.togglePlayPause() },
        onNext = onNextVideo,
        onPrevious = onPreviousVideo
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
    val displayedPositionMs = seekPreviewFraction?.let { (it.coerceIn(0f, 1f) * effectiveDurationMs).toLong() }
        ?: currentPositionMs
    val rightTimeLabel = if (playbackSettings.showRemainingTime) {
        "-${formatDurationMs((effectiveDurationMs - displayedPositionMs).coerceAtLeast(0L))}"
    } else formatDurationMs(effectiveDurationMs)
    val configuration = LocalConfiguration.current
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
        modifier = Modifier
            .fillMaxSize()
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
                    onDragEnd = { dragMode = "NONE" },
                    onDragCancel = { dragMode = "NONE" },
                    onDrag = { change, amount ->
                        if (controlsLocked || dragMode == "IGNORE") return@detectDragGestures
                        change.consume()
                        dragAccumulatedX += amount.x
                        dragAccumulatedY += amount.y
                        if (dragMode == "NONE") {
                            if (abs(dragAccumulatedX) > 24f && abs(dragAccumulatedX) > abs(dragAccumulatedY)) {
                                if (playbackSettings.horizontalSwipeToSeek) dragMode = "SEEK"
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
                                gestureHud = Symbols.VolumeUp to "Volume ${target * 100 / maxVol}%"
                            }
                            "BRIGHTNESS" -> {
                                val win = activity?.window ?: return@detectDragGestures
                                val ratio = -dragAccumulatedY / (surfaceHeightPx * 0.65f)
                                val target = (dragStartBrightness + ratio).coerceIn(0.02f, 1.0f)
                                win.attributes = win.attributes.also { it.screenBrightness = target }
                                gestureHud = Symbols.BrightnessMedium to "Brightness ${(target * 100).toInt()}%"
                            }
                        }
                    }
                )
            }
            .testTag("video_player_modal")
    ) {
        // 1. Video (official Media3 Compose ContentFrame)
        playerController.RenderVideoSurface(
            modifier = Modifier.align(Alignment.Center).fillMaxSize(),
            contentScale = when (aspectMode) {
                VideoAspectMode.FIT -> ContentScale.Fit
                VideoAspectMode.CROP -> ContentScale.Crop
                VideoAspectMode.STRETCH -> ContentScale.FillBounds
            }
        )

        if (audioOnly) {
            AudioOnlyCover(
                title = video.title,
                onShowVideo = { audioOnly = false; poke() },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (playerState.isBuffering && !audioOnly) {
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

        // 3. Gesture feedback
        val hud = gestureHud
        if (hud != null && !inPip) {
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
                .padding(start = 16.dp, end = 16.dp, bottom = if (controlsVisible) 148.dp else 24.dp)
        ) {
            androidx.compose.material3.Snackbar(
                action = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            playerController.seekTo(0L)
                            showResumedPill = false
                            gestureHud = Symbols.Replay to "From the beginning"
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
            visible = controlsVisible && !inPip,
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
                        speed = playerState.playbackSpeed,
                        sleepLabel = when {
                            sleepEndOfVideo -> "End"
                            sleepDeadline != null -> formatDurationMs(sleepRemainingMs + 999L)
                            else -> null
                        },
                        rotationLocked = rotationOverride != null,
                        onBack = closeAndPersist,
                        onSubtitles = { showSubtitleSheet = true; poke() },
                        onSpeed = { showSpeedSheet = true; poke() },
                        onSleep = { showSleepSheet = true; poke() },
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
                        onMore = { showMoreSheet = true; poke() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    PlayerBottomBar(
                        isPlaying = playerState.isPlaying,
                        positionLabel = formatDurationMs(displayedPositionMs),
                        rightLabel = rightTimeLabel,
                        progress = progressFraction,
                        aspectMode = aspectMode,
                        skipSeconds = playbackSettings.skipDurationSec,
                        onSeekPreview = { seekPreviewFraction = it; poke() },
                        onSeekFinished = { f ->
                            seekPreviewFraction = null
                            playerController.seekTo((f * effectiveDurationMs).toLong())
                            poke()
                        },
                        onPlayPause = { playerController.togglePlayPause(); poke() },
                        onPrevious = onPreviousVideo,
                        onNext = onNextVideo,
                        onLock = { controlsLocked = true; poke() },
                        onAspect = {
                            val next = VideoAspectMode.entries[(aspectMode.ordinal + 1) % VideoAspectMode.entries.size]
                            setAspect(next)
                            poke()
                        },
                        isPlayingForSeekBar = playerState.isPlaying && !playerState.isEnded,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
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
            offOption = null,
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
    if (showMoreSheet) {
        PlayerMoreSheet(
            audioOnly = audioOnly,
            sleepStatus = when {
                sleepEndOfVideo -> "At the end of this video"
                sleepDeadline != null -> "${formatDurationMs(sleepRemainingMs + 999L)} left"
                else -> "Off"
            },
            showPip = playbackSettings.pictureInPicture && activity != null && !audioOnly,
            audioTrackLabel = playerTracks.audioTracks.takeIf { it.size > 1 }
                ?.let { tracks -> tracks.firstOrNull { it.isSelected }?.label ?: "Choose" },
            onAudioOnly = { on -> audioOnly = on; if (on) showMoreSheet = false },
            onSleep = { showMoreSheet = false; showSleepSheet = true },
            onPip = { showMoreSheet = false; enterPip(activity, video.width, video.height, playerState.isPlaying) },
            onAudioTrack = { showMoreSheet = false; showAudioSheet = true },
            onDetails = { showMoreSheet = false; showInfoSheet = true },
            onDismiss = { showMoreSheet = false }
        )
    }
    if (showSleepSheet) {
        SleepTimerSheet(
            active = sleepDeadline != null || sleepEndOfVideo,
            endOfVideo = sleepEndOfVideo,
            remainingMs = sleepRemainingMs,
            onStart = { minutes ->
                sleepEndOfVideo = false
                sleepRemainingMs = minutes * 60_000L
                sleepDeadline = SystemClock.elapsedRealtime() + minutes * 60_000L
                gestureHud = Symbols.Bedtime to "Sleep in $minutes min"
                showSleepSheet = false
            },
            onEndOfVideo = {
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

// ---------------------------------------------------------------------------------------------
// Bars
// ---------------------------------------------------------------------------------------------

/** Translucent tonal colours for the buttons drawn over the video. */
@Composable
private fun overlayButtonColors(active: Boolean = false) = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.16f),
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
    speed: Float,
    sleepLabel: String?,
    rotationLocked: Boolean,
    onBack: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onRotate: () -> Unit,
    onRotateLongPress: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val white = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
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
                        containerColor = Color.White.copy(alpha = 0.16f),
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp).testTag("player_sleep_chip")
                )
            }
        }
        // Order: subtitles, speed, rotate, more.
        PlayerIconButton(onClick = onSubtitles, contentDescription = "Subtitles", active = subtitlesOn, modifier = Modifier.testTag("player_subtitles_btn")) {
            Icon(if (subtitlesOn) Symbols.ClosedCaption else Symbols.ClosedCaptionDisabled, contentDescription = null)
        }
        PlayerIconButton(onClick = onSpeed, contentDescription = "Playback speed", active = speed != 1.0f, modifier = Modifier.testTag("player_speed_btn")) {
            if (speed != 1.0f) {
                Text(speedLabel(speed), style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Symbols.Speed, contentDescription = null)
            }
        }
        PlayerIconButton(
            onClick = onRotate,
            onLongClick = onRotateLongPress,
            contentDescription = "Rotate (hold for auto-rotate)",
            active = rotationLocked,
            modifier = Modifier.testTag("player_rotate_btn")
        ) {
            Icon(Symbols.ScreenRotation, contentDescription = null)
        }
        PlayerIconButton(onClick = onMore, contentDescription = "More", modifier = Modifier.testTag("player_more_btn")) {
            Icon(Symbols.MoreVert, contentDescription = null)
        }
    }
}

@Composable
private fun PlayerBottomBar(
    isPlaying: Boolean,
    positionLabel: String,
    rightLabel: String,
    progress: Float,
    aspectMode: VideoAspectMode,
    skipSeconds: Int,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLock: () -> Unit,
    onAspect: () -> Unit,
    isPlayingForSeekBar: Boolean,
    modifier: Modifier = Modifier
) {
    val tonal = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
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
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).testTag("player_seek_slider")
            )
            Text(rightLabel, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerIconButton(onClick = onLock, contentDescription = "Lock controls") {
                Icon(Symbols.LockOpen, contentDescription = null)
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
                        FilledIconButton(
                            onClick = onPlayPause,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
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
            PlayerIconButton(onClick = onAspect, contentDescription = "Aspect: ${aspectMode.label}", modifier = Modifier.testTag("player_aspect_btn")) {
                Icon(
                    if (aspectMode == VideoAspectMode.FIT) Symbols.AspectRatio else Symbols.ZoomOutMap,
                    contentDescription = null
                )
            }
        }
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
            val count = tracks.size + 1
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
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Symbols.FileOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Load subtitle file")
            }

            Text(
                "Look",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                Icon(Symbols.TextFields, contentDescription = null, tint = scheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Text size", style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                SubtitleSize.entries.forEachIndexed { i, size ->
                    ToggleButton(
                        checked = settings.subtitleSize == size,
                        onCheckedChange = { onUpdateSettings { it.copy(subtitleSize = size) } },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            SubtitleSize.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = scheme.surfaceContainerHighest,
                            contentColor = scheme.onSurface,
                            checkedContainerColor = scheme.primary,
                            checkedContentColor = scheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text(size.label) }
                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedSheet(speed: Float, onSpeed: (Float) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val presets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f)
    var slider by remember(speed) { mutableFloatStateOf(speed) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("player_speed_sheet")
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(speedLabel(slider), style = MaterialTheme.typography.titleLarge, color = scheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p ->
                    FilterChip(
                        selected = abs(speed - p) < 0.01f,
                        onClick = { onSpeed(p); slider = p },
                        label = { Text(if (p == 1f) "Normal" else speedLabel(p)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.primary,
                            selectedLabelColor = scheme.onPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Slider(
                value = slider,
                onValueChange = { slider = (it * 20).toInt() / 20f },
                onValueChangeFinished = { onSpeed(slider) },
                valueRange = 0.25f..3f
            )
        }
    }
}

@Composable
private fun TrackSheet(
    title: String,
    icon: ImageVector,
    tracks: List<PlayerTrackOption>,
    offOption: String?,
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
            if (offOption != null) Text(offOption)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Audio only, More, Sleep timer
// ---------------------------------------------------------------------------------------------

/** Shown instead of the picture while only the sound plays. */
@Composable
private fun AudioOnlyCover(title: String, onShowVideo: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier.size(112.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Symbols.Headphones, contentDescription = null, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text("Playing audio only", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onShowVideo,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.height(ButtonDefaults.MediumContainerHeight).testTag("player_show_video_btn"),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)
            ) {
                Icon(Symbols.Videocam, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
                Text("Show video", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
    }
}

@Composable
private fun PlayerMoreSheet(
    audioOnly: Boolean,
    sleepStatus: String,
    showPip: Boolean,
    audioTrackLabel: String?,
    onAudioOnly: (Boolean) -> Unit,
    onSleep: () -> Unit,
    onPip: () -> Unit,
    onAudioTrack: () -> Unit,
    onDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val colors = ListItemDefaults.colors(
        containerColor = scheme.surfaceContainerHigh,
        selectedContainerColor = scheme.surfaceContainerHigh
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("player_more_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            val count = 3 + (if (showPip) 1 else 0) + (if (audioTrackLabel != null) 1 else 0)
            var i = 0
            SegmentedListItem(
                checked = audioOnly,
                onCheckedChange = onAudioOnly,
                shapes = ListItemDefaults.segmentedShapes(index = i++, count = count),
                leadingContent = { Icon(Symbols.Headphones, contentDescription = null) },
                supportingContent = { Text("Hide the picture; keeps playing when you leave the app") },
                trailingContent = { Switch(checked = audioOnly, onCheckedChange = null) },
                colors = colors,
                modifier = Modifier.testTag("player_audio_only_item")
            ) { Text("Audio only") }
            SegmentedListItem(
                onClick = onSleep,
                shapes = ListItemDefaults.segmentedShapes(index = i++, count = count),
                leadingContent = { Icon(Symbols.Bedtime, contentDescription = null) },
                supportingContent = { Text(sleepStatus) },
                colors = colors
            ) { Text("Sleep timer") }
            if (showPip) {
                SegmentedListItem(
                    onClick = onPip,
                    shapes = ListItemDefaults.segmentedShapes(index = i++, count = count),
                    leadingContent = { Icon(Symbols.PictureInPictureAlt, contentDescription = null) },
                    colors = colors
                ) { Text("Picture-in-picture") }
            }
            if (audioTrackLabel != null) {
                SegmentedListItem(
                    onClick = onAudioTrack,
                    shapes = ListItemDefaults.segmentedShapes(index = i++, count = count),
                    leadingContent = { Icon(Symbols.Audiotrack, contentDescription = null) },
                    supportingContent = { Text(audioTrackLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = colors
                ) { Text("Audio track") }
            }
            SegmentedListItem(
                onClick = onDetails,
                shapes = ListItemDefaults.segmentedShapes(index = i, count = count),
                leadingContent = { Icon(Symbols.Info, contentDescription = null) },
                colors = colors
            ) { Text("Details") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerSheet(
    active: Boolean,
    endOfVideo: Boolean,
    remainingMs: Long,
    onStart: (minutes: Int) -> Unit,
    onEndOfVideo: () -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val presets = listOf(5, 10, 15, 30, 45, 60, 90, 120)
    var minutes by remember { mutableFloatStateOf(30f) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("player_sleep_sheet")
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sleep timer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (active) {
                    Text(
                        if (endOfVideo) "End of video" else "${formatDurationMs(remainingMs + 999L)} left",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { m ->
                    FilterChip(
                        selected = false,
                        onClick = { onStart(m) },
                        label = { Text(if (m < 60) "$m min" else if (m % 60 == 0) "${m / 60} h" else "${m / 60} h ${m % 60} min") }
                    )
                }
                FilterChip(
                    selected = endOfVideo,
                    onClick = onEndOfVideo,
                    label = { Text("End of video") },
                    leadingIcon = { Icon(Symbols.Movie, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = scheme.primary,
                        selectedLabelColor = scheme.onPrimary,
                        selectedLeadingIconColor = scheme.onPrimary
                    )
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Custom", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${minutes.toInt()} min", style = MaterialTheme.typography.titleSmall, color = scheme.primary)
            }
            // 5 to 120 minutes in 5-minute steps.
            Slider(
                value = minutes,
                onValueChange = { minutes = ((it / 5f).toInt() * 5).coerceIn(5, 120).toFloat() },
                valueRange = 5f..120f,
                steps = 22
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active) {
                    OutlinedButton(
                        onClick = onTurnOff,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Turn off") }
                }
                Button(
                    onClick = { onStart(minutes.toInt()) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Start ${minutes.toInt()} min") }
            }
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
