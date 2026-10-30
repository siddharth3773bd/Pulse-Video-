package com.example.player

import com.example.model.cleanArtistName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.MediaStoreScanner
import com.example.model.AudioTrackItem
import com.example.model.VideoItem
import com.example.model.formatDurationMs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Everything the audio pop-up needs to know about a file opened from another app. */
class ExternalAudioInfo(
    val track: AudioTrackItem,
    val cover: Bitmap?,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val format: String,
    val path: String,
    /** The file's name as the other app shows it, e.g. "Sahiba.mp3". */
    val fileName: String = ""
)

/** Helpers to turn a file opened from a file manager (content:// or file://) into app models. */
object ExternalMedia {

    private fun queryNameAndSize(context: Context, uri: Uri): Pair<String?, Long> {
        var name: String? = null
        var size = 0L
        try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                return f.name.takeIf { it.isNotBlank() } to f.length()
            }
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0) name = c.getString(ni)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        } catch (_: Throwable) {
        }
        return name to size
    }

    private fun canOpen(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Throwable) {
            false
        }
    }

    fun resolvePath(context: Context, uri: Uri): String {
        try {
            if (uri.scheme == "file") {
                uri.path?.let { return it }
            }
            // Many file managers put the real path in the link, e.g.
            // content://com.coloros.filemanager/root/storage/emulated/0/My Documents/Sahiba.mp3
            uri.path?.indexOf("/storage/")?.takeIf { it >= 0 }?.let { at -> return uri.path!!.substring(at) }
            if (uri.authority == "com.android.externalstorage.documents") {
                val docId = DocumentsContract.getDocumentId(uri)
                val parts = docId.split(":", limit = 2)
                if (parts.size == 2) {
                    val root = if (parts[0].equals("primary", ignoreCase = true)) {
                        "/storage/emulated/0"
                    } else {
                        "/storage/${parts[0]}"
                    }
                    return "$root/${parts[1]}"
                }
            }
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (i >= 0) {
                        val value = c.getString(i)
                        if (!value.isNullOrBlank()) return value
                    }
                }
            }
        } catch (_: Throwable) {
        }
        return uri.toString()
    }

    private fun decodeCover(bytes: ByteArray?): Bitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) {
            null
        }
    }

    /** Reads tags and technical info for an audio file. Call off the main thread. Returns null if it cannot be opened. */
    fun readAudioInfo(context: Context, uri: Uri): ExternalAudioInfo? {
        if (!canOpen(context, uri)) return null
        val (name, size) = queryNameAndSize(context, uri)
        val displayName = name ?: uri.lastPathSegment ?: "Audio"
        val baseTitle = displayName.substringBeforeLast('.', displayName)
        val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs = 0L
        var bitrateKbps = 0
        var sampleRate = 0
        var coverBytes: ByteArray? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            bitrateKbps = ((retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull() ?: 0L) / 1000L).toInt()
            sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.toIntOrNull() ?: 0
            coverBytes = retriever.embeddedPicture
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }

        val track = AudioTrackItem(
            mediaId = 0L,
            id = uri.toString(),
            contentUri = uri,
            title = title?.takeIf { it.isNotBlank() } ?: baseTitle,
            artist = cleanArtistName(artist),
            album = album?.takeIf { it.isNotBlank() } ?: "Unknown album",
            ext = ext.ifBlank { "audio" },
            durationMs = durationMs,
            durationSec = (durationMs / 1000L).toInt(),
            durationLabel = formatDurationMs(durationMs),
            sizeBytes = size,
            sizeMB = size / 1048576.0,
            folder = "External"
        )

        return ExternalAudioInfo(
            track = track,
            cover = decodeCover(coverBytes),
            sampleRateHz = sampleRate,
            bitrateKbps = bitrateKbps,
            format = ext.uppercase(Locale.US),
            path = resolvePath(context, uri),
            fileName = displayName
        )
    }

    /**
     * The same song in the music library (MediaStore), if it's there. Doppler can always read
     * library songs itself, while a file shared by another app only stays readable while that
     * app's grant lasts (it ends when the pop-up closes). Matched by full path, then by file
     * name with the same size / length. Needs the audio permission; null otherwise.
     */
    suspend fun findInLibrary(context: Context, info: ExternalAudioInfo): AudioTrackItem? {
        if (!MediaStoreScanner.hasAudioPermission(context)) return null
        val library = try {
            MediaStoreScanner.scanAudio(context)
        } catch (_: Throwable) {
            return null
        }
        val path = info.path.takeIf { it.startsWith("/") }
        if (path != null) library.firstOrNull { it.path == path }?.let { return it }
        val name = info.fileName.takeIf { it.isNotBlank() } ?: return null
        val size = info.track.sizeBytes
        val duration = info.track.durationMs
        return library.firstOrNull { song ->
            song.path.substringAfterLast('/') == name &&
                (size <= 0L || song.sizeBytes == size) &&
                (duration <= 0L || abs(song.durationMs - duration) <= 2_000L)
        }
    }

    /**
     * A private copy of a file that isn't in the library, so it keeps playing in the app after
     * the pop-up closes (when the other app's grant ends). Only the latest copy is kept.
     */
    fun copyToAppStorage(context: Context, info: ExternalAudioInfo): AudioTrackItem? {
        return try {
            val dir = File(context.cacheDir, "opened_audio").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val name = info.fileName.ifBlank { "audio.${info.track.ext}" }.replace('/', '_')
            val target = File(dir, name)
            context.contentResolver.openInputStream(info.track.contentUri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            val uri = Uri.fromFile(target)
            info.track.copy(id = uri.toString(), contentUri = uri, path = target.absolutePath)
        } catch (_: Throwable) {
            null
        }
    }

    /** Builds a VideoItem for a video opened from another app. Call off the main thread. Returns null if it cannot be opened. */
    fun buildVideoItem(context: Context, uri: Uri): VideoItem? {
        if (!canOpen(context, uri)) return null
        val (name, size) = queryNameAndSize(context, uri)
        val displayName = name ?: uri.lastPathSegment ?: "Video"
        val title = displayName.substringBeforeLast('.', displayName)
        val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US).ifBlank { "mp4" }

        var durationMs = 0L
        var width = 0
        var height = 0
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }

        val now = System.currentTimeMillis()
        return VideoItem(
            mediaId = 0L,
            id = uri.toString(),
            contentUri = uri,
            title = title,
            displayName = displayName,
            ext = ext,
            width = width,
            height = height,
            sizeBytes = size,
            sizeMB = size / 1048576.0,
            dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(now)),
            timestamp = now,
            durationMs = durationMs,
            durationSec = (durationMs / 1000L).toInt(),
            durationLabel = formatDurationMs(durationMs),
            progress = 0f,
            folder = "External",
            path = resolvePath(context, uri)
        )
    }
}
