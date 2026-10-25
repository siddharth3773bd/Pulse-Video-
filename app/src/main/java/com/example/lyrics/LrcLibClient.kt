package com.example.lyrics

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Thrown when the phone can't reach LRCLIB (no internet, server down, timeout). */
class LyricsNetworkException(cause: Throwable) : IOException(cause)

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
        val body = request("$BASE/search", params) ?: return emptyList()
        val array = JSONArray(body)
        return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(::parse) }
    }

    /** GET [url] with [params]. Returns null on 404; throws [LyricsNetworkException] when offline. */
    private suspend fun request(url: String, params: Map<String, String>): String? = withContext(Dispatchers.IO) {
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        val connection = try {
            (URL("$url?$query").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
        } catch (e: IOException) {
            throw LyricsNetworkException(e)
        }
        try {
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_NOT_FOUND -> null
                code in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                else -> throw LyricsNetworkException(IOException("LRCLIB returned $code"))
            }
        } catch (e: IOException) {
            if (e is LyricsNetworkException) throw e
            throw LyricsNetworkException(e)
        } finally {
            connection.disconnect()
        }
    }

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
