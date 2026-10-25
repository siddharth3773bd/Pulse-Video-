@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferencesRepository
import com.example.model.AudioPlaylist
import com.example.model.AudioTrackItem
import com.example.model.LIKED_PLAYLIST_ID
import com.example.model.LibraryPreferences
import com.example.model.LibrarySongSort
import com.example.model.LibraryTab
import com.example.ui.theme.LocalM3Palette
import kotlinx.coroutines.launch

/** Bottom padding of every list so the last rows scroll above the nav pill (and the mini player). */
private val LocalLibraryBottomPadding = compositionLocalOf { 132.dp }

private data class AlbumGroup(
    val key: String,
    val name: String,
    val artist: String,
    val tracks: List<AudioTrackItem>
)

private data class ArtistGroup(
    val key: String,
    val name: String,
    val tracks: List<AudioTrackItem>
)

/** What the playlist name dialog is doing. */
private sealed interface PlaylistDialog {
    data class Create(val trackIds: List<String>) : PlaylistDialog
    data class Rename(val id: String, val name: String) : PlaylistDialog
    data class ConfirmDelete(val id: String, val name: String) : PlaylistDialog
}

/** Everything a detail page (album, artist or playlist) shows above its songs. */
private data class DetailInfo(
    val title: String,
    val subtitle: String,
    val artUri: Uri?,
    val artShape: Shape,
    val placeholderIcon: ImageVector,
    val tracks: List<AudioTrackItem>,
    /** Set for user playlists: enables "Remove from playlist" and the playlist menu. */
    val playlist: AudioPlaylist? = null
)

private fun sortSongs(
    tracks: List<AudioTrackItem>,
    sort: LibrarySongSort,
    ascending: Boolean
): List<AudioTrackItem> {
    val base = when (sort) {
        LibrarySongSort.DEFAULT -> tracks
        LibrarySongSort.TITLE -> tracks.sortedBy { it.title.lowercase() }
        LibrarySongSort.ARTIST -> tracks.sortedWith(
            compareBy<AudioTrackItem>({ it.artist.lowercase() }, { it.title.lowercase() })
        )
        LibrarySongSort.ALBUM -> tracks.sortedWith(
            compareBy<AudioTrackItem>({ it.album.lowercase() }, { it.title.lowercase() })
        )
        LibrarySongSort.DATE_ADDED -> tracks.sortedBy { it.dateAddedSec }
        LibrarySongSort.DURATION -> tracks.sortedBy { it.durationMs }
    }
    return if (ascending) base else base.asReversed()
}

private fun buildAlbums(tracks: List<AudioTrackItem>, ascending: Boolean): List<AlbumGroup> {
    val groups = tracks.groupBy { albumKeyOf(it) }.map { (key, list) ->
        val sorted = list.sortedBy { it.title.lowercase() }
        AlbumGroup(
            key = key,
            name = sorted.first().album,
            artist = sorted.first().artist,
            tracks = sorted
        )
    }.sortedBy { it.name.lowercase() }
    return if (ascending) groups else groups.asReversed()
}

private fun buildArtists(tracks: List<AudioTrackItem>, ascending: Boolean): List<ArtistGroup> {
    val groups = tracks.groupBy { it.artist.lowercase() }.map { (key, list) ->
        val sorted = list.sortedBy { it.title.lowercase() }
        ArtistGroup(key = key, name = sorted.first().artist, tracks = sorted)
    }.sortedBy { it.name.lowercase() }
    return if (ascending) groups else groups.asReversed()
}

private fun matchesQuery(track: AudioTrackItem, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return false
    return track.title.contains(q, ignoreCase = true) ||
        track.artist.contains(q, ignoreCase = true) ||
        track.album.contains(q, ignoreCase = true)
}

private fun songCountLabel(count: Int): String = if (count == 1) "1 song" else "$count songs"
private fun albumCountLabel(count: Int): String = if (count == 1) "1 album" else "$count albums"
private fun artistCountLabel(count: Int): String = if (count == 1) "1 artist" else "$count artists"
private fun playlistCountLabel(count: Int): String = if (count == 1) "1 playlist" else "$count playlists"

