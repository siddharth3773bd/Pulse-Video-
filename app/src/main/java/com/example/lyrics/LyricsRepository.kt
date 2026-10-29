package com.example.lyrics

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.inspector.MetadataRetriever
import com.example.model.AudioTrackItem
import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** What the app has saved for one song. */
data class SongLyricsRecord(
    /** Lyrics from a file the user loaded; beats everything else. */
    val userText: String? = null,
    /** Lyrics found on LRCLIB (saved so they work offline next time). */
    val onlineText: String? = null,
    val instrumental: Boolean = false,
    /** LRCLIB had nothing for this song (cleared by Search again). */
    val notFound: Boolean = false,
    /** "Use online lyrics instead" even though the song file has its own. */
    val preferOnline: Boolean = false,
    /** Sync adjustment: positive shows lines later. */
    val offsetMs: Long = 0L,
    /** Which matching rules found [onlineText] / [notFound]; older results get one fresh look. */
    val matchVersion: Int = 0,
    /** Another version came close when these lyrics were picked automatically. */
    val ambiguous: Boolean = false
)

/**
 * Finds lyrics for a song. Order: the user's own file, then (if chosen) online, then the
 * lyrics inside the song file, then LRCLIB. Online results are saved on the phone.
 */
object LyricsRepository {

    // ---------------------------------------------------------------- resolve

    /** Bumped when the matching gets better, so automatic picks made before are looked at again. */
    private const val MATCH_VERSION = 2

    /**
     * Works out what to show for [track].
     * [allowNetwork] = may search LRCLIB now (auto-search on, or the user asked).
     */
    suspend fun resolve(
        context: Context,
        track: AudioTrackItem,
        settings: LyricsSettings,
        forceOnline: Boolean = false
    ): LyricsState = withContext(Dispatchers.IO) {
        val stored = readRecord(context, track)
        val embeddedText = readEmbedded(context, track)
        val hasEmbedded = !embeddedText.isNullOrBlank()
        // An automatic pick (or "not found") from older, simpler matching gets one fresh look.
        val outdated = stored.userText == null && !stored.preferOnline && stored.matchVersion < MATCH_VERSION &&
            (stored.onlineText != null || stored.notFound)
        val record = if (outdated && (settings.autoSearch || forceOnline)) {
            stored.copy(onlineText = null, notFound = false, instrumental = false)
        } else stored

        fun loaded(text: String, source: LyricsSource): LyricsState {
            val lyrics = LrcParser.parse(text)
            val fresh = readRecordCached(track) ?: record
            return if (lyrics.isEmpty) LyricsState.NotFound
            else LyricsState.Loaded(
                lyrics, source, hasEmbedded, record.userText != null, record.offsetMs,
                otherVersions = source == LyricsSource.ONLINE && fresh.ambiguous && !fresh.preferOnline
            )
        }

        // 1. The user's own file.
        record.userText?.let { return@withContext loaded(it, LyricsSource.USER_FILE) }

        val wantsOnline = forceOnline || record.preferOnline
        val mayFetch = forceOnline || settings.autoSearch || record.preferOnline

        // 2. Already saved from LRCLIB and wanted (or nothing better).
        val savedOnline = record.onlineText
        if (savedOnline != null && (wantsOnline || !hasEmbedded)) {
            return@withContext loaded(savedOnline, LyricsSource.ONLINE)
        }

        // 3. Lyrics inside the song file.
        if (hasEmbedded && !wantsOnline) {
            val embedded = LrcParser.parse(embeddedText!!)
            // Plain embedded lyrics: with auto-search on, quietly look for synced ones online.
            if (!embedded.synced && settings.autoSearch && !record.notFound) {
                val synced = savedOnline?.takeIf { LrcParser.parse(it).synced }
                    ?: runCatching { fetchOnline(context, track, record) }.getOrNull()
                        ?.takeIf { LrcParser.parse(it).synced }
                if (synced != null) return@withContext loaded(synced, LyricsSource.ONLINE)
            }
            return@withContext loaded(embeddedText, LyricsSource.EMBEDDED)
        }

        // 4. LRCLIB.
        if (record.instrumental && !forceOnline) return@withContext LyricsState.Instrumental
        if (record.notFound && !forceOnline) {
            return@withContext if (hasEmbedded) loaded(embeddedText!!, LyricsSource.EMBEDDED) else LyricsState.NotFound
        }
        if (!mayFetch) return@withContext LyricsState.NeedsChoice
        try {
            val text = fetchOnline(context, track, record)
            val fresh = readRecord(context, track)
            when {
                text != null -> loaded(text, LyricsSource.ONLINE)
                fresh.instrumental -> LyricsState.Instrumental
                hasEmbedded -> loaded(embeddedText!!, LyricsSource.EMBEDDED)
                else -> LyricsState.NotFound
            }
        } catch (e: LyricsNetworkException) {
            when {
                // Couldn't look again: keep what was found before.
                outdated && stored.onlineText != null -> loaded(stored.onlineText, LyricsSource.ONLINE)
                hasEmbedded -> loaded(embeddedText!!, LyricsSource.EMBEDDED)
                e.offline || !isOnline(context) -> LyricsState.NoInternet
                else -> LyricsState.ServiceBusy
            }
        }
    }

