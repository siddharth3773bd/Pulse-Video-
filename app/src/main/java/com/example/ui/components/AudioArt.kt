package com.example.ui.components

import com.example.ui.icons.Symbols
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** In-memory cache for album art so scrolling a long song list stays smooth. */
object AudioArtCache {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024L / 10L).toInt().coerceAtLeast(4096)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val missing = HashSet<String>()

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun isMissing(key: String): Boolean = synchronized(missing) { key in missing }

    fun markMissing(key: String) {
        synchronized(missing) { missing.add(key) }
    }
}

/** Loads (or returns the cached) album art for [uri]; null when the file has no art. */
suspend fun loadAudioArtBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    val key = uri.toString()
    AudioArtCache.get(key)?.let { return it }
    if (AudioArtCache.isMissing(key)) return null
    val loaded = withContext(Dispatchers.IO) { readArt(context, uri) }
    if (loaded != null) AudioArtCache.put(key, loaded) else AudioArtCache.markMissing(key)
    return loaded
}

/**
 * Android's ready-made thumbnail for library songs; for a file shared by another app (a file
 * manager), which has no thumbnail, the picture stored inside the file itself.
 */
private fun readArt(context: android.content.Context, uri: Uri): Bitmap? {
    try {
        return context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
    } catch (_: Exception) {
    }
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val bytes = retriever.embeddedPicture ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

/** The album art bitmap for [uri] as Compose state (null while loading or when missing). */
@Composable
fun rememberAudioArtBitmap(uri: Uri?): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf(uri?.let { AudioArtCache.get(it.toString()) }) }
    LaunchedEffect(uri) {
        if (uri != null && bitmap == null) bitmap = loadAudioArtBitmap(context, uri)
    }
    return bitmap
}

/**
 * Album art loaded off the main thread with ContentResolver.loadThumbnail.
 * Shows a music-note placeholder while loading or when the file has no art.
 */
@Composable
fun AudioArt(
    uri: Uri,
    placeholderBackground: Color,
    placeholderTint: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val key = uri.toString()
    var bitmap by remember(key) { mutableStateOf(AudioArtCache.get(key)) }

    LaunchedEffect(key) {
        if (bitmap == null && !AudioArtCache.isMissing(key)) bitmap = loadAudioArtBitmap(context, uri)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(placeholderBackground),
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
            Icon(
                imageVector = Symbols.MusicNote,
                contentDescription = null,
                tint = placeholderTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
