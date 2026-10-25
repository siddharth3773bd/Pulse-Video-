package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import com.example.data.MediaStoreScanner
import com.example.ui.components.SongInfoSheet
import com.example.ui.components.NowPlayingScreen
import com.example.ui.components.AudioMiniPlayer
import com.example.player.AudioPlaybackManager
import com.example.model.LibraryPreferences
import com.example.model.AudioTrackItem
import com.example.data.AppPreferencesRepository
import com.example.model.BottomTab
import com.example.model.LayoutMode
import com.example.model.MeSubPage
import com.example.model.ThemeMode
import com.example.model.VideoItem
import com.example.model.ViewMode
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AudioLibraryScreen
import com.example.ui.components.ColorOsDetailsSheet
import com.example.ui.components.ColorOsFloatingMenuOverlay
import com.example.ui.components.ColorOsRenameDialog
import com.example.ui.components.FoldersLibraryView
import com.example.ui.components.MeTabScreen
import com.example.ui.components.MediaPermissionPromptCard
import com.example.ui.components.ResumePlaybackBottomSheet
import com.example.ui.components.SortAndViewBottomSheet
import com.example.ui.components.TreeHierarchyView
import com.example.ui.components.VideoGridItem
import com.example.ui.components.VideoListItem
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.AlbumArtTheme
import com.example.ui.theme.LocalM3Palette
import com.example.model.NowPlayingBackground
import com.example.model.NowPlayingColorSource
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.DopplerTheme
import com.example.viewmodel.VideoPlayerViewModel
import com.example.viewmodel.VideoUiState

class MainActivity : ComponentActivity() {

