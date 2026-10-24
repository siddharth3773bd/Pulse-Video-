@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferencesRepository
import com.example.model.AudioTrackItem
import com.example.model.LibraryPreferences
import com.example.model.LibrarySongSort
import com.example.model.LibraryTab
import com.example.model.NowPlayingAction
import com.example.ui.theme.LocalM3Palette
import kotlinx.coroutines.launch

private enum class LibrarySheetType { SETTINGS, MANAGE, SORT, TRAY }

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
    val groups = tracks.groupBy { track ->
        if (track.albumId != 0L) "id:${track.albumId}" else "name:${track.album.lowercase()}"
    }.map { (key, list) ->
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

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

/**
 * Music library, built from official Material 3 components:
 * TopAppBar (title, search, settings) -> PrimaryScrollableTabRow -> one action row
 * (count, Shuffle, Sort) -> HorizontalPager of ListItem lists / Card grid on one flat background.
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
    extraBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs by remember(context) {
        AppPreferencesRepository.observeLibraryPreferences(context)
    }.collectAsState(initial = LibraryPreferences())

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
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<LibrarySheetType?>(null) }
    var infoTrack by remember { mutableStateOf<AudioTrackItem?>(null) }

    val sortedSongs = remember(tracks, prefs.songSort, prefs.songAscending) {
        sortSongs(tracks, prefs.songSort, prefs.songAscending)
    }
    val likedSongs = remember(sortedSongs, prefs.likedIds) {
        sortedSongs.filter { it.id in prefs.likedIds }
    }
    val albums = remember(tracks, prefs.groupAscending) { buildAlbums(tracks, prefs.groupAscending) }
    val artists = remember(tracks, prefs.groupAscending) { buildArtists(tracks, prefs.groupAscending) }

    fun detailTracksFor(tab: LibraryTab): List<AudioTrackItem>? {
        if (detailTabId != tab.id || detailKey == null) return null
        return when (tab) {
            LibraryTab.ALBUMS -> albums.firstOrNull { it.key == detailKey }?.tracks
            LibraryTab.ARTISTS -> artists.firstOrNull { it.key == detailKey }?.tracks
            else -> null
        }
    }

    fun detailTitleFor(tab: LibraryTab): String = when (tab) {
        LibraryTab.ALBUMS -> albums.firstOrNull { it.key == detailKey }?.name.orEmpty()
        LibraryTab.ARTISTS -> artists.firstOrNull { it.key == detailKey }?.name.orEmpty()
        else -> ""
    }

    val currentDetailTracks = detailTracksFor(currentTab)
    val inDetail = currentDetailTracks != null
    val closeDetail = {
        detailTabId = null
        detailKey = null
    }
    val closeSearch = {
        searchActive = false
        query = ""
    }

    BackHandler(enabled = searchActive) { closeSearch() }
    BackHandler(enabled = !searchActive && inDetail) { closeDetail() }

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

    CompositionLocalProvider(LocalLibraryBottomPadding provides (132.dp + extraBottomPadding)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(background)
                .testTag("audio_library_screen")
        ) {
            if (searchActive) {
                LibrarySearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = closeSearch
                )
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
                    else -> SongsList(
                        songs = results,
                        playingAudio = playingAudio,
                        background = background,
                        onPlay = playFromList,
                        onMore = { infoTrack = it }
                    )
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = if (inDetail) detailTitleFor(currentTab) else "Music",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (inDetail) {
                            IconButton(onClick = closeDetail) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { searchActive = true },
                            modifier = Modifier.testTag("library_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(
                            onClick = { sheet = LibrarySheetType.SETTINGS },
                            modifier = Modifier.testTag("library_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Library settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = background,
                        scrolledContainerColor = background
                    )
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
                                onClick = { selectTab(tab) },
                                text = { Text(text = tab.label, maxLines = 1) }
                            )
                        }
                    }
                }

                // One slim action row: count on the left, Shuffle and Sort on the right.
                val detail = currentDetailTracks
                when {
                    detail != null -> LibraryActionRow(
                        label = songCountLabel(detail.size),
                        onShuffle = { shuffle(detail) }.takeIf { detail.isNotEmpty() },
                        onSort = null
                    )
                    currentTab == LibraryTab.SONGS -> LibraryActionRow(
                        label = songCountLabel(sortedSongs.size),
                        onShuffle = { shuffle(sortedSongs) }.takeIf { sortedSongs.isNotEmpty() },
                        onSort = { sheet = LibrarySheetType.SORT }
                    )
                    currentTab == LibraryTab.LIKED -> LibraryActionRow(
                        label = songCountLabel(likedSongs.size),
                        onShuffle = { shuffle(likedSongs) }.takeIf { likedSongs.isNotEmpty() },
                        onSort = null
                    )
                    currentTab == LibraryTab.ALBUMS -> LibraryActionRow(
                        label = albumCountLabel(albums.size),
                        onShuffle = null,
                        onSort = { sheet = LibrarySheetType.SORT }
                    )
                    currentTab == LibraryTab.ARTISTS -> LibraryActionRow(
                        label = artistCountLabel(artists.size),
                        onShuffle = null,
                        onSort = { sheet = LibrarySheetType.SORT }
                    )
                    else -> Unit
                }

                HorizontalPager(
                    state = pagerState,
                    key = { index -> visibleTabs.getOrNull(index)?.id ?: index.toString() },
                    userScrollEnabled = !inDetail,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val tab = visibleTabs.getOrNull(page)
                    if (tab != null) {
                        val pageDetail = detailTracksFor(tab)
                        when {
                            pageDetail != null -> SongsList(
                                songs = pageDetail,
                                playingAudio = playingAudio,
                                background = background,
                                onPlay = playFromList,
                                onMore = { infoTrack = it }
                            )
                            tab == LibraryTab.SONGS -> {
                                if (sortedSongs.isEmpty()) {
                                    LibraryEmptyState(
                                        icon = Icons.Default.MusicNote,
                                        title = "No audio files found",
                                        subtitle = "Music files stored on your device will appear here."
                                    )
                                } else {
                                    SongsList(
                                        songs = sortedSongs,
                                        playingAudio = playingAudio,
                                        background = background,
                                        onPlay = playFromList,
                                        onMore = { infoTrack = it }
                                    )
                                }
                            }
                            tab == LibraryTab.LIKED -> {
                                if (likedSongs.isEmpty()) {
                                    LibraryEmptyState(
                                        icon = tab.icon,
                                        title = "No liked songs yet",
                                        subtitle = "Tap the heart on the Now Playing screen to like a song."
                                    )
                                } else {
                                    SongsList(
                                        songs = likedSongs,
                                        playingAudio = playingAudio,
                                        background = background,
                                        onPlay = playFromList,
                                        onMore = { infoTrack = it }
                                    )
                                }
                            }
                            tab == LibraryTab.ALBUMS -> AlbumsGrid(
                                albums = albums,
                                onOpen = { album ->
                                    detailTabId = LibraryTab.ALBUMS.id
                                    detailKey = album.key
                                }
                            )
                            tab == LibraryTab.ARTISTS -> ArtistsList(
                                artists = artists,
                                background = background,
                                onOpen = { artist ->
                                    detailTabId = LibraryTab.ARTISTS.id
                                    detailKey = artist.key
                                }
                            )
                            else -> LibraryEmptyState(
                                icon = tab.icon,
                                title = "Coming soon",
                                subtitle = "${tab.label} will be available in a future update."
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Sheets -------------------------------------------------------------------------
    val saveTabs: (List<LibraryTab>, Set<LibraryTab>) -> Unit = { order, hidden ->
        scope.launch { AppPreferencesRepository.updateLibraryTabs(context, order, hidden) }
    }

    when (sheet) {
        LibrarySheetType.SETTINGS -> LibrarySettingsSheet(
            onOpenManageTabs = { sheet = LibrarySheetType.MANAGE },
            onOpenTray = { sheet = LibrarySheetType.TRAY },
            onDismiss = { sheet = null }
        )
        LibrarySheetType.TRAY -> NowPlayingTraySheet(
            order = prefs.trayOrder,
            hidden = prefs.hiddenTray,
            onChange = { order, hidden ->
                scope.launch { AppPreferencesRepository.updateLibraryTray(context, order, hidden) }
            },
            onReset = {
                scope.launch {
                    AppPreferencesRepository.updateLibraryTray(context, NowPlayingAction.entries, emptySet())
                }
            },
            onDismiss = { sheet = null }
        )
        LibrarySheetType.MANAGE -> ManageTabsSheet(
            prefs = prefs,
            onChange = saveTabs,
            onReset = { saveTabs(LibraryTab.entries, emptySet()) },
            onDismiss = { sheet = null }
        )
        LibrarySheetType.SORT -> {
            val isSongs = currentTab == LibraryTab.SONGS
            LibrarySortSheet(
                showFields = isSongs,
                field = prefs.songSort,
                ascending = if (isSongs) prefs.songAscending else prefs.groupAscending,
                onSelectField = { field ->
                    scope.launch {
                        AppPreferencesRepository.updateLibrarySongSort(context, field, prefs.songAscending)
                    }
                },
                onToggleOrder = {
                    scope.launch {
                        if (isSongs) {
                            AppPreferencesRepository.updateLibrarySongSort(
                                context,
                                prefs.songSort,
                                !prefs.songAscending
                            )
                        } else {
                            AppPreferencesRepository.updateLibraryGroupAscending(context, !prefs.groupAscending)
                        }
                    }
                },
                onDismiss = { sheet = null }
            )
        }
        null -> Unit
    }

    infoTrack?.let { track ->
        SongInfoSheet(track = track, onDismiss = { infoTrack = null })
    }
}

// ---------------------------------------------------------------------------------------------
// Search + action row
// ---------------------------------------------------------------------------------------------

/** Official Material 3 search input field, shown in place of the top app bar while searching. */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {},
        expanded = true,
        onExpandedChange = {},
        placeholder = { Text("Search songs, artists, albums") },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("library_search_field")
    )
}

