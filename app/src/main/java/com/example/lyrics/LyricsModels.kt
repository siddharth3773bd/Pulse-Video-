package com.example.lyrics

/** One word with its own timing (only in "enhanced" LRC files). */
data class LyricWord(val text: String, val startMs: Long, val endMs: Long)

/** A line of lyrics. [timeMs] is -1 for plain (unsynced) lyrics. */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

/** Parsed lyrics, ready to show. */
data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean
) {
    val isEmpty: Boolean get() = lines.none { it.text.isNotBlank() }
}

/** Where the lyrics on screen came from. */
enum class LyricsSource(val label: String) {
    USER_FILE("Your file"),
    EMBEDDED("Inside the song file"),
    ONLINE("LRCLIB")
}

/** What the lyrics screen shows. */
sealed interface LyricsState {
    data object Loading : LyricsState
    data class Loaded(
        val lyrics: Lyrics,
        val source: LyricsSource,
        /** The song file has its own lyrics (so "Use online lyrics instead" makes sense). */
        val hasEmbedded: Boolean,
        val hasUserFile: Boolean,
        val offsetMs: Long
    ) : LyricsState

    /** Nothing saved and auto-search is off: ask "Search online / Load lyrics file". */
    data object NeedsChoice : LyricsState
    data object NotFound : LyricsState
    data object Instrumental : LyricsState
    data object NoInternet : LyricsState
}

/** One LRCLIB result, used by "Search again". */
data class LyricsSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSec: Int,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
) {
    val hasSynced: Boolean get() = !syncedLyrics.isNullOrBlank()
    val hasAnyLyrics: Boolean get() = hasSynced || !plainLyrics.isNullOrBlank()
}

/** Lyrics text size on the lyrics screen. */
enum class LyricsTextSize(val id: String, val label: String, val activeSp: Int, val plainSp: Int) {
    SMALL("small", "Small", 24, 19),
    MEDIUM("medium", "Medium", 30, 22),
    LARGE("large", "Large", 36, 26)
}

/** Lyrics settings that apply to every song. */
data class LyricsSettings(
    /** Search LRCLIB automatically when a song has no lyrics of its own. */
    val autoSearch: Boolean = false,
    val textSize: LyricsTextSize = LyricsTextSize.MEDIUM,
    val alignCenter: Boolean = false,
    val blur: Boolean = true
)