    private lateinit var externalViewModel: VideoPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Same instance that the compose viewModel() call below returns for this activity.
        externalViewModel = androidx.lifecycle.ViewModelProvider(this)[VideoPlayerViewModel::class.java]
        if (savedInstanceState == null) {
            handleExternalVideoIntent(intent)
        }
        setContent {
            val viewModel: VideoPlayerViewModel = viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.initializeAndScan(context)
            }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DopplerTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.dynamicColor,
                paletteId = uiState.colorTheme,
                customColorArgb = uiState.customColorArgb,
                amoledBlack = uiState.amoledBlack
            ) {
                Material3VideoPlayerScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalVideoIntent(intent)
    }

    /**
     * "Open with" support: a video chosen in a file manager plays in the normal full-screen player.
     * When the player is closed the activity finishes so the user returns to the file manager.
     */
    private fun handleExternalVideoIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val mime = intent.type ?: try {
            contentResolver.getType(uri)
        } catch (_: Throwable) {
            null
        }
        if (mime != null && !mime.startsWith("video/") && mime != "application/x-matroska") return

        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) {
                try {
                    com.example.player.ExternalMedia.buildVideoItem(applicationContext, uri)
                } catch (_: Throwable) {
                    null
                }
            }
            if (item == null) {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "Can't open this video file",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }
            externalViewModel.playVideo(item, this@MainActivity)
            // Wait until the player has actually opened, then close the app when it is closed again.
            externalViewModel.uiState.first { it.playingVideo != null }
            externalViewModel.uiState.first { it.playingVideo == null }
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Material3VideoPlayerScreen(
    uiState: VideoUiState,
    viewModel: VideoPlayerViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    val systemDark = isSystemInDarkTheme()
    val isDarkActive = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val effectiveAmoled = uiState.amoledBlack && isDarkActive
    val outerChromeBg = if (effectiveAmoled) Color.Black else MaterialTheme.colorScheme.background

    // Rescan when returning to foreground
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndScan(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Register ContentObserver on MediaStore for automatic live updates
    DisposableEffect(context) {
        val mainHandler = Handler(Looper.getMainLooper())
        val contentObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                viewModel.refreshPermissionsAndScan(context)
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
        } catch (_: Exception) {
        }
        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (_: Exception) {
            }
        }
    }

    // Permission launchers
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.markVideoPermissionAsked()
        viewModel.refreshPermissionsAndScan(context)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.markAudioPermissionAsked()
        viewModel.refreshPermissionsAndScan(context)
    }

    var notificationPermissionAsked by remember { mutableStateOf(false) }
    var pendingAudioActionAfterNotifPerm by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val action = pendingAudioActionAfterNotifPerm
        pendingAudioActionAfterNotifPerm = null
        action?.invoke()
    }

    val runWithNotificationPermissionCheck: (() -> Unit) -> Unit = { audioAction ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionAsked &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionAsked = true
            pendingAudioActionAfterNotifPerm = audioAction
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            audioAction()
        }
    }

    // System confirmation dialog launcher for Rename (createWriteRequest) and Delete (createDeleteRequest)
    lateinit var launchIntentSender: (android.content.IntentSender) -> Unit
    val mediaIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onMediaIntentSenderResult(
            context = context,
            confirmed = result.resultCode == Activity.RESULT_OK,
            onLaunchIntentSender = { sender -> launchIntentSender(sender) }
        )
    }
    launchIntentSender = { sender ->
        try {
            mediaIntentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } catch (_: Exception) {
            viewModel.triggerToast("Unable to open system confirmation dialog")
        }
    }

    // Legacy Android 9 and below WRITE_EXTERNAL_STORAGE launcher (requested only when renaming/deleting)
    val legacyWritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLegacyWritePermissionResult(
            context = context,
            granted = granted,
            onLaunchIntentSender = launchIntentSender
        )
    }

    val defaultHomeTab = if (BottomTab.VIDEOS in uiState.activeTabs) {
        BottomTab.VIDEOS
    } else {
        uiState.activeTabs.firstOrNull() ?: BottomTab.VIDEOS
    }

    // Back navigation handler
    BackHandler(
        enabled = uiState.isPillEditMode ||
            uiState.resumePromptTarget != null ||
            uiState.isSelectionMode ||
            uiState.searchOpen ||
            uiState.meSubPage != MeSubPage.NONE ||
            uiState.selectedFolderFilter != null ||
            uiState.selectedPlaylistFilter != null ||
            uiState.bottomTab != defaultHomeTab
    ) {
        when {
            uiState.isPillEditMode -> viewModel.setPillEditMode(false)
            uiState.resumePromptTarget != null -> viewModel.dismissResumePrompt()
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.searchOpen -> viewModel.setSearchOpen(false)
            uiState.meSubPage != MeSubPage.NONE -> viewModel.setMeSubPage(MeSubPage.NONE)
            uiState.selectedFolderFilter != null -> viewModel.selectFolderFilter(null)
            uiState.selectedPlaylistFilter != null -> viewModel.selectPlaylistFilter(null)
            uiState.bottomTab != defaultHomeTab -> viewModel.setBottomTab(defaultHomeTab, context)
        }
    }

    val contentGraphicsLayer = rememberGraphicsLayer()
    val pullToRefreshState = rememberPullToRefreshState()

    // ---- Audio: mini player pill + Now Playing screen ----
    val audioManager = remember(context) { AudioPlaybackManager.getInstance(context) }
    val audioState by audioManager.state.collectAsStateWithLifecycle()
    val libraryPrefs by remember(context) {
        AppPreferencesRepository.observeLibraryPreferences(context)
    }.collectAsStateWithLifecycle(initialValue = LibraryPreferences())
    val audioUiScope = rememberCoroutineScope()
    val miniTrack = uiState.playingAudio
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var nowPlayingInfoTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    var lastNowPlayingTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    LaunchedEffect(miniTrack) {
        if (miniTrack != null) {
            lastNowPlayingTrack = miniTrack
        } else {
            showNowPlaying = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(outerChromeBg)
            .testTag("main_player_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .drawWithContent {
                    contentGraphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(contentGraphicsLayer)
                }
        ) {
            // 1. Top Header Bar (hidden when inside a Me sub-page, as each sub-page has its own Expressive header)
            val hideMainHeader = (uiState.bottomTab == BottomTab.ME && uiState.meSubPage != MeSubPage.NONE) ||
                (uiState.bottomTab == BottomTab.AUDIO && uiState.hasAudioPermission)
            if (!hideMainHeader) {
                M3ExpressiveHeader(
                    uiState = uiState,
                    effectiveAmoled = effectiveAmoled,
                    onClearSelection = { viewModel.clearSelection() },
                    onToggleSelectAll = { viewModel.toggleSelectAll() },
                    onOpenSearch = { viewModel.setSearchOpen(true) },
                    onCloseSearch = { viewModel.setSearchOpen(false) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onOpenSortSheet = { viewModel.openSortSheet() },
                    onClearSubFilter = {
                        viewModel.selectFolderFilter(null)
                        viewModel.selectPlaylistFilter(null)
                    }
                )
            }

            // 2. Main Content Area
            if (uiState.bottomTab == BottomTab.ME) {
                MeTabScreen(
                    uiState = uiState,
                    effectiveAmoled = effectiveAmoled,
                    onOpenSubPage = { subPage -> viewModel.setMeSubPage(subPage) },
                    onCloseSubPage = { viewModel.setMeSubPage(MeSubPage.NONE) },
                    onPlayVideo = { video -> viewModel.playVideo(video, context) },
                    onClearHistory = { viewModel.clearVideoHistory(context) },
                    onCreatePlaylist = { name -> viewModel.createPlaylist(name, context) },
                    onRenamePlaylist = { plId, newName -> viewModel.renamePlaylist(plId, newName, context) },
                    onDeletePlaylist = { plId -> viewModel.deletePlaylist(plId, context) },
                    onAddVideosToPlaylist = { plId, videoIds ->
                        viewModel.addVideosToPlaylistById(plId, videoIds, context)
                    },
                    onRemoveVideoFromPlaylist = { plId, videoId ->
                        viewModel.removeVideoFromPlaylist(plId, videoId, context)
                    },
                    onSelectThemeMode = { mode -> viewModel.setThemeMode(mode, context) },
                    onSetDynamicColor = { enabled -> viewModel.setDynamicColor(enabled, context) },
                    onSelectColorTheme = { themeId -> viewModel.setColorTheme(themeId, context) },
                    onSelectCustomColor = { colorArgb -> viewModel.setCustomColor(colorArgb, context) },
                    onToggleAmoledBlack = { viewModel.toggleAmoledBlack(context) },
                    onToggleShowResolutionTier = { viewModel.toggleShowResolutionTier(context) },
                    onUpdatePlaybackSettings = { transform ->
                        viewModel.updatePlaybackSettings(context, transform)
                    },
                    onSetTabActive = { tab, active ->
                        viewModel.setTabActive(tab, active, context)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshPermissionsAndScan(context, showSpinner = true) },
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = pullToRefreshState,
                            isRefreshing = uiState.isRefreshing,
                            containerColor = palette.secondaryContainer,
                            color = palette.onSecondaryContainer,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (uiState.bottomTab) {
                        BottomTab.AUDIO -> {
                            if (!uiState.hasAudioPermission) {
                                val permName = MediaStoreScanner.getAudioPermissionName()
                                val showSettings = uiState.audioPermissionAsked &&
                                    (activity == null || !ActivityCompat.shouldShowRequestPermissionRationale(activity, permName))
                                MediaPermissionPromptCard(
                                    title = "Audio access needed",
                                    explanation = "Grant audio permission so Videos can scan and play music stored on your device.",
                                    showOpenSettingsButton = uiState.audioPermissionAsked || showSettings,
                                    onGrantPermission = {
                                        audioPermissionLauncher.launch(permName)
                                    },
                                    onOpenSettings = {
                                        openAppSystemSettings(context)
                                    }
                                )
                            } else {
                                AudioLibraryScreen(
                                    tracks = uiState.audioTracks,
                                    playingAudio = uiState.playingAudio,
                                    isAudioPlaying = uiState.isAudioPlaying,
                                    audioProgress = uiState.audioProgress,
                                    amoledBlack = effectiveAmoled,
                                    headerBackground = outerChromeBg,
                                    extraBottomPadding = if (uiState.playingAudio != null) 84.dp else 0.dp,
                                    onPlayQueue = { queue, startIndex, shuffle ->
                                        runWithNotificationPermissionCheck {
                                            viewModel.playAudioQueue(context, queue, startIndex, shuffle)
                                        }
                                    },
                                    onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                                    onSeekAudio = { viewModel.seekAudio(it) },
                                    onSkipPrevious = { viewModel.skipAudioPrevious() },
                                    onSkipNext = { viewModel.skipAudioNext() },
                                    onPlayNext = { track -> audioManager.playNext(track) },
                                    onAddToQueue = { track -> audioManager.addToQueue(track) },
                                    onLibraryChanged = { viewModel.refreshPermissionsAndScan(context) }
                                )
                            }
                        }

                        BottomTab.VIDEOS, BottomTab.ME -> {
                            if (!uiState.hasVideoPermission) {
                                val permName = MediaStoreScanner.getVideoPermissionName()
                                val showSettings = uiState.videoPermissionAsked &&
                                    (activity == null || !ActivityCompat.shouldShowRequestPermissionRationale(activity, permName))
                                MediaPermissionPromptCard(
                                    title = "Video access needed",
                                    explanation = "Grant video permission so Videos can scan and play video files stored on your device.",
                                    showOpenSettingsButton = uiState.videoPermissionAsked || showSettings,
                                    onGrantPermission = {
                                        videoPermissionLauncher.launch(permName)
                                    },
                                    onOpenSettings = {
                                        openAppSystemSettings(context)
                                    }
                                )
                            } else {
                                when {
                                    uiState.viewMode == ViewMode.FOLDERS &&
                                        uiState.selectedFolderFilter == null &&
                                        uiState.selectedPlaylistFilter == null -> {
                                        FoldersLibraryView(
                                            videos = uiState.videos,
                                            playlists = uiState.playlists,
                                            amoledBlack = effectiveAmoled,
                                            onSelectFolder = { folder -> viewModel.selectFolderFilter(folder) },
                                            onSelectPlaylist = { plId -> viewModel.selectPlaylistFilter(plId) }
                                        )
                                    }

                                    uiState.viewMode == ViewMode.TREE &&
                                        uiState.selectedFolderFilter == null &&
                                        uiState.selectedPlaylistFilter == null -> {
                                        TreeHierarchyView(
                                            videos = uiState.filteredAndSortedVideos,
                                            expandedNodes = uiState.expandedTreeNodes,
                                            showResolutionTier = uiState.showResolutionTier,
                                            onToggleNode = { viewModel.toggleTreeNode(it) },
                                            onPlayVideo = { viewModel.playVideo(it, context) },
                                            onOpenMenu = { viewModel.openVideoMenu(it) }
                                        )
                                    }

                                    else -> {
                                        VideosContentListOrGrid(
                                            uiState = uiState,
                                            effectiveAmoled = effectiveAmoled,
                                            onVideoClick = { video ->
                                                if (uiState.isSelectionMode) {
                                                    viewModel.toggleSelectId(video.id)
                                                } else {
                                                    viewModel.playVideo(video, context)
                                                }
                                            },
                                            onVideoLongClick = { video ->
                                                viewModel.clearAllOverlays()
                                                viewModel.toggleSelectId(video.id)
                                            },
                                            onVideoMenuClick = { video ->
                                                viewModel.openVideoMenu(video)
                                            },
                                            onRescan = { viewModel.refreshPermissionsAndScan(context, showSpinner = true) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Floating M3 Expressive Pill Toolbar (Bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isSelectionMode) {
                MultiSelectContextualPill(
                    onShareSelected = {
                        val selectedVideos = uiState.videos.filter { it.id in uiState.selectedIds }
                        shareVideosContentUri(context = context, videos = selectedVideos)
                        viewModel.clearSelection()
                    },
                    onAddToSelected = {
                        viewModel.openPlaylistSheetForIds(uiState.selectedIds)
                    },
                    onDeleteSelected = {
                        val selectedVideos = uiState.videos.filter { it.id in uiState.selectedIds }
                        viewModel.requestDeleteVideos(
                            context = context,
                            targetVideos = selectedVideos,
                            onLaunchIntentSender = launchIntentSender,
                            onRequestLegacyWritePermission = {
                                legacyWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        )
                    }
                )
            } else {
                MainNavigationPill(
                    bottomTab = uiState.bottomTab,
                    activeTabs = uiState.activeTabs,
                    isEditMode = uiState.isPillEditMode,
                    contentGraphicsLayer = contentGraphicsLayer,
                    onSelectTab = { viewModel.setBottomTab(it, context) },
                    onEnterEditMode = { viewModel.setPillEditMode(true) },
                    onExitEditMode = { viewModel.setPillEditMode(false) },
                    onSetTabActive = { tab, active -> viewModel.setTabActive(tab, active, context) }
                )
            }
        }

        // 3b. Pill mini player (Audio tab) above the floating navigation pill
        if (uiState.bottomTab == BottomTab.AUDIO &&
            uiState.hasAudioPermission &&
            miniTrack != null &&
            !showNowPlaying &&
            !uiState.isSelectionMode &&
            !uiState.isPillEditMode
        ) {
            AlbumArtTheme(
                track = miniTrack,
                applyScheme = libraryPrefs.nowPlayingColorSource == NowPlayingColorSource.ALBUM_ART,
                extractColors = false
            ) {
            AudioMiniPlayer(
                track = miniTrack,
                isPlaying = uiState.isAudioPlaying,
                onOpen = { showNowPlaying = true },
                onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                onSkipPrevious = { viewModel.skipAudioPrevious() },
                onSkipNext = { viewModel.skipAudioNext() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
            )
            }
        }

        // 3c. Now Playing screen (slides up over everything except the video player)
        AnimatedVisibility(
            visible = showNowPlaying && miniTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val nowTrack = miniTrack ?: lastNowPlayingTrack
            if (nowTrack != null) {
                AlbumArtTheme(
                    track = nowTrack,
                    applyScheme = libraryPrefs.nowPlayingColorSource == NowPlayingColorSource.ALBUM_ART,
                    extractColors = libraryPrefs.nowPlayingBackground == NowPlayingBackground.GRADIENT
                ) {
                NowPlayingScreen(
                    track = nowTrack,
                    isPlaying = uiState.isAudioPlaying,
                    positionMs = audioState.positionMs,
                    durationMs = audioState.durationMs,
                    shuffleEnabled = audioState.shuffleEnabled,
                    repeatMode = audioState.repeatMode,
                    liked = nowTrack.id in libraryPrefs.likedIds,
                    trayActions = libraryPrefs.visibleTray,
                    onClose = { showNowPlaying = false },
                    onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                    onSeekTo = { viewModel.seekAudio(it) },
                    onSkipPrevious = { viewModel.skipAudioPrevious() },
                    onSkipNext = { viewModel.skipAudioNext() },
                    onToggleShuffle = { audioManager.toggleShuffle() },
                    onCycleRepeat = { audioManager.cycleRepeat() },
                    onToggleLike = {
                        val liked = nowTrack.id in libraryPrefs.likedIds
                        audioUiScope.launch {
                            AppPreferencesRepository.setTrackLiked(context, nowTrack.id, !liked)
                        }
                    },
                    onLyrics = {
                        android.widget.Toast.makeText(context, "Lyrics are coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    sleepTimerRemainingMs = audioState.sleepTimerRemainingMs,
                    sleepAtEndOfSong = audioState.sleepAtEndOfSong,
                    onStartSleepTimer = { minutes -> audioManager.startSleepTimer(minutes) },
                    onSleepAtEndOfSong = { audioManager.startSleepAtEndOfSong() },
                    onCancelSleepTimer = { audioManager.cancelSleepTimer() },
                    background = libraryPrefs.nowPlayingBackground,
                    buttonColors = libraryPrefs.nowPlayingButtonColors
                )
                }
            }
        }

        nowPlayingInfoTrack?.let { infoTrack ->
            SongInfoSheet(track = infoTrack, onDismiss = { nowPlayingInfoTrack = null })
        }

        // 4. Overlays, Menus, Bottom Sheets & Dialogs
        if (uiState.resumePromptTarget != null) {
            val promptTarget = uiState.resumePromptTarget
            ResumePlaybackBottomSheet(
                target = promptTarget,
                onResumeFromSaved = {
                    viewModel.startVideoPlaybackAt(
                        video = promptTarget.video,
                        startPositionMs = promptTarget.resumePositionMs,
                        context = context
                    )
                },
                onStartFromBeginning = {
                    viewModel.startVideoPlaybackAt(
                        video = promptTarget.video,
                        startPositionMs = 0L,
                        context = context
                    )
                },
                onDismiss = { viewModel.dismissResumePrompt() }
            )
        }

        if (uiState.activeMenuVideo != null) {
            val menuVideo = uiState.activeMenuVideo
            ColorOsFloatingMenuOverlay(
                video = menuVideo,
                onAddToPlaylist = { viewModel.openPlaylistSheetForIds(listOf(menuVideo.id)) },
                onRename = { viewModel.openRenameDialog(menuVideo) },
                onShare = {
                    viewModel.clearAllOverlays()
                    shareVideosContentUri(context, listOf(menuVideo))
                },
                onDelete = {
                    viewModel.requestDeleteVideos(
                        context = context,
                        targetVideos = listOf(menuVideo),
                        onLaunchIntentSender = launchIntentSender,
                        onRequestLegacyWritePermission = {
                            legacyWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    )
                },
                onSelect = { viewModel.selectSingleFromMenu(menuVideo) },
                onDetails = { viewModel.openDetailsSheet(menuVideo) },
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        if (uiState.sortSheetOpen && uiState.bottomTab == BottomTab.VIDEOS) {
            SortAndViewBottomSheet(
                smartGroups = uiState.smartGroups,
                viewMode = uiState.viewMode,
                layoutMode = uiState.layoutMode,
                sortBy = uiState.sortBy,
                sortDir = uiState.sortDir,
                showResolutionTier = uiState.showResolutionTier,
                onToggleSmartGroups = { viewModel.toggleSmartGroups(context) },
                onSelectViewMode = { viewModel.setViewMode(it, context) },
                onSelectLayoutMode = { viewModel.setLayoutMode(it, context) },
                onToggleShowResolutionTier = { viewModel.toggleShowResolutionTier(context) },
                onSelectSort = { viewModel.handleSortOptionClick(it, context) },
                onDismiss = { viewModel.clearAllOverlays() },
                isAudioTab = uiState.bottomTab == BottomTab.AUDIO
            )
        }

        if (uiState.detailsVideo != null) {
            ColorOsDetailsSheet(
                video = uiState.detailsVideo,
                onCopyPath = { path ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Video Path", path))
                    viewModel.triggerToast("Path copied")
                },
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        if (uiState.renameTarget != null) {
            ColorOsRenameDialog(
                video = uiState.renameTarget,
                renameValue = uiState.renameValue,
                onValueChange = { viewModel.updateRenameValue(it) },
                onConfirm = {
                    viewModel.submitRenameRequest(
                        context = context,
                        onLaunchIntentSender = launchIntentSender,
                        onRequestLegacyWritePermission = {
                            legacyWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    )
                },
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        if (uiState.playlistTargetIds.isNotEmpty()) {
            AddToPlaylistDialog(
                playlists = uiState.playlists,
                onSelectPlaylistName = { plName -> viewModel.addTargetsToPlaylist(plName, context) },
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        // 5. Fullscreen Video Player Modal (Media3 ExoPlayer via PlayerController)
        if (uiState.playingVideo != null) {
            val activePlaying = uiState.playingVideo
            val controller = viewModel.getOrCreatePlayerController(context)
            VideoPlayerModal(
                video = activePlaying,
                playerController = controller,
                playbackSettings = uiState.playbackSettings,
                showResolutionTier = uiState.showResolutionTier,
                onSavePosition = { posMs, durMs, forcePersist ->
                    viewModel.recordVideoPlaybackHistory(
                        videoId = activePlaying.id,
                        positionMs = posMs,
                        durationMs = durMs,
                        forcePersist = forcePersist,
                        context = context
                    )
                },
                onPreviousVideo = { viewModel.playAdjacentVideo(-1, context) },
                onNextVideo = { viewModel.playAdjacentVideo(1, context) },
                onOpenDetails = {
                    viewModel.closeVideoPlayer(context)
                    viewModel.openDetailsSheet(activePlaying)
                },
                onClose = { viewModel.closeVideoPlayer(context) }
            )
        }

        // 6. Floating M3 Toast Banner
        AnimatedVisibility(
            visible = uiState.toastMsg != null,
            enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                scaleIn(initialScale = 0.92f, animationSpec = motionScheme.fastSpatialSpec()),
            exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                scaleOut(targetScale = 0.92f, animationSpec = motionScheme.fastSpatialSpec()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 104.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = palette.onSurface,
                shadowElevation = 12.dp,
                modifier = Modifier.testTag("toast_banner")
            ) {
                Text(
                    text = uiState.toastMsg.orEmpty(),
                    color = palette.surface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3ExpressiveHeader(
    uiState: VideoUiState,
    effectiveAmoled: Boolean,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSortSheet: () -> Unit,
    onClearSubFilter: () -> Unit
) {
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    val outerChromeBg = if (effectiveAmoled) Color.Black else MaterialTheme.colorScheme.background
    val headerBg by animateColorAsState(
        targetValue = if (uiState.isSelectionMode) palette.surfaceContainerHigh else outerChromeBg,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "headerBg"
    )

    val activePlaylistName = uiState.playlists.find { it.id == uiState.selectedPlaylistFilter }?.name
    val activeSubTitle = uiState.selectedFolderFilter ?: activePlaylistName

    val titleText = when {
        activeSubTitle != null -> activeSubTitle
        uiState.bottomTab == BottomTab.AUDIO -> "Audio"
        uiState.bottomTab == BottomTab.ME -> "Me"
        else -> "Videos"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        when {
            uiState.isSelectionMode -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("selection_mode_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onClearSelection,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier
                                .size(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("clear_selection_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear selection",
                                tint = palette.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "${uiState.selectedIds.size}",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val allSelected = uiState.selectedIds.size == uiState.filteredAndSortedVideos.size &&
                        uiState.filteredAndSortedVideos.isNotEmpty()
                    TextButton(
                        onClick = onToggleSelectAll,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("select_all_btn")
                    ) {
                        Text(
                            text = if (allSelected) "Deselect all" else "Select all",
                            color = palette.primary,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            uiState.searchOpen -> {
                val searchFocusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                LaunchedEffect(uiState.searchOpen) {
                    if (uiState.searchOpen) {
                        searchFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = palette.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("search_bar_container")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = palette.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search videos",
                                    color = palette.outline,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 16.sp
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = palette.onSurface,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(palette.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .testTag("search_input_field")
                            )
                        }
                        IconButton(
                            onClick = onCloseSearch,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = palette.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (activeSubTitle != null) {
                            IconButton(
                                onClick = onClearSubFilter,
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("back_from_folder_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = palette.onSurface
                                )
                            }
                        }
                        Text(
                            text = titleText,
                            color = palette.onSurface,
                            style = MaterialTheme.typography.headlineLargeEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (uiState.bottomTab != BottomTab.ME) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onOpenSearch,
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("header_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search videos",
                                    tint = palette.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            if (uiState.bottomTab == BottomTab.VIDEOS) {
                                IconButton(
                                    onClick = onOpenSortSheet,
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("header_sort_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Sort and view options",
                                        tint = palette.onSurfaceVariant,
                                        modifier = Modifier.size(21.dp)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideosContentListOrGrid(
    uiState: VideoUiState,
    effectiveAmoled: Boolean,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onVideoMenuClick: (VideoItem) -> Unit,
    onRescan: () -> Unit
) {
    val palette = LocalM3Palette.current
    val videos = uiState.filteredAndSortedVideos

    if (videos.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("videos_empty_state"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(palette.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (uiState.searchQuery.isNotBlank()) "No matching videos found" else "No videos found on device",
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (uiState.searchQuery.isNotBlank()) {
                    "Try a different search term."
                } else {
                    "Videos saved to your device storage will automatically appear here."
                },
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            FilledTonalButton(
                onClick = onRescan,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.surfaceContainerHigh,
                    contentColor = palette.primary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier.testTag("rescan_media_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rescan storage",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    if (uiState.layoutMode == LayoutMode.GRID) {
        val seriesData = uiState.smartSeriesData
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .testTag("videos_grid_container"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 128.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.smartGroups && seriesData.groups.isNotEmpty()) {
                seriesData.groups.forEach { (seriesTitle, eps) ->
                    item(span = { GridItemSpan(2) }, key = "header_$seriesTitle") {
                        SeriesGroupHeader(seriesTitle = seriesTitle)
                    }
                    items(eps, key = { it.id }) { ep ->
                        VideoGridItem(
                            video = ep,
                            isSelected = ep.id in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            amoledBlack = effectiveAmoled,
                            showResolutionTier = uiState.showResolutionTier,
                            badgeOverride = ep.episode?.let { "EP $it" },
                            onClick = { onVideoClick(ep) },
                            onLongClick = { onVideoLongClick(ep) },
                            onMenuClick = { onVideoMenuClick(ep) }
                        )
                    }
                }
                if (seriesData.standalone.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }, key = "header_standalone") {
                        Text(
                            text = "Other Videos",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    items(seriesData.standalone, key = { it.id }) { v ->
                        VideoGridItem(
                            video = v,
                            isSelected = v.id in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            amoledBlack = effectiveAmoled,
                            showResolutionTier = uiState.showResolutionTier,
                            onClick = { onVideoClick(v) },
                            onLongClick = { onVideoLongClick(v) },
                            onMenuClick = { onVideoMenuClick(v) }
                        )
                    }
                }
            } else {
                items(videos, key = { it.id }) { v ->
                    VideoGridItem(
                        video = v,
                        isSelected = v.id in uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        amoledBlack = effectiveAmoled,
                        showResolutionTier = uiState.showResolutionTier,
                        onClick = { onVideoClick(v) },
                        onLongClick = { onVideoLongClick(v) },
                        onMenuClick = { onVideoMenuClick(v) }
                    )
                }
            }
        }
    } else {
        val seriesData = uiState.smartSeriesData
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("videos_list_container"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (uiState.smartGroups && seriesData.groups.isNotEmpty()) {
                seriesData.groups.forEach { (seriesTitle, eps) ->
                    item(key = "series_header_$seriesTitle") {
                        SeriesGroupHeader(
                            seriesTitle = seriesTitle,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(eps, key = { _, ep -> ep.id }) { idx, ep ->
                        VideoListItem(
                            video = ep,
                            index = idx,
                            totalCount = eps.size,
                            isSelected = ep.id in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            amoledBlack = effectiveAmoled,
                            showResolutionTier = uiState.showResolutionTier,
                            badgeOverride = ep.episode?.let { "EP $it" },
                            onClick = { onVideoClick(ep) },
                            onLongClick = { onVideoLongClick(ep) },
                            onMenuClick = { onVideoMenuClick(ep) }
                        )
                    }
                }
                if (seriesData.standalone.isNotEmpty()) {
                    item(key = "standalone_header") {
                        Text(
                            text = "Other Videos",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(seriesData.standalone, key = { _, v -> v.id }) { idx, v ->
                        VideoListItem(
                            video = v,
                            index = idx,
                            totalCount = seriesData.standalone.size,
                            isSelected = v.id in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            amoledBlack = effectiveAmoled,
                            showResolutionTier = uiState.showResolutionTier,
                            onClick = { onVideoClick(v) },
                            onLongClick = { onVideoLongClick(v) },
                            onMenuClick = { onVideoMenuClick(v) }
                        )
                    }
                }
            } else {
                itemsIndexed(videos, key = { _, v -> v.id }) { idx, v ->
                    VideoListItem(
                        video = v,
                        index = idx,
                        totalCount = videos.size,
                        isSelected = v.id in uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        amoledBlack = effectiveAmoled,
                        showResolutionTier = uiState.showResolutionTier,
                        onClick = { onVideoClick(v) },
                        onLongClick = { onVideoLongClick(v) },
                        onMenuClick = { onVideoMenuClick(v) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesGroupHeader(
    seriesTitle: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = seriesTitle,
            color = palette.onSurface,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultiSelectContextualPill(
    onShareSelected: () -> Unit,
    onAddToSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val palette = LocalM3Palette.current

    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = palette.surfaceContainerHighest,
            toolbarContentColor = palette.onSurface
        ),
        modifier = Modifier.testTag("multi_select_pill_toolbar")
    ) {
        ContextualPillButton(
            icon = Icons.Default.Share,
            label = "Share",
            tint = palette.onSurface,
            onClick = onShareSelected,
            testTag = "pill_share_btn"
        )
        ContextualPillButton(
            icon = Icons.Default.CreateNewFolder,
            label = "Add to",
            tint = palette.onSurface,
            onClick = onAddToSelected,
            testTag = "pill_add_to_btn"
        )
        ContextualPillButton(
            icon = Icons.Default.DeleteOutline,
            label = "Delete",
            tint = palette.error,
            onClick = onDeleteSelected,
            testTag = "pill_delete_btn"
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContextualPillButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    TextButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        modifier = Modifier
            .widthIn(min = 84.dp)
            .height(54.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = tint,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainNavigationPill(
    bottomTab: BottomTab,
    activeTabs: List<BottomTab>,
    isEditMode: Boolean,
    contentGraphicsLayer: GraphicsLayer,
    onSelectTab: (BottomTab) -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onSetTabActive: (BottomTab, Boolean) -> Unit
) {
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    var pillTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }

    val allFixedTabs = remember {
        listOf(
            Triple(BottomTab.AUDIO, "Audio", Icons.Default.MusicNote),
            Triple(BottomTab.VIDEOS, "Videos", Icons.Default.PlayArrow),
            Triple(BottomTab.ME, "Me", Icons.Default.Person)
        )
    }

    val visibleTabs = if (isEditMode) {
        allFixedTabs
    } else {
        allFixedTabs.filter { (tab, _, _) -> tab in activeTabs }.ifEmpty {
            listOf(Triple(BottomTab.VIDEOS, "Videos", Icons.Default.PlayArrow))
        }
    }

    val pillShape = CircleShape

    Box(
        modifier = Modifier
            .height(66.dp)
            .onGloballyPositioned { coords ->
                pillTopLeftInRoot = coords.positionInRoot()
            }
            .shadow(20.dp, pillShape, ambientColor = Color.Black.copy(alpha = 0.25f))
            .clip(pillShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        palette.outlineVariant.copy(alpha = 0.45f)
                    )
                ),
                shape = pillShape
            )
            .testTag("main_navigation_pill"),
        contentAlignment = Alignment.Center
    ) {
        // Frosted glass backdrop blur of the content scrolling behind the pill
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .blur(26.dp)
                .drawBehind {
                    translate(
                        left = -pillTopLeftInRoot.x,
                        top = -pillTopLeftInRoot.y
                    ) {
                        drawLayer(contentGraphicsLayer)
                    }
                }
        )

        // See-through frosted glass tint overlay
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.surfaceContainerHigh.copy(alpha = 0.68f),
                            palette.surfaceContainer.copy(alpha = 0.76f)
                        )
                    )
                )
        )

        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = Color.Transparent,
                toolbarContentColor = palette.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            visibleTabs.forEach { (tab, label, icon) ->
                val isTabEnabledInNav = tab in activeTabs
                val isSelected = !isEditMode && bottomTab == tab
                val isLastRemainingActive = isTabEnabledInNav && activeTabs.size <= 1

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.90f else 1f,
                    animationSpec = motionScheme.fastSpatialSpec(),
                    label = "pillItemScale_${tab.id}"
                )

                val horizontalPad by animateDpAsState(
                    targetValue = when {
                        isEditMode -> 14.dp
                        isSelected -> 20.dp
                        else -> 14.dp
                    },
                    animationSpec = motionScheme.fastSpatialSpec(),
                    label = "navPillPad_${tab.id}"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(pillShape)
                            .background(
                                when {
                                    isSelected -> palette.secondaryContainer.copy(alpha = 0.92f)
                                    isEditMode && isTabEnabledInNav -> palette.surfaceContainerHighest.copy(alpha = 0.65f)
                                    else -> Color.Transparent
                                }
                            )
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (isEditMode) {
                                        if (isTabEnabledInNav) {
                                            if (!isLastRemainingActive) {
                                                onSetTabActive(tab, false)
                                            }
                                        } else {
                                            onSetTabActive(tab, true)
                                        }
                                    } else {
                                        onSelectTab(tab)
                                    }
                                },
                                onLongClick = {
                                    if (!isEditMode) {
                                        onEnterEditMode()
                                    }
                                }
                            )
                            .padding(horizontal = horizontalPad)
                            .alpha(
                                when {
                                    isEditMode && isLastRemainingActive -> 0.42f
                                    isEditMode && !isTabEnabledInNav -> 0.48f
                                    else -> 1f
                                }
                            )
                            .testTag("bottom_tab_${tab.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) palette.onSecondaryContainer else palette.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = {
                                (fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                                    scaleIn(
                                        initialScale = 0.85f,
                                        animationSpec = motionScheme.fastSpatialSpec()
                                    )) togetherWith
                                    (fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                                        scaleOut(
                                            targetScale = 0.85f,
                                            animationSpec = motionScheme.fastSpatialSpec()
                                        ))
                            },
                            label = "navLabel_${tab.id}"
                        ) { showLabel ->
                            if (showLabel) {
                                Row {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        color = palette.onSecondaryContainer,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Edit mode badge
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode && (!isTabEnabledInNav || !isLastRemainingActive),
                        enter = scaleIn(animationSpec = motionScheme.fastSpatialSpec()) +
                            fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                        exit = scaleOut(animationSpec = motionScheme.fastSpatialSpec()) +
                            fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    ) {
                        val isRemoveBadge = isTabEnabledInNav
                        val badgeColor = if (isRemoveBadge) Color(0xFFE53935) else Color(0xFF1E88E5)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable {
                                    if (isRemoveBadge) {
                                        if (!isLastRemainingActive) {
                                            onSetTabActive(tab, false)
                                        }
                                    } else {
                                        onSetTabActive(tab, true)
                                    }
                                }
                                .testTag("pill_edit_badge_${tab.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRemoveBadge) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isRemoveBadge) "Remove $label" else "Add $label",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // "Done" button to exit edit mode
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn(animationSpec = motionScheme.fastSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                exit = scaleOut(animationSpec = motionScheme.fastSpatialSpec()) +
                    fadeOut(animationSpec = motionScheme.fastEffectsSpec())
            ) {
                Button(
                    onClick = onExitEditMode,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .height(42.dp)
                        .testTag("pill_edit_done_btn")
                ) {
                    Text(
                        text = "Done",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun shareVideosContentUri(context: Context, videos: List<VideoItem>) {
    if (videos.isEmpty()) return
    try {
        val intent = if (videos.size == 1) {
            val single = videos.first()
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, single.contentUri)
                clipData = ClipData.newRawUri(single.displayName, single.contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uriList = ArrayList<Uri>(videos.map { it.contentUri })
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share video").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}

private fun openAppSystemSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
