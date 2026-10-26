@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MeSubPage
import com.example.model.PlaybackSettings
import com.example.model.ThemeMode
import com.example.model.VideoItem
import com.example.ui.icons.Symbols
import com.example.viewmodel.VideoUiState

/**
 * The Me tab: your video playlists and watch history, and the app's settings.
 *
 * Settings pages open on top of this home page. Android's predictive back works on them: while the
 * back gesture is held the page shrinks and follows the finger with this page visible behind it,
 * and letting go closes it.
 */
@Composable
fun MeTabScreen(
    uiState: VideoUiState,
    onOpenSubPage: (MeSubPage) -> Unit,
    onCloseSubPage: () -> Unit,
    onOpenTabsSheet: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveFromHistory: (videoId: String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (playlistId: String, newName: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onAddVideosToPlaylist: (playlistId: String, videoIds: List<String>) -> Unit,
    onRemoveVideoFromPlaylist: (playlistId: String, videoId: String) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSelectColorTheme: (String) -> Unit,
    onSelectCustomColor: (Int) -> Unit,
    onToggleAmoledBlack: () -> Unit,
    onUpdatePlaybackSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit,
    onLibraryChanged: () -> Unit,
    modifier: Modifier = Modifier,
    /** History thumbnails take part in the video player's open/close animation. */
    videoTransition: VideoTransitionScope? = null
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val page = uiState.meSubPage

    val videoById = remember(uiState.videos) { uiState.videos.associateBy { it.id } }
    val historyPairs = remember(uiState.historyEntries, videoById) {
        uiState.historyEntries.mapNotNull { entry -> videoById[entry.mediaUri]?.let { entry to it } }
    }

    val back = rememberPredictiveBack(enabled = page != MeSubPage.NONE, onBack = onCloseSubPage)
    // A page that opens starts from full size (the last back gesture may have left it shrunk).
    LaunchedEffect(page) { if (page != MeSubPage.NONE) back.reset() }

    Box(modifier = modifier.fillMaxSize()) {
        MeHome(
            playlistCount = uiState.playlists.size,
            historyCount = historyPairs.size,
            onOpenSubPage = onOpenSubPage,
            onOpenTabsSheet = onOpenTabsSheet
        )

        // The open page sits on top of the home page (which shows behind it during the back
        // gesture). Opening: fades in while growing a little; closing: fades out while shrinking.
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState != MeSubPage.NONE) {
                    ContentTransform(
                        targetContentEnter = fadeIn(motion.defaultEffectsSpec()) +
                            scaleIn(motion.defaultSpatialSpec(), initialScale = 0.92f),
                        initialContentExit = ExitTransition.None
                    )
                } else {
                    ContentTransform(
                        targetContentEnter = EnterTransition.None,
                        // Quick, no bounce: the page is gone before the next tap.
                        initialContentExit = fadeOut(motion.fastEffectsSpec()) +
                            scaleOut(motion.fastEffectsSpec(), targetScale = 0.92f)
                    )
                }
            },
            label = "meSubPage",
            modifier = Modifier.fillMaxSize()
        ) { shown ->
            if (shown != MeSubPage.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .predictiveBackPreview(back)
                        .background(scheme.background)
                        // The page is opaque to touches too: nothing reaches the home page behind.
                        .clickable(interactionSource = null, indication = null) {}
                ) {
                    when (shown) {
                        MeSubPage.PLAYLISTS -> VideoPlaylistsPage(
                            playlists = uiState.playlists,
                            allVideos = uiState.videos,
                            videoById = videoById,
                            onBack = onCloseSubPage,
                            onPlayVideo = onPlayVideo,
                            onCreatePlaylist = onCreatePlaylist,
                            onRenamePlaylist = onRenamePlaylist,
                            onDeletePlaylist = onDeletePlaylist,
                            onAddVideosToPlaylist = onAddVideosToPlaylist,
                            onRemoveVideoFromPlaylist = onRemoveVideoFromPlaylist
                        )
                        MeSubPage.HISTORY -> CompositionLocalProvider(LocalVideoTransition provides videoTransition) {
                            WatchHistoryPage(
                                historyPairs = historyPairs,
                                onBack = onCloseSubPage,
                                onPlayVideo = onPlayVideo,
                                onClearHistory = onClearHistory,
                                onRemove = onRemoveFromHistory
                            )
                        }
                        MeSubPage.APPEARANCE -> AppearancePage(
                            uiState = uiState,
                            onBack = onCloseSubPage,
                            onSelectThemeMode = onSelectThemeMode,
                            onSetDynamicColor = onSetDynamicColor,
                            onSelectColorTheme = onSelectColorTheme,
                            onSelectCustomColor = onSelectCustomColor,
                            onToggleAmoledBlack = onToggleAmoledBlack
                        )
                        MeSubPage.VIDEO_SETTINGS -> VideoSettingsPage(
                            settings = uiState.playbackSettings,
                            onBack = onCloseSubPage,
                            onUpdateSettings = onUpdatePlaybackSettings
                        )
                        MeSubPage.MUSIC_SETTINGS -> MusicSettingsPage(
                            audioTracks = uiState.audioTracks,
                            onBack = onCloseSubPage
                        )
                        MeSubPage.BIN -> BinPage(
                            onBack = onCloseSubPage,
                            onLibraryChanged = onLibraryChanged
                        )
                        MeSubPage.NONE -> Unit
                    }
                }
            }
        }
    }
}