    /** Searches LRCLIB for [track], saves the outcome and returns the lyrics text (or null). */
    private suspend fun fetchOnline(context: Context, track: AudioTrackItem, record: SongLyricsRecord): String? {
        val (result, ambiguous) = findBest(track)
        val base = record.copy(matchVersion = MATCH_VERSION, ambiguous = ambiguous)
        return when {
            result == null -> {
                writeRecord(context, track, base.copy(notFound = true, instrumental = false))
                null
            }
            result.hasAnyLyrics -> {
                val text = result.syncedLyrics ?: result.plainLyrics!!
                writeRecord(context, track, base.copy(onlineText = text, notFound = false, instrumental = false))
                text
            }
            else -> {
                writeRecord(context, track, base.copy(instrumental = result.instrumental, notFound = !result.instrumental))
                null
            }
        }
    }

    /** The song's details, cleaned (see [LyricsMatcher]). */
    fun queryFor(track: AudioTrackItem): LyricsMatcher.Query = LyricsMatcher.queryFor(
        title = track.title,
        artist = track.artist,
        album = track.album,
        folder = track.folder,
        durationSec = (track.durationMs / 1000L).toInt()
    )

    /**
     * Asks LRCLIB several ways at once and lets [LyricsMatcher] pick: the best result if it's
     * clearly good (null otherwise), and whether another version came close.
     */
    private suspend fun findBest(track: AudioTrackItem): Pair<LyricsSearchResult?, Boolean> = coroutineScope {
        val ranked = rankCandidates(queryFor(track))
        LyricsMatcher.decide(ranked)
    }

    /** All candidates for [query], best first. Throws [LyricsNetworkException] only if every request failed. */
    suspend fun rankCandidates(query: LyricsMatcher.Query): List<LyricsMatcher.Scored> = coroutineScope {
        val title = query.title
        val artists = query.artists
        val requests = buildList<suspend () -> List<LyricsSearchResult>> {
            artists.firstOrNull()?.let { artist ->
                add { listOfNotNull(LrcLibClient.get(title, artist, query.albumHint, query.durationSec)) }
                add { LrcLibClient.search(title, artist) }
            }
            artists.getOrNull(1)?.let { artist -> add { LrcLibClient.search(title, artist) } }
            query.albumHint?.let { hint -> add { LrcLibClient.searchWords("$title $hint") } }
            add { LrcLibClient.search(title, null) }
        }
        val outcomes = requests.map { request -> async { runCatching { request() } } }.awaitAll()
        val found = outcomes.mapNotNull { it.getOrNull() }.flatten()
        if (found.isEmpty()) {
            outcomes.firstNotNullOfOrNull { it.exceptionOrNull() as? LyricsNetworkException }?.let { throw it }
        }
        LyricsMatcher.rank(found, query)
    }

    /** Whether the phone has a working internet connection right now. */
    private fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** "Song (From “Movie”) [Remastered] - site.com" -> "Song". */
    fun cleanTitle(title: String): String = LyricsMatcher.cleanTitle(title).first

    private fun readRecordCached(track: AudioTrackItem): SongLyricsRecord? = cache[keyOf(track)]

