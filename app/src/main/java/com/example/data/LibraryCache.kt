package com.example.data

import com.example.model.cleanArtistName
import android.content.Context
import android.net.Uri
import com.example.model.AudioTrackItem
import com.example.model.VideoItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The last scanned library, kept in a small file so the next start can show it straight away
 * while the real scan runs in the background (which then quietly corrects anything that changed).
 * A plain binary file: thousands of items read in a few milliseconds.
 */
object LibraryCache {
    private const val FILE_NAME = "library_cache.bin"
    private const val VERSION = 1
    private val lock = Mutex()

    class Snapshot(val videos: List<VideoItem>, val audio: List<AudioTrackItem>)

    suspend fun load(context: Context): Snapshot? = withContext(Dispatchers.IO) {
        lock.withLock {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.isFile) return@withLock null
            try {
                DataInputStream(BufferedInputStream(file.inputStream(), 64 * 1024)).use { input ->
                    if (input.readInt() != VERSION) return@use null
                    val videos = List(input.readInt()) { input.readVideo() }
                    val audio = List(input.readInt()) { input.readAudio() }
                    Snapshot(videos, audio)
                }
            } catch (_: Exception) {
                file.delete()
                null
            }
        }
    }

    suspend fun save(context: Context, videos: List<VideoItem>, audio: List<AudioTrackItem>) = withContext(Dispatchers.IO) {
        lock.withLock {
            val file = File(context.filesDir, FILE_NAME)
            val temp = File(context.filesDir, "$FILE_NAME.tmp")
            try {
                DataOutputStream(BufferedOutputStream(temp.outputStream(), 64 * 1024)).use { out ->
                    out.writeInt(VERSION)
                    out.writeInt(videos.size)
                    videos.forEach { out.writeVideo(it) }
                    out.writeInt(audio.size)
                    audio.forEach { out.writeAudio(it) }
                }
                if (!temp.renameTo(file)) temp.delete()
            } catch (_: Exception) {
                temp.delete()
            }
        }
    }

    private fun DataOutputStream.writeVideo(v: VideoItem) {
        writeLong(v.mediaId)
        writeUTF(v.id)
        writeUTF(v.contentUri.toString())
        writeUTF(v.title)
        writeUTF(v.displayName)
        writeUTF(v.ext)
        writeUTF(v.series ?: "")
        writeInt(v.episode ?: -1)
        writeInt(v.width)
        writeInt(v.height)
        writeLong(v.sizeBytes)
        writeDouble(v.sizeMB)
        writeUTF(v.dateStr)
        writeLong(v.timestamp)
        writeLong(v.durationMs)
        writeInt(v.durationSec)
        writeUTF(v.durationLabel)
        writeUTF(v.folder)
        writeUTF(v.path)
    }

    private fun DataInputStream.readVideo(): VideoItem = VideoItem(
        mediaId = readLong(),
        id = readUTF(),
        contentUri = Uri.parse(readUTF()),
        title = readUTF(),
        displayName = readUTF(),
        ext = readUTF(),
        series = readUTF().ifEmpty { null },
        episode = readInt().takeIf { it >= 0 },
        width = readInt(),
        height = readInt(),
        sizeBytes = readLong(),
        sizeMB = readDouble(),
        dateStr = readUTF(),
        timestamp = readLong(),
        durationMs = readLong(),
        durationSec = readInt(),
        durationLabel = readUTF(),
        progress = 0f,
        folder = readUTF(),
        path = readUTF()
    )

    private fun DataOutputStream.writeAudio(a: AudioTrackItem) {
        writeLong(a.mediaId)
        writeUTF(a.id)
        writeUTF(a.contentUri.toString())
        writeUTF(a.title)
        writeUTF(a.artist)
        writeUTF(a.album)
        writeUTF(a.ext)
        writeLong(a.durationMs)
        writeInt(a.durationSec)
        writeUTF(a.durationLabel)
        writeLong(a.sizeBytes)
        writeDouble(a.sizeMB)
        writeUTF(a.folder)
        writeLong(a.albumId)
        writeLong(a.artistId)
        writeLong(a.dateAddedSec)
        writeUTF(a.path)
        writeBoolean(a.isRecording)
    }

    private fun DataInputStream.readAudio(): AudioTrackItem = AudioTrackItem(
        mediaId = readLong(),
        id = readUTF(),
        contentUri = Uri.parse(readUTF()),
        title = readUTF(),
        artist = cleanArtistName(readUTF()),
        album = readUTF(),
        ext = readUTF(),
        durationMs = readLong(),
        durationSec = readInt(),
        durationLabel = readUTF(),
        sizeBytes = readLong(),
        sizeMB = readDouble(),
        folder = readUTF(),
        albumId = readLong(),
        artistId = readLong(),
        dateAddedSec = readLong(),
        path = readUTF(),
        isRecording = readBoolean()
    )
}
