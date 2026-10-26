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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier
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
                        initialContentExit = fadeOut(motion.defaultEffectsSpec()) +
                            scaleOut(motion.fastSpatialSpec(), targetScale = 0.92f)
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
                        MeSubPage.HISTORY -> WatchHistoryPage(
                            historyPairs = historyPairs,
                            onBack = onCloseSubPage,
                            onPlayVideo = onPlayVideo,
                            onClearHistory = onClearHistory,
                            onRemove = onRemoveFromHistory
                        )
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
                        MeSubPage.NONE -> Unit
                    }
                }
            }
        }
    }
}

/** Home of the Me tab: two shortcuts (playlists, history), then the settings. */
@Composable
private fun MeHome(
    playlistCount: Int,
    historyCount: Int,
    onOpenSubPage: (MeSubPage) -> Unit,
    onOpenTabsSheet: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_tab_home_screen")
    ) {
        TopAppBar(
            title = { Text("Me", style = MaterialTheme.typography.headlineLargeEmphasized, maxLines = 1) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
        )
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
                    MeShortcutCard(
                        icon = Symbols.PlaylistPlay,
                        title = "Playlists",
                        subtitle = if (playlistCount == 1) "1 video playlist" else "$playlistCount video playlists",
                        onClick = { onOpenSubPage(MeSubPage.PLAYLISTS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("me_card_video_playlist")
                    )
                    MeShortcutCard(
                        icon = Symbols.History,
                        title = "History",
                        subtitle = if (historyCount == 1) "1 video watched" else "$historyCount videos watched",
                        onClick = { onOpenSubPage(MeSubPage.HISTORY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("me_card_video_history")
                    )
                }
            }
            settingsGroup(
                key = "settings",
                title = "Settings",
                rows = listOf(
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
                        key = "navigation",
                        icon = Symbols.BottomNavigation,
                        title = "Navigation bar",
                        summary = "Choose which tabs show",
                        badge = true,
                        onClick = onOpenTabsSheet
                    )
                )
            )
        }
    }
}

/** A big shortcut card on the Me home page. */
@Composable
private fun MeShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IconBadge(icon = icon, size = 48.dp)
            Spacer(modifier = Modifier.height(16.dp))
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
}
