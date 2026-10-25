package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.core.content.ContextCompat
import com.example.model.AudioTrackItem
import com.example.model.VideoItem
import com.example.model.formatDurationMs
import com.example.model.parseSeriesAndEpisode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object MediaStoreScanner {

    // Sized by memory (about 1/6 of the app's heap) instead of a fixed 60 items, so scrolling back
    // doesn't throw thumbnails away and decode them again.
    private val thumbnailCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024L / 6L).toInt().coerceAtLeast(16 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    /** At most 4 thumbnails decode at once, so a fast scroll doesn't flood the disk and CPU. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(4)

    fun getVideoPermissionName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getAudioPermissionName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasVideoPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            getVideoPermissionName()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            getAudioPermissionName()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLegacyWritePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanVideos(
        context: Context,
        progressHistory: Map<String, Float>
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        if (!hasVideoPermission(context)) return@withContext emptyList()

        val result = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            @Suppress("DEPRECATION") MediaStore.Video.Media.DATA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val displayName = cursor.getString(displayNameCol) ?: cursor.getString(titleCol) ?: "Video_$mediaId.mp4"
                    val dotIndex = displayName.lastIndexOf('.')
                    val cleanTitle = if (dotIndex > 0) displayName.substring(0, dotIndex) else displayName
                    val ext = if (dotIndex > 0 && dotIndex < displayName.length - 1) {
                        displayName.substring(dotIndex + 1).lowercase(Locale.US)
                    } else {
                        "mp4"
                    }

                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                    val bucketName = cursor.getString(bucketCol)
                        ?: rawPath?.let { File(it).parentFile?.name }
                        ?: relPath?.trimEnd('/')?.substringAfterLast('/')
                        ?: "Videos"

                    val fullPath = when {
                        !rawPath.isNullOrBlank() -> rawPath
                        !relPath.isNullOrBlank() -> "${relPath.trimEnd('/')}/$displayName"
                        else -> "$bucketName/$displayName"
                    }

                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val sizeMB = sizeBytes.toDouble() / (1024.0 * 1024.0)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val durationSec = (durationMs / 1000L).toInt()
                    val width = if (!cursor.isNull(widthCol)) cursor.getInt(widthCol) else 0
                    val height = if (!cursor.isNull(heightCol)) cursor.getInt(heightCol) else 0
                    val dateAddedSec = cursor.getLong(dateAddedCol)
                    val timestampMs = if (dateAddedSec > 0L) dateAddedSec * 1000L else System.currentTimeMillis()
                    val dateStr = dateFormat.format(Date(timestampMs))

                    val seriesAndEp = parseSeriesAndEpisode(cleanTitle)
                    val uriStr = contentUri.toString()
                    val savedProgress = progressHistory[uriStr] ?: 0f

                    result.add(
                        VideoItem(
                            mediaId = mediaId,
                            id = uriStr,
                            contentUri = contentUri,
                            title = cleanTitle,
                            displayName = displayName,
                            ext = ext,
                            series = seriesAndEp?.first,
                            episode = seriesAndEp?.second,
                            width = width,
                            height = height,
                            sizeBytes = sizeBytes,
                            sizeMB = sizeMB,
                            dateStr = dateStr,
                            timestamp = timestampMs,
                            durationMs = durationMs,
                            durationSec = durationSec,
                            durationLabel = formatDurationMs(durationMs),
                            progress = savedProgress,
                            folder = bucketName.ifBlank { "Videos" },
                            path = fullPath
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        result
    }

    suspend fun scanAudio(context: Context): List<AudioTrackItem> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission(context)) return@withContext emptyList()

        val result = mutableListOf<AudioTrackItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.IS_RECORDING
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} == 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} == 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} == 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val bucketCol = cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val relPathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                val isRecordingCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RECORDING)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val displayName = cursor.getString(displayNameCol) ?: ""
                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.').ifBlank { "Audio_$mediaId" }
                    val ext = displayName.substringAfterLast('.', "mp3").lowercase(Locale.US)
                    val artist = cursor.getString(artistCol)?.takeIf {
                        it.isNotBlank() && it != "<unknown>"
                    } ?: "Unknown artist"
                    val album = cursor.getString(albumCol)?.takeIf {
                        it.isNotBlank() && it != "<unknown>"
                    } ?: "Unknown album"
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val durationSec = (durationMs / 1000L).toInt()
                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val sizeMB = sizeBytes.toDouble() / (1024.0 * 1024.0)
                    val folder = if (bucketCol >= 0) {
                        cursor.getString(bucketCol) ?: "Music"
                    } else {
                        "Music"
                    }

                    val absPath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
                    val fullPath = absPath.ifBlank { relPath + displayName }

                    result.add(
                        AudioTrackItem(
                            mediaId = mediaId,
                            id = contentUri.toString(),
                            contentUri = contentUri,
                            title = rawTitle,
                            artist = artist,
                            album = album,
                            ext = ext,
                            durationMs = durationMs,
                            durationSec = durationSec,
                            durationLabel = formatDurationMs(durationMs),
                            sizeBytes = sizeBytes,
                            sizeMB = sizeMB,
                            folder = folder,
                            albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L,
                            artistId = if (artistIdCol >= 0) cursor.getLong(artistIdCol) else 0L,
                            dateAddedSec = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L,
                            path = fullPath,
                            isRecording = isRecordingCol >= 0 && cursor.getInt(isRecordingCol) == 1
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        result
    }

    fun getCachedThumbnail(uriKey: String): Bitmap? = thumbnailCache.get(uriKey)

    // Thumbnails are also kept on disk (small JPEGs in the app's cache folder), so they don't have
    // to be made again every time the app opens.
    private const val THUMB_W = 512
    private const val THUMB_H = 288
    @Volatile private var thumbDir: File? = null

    private fun thumbFile(context: Context, key: String): File {
        val dir = thumbDir ?: File(context.cacheDir, "thumbs").also {
            it.mkdirs()
            thumbDir = it
        }
        val digest = java.security.MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        return File(dir, "$name.jpg")
    }

    /**
     * A sharp thumbnail for [videoUri]: from memory, else from the disk cache, else MediaStore's
     * own thumbnail (falling back to a frame from the video), which is then saved to disk.
     */
    suspend fun loadVideoThumbnail(
        context: Context,
        videoUri: Uri,
        @Suppress("UNUSED_PARAMETER") videoId: Long
    ): Bitmap? = withContext(thumbnailDispatcher) {
        val cacheKey = videoUri.toString()
        thumbnailCache.get(cacheKey)?.let { return@withContext it }
        // Scrolled past already? Don't spend time on it.
        ensureActive()

        val file = thumbFile(context, cacheKey)
        var bmp: Bitmap? = if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.path)
            } catch (_: Throwable) {
                null
            }
        } else null
        val fromDisk = bmp != null

        if (bmp == null) {
            bmp = try {
                context.contentResolver.loadThumbnail(videoUri, Size(THUMB_W, THUMB_H), null)
            } catch (_: Exception) {
                null
            }
        }
        if (bmp == null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                bmp = retriever.getScaledFrameAtTime(
                    1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, THUMB_W, THUMB_H
                )
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
        val result = bmp
        if (result != null) {
            if (!fromDisk) {
                try {
                    val tmp = File(file.path + ".tmp")
                    tmp.outputStream().use { out -> result.compress(Bitmap.CompressFormat.JPEG, 88, out) }
                    tmp.renameTo(file)
                } catch (_: Throwable) {
                }
            }
            // Upload to the GPU now, off the main thread, so it doesn't stutter the scroll later.
            result.prepareToDraw()
            thumbnailCache.put(cacheKey, result)
        }
        result
    }

    fun evictThumbnail(uriKey: String) {
        thumbnailCache.remove(uriKey)
        thumbDir?.let { dir ->
            runCatching {
                val digest = java.security.MessageDigest.getInstance("SHA-1").digest(uriKey.toByteArray())
                File(dir, digest.joinToString("") { "%02x".format(it) } + ".jpg").delete()
            }
        }
    }
}
