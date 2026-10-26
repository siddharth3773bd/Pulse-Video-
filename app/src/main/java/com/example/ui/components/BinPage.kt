@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MediaBin
import com.example.data.MediaStoreScanner
import com.example.model.formatDurationMs
import com.example.ui.icons.Symbols
import kotlin.math.ceil

/**
 * Me › Bin: videos and songs deleted from the app. They sit in Android's own bin, which deletes
 * them for good after 30 days. Tap items to pick them, then Restore or Delete forever; with
 * nothing picked, the same buttons work on everything. Android asks to confirm each time.
 */
@Composable
internal fun BinPage(onBack: () -> Unit, onLibraryChanged: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<MediaBin.Item>?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(reloadTick) {
        val loaded = MediaBin.load(context)
        items = loaded
        val keys = loaded.mapTo(HashSet()) { it.key }
        picked = picked.filterTo(HashSet()) { it in keys }
    }

    // What the last confirmation was for, so the message after it can say what happened.
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var pendingUris by remember { mutableStateOf(emptyList<Uri>()) }
    val confirmLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingUris.forEach { MediaStoreScanner.evictThumbnail(it.toString()) }
            pendingMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            picked = emptySet()
            onLibraryChanged()
        }
        pendingMessage = null
        pendingUris = emptyList()
        reloadTick++
    }
    fun launch(uris: List<Uri>, restore: Boolean) {
        if (uris.isEmpty()) return
        val count = uris.size
        val what = if (count == 1) "1 item" else "$count items"
        runCatching {
            val request = if (restore) {
                MediaStore.createTrashRequest(context.contentResolver, uris, false)
            } else {
                MediaStore.createDeleteRequest(context.contentResolver, uris)
            }
            pendingMessage = if (restore) "$what restored" else "$what deleted for good"
            pendingUris = if (restore) emptyList() else uris
            confirmLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }.onFailure {
            Toast.makeText(context, "Couldn't open Android's confirmation", Toast.LENGTH_SHORT).show()
        }
    }

    val list = items
    val videos = list.orEmpty().filter { it.isVideo }
    val songs = list.orEmpty().filter { !it.isVideo }
    val chosen = list.orEmpty().filter { it.key in picked }
    val targets = chosen.ifEmpty { list.orEmpty() }

    Box(modifier = Modifier.fillMaxSize().testTag("me_subpage_bin")) {
        SettingsPage(
            title = "Bin",
            subtitle = when {
                picked.isNotEmpty() -> "${picked.size} selected"
                list.isNullOrEmpty() -> "Android deletes items for good after 30 days"
                list.size == 1 -> "1 item · deleted for good after 30 days"
                else -> "${list.size} items · deleted for good after 30 days"
            },
            onBack = onBack,
            contentBottomPadding = 112.dp
        ) {
            when {
                list == null -> item(key = "loading") {
                    Box(Modifier.padding(top = 48.dp).fillParentMaxWidth(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
                list.isEmpty() -> item(key = "empty") {
                    SettingsEmptyState(
                        icon = Symbols.Delete,
                        title = "The bin is empty",
                        body = "Videos and songs you delete wait here for 30 days, so you can bring them back."
                    )
                }
                else -> {
                    fun section(key: String, title: String, part: List<MediaBin.Item>) {
                        if (part.isEmpty()) return
                        item(key = "$key:title") { SettingsSectionTitle(title, Modifier.animateItem()) }
                        itemsIndexed(part, key = { _, entry -> entry.key }) { index, binItem ->
                            BinRow(
                                item = binItem,
                                index = index,
                                count = part.size,
                                checked = binItem.key in picked,
                                onCheckedChange = { on -> picked = if (on) picked + binItem.key else picked - binItem.key },
                                modifier = Modifier
                                    .animateItem()
                                    .padding(bottom = if (index < part.lastIndex) ListItemDefaults.SegmentedGap else 12.dp)
                            )
                        }
                    }
                    section("videos", "Videos", videos)
                    section("songs", "Songs", songs)
                }
            }
        }

        // Official Expressive floating toolbar: Restore and Delete forever.
        AnimatedVisibility(
            visible = !list.isNullOrEmpty(),
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = Modifier.testTag("bin_toolbar")
            ) {
                ToolbarTextButton(
                    label = if (chosen.isEmpty()) "Restore all" else "Restore",
                    icon = Symbols.RestoreFromTrash,
                    onClick = { launch(targets.map { it.uri }, restore = true) },
                    tag = "bin_restore"
                )
                ToolbarTextButton(
                    label = if (chosen.isEmpty()) "Empty bin" else "Delete forever",
                    icon = Symbols.DeleteForever,
                    onClick = { launch(targets.map { it.uri }, restore = false) },
                    tag = "bin_delete_forever"
                )
            }
        }
    }
}

@Composable
private fun ToolbarTextButton(label: String, icon: ImageVector, onClick: () -> Unit, tag: String) {
    TextButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        modifier = Modifier.testTag(tag)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        androidx.compose.foundation.layout.Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label, maxLines = 1)
    }
}

/** One item in the bin: its picture, name, and how long until Android deletes it. */
@Composable
private fun BinRow(
    item: MediaBin.Item,
    index: Int,
    count: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = scheme.surfaceContainer,
            selectedContainerColor = scheme.secondaryContainer,
            supportingContentColor = scheme.onSurfaceVariant
        ),
        leadingContent = { BinThumbnail(item) },
        supportingContent = {
            Text(
                listOfNotNull(
                    item.artist.takeIf { it.isNotBlank() },
                    formatDurationMs(item.durationMs).takeIf { item.durationMs > 0L },
                    daysLeftLabel(item.expiresAtMs)
                ).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
        modifier = modifier.testTag("bin_item_${item.key}")
    ) {
        Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BinThumbnail(item: MediaBin.Item) {
    val scheme = MaterialTheme.colorScheme
    if (item.isVideo) {
        val context = LocalContext.current
        var bitmap by remember(item.key) { mutableStateOf<Bitmap?>(MediaStoreScanner.getCachedThumbnail(item.key)) }
        LaunchedEffect(item.key) {
            if (bitmap == null) bitmap = MediaStoreScanner.loadVideoThumbnail(context, item.uri, 0L)
        }
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(45.dp)
                .clip(shape)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Symbols.Movie, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    } else {
        AudioArt(
            uri = item.uri,
            placeholderBackground = scheme.surfaceContainerHighest,
            placeholderTint = scheme.primary,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            iconSize = 22.dp
        )
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L

private fun daysLeftLabel(expiresAtMs: Long): String? {
    if (expiresAtMs <= 0L) return null
    val days = ceil((expiresAtMs - System.currentTimeMillis()).toDouble() / DAY_MS).toInt()
    return when {
        days <= 0 -> "Deleted soon"
        days == 1 -> "1 day left"
        else -> "$days days left"
    }
}
