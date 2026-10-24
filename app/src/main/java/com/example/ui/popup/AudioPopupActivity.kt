package com.example.ui.popup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.data.AppPreferencesRepository
import com.example.data.StoredAppPreferences
import com.example.model.formatDurationMs
import com.example.player.AudioPlaybackManager
import com.example.player.ExternalAudioInfo
import com.example.player.ExternalMedia
import com.example.ui.components.PlaybackSeekBar
import com.example.ui.theme.PulseVideoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small bottom pop-up player shown when an audio file is opened from a file manager.
 * Playback goes through the shared AudioPlaybackService, so closing this pop-up never stops the music.
 */
class AudioPopupActivity : ComponentActivity() {

    private var currentUri by mutableStateOf<Uri?>(null)

    /** True while this pop-up itself launches something (Open app, permission dialog) so leaving is not a "close". */
    internal var suppressStopOnLeave = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentUri = extractUri(intent)
        setContent {
            val context = LocalContext.current
            val prefsFlow = remember { AppPreferencesRepository.observePreferences(context.applicationContext) }
            val prefs by prefsFlow.collectAsStateWithLifecycle(initialValue = StoredAppPreferences())
            PulseVideoTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
                paletteId = prefs.colorTheme,
                customColorArgb = prefs.customColorArgb,
                amoledBlack = prefs.amoledBlack
            ) {
                AudioPopupContent(
                    uri = currentUri,
                    onStopAndClose = {
                        // Closing the pop-up in any way (X, outside tap, back) stops the music.
                        AudioPlaybackManager.getInstance(applicationContext).stopAndClear()
                        finish()
                    },
                    onFailed = { finish() },
                    onOpenApp = {
                        suppressStopOnLeave = true
                        try {
                            startActivity(
                                Intent(this@AudioPopupActivity, MainActivity::class.java).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                )
                            )
                        } catch (_: Throwable) {
                        }
                        finish()
                    }
                )
            }
        }
    }

    /** Pressing Home or opening Recents counts as closing the pop-up: stop the music. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        stopAndCloseBecauseUserLeft()
    }

    /**
     * Backup for launchers/gestures where onUserLeaveHint is not delivered: once the pop-up is no longer
     * visible while the screen is still on, treat it as closed. A screen timeout (screen off) is ignored.
     */
    override fun onStop() {
        super.onStop()
        val pm = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        if (pm?.isInteractive == false) return
        stopAndCloseBecauseUserLeft()
    }

    private fun stopAndCloseBecauseUserLeft() {
        if (suppressStopOnLeave || isFinishing || isChangingConfigurations) return
        AudioPlaybackManager.getInstance(applicationContext).stopAndClear()
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentUri = extractUri(intent)
    }

    private fun extractUri(intent: Intent?): Uri? {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return null
        return intent.data
    }
}

@Composable
private fun AudioPopupContent(
    uri: Uri?,
    onStopAndClose: () -> Unit,
    onFailed: () -> Unit,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioPlaybackManager.getInstance(context) }
    var info by remember { mutableStateOf<ExternalAudioInfo?>(null) }
    var failed by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    // Ask for notification permission once (playback works even when denied).
    val popupActivity = context as? AudioPopupActivity
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { popupActivity?.suppressStopOnLeave = false }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val sp = context.getSharedPreferences("popup_prefs", android.content.Context.MODE_PRIVATE)
            if (!sp.getBoolean("asked_notifications", false)) {
                sp.edit().putBoolean("asked_notifications", true).apply()
                popupActivity?.suppressStopOnLeave = true
                try {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Throwable) {
                    popupActivity?.suppressStopOnLeave = false
                }
            }
        }
    }

    LaunchedEffect(uri) {
        if (uri == null) {
            failed = true
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                ExternalMedia.readAudioInfo(context, uri)
            } catch (_: Throwable) {
                null
            }
        }
        if (loaded == null) {
            failed = true
        } else {
            info = loaded
            dragFraction = null
            manager.playQueue(listOf(loaded.track), 0)
        }
    }

    LaunchedEffect(failed) {
        if (failed) {
            Toast.makeText(context, "Can't open this audio file", Toast.LENGTH_SHORT).show()
            onFailed()
        }
    }

    LaunchedEffect(Unit) { shown = true }

    BackHandler(onBack = onStopAndClose)

    val playback by manager.state.collectAsStateWithLifecycle()
    val loadedInfo = info

    val noRipple = remember { MutableInteractionSource() }
    val consumeClicks = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = noRipple, indication = null, onClick = onStopAndClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (loadedInfo != null) {
            val track = loadedInfo.track
            val isCurrent = playback.currentTrack?.id == track.id
            val isPlaying = isCurrent && playback.isPlaying && !playback.isEnded
            val durationMs = if (isCurrent && playback.durationMs > 0L) {
                playback.durationMs
            } else {
                track.durationMs
            }
            val positionMs = if (isCurrent) playback.positionMs else 0L
            val liveFraction = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val fraction = dragFraction ?: liveFraction
            val shownPositionMs = (fraction * durationMs).toLong()

            AnimatedVisibility(
                visible = shown,
                enter = slideInVertically(animationSpec = tween(260), initialOffsetY = { it }) +
                    fadeIn(animationSpec = tween(200))
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = consumeClicks, indication = null) { }
                ) {
                    Column(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val coverBitmap = remember(loadedInfo) { loadedInfo.cover?.asImageBitmap() }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (coverBitmap != null) {
                                    Image(
                                        bitmap = coverBitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalIconButton(onClick = onStopAndClose) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDurationMs(shownPositionMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PlaybackSeekBar(
                                progress = fraction,
                                onSeekPreview = { dragFraction = it },
                                onSeekFinished = { target ->
                                    dragFraction = null
                                    if (durationMs > 0L) {
                                        manager.seekTo((target * durationMs).toLong())
                                    }
                                },
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatDurationMs(durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalButton(onClick = { infoExpanded = !infoExpanded }) {
                                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Info")
                                Icon(
                                    imageVector = if (infoExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            FilledIconButton(
                                onClick = {
                                    if (isCurrent) {
                                        manager.togglePlayPause()
                                    } else {
                                        manager.playQueue(listOf(track), 0)
                                    }
                                },
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            FilledTonalButton(onClick = onOpenApp) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Open app")
                            }
                        }

                        AnimatedVisibility(visible = infoExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                InfoLine("Duration", formatDurationMs(durationMs))
                                InfoLine("Album", track.album)
                                InfoLine("Artist", track.artist)
                                val technical = buildList {
                                    if (loadedInfo.sampleRateHz > 0) {
                                        add(String.format(java.util.Locale.US, "%.1f kHz", loadedInfo.sampleRateHz / 1000f))
                                    }
                                    if (loadedInfo.bitrateKbps > 0) add("${loadedInfo.bitrateKbps} kbps")
                                    if (loadedInfo.format.isNotBlank()) add(loadedInfo.format)
                                }.joinToString(" · ").ifBlank { "-" }
                                InfoLine("Song info", technical)
                                InfoLine("Path", loadedInfo.path)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
