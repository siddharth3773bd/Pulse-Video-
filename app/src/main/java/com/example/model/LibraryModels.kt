package com.example.model

import com.example.ui.icons.Symbols
import androidx.compose.ui.graphics.vector.ImageVector

/** Tabs inside the Audio "Library" screen. */
enum class LibraryTab(val id: String, val label: String) {
    SONGS("songs", "Songs"),
    ALBUMS("albums", "Albums"),
    ARTISTS("artists", "Artists"),
    PLAYLISTS("playlists", "Playlists");

    val icon: ImageVector
        get() = when (this) {
            SONGS -> Symbols.MusicNote
            ALBUMS -> Symbols.Album
            ARTISTS -> Symbols.Person
            PLAYLISTS -> Symbols.PlaylistPlay
        }
}

/** Buttons that can sit in the bottom tray of the Now Playing screen. */
enum class NowPlayingAction(val id: String, val label: String) {
    SHUFFLE("shuffle", "Shuffle"),
    REPEAT("repeat", "Repeat"),
    SLEEP("sleep", "Sleep timer"),
    QUEUE("queue", "Queue"),
    AMBIENT("ambient", "Ambient mode")
}

/**
 * Ambient mode: a full-screen, always-on view of the song (artwork, name, artist, lyrics), on the
 * same background as Now Playing. Song name and artist only show with the artwork; artwork and
 * lyrics can't both be off.
 */
data class AmbientSettings(
    val showArtwork: Boolean = true,
    val showTitle: Boolean = true,
    val showArtist: Boolean = true,
    val showLyrics: Boolean = true,
    /** Black background for the night, whatever Now Playing uses. */
    val black: Boolean = false,
    /** Turning the phone sideways on Now Playing opens ambient mode. */
    val openOnRotate: Boolean = true
)

enum class LibrarySongSort(val id: String, val label: String) {
    DATE_ADDED("date_added", "Date added"),
    TITLE("title", "Title"),
    ARTIST("artist", "Artist"),
    ALBUM("album", "Album"),
    DURATION("duration", "Duration");

    /**
     * The order most people want for this field, used when the field is picked and shown first:
     * newest and longest first; names A to Z.
     */
    val naturalAscending: Boolean
        get() = when (this) {
            TITLE, ARTIST, ALBUM -> true
            DATE_ADDED, DURATION -> false
        }

    /** Plain names for the two sort directions, e.g. "A to Z" / "Z to A". */
    val ascendingLabel: String
        get() = when (this) {
            TITLE, ARTIST, ALBUM -> "A to Z"
            DATE_ADDED -> "Oldest first"
            DURATION -> "Shortest first"
        }
    val descendingLabel: String
        get() = when (this) {
            TITLE, ARTIST, ALBUM -> "Z to A"
            DATE_ADDED -> "Newest first"
            DURATION -> "Longest first"
        }

    companion object {
        /** The old "Default" sort was the scan order: newest added first. */
        const val LEGACY_DEFAULT_ID = "default"
    }
}

/** A song hidden from the library. Title and artist are kept so the list still reads well. */
data class BlacklistedSong(
    val id: String,
    val title: String,
    val artist: String
)

/** A user-made music playlist. Tracks are stored by their id (content URI). */
data class AudioPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>
)

/** Id of the built-in "Liked songs" playlist shown first in the Playlists tab. */
const val LIKED_PLAYLIST_ID = "liked"

/** Where the Now Playing screen and mini player take their colours from. */
enum class NowPlayingColorSource(val id: String, val label: String, val description: String) {
    ALBUM_ART("album_art", "Album art", "Colours picked from the song's cover"),
    WALLPAPER("wallpaper", "Wallpaper & app theme", "Your wallpaper colours or the app's theme")
}

/** Background of the Now Playing screen. */
enum class NowPlayingBackground(val id: String, val label: String, val description: String) {
    FOLLOW_THEME("follow_theme", "Colour", "One solid colour from the current colours"),
    GRADIENT("gradient", "Gradient", "A soft gradient of the current colours"),
    BLUR("blur", "Blur", "The album art, softly blurred"),
    AMOLED("amoled", "Black", "Pure black, saves battery on OLED screens")
}

/** Persisted settings of the Library screen. */
data class LibraryPreferences(
    val tabOrder: List<LibraryTab> = LibraryTab.entries,
    val hiddenTabs: Set<LibraryTab> = emptySet(),
    val songSort: LibrarySongSort = LibrarySongSort.DATE_ADDED,
    val songAscending: Boolean = false,
    val groupAscending: Boolean = true,
    val trayOrder: List<NowPlayingAction> = NowPlayingAction.entries,
    val hiddenTray: Set<NowPlayingAction> = emptySet(),
    val likedIds: Set<String> = emptySet(),
    /** Hide audio shorter than [SHORT_AUDIO_LIMIT_MS] (ringtones, notification sounds, clips). */
    val hideShortAudio: Boolean = true,
    /** Hide voice and call recordings. */
    val hideRecordings: Boolean = true,
    val nowPlayingColorSource: NowPlayingColorSource = NowPlayingColorSource.ALBUM_ART,
    val nowPlayingBackground: NowPlayingBackground = NowPlayingBackground.GRADIENT,
    /** Songs the user blacklisted (hidden from the library, still on the device). */
    val blacklistedSongs: List<BlacklistedSong> = emptyList(),
    /** Folders the user blacklisted; everything inside them (and their sub-folders) is hidden. */
    val blacklistedFolders: Set<String> = emptySet(),
    val playlists: List<AudioPlaylist> = emptyList(),
    val ambient: AmbientSettings = AmbientSettings(),
    val lyrics: com.example.lyrics.LyricsSettings = com.example.lyrics.LyricsSettings()
) {
    val blacklistedIds: Set<String> by lazy { blacklistedSongs.mapTo(HashSet()) { it.id } }

    /** True when [track] sits in a blacklisted folder or one of its sub-folders. */
    fun isInBlacklistedFolder(track: AudioTrackItem): Boolean {
        if (blacklistedFolders.isEmpty()) return false
        val dir = track.folderPath
        return blacklistedFolders.any { dir == it || dir.startsWith("$it/") }
    }

    companion object {
        const val SHORT_AUDIO_LIMIT_MS = 30_000L
    }

    /** Applies the library filters to the scanned tracks. */
    fun filterTracks(tracks: List<AudioTrackItem>): List<AudioTrackItem> =
        if (!hideShortAudio && !hideRecordings && blacklistedSongs.isEmpty() && blacklistedFolders.isEmpty()) tracks
        else tracks.filter { t ->
            !(hideShortAudio && t.durationMs in 1 until SHORT_AUDIO_LIMIT_MS) &&
                !(hideRecordings && t.looksLikeRecording) &&
                t.id !in blacklistedIds &&
                !isInBlacklistedFolder(t)
        }

    /** Tray buttons in the user's order with hidden ones removed. */
    val visibleTray: List<NowPlayingAction>
        get() = trayOrder.filter { it !in hiddenTray }

    /** Tabs in the user's order with hidden ones removed. Never empty. */
    val visibleTabs: List<LibraryTab>
        get() = tabOrder.filter { it !in hiddenTabs }.ifEmpty { listOf(tabOrder.firstOrNull() ?: LibraryTab.SONGS) }
}
