@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferencesRepository
import com.example.model.AudioPlaylist
import com.example.model.AudioTrackItem
import com.example.model.LibraryPreferences
import com.example.model.LibrarySongSort
import com.example.model.LibraryTab
import kotlinx.coroutines.launch

/** Bottom padding of every list, so the last rows clear the mini player and the selection toolbar. */
private val LocalLibraryBottomPadding = compositionLocalOf { 16.dp }

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

/** A page another screen asks the Music tab to open (Now Playing ⋮ › Go to album / artist). */
data class LibraryOpenRequest(val tab: LibraryTab, val key: String)

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

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

/**
 * Music library, built from official Material 3 Expressive components, laid out like Videos:
 * the search app bar (scrolls away, comes back on scroll up) with the full-screen search, fixed
 * tabs, and a HorizontalPager of plain list rows. Long-press a song to select several; the
 * selection gets a pinned top bar and its own floating toolbar.
 */
@Composable
fun AudioLibraryScreen(
    tracks: List<AudioTrackItem>,
    playingAudio: AudioTrackItem?,
    isAudioPlaying: Boolean,
    headerBackground: Color,
    onPlayQueue: (List<AudioTrackItem>, Int, Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    extraBottomPadding: Dp = 0.dp,
    onPlayNext: (List<AudioTrackItem>) -> Unit = {},
    onAddToQueue: (List<AudioTrackItem>) -> Unit = {},
    onLibraryChanged: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    openRequest: LibraryOpenRequest? = null,
    onOpenRequestHandled: () -> Unit = {}
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

    // Official Material 3 search: the header's search bar expands into the full-screen search.
    val textFieldState = rememberTextFieldState()
    // The contained search bar defaults to the "fast" spring, which pops open abruptly.
    // The slower expressive spatial spring (the one the regular SearchBar uses) feels smoother.
    val searchBarState = rememberContainedSearchBarState(
        animationSpecForExpand = MaterialTheme.motionScheme.slowSpatialSpec(),
        animationSpecForCollapse = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
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
            LibraryTab.PLAYLISTS -> prefs.playlists.firstOrNull { it.id == detailKey }?.let { playlist ->
                val list = playlistTracks(playlist)
                DetailInfo(
                    title = playlist.name,
                    subtitle = "${songCountLabel(list.size)} · ${totalDurationLabel(list)}",
                    artUri = list.firstOrNull()?.contentUri,
                    artShape = RoundedCornerShape(28.dp),
                    placeholderIcon = Symbols.PlaylistPlay,
                    tracks = list,
                    playlist = playlist
                )
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

    // Opened from elsewhere (Now Playing ⋮): show that album or artist.
    LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        if (request.tab in visibleTabs) {
            openDetail(request.tab, request.key)
            selectTab(request.tab)
        }
        onOpenRequestHandled()
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
    // The search app bar shows on the main pages; album/artist/playlist pages and selection get a
    // pinned top app bar instead (same as Videos).
    val searchHeader = !inDetail && !selectionMode
    val searchScroll = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    // Bring the search bar back whenever the page or mode changes.
    LaunchedEffect(currentTab, detailTabId, detailKey, selectionMode) {
        searchScroll.scrollState.scrollOffset = 0f
    }
    var sortSheetOpen by remember { mutableStateOf(false) }

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

    /** The ⋮ menu for [track], shown from the row in list [listKey]. */
    val songMenu: @Composable (String, AudioTrackItem, AudioPlaylist?) -> Unit = { listKey, track, playlist ->
        val menuKey = "$listKey|${track.id}"
        val albumKey = albumKeyOf(track)
        val artistKey = track.artist.lowercase()
        SongActionsMenu(
            expanded = openSongMenu == menuKey,
            onDismiss = { if (openSongMenu == menuKey) openSongMenu = null },
            canGoToAlbum = LibraryTab.ALBUMS in visibleTabs && albums.any { it.key == albumKey } &&
                !(detailTabId == LibraryTab.ALBUMS.id && detailKey == albumKey),
            canGoToArtist = LibraryTab.ARTISTS in visibleTabs && artists.any { it.key == artistKey } &&
                !(detailTabId == LibraryTab.ARTISTS.id && detailKey == artistKey),
            onPlayNext = { onPlayNext(listOf(track)) },
            onAddToQueue = { onAddToQueue(listOf(track)) },
            onAddToPlaylist = { addToPlaylistTracks = listOf(track) },
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
            isAudioPlaying = isAudioPlaying,
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
    val detailActions: @Composable () -> Unit = {
        if (detailPlaylist != null) {
            Box {
                TooltipIconButton(
                    label = "Playlist options",
                    icon = Symbols.MoreVert,
                    onClick = { openPlaylistMenu = "detail|${detailPlaylist.id}" },
                    modifier = Modifier.testTag("playlist_detail_more_btn")
                )
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
    }

    // The navigation bar sits below this screen, so lists only need a little room at the end:
    // more while selecting (the selection toolbar floats over the list then).
    val listBottomPadding = if (selectionMode) {
        96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else 16.dp
    CompositionLocalProvider(LocalLibraryBottomPadding provides (listBottomPadding + extraBottomPadding)) {
        Surface(
            color = background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (searchHeader) Modifier.nestedScroll(searchScroll.nestedScrollConnection)
                            else Modifier
                        )
                        .testTag("audio_library_screen")
                ) {
                    MusicHeader(
                        background = background,
                        searchState = searchBarState,
                        textFieldState = textFieldState,
                        searchScrollBehavior = searchScroll,
                        selectionCount = selectedIds.size,
                        allSelected = currentSongList != null && selectedTracks.size == currentSongList.size,
                        detailTitle = currentDetail?.title,
                        onClearSelection = { selectedIds = emptySet() },
                        onToggleSelectAll = {
                            selectedIds = if (currentSongList != null && selectedTracks.size < currentSongList.size) {
                                currentSongList.mapTo(HashSet()) { it.id }
                            } else {
                                emptySet()
                            }
                        },
                        onBack = closeDetail,
                        detailActions = detailActions,
                        headerAction = {
                            // One action beside the search bar: Sort, or New playlist on Playlists.
                            if (currentTab == LibraryTab.PLAYLISTS) {
                                TooltipIconButton(
                                    label = "New playlist",
                                    icon = Symbols.Add,
                                    onClick = { playlistDialog = PlaylistDialog.Create(emptyList()) },
                                    modifier = Modifier.testTag("library_new_playlist_button")
                                )
                            } else {
                                TooltipIconButton(
                                    label = "Sort",
                                    icon = Symbols.Sort,
                                    onClick = { sortSheetOpen = true },
                                    modifier = Modifier.testTag("library_sort_button")
                                )
                            }
                        }
                    )

                    if (!inDetail && visibleTabs.size > 1) {
                        // Fixed tabs: Music has at most four, so they share the width evenly.
                        PrimaryTabRow(
                            selectedTabIndex = currentPage,
                            containerColor = background
                        ) {
                            visibleTabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = index == currentPage,
                                    onClick = { if (!selectionMode) selectTab(tab) },
                                    text = { Text(text = tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("library_tab_${tab.id}")
                                )
                            }
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
                                        icon = Symbols.MusicNote,
                                        title = "No audio files found",
                                        subtitle = "Music files stored on your device will appear here."
                                    )
                                } else {
                                    Songs(
                                        listKey = "songs",
                                        songs = sortedSongs,
                                        header = {
                                            LibraryActionRow(
                                                label = songCountLabel(sortedSongs.size),
                                                onShuffle = { shuffle(sortedSongs) }
                                            )
                                        }
                                    )
                                }
                            }
                            tab == LibraryTab.ALBUMS -> AlbumsGrid(
                                albums = albums,
                                header = { LibraryActionRow(label = albumCountLabel(albums.size), horizontalPadding = 0.dp) },
                                onOpen = { album -> openDetail(LibraryTab.ALBUMS, album.key) }
                            )
                            tab == LibraryTab.ARTISTS -> ArtistsList(
                                artists = artists,
                                header = { LibraryActionRow(label = artistCountLabel(artists.size)) },
                                onOpen = { artist -> openDetail(LibraryTab.ARTISTS, artist.key) }
                            )
                            tab == LibraryTab.PLAYLISTS -> PlaylistsList(
                                playlists = prefs.playlists,
                                header = { LibraryActionRow(label = playlistCountLabel(prefs.playlists.size)) },
                                firstTrackOf = { playlist -> playlist.trackIds.firstNotNullOfOrNull { tracksById[it] } },
                                countOf = { playlist -> playlistTracks(playlist).size },
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

                // Status bar protection while the search bar is scrolled away (same as Videos).
                if (searchHeader) {
                    Spacer(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(background.copy(alpha = 0.92f))
                    )
                }
            }
        }
    }

    // ---- Full-screen search ----------------------------------------------------------------
    val searchColors = SearchBarDefaults.containedColors(state = searchBarState)
    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = { LibrarySearchInputField(searchBarState, textFieldState, placeholder = "Search music") },
        colors = searchColors
    ) {
        val results = remember(sortedSongs, query) {
            sortedSongs.filter { matchesQuery(it, query) }
        }
        when {
            query.isBlank() -> LibraryEmptyState(
                icon = Symbols.Search,
                title = "Search your music",
                subtitle = "Find songs by title, artist or album."
            )
            results.isEmpty() -> LibraryEmptyState(
                icon = Symbols.Search,
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
    if (currentTab == LibraryTab.SONGS) {
        LibrarySortMenu(
            expanded = sortSheetOpen,
            onDismiss = { sortSheetOpen = false },
            field = prefs.songSort,
            ascending = prefs.songAscending,
            onSelectField = { field ->
                // A new field starts in its natural order (newest, longest, A to Z).
                scope.launch {
                    AppPreferencesRepository.updateLibrarySongSort(context, field, field.naturalAscending)
                }
            },
            onSelectAscending = { asc ->
                scope.launch { AppPreferencesRepository.updateLibrarySongSort(context, prefs.songSort, asc) }
            }
        )
    } else {
        LibrarySortMenu(
            expanded = sortSheetOpen,
            onDismiss = { sortSheetOpen = false },
            field = null,
            ascending = prefs.groupAscending,
            onSelectField = {},
            onSelectAscending = { asc ->
                scope.launch { AppPreferencesRepository.updateLibraryGroupAscending(context, asc) }
            }
        )
    }
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
internal fun albumKeyOf(track: AudioTrackItem): String =
    if (track.albumId != 0L) "id:${track.albumId}" else "name:${track.album.lowercase()}"

/** Android share sheet with the audio file attached. */
internal fun shareTrack(context: Context, track: AudioTrackItem) {
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
 * Same header as Videos. Main pages: the official search app bar with one action beside it.
 * While selecting: a pinned top app bar with the count, Close and Select all. Inside an album,
 * artist or playlist: a pinned top app bar with Back.
 */
@Composable
private fun MusicHeader(
    background: Color,
    searchState: SearchBarState,
    textFieldState: TextFieldState,
    searchScrollBehavior: SearchBarScrollBehavior,
    selectionCount: Int,
    allSelected: Boolean,
    detailTitle: String?,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBack: () -> Unit,
    detailActions: @Composable () -> Unit,
    headerAction: @Composable () -> Unit
) {
    when {
        selectionCount > 0 -> TopAppBar(
            title = {
                Text(
                    text = "$selectionCount selected",
                    modifier = Modifier.testTag("selection_count")
                )
            },
            navigationIcon = {
                TooltipIconButton(
                    label = "Clear selection",
                    icon = Symbols.Close,
                    onClick = onClearSelection,
                    modifier = Modifier.testTag("selection_close_btn")
                )
            },
            actions = {
                TextButton(
                    onClick = onToggleSelectAll,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("selection_all_btn")
                ) { Text(if (allSelected) "Deselect all" else "Select all") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        )

        detailTitle != null -> TopAppBar(
            title = { Text(detailTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                TooltipIconButton(
                    label = "Back",
                    icon = Symbols.ArrowBack,
                    onClick = onBack,
                    modifier = Modifier.testTag("music_detail_back_btn")
                )
            },
            actions = { detailActions() },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
        )

        else -> AppBarWithSearch(
            state = searchState,
            inputField = { LibrarySearchInputField(searchState, textFieldState, placeholder = "Search music") },
            actions = { headerAction() },
            colors = SearchBarDefaults.appBarWithSearchColors(
                appBarContainerColor = background,
                scrolledAppBarContainerColor = background
            ),
            scrollBehavior = searchScrollBehavior,
            modifier = Modifier.testTag("music_search_app_bar")
        )
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
                            imageVector = Symbols.PlayArrow,
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
                    val shuffleContent: @Composable RowScope.() -> Unit = {
                        Icon(
                            imageVector = Symbols.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Shuffle")
                    }
                    val shuffleModifier = Modifier
                        .animateWidth(shuffleInteraction)
                        .testTag("library_shuffle_button")
                    if (onPlay == null) {
                        // Shuffle on its own is the main action, so it is the filled button.
                        Button(
                            onClick = onShuffle,
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            interactionSource = shuffleInteraction,
                            modifier = shuffleModifier,
                            content = shuffleContent
                        )
                    } else {
                        FilledTonalButton(
                            onClick = onShuffle,
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            interactionSource = shuffleInteraction,
                            modifier = shuffleModifier,
                            content = shuffleContent
                        )
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
 * First row of a library list (it scrolls away with the list): Shuffle or another [leading]
 * button on the left and the item count. With no button the count sits on the left.
 */
@Composable
private fun LibraryActionRow(
    label: String,
    onShuffle: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    // Lists sit 8 dp from the edge (rows bring their own padding); this lines the row up at 16 dp.
    horizontalPadding: Dp = 8.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = horizontalPadding)
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasButton = leading != null || onShuffle != null
        if (leading != null) {
            leading()
        } else if (onShuffle != null) {
            PlayShuffleButtons(onPlay = null, onShuffle = onShuffle)
        }
        if (hasButton) Spacer(modifier = Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .testTag("library_count_label")
        )
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
    val scheme = MaterialTheme.colorScheme
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
                placeholderBackground = scheme.surfaceContainerHighest,
                placeholderTint = scheme.primary,
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

/** Song list: plain Material 3 Expressive list rows with the official gap between them. */
@Composable
private fun SongsList(
    songs: List<AudioTrackItem>,
    playingAudio: AudioTrackItem?,
    isAudioPlaying: Boolean,
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
        contentPadding = libraryListPadding(bottomPadding),
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
                playing = playingAudio?.id == track.id,
                isAudioPlaying = isAudioPlaying,
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

/** Lists sit 8 dp from the screen edge; each row adds its own 8 dp inside. */
private fun libraryListPadding(bottom: Dp) = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = bottom)

/** Row colours shared by the library lists: no container, tinted only when selected. */
@Composable
private fun libraryRowColors(): ListItemColors {
    val scheme = MaterialTheme.colorScheme
    return ListItemDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = scheme.onSurface,
        supportingContentColor = scheme.onSurfaceVariant,
        selectedContainerColor = scheme.secondaryContainer,
        selectedContentColor = scheme.onSecondaryContainer,
        selectedSupportingContentColor = scheme.onSecondaryContainer
    )
}

@Composable
private fun libraryRowShapes() = ListItemDefaults.shapes(
    shape = MaterialTheme.shapes.large,
    selectedShape = MaterialTheme.shapes.large
)

private val LibraryRowPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 8.dp, bottom = 8.dp)

/**
 * One song: official Expressive list item, the same as a video row. The song that is loaded gets
 * a small "playing" badge on its art and its title in the theme colour. Selection works like
 * Videos: a check on the art, the row tinted, and the ⋮ slot kept so nothing moves.
 */
@Composable
private fun SongRow(
    track: AudioTrackItem,
    playing: Boolean,
    isAudioPlaying: Boolean,
    selectionMode: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onMore: () -> Unit,
    menu: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val showPlaying = playing && !selectionMode
    ListItem(
        selected = selectionMode && checked,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = if (onLongClick != null) "Select" else null,
        leadingContent = {
            SongArt(
                track = track,
                playing = showPlaying,
                animatePlaying = isAudioPlaying,
                selection = when {
                    !selectionMode -> ThumbSelection.NONE
                    checked -> ThumbSelection.SELECTED
                    else -> ThumbSelection.SELECTABLE
                }
            )
        },
        trailingContent = {
            // Same 48 dp slot while selecting, so titles never reflow.
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (!selectionMode) {
                    IconButton(
                        onClick = onMore,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.testTag("song_menu_button_${track.mediaId}")
                    ) {
                        Icon(
                            Symbols.MoreVert,
                            contentDescription = "More options for ${track.title}",
                            tint = scheme.onSurfaceVariant
                        )
                    }
                    menu()
                }
            }
        },
        supportingContent = {
            Text(text = track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        shapes = libraryRowShapes(),
        colors = libraryRowColors(),
        contentPadding = LibraryRowPadding,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_track_row_${track.mediaId}")
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (showPlaying) scheme.primary else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A song's art. The song that is loaded gets a round "playing" badge (it pulses gently while the
 * music plays). While selecting, a check sits on top and a selected art shrinks a little.
 */
@Composable
internal fun SongArt(
    track: AudioTrackItem,
    playing: Boolean,
    animatePlaying: Boolean,
    selection: ThumbSelection
) {
    val scheme = MaterialTheme.colorScheme
    val selected = selection == ThumbSelection.SELECTED
    val inset by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "songArtInset"
    )
    Box(Modifier.size(56.dp)) {
        AudioArt(
            uri = track.contentUri,
            placeholderBackground = scheme.surfaceContainerHighest,
            placeholderTint = scheme.primary,
            // The spring can overshoot a hair below 0; padding must never be negative.
            modifier = Modifier
                .fillMaxSize()
                .padding(inset.coerceAtLeast(0.dp)),
            shape = RoundedCornerShape(12.dp)
        )
        AnimatedVisibility(
            visible = playing,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
        ) {
            PlayingBadge(animate = animatePlaying)
        }
        AnimatedVisibility(
            visible = selection != ThumbSelection.NONE,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.6f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(2.dp)
        ) {
            SelectionCheck(selected = selected)
        }
    }
}

/** Small primary circle with an equaliser icon; it breathes while the song plays. */
@Composable
internal fun PlayingBadge(animate: Boolean) {
    val scheme = MaterialTheme.colorScheme
    // Only runs while playing; paused, the bars stand still.
    val pulse = if (animate) {
        rememberInfiniteTransition(label = "playingPulse").animateFloat(
            initialValue = 1f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
            label = "playingScale"
        )
    } else null
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(scheme.primary)
            .semantics { contentDescription = "Now playing" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Symbols.GraphicEq,
            contentDescription = null,
            tint = scheme.onPrimary,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer { scaleY = pulse?.value ?: 1f }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Albums / Artists
// ---------------------------------------------------------------------------------------------

@Composable
private fun AlbumsGrid(
    albums: List<AlbumGroup>,
    header: @Composable () -> Unit,
    onOpen: (AlbumGroup) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
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
        item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") { header() }
        items(albums, key = { it.key }) { album ->
            Card(
                onClick = { onOpen(album) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                AudioArt(
                    uri = album.tracks.first().contentUri,
                    placeholderBackground = scheme.surfaceContainerHighest,
                    placeholderTint = scheme.primary,
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
    header: @Composable () -> Unit,
    onOpen: (ArtistGroup) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
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
        contentPadding = libraryListPadding(LocalLibraryBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        item(key = "header", contentType = "header") { header() }
        items(artists, key = { it.key }) { artist ->
            ListItem(
                onClick = { onOpen(artist) },
                leadingContent = {
                    AudioArt(
                        uri = artist.tracks.first().contentUri,
                        placeholderBackground = scheme.surfaceContainerHighest,
                        placeholderTint = scheme.primary,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape
                    )
                },
                supportingContent = { Text(songCountLabel(artist.tracks.size)) },
                trailingContent = {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Symbols.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                    }
                },
                shapes = libraryRowShapes(),
                colors = libraryRowColors(),
                contentPadding = LibraryRowPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("artist_row_${artist.key}")
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
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

/** The user's playlists. */
@Composable
private fun PlaylistsList(
    playlists: List<AudioPlaylist>,
    header: @Composable () -> Unit,
    firstTrackOf: (AudioPlaylist) -> AudioTrackItem?,
    countOf: (AudioPlaylist) -> Int,
    onOpen: (AudioPlaylist) -> Unit,
    onMore: (AudioPlaylist) -> Unit,
    menu: @Composable (AudioPlaylist) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .libraryScrollbar(listState)
            .testTag("library_playlists_list"),
        contentPadding = libraryListPadding(LocalLibraryBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        item(key = "header", contentType = "header") { header() }
        items(playlists, key = { it.id }) { playlist ->
            val first = firstTrackOf(playlist)
            ListItem(
                onClick = { onOpen(playlist) },
                leadingContent = {
                    if (first != null) {
                        AudioArt(
                            uri = first.contentUri,
                            placeholderBackground = scheme.surfaceContainerHighest,
                            placeholderTint = scheme.primary,
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Symbols.PlaylistPlay, contentDescription = null, tint = scheme.onSecondaryContainer)
                        }
                    }
                },
                supportingContent = { Text(songCountLabel(countOf(playlist))) },
                trailingContent = {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { onMore(playlist) },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.testTag("playlist_menu_button_${playlist.id}")
                        ) {
                            Icon(
                                Symbols.MoreVert,
                                contentDescription = "Options for ${playlist.name}",
                                tint = scheme.onSurfaceVariant
                            )
                        }
                        menu(playlist)
                    }
                },
                shapes = libraryRowShapes(),
                colors = libraryRowColors(),
                contentPadding = LibraryRowPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_row_${playlist.id}")
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (playlists.isEmpty()) {
            item(key = "hint") {
                Text(
                    text = "Tap + above to make a playlist, or long-press songs to add several at once.",
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
