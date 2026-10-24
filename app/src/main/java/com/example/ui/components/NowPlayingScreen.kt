package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.model.AudioTrackItem
import com.example.model.NowPlayingAction
import com.example.model.formatDurationMs
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily

/** Simple "lines of text" icon for the Lyrics button (no extra icon dependency needed). */
val LyricsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Lyrics",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 7f); lineTo(4f, 7f); close()
            moveTo(4f, 9f); lineTo(20f, 9f); lineTo(20f, 11f); lineTo(4f, 11f); close()
            moveTo(4f, 13f); lineTo(20f, 13f); lineTo(20f, 15f); lineTo(4f, 15f); close()
            moveTo(4f, 17f); lineTo(14f, 17f); lineTo(14f, 19f); lineTo(4f, 19f); close()
        }
    }.build()
}

fun nowPlayingActionIcon(action: NowPlayingAction): ImageVector = when (action) {
    NowPlayingAction.SHUFFLE -> Icons.Default.Shuffle
    NowPlayingAction.REPEAT -> Icons.Default.Repeat
    NowPlayingAction.LYRICS -> LyricsIcon
    NowPlayingAction.INFO -> Icons.Default.Info
}

/**
 * Full-screen Now Playing page. Layout is inspired by Echo Music (centered "Now Playing" header,
 * big cover) with Pixel Player style controls and a customizable bottom button tray.
 */
@Composable
fun NowPlayingScreen(
    track: AudioTrackItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    liked: Boolean,
    trayActions: List<NowPlayingAction>,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenInfo: () -> Unit,
    onLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val context = LocalContext.current
    val closeLatest by rememberUpdatedState(onClose)
    val closeThresholdPx = with(LocalDensity.current) { 90.dp.toPx() }

    BackHandler(onBack = onClose)

    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var techLabel by remember(track.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(track.id) {
        techLabel = try {
            audioTechLabel(context, track)
        } catch (_: Throwable) {
            null
        }
    }

    val safeDuration = if (durationMs > 0L) durationMs else track.durationMs
    val liveFraction = if (safeDuration > 0L) {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = dragFraction ?: liveFraction
    val shownPositionMs = (fraction * safeDuration).toLong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.primaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .pointerInput(closeThresholdPx) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > closeThresholdPx) closeLatest()
                        total = 0f
                    },
                    onDragCancel = { total = 0f }
                ) { _, dragAmount ->
                    total += dragAmount
                }
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp)
            .testTag("audio_now_playing")
    ) {
        // ---- Header: collapse button, centered "Now Playing" + album, empty right side ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = palette.onPrimaryContainer.copy(alpha = 0.12f),
                    contentColor = palette.onPrimaryContainer
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close Now Playing")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Now Playing",
                    color = palette.onPrimaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = track.album.uppercase(),
                    color = palette.onPrimaryContainer.copy(alpha = 0.7f),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        // ---- Cover (takes whatever vertical room is left, always square) ----
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHighest,
                placeholderTint = palette.primary,
                modifier = Modifier.size(side),
                shape = RoundedCornerShape(28.dp),
                iconSize = 72.dp
            )
        }

        // ---- Title / artist + Like button ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = palette.onPrimaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = palette.onPrimaryContainer.copy(alpha = 0.75f),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FilledIconToggleButton(
                checked = liked,
                onCheckedChange = { onToggleLike() },
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = palette.onPrimaryContainer.copy(alpha = 0.12f),
                    contentColor = palette.onPrimaryContainer,
                    checkedContainerColor = palette.primary,
                    checkedContentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("now_playing_like")
            ) {
                Icon(
                    imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (liked) "Remove from liked songs" else "Add to liked songs"
                )
            }
        }

        // ---- Seek bar + times + technical chip ----
        Spacer(modifier = Modifier.height(12.dp))
        WavySeekBar(
            progress = fraction,
            onSeekPreview = { dragFraction = it },
            onSeekFinished = { target ->
                dragFraction = null
                onSeekTo(target)
            },
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_seek_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDurationMs(shownPositionMs),
                color = palette.onPrimaryContainer.copy(alpha = 0.85f),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val chip = techLabel
                if (!chip.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.onPrimaryContainer.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = chip,
                            color = palette.onPrimaryContainer,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = formatDurationMs(safeDuration),
                color = palette.onPrimaryContainer.copy(alpha = 0.85f),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ---- Main controls ----
        Spacer(modifier = Modifier.height(16.dp))
        val playCorner by animateDpAsState(
            targetValue = if (isPlaying) 26.dp else 40.dp,
            label = "nowPlayingPlayCorner"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onSkipPrevious,
                shape = RoundedCornerShape(40.dp),
                color = palette.secondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = palette.onSecondaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Surface(
                onClick = onTogglePlayPause,
                shape = RoundedCornerShape(playCorner),
                color = palette.primary,
                modifier = Modifier
                    .weight(1.25f)
                    .height(80.dp)
                    .testTag("now_playing_play_pause")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = palette.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Surface(
                onClick = onSkipNext,
                shape = RoundedCornerShape(40.dp),
                color = palette.secondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = palette.onSecondaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // ---- Bottom tray (customizable) ----
        if (trayActions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(palette.onPrimaryContainer.copy(alpha = 0.10f))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                trayActions.forEach { action ->
                    val active = when (action) {
                        NowPlayingAction.SHUFFLE -> shuffleEnabled
                        NowPlayingAction.REPEAT -> repeatMode != Player.REPEAT_MODE_OFF
                        else -> false
                    }
                    val icon = when {
                        action == NowPlayingAction.REPEAT && repeatMode == Player.REPEAT_MODE_ONE ->
                            Icons.Default.RepeatOne
                        else -> nowPlayingActionIcon(action)
                    }
                    val onClick = when (action) {
                        NowPlayingAction.SHUFFLE -> onToggleShuffle
                        NowPlayingAction.REPEAT -> onCycleRepeat
                        NowPlayingAction.LYRICS -> onLyrics
                        NowPlayingAction.INFO -> onOpenInfo
                    }
                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(24.dp),
                        color = if (active) palette.primary else palette.onPrimaryContainer.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("now_playing_tray_${action.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = icon,
                                contentDescription = action.label,
                                tint = if (active) palette.onPrimary else palette.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
