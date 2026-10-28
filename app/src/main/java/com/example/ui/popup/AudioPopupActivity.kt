@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.popup

import com.example.ui.icons.Symbols
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import com.example.ui.components.AudioArtCache
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.PlaybackErrorDialog
import com.example.ui.components.PlaybackSeekBar
import com.example.ui.components.SongInfoSheet
import com.example.ui.theme.DopplerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext

/**
 * Pop-up player shown when an audio file is opened from a file manager: an official bottom sheet
 * with the cover, the wavy seek bar and one big Play/Pause. Playback goes through the shared
 * AudioPlaybackService. Closing the sheet (swipe down, tap outside, Back) stops the song;
 * "Open in Doppler" hands it over to the app's Now Playing screen instead.
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
            DopplerTheme(
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
                                Intent(this@AudioPopupActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    .putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
                            )
                        } catch (_: Throwable) {
                        }
                        finish()
                    },
                    onLeaveBriefly = { suppressStopOnLeave = true }
                )
            }
        }
    }

    /** Back from the share sheet or the permission dialog: leaving counts as closing again. */
    override fun onResume() {
        super.onResume()
        suppressStopOnLeave = false
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
    onOpenApp: () -> Unit,
    onLeaveBriefly: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioPlaybackManager.getInstance(context) }
    var info by remember { mutableStateOf<ExternalAudioInfo?>(null) }
    var failed by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var showInfo by remember { mutableStateOf(false) }

    // Ask for notification permission once (playback works even when denied).
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val sp = context.getSharedPreferences("popup_prefs", android.content.Context.MODE_PRIVATE)
            if (!sp.getBoolean("asked_notifications", false)) {
                sp.edit().putBoolean("asked_notifications", true).apply()
                onLeaveBriefly()
                try {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Throwable) {
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
            // Play the library's own copy when the song is in it: Doppler can always read that,
            // while the file manager's link stops working once this pop-up closes.
            val library = ExternalMedia.findInLibrary(context, loaded)
            val playable = if (library != null) {
                ExternalAudioInfo(
                    track = library,
                    cover = loaded.cover,
                    sampleRateHz = loaded.sampleRateHz,
                    bitrateKbps = loaded.bitrateKbps,
                    format = loaded.format,
                    path = library.path,
                    fileName = loaded.fileName
                )
            } else loaded
            // The app's own screens (Now Playing after "Open in Doppler") reuse this cover.
            loaded.cover?.let { AudioArtCache.put(playable.track.contentUri.toString(), it) }
            info = playable
            dragFraction = null
            // A quick listen from a file manager doesn't replace the queue the app remembers.
            manager.playQueue(listOf(playable.track), 0, remember = false)
        }
    }
    LaunchedEffect(failed) {
        if (failed) {
            Toast.makeText(context, "Can't open this audio file", Toast.LENGTH_SHORT).show()
            onFailed()
        }
    }

    val scope = rememberCoroutineScope()
    // "Open in Doppler": a song that isn't in the library is copied into the app first (while
    // the file manager still allows reading it), so it keeps playing and seeking in the app.
    val openInApp: () -> Unit = {
        val current = info
        scope.launch {
            if (current != null && current.track.mediaId == 0L) {
                val copy = withContext(Dispatchers.IO) { ExternalMedia.copyToAppStorage(context, current) }
                if (copy != null) {
                    current.cover?.let { AudioArtCache.put(copy.contentUri.toString(), it) }
                    manager.continueWith(copy)
                }
            } else if (current != null) {
                // A library song: from now on it's the app's queue, remembered as usual.
                manager.rememberCurrentQueue()
            }
            onOpenApp()
        }
    }
    val playback by manager.state.collectAsStateWithLifecycle()
    val error by manager.lastError.collectAsStateWithLifecycle()
    val loadedInfo = info ?: return
    val track = loadedInfo.track
    val scheme = MaterialTheme.colorScheme

    val isCurrent = playback.currentTrack?.id == track.id
    val isPlaying = isCurrent && playback.isPlaying && !playback.isEnded
    val durationMs = if (isCurrent && playback.durationMs > 0L) playback.durationMs else track.durationMs
    val positionMs = if (isCurrent) playback.positionMs else 0L
    val liveFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val fraction = dragFraction ?: liveFraction
    val shownPositionMs = (fraction * durationMs).toLong()

    ModalBottomSheet(
        onDismissRequest = onStopAndClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("audio_popup_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Cover, title and artist, and ⋮ with the rest.
            Row(verticalAlignment = Alignment.CenterVertically) {
                val coverBitmap = remember(loadedInfo) { loadedInfo.cover?.asImageBitmap() }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.secondaryContainer),
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
                            imageVector = Symbols.MusicNote,
                            contentDescription = null,
                            tint = scheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("audio_popup_title")
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Wavy seek bar with the times under it.
            Spacer(Modifier.height(16.dp))
            PlaybackSeekBar(
                progress = fraction,
                onSeekPreview = { dragFraction = it },
                onSeekFinished = { target ->
                    dragFraction = null
                    if (durationMs > 0L) manager.seekTo((target * durationMs).toLong())
                },
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_popup_seek")
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDurationMs(shownPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDurationMs(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            // Song info · Play/Pause · Open in Doppler: a big round play button in the middle and
            // two small tonal buttons, in an Expressive button group like the video player's: the
            // pressed button springs wider and squeezes its neighbours.
            Spacer(Modifier.height(12.dp))
            val infoInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val openInteraction = remember { MutableInteractionSource() }
            val togglePlay = {
                if (isCurrent) manager.togglePlayPause() else manager.playQueue(listOf(track), 0)
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ButtonGroup(
                    overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    customItem(
                        buttonGroupContent = {
                            // The width effect goes on the tooltip box (the group's direct child);
                            // the button inside simply fills it.
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text("Song info") } },
                                state = rememberTooltipState(),
                                modifier = Modifier.animateWidth(infoInteraction)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { showInfo = true },
                                    shapes = IconButtonDefaults.shapes(),
                                    interactionSource = infoInteraction,
                                    modifier = Modifier
                                        .widthIn(min = 56.dp)
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("audio_popup_info")
                                ) { Icon(Symbols.Info, contentDescription = "Song info") }
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(text = { Text("Song info") }, onClick = { showInfo = true; menuState.dismiss() })
                        }
                    )
                    customItem(
                        buttonGroupContent = {
                            FilledIconToggleButton(
                                checked = !isPlaying,
                                onCheckedChange = { togglePlay() },
                                // Stays round for both play and pause; squishes to the pressed shape on touch.
                                shapes = IconButtonDefaults.toggleableShapes(
                                    shape = IconButtonDefaults.largeRoundShape,
                                    pressedShape = IconButtonDefaults.largePressedShape,
                                    checkedShape = IconButtonDefaults.largeRoundShape
                                ),
                                colors = IconButtonDefaults.filledIconToggleButtonColors(
                                    containerColor = scheme.primary,
                                    contentColor = scheme.onPrimary,
                                    checkedContainerColor = scheme.primary,
                                    checkedContentColor = scheme.onPrimary
                                ),
                                interactionSource = playInteraction,
                                modifier = Modifier
                                    .animateWidth(playInteraction)
                                    .size(80.dp)
                                    .testTag("audio_popup_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Symbols.Pause else Symbols.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(text = { Text(if (isPlaying) "Pause" else "Play") }, onClick = { togglePlay(); menuState.dismiss() })
                        }
                    )
                    customItem(
                        buttonGroupContent = {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text("Open in Doppler") } },
                                state = rememberTooltipState(),
                                modifier = Modifier.animateWidth(openInteraction)
                            ) {
                                FilledTonalIconButton(
                                    onClick = openInApp,
                                    shapes = IconButtonDefaults.shapes(),
                                    interactionSource = openInteraction,
                                    modifier = Modifier
                                        .widthIn(min = 56.dp)
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("audio_popup_open_app")
                                ) { Icon(Symbols.OpenInNew, contentDescription = "Open in Doppler") }
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(text = { Text("Open in Doppler") }, onClick = { openInApp(); menuState.dismiss() })
                        }
                    )
                }
            }
        }
    }

    if (showInfo) {
        SongInfoSheet(track = track, onDismiss = { showInfo = false })
    }
    // The file couldn't play: show the full reason (with Copy), then close.
    error?.let { report ->
        PlaybackErrorDialog(
            error = report,
            onDismiss = {
                manager.clearError()
                onStopAndClose()
            }
        )
    }
}
