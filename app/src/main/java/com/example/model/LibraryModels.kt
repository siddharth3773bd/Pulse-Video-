package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
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
    FOLDERS("folders", "Folders"),
    LIKED("liked", "Liked");

    val icon: ImageVector
        get() = when (this) {
            SONGS -> Icons.Default.MusicNote
            ALBUMS -> Icons.Default.Album
            ARTISTS -> Icons.Default.Person
            PLAYLISTS -> Icons.Default.PlaylistPlay
            FOLDERS -> Icons.Default.Folder
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

enum class LibraryNavMode(val id: String) {
    TAB_ROW("tab_row"),
    COMPACT("compact")
}

enum class LibrarySongSort(val id: String, val label: String) {
    DEFAULT("default", "Default Order"),
    TITLE("title", "Title"),
    ARTIST("artist", "Artist"),
    ALBUM("album", "Album"),
    DATE_ADDED("date_added", "Date Added"),
    DURATION("duration", "Duration")
}

/** Persisted settings of the Library screen. */
data class LibraryPreferences(
    val tabOrder: List<LibraryTab> = LibraryTab.entries,
    val hiddenTabs: Set<LibraryTab> = emptySet(),
    val navMode: LibraryNavMode = LibraryNavMode.TAB_ROW,
    val songSort: LibrarySongSort = LibrarySongSort.DEFAULT,
    val songAscending: Boolean = true,
    val groupAscending: Boolean = true,
    val trayOrder: List<NowPlayingAction> = NowPlayingAction.entries,
    val hiddenTray: Set<NowPlayingAction> = emptySet(),
    val likedIds: Set<String> = emptySet()
) {
    /** Tray buttons in the user's order with hidden ones removed. */
    val visibleTray: List<NowPlayingAction>
        get() = trayOrder.filter { it !in hiddenTray }

    /** Tabs in the user's order with hidden ones removed. Never empty. */
    val visibleTabs: List<LibraryTab>
        get() = tabOrder.filter { it !in hiddenTabs }.ifEmpty { listOf(tabOrder.firstOrNull() ?: LibraryTab.SONGS) }
}
