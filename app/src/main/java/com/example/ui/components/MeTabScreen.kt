package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BottomTab
import com.example.model.DecoderMode
import com.example.model.MeSubPage
import com.example.model.PlaybackSettings
import com.example.model.PlaylistItem
import com.example.model.ScreenOrientationMode
import com.example.model.ThemeMode
import com.example.model.VideoHistoryEntry
import com.example.model.VideoItem
import com.example.model.formatLastWatchedDate
import com.example.model.resolutionLabel
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.PresetAppearanceSwatches
import com.example.ui.theme.colorToHex
import com.example.ui.theme.parseHexColorOrNull
import com.example.viewmodel.VideoUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeTabScreen(
    uiState: VideoUiState,
    effectiveAmoled: Boolean,
    onOpenSubPage: (MeSubPage) -> Unit,
    onCloseSubPage: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onClearHistory: () -> Unit,
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
    onToggleShowResolutionTier: () -> Unit,
    onUpdatePlaybackSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit,
    onSetTabActive: (BottomTab, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val motionScheme = MaterialTheme.motionScheme

    if (uiState.meSubPage != MeSubPage.NONE) {
        BackHandler(onBack = onCloseSubPage)
    }

    val videoById = remember(uiState.videos) {
        uiState.videos.associateBy { it.id }
    }
    val historyPairs = remember(uiState.historyEntries, videoById) {
        uiState.historyEntries.mapNotNull { entry ->
            val video = videoById[entry.mediaUri] ?: return@mapNotNull null
            entry to video
        }
    }

    AnimatedContent(
        targetState = uiState.meSubPage,
        transitionSpec = {
            (fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = motionScheme.defaultSpatialSpec()
                )) togetherWith
                (fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                    scaleOut(
                        targetScale = 0.94f,
                        animationSpec = motionScheme.fastSpatialSpec()
                    ))
        },
        label = "meSubPageTransition",
        modifier = modifier.fillMaxSize()
    ) { subPage ->
        when (subPage) {
            MeSubPage.NONE -> MeHomeMenuScreen(
                playlistCount = uiState.playlists.size,
                historyCount = historyPairs.size,
                effectiveAmoled = effectiveAmoled,
                onOpenSubPage = onOpenSubPage
            )

            MeSubPage.PLAYLISTS -> VideoPlaylistSubPage(
                playlists = uiState.playlists,
                allVideos = uiState.videos,
                videoById = videoById,
                showResolutionTier = uiState.showResolutionTier,
                effectiveAmoled = effectiveAmoled,
                onBack = onCloseSubPage,
                onPlayVideo = onPlayVideo,
                onCreatePlaylist = onCreatePlaylist,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onAddVideosToPlaylist = onAddVideosToPlaylist,
                onRemoveVideoFromPlaylist = onRemoveVideoFromPlaylist
            )

            MeSubPage.HISTORY -> VideoHistorySubPage(
                historyPairs = historyPairs,
                showResolutionTier = uiState.showResolutionTier,
                effectiveAmoled = effectiveAmoled,
                onBack = onCloseSubPage,
                onPlayVideo = onPlayVideo,
                onClearHistory = onClearHistory
            )

            MeSubPage.APPEARANCE -> AppearanceSubPage(
                uiState = uiState,
                effectiveAmoled = effectiveAmoled,
                onBack = onCloseSubPage,
                onSelectThemeMode = onSelectThemeMode,
                onSetDynamicColor = onSetDynamicColor,
                onSelectColorTheme = onSelectColorTheme,
                onSelectCustomColor = onSelectCustomColor,
                onToggleAmoledBlack = onToggleAmoledBlack,
                onToggleShowResolutionTier = onToggleShowResolutionTier
            )

            MeSubPage.VIDEO_SETTINGS -> VideoPlaybackSettingsSubPage(
                settings = uiState.playbackSettings,
                effectiveAmoled = effectiveAmoled,
                onBack = onCloseSubPage,
                onUpdateSettings = onUpdatePlaybackSettings
            )

            MeSubPage.NAVIGATION -> NavigationCustomizeSubPage(
                activeTabs = uiState.activeTabs,
                effectiveAmoled = effectiveAmoled,
                onBack = onCloseSubPage,
                onSetTabActive = onSetTabActive
            )
        }
    }
}

