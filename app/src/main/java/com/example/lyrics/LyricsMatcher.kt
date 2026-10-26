package com.example.lyrics

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Finds the right lyrics for a song file, even when its tags are messy (downloaded songs often
 * have site names in the title, every singer crammed into the artist, a time as the artist, or the
 * folder name as the album).
 *
 * 1. Clean what the file says: the real title, the movie or album it's from, the separate artists.
 * 2. Ask LRCLIB a few different ways (see [LyricsRepository]).
 * 3. Give every result points: same title, a shared artist, the same length, the same movie or
 *    album, lyrics that cover the whole song, synced lines. The best one wins if it's clearly good.
 */
object LyricsMatcher {

    /** What we know about the song, cleaned up. */
    data class Query(
        val title: String,
        val artists: List<String>,
        /** The movie (from "(From "X")") or a real album name; null when unknown. */
        val albumHint: String?,
        val durationSec: Int
    ) {
        val mainArtist: String? get() = artists.firstOrNull()
    }

    /** A result with its points. */
    data class Scored(val result: LyricsSearchResult, val score: Int)

    /** A clear winner needs at least this many points. */
    const val GOOD_SCORE = 50

    /** Two results this close (with different lyrics) mean "maybe the other one". */
    private const val CLOSE_CALL = 8

    fun queryFor(title: String, artist: String, album: String, folder: String, durationSec: Int): Query {
        val (cleanTitle, movie) = cleanTitle(title)
        val artists = splitArtists(artist)
        val albumHint = movie ?: cleanAlbum(album, folder)
        return Query(cleanTitle, artists, albumHint, durationSec)
    }

    // ------------------------------------------------------------------ cleaning

    /** Site names that download pages stamp into tags. */
    private val SiteWords = Regex(
        """(?i)\b(pagal\s?world|pagalfree|pagal\s?new|djmaza|dj\s?punjab|mr[-\s]?jatt|songs\.?pk|wapking|webmusic|""" +
            """raag(song)?|bestwap|downloadming|masstamilan|isaimini|starmusiq|naa\s?songs|gaana|jiosaavn|saavn|""" +
            """hungama|wynk|spotify|youtube|ytmp3|y2mate|savefrom|mp3\s?juice|songspk|djjohal|riskyjatt)\b"""
    )
    private val Domain = Regex("""(?i)\b(www\.)?[a-z0-9-]+\.(com|in|net|org|info|co|cc|me|pk|io|fm|biz|xyz)\b""")
    private val Bracketed = Regex("""\s*[(\[{【][^)\]}】]*[)\]}】]""")
    /** "… - From Jawan" (after a dash, so a title like "Message from Heaven" is left alone). */
    private val FromMovie = Regex("""(?i)\s[-–—|:]\s*from\s+(?:the\s+(?:movie|film|album)\s+)?(.+)$""")
    /** "(From "Zid")", "[From Fighter]". */
    private val FromMovieInside = Regex("""(?i)[(\[{]\s*from\s+(?:the\s+(?:movie|film|album)\s+)?[\["“”']*\s*([^"“”')\]}]+?)\s*[\]"“”']*\s*[)\]}]""")
    /** From "Fighter" / From ["Fighter"], anywhere. */
    private val QuotedFrom = Regex("""(?i)\bfrom\s*[\[(]?\s*["“”']([^"“”']+)["“”']\s*[\])]?""")
    private val TrackNumber = Regex("""^\s*\d{1,3}\s*[-._)]\s*""")
    private val Featuring = Regex("""(?i)\s+(feat\.?|ft\.?|featuring)\s+.*$""")
    private val TrailingJunk = Regex(
        """(?i)\s*[-|–—:]\s*(.*\b(remaster(ed)?|version|edit|mix|live|lyric(al|s)?|official|video|audio|full\s+song|hd|hq|4k|""" +
            """\d{2,3}\s?kbps|slowed|reverb|lofi|lo-fi|reprise|unplugged|cover)\b.*)$"""
    )
    private val QualityWords = Regex("""(?i)\b(\d{2,3}\s?kbps|official\s+(music\s+)?video|lyrical(\s+video)?|full\s+(video\s+)?song|video\s+song|audio\s+song|hd|hq|4k|1080p|720p)\b""")

