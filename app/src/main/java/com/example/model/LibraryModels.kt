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
    val groupAscending: Boolean = true
) {
    /** Tabs in the user's order with hidden ones removed. Never empty. */
    val visibleTabs: List<LibraryTab>
        get() = tabOrder.filter { it !in hiddenTabs }.ifEmpty { listOf(tabOrder.firstOrNull() ?: LibraryTab.SONGS) }
}
