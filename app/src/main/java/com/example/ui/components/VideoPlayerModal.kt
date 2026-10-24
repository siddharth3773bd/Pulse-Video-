package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlaybackSettings
import com.example.model.ScreenOrientationMode
import com.example.model.VideoItem
import com.example.model.formatDurationMs
import com.example.model.resolutionLabel
import com.example.player.PlayerController
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlin.math.abs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoPlayerModal(
    video: VideoItem,
    playerController: PlayerController,
    playbackSettings: PlaybackSettings,
    showResolutionTier: Boolean,
    onSavePosition: (positionMs: Long, durationMs: Long, forcePersist: Boolean) -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onOpenDetails: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val palette = LocalM3Palette.current

    val playerState by playerController.state.collectAsStateWithLifecycle()
    val playerTracks by playerController.tracks.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var gestureHudText by remember { mutableStateOf<String?>(null) }
    var isLongPressing2x by remember { mutableStateOf(false) }
    var speedBeforeLongPress by remember { mutableFloatStateOf(1.0f) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    var surfaceWidthPx by remember { mutableIntStateOf(1) }
    var surfaceHeightPx by remember { mutableIntStateOf(1) }

    // Keep the screen awake while a video is playing; let it time out normally when paused.
    val keepScreenOn = playerState.isPlaying
    DisposableEffect(activity, keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Apply forced screen orientation setting
    DisposableEffect(activity, playbackSettings.forceOrientation) {
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (activity != null) {
            activity.requestedOrientation = when (playbackSettings.forceOrientation) {
                ScreenOrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
        onDispose {
            activity?.requestedOrientation = prevOrientation
        }
    }

    // Periodically save playback position
    LaunchedEffect(video.id, playerState.positionMs, playerState.durationMs) {
        val dur = playerState.durationMs.takeIf { it > 0L } ?: video.durationMs
        if (playerState.positionMs > 0L && dur > 0L) {
            onSavePosition(playerState.positionMs, dur, false)
        }
    }

    // Clear temporary gesture HUD text after a short delay
    LaunchedEffect(gestureHudText) {
        if (gestureHudText != null && !isLongPressing2x) {
            delay(900L)
            gestureHudText = null
        }
    }

    val closeAndPersist = {
        val dur = playerState.durationMs.takeIf { it > 0L } ?: video.durationMs
        onSavePosition(playerState.positionMs, dur, true)
        onClose()
    }

    BackHandler(onBack = closeAndPersist)

    val effectiveDurationMs = playerState.durationMs.takeIf { it > 0L } ?: video.durationMs.coerceAtLeast(1000L)
    val currentPositionMs = playerState.positionMs.coerceIn(0L, effectiveDurationMs)
    val progressFraction = (currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)

    var seekPreviewFraction by remember { mutableStateOf<Float?>(null) }
    val displayedPositionMs = seekPreviewFraction?.let {
        (it.coerceIn(0f, 1f) * effectiveDurationMs).toLong()
    } ?: currentPositionMs

    val currentTimeLabel = formatDurationMs(displayedPositionMs)
    val rightTimeLabel = if (playbackSettings.showRemainingTime) {
        val remainingMs = (effectiveDurationMs - displayedPositionMs).coerceAtLeast(0L)
        "-${formatDurationMs(remainingMs)}"
    } else {
        formatDurationMs(effectiveDurationMs)
    }

    val badge = if (showResolutionTier) resolutionLabel(video.width, video.height) else null
    val skipMs = playbackSettings.skipDurationSec * 1000L

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    var dragMode by remember { mutableStateOf("NONE") } // NONE, SEEK, VOLUME, BRIGHTNESS
    var dragStartSeekMs by remember { mutableLongStateOf(0L) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartVolume by remember { mutableIntStateOf(0) }
    var dragStartBrightness by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                surfaceWidthPx = size.width.coerceAtLeast(1)
                surfaceHeightPx = size.height.coerceAtLeast(1)
            }
            .pointerInput(
                controlsLocked,
                playbackSettings,
                effectiveDurationMs
            ) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        if (controlsLocked) return@detectTapGestures
                        val width = surfaceWidthPx.toFloat()
                        val leftZone = width * 0.35f
                        val rightZone = width * 0.65f
                        when {
                            playbackSettings.doubleTapToSeek && offset.x < leftZone -> {
                                val target = (playerController.state.value.positionMs - skipMs).coerceAtLeast(0L)
                                playerController.seekTo(target)
                                gestureHudText = "-${playbackSettings.skipDurationSec}s"
                            }
                            playbackSettings.doubleTapToSeek && offset.x > rightZone -> {
                                val dur = playerController.state.value.durationMs.takeIf { it > 0L } ?: effectiveDurationMs
                                val target = (playerController.state.value.positionMs + skipMs).coerceAtMost(dur)
                                playerController.seekTo(target)
                                gestureHudText = "+${playbackSettings.skipDurationSec}s"
                            }
                            playbackSettings.doubleTapToPause -> {
                                playerController.togglePlayPause()
                                gestureHudText = if (playerController.state.value.isPlaying) "Play" else "Pause"
                            }
                        }
                    },
                    onLongPress = {
                        if (controlsLocked || !playbackSettings.longPressFor2xSpeed) return@detectTapGestures
                        speedBeforeLongPress = playerController.state.value.playbackSpeed
                        isLongPressing2x = true
                        playerController.setSpeed(2.0f)
                        gestureHudText = "2.0x Speed"
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (isLongPressing2x) {
                            isLongPressing2x = false
                            playerController.setSpeed(speedBeforeLongPress)
                            gestureHudText = null
                        }
                    }
                )
            }
            .pointerInput(
                controlsLocked,
                playbackSettings,
                effectiveDurationMs
            ) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragMode = "NONE"
                        dragAccumulatedX = 0f
                        dragAccumulatedY = 0f
                        dragStartX = startOffset.x
                        dragStartSeekMs = playerController.state.value.positionMs
                        dragStartVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        val winAttr = activity?.window?.attributes
                        val currentBright = winAttr?.screenBrightness ?: -1f
                        dragStartBrightness = if (currentBright in 0.01f..1.0f) currentBright else 0.5f
                    },
                    onDragEnd = {
                        dragMode = "NONE"
                    },
                    onDragCancel = {
                        dragMode = "NONE"
                    },
                    onDrag = { change, dragAmount ->
                        if (controlsLocked) return@detectDragGestures
                        change.consume()
                        dragAccumulatedX += dragAmount.x
                        dragAccumulatedY += dragAmount.y

                        if (dragMode == "NONE") {
                            if (abs(dragAccumulatedX) > 24f && abs(dragAccumulatedX) > abs(dragAccumulatedY)) {
                                if (playbackSettings.horizontalSwipeToSeek) {
                                    dragMode = "SEEK"
                                }
                            } else if (abs(dragAccumulatedY) > 24f && abs(dragAccumulatedY) > abs(dragAccumulatedX)) {
                                val isRightSide = dragStartX >= surfaceWidthPx * 0.5f
                                if (isRightSide && playbackSettings.rightSwipeForVolume) {
                                    dragMode = "VOLUME"
                                } else if (!isRightSide && playbackSettings.leftSwipeForBrightness) {
                                    dragMode = "BRIGHTNESS"
                                }
                            }
                        }

                        when (dragMode) {
                            "SEEK" -> {
                                val dur = playerController.state.value.durationMs.takeIf { it > 0L } ?: effectiveDurationMs
                                val deltaMs = ((dragAccumulatedX / surfaceWidthPx.toFloat()) * 90_000f).toLong()
                                val targetMs = (dragStartSeekMs + deltaMs).coerceIn(0L, dur)
                                playerController.seekTo(targetMs)
                                val sign = if (deltaMs >= 0) "+" else "-"
                                gestureHudText = "${formatDurationMs(targetMs)} ($sign${abs(deltaMs / 1000L)}s)"
                            }

                            "VOLUME" -> {
                                val am = audioManager ?: return@detectDragGestures
                                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val deltaRatio = -dragAccumulatedY / (surfaceHeightPx * 0.65f)
                                val targetVol = (dragStartVolume + (deltaRatio * maxVol).toInt()).coerceIn(0, maxVol)
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                val pct = ((targetVol.toFloat() / maxVol.toFloat()) * 100).toInt()
                                gestureHudText = "Volume $pct%"
                            }

                            "BRIGHTNESS" -> {
                                val win = activity?.window ?: return@detectDragGestures
                                val deltaRatio = -dragAccumulatedY / (surfaceHeightPx * 0.65f)
                                val targetBright = (dragStartBrightness + deltaRatio).coerceIn(0.02f, 1.0f)
                                val params = win.attributes
                                params.screenBrightness = targetBright
                                win.attributes = params
                                val pct = (targetBright * 100).toInt()
                                gestureHudText = "Brightness $pct%"
                            }
                        }
                    }
                )
            }
            .testTag("video_player_modal")
    ) {
        // 1. Video Render Surface via PlayerController interface
        playerController.RenderVideoSurface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        )

        // 1b. Official Material 3 Expressive ContainedLoadingIndicator when buffering
        if (playerState.isBuffering) {
            ContainedLoadingIndicator(
                containerColor = palette.secondaryContainer,
                indicatorColor = palette.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
            )
        }

        // 2. Plain readable subtitle overlay (if active cues are present)
        if (playerTracks.subtitlesEnabled && playerState.activeSubtitleText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = if (controlsVisible && !controlsLocked) 176.dp else 40.dp
                    )
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("player_subtitle_overlay")
            ) {
                Text(
                    text = playerState.activeSubtitleText,
                    color = Color.White,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3. Center Gesture HUD feedback (seek, volume, brightness, 2x speed)
        if (gestureHudText != null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.Black.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("player_gesture_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val hud = gestureHudText.orEmpty()
                    val icon = when {
                        hud.startsWith("Volume") -> Icons.AutoMirrored.Filled.VolumeUp
                        hud.startsWith("Brightness") -> Icons.Default.BrightnessMedium
                        hud.contains("Speed") -> Icons.Default.Speed
                        hud.startsWith("-") -> Icons.Default.FastRewind
                        else -> Icons.Default.FastForward
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = hud,
                        color = Color.White,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Top Bar Overlay with Expressive IconButtons & FilledTonalButton
        if (controlsVisible && !controlsLocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = closeAndPersist,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("player_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to library",
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitleMeta = buildString {
                        append(video.folder)
                        if (badge != null) {
                            append(" · ")
                            append(badge)
                        }
                    }
                    Text(
                        text = subtitleMeta,
                        color = Color.White.copy(alpha = 0.75f),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Audio track selector button
                if (playerTracks.audioTracks.size > 1) {
                    IconButton(
                        onClick = { showAudioTrackDialog = true },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_audio_tracks_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio tracks",
                            tint = Color.White
                        )
                    }
                }

                // Subtitles button
                IconButton(
                    onClick = { showSubtitleDialog = true },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("player_subtitles_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = "Subtitles",
                        tint = if (playerTracks.subtitlesEnabled && playerTracks.subtitleTracks.isNotEmpty()) {
                            palette.primary
                        } else {
                            Color.White
                        }
                    )
                }

                // Playback speed button
                IconButton(
                    onClick = { showSpeedDialog = true },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("player_speed_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback speed",
                        tint = if (playerState.playbackSpeed != 1.0f) palette.primary else Color.White
                    )
                }

                FilledTonalButton(
                    onClick = onOpenDetails,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.surfaceContainerHigh,
                        contentColor = palette.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("player_info_btn")
                ) {
                    Text(
                        text = "Info",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Bottom Controls Overlay
        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (controlsLocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilledTonalButton(
                            onClick = { controlsLocked = false },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = palette.surfaceContainerHigh,
                                contentColor = palette.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock controls",
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tap to unlock controls",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentTimeLabel,
                            color = Color.White,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        WavySeekBar(
                            progress = progressFraction,
                            onSeekPreview = { previewFraction ->
                                seekPreviewFraction = previewFraction
                            },
                            onSeekFinished = { finalFraction ->
                                seekPreviewFraction = null
                                val targetMs = (finalFraction * effectiveDurationMs).toLong()
                                playerController.seekTo(targetMs)
                            },
                            isPlaying = playerState.isPlaying && !playerState.isEnded,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .testTag("player_seek_slider")
                        )
                        Text(
                            text = rightTimeLabel,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = onPreviousVideo,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous video",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val target = (currentPositionMs - skipMs).coerceAtLeast(0L)
                                playerController.seekTo(target)
                            },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind ${playbackSettings.skipDurationSec} seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        FilledIconToggleButton(
                            checked = playerState.isPlaying,
                            onCheckedChange = { playerController.togglePlayPause() },
                            shapes = IconButtonDefaults.toggleableShapes(),
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = palette.secondaryContainer,
                                contentColor = palette.onSecondaryContainer,
                                checkedContainerColor = palette.primary,
                                checkedContentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("player_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val target = (currentPositionMs + skipMs).coerceAtMost(effectiveDurationMs)
                                playerController.seekTo(target)
                            },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward ${playbackSettings.skipDurationSec} seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = onNextVideo,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next video",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { controlsLocked = true },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = palette.surfaceContainerHigh.copy(alpha = 0.85f),
                                contentColor = palette.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock controls",
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lock",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Audio Track Selection Dialog
    if (showAudioTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAudioTrackDialog = false },
            title = { Text("Audio track", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    playerTracks.audioTracks.forEachIndexed { idx, option ->
                        SegmentedListItem(
                            selected = option.isSelected,
                            onClick = {
                                playerController.selectAudioTrack(option)
                                showAudioTrackDialog = false
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = idx,
                                count = playerTracks.audioTracks.size.coerceAtLeast(1)
                            ),
                            leadingContent = {
                                RadioButton(
                                    selected = option.isSelected,
                                    onClick = null
                                )
                            },
                            content = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAudioTrackDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Subtitle Track Selection Dialog
    if (showSubtitleDialog) {
        val totalCount = 1 + playerTracks.subtitleTracks.size
        val isOffSelected = !playerTracks.subtitlesEnabled || playerTracks.subtitleTracks.none { it.isSelected }
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Subtitles", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SegmentedListItem(
                        selected = isOffSelected,
                        onClick = {
                            playerController.setSubtitlesEnabled(false)
                            showSubtitleDialog = false
                        },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = 0,
                            count = totalCount
                        ),
                        leadingContent = {
                            RadioButton(
                                selected = isOffSelected,
                                onClick = null
                            )
                        },
                        content = {
                            Text(
                                text = "Off",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (playerTracks.subtitleTracks.isEmpty()) {
                        Text(
                            text = "No embedded or external (.srt/.vtt) subtitles found for this video.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        playerTracks.subtitleTracks.forEachIndexed { idx, option ->
                            val isSelected = playerTracks.subtitlesEnabled && option.isSelected
                            SegmentedListItem(
                                selected = isSelected,
                                onClick = {
                                    playerController.selectSubtitleTrack(option)
                                    showSubtitleDialog = false
                                },
                                shapes = ListItemDefaults.segmentedShapes(
                                    index = idx + 1,
                                    count = totalCount
                                ),
                                leadingContent = {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                },
                                content = {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSubtitleDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback speed", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    speeds.forEachIndexed { idx, spd ->
                        val isSelected = abs(playerState.playbackSpeed - spd) < 0.05f
                        SegmentedListItem(
                            selected = isSelected,
                            onClick = {
                                playerController.setSpeed(spd)
                                showSpeedDialog = false
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = idx,
                                count = speeds.size
                            ),
                            leadingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                            },
                            content = {
                                Text(
                                    text = "${spd}x",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSpeedDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Close")
                }
            }
        )
    }
}
