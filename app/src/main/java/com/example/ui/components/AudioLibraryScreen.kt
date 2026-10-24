@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferencesRepository
import com.example.model.AudioTrackItem
import com.example.model.LibraryNavMode
import com.example.model.LibraryPreferences
import com.example.model.LibrarySongSort
import com.example.model.LibraryTab
import com.example.model.NowPlayingAction
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.launch

private enum class LibrarySheetType { SETTINGS, NAV_MODE, MANAGE, JUMP, SORT, TRAY }

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

private data class FolderGroup(
    val key: String,
    val name: String,
    val subtitle: String,
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

private fun buildFolders(tracks: List<AudioTrackItem>, ascending: Boolean): List<FolderGroup> {
    val groups = tracks.groupBy { track ->
        track.path.substringBeforeLast('/', "").ifBlank { track.folder }
    }.map { (dir, list) ->
        val sorted = list.sortedBy { it.title.lowercase() }
        val name = dir.substringAfterLast('/').ifBlank { sorted.first().folder }
        FolderGroup(key = dir, name = name, subtitle = dir, tracks = sorted)
    }.sortedBy { it.name.lowercase() }
    return if (ascending) groups else groups.asReversed()
}

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
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
    val palette = LocalM3Palette.current
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

    val currentTab = visibleTabs.getOrNull(pagerState.currentPage) ?: visibleTabs.first()

    val selectTab: (LibraryTab) -> Unit = { tab ->
        val idx = visibleTabs.indexOf(tab)
        if (idx >= 0) {
            selectedTabId = tab.id
            scope.launch { pagerState.animateScrollToPage(idx) }
        }
    }

    var detailTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<LibrarySheetType?>(null) }
    var infoTrack by remember { mutableStateOf<AudioTrackItem?>(null) }

    val sortedSongs = remember(tracks, prefs.songSort, prefs.songAscending) {
        sortSongs(tracks, prefs.songSort, prefs.songAscending)
    }
    val albums = remember(tracks, prefs.groupAscending) { buildAlbums(tracks, prefs.groupAscending) }
    val artists = remember(tracks, prefs.groupAscending) { buildArtists(tracks, prefs.groupAscending) }
    val folders = remember(tracks, prefs.groupAscending) { buildFolders(tracks, prefs.groupAscending) }

    fun detailTracksFor(tab: LibraryTab): List<AudioTrackItem>? {
        if (detailTabId != tab.id || detailKey == null) return null
        return when (tab) {
            LibraryTab.ALBUMS -> albums.firstOrNull { it.key == detailKey }?.tracks
            LibraryTab.ARTISTS -> artists.firstOrNull { it.key == detailKey }?.tracks
            LibraryTab.FOLDERS -> folders.firstOrNull { it.key == detailKey }?.tracks
            else -> null
        }
    }

    fun detailTitleFor(tab: LibraryTab): String = when (tab) {
        LibraryTab.ALBUMS -> albums.firstOrNull { it.key == detailKey }?.name.orEmpty()
        LibraryTab.ARTISTS -> artists.firstOrNull { it.key == detailKey }?.name.orEmpty()
        LibraryTab.FOLDERS -> folders.firstOrNull { it.key == detailKey }?.name.orEmpty()
        else -> ""
    }

    val currentDetailTracks = detailTracksFor(currentTab)

    BackHandler(enabled = detailKey != null && currentTab.id == detailTabId) {
        detailTabId = null
        detailKey = null
    }

    val containerColor = if (amoledBlack) palette.surfaceContainerLowest else palette.surfaceContainerLow
    val rowColor = if (amoledBlack) palette.surfaceContainer else palette.surfaceContainerHigh

    val showToolbar = currentTab == LibraryTab.SONGS ||
        currentTab == LibraryTab.ALBUMS ||
        currentTab == LibraryTab.ARTISTS ||
        currentTab == LibraryTab.FOLDERS
    val showSort = currentDetailTracks == null && showToolbar

    val playFromList: (List<AudioTrackItem>, Int) -> Unit = { list, index ->
        val target = list.getOrNull(index)
        if (target != null) {
            if (playingAudio?.id == target.id) onTogglePlayPause() else onPlayQueue(list, index, false)
        }
    }

    CompositionLocalProvider(LocalLibraryBottomPadding provides (132.dp + extraBottomPadding)) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(headerBackground)
            .testTag("audio_library_screen")
    ) {
        LibraryHeader(
            navMode = prefs.navMode,
            visibleTabs = visibleTabs,
            currentTab = currentTab,
            currentPage = pagerState.currentPage,
            onSelectTab = selectTab,
            onOpenSettings = { sheet = LibrarySheetType.SETTINGS },
            onOpenManage = { sheet = LibrarySheetType.MANAGE },
            onOpenJump = { sheet = LibrarySheetType.JUMP }
        )

        Surface(
            color = containerColor,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showToolbar) {
                    LibraryToolbar(
                        showSort = showSort,
                        onShuffle = {
                            val pool = currentDetailTracks ?: sortedSongs
                            if (pool.isNotEmpty()) onPlayQueue(pool, pool.indices.random(), true)
                        },
                        onSort = { sheet = LibrarySheetType.SORT }
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    key = { index -> visibleTabs.getOrNull(index)?.id ?: index.toString() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val tab = visibleTabs.getOrNull(page)
                    if (tab != null) {
                        val detail = detailTracksFor(tab)
                        when {
                            detail != null -> {
                                LibraryDetailPage(
                                    title = detailTitleFor(tab),
                                    tracks = detail,
                                    playingAudio = playingAudio,
                                    isAudioPlaying = isAudioPlaying,
                                    rowColor = rowColor,
                                    onBack = {
                                        detailTabId = null
                                        detailKey = null
                                    },
                                    onPlay = playFromList,
                                    onMore = { infoTrack = it }
                                )
                            }
                            tab == LibraryTab.SONGS -> {
                                SongsList(
                                    songs = sortedSongs,
                                    playingAudio = playingAudio,
                                    isAudioPlaying = isAudioPlaying,
                                    audioProgress = audioProgress,
                                    showNowPlayingCard = true,
                                    rowColor = rowColor,
                                    cardColor = palette.surfaceContainerHigh,
                                    onPlay = playFromList,
                                    onMore = { infoTrack = it },
                                    onTogglePlayPause = onTogglePlayPause,
                                    onSeekAudio = onSeekAudio,
                                    onSkipPrevious = onSkipPrevious,
                                    onSkipNext = onSkipNext
                                )
                            }
                            tab == LibraryTab.LIKED -> {
                                val likedSongs = remember(sortedSongs, prefs.likedIds) {
                                    sortedSongs.filter { it.id in prefs.likedIds }
                                }
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
                                        isAudioPlaying = isAudioPlaying,
                                        audioProgress = audioProgress,
                                        showNowPlayingCard = false,
                                        rowColor = rowColor,
                                        cardColor = palette.surfaceContainerHigh,
                                        onPlay = playFromList,
                                        onMore = { infoTrack = it },
                                        onTogglePlayPause = onTogglePlayPause,
                                        onSeekAudio = onSeekAudio,
                                        onSkipPrevious = onSkipPrevious,
                                        onSkipNext = onSkipNext
                                    )
                                }
                            }
                            tab == LibraryTab.ALBUMS -> {
                                AlbumsGrid(
                                    albums = albums,
                                    rowColor = rowColor,
                                    onOpen = { album ->
                                        detailTabId = LibraryTab.ALBUMS.id
                                        detailKey = album.key
                                    }
                                )
                            }
                            tab == LibraryTab.ARTISTS -> {
                                ArtistsList(
                                    artists = artists,
                                    rowColor = rowColor,
                                    onOpen = { artist ->
                                        detailTabId = LibraryTab.ARTISTS.id
                                        detailKey = artist.key
                                    }
                                )
                            }
                            tab == LibraryTab.FOLDERS -> {
                                FoldersList(
                                    folders = folders,
                                    rowColor = rowColor,
                                    onOpen = { folder ->
                                        detailTabId = LibraryTab.FOLDERS.id
                                        detailKey = folder.key
                                    }
                                )
                            }
                            else -> {
                                LibraryEmptyState(
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
    }
    }

    // ---- Sheets -------------------------------------------------------------------------
    val saveTabs: (List<LibraryTab>, Set<LibraryTab>) -> Unit = { order, hidden ->
        scope.launch { AppPreferencesRepository.updateLibraryTabs(context, order, hidden) }
    }

    when (sheet) {
        LibrarySheetType.SETTINGS -> LibrarySettingsSheet(
            navMode = prefs.navMode,
            onOpenNavigation = { sheet = LibrarySheetType.NAV_MODE },
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
        LibrarySheetType.NAV_MODE -> LibraryNavigationSheet(
            selected = prefs.navMode,
            onSelect = { mode ->
                scope.launch { AppPreferencesRepository.updateLibraryNavMode(context, mode) }
                sheet = null
            },
            onDismiss = { sheet = null }
        )
        LibrarySheetType.MANAGE -> ManageTabsSheet(
            prefs = prefs,
            onChange = saveTabs,
            onReset = { saveTabs(LibraryTab.entries, emptySet()) },
            onDismiss = { sheet = null }
        )
        LibrarySheetType.JUMP -> LibraryJumpSheet(
            tabs = visibleTabs,
            current = currentTab,
            onSelect = { tab ->
                selectTab(tab)
                sheet = null
            },
            onReorder = { sheet = LibrarySheetType.MANAGE },
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
// Header + navigation
// ---------------------------------------------------------------------------------------------

@Composable
private fun LibraryHeader(
    navMode: LibraryNavMode,
    visibleTabs: List<LibraryTab>,
    currentTab: LibraryTab,
    currentPage: Int,
    onSelectTab: (LibraryTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManage: () -> Unit,
    onOpenJump: () -> Unit
) {
    val palette = LocalM3Palette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp)
    ) {
        if (navMode == LibraryNavMode.TAB_ROW) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = palette.onSurface,
                    style = MaterialTheme.typography.headlineLargeEmphasized,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                LibraryGearButton(onClick = onOpenSettings)
            }

            val listState = rememberLazyListState()
            LaunchedEffect(currentPage) {
                listState.animateScrollToItem(currentPage.coerceAtLeast(0))
            }
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)
            ) {
                items(visibleTabs, key = { it.id }) { tab ->
                    LibraryTabChip(
                        label = tab.label,
                        selected = tab == currentTab,
                        onClick = { onSelectTab(tab) }
                    )
                }
                item(key = "edit_chip") {
                    Surface(
                        onClick = onOpenManage,
                        shape = RoundedCornerShape(50),
                        color = palette.surfaceContainerHigh
                    ) {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Manage tabs",
                                tint = palette.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = currentTab.icon,
                            contentDescription = null,
                            tint = palette.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = currentTab.label,
                            color = palette.onPrimaryContainer,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    onClick = onOpenJump,
                    shape = RoundedCornerShape(50),
                    color = palette.primaryContainer
                ) {
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .width(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Jump to tab",
                            tint = palette.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                LibraryGearButton(onClick = onOpenSettings)
            }

            PagerDots(
                count = visibleTabs.size,
                current = currentPage,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun LibraryGearButton(onClick: () -> Unit) {
    val palette = LocalM3Palette.current
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = palette.secondaryContainer,
            contentColor = palette.onSecondaryContainer
        ),
        modifier = Modifier
            .size(48.dp)
            .testTag("library_settings_button")
    ) {
        Icon(imageVector = Icons.Default.Settings, contentDescription = "Library settings")
    }
}

@Composable
private fun LibraryTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalM3Palette.current
    val bg by animateColorAsState(
        targetValue = if (selected) palette.primaryContainer else palette.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "chipBg"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Box(
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.uppercase(),
                color = if (selected) palette.onPrimaryContainer else palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val palette = LocalM3Palette.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isCurrent = index == current
            val width by animateDpAsState(
                targetValue = if (isCurrent) 30.dp else 12.dp,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isCurrent) palette.primary else palette.primary.copy(alpha = 0.35f)
                    )
            )
        }
    }
}

@Composable
private fun LibraryToolbar(
    showSort: Boolean,
    onShuffle: () -> Unit,
    onSort: () -> Unit
) {
    val palette = LocalM3Palette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onShuffle,
            shape = RoundedCornerShape(50),
            color = palette.secondaryContainer,
            modifier = Modifier.testTag("library_shuffle_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = palette.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Shuffle",
                    color = palette.onSecondaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showSort) {
            Surface(
                onClick = onSort,
                shape = RoundedCornerShape(18.dp),
                color = palette.primaryContainer,
                modifier = Modifier.testTag("library_sort_button")
            ) {
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .width(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = palette.onPrimaryContainer
                    )
                }
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
    isAudioPlaying: Boolean,
    audioProgress: Float,
    showNowPlayingCard: Boolean,
    rowColor: Color,
    cardColor: Color,
    onPlay: (List<AudioTrackItem>, Int) -> Unit,
    onMore: (AudioTrackItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    if (songs.isEmpty()) {
        LibraryEmptyState(
            icon = Icons.Default.MusicNote,
            title = "No audio files found",
            subtitle = "Music files stored on your device will appear here."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_library_view"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = LocalLibraryBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(songs, key = { _, t -> t.id }) { index, track ->
            val selected = playingAudio?.id == track.id
            SongRow(
                track = track,
                selected = selected,
                playing = selected && isAudioPlaying,
                rowColor = rowColor,
                onClick = { onPlay(songs, index) },
                onMore = { onMore(track) }
            )
        }
    }
}

@Composable
private fun SongRow(
    track: AudioTrackItem,
    selected: Boolean,
    playing: Boolean,
    rowColor: Color,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val palette = LocalM3Palette.current
    val cardBg by animateColorAsState(
        targetValue = if (selected) palette.primaryContainer else rowColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "songRowBg"
    )
    // The selected (current) song turns into a rounder "pill" with a circular cover.
    val rowCorner by animateDpAsState(
        targetValue = if (selected) 40.dp else 20.dp,
        animationSpec = tween(260),
        label = "songRowCorner"
    )
    val artCorner by animateDpAsState(
        targetValue = if (selected) 25.dp else 12.dp,
        animationSpec = tween(260),
        label = "songArtCorner"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(rowCorner),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_track_row_${track.mediaId}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioArt(
                uri = track.contentUri,
                placeholderBackground = palette.surfaceContainerHighest,
                placeholderTint = palette.primary,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(artCorner)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (selected) palette.primary else palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = if (selected) palette.primary.copy(alpha = 0.8f) else palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                EqualizerIndicator(isPlaying = playing, color = palette.primary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            FilledIconButton(
                onClick = onMore,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (selected) palette.primary else palette.surfaceContainerHighest,
                    contentColor = if (selected) palette.onPrimary else palette.onSurface
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Song info")
            }
        }
    }
}

@Composable
private fun EqualizerIndicator(isPlaying: Boolean, color: Color) {
    if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "equalizer")
        val bar1 = transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar1"
        )
        val bar2 = transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar2"
        )
        val bar3 = transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar3"
        )
        Row(
            modifier = Modifier
                .height(18.dp)
                .width(18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(bar1, bar2, bar3).forEach { bar ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(bar.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.height(18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Albums / Artists / Folders
// ---------------------------------------------------------------------------------------------

@Composable
private fun AlbumsGrid(
    albums: List<AlbumGroup>,
    rowColor: Color,
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
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = LocalLibraryBottomPadding.current),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(albums, key = { it.key }) { album ->
            Surface(
                onClick = { onOpen(album) },
                shape = RoundedCornerShape(24.dp),
                color = rowColor
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    AudioArt(
                        uri = album.tracks.first().contentUri,
                        placeholderBackground = palette.surfaceContainerHighest,
                        placeholderTint = palette.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(16.dp),
                        iconSize = 40.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = album.name,
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        color = palette.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
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
    rowColor: Color,
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
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = LocalLibraryBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(artists, key = { it.key }) { artist ->
            Surface(
                onClick = { onOpen(artist) },
                shape = RoundedCornerShape(20.dp),
                color = rowColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioArt(
                        uri = artist.tracks.first().contentUri,
                        placeholderBackground = palette.surfaceContainerHighest,
                        placeholderTint = palette.primary,
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = songCountLabel(artist.tracks.size),
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersList(
    folders: List<FolderGroup>,
    rowColor: Color,
    onOpen: (FolderGroup) -> Unit
) {
    val palette = LocalM3Palette.current
    if (folders.isEmpty()) {
        LibraryEmptyState(
            icon = LibraryTab.FOLDERS.icon,
            title = "No folders found",
            subtitle = "Folders that contain music will appear here."
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_folders_list"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = LocalLibraryBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(folders, key = { it.key }) { folder ->
            Surface(
                onClick = { onOpen(folder) },
                shape = RoundedCornerShape(20.dp),
                color = rowColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(palette.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = palette.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = songCountLabel(folder.tracks.size),
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDetailPage(
    title: String,
    tracks: List<AudioTrackItem>,
    playingAudio: AudioTrackItem?,
    isAudioPlaying: Boolean,
    rowColor: Color,
    onBack: () -> Unit,
    onPlay: (List<AudioTrackItem>, Int) -> Unit,
    onMore: (AudioTrackItem) -> Unit
) {
    val palette = LocalM3Palette.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = songCountLabel(tracks.size),
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp
                )
            }
        }
        SongsList(
            songs = tracks,
            playingAudio = playingAudio,
            isAudioPlaying = isAudioPlaying,
            audioProgress = 0f,
            showNowPlayingCard = false,
            rowColor = rowColor,
            cardColor = rowColor,
            onPlay = onPlay,
            onMore = onMore,
            onTogglePlayPause = {},
            onSeekAudio = {},
            onSkipPrevious = {},
            onSkipNext = {}
        )
    }
}

private fun songCountLabel(count: Int): String = if (count == 1) "1 song" else "$count songs"

@Composable
private fun LibraryEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalM3Palette.current
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
                .background(palette.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = palette.onSurface,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = palette.onSurfaceVariant,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
