package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector

/** Tabs inside the Audio "Library" screen. */
enum class LibraryTab(val id: String, val label: String) {
    SONGS("songs", "Songs"),
    ALBUMS("albums", "Albums"),
    ARTISTS("artists", "Artists"),
    PLAYLISTS("playlists", "Playlists"),
    LIKED("liked", "Liked");

    val icon: ImageVector
        get() = when (this) {
            SONGS -> Icons.Default.MusicNote
            ALBUMS -> Icons.Default.Album
            ARTISTS -> Icons.Default.Person
            PLAYLISTS -> Icons.Default.PlaylistPlay
            LIKED -> Icons.Default.Favorite
        }
}

/** Buttons that can sit in the bottom tray of the Now Playing screen. */
enum class NowPlayingAction(val id: String, val label: String) {
    SHUFFLE("shuffle", "Shuffle"),
    REPEAT("repeat", "Repeat"),
    LYRICS("lyrics", "Lyrics"),
    SLEEP("sleep", "Sleep timer")
}

enum class LibrarySongSort(val id: String, val label: String) {
    DEFAULT("default", "Default Order"),
    TITLE("title", "Title"),
    ARTIST("artist", "Artist"),
    ALBUM("album", "Album"),
    DATE_ADDED("date_added", "Date Added"),
    DURATION("duration", "Duration")
}

/** Where the Now Playing screen and mini player take their colours from. */
enum class NowPlayingColorSource(val id: String, val label: String, val description: String) {
    ALBUM_ART("album_art", "Album art", "Colours picked from the song's cover"),
    WALLPAPER("wallpaper", "Wallpaper & app theme", "Your wallpaper colours or the app's theme")
}

/** Background of the Now Playing screen. */
enum class NowPlayingBackground(val id: String, val label: String, val description: String) {
    FOLLOW_THEME("follow_theme", "Follow theme", "A solid colour from the current colours"),
    GRADIENT("gradient", "Gradient", "A gradient made from the album art"),
    BLUR("blur", "Blur", "The album art, softly blurred"),
    AMOLED("amoled", "AMOLED black", "Pure black, saves battery on OLED screens")
}

/** Colours of the Now Playing buttons, seek bar and toggles. */
enum class NowPlayingButtonColors(val id: String, val label: String, val description: String) {
    DEFAULT("default", "Default", "White on image backgrounds, theme colours otherwise"),
    PRIMARY("primary", "Primary colour", "The main colour of the current colours"),
    TERTIARY("tertiary", "Tertiary colour", "The accent colour of the current colours")
}

/** Persisted settings of the Library screen. */
data class LibraryPreferences(
    val tabOrder: List<LibraryTab> = LibraryTab.entries,
    val hiddenTabs: Set<LibraryTab> = emptySet(),
    val songSort: LibrarySongSort = LibrarySongSort.DEFAULT,
    val songAscending: Boolean = true,
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
    val nowPlayingButtonColors: NowPlayingButtonColors = NowPlayingButtonColors.DEFAULT
) {
    companion object {
        const val SHORT_AUDIO_LIMIT_MS = 30_000L
    }

    /** Applies the library filters to the scanned tracks. */
    fun filterTracks(tracks: List<AudioTrackItem>): List<AudioTrackItem> =
        if (!hideShortAudio && !hideRecordings) tracks
        else tracks.filter { t ->
            !(hideShortAudio && t.durationMs in 1 until SHORT_AUDIO_LIMIT_MS) &&
                !(hideRecordings && t.looksLikeRecording)
        }

    /** Tray buttons in the user's order with hidden ones removed. */
    val visibleTray: List<NowPlayingAction>
        get() = trayOrder.filter { it !in hiddenTray }

    /** Tabs in the user's order with hidden ones removed. Never empty. */
    val visibleTabs: List<LibraryTab>
        get() = tabOrder.filter { it !in hiddenTabs }.ifEmpty { listOf(tabOrder.firstOrNull() ?: LibraryTab.SONGS) }
}