/** "42 min" or "1 h 12 min". */
private fun totalDurationLabel(tracks: List<AudioTrackItem>): String {
    val minutes = (tracks.sumOf { it.durationMs } / 60_000L).coerceAtLeast(if (tracks.isEmpty()) 0 else 1)
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

/** Last part of a folder path, e.g. "WhatsApp Audio". */
private fun folderName(path: String): String = path.substringAfterLast('/').ifBlank { path }

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

/**
 * Music library, built from official Material 3 components:
 * a "Music" title with a search icon that opens the full-screen search, PrimaryScrollableTabRow,
 * an action row (Play + Shuffle, count, Sort menu) and a HorizontalPager of segmented lists.
 * Long-press a song to select several; the selection gets its own floating toolbar.
 */
@Composable
fun AudioLibraryScreen(
    tracks: List<AudioTrackItem>,
    playingAudio: AudioTrackItem?,
    isAudioPlaying: Boolean,
    audioProgress: Float,
    amoledBlack: Boolean,
    headerBackground: Color,
    onPlayQueue: (List<AudioTrackItem>, Int, Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
    extraBottomPadding: Dp = 0.dp,
    onPlayNext: (List<AudioTrackItem>) -> Unit = {},
    onAddToQueue: (List<AudioTrackItem>) -> Unit = {},
    onLibraryChanged: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs by remember(context) {
        AppPreferencesRepository.observeLibraryPreferences(context)
    }.collectAsState(initial = LibraryPreferences())

    // Library filters from Me > Music (short clips, recordings, blacklist).
    val libraryTracks = remember(
        tracks,
        prefs.hideShortAudio,
        prefs.hideRecordings,
        prefs.blacklistedSongs,
        prefs.blacklistedFolders
    ) {
        prefs.filterTracks(tracks)
    }
    val tracksById = remember(libraryTracks) { libraryTracks.associateBy { it.id } }

    val visibleTabs = prefs.visibleTabs
    val tabsState by rememberUpdatedState(visibleTabs)

    var selectedTabId by rememberSaveable { mutableStateOf(LibraryTab.SONGS.id) }
    val pagerState = rememberPagerState(
        initialPage = visibleTabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0),
        pageCount = { visibleTabs.size }
    )

    // Keep the same tab selected when tabs are hidden or reordered.
    LaunchedEffect(visibleTabs) {
        val idx = visibleTabs.indexOfFirst { it.id == selectedTabId }
        val target = if (idx >= 0) idx else 0
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    // Remember which tab the user swiped to.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            tabsState.getOrNull(page)?.let { selectedTabId = it.id }
        }
    }

    val currentPage = pagerState.currentPage.coerceIn(0, (visibleTabs.size - 1).coerceAtLeast(0))
    val currentTab = visibleTabs.getOrNull(currentPage) ?: visibleTabs.first()

    val selectTab: (LibraryTab) -> Unit = { tab ->
        val idx = visibleTabs.indexOf(tab)
        if (idx >= 0) {
            selectedTabId = tab.id
            scope.launch { pagerState.animateScrollToPage(idx) }
        }
    }

    var detailTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }

    // Official Material 3 search: the search icon is the collapsed SearchBar, and the full-screen
    // search view expands out of it.
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberContainedSearchBarState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val query = textFieldState.text.toString()

    var infoTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    /** "<list key>|<track id>" of the song whose ⋮ menu is open. */
    var openSongMenu by remember { mutableStateOf<String?>(null) }
    var openPlaylistMenu by remember { mutableStateOf<String?>(null) }
    var addToPlaylistTracks by remember { mutableStateOf<List<AudioTrackItem>?>(null) }
    var playlistDialog by remember { mutableStateOf<PlaylistDialog?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectionMode = selectedIds.isNotEmpty()

    val sortedSongs = remember(libraryTracks, prefs.songSort, prefs.songAscending) {
        sortSongs(libraryTracks, prefs.songSort, prefs.songAscending)
    }
    val likedSongs = remember(sortedSongs, prefs.likedIds) {
        sortedSongs.filter { it.id in prefs.likedIds }
    }
    val albums = remember(libraryTracks, prefs.groupAscending) { buildAlbums(libraryTracks, prefs.groupAscending) }
    val artists = remember(libraryTracks, prefs.groupAscending) { buildArtists(libraryTracks, prefs.groupAscending) }

    fun playlistTracks(playlist: AudioPlaylist): List<AudioTrackItem> =
        playlist.trackIds.mapNotNull { tracksById[it] }

    fun detailFor(tab: LibraryTab): DetailInfo? {
        if (detailTabId != tab.id || detailKey == null) return null
        return when (tab) {
            LibraryTab.ALBUMS -> albums.firstOrNull { it.key == detailKey }?.let { album ->
                DetailInfo(
                    title = album.name,
                    subtitle = "${album.artist} · ${songCountLabel(album.tracks.size)} · ${totalDurationLabel(album.tracks)}",
                    artUri = album.tracks.first().contentUri,
                    artShape = RoundedCornerShape(28.dp),
                    placeholderIcon = LibraryTab.ALBUMS.icon,
                    tracks = album.tracks
                )
            }
            LibraryTab.ARTISTS -> artists.firstOrNull { it.key == detailKey }?.let { artist ->
                DetailInfo(
                    title = artist.name,
                    subtitle = "${songCountLabel(artist.tracks.size)} · ${totalDurationLabel(artist.tracks)}",
                    artUri = artist.tracks.first().contentUri,
                    artShape = CircleShape,
                    placeholderIcon = LibraryTab.ARTISTS.icon,
                    tracks = artist.tracks
                )
            }
            LibraryTab.PLAYLISTS -> if (detailKey == LIKED_PLAYLIST_ID) {
                DetailInfo(
                    title = "Liked songs",
                    subtitle = "${songCountLabel(likedSongs.size)} · ${totalDurationLabel(likedSongs)}",
                    artUri = null,
                    artShape = RoundedCornerShape(28.dp),
                    placeholderIcon = Icons.Default.Favorite,
                    tracks = likedSongs
                )
            } else {
                prefs.playlists.firstOrNull { it.id == detailKey }?.let { playlist ->
                    val list = playlistTracks(playlist)
                    DetailInfo(
                        title = playlist.name,
                        subtitle = "${songCountLabel(list.size)} · ${totalDurationLabel(list)}",
                        artUri = list.firstOrNull()?.contentUri,
                        artShape = RoundedCornerShape(28.dp),
                        placeholderIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        tracks = list,
                        playlist = playlist
                    )
                }
            }
            else -> null
        }
    }

    val currentDetail = detailFor(currentTab)
    val inDetail = currentDetail != null
    val closeDetail = {
        detailTabId = null
        detailKey = null
    }
    fun openDetail(tab: LibraryTab, key: String) {
        detailTabId = tab.id
        detailKey = key
    }

    /** The song list on screen right now; long-press selection works inside it. */
    val currentSongList: List<AudioTrackItem>? = when {
        currentDetail != null -> currentDetail.tracks
        currentTab == LibraryTab.SONGS -> sortedSongs
        else -> null
    }
    val selectedTracks = remember(selectedIds, currentSongList) {
        currentSongList.orEmpty().filter { it.id in selectedIds }
    }

    // Leaving the list (other tab, other page) ends the selection.
    LaunchedEffect(currentTab, detailTabId, detailKey) { selectedIds = emptySet() }
    // Start each search fresh once the search screen is closed.
    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) textFieldState.clearText()
    }
    // The nav pill and mini player make room for the selection toolbar.
    LaunchedEffect(selectionMode) { onSelectionModeChange(selectionMode) }
    DisposableEffect(Unit) { onDispose { onSelectionModeChange(false) } }

    BackHandler(enabled = selectionMode) { selectedIds = emptySet() }
    BackHandler(enabled = inDetail && !selectionMode) { closeDetail() }

    val background = headerBackground

    val playFromList: (List<AudioTrackItem>, Int) -> Unit = { list, index ->
        val target = list.getOrNull(index)
        if (target != null) {
            if (playingAudio?.id == target.id) onTogglePlayPause() else onPlayQueue(list, index, false)
        }
    }
    val shuffle: (List<AudioTrackItem>) -> Unit = { pool ->
        if (pool.isNotEmpty()) onPlayQueue(pool, pool.indices.random(), true)
    }
    val toggleSelected: (AudioTrackItem) -> Unit = { track ->
        selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
    }

    // Delete uses Android's own confirmation dialog (MediaStore.createDeleteRequest).
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedIds = emptySet()
            onLibraryChanged()
        }
    }
    val deleteTracks: (List<AudioTrackItem>) -> Unit = { list ->
        if (list.isNotEmpty()) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, list.map { it.contentUri })
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure {
                Toast.makeText(context, "Couldn't delete these files", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val blacklistTracks: (List<AudioTrackItem>) -> Unit = { list ->
        if (list.isNotEmpty()) {
            scope.launch { AppPreferencesRepository.blacklistSongs(context, list) }
            val what = if (list.size == 1) "Song" else "${list.size} songs"
            Toast.makeText(context, "$what blacklisted. Manage in Me › Music.", Toast.LENGTH_SHORT).show()
            selectedIds = emptySet()
        }
    }
    val blacklistFolder: (AudioTrackItem) -> Unit = { track ->
        val folder = track.folderPath
        if (folder.isBlank()) {
            Toast.makeText(context, "Couldn't find this song's folder", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch { AppPreferencesRepository.setFolderBlacklisted(context, folder, true) }
            Toast.makeText(
                context,
                "“${folderName(folder)}” blacklisted. Manage in Me › Music.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** The ⋮ menu for [track], shown from the row in list [listKey]. */
    val songMenu: @Composable (String, AudioTrackItem, AudioPlaylist?) -> Unit = { listKey, track, playlist ->
        val menuKey = "$listKey|${track.id}"
        val albumKey = albumKeyOf(track)
        val artistKey = track.artist.lowercase()
        val liked = track.id in prefs.likedIds
        SongActionsMenu(
            expanded = openSongMenu == menuKey,
            onDismiss = { if (openSongMenu == menuKey) openSongMenu = null },
            liked = liked,
            canGoToAlbum = LibraryTab.ALBUMS in visibleTabs && albums.any { it.key == albumKey } &&
                !(detailTabId == LibraryTab.ALBUMS.id && detailKey == albumKey),
            canGoToArtist = LibraryTab.ARTISTS in visibleTabs && artists.any { it.key == artistKey } &&
                !(detailTabId == LibraryTab.ARTISTS.id && detailKey == artistKey),
            onPlayNext = { onPlayNext(listOf(track)) },
            onAddToQueue = { onAddToQueue(listOf(track)) },
            onAddToPlaylist = { addToPlaylistTracks = listOf(track) },
            onToggleLike = {
                scope.launch { AppPreferencesRepository.setTrackLiked(context, track.id, !liked) }
            },
            onGoToAlbum = {
                scope.launch { searchBarState.animateToCollapsed() }
                openDetail(LibraryTab.ALBUMS, albumKey)
                selectTab(LibraryTab.ALBUMS)
            },
            onGoToArtist = {
                scope.launch { searchBarState.animateToCollapsed() }
                openDetail(LibraryTab.ARTISTS, artistKey)
                selectTab(LibraryTab.ARTISTS)
            },
            onShare = { shareTrack(context, track) },
            onSongInfo = { infoTrack = track },
            onBlacklistSong = { blacklistTracks(listOf(track)) },
            onBlacklistFolder = { blacklistFolder(track) },
            onDelete = { deleteTracks(listOf(track)) },
            onRemoveFromPlaylist = playlist?.let { pl ->
                fun() {
                    scope.launch { AppPreferencesRepository.removeFromAudioPlaylist(context, pl.id, listOf(track.id)) }
                }
            }
        )
    }

    /** Song rows shared by every list: tap plays, long-press selects, ⋮ opens the menu. */
    @Composable
    fun Songs(
        listKey: String,
        songs: List<AudioTrackItem>,
        header: (@Composable () -> Unit)? = null,
        playlist: AudioPlaylist? = null,
        selectable: Boolean = true,
        bottomPadding: Dp = LocalLibraryBottomPadding.current
    ) {
        SongsList(
            songs = songs,
            playingAudio = playingAudio,
            selectionMode = selectable && selectionMode,
            selectedIds = selectedIds,
            header = header,
            bottomPadding = bottomPadding,
            onClick = { list, index ->
                if (selectable && selectionMode) toggleSelected(list[index]) else playFromList(list, index)
            },
            onLongClick = if (selectable) toggleSelected else null,
            onMore = { track -> openSongMenu = "$listKey|${track.id}" },
            menu = { track -> songMenu(listKey, track, playlist) }
        )
    }

    /** Playlist pages get a ⋮ in the header with Rename and Delete. */
    val detailPlaylist = currentDetail?.playlist
    val detailTracks = currentDetail?.tracks.orEmpty()
    val detailActions: (@Composable () -> Unit)? = if (detailPlaylist == null) null else @Composable {
        Box {
            IconButton(
                onClick = { openPlaylistMenu = "detail|${detailPlaylist.id}" },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.testTag("playlist_detail_more_btn")
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Playlist options")
            }
            PlaylistActionsMenu(
                expanded = openPlaylistMenu == "detail|${detailPlaylist.id}",
                onDismiss = { openPlaylistMenu = null },
                onPlayNext = { onPlayNext(detailTracks) },
                onAddToQueue = { onAddToQueue(detailTracks) },
                onRename = { playlistDialog = PlaylistDialog.Rename(detailPlaylist.id, detailPlaylist.name) },
                onDelete = { playlistDialog = PlaylistDialog.ConfirmDelete(detailPlaylist.id, detailPlaylist.name) }
            )
        }
    }

    CompositionLocalProvider(LocalLibraryBottomPadding provides (132.dp + extraBottomPadding)) {
        Surface(
            color = background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("audio_library_screen")
                ) {
                    MusicHeader(
                        background = background,
                        selectionCount = selectedIds.size,
                        detailTitle = currentDetail?.title,
                        allSelected = currentSongList != null && selectedTracks.size == currentSongList.size,
                        allSelectedLiked = selectedTracks.isNotEmpty() && selectedTracks.all { it.id in prefs.likedIds },
                        onClearSelection = { selectedIds = emptySet() },
                        onToggleSelectAll = {
                            selectedIds = if (currentSongList != null && selectedTracks.size < currentSongList.size) {
                                currentSongList.mapTo(HashSet()) { it.id }
                            } else {
                                emptySet()
                            }
                        },
                        onLikeSelected = {
                            val likeAll = !selectedTracks.all { it.id in prefs.likedIds }
                            scope.launch {
                                AppPreferencesRepository.setTracksLiked(context, selectedTracks.map { it.id }, likeAll)
                            }
                        },
                        onBack = closeDetail,
                        detailActions = detailActions,
                        searchButton = {
                            // The collapsed search bar is just the search icon.
                            SearchBar(
                                state = searchBarState,
                                inputField = {
                                    IconButton(
                                        onClick = { scope.launch { searchBarState.animateToExpanded() } },
                                        shapes = IconButtonDefaults.shapes(),
                                        modifier = Modifier.testTag("header_search_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search music",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = CircleShape,
                                colors = SearchBarDefaults.colors(
                                    containerColor = Color.Transparent,
                                    inputFieldColors = SearchBarDefaults.inputFieldColors()
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            )
                        }
                    )

                    if (!inDetail && visibleTabs.size > 1) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = currentPage,
                            containerColor = background,
                            edgePadding = 16.dp
                        ) {
                            visibleTabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = index == currentPage,
                                    onClick = { if (!selectionMode) selectTab(tab) },
                                    text = { Text(text = tab.label, maxLines = 1) }
                                )
                            }
                        }
                    }

                    if (!inDetail) {
                        when (currentTab) {
                            LibraryTab.SONGS -> LibraryActionRow(
                                label = songCountLabel(sortedSongs.size),
                                onPlay = { onPlayQueue(sortedSongs, 0, false) }.takeIf { sortedSongs.isNotEmpty() },
                                onShuffle = { shuffle(sortedSongs) }.takeIf { sortedSongs.isNotEmpty() },
                                sortMenu = { expanded, onDismiss ->
                                    LibrarySortMenu(
                                        expanded = expanded,
                                        onDismiss = onDismiss,
                                        field = prefs.songSort,
                                        ascending = prefs.songAscending,
                                        onSelectField = { field ->
                                            scope.launch {
                                                AppPreferencesRepository.updateLibrarySongSort(context, field, prefs.songAscending)
                                            }
                                        },
                                        onSelectAscending = { asc ->
                                            scope.launch {
                                                AppPreferencesRepository.updateLibrarySongSort(context, prefs.songSort, asc)
                                            }
                                        }
                                    )
                                }
                            )
                            LibraryTab.ALBUMS, LibraryTab.ARTISTS -> LibraryActionRow(
                                label = if (currentTab == LibraryTab.ALBUMS) albumCountLabel(albums.size)
                                else artistCountLabel(artists.size),
                                onPlay = null,
                                onShuffle = null,
                                sortMenu = { expanded, onDismiss ->
                                    LibrarySortMenu(
                                        expanded = expanded,
                                        onDismiss = onDismiss,
                                        field = null,
                                        ascending = prefs.groupAscending,
                                        onSelectField = {},
                                        onSelectAscending = { asc ->
                                            scope.launch { AppPreferencesRepository.updateLibraryGroupAscending(context, asc) }
                                        }
                                    )
                                }
                            )
                            LibraryTab.PLAYLISTS -> LibraryActionRow(
                                label = playlistCountLabel(prefs.playlists.size),
                                onPlay = null,
                                onShuffle = null,
                                sortMenu = null,
                                leading = {
                                    FilledTonalButton(
                                        onClick = { playlistDialog = PlaylistDialog.Create(emptyList()) },
                                        shapes = ButtonDefaults.shapes(),
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                        modifier = Modifier.testTag("library_new_playlist_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                        Text("New playlist")
                                    }
                                }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        key = { index -> visibleTabs.getOrNull(index)?.id ?: index.toString() },
                        userScrollEnabled = !inDetail && !selectionMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        val tab = visibleTabs.getOrNull(page) ?: return@HorizontalPager
                        val pageDetail = detailFor(tab)
                        when {
                            pageDetail != null -> Songs(
                                listKey = "detail",
                                songs = pageDetail.tracks,
                                playlist = pageDetail.playlist,
                                header = {
                                    DetailHero(
                                        info = pageDetail,
                                        onPlay = { onPlayQueue(pageDetail.tracks, 0, false) }
                                            .takeIf { pageDetail.tracks.isNotEmpty() },
                                        onShuffle = { shuffle(pageDetail.tracks) }
                                            .takeIf { pageDetail.tracks.isNotEmpty() }
                                    )
                                }
                            )
                            tab == LibraryTab.SONGS -> {
                                if (sortedSongs.isEmpty()) {
                                    LibraryEmptyState(
                                        icon = Icons.Default.MusicNote,
                                        title = "No audio files found",
                                        subtitle = "Music files stored on your device will appear here."
                                    )
                                } else {
                                    Songs(listKey = "songs", songs = sortedSongs)
                                }
                            }
                            tab == LibraryTab.ALBUMS -> AlbumsGrid(
                                albums = albums,
                                onOpen = { album -> openDetail(LibraryTab.ALBUMS, album.key) }
                            )
                            tab == LibraryTab.ARTISTS -> ArtistsList(
                                artists = artists,
                                onOpen = { artist -> openDetail(LibraryTab.ARTISTS, artist.key) }
                            )
                            tab == LibraryTab.PLAYLISTS -> PlaylistsList(
                                likedCount = likedSongs.size,
                                playlists = prefs.playlists,
                                firstTrackOf = { playlist -> playlist.trackIds.firstNotNullOfOrNull { tracksById[it] } },
                                countOf = { playlist -> playlistTracks(playlist).size },
                                openMenuId = openPlaylistMenu,
                                onOpenLiked = { openDetail(LibraryTab.PLAYLISTS, LIKED_PLAYLIST_ID) },
                                onOpen = { playlist -> openDetail(LibraryTab.PLAYLISTS, playlist.id) },
                                onMore = { playlist -> openPlaylistMenu = "list|${playlist.id}" },
                                menu = { playlist ->
                                    PlaylistActionsMenu(
                                        expanded = openPlaylistMenu == "list|${playlist.id}",
                                        onDismiss = { openPlaylistMenu = null },
                                        onPlayNext = { onPlayNext(playlistTracks(playlist)) },
                                        onAddToQueue = { onAddToQueue(playlistTracks(playlist)) },
                                        onRename = { playlistDialog = PlaylistDialog.Rename(playlist.id, playlist.name) },
                                        onDelete = { playlistDialog = PlaylistDialog.ConfirmDelete(playlist.id, playlist.name) }
                                    )
                                }
                            )
                        }
                    }
                }

                if (selectionMode) {
                    AudioSelectionToolbar(
                        onPlayNext = {
                            onPlayNext(selectedTracks)
                            selectedIds = emptySet()
                        },
                        onAddToQueue = {
                            onAddToQueue(selectedTracks)
                            selectedIds = emptySet()
                        },
                        onAddToPlaylist = { addToPlaylistTracks = selectedTracks },
                        onBlacklist = { blacklistTracks(selectedTracks) },
                        onDelete = { deleteTracks(selectedTracks) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // ---- Full-screen search ----------------------------------------------------------------
    val searchColors = SearchBarDefaults.containedColors(state = searchBarState)
    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                colors = searchColors.inputFieldColors,
                onSearch = { keyboardController?.hide() },
                placeholder = {
                    Text(
                        text = "Search your music",
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                },
                leadingIcon = {
                    IconButton(
                        onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { textFieldState.clearText() },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.testTag("library_search_field")
            )
        },
        colors = searchColors
    ) {
        val results = remember(sortedSongs, query) {
            sortedSongs.filter { matchesQuery(it, query) }
        }
        when {
            query.isBlank() -> LibraryEmptyState(
                icon = Icons.Default.Search,
                title = "Search your music",
                subtitle = "Find songs by title, artist or album."
            )
            results.isEmpty() -> LibraryEmptyState(
                icon = Icons.Default.Search,
                title = "No results",
                subtitle = "Nothing matches “${query.trim()}”."
            )
            else -> Songs(
                listKey = "search",
                songs = results,
                selectable = false,
                bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        }
    }

    // ---- Sheets and dialogs -----------------------------------------------------------------
    infoTrack?.let { track ->
        SongInfoSheet(track = track, onDismiss = { infoTrack = null })
    }

    addToPlaylistTracks?.let { list ->
        AddToPlaylistSheet(
            playlists = prefs.playlists,
            songCount = list.size,
            onCreateNew = {
                addToPlaylistTracks = null
                playlistDialog = PlaylistDialog.Create(list.map { it.id })
            },
            onPick = { playlist ->
                addToPlaylistTracks = null
                selectedIds = emptySet()
                scope.launch { AppPreferencesRepository.addToAudioPlaylist(context, playlist.id, list.map { it.id }) }
                Toast.makeText(context, "Added to “${playlist.name}”", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { addToPlaylistTracks = null }
        )
    }

    when (val dialog = playlistDialog) {
        is PlaylistDialog.Create -> PlaylistNameDialog(
            title = "New playlist",
            initialName = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                playlistDialog = null
                selectedIds = emptySet()
                scope.launch { AppPreferencesRepository.createAudioPlaylist(context, name, dialog.trackIds) }
                if (dialog.trackIds.isNotEmpty()) {
                    Toast.makeText(context, "Added to “$name”", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { playlistDialog = null }
        )
        is PlaylistDialog.Rename -> PlaylistNameDialog(
            title = "Rename playlist",
            initialName = dialog.name,
            confirmLabel = "Rename",
            onConfirm = { name ->
                playlistDialog = null
                scope.launch { AppPreferencesRepository.renameAudioPlaylist(context, dialog.id, name) }
            },
            onDismiss = { playlistDialog = null }
        )
        is PlaylistDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = { playlistDialog = null },
            title = { Text("Delete “${dialog.name}”?") },
            text = { Text("The playlist is removed. The songs stay on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistDialog = null
                        if (detailTabId == LibraryTab.PLAYLISTS.id && detailKey == dialog.id) closeDetail()
                        scope.launch { AppPreferencesRepository.deleteAudioPlaylist(context, dialog.id) }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("playlist_delete_confirm")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistDialog = null }, shapes = ButtonDefaults.shapes()) {
                    Text("Cancel")
                }
            }
        )
        null -> Unit
    }
}

/** Same key the Albums tab groups by. */
private fun albumKeyOf(track: AudioTrackItem): String =
    if (track.albumId != 0L) "id:${track.albumId}" else "name:${track.album.lowercase()}"

/** Android share sheet with the audio file attached. */
private fun shareTrack(context: Context, track: AudioTrackItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, track.contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Share “${track.title}”")) }
        .onFailure { Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show() }
}

// ---------------------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------------------

/**
 * Same header as the Videos and Me tabs: a large title with the search icon on the right.
 * Inside an album, artist or playlist it shows Back; while selecting it shows the selection.
 */
@Composable
private fun MusicHeader(
    background: Color,
    selectionCount: Int,
    detailTitle: String?,
    allSelected: Boolean,
    allSelectedLiked: Boolean,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onLikeSelected: () -> Unit,
    onBack: () -> Unit,
    detailActions: (@Composable () -> Unit)?,
    searchButton: @Composable () -> Unit
) {
    val headerBg by animateColorAsState(
        targetValue = if (selectionCount > 0) MaterialTheme.colorScheme.surfaceContainerHigh else background,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "musicHeaderBg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                selectionCount > 0 -> {
                    IconButton(
                        onClick = onClearSelection,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.testTag("selection_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$selectionCount selected",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("selection_count")
                    )
                    IconButton(
                        onClick = onLikeSelected,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.testTag("selection_like_btn")
                    ) {
                        Icon(
                            imageVector = if (allSelectedLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (allSelectedLiked) "Unlike" else "Like"
                        )
                    }
                    IconButton(
                        onClick = onToggleSelectAll,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.testTag("selection_all_btn")
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = if (allSelected) "Deselect all" else "Select all"
                        )
                    }
                }
                detailTitle != null -> {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.testTag("music_detail_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = detailTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    detailActions?.invoke()
                }
                else -> {
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("music_title")
                    )
                    searchButton()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Action row
// ---------------------------------------------------------------------------------------------

/** Official Expressive button group: Play (filled) and Shuffle (tonal). */
@Composable
private fun PlayShuffleButtons(onPlay: (() -> Unit)?, onShuffle: (() -> Unit)?) {
    val playInteraction = remember { MutableInteractionSource() }
    val shuffleInteraction = remember { MutableInteractionSource() }
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onPlay != null) {
            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = onPlay,
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        interactionSource = playInteraction,
                        modifier = Modifier
                            .animateWidth(playInteraction)
                            .testTag("library_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Play")
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = {
                            onPlay()
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
        if (onShuffle != null) {
            customItem(
                buttonGroupContent = {
                    FilledTonalButton(
                        onClick = onShuffle,
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        interactionSource = shuffleInteraction,
                        modifier = Modifier
                            .animateWidth(shuffleInteraction)
                            .testTag("library_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Shuffle")
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Shuffle") },
                        onClick = {
                            onShuffle()
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}

/**
 * Play and Shuffle (or another [leading] button) on the left; the count and the Sort button,
 * which opens the Expressive sort menu, on the right.
 */
@Composable
private fun LibraryActionRow(
    label: String,
    onPlay: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    sortMenu: (@Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit)?,
    leading: (@Composable () -> Unit)? = null
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
        } else if (onPlay != null || onShuffle != null) {
            PlayShuffleButtons(onPlay = onPlay, onShuffle = onShuffle)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("library_count_label")
        )
        if (sortMenu != null) {
            Box {
                IconButton(
                    onClick = { sortExpanded = true },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.testTag("library_sort_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                sortMenu(sortExpanded) { sortExpanded = false }
            }
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detail header (album, artist, playlist)
// ---------------------------------------------------------------------------------------------

@Composable
private fun DetailHero(
    info: DetailInfo,
    onPlay: (() -> Unit)?,
    onShuffle: (() -> Unit)?
) {
    val palette = LocalM3Palette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
            .testTag("music_detail_header"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (info.artUri != null) {
            AudioArt(
                uri = info.artUri,
                placeholderBackground = palette.surfaceContainerHighest,
                placeholderTint = palette.primary,
                modifier = Modifier.size(200.dp),
                shape = info.artShape,
                iconSize = 64.dp
            )
        } else {
            Surface(
                shape = info.artShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(200.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(info.placeholderIcon, contentDescription = null, modifier = Modifier.size(72.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = info.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = info.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (onPlay != null || onShuffle != null) {
            Spacer(modifier = Modifier.height(16.dp))
            PlayShuffleButtons(onPlay = onPlay, onShuffle = onShuffle)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Songs
// ---------------------------------------------------------------------------------------------

/** Official visual scrollbar (Material 3) for lazy lists and grids. */
@Composable
private fun Modifier.libraryScrollbar(state: androidx.compose.foundation.gestures.ScrollableState): Modifier {
    val indicator = state.scrollIndicatorState ?: return this
    return this.nonInteractiveScrollbar(state = indicator, orientation = Orientation.Vertical)
}

/**
 * Material 3 Expressive segmented list: rows sit in rounded containers separated by the official
 * gap, with position-aware corners (ListItemDefaults.segmentedShapes).
 */
@Composable
private fun SongsList(
    songs: List<AudioTrackItem>,
    playingAudio: AudioTrackItem?,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    header: (@Composable () -> Unit)?,
    bottomPadding: Dp,
    onClick: (List<AudioTrackItem>, Int) -> Unit,
    onLongClick: ((AudioTrackItem) -> Unit)?,
    onMore: (AudioTrackItem) -> Unit,
    menu: @Composable (AudioTrackItem) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .libraryScrollbar(listState)
            .testTag("audio_library_view"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        if (header != null) {
            item(key = "header", contentType = "header") { header() }
        }
        if (songs.isEmpty() && header != null) {
            item(key = "empty", contentType = "empty") {
                Text(
                    text = "No songs here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        }
        itemsIndexed(songs, key = { _, t -> t.id }, contentType = { _, _ -> "song" }) { index, track ->
            SongRow(
                track = track,
                index = index,
                count = songs.size,
                playing = playingAudio?.id == track.id,
                selectionMode = selectionMode,
                checked = track.id in selectedIds,
                onClick = { onClick(songs, index) },
                onLongClick = onLongClick?.let { handler -> { handler(track) } },
                onMore = { onMore(track) },
                menu = { menu(track) }
            )
        }
    }
}

/**
 * Official segmented list item. Outside selection, the current song uses the selected state;
 * while selecting, the selected state marks the chosen songs and a check covers the art.
 */
@Composable
private fun SongRow(
    track: AudioTrackItem,
    index: Int,
    count: Int,
    playing: Boolean,
    selectionMode: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onMore: () -> Unit,
    menu: @Composable () -> Unit
) {
    val palette = LocalM3Palette.current
    SegmentedListItem(
        selected = if (selectionMode) checked else playing,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = "Select",
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = {
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                AudioArt(
                    uri = track.contentUri,
                    placeholderBackground = palette.surfaceContainerHighest,
                    placeholderTint = palette.primary,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                if (selectionMode && checked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            // While selecting, the ⋮ steps aside; the selection toolbar holds the actions.
            if (!selectionMode) {
                Box {
                    IconButton(onClick = onMore, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    menu()
                }
            }
        },
        colors = segmentedListColors(),
        modifier = Modifier.testTag("audio_track_row_${track.mediaId}")
    ) {
        Text(
            text = track.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Container colours used by the official segmented-list samples. */
@Composable
private fun segmentedListColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
)

// ---------------------------------------------------------------------------------------------
// Albums / Artists
// ---------------------------------------------------------------------------------------------

@Composable
private fun AlbumsGrid(
    albums: List<AlbumGroup>,
    onOpen: (AlbumGroup) -> Unit
) {
    val palette = LocalM3Palette.current
    if (albums.isEmpty()) {
        LibraryEmptyState(
            icon = LibraryTab.ALBUMS.icon,
            title = "No albums found",
            subtitle = "Albums from your music will appear here."
        )
        return
    }
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .libraryScrollbar(gridState)
            .testTag("library_albums_grid"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = LocalLibraryBottomPadding.current
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { it.key }) { album ->
            Card(
                onClick = { onOpen(album) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                AudioArt(
                    uri = album.tracks.first().contentUri,
                    placeholderBackground = palette.surfaceContainerHighest,
                    placeholderTint = palette.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(0.dp),
                    iconSize = 40.dp
                )
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsList(
    artists: List<ArtistGroup>,
    onOpen: (ArtistGroup) -> Unit
) {
    val palette = LocalM3Palette.current
    if (artists.isEmpty()) {
        LibraryEmptyState(
            icon = LibraryTab.ARTISTS.icon,
            title = "No artists found",
            subtitle = "Artists from your music will appear here."
        )
        return
    }
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .libraryScrollbar(listState)
            .testTag("library_artists_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = LocalLibraryBottomPadding.current
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        itemsIndexed(artists, key = { _, a -> a.key }) { index, artist ->
            SegmentedListItem(
                onClick = { onOpen(artist) },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = artists.size),
                leadingContent = {
                    AudioArt(
                        uri = artist.tracks.first().contentUri,
                        placeholderBackground = palette.surfaceContainerHighest,
                        placeholderTint = palette.primary,
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape
                    )
                },
                supportingContent = { Text(songCountLabel(artist.tracks.size)) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                colors = segmentedListColors()
            ) {
                Text(
                    text = artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Playlists
// ---------------------------------------------------------------------------------------------

/** "Liked songs" pinned first, then the user's playlists. */
@Composable
private fun PlaylistsList(
    likedCount: Int,
    playlists: List<AudioPlaylist>,
    firstTrackOf: (AudioPlaylist) -> AudioTrackItem?,
    countOf: (AudioPlaylist) -> Int,
    openMenuId: String?,
    onOpenLiked: () -> Unit,
    onOpen: (AudioPlaylist) -> Unit,
    onMore: (AudioPlaylist) -> Unit,
    menu: @Composable (AudioPlaylist) -> Unit
) {
    val palette = LocalM3Palette.current
    val rows = playlists.size + 1
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .libraryScrollbar(listState)
            .testTag("library_playlists_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = LocalLibraryBottomPadding.current
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        item(key = LIKED_PLAYLIST_ID) {
            SegmentedListItem(
                onClick = onOpenLiked,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = rows),
                leadingContent = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                        }
                    }
                },
                supportingContent = { Text(songCountLabel(likedCount)) },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                colors = segmentedListColors(),
                modifier = Modifier.testTag("playlist_row_liked")
            ) {
                Text("Liked songs")
            }
        }
        itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
            val first = firstTrackOf(playlist)
            SegmentedListItem(
                onClick = { onOpen(playlist) },
                shapes = ListItemDefaults.segmentedShapes(index = index + 1, count = rows),
                leadingContent = {
                    if (first != null) {
                        AudioArt(
                            uri = first.contentUri,
                            placeholderBackground = palette.surfaceContainerHighest,
                            placeholderTint = palette.primary,
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
                            }
                        }
                    }
                },
                supportingContent = { Text(songCountLabel(countOf(playlist))) },
                trailingContent = {
                    Box {
                        IconButton(onClick = { onMore(playlist) }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Playlist options")
                        }
                        menu(playlist)
                    }
                },
                colors = segmentedListColors(),
                modifier = Modifier.testTag("playlist_row_${playlist.id}")
            ) {
                Text(text = playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (playlists.isEmpty()) {
            item(key = "hint") {
                Text(
                    text = "Make a playlist with New playlist, or long-press songs to add several at once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------------------------

@Composable
private fun LibraryEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("audio_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(MaterialShapes.Sunny.toShape())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