@Composable
private fun LibraryActionRow(
    label: String,
    onShuffle: (() -> Unit)?,
    onSort: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (onShuffle != null) {
            FilledTonalButton(
                onClick = onShuffle,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.testTag("library_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Shuffle")
            }
        }
        if (onSort != null) {
            IconButton(
                onClick = onSort,
                modifier = Modifier.testTag("library_sort_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Songs
// ---------------------------------------------------------------------------------------------

@Composable
private fun SongsList(
    songs: List<AudioTrackItem>,
    playingAudio: AudioTrackItem?,
    background: Color,
    onPlay: (List<AudioTrackItem>, Int) -> Unit,
    onMore: (AudioTrackItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_library_view"),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 4.dp,
            bottom = LocalLibraryBottomPadding.current
        )
    ) {
        itemsIndexed(songs, key = { _, t -> t.id }) { index, track ->
            SongRow(
                track = track,
                selected = playingAudio?.id == track.id,
                background = background,
                onClick = { onPlay(songs, index) },
                onMore = { onMore(track) }
            )
        }
    }
}

/** Official Material 3 Expressive list item; the current song uses the list item's selected state. */
@Composable
private fun SongRow(
    track: AudioTrackItem,
    selected: Boolean,
    background: Color,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val palette = LocalM3Palette.current
    ListItem(
        selected = selected,
        onClick = onClick,
        leadingContent = {
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHighest,
                placeholderTint = palette.primary,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp)
            )
        },
        supportingContent = {
            Text(
                text = track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = "Song info")
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = background,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.testTag("audio_track_row_${track.mediaId}")
    ) {
        Text(
            text = track.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
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
    background: Color,
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_artists_list"),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 4.dp,
            bottom = LocalLibraryBottomPadding.current
        )
    ) {
        items(artists, key = { it.key }) { artist ->
            ListItem(
                onClick = { onOpen(artist) },
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
                colors = ListItemDefaults.colors(
                    containerColor = background,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
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