    /** "01 - Vande Mataram (The Fighter Anthem) From ["Fighter"] - PagalWorld.com" -> ("Vande Mataram", "Fighter"). */
    fun cleanTitle(raw: String): Pair<String, String?> {
        var t = raw.replace('_', ' ').trim()
        t = t.replace(Regex("""(?i)\.(mp3|m4a|flac|wav|ogg|opus|aac)$"""), "")
        // The movie or album the song is from, wherever it sits.
        var movie: String? = null
        FromMovieInside.find(t)?.let { m ->
            movie = m.groupValues[1].trim()
            t = t.removeRange(m.range)
        }
        for (pattern in listOf(QuotedFrom, FromMovie)) {
            if (movie != null) break
            pattern.find(t)?.let { m ->
                movie = m.groupValues[1].trim()
                t = t.removeRange(m.range)
            }
        }
        t = t.replace(Domain, " ").replace(SiteWords, " ")
        t = t.replace(Bracketed, " ")
        t = t.replace(TrackNumber, "")
        t = t.replace(Featuring, "")
        t = t.replace(TrailingJunk, "")
        t = t.replace(QualityWords, " ")
        t = t.replace(Regex("""\s{2,}"""), " ").trim(' ', '-', '|', '.', ',', ':', '–', '—')
        val cleanMovie = movie?.replace(Regex("""\s{2,}"""), " ")?.trim(' ', '-', '|', '.', ',', '"', '\'')
            ?.takeIf { it.isNotBlank() }
        return (t.ifBlank { raw.trim() }) to cleanMovie
    }

    /** True when an artist tag is junk: a time ("5:42"), a number, "Unknown", a website. */
    fun isJunkArtist(name: String): Boolean {
        val n = name.trim()
        if (n.isBlank()) return true
        if (Regex("""^\d{1,2}[:.]\d{2}([:.]\d{2})?$""").matches(n)) return true
        if (Regex("""^[\d\s.,:-]+$""").matches(n)) return true
        val lower = n.lowercase(Locale.ROOT)
        if (lower in setOf("unknown", "unknown artist", "<unknown>", "various artists", "va", "artist", "n/a", "none")) return true
        if (Domain.containsMatchIn(n) || SiteWords.containsMatchIn(n)) return true
        return false
    }

    /** "Vishal-Sheykhar, B Praak & Vishal Dadlani" -> ["Vishal-Sheykhar", "B Praak", "Vishal Dadlani"]. */
    fun splitArtists(raw: String): List<String> =
        raw.split(Regex("""\s*(,|&|;|/|\||\s\+\s|\sx\s|\sfeat\.?\s|\sft\.?\s|\sfeaturing\s|\sand\s|\swith\s|、)\s*""", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() && !isJunkArtist(it) }
            .distinctBy { normalize(it) }

    /** An album tag worth using: not the folder's name (Android's guess), not "Unknown", not a website. */
    fun cleanAlbum(album: String, folder: String): String? {
        val a = album.trim()
        if (a.isBlank()) return null
        val lower = a.lowercase(Locale.ROOT)
        if (lower == folder.trim().lowercase(Locale.ROOT)) return null
        if (lower in setOf("unknown album", "<unknown>", "unknown", "music", "download", "downloads", "my documents",
                "documents", "audio", "songs", "whatsapp audio", "telegram audio", "recordings")) return null
        if (Domain.containsMatchIn(a) || SiteWords.containsMatchIn(a)) return null
        return cleanTitle(a).first.takeIf { it.isNotBlank() }
    }

    /** Lower case, no accents, letters and digits only, single spaces. */
    fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")
        return noAccents.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
    }

    private fun words(text: String): Set<String> = normalize(text).split(' ').filter { it.isNotBlank() }.toSet()

    // ------------------------------------------------------------------ scoring

