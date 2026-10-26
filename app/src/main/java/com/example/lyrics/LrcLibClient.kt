package com.example.lyrics

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Thrown when LRCLIB can't be asked. [offline] = the phone couldn't reach the internet at all;
 * otherwise LRCLIB itself is busy or slow (it answers "overloaded" at busy times).
 */
class LyricsNetworkException(cause: Throwable, val offline: Boolean) : IOException(cause)

/**
 * LRCLIB (lrclib.net): a free, open lyrics database. No account or key.
 * Uses the platform HTTP client and JSON parser, so no extra libraries.
 */
object LrcLibClient {
    private const val BASE = "https://lrclib.net/api"
    private const val USER_AGENT = "Doppler/1.0 (personal Android music player)"

    /** Exact lookup by title, artist, album and length. Null when LRCLIB has no such song. */
    suspend fun get(title: String, artist: String, album: String?, durationSec: Int): LyricsSearchResult? {
        val params = buildMap {
            put("track_name", title)
            put("artist_name", artist)
            if (!album.isNullOrBlank()) put("album_name", album)
            if (durationSec > 0) put("duration", durationSec.toString())
        }
        val body = request("$BASE/get", params) ?: return null
        return parse(JSONObject(body))
    }

    /** Free search; [title] and [artist] go in their own fields when both are known. */
    suspend fun search(title: String, artist: String?, album: String? = null): List<LyricsSearchResult> {
        val params = buildMap {
            put("track_name", title)
            if (!artist.isNullOrBlank()) put("artist_name", artist)
            if (!album.isNullOrBlank()) put("album_name", album)
        }
        return parseList(request("$BASE/search", params))
    }

    /** Search by any words (title, artist, movie together), like typing into LRCLIB's own search. */
    suspend fun searchWords(words: String): List<LyricsSearchResult> =
        parseList(request("$BASE/search", mapOf("q" to words)))

    private fun parseList(body: String?): List<LyricsSearchResult> {
        if (body == null) return emptyList()
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(::parse) }
    }

    /**
     * GET [url] with [params]. Returns null when LRCLIB has nothing (404) or can't use the words
     * (400). When LRCLIB is busy (500/503/429) or slow, waits a moment and tries again, twice.
     */
    private suspend fun request(url: String, params: Map<String, String>): String? = withContext(Dispatchers.IO) {
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        var lastError: LyricsNetworkException? = null
        repeat(3) { attempt ->
            if (attempt > 0) delay(lastError?.let { retryDelayMs(it) } ?: 1_000L)
            try {
                return@withContext requestOnce("$url?$query")
            } catch (e: LyricsNetworkException) {
                lastError = e
                // No internet at all: asking again right away won't help.
                if (e.offline) throw e
            }
        }
        throw lastError ?: LyricsNetworkException(IOException("LRCLIB didn't answer"), offline = false)
    }

    private fun retryDelayMs(e: LyricsNetworkException): Long = (e.cause as? BusyException)?.retryAfterMs ?: 1_000L

    private class BusyException(code: Int, val retryAfterMs: Long) : IOException("LRCLIB returned $code")

    private fun requestOnce(fullUrl: String): String? {
        val connection = try {
            (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
        } catch (e: IOException) {
            throw LyricsNetworkException(e, offline = isOfflineError(e))
        }
        try {
            val code = connection.responseCode
            return when {
                code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_BAD_REQUEST -> null
                code in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                else -> {
                    val retryAfter = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
                        ?.coerceIn(1L, 5L)?.times(1_000L) ?: 1_000L
                    throw LyricsNetworkException(BusyException(code, retryAfter), offline = false)
                }
            }
        } catch (e: IOException) {
            if (e is LyricsNetworkException) throw e
            throw LyricsNetworkException(e, offline = isOfflineError(e))
        } finally {
            connection.disconnect()
        }
    }

    /** Can't find or reach the server at all (rather than a slow or busy server). */
    private fun isOfflineError(e: IOException): Boolean =
        e is java.net.UnknownHostException || e is java.net.NoRouteToHostException ||
            (e is java.net.ConnectException && e !is java.net.SocketTimeoutException)

    private fun parse(o: JSONObject): LyricsSearchResult = LyricsSearchResult(
        id = o.optLong("id"),
        trackName = o.optString("trackName"),
        artistName = o.optString("artistName"),
        albumName = o.optString("albumName"),
        durationSec = o.optDouble("duration", 0.0).toInt(),
        instrumental = o.optBoolean("instrumental", false),
        plainLyrics = o.optString("plainLyrics").takeIf { !o.isNull("plainLyrics") && it.isNotBlank() },
        syncedLyrics = o.optString("syncedLyrics").takeIf { !o.isNull("syncedLyrics") && it.isNotBlank() }
    )
}