    // ---------------------------------------------------------------- user actions

    /** "Search again" with the user's own words: every version found, best match first. */
    suspend fun searchManually(title: String, artist: String, album: String, durationSec: Int): List<LyricsSearchResult> {
        val query = LyricsMatcher.Query(
            title = LyricsMatcher.cleanTitle(title).first.ifBlank { title.trim() },
            artists = LyricsMatcher.splitArtists(artist),
            albumHint = album.trim().ifBlank { null },
            durationSec = durationSec
        )
        return rankCandidates(query).map { it.result }
    }

    /** The user picked [result] from "Search again": use it for this song. */
    suspend fun useResult(context: Context, track: AudioTrackItem, result: LyricsSearchResult) {
        val record = readRecord(context, track)
        val text = result.syncedLyrics ?: result.plainLyrics
        writeRecord(
            context, track,
            record.copy(
                onlineText = text,
                instrumental = result.instrumental && text == null,
                notFound = false,
                preferOnline = true
            )
        )
    }

    suspend fun setUserFile(context: Context, track: AudioTrackItem, text: String?) {
        writeRecord(context, track, readRecord(context, track).copy(userText = text))
    }

    suspend fun setPreferOnline(context: Context, track: AudioTrackItem, prefer: Boolean) {
        writeRecord(context, track, readRecord(context, track).copy(preferOnline = prefer, notFound = false))
    }

    suspend fun setOffset(context: Context, track: AudioTrackItem, offsetMs: Long) {
        writeRecord(context, track, readRecord(context, track).copy(offsetMs = offsetMs))
    }


    /** Lyrics files the app understands. */
    private val lyricsExtensions = setOf("lrc", "txt")

    /** The picked file's name, or null if the provider doesn't say. */
    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    /** True for a .lrc / .txt name (and when the name is unknown: the content is checked instead). */
    fun isLyricsFileName(name: String?): Boolean =
        name == null || name.substringAfterLast('.', "").lowercase(Locale.ROOT) in lyricsExtensions

    /** A lyrics file is small text; anything bigger than this is not one (audio, video, photo…). */
    private const val MAX_LYRICS_BYTES = 1_000_000

    /** Reads a picked .lrc / .txt file as text; null if it is too big or isn't text at all. */
    suspend fun readTextFile(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                // Read at most one byte more than allowed, so a huge file is never loaded whole.
                val buffer = ByteArray(MAX_LYRICS_BYTES + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = input.read(buffer, total, buffer.size - total)
                    if (read < 0) break
                    total += read
                }
                if (total > MAX_LYRICS_BYTES) null else buffer.copyOf(total)
            } ?: return@runCatching null
            if (looksBinary(bytes)) return@runCatching null
            decode(bytes)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /** Text has (almost) no control characters or zero bytes; audio, video and photos are full of them. */
    private fun looksBinary(bytes: ByteArray): Boolean {
        val utf16 = bytes.size >= 2 &&
            ((bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) || (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()))
        if (utf16) return false
        val n = minOf(bytes.size, 4096)
        var bad = 0
        for (i in 0 until n) {
            val v = bytes[i].toInt() and 0xFF
            if (v == 0) return true
            if (v < 9 || v in 14..31) bad++
        }
        return bad * 100 > n
    }

    // ---------------------------------------------------------------- storage

    private val cache = ConcurrentHashMap<String, SongLyricsRecord>()