/**
 * Home of the Me tab: two shortcuts (playlists, history), then the settings. Sideways the
 * shortcuts and the settings sit side by side, so everything fits on one screen.
 */
@Composable
private fun MeHome(
    playlistCount: Int,
    historyCount: Int,
    onOpenSubPage: (MeSubPage) -> Unit,
    onOpenTabsSheet: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val twoPane = currentWindowLayout().twoPane
    val playlistsCard: @Composable (Modifier, Boolean) -> Unit = { modifier, compact ->
        MeShortcutCard(
            icon = Symbols.PlaylistPlay,
            title = "Playlists",
            subtitle = if (playlistCount == 1) "1 video playlist" else "$playlistCount video playlists",
            onClick = { onOpenSubPage(MeSubPage.PLAYLISTS) },
            compact = compact,
            modifier = modifier.testTag("me_card_video_playlist")
        )
    }
    val historyCard: @Composable (Modifier, Boolean) -> Unit = { modifier, compact ->
        MeShortcutCard(
            icon = Symbols.History,
            title = "History",
            subtitle = if (historyCount == 1) "1 video watched" else "$historyCount videos watched",
            onClick = { onOpenSubPage(MeSubPage.HISTORY) },
            compact = compact,
            modifier = modifier.testTag("me_card_video_history")
        )
    }
    val settingsRows = listOf(
        SettingRow.Link(
            key = "appearance",
            icon = Symbols.Palette,
            title = "Appearance",
            summary = "Theme and colours",
            badge = true,
            onClick = { onOpenSubPage(MeSubPage.APPEARANCE) }
        ),
        SettingRow.Link(
            key = "video",
            icon = Symbols.VideoSettings,
            title = "Video",
            summary = "Gestures, playback and languages",
            badge = true,
            onClick = { onOpenSubPage(MeSubPage.VIDEO_SETTINGS) }
        ),
        SettingRow.Link(
            key = "music",
            icon = Symbols.LibraryMusic,
            title = "Music",
            summary = "Library, Now Playing and hidden songs",
            badge = true,
            onClick = { onOpenSubPage(MeSubPage.MUSIC_SETTINGS) }
        ),
        SettingRow.Link(
            key = "bin",
            icon = Symbols.Delete,
            title = "Bin",
            summary = "Deleted videos and songs, kept for 30 days",
            badge = true,
            onClick = { onOpenSubPage(MeSubPage.BIN) }
        ),
        SettingRow.Link(
            key = "navigation",
            icon = Symbols.BottomNavigation,
            title = "Navigation bar",
            summary = "Choose which tabs show",
            badge = true,
            onClick = onOpenTabsSheet
        )
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_tab_home_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    "Me",
                    style = if (twoPane) MaterialTheme.typography.headlineMediumEmphasized
                    else MaterialTheme.typography.headlineLargeEmphasized,
                    maxLines = 1
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
        )
        if (twoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    playlistsCard(Modifier.fillMaxWidth(), true)
                    historyCard(Modifier.fillMaxWidth(), true)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    settingsGroup(key = "settings", title = null, rows = settingsRows)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            ) {
                item(key = "shortcuts") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        playlistsCard(Modifier.weight(1f), false)
                        historyCard(Modifier.weight(1f), false)
                    }
                }
                settingsGroup(key = "settings", title = "Settings", rows = settingsRows)
            }
        }
    }
}

/**
 * A shortcut card on the Me home page: badge above the text, or ([compact], sideways) beside it.
 */
@Composable
private fun MeShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        val text: @Composable () -> Unit = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (compact) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = icon, size = 48.dp)
                Spacer(modifier = Modifier.width(16.dp))
                text()
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                IconBadge(icon = icon, size = 48.dp)
                Spacer(modifier = Modifier.height(16.dp))
                text()
            }
        }
    }
}