@Composable
private fun MeHomeMenuScreen(
    playlistCount: Int,
    historyCount: Int,
    effectiveAmoled: Boolean,
    onOpenSubPage: (MeSubPage) -> Unit
) {
    val palette = LocalM3Palette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 148.dp)
            .testTag("me_tab_home_screen")
    ) {
        // Top: Two wide cards side by side ("Video Playlist" and "Video History")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveTopWideCard(
                icon = Icons.Default.PlaylistPlay,
                title = "Video Playlist",
                subtitle = if (playlistCount == 1) "1 playlist" else "$playlistCount playlists",
                effectiveAmoled = effectiveAmoled,
                onClick = { onOpenSubPage(MeSubPage.PLAYLISTS) },
                testTag = "me_card_video_playlist",
                modifier = Modifier.weight(1f)
            )

            ExpressiveTopWideCard(
                icon = Icons.Default.History,
                title = "Video History",
                subtitle = if (historyCount == 1) "1 watched" else "$historyCount watched",
                effectiveAmoled = effectiveAmoled,
                onClick = { onOpenSubPage(MeSubPage.HISTORY) },
                testTag = "me_card_video_history",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // "Preferences" heading
        Text(
            text = "Preferences",
            color = palette.primary,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Expressive preference rows using official SegmentedListItem
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExpressivePreferenceMenuRow(
                icon = Icons.Default.Palette,
                title = "Appearance",
                subtitle = "Theme and colors",
                index = 0,
                totalCount = 3,
                effectiveAmoled = effectiveAmoled,
                onClick = { onOpenSubPage(MeSubPage.APPEARANCE) },
                testTag = "me_pref_appearance"
            )
            ExpressivePreferenceMenuRow(
                icon = Icons.Default.Tune,
                title = "Video",
                subtitle = "Playback, gestures and more",
                index = 1,
                totalCount = 3,
                effectiveAmoled = effectiveAmoled,
                onClick = { onOpenSubPage(MeSubPage.VIDEO_SETTINGS) },
                testTag = "me_pref_video"
            )
            ExpressivePreferenceMenuRow(
                icon = Icons.Default.ViewCarousel,
                title = "Navigation",
                subtitle = "Customize navigation bar items",
                index = 2,
                totalCount = 3,
                effectiveAmoled = effectiveAmoled,
                onClick = { onOpenSubPage(MeSubPage.NAVIGATION) },
                testTag = "me_pref_navigation"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveTopWideCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    effectiveAmoled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cardScale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 34.dp else 26.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cardCorner"
    )
    val cardBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 1.dp,
                color = palette.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(palette.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressivePreferenceMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    index: Int,
    totalCount: Int,
    effectiveAmoled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalM3Palette.current
    val rowBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = totalCount.coerceAtLeast(1)
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = rowBg,
            contentColor = palette.onSurface,
            supportingContentColor = palette.onSurfaceVariant
        ),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(palette.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        supportingContent = {
            Text(
                text = subtitle,
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        },
        content = {
            Text(
                text = title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveSubPageHeader(
    icon: ImageVector,
    title: String,
    onBack: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val palette = LocalM3Palette.current

    MediumFlexibleTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(MaterialShapes.Sunny.toShape())
                        .background(palette.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    color = palette.onSurface,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            FilledTonalIconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = palette.surfaceContainerHigh,
                    contentColor = palette.onSurface
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(44.dp)
                    .testTag("me_subpage_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        actions = {
            if (trailingAction != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    trailingAction()
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}

// ============================================================================
// 4. VIDEO HISTORY SUB-PAGE
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoHistorySubPage(
    historyPairs: List<Pair<VideoHistoryEntry, VideoItem>>,
    showResolutionTier: Boolean,
    effectiveAmoled: Boolean,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onClearHistory: () -> Unit
) {
    val palette = LocalM3Palette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_subpage_video_history")
    ) {
        ExpressiveSubPageHeader(
            icon = Icons.Default.History,
            title = "Video History",
            onBack = onBack,
            trailingAction = if (historyPairs.isNotEmpty()) {
                {
                    FilledTonalButton(
                        onClick = onClearHistory,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.surfaceContainerHigh,
                            contentColor = palette.error
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear history",
                            tint = palette.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear history",
                            color = palette.error,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null
        )

        if (historyPairs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(palette.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = palette.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No watch history yet",
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Videos you play will appear here (up to 25 recent videos).",
                        color = palette.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 148.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = historyPairs,
                    key = { _, (entry, _) -> "hist_${entry.mediaUri}" }
                ) { idx, (entry, video) ->
                    val badge = if (showResolutionTier) resolutionLabel(video.width, video.height) else null
                    val watchedDate = formatLastWatchedDate(entry.lastPlayedTimestamp)
                    val rowBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

                    SegmentedListItem(
                        onClick = { onPlayVideo(video) },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = idx,
                            count = historyPairs.size.coerceAtLeast(1)
                        ),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = rowBg,
                            contentColor = palette.onSurface,
                            supportingContentColor = palette.onSurfaceVariant
                        ),
                        leadingContent = {
                            VideoThumbnailBox(
                                video = video,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(68.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        },
                        supportingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (badge != null) {
                                    Text(
                                        text = badge,
                                        color = palette.onSurfaceVariant,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = watchedDate,
                                    color = palette.onSurfaceVariant,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        content = {
                            Text(
                                text = video.title,
                                color = palette.onSurface,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.5.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${video.mediaId}")
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. VIDEO PLAYLIST SUB-PAGE
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoPlaylistSubPage(
    playlists: List<PlaylistItem>,
    allVideos: List<VideoItem>,
    videoById: Map<String, VideoItem>,
    showResolutionTier: Boolean,
    effectiveAmoled: Boolean,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (playlistId: String, newName: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onAddVideosToPlaylist: (playlistId: String, videoIds: List<String>) -> Unit,
    onRemoveVideoFromPlaylist: (playlistId: String, videoId: String) -> Unit
) {
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameInput by remember { mutableStateOf("") }
    var renamePlaylistTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    var renamePlaylistInput by remember { mutableStateOf("") }
    var addVideosPlaylistTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_subpage_video_playlist")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ExpressiveSubPageHeader(
                icon = Icons.Default.PlaylistPlay,
                title = "Video Playlist",
                onBack = onBack,
                trailingAction = {
                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.LeadingButton(
                                onClick = {
                                    newPlaylistNameInput = ""
                                    showCreatePlaylistDialog = true
                                },
                                modifier = Modifier.testTag("me_create_playlist_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create playlist",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Create",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        trailingButton = {
                            SplitButtonDefaults.TrailingButton(
                                checked = fabMenuExpanded,
                                onCheckedChange = { fabMenuExpanded = it }
                            ) {
                                Icon(
                                    imageVector = if (fabMenuExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Playlist actions",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            )

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(MaterialShapes.SoftBurst.toShape())
                                .background(palette.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = palette.onSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No playlists yet",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap Create to build a custom video playlist.",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 148.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = playlists,
                        key = { _, pl -> "pl_${pl.id}" }
                    ) { _, pl ->
                        val resolvedVideos = pl.mediaUris.mapNotNull { videoById[it] }
                        val firstVideo = resolvedVideos.firstOrNull()
                        val isExpanded = expandedPlaylistId == pl.id
                        val cardBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("me_playlist_card_${pl.name}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedPlaylistId = if (isExpanded) null else pl.id
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (firstVideo != null) {
                                            VideoThumbnailBox(
                                                video = firstVideo,
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .height(56.dp)
                                                    .clip(MaterialTheme.shapes.medium)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .height(56.dp)
                                                    .clip(MaterialTheme.shapes.medium)
                                                    .background(palette.surfaceContainerHigh),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlaylistPlay,
                                                    contentDescription = null,
                                                    tint = palette.primary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pl.name,
                                                color = palette.onSurface,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (resolvedVideos.size == 1) "1 video" else "${resolvedVideos.size} videos",
                                                color = palette.onSurfaceVariant,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { addVideosPlaylistTarget = pl },
                                            shapes = IconButtonDefaults.shapes(),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add videos to ${pl.name}",
                                                tint = palette.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                renamePlaylistTarget = pl
                                                renamePlaylistInput = pl.name
                                            },
                                            shapes = IconButtonDefaults.shapes(),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename ${pl.name}",
                                                tint = palette.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeletePlaylist(pl.id) },
                                            shapes = IconButtonDefaults.shapes(),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete ${pl.name}",
                                                tint = palette.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = palette.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(animationSpec = motionScheme.defaultSpatialSpec()) +
                                        fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                                    exit = shrinkVertically(animationSpec = motionScheme.fastSpatialSpec()) +
                                        fadeOut(animationSpec = motionScheme.fastEffectsSpec())
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        HorizontalDivider(color = palette.surfaceContainerHighest)
                                        if (resolvedVideos.isEmpty()) {
                                            Text(
                                                text = "No videos in this playlist yet. Tap + to add videos.",
                                                color = palette.onSurfaceVariant,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        } else {
                                            resolvedVideos.forEachIndexed { vIdx, video ->
                                                val badge = if (showResolutionTier) resolutionLabel(video.width, video.height) else null
                                                SegmentedListItem(
                                                    onClick = { onPlayVideo(video) },
                                                    shapes = ListItemDefaults.segmentedShapes(
                                                        index = vIdx,
                                                        count = resolvedVideos.size.coerceAtLeast(1)
                                                    ),
                                                    colors = ListItemDefaults.segmentedColors(
                                                        containerColor = palette.surfaceContainerHigh,
                                                        contentColor = palette.onSurface,
                                                        supportingContentColor = palette.onSurfaceVariant
                                                    ),
                                                    leadingContent = {
                                                        VideoThumbnailBox(
                                                            video = video,
                                                            modifier = Modifier
                                                                .width(88.dp)
                                                                .height(50.dp)
                                                                .clip(MaterialTheme.shapes.small)
                                                        )
                                                    },
                                                    supportingContent = {
                                                        Text(
                                                            text = buildString {
                                                                if (badge != null) {
                                                                    append(badge)
                                                                    append(" · ")
                                                                }
                                                                append(video.durationLabel)
                                                            },
                                                            color = palette.onSurfaceVariant,
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    },
                                                    trailingContent = {
                                                        IconButton(
                                                            onClick = { onRemoveVideoFromPlaylist(pl.id, video.id) },
                                                            shapes = IconButtonDefaults.shapes(),
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.RemoveCircleOutline,
                                                                contentDescription = "Remove from playlist",
                                                                tint = palette.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    },
                                                    content = {
                                                        Text(
                                                            text = video.title,
                                                            color = palette.onSurface,
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Official Material 3 Expressive FloatingActionButtonMenu
        FloatingActionButtonMenu(
            expanded = fabMenuExpanded,
            button = {
                ToggleFloatingActionButton(
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = it }
                ) {
                    Icon(
                        imageVector = if (fabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Playlist quick actions"
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 104.dp)
        ) {
            FloatingActionButtonMenuItem(
                onClick = {
                    fabMenuExpanded = false
                    newPlaylistNameInput = ""
                    showCreatePlaylistDialog = true
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = "New playlist",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(
                    text = "Create playlist",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistNameInput,
                    onValueChange = { newPlaylistNameInput = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Playlist name...",
                            fontFamily = PlusJakartaSansFontFamily
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newPlaylistNameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreatePlaylist(trimmed)
                            showCreatePlaylistDialog = false
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Create", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Playlist Dialog
    val currentRenameTarget = renamePlaylistTarget
    if (currentRenameTarget != null) {
        AlertDialog(
            onDismissRequest = { renamePlaylistTarget = null },
            title = {
                Text(
                    text = "Rename playlist",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = renamePlaylistInput,
                    onValueChange = { renamePlaylistInput = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renamePlaylistInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenamePlaylist(currentRenameTarget.id, trimmed)
                            renamePlaylistTarget = null
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { renamePlaylistTarget = null },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Videos to Playlist Dialog
    val currentAddTarget = addVideosPlaylistTarget
    if (currentAddTarget != null) {
        val availableVideos = allVideos.filter { it.id !in currentAddTarget.mediaUris }
        AlertDialog(
            onDismissRequest = { addVideosPlaylistTarget = null },
            title = {
                Text(
                    text = "Add videos to ${currentAddTarget.name}",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (availableVideos.isEmpty()) {
                    Text(
                        text = if (allVideos.isEmpty()) {
                            "No videos found on device."
                        } else {
                            "All device videos are already in this playlist."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        availableVideos.forEachIndexed { idx, video ->
                            SegmentedListItem(
                                onClick = {
                                    onAddVideosToPlaylist(currentAddTarget.id, listOf(video.id))
                                    addVideosPlaylistTarget = null
                                },
                                shapes = ListItemDefaults.segmentedShapes(
                                    index = idx,
                                    count = availableVideos.size.coerceAtLeast(1)
                                ),
                                colors = ListItemDefaults.segmentedColors(
                                    containerColor = palette.surfaceContainer,
                                    contentColor = palette.onSurface,
                                    supportingContentColor = palette.onSurfaceVariant
                                ),
                                supportingContent = {
                                    Text(
                                        text = video.durationLabel,
                                        color = palette.onSurfaceVariant,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                content = {
                                    Text(
                                        text = video.title,
                                        color = palette.onSurface,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { addVideosPlaylistTarget = null },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// ============================================================================
// 6. APPEARANCE SUB-PAGE
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearanceSubPage(
    uiState: VideoUiState,
    effectiveAmoled: Boolean,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSelectColorTheme: (String) -> Unit,
    onSelectCustomColor: (Int) -> Unit,
    onToggleAmoledBlack: () -> Unit,
    onToggleShowResolutionTier: () -> Unit
) {
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    var showCustomColorPicker by remember { mutableStateOf(false) }
    val cardBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_subpage_appearance")
    ) {
        ExpressiveSubPageHeader(
            icon = Icons.Default.Palette,
            title = "Appearance",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme Mode Card: System (default), Light, Dark
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Theme mode",
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose how Videos adapts to system light and dark themes",
                        color = palette.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        val modes = listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark"
                        )
                        modes.forEachIndexed { index, (mode, label) ->
                            val active = uiState.themeMode == mode
                            val shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            ToggleButton(
                                checked = active,
                                onCheckedChange = { onSelectThemeMode(mode) },
                                shapes = shapes,
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = palette.surfaceContainerHigh,
                                    contentColor = palette.onSurfaceVariant,
                                    checkedContainerColor = palette.primary,
                                    checkedContentColor = palette.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("me_theme_mode_${mode.id}")
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Color (Material You) Switch
            ExpressiveSettingToggleCard(
                title = "Dynamic color (Material You)",
                subtitle = "Use Android 12+ wallpaper colors across the interface",
                checked = uiState.dynamicColor,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onSetDynamicColor(!uiState.dynamicColor) },
                testTag = "me_dynamic_color_row"
            )

            // Colors (shown ONLY when Dynamic color is OFF)
            AnimatedVisibility(
                visible = !uiState.dynamicColor,
                enter = expandVertically(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(animationSpec = motionScheme.fastSpatialSpec()) +
                    fadeOut(animationSpec = motionScheme.fastEffectsSpec())
            ) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Color scheme",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pick a preset accent or craft a custom seed color",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2 rows of 3 round swatches (5 presets + Custom)
                        val customSeedColor = Color(uiState.customColorArgb)
                        val swatchItems = PresetAppearanceSwatches.map { preset ->
                            Triple(preset.id, preset.label, preset.swatchColor)
                        } + Triple("custom", "Custom", customSeedColor)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            swatchItems.chunked(3).forEach { rowSwatches ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowSwatches.forEach { (id, label, swatchColor) ->
                                        val active = uiState.colorTheme == id
                                        val isCustom = id == "custom"
                                        val corner by animateDpAsState(
                                            targetValue = if (active) 24.dp else 16.dp,
                                            animationSpec = motionScheme.fastSpatialSpec(),
                                            label = "swatchCorner_$id"
                                        )

                                        Card(
                                            onClick = {
                                                if (isCustom) {
                                                    showCustomColorPicker = true
                                                } else {
                                                    onSelectColorTheme(id)
                                                }
                                            },
                                            shape = RoundedCornerShape(corner),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (active) palette.secondaryContainer else palette.surfaceContainerHigh
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    width = if (active) 2.dp else 1.dp,
                                                    color = if (active) palette.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(corner)
                                                )
                                                .testTag("appearance_swatch_$id")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(
                                                            if (active) {
                                                                MaterialShapes.Sunny.toShape()
                                                            } else {
                                                                CircleShape
                                                            }
                                                        )
                                                        .background(swatchColor)
                                                        .border(
                                                            width = 2.dp,
                                                            color = Color.White.copy(alpha = 0.25f),
                                                            shape = if (active) {
                                                                MaterialShapes.Sunny.toShape()
                                                            } else {
                                                                CircleShape
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (active) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    } else if (isCustom) {
                                                        Icon(
                                                            imageVector = Icons.Default.Colorize,
                                                            contentDescription = "Custom color",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = label,
                                                    color = if (active) palette.onSecondaryContainer else palette.onSurface,
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AMOLED black toggle (applies when dark)
            ExpressiveSettingToggleCard(
                title = "AMOLED black",
                subtitle = "Pure black background when dark theme is active",
                checked = uiState.amoledBlack,
                effectiveAmoled = effectiveAmoled,
                onToggle = onToggleAmoledBlack,
                testTag = "me_amoled_black_row"
            )

            // Show resolution badges switch
            ExpressiveSettingToggleCard(
                title = "Show resolution badges",
                subtitle = "Display 8K, 4K, 1440p, 1080p, 720p and SD tags on videos",
                checked = uiState.showResolutionTier,
                effectiveAmoled = effectiveAmoled,
                onToggle = onToggleShowResolutionTier,
                testTag = "me_resolution_badges_row"
            )
        }
    }

    if (showCustomColorPicker) {
        CustomSeedColorPickerDialog(
            initialColorArgb = uiState.customColorArgb,
            onConfirmColor = { chosenArgb ->
                onSelectCustomColor(chosenArgb)
                showCustomColorPicker = false
            },
            onDismiss = { showCustomColorPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomSeedColorPickerDialog(
    initialColorArgb: Int,
    onConfirmColor: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val initialHsv = remember(initialColorArgb) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColorArgb, hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0].coerceIn(0f, 360f)) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceIn(0f, 1f)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0f, 1f)) }

    val currentColor = remember(hue, saturation, brightness) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }
    var hexInput by remember(currentColor) {
        mutableStateOf(colorToHex(currentColor))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom Theme Color",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live preview swatch & Hex field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialShapes.Sunny.toShape())
                            .background(currentColor)
                            .border(2.dp, palette.outlineVariant, MaterialShapes.Sunny.toShape())
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { next ->
                            val filtered = next.uppercase().take(7)
                            hexInput = filtered
                            val parsed = parseHexColorOrNull(filtered)
                            if (parsed != null) {
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(parsed.toArgb(), hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                            }
                        },
                        singleLine = true,
                        label = {
                            Text(
                                text = "Hex (#RRGGBB)",
                                fontFamily = PlusJakartaSansFontFamily
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_color_hex_input")
                    )
                }

                // Hue Slider (0..360)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hue",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${hue.roundToInt()}°",
                            color = palette.primary,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            activeTrackColor = currentColor
                        ),
                        modifier = Modifier.testTag("custom_color_hue_slider")
                    )
                }

                // Saturation Slider (0..100%)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Saturation",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(saturation * 100).roundToInt()}%",
                            color = palette.primary,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            activeTrackColor = currentColor
                        ),
                        modifier = Modifier.testTag("custom_color_saturation_slider")
                    )
                }

                // Brightness Slider (0..100%)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Brightness",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(brightness * 100).roundToInt()}%",
                            color = palette.primary,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0.15f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            activeTrackColor = currentColor
                        ),
                        modifier = Modifier.testTag("custom_color_brightness_slider")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmColor(currentColor.toArgb()) },
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Apply Color", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// 7. VIDEO PLAYBACK SETTINGS SUB-PAGE
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoPlaybackSettingsSubPage(
    settings: PlaybackSettings,
    effectiveAmoled: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit
) {
    val palette = LocalM3Palette.current
    val cardBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

    var showOrientationSheet by remember { mutableStateOf(false) }
    var showDecoderSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_subpage_video_settings")
    ) {
        ExpressiveSubPageHeader(
            icon = Icons.Default.Tune,
            title = "Video",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Gestures",
                color = palette.primary,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            ExpressiveSettingToggleCard(
                title = "Double tap to seek",
                subtitle = "Double tap left or right side of video to rewind or fast-forward",
                checked = settings.doubleTapToSeek,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(doubleTapToSeek = !it.doubleTapToSeek) } },
                testTag = "setting_double_tap_seek"
            )

            // Expressive Slider Card for Skip duration (5 to 30 seconds in steps of 5, default 10)
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setting_skip_duration_slider_card")
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Skip duration",
                                color = palette.onSurface,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Seconds jumped when seeking via double tap or buttons",
                                color = palette.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = palette.secondaryContainer
                        ) {
                            Text(
                                text = "${settings.skipDurationSec}s",
                                color = palette.onSecondaryContainer,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = settings.skipDurationSec.toFloat(),
                        onValueChange = { rawValue ->
                            val snapped = ((rawValue / 5f).roundToInt() * 5).coerceIn(5, 30)
                            if (snapped != settings.skipDurationSec) {
                                onUpdateSettings { it.copy(skipDurationSec = snapped) }
                            }
                        },
                        valueRange = 5f..30f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = palette.surfaceContainerHighest,
                            activeTickColor = palette.onPrimary,
                            inactiveTickColor = palette.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_skip_duration_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 10, 15, 20, 25, 30).forEach { sec ->
                            Text(
                                text = "${sec}s",
                                color = if (settings.skipDurationSec == sec) palette.primary else palette.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = if (settings.skipDurationSec == sec) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            ExpressiveSettingToggleCard(
                title = "Double tap to play/pause",
                subtitle = "Double tap the center of the screen to toggle playback",
                checked = settings.doubleTapToPause,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(doubleTapToPause = !it.doubleTapToPause) } },
                testTag = "setting_double_tap_pause"
            )

            ExpressiveSettingToggleCard(
                title = "Long press for 2x speed",
                subtitle = "Hold anywhere on the video to temporarily play at 2x speed",
                checked = settings.longPressFor2xSpeed,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(longPressFor2xSpeed = !it.longPressFor2xSpeed) } },
                testTag = "setting_long_press_2x"
            )

            ExpressiveSettingToggleCard(
                title = "Horizontal swipe to seek",
                subtitle = "Slide horizontally across the video to scrub through time",
                checked = settings.horizontalSwipeToSeek,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(horizontalSwipeToSeek = !it.horizontalSwipeToSeek) } },
                testTag = "setting_horizontal_swipe_seek"
            )

            ExpressiveSettingToggleCard(
                title = "Left vertical swipe for brightness",
                subtitle = "Slide up or down on the left half to adjust screen brightness",
                checked = settings.leftSwipeForBrightness,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(leftSwipeForBrightness = !it.leftSwipeForBrightness) } },
                testTag = "setting_left_swipe_brightness"
            )

            ExpressiveSettingToggleCard(
                title = "Right vertical swipe for volume",
                subtitle = "Slide up or down on the right half to adjust media volume",
                checked = settings.rightSwipeForVolume,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(rightSwipeForVolume = !it.rightSwipeForVolume) } },
                testTag = "setting_right_swipe_volume"
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Playback & Display",
                color = palette.primary,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            // Dropdown Row: Force screen orientation
            ExpressiveDropdownSettingRow(
                title = "Force screen orientation",
                currentValueLabel = settings.forceOrientation.label,
                effectiveAmoled = effectiveAmoled,
                onClick = { showOrientationSheet = true },
                testTag = "setting_force_orientation_dropdown"
            )

            // Dropdown Row: Default decoder
            ExpressiveDropdownSettingRow(
                title = "Default decoder",
                currentValueLabel = settings.defaultDecoder.label,
                effectiveAmoled = effectiveAmoled,
                onClick = { showDecoderSheet = true },
                testTag = "setting_default_decoder_dropdown"
            )

            ExpressiveSettingToggleCard(
                title = "Show remaining time instead of total duration",
                subtitle = "Display time left (-mm:ss) on the right side of the seek bar",
                checked = settings.showRemainingTime,
                effectiveAmoled = effectiveAmoled,
                onToggle = { onUpdateSettings { it.copy(showRemainingTime = !it.showRemainingTime) } },
                testTag = "setting_show_remaining_time"
            )
        }
    }

    if (showOrientationSheet) {
        ExpressiveRadioBottomSheet(
            title = "Force screen orientation",
            options = ScreenOrientationMode.entries.map { it to it.label },
            selectedOption = settings.forceOrientation,
            onSelectOption = { chosen ->
                onUpdateSettings { it.copy(forceOrientation = chosen) }
                showOrientationSheet = false
            },
            onDismiss = { showOrientationSheet = false }
        )
    }

    if (showDecoderSheet) {
        ExpressiveRadioBottomSheet(
            title = "Default decoder",
            options = DecoderMode.entries.map { it to it.label },
            selectedOption = settings.defaultDecoder,
            onSelectOption = { chosen ->
                onUpdateSettings { it.copy(defaultDecoder = chosen) }
                showDecoderSheet = false
            },
            onDismiss = { showDecoderSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveDropdownSettingRow(
    title: String,
    currentValueLabel: String,
    effectiveAmoled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalM3Palette.current
    val cardBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.segmentedColors(
            containerColor = cardBg,
            contentColor = palette.onSurface,
            supportingContentColor = palette.primary
        ),
        supportingContent = {
            Text(
                text = currentValueLabel,
                color = palette.primary,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(palette.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        content = {
            Text(
                text = title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ExpressiveRadioBottomSheet(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelectOption: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) },
        modifier = Modifier.testTag("expressive_radio_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEachIndexed { idx, (option, label) ->
                    val isSelected = option == selectedOption
                    SegmentedListItem(
                        selected = isSelected,
                        onClick = { onSelectOption(option) },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = idx,
                            count = options.size.coerceAtLeast(1)
                        ),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = palette.surfaceContainer,
                            contentColor = palette.onSurface,
                            selectedContainerColor = palette.secondaryContainer,
                            selectedContentColor = palette.onSecondaryContainer
                        ),
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = palette.primary,
                                    unselectedColor = palette.onSurfaceVariant
                                )
                            )
                        },
                        content = {
                            Text(
                                text = label,
                                color = if (isSelected) palette.onSecondaryContainer else palette.onSurface,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ============================================================================
// 8. NAVIGATION CUSTOMIZATION SUB-PAGE
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationCustomizeSubPage(
    activeTabs: List<BottomTab>,
    effectiveAmoled: Boolean,
    onBack: () -> Unit,
    onSetTabActive: (BottomTab, Boolean) -> Unit
) {
    val palette = LocalM3Palette.current
    val fixedOrderTabs = listOf(
        Triple(BottomTab.AUDIO, "Audio", Icons.Default.MusicNote to "Play local music and audio tracks"),
        Triple(BottomTab.VIDEOS, "Videos", Icons.Default.PlayArrow to "Main video library with list, grid and tree views"),
        Triple(BottomTab.ME, "Me", Icons.Default.Person to "Playlists, watch history and app preferences")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("me_subpage_navigation")
    ) {
        ExpressiveSubPageHeader(
            icon = Icons.Default.ViewCarousel,
            title = "Navigation",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Choose which items appear in the bottom navigation pill. You can also long-press any icon on the bottom pill to edit directly.",
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            fixedOrderTabs.forEachIndexed { idx, (tab, title, iconAndSub) ->
                val (icon, subtitle) = iconAndSub
                val isChecked = tab in activeTabs
                // Minimum One Tab Rule: when only 1 tab is active, that last active tab cannot be turned off
                val isLastRemainingActive = isChecked && activeTabs.size <= 1
                val canToggle = !isLastRemainingActive
                val rowBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

                SegmentedListItem(
                    checked = isChecked,
                    onCheckedChange = {
                        if (canToggle) {
                            onSetTabActive(tab, !isChecked)
                        }
                    },
                    enabled = canToggle,
                    shapes = ListItemDefaults.segmentedShapes(
                        index = idx,
                        count = fixedOrderTabs.size
                    ),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = rowBg,
                        contentColor = palette.onSurface,
                        supportingContentColor = palette.onSurfaceVariant,
                        selectedContainerColor = rowBg,
                        selectedContentColor = palette.onSurface,
                        selectedSupportingContentColor = palette.onSurfaceVariant
                    ),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                                .background(
                                    if (isChecked) palette.secondaryContainer else palette.surfaceContainerHigh
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isChecked) palette.onSecondaryContainer else palette.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text = if (isLastRemainingActive) {
                                "At least one navigation tab must stay active"
                            } else {
                                subtitle
                            },
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isChecked,
                            onCheckedChange = null,
                            enabled = canToggle,
                            thumbContent = if (isChecked) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    },
                    content = {
                        Text(
                            text = title,
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (canToggle) 1f else 0.65f)
                        .testTag("nav_toggle_row_${tab.id}")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveSettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    effectiveAmoled: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    val palette = LocalM3Palette.current
    val cardBg = if (effectiveAmoled) palette.surfaceContainerLow else palette.surfaceContainer

    SegmentedListItem(
        checked = checked,
        onCheckedChange = { onToggle() },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.segmentedColors(
            containerColor = cardBg,
            contentColor = palette.onSurface,
            supportingContentColor = palette.onSurfaceVariant,
            selectedContainerColor = cardBg,
            selectedContentColor = palette.onSurface,
            selectedSupportingContentColor = palette.onSurfaceVariant
        ),
        supportingContent = {
            Text(
                text = subtitle,
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null
            )
        },
        content = {
            Text(
                text = title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