    /** Saved by title + artist + length, so it survives the file being moved or rescanned. */
    private fun keyOf(track: AudioTrackItem): String {
        val raw = "${track.title.trim().lowercase(Locale.ROOT)}|${track.artist.trim().lowercase(Locale.ROOT)}|${track.durationMs / 1000L}"
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun fileOf(context: Context, key: String) = File(File(context.filesDir, "lyrics").apply { mkdirs() }, "$key.json")

    suspend fun readRecord(context: Context, track: AudioTrackItem): SongLyricsRecord = withContext(Dispatchers.IO) {
        val key = keyOf(track)
        cache[key]?.let { return@withContext it }
        val file = fileOf(context, key)
        val record = if (file.exists()) {
            runCatching {
                val o = JSONObject(file.readText())
                SongLyricsRecord(
                    userText = o.optString("userText").takeIf { o.has("userText") && !o.isNull("userText") },
                    onlineText = o.optString("onlineText").takeIf { o.has("onlineText") && !o.isNull("onlineText") },
                    instrumental = o.optBoolean("instrumental"),
                    notFound = o.optBoolean("notFound"),
                    preferOnline = o.optBoolean("preferOnline"),
                    offsetMs = o.optLong("offsetMs"),
                    matchVersion = o.optInt("matchVersion"),
                    ambiguous = o.optBoolean("ambiguous")
                )
            }.getOrDefault(SongLyricsRecord())
        } else SongLyricsRecord()
        cache[key] = record
        record
    }

    private suspend fun writeRecord(context: Context, track: AudioTrackItem, record: SongLyricsRecord) =
        withContext(Dispatchers.IO) {
            val key = keyOf(track)
            cache[key] = record
            val o = JSONObject().apply {
                put("title", track.title)
                put("artist", track.artist)
                record.userText?.let { put("userText", it) }
                record.onlineText?.let { put("onlineText", it) }
                put("instrumental", record.instrumental)
                put("notFound", record.notFound)
                put("preferOnline", record.preferOnline)
                put("offsetMs", record.offsetMs)
                put("matchVersion", record.matchVersion)
                put("ambiguous", record.ambiguous)
            }
            runCatching { fileOf(context, key).writeText(o.toString()) }
        }

    // ---------------------------------------------------------------- embedded lyrics

    private val embeddedCache = ConcurrentHashMap<String, String>()
    private const val NONE = "\u0000"

    /** Lyrics stored inside the song file (MP3 USLT, M4A ©lyr, FLAC/Ogg LYRICS), via Media3. */
    @OptIn(UnstableApi::class)
    suspend fun readEmbedded(context: Context, track: AudioTrackItem): String? = withContext(Dispatchers.IO) {
        embeddedCache[track.id]?.let { return@withContext it.takeIf { v -> v != NONE } }
        val found = runCatching {
            MetadataRetriever.Builder(context, MediaItem.fromUri(track.contentUri)).build().use { retriever ->
                val groups = retriever.retrieveTrackGroups().get(10, TimeUnit.SECONDS)
                var text: String? = null
                loop@ for (g in 0 until groups.length) {
                    val group = groups[g]
                    for (f in 0 until group.length) {
                        val metadata = group.getFormat(f).metadata ?: continue
                        for (i in 0 until metadata.length()) {
                            text = when (val entry = metadata[i]) {
                                is TextInformationFrame ->
                                    if (entry.id == "USLT") entry.values.joinToString("\n") else null
                                is BinaryFrame -> if (entry.id == "USLT") parseUslt(entry.data) else null
                                is VorbisComment ->
                                    if (entry.key.uppercase(Locale.ROOT) in setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS")) entry.value else null
                                else -> null
                            }
                            if (!text.isNullOrBlank()) break@loop
                        }
                    }
                }
                text
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
        embeddedCache[track.id] = found ?: NONE
        found
    }

    /** ID3 USLT frame: encoding, language, description, then the lyrics. */
    private fun parseUslt(data: ByteArray): String? {
        if (data.size < 5) return null
        val encoding = data[0].toInt()
        val charset: Charset = when (encoding) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        val wide = encoding == 1 || encoding == 2
        var i = 4 // skip encoding byte + 3-byte language
        // Skip the description, which ends in a 0 (or 0 0 for UTF-16).
        while (i < data.size) {
            if (wide) {
                if (i + 1 < data.size && data[i].toInt() == 0 && data[i + 1].toInt() == 0) { i += 2; break }
                i += 2
            } else {
                if (data[i].toInt() == 0) { i += 1; break }
                i += 1
            }
        }
        if (i >= data.size) return null
        return String(data, i, data.size - i, charset).trimEnd('\u0000').takeIf { it.isNotBlank() }
    }

    private fun decode(bytes: ByteArray): String = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
            String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> String(bytes, Charsets.UTF_16LE).drop(1)
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> String(bytes, Charsets.UTF_16BE).drop(1)
        else -> String(bytes, Charsets.UTF_8)
    }
}