    fun score(result: LyricsSearchResult, query: Query): Int {
        var points = 0

        // Title.
        val ours = normalize(query.title)
        val theirs = normalize(cleanTitle(result.trackName).first)
        points += when {
            ours.isEmpty() || theirs.isEmpty() -> 0
            ours == theirs -> 40
            ours.contains(theirs) || theirs.contains(ours) -> 30
            else -> {
                val a = words(ours)
                val b = words(theirs)
                val overlap = a.intersect(b).size.toDouble() / a.union(b).size.coerceAtLeast(1)
                if (overlap >= 0.5) (overlap * 30).toInt() else -30
            }
        }

        // Artist: any of ours among theirs.
        if (query.artists.isNotEmpty()) {
            val theirArtists = normalize(result.artistName)
            val theirWords = words(result.artistName)
            val full = query.artists.any { a ->
                val n = normalize(a)
                n.isNotEmpty() && (theirArtists.contains(n) || n.contains(theirArtists) && theirArtists.isNotEmpty())
            }
            val partial = query.artists.any { a -> words(a).any { it.length > 2 && it in theirWords } }
            points += when {
                full -> 25
                partial -> 12
                else -> -10
            }
        }

        // Length: the same recording is within a couple of seconds.
        if (query.durationSec > 0 && result.durationSec > 0) {
            val diff = abs(query.durationSec - result.durationSec)
            points += when {
                diff <= 2 -> 25
                diff <= 5 -> 15
                diff <= 10 -> 5
                diff <= 20 -> -10
                else -> -30
            }
        }

        // Same movie or album.
        val hint = query.albumHint?.let(::normalize)
        if (!hint.isNullOrEmpty() && result.albumName.isNotBlank()) {
            val album = normalize(result.albumName)
            if (album == hint || album.contains(hint) || hint.contains(album)) points += 10
        }

        // Do the lyrics cover the whole song? (Some entries stop after the first lines.)
        val synced = result.syncedLyrics
        if (!synced.isNullOrBlank()) {
            points += 8
            val lastSec = lastTimestampSec(synced)
            val length = if (query.durationSec > 0) query.durationSec else result.durationSec
            if (length > 0 && lastSec >= 0) {
                val covered = lastSec.toDouble() / length
                points += when {
                    covered >= 0.6 -> 10
                    covered >= 0.35 -> 0
                    else -> -35
                }
            }
        } else if (!result.plainLyrics.isNullOrBlank()) {
            val lines = result.plainLyrics.lines().count { it.isNotBlank() }
            if (lines < 4) points -= 20
        }
        return points
    }

    /** Best first. */
    fun rank(results: List<LyricsSearchResult>, query: Query): List<Scored> =
        results.filter { it.hasAnyLyrics || it.instrumental }
            .distinctBy { it.id }
            .map { Scored(it, score(it, query)) }
            .sortedByDescending { it.score }

    /** The winner when it's clearly good, and whether another version came close. */
    fun decide(ranked: List<Scored>): Pair<LyricsSearchResult?, Boolean> {
        val best = ranked.firstOrNull() ?: return null to false
        if (best.score < GOOD_SCORE) return null to false
        val runnerUp = ranked.drop(1).firstOrNull { other ->
            best.score - other.score <= CLOSE_CALL && !sameLyrics(best.result, other.result)
        }
        return best.result to (runnerUp != null)
    }

    private fun sameLyrics(a: LyricsSearchResult, b: LyricsSearchResult): Boolean {
        val ta = normalize(a.syncedLyrics ?: a.plainLyrics.orEmpty()).replace(Regex("""\d+"""), "")
        val tb = normalize(b.syncedLyrics ?: b.plainLyrics.orEmpty()).replace(Regex("""\d+"""), "")
        if (ta.isEmpty() || tb.isEmpty()) return ta == tb
        val wa = ta.split(' ').toSet()
        val wb = tb.split(' ').toSet()
        return wa.intersect(wb).size.toDouble() / wa.union(wb).size.coerceAtLeast(1) > 0.85
    }

    /** The last "[mm:ss.xx]" in synced lyrics, in seconds; -1 if none. */
    private fun lastTimestampSec(lrc: String): Int {
        val match = Regex("""\[(\d{1,3}):(\d{2})(?:[.:]\d{1,3})?]""").findAll(lrc).lastOrNull() ?: return -1
        return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
    }
}
