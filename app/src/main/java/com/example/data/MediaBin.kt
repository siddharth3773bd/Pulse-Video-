package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android's own bin (MediaStore trash, Android 11+). Deleting a video or song moves it here;
 * Android keeps it for 30 days and then deletes it for good. Other apps that use the same bin
 * (like Files by Google) see the same items.
 */
object MediaBin {

    data class Item(
        val uri: Uri,
        val name: String,
        val isVideo: Boolean,
        /** Song artist (blank for videos). */
        val artist: String,
        val durationMs: Long,
        val sizeBytes: Long,
        /** When Android deletes it for good (ms since 1970), or 0 if unknown. */
        val expiresAtMs: Long
    ) {
        val key: String get() = uri.toString()
    }

    /** Everything in the bin that this app can see (videos and songs), newest deleted first. */
    suspend fun load(context: Context): List<Item> = withContext(Dispatchers.IO) {
        val videos = query(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        val songs = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, isVideo = false)
        (videos + songs).sortedByDescending { it.expiresAtMs }
    }

    private fun query(context: Context, collection: Uri, isVideo: Boolean): List<Item> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.TITLE)
            add(MediaStore.MediaColumns.DATE_EXPIRES)
            add(MediaStore.MediaColumns.DURATION)
            add(MediaStore.MediaColumns.SIZE)
            if (!isVideo) add(MediaStore.Audio.AudioColumns.ARTIST)
        }.toTypedArray()
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.MediaColumns.DATE_EXPIRES))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        }
        val items = ArrayList<Item>()
        try {
            context.contentResolver.query(collection, projection, args, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
                val expiresCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val artistCol = if (isVideo) -1 else c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val fileName = cleanName(c.getString(nameCol).orEmpty())
                    val title = cleanName(c.getString(titleCol).orEmpty())
                    val name = when {
                        // Songs: the tag title reads better than the file name.
                        !isVideo && title.isNotBlank() -> title
                        else -> fileName.substringBeforeLast('.').ifBlank { fileName }
                    }
                    val artist = if (artistCol >= 0) c.getString(artistCol).orEmpty() else ""
                    items += Item(
                        uri = ContentUris.withAppendedId(collection, id),
                        name = name.ifBlank { if (isVideo) "Video" else "Song" },
                        isVideo = isVideo,
                        artist = if (artist == MediaStore.UNKNOWN_STRING) "" else artist,
                        durationMs = c.getLong(durationCol),
                        sizeBytes = c.getLong(sizeCol),
                        expiresAtMs = c.getLong(expiresCol) * 1000L
                    )
                }
            }
        } catch (_: Exception) {
        }
        return items
    }

    /** Trashed files are renamed ".trashed-<time>-name"; show the name without that. */
    private fun cleanName(raw: String): String = raw.replace(TrashedPrefix, "")

    private val TrashedPrefix = Regex("^\\.trashed-\\d+-")
}
