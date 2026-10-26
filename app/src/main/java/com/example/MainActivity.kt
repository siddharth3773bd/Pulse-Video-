package com.example

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import com.example.ui.icons.Symbols
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Snackbar
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.items
import com.example.ui.components.LibrarySearchInputField
import com.example.ui.components.TooltipIconButton
import android.Manifest
import android.app.Activity
import android.content.ClipData
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
import com.example.ui.components.VideoSearchView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.ui.components.AmbientModeScreen
import com.example.ui.components.LyricsScreen
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
import com.example.ui.components.AddVideosToPlaylistSheet
import com.example.ui.components.AudioLibraryScreen
import com.example.ui.components.VideoDetailsSheet
import com.example.ui.components.VideoActionsMenu
import com.example.ui.components.RenameVideoDialog
import com.example.ui.components.FoldersLibraryView
import com.example.ui.components.MeTabScreen
import com.example.ui.components.MediaPermissionPromptCard
import com.example.ui.components.SortAndViewBottomSheet
import com.example.ui.components.TreeHierarchyView
import com.example.ui.components.VideoGridItem
import com.example.ui.components.VideoListItem
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.AlbumArtTheme
import com.example.model.NowPlayingBackground
import com.example.model.NowPlayingColorSource
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.graphicsLayer
import android.widget.Toast
import com.example.model.LibraryTab
import com.example.ui.components.LibraryOpenRequest
import com.example.ui.theme.DopplerTheme
import com.example.viewmodel.VideoPlayerViewModel
import com.example.viewmodel.VideoUiState

class MainActivity : ComponentActivity() {

    companion object {
        /** Set by the pop-up player's "Open in Doppler": show the Now Playing screen. */
        const val EXTRA_OPEN_NOW_PLAYING = "com.example.extra.OPEN_NOW_PLAYING"
    }

    private lateinit var externalViewModel: VideoPlayerViewModel

    /** Goes up by one each time something asks for Now Playing to open. */
    private val openNowPlayingSignal = androidx.compose.runtime.mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Same instance that the compose viewModel() call below returns for this activity.
        externalViewModel = androidx.lifecycle.ViewModelProvider(this)[VideoPlayerViewModel::class.java]
        externalViewModel.initializeAndScan(this)
        if (savedInstanceState == null) {
            handleExternalVideoIntent(intent)
            handleOpenNowPlaying(intent)
        }
        holdSplashUntilReady()
        setContent {
            val viewModel: VideoPlayerViewModel = viewModel()
            val context = LocalContext.current
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DopplerTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.dynamicColor,
                paletteId = uiState.colorTheme,
                customColorArgb = uiState.customColorArgb,
                amoledBlack = uiState.amoledBlack
            ) {
                // Text and icons default to the theme's on-surface colour everywhere (without this,
                // text outside a Surface fell back to black and was unreadable on dark screens).
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
                ) {
                    Material3VideoPlayerScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        openNowPlayingSignal = openNowPlayingSignal.intValue
                    )
                    // If the app closed because of an error last time, show that error once.
                    var crashReport by remember { mutableStateOf(com.example.crash.CrashReporter.takeLastReport(context)) }
                    crashReport?.let { report ->
                        com.example.ui.components.CrashReportDialog(report = report, onDismiss = { crashReport = null })
                    }
                }
            }
        }
    }

    /**
     * Keeps Android's splash screen up until the first screen is ready (settings read and the
     * library scanned), so the permission card and a loading spinner don't flash in between.
     * Never longer than 1.5 s.
     */
    private fun holdSplashUntilReady() {
        val content = findViewById<android.view.View>(android.R.id.content)
        val start = android.os.SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val s = externalViewModel.uiState.value
                val ready = (s.prefsLoaded && (s.videosLoaded || !s.hasVideoPermission)) ||
                    s.playingVideo != null ||
                    android.os.SystemClock.uptimeMillis() - start > 1_500L
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalVideoIntent(intent)
        handleOpenNowPlaying(intent)
    }

    private fun handleOpenNowPlaying(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
            intent.removeExtra(EXTRA_OPEN_NOW_PLAYING)
            openNowPlayingSignal.intValue++
        }
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
    viewModel: VideoPlayerViewModel,
    openNowPlayingSignal: Int = 0
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
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

    // Ask for a tab's permission as soon as that tab is shown (once per app session), instead of
    // waiting for "Grant permission" to be tapped. If it is denied, the tab keeps its prompt card.
    var autoAskedVideoPermission by rememberSaveable { mutableStateOf(false) }
    var autoAskedAudioPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.bottomTab, uiState.prefsLoaded) {
        if (!uiState.prefsLoaded) return@LaunchedEffect
        fun isGranted(name: String) = androidx.core.content.ContextCompat.checkSelfPermission(
            context, name
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        when (uiState.bottomTab) {
            BottomTab.VIDEOS -> if (!autoAskedVideoPermission) {
                autoAskedVideoPermission = true
                val name = MediaStoreScanner.getVideoPermissionName()
                if (!isGranted(name)) videoPermissionLauncher.launch(name)
            }
            BottomTab.AUDIO -> if (!autoAskedAudioPermission) {
                autoAskedAudioPermission = true
                val name = MediaStoreScanner.getAudioPermissionName()
                if (!isGranted(name)) audioPermissionLauncher.launch(name)
            }
            else -> Unit
        }
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
        enabled = uiState.isSelectionMode ||
            uiState.searchOpen ||
            uiState.meSubPage != MeSubPage.NONE ||
            uiState.selectedFolderFilter != null ||
            uiState.selectedPlaylistFilter != null ||
            uiState.bottomTab != defaultHomeTab
    ) {
        when {
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.searchOpen -> viewModel.setSearchOpen(false)
            uiState.meSubPage != MeSubPage.NONE -> viewModel.setMeSubPage(MeSubPage.NONE)
            uiState.selectedFolderFilter != null -> viewModel.selectFolderFilter(null)
            uiState.selectedPlaylistFilter != null -> viewModel.selectPlaylistFilter(null)
            uiState.bottomTab != defaultHomeTab -> viewModel.setBottomTab(defaultHomeTab, context)
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    // Videos header: the official search app bar (scrolls away, comes back on scroll up) with the
    // official full-screen search. Selection and folder views get a pinned top app bar instead.
    val videosSearchHeader = uiState.bottomTab == BottomTab.VIDEOS && uiState.hasVideoPermission &&
        !uiState.isSelectionMode && uiState.selectedFolderFilter == null && uiState.selectedPlaylistFilter == null
    val videosSearchScroll = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    LaunchedEffect(uiState.bottomTab, uiState.selectedFolderFilter, uiState.viewMode, uiState.isSelectionMode) {
        videosSearchScroll.scrollState.scrollOffset = 0f
    }
    val videoSearchState = rememberContainedSearchBarState(
        animationSpecForExpand = MaterialTheme.motionScheme.slowSpatialSpec(),
        animationSpecForCollapse = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val videoSearchText = rememberTextFieldState()
    val headerScope = rememberCoroutineScope()

    // Long-press the navigation bar: choose which tabs show.
    var showTabsSheet by remember { mutableStateOf(false) }

    // ---- Audio: mini player pill + Now Playing screen ----
    val audioManager = remember(context) { AudioPlaybackManager.getInstance(context) }
    val audioState by audioManager.state.collectAsStateWithLifecycle()
    val playbackError by audioManager.lastError.collectAsStateWithLifecycle()
    val libraryPrefs by remember(context) {
        AppPreferencesRepository.observeLibraryPreferences(context)
    }.collectAsStateWithLifecycle(initialValue = LibraryPreferences())
    val audioUiScope = rememberCoroutineScope()
    val miniTrack = uiState.playingAudio
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    // Ambient mode: opened from the Now Playing tray, or automatically when the phone is turned
    // sideways on Now Playing (unless closed while sideways; turning upright resets that).
    var ambientRequested by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showNowPlaying) { if (!showNowPlaying) showLyrics = false }
    var ambientDismissedSideways by rememberSaveable { mutableStateOf(false) }
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(isLandscape) { if (!isLandscape) ambientDismissedSideways = false }
    LaunchedEffect(showNowPlaying) { if (!showNowPlaying) ambientRequested = false }
    val showAmbient = showNowPlaying && miniTrack != null && (
        ambientRequested ||
            (isLandscape && libraryPrefs.ambient.openOnRotate && !ambientDismissedSideways)
        )
    // Long-press selection in the Music tab: its own toolbar replaces the nav pill and mini player.
    var audioSelectionActive by remember { mutableStateOf(false) }
    var nowPlayingInfoTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    // Now Playing ⋮: add the song to a playlist (pick one, or name a new one).
    var nowPlayingAddToPlaylist by remember { mutableStateOf<AudioTrackItem?>(null) }
    var nowPlayingNewPlaylistFor by remember { mutableStateOf<AudioTrackItem?>(null) }
    // Now Playing ⋮ › Go to album / artist: the Music tab opens that page.
    var libraryOpenRequest by remember { mutableStateOf<LibraryOpenRequest?>(null) }
    var lastNowPlayingTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    LaunchedEffect(miniTrack) {
        if (miniTrack != null) {
            lastNowPlayingTrack = miniTrack
        } else {
            showNowPlaying = false
        }
    }
    // "Open in Doppler" from the pop-up player: Music tab with Now Playing open, once the song is here.
    val miniTrackLatest by rememberUpdatedState(miniTrack)
    LaunchedEffect(openNowPlayingSignal) {
        if (openNowPlayingSignal == 0) return@LaunchedEffect
        val ready = kotlinx.coroutines.withTimeoutOrNull(3_000L) {
            androidx.compose.runtime.snapshotFlow { miniTrackLatest }.first { it != null }
        }
        if (ready != null) {
            viewModel.setBottomTab(BottomTab.AUDIO, context)
            showNowPlaying = true
        }
    }

    /** The ⋮ menu of a video, anchored to its own button. */
    val videoMenu: @Composable (VideoItem) -> Unit = { menuVideo ->
        VideoActionsMenu(
            expanded = uiState.activeMenuVideo?.id == menuVideo.id,
            onDismiss = { viewModel.clearAllOverlays() },
            onAddToPlaylist = { viewModel.openPlaylistSheetForIds(listOf(menuVideo.id)) },
            onRename = { viewModel.openRenameDialog(menuVideo) },
            onShare = {
                viewModel.clearAllOverlays()
                shareVideosContentUri(context, listOf(menuVideo))
            },
            onDetails = { viewModel.openDetailsSheet(menuVideo) },
            onDelete = {
                viewModel.requestDeleteVideos(
                    context = context,
                    targetVideos = listOf(menuVideo),
                    onLaunchIntentSender = launchIntentSender,
                    onRequestLegacyWritePermission = {
                        legacyWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                )
            }
        )
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
        ) {
            // 1. Header (Me sub-pages and the Music library draw their own)
            val hideMainHeader = (uiState.bottomTab == BottomTab.ME && uiState.meSubPage != MeSubPage.NONE) ||
                (uiState.bottomTab == BottomTab.AUDIO && uiState.hasAudioPermission)
            if (!hideMainHeader) {
                MainHeader(
                    uiState = uiState,
                    containerColor = outerChromeBg,
                    videosSearchHeader = videosSearchHeader,
                    searchState = videoSearchState,
                    searchText = videoSearchText,
                    searchScrollBehavior = videosSearchScroll,
                    onClearSelection = { viewModel.clearSelection() },
                    onToggleSelectAll = { viewModel.toggleSelectAll() },
                    onOpenSortSheet = { viewModel.openSortSheet() },
                    onClearSubFilter = {
                        viewModel.selectFolderFilter(null)
                        viewModel.selectPlaylistFilter(null)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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
                    onUpdatePlaybackSettings = { transform ->
                        viewModel.updatePlaybackSettings(context, transform)
                    },
                    onSetTabActive = { tab, active ->
                        viewModel.setTabActive(tab, active, context)
                    },
                    modifier = Modifier.fillMaxSize()
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
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (videosSearchHeader) {
                                Modifier.nestedScroll(videosSearchScroll.nestedScrollConnection)
                            } else Modifier
                        )
                ) {
                    when (uiState.bottomTab) {
                        BottomTab.AUDIO -> {
                            if (!uiState.hasAudioPermission) {
                                val permName = MediaStoreScanner.getAudioPermissionName()
                                val showSettings = uiState.audioPermissionAsked &&
                                    (activity == null || !ActivityCompat.shouldShowRequestPermissionRationale(activity, permName))
                                MediaPermissionPromptCard(
                                    title = "Audio access needed",
                                    explanation = "Grant audio permission so Doppler can scan and play music stored on your device.",
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
                                    headerBackground = outerChromeBg,
                                    extraBottomPadding = if (uiState.playingAudio != null) 88.dp else 0.dp,
                                    onPlayQueue = { queue, startIndex, shuffle ->
                                        runWithNotificationPermissionCheck {
                                            viewModel.playAudioQueue(context, queue, startIndex, shuffle)
                                        }
                                    },
                                    onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                                    onPlayNext = { list -> audioManager.playNext(list) },
                                    onAddToQueue = { list -> audioManager.addToQueue(list) },
                                    onLibraryChanged = { viewModel.refreshPermissionsAndScan(context) },
                                    onSelectionModeChange = { audioSelectionActive = it },
                                    openRequest = libraryOpenRequest,
                                    onOpenRequestHandled = { libraryOpenRequest = null }
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
                                    explanation = "Grant video permission so Doppler can scan and play video files stored on your device.",
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
                                            onSelectFolder = { folder -> viewModel.selectFolderFilter(folder) }
                                        )
                                    }

                                    uiState.viewMode == ViewMode.TREE &&
                                        uiState.selectedFolderFilter == null &&
                                        uiState.selectedPlaylistFilter == null -> {
                                        TreeHierarchyView(
                                            videos = uiState.filteredAndSortedVideos,
                                            expandedNodes = uiState.expandedTreeNodes,
                                            onToggleNode = { viewModel.toggleTreeNode(it) },
                                            onPlayVideo = { viewModel.playVideo(it, context) },
                                            onOpenMenu = { viewModel.openVideoMenu(it) },
                                            menu = videoMenu
                                        )
                                    }

                                    else -> {
                                        VideosContentListOrGrid(
                                            uiState = uiState,
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
                                            menu = videoMenu,
                                            onRescan = { viewModel.refreshPermissionsAndScan(context, showSpinner = true) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Music mini player, floating just above the navigation bar
                if (uiState.bottomTab == BottomTab.AUDIO &&
                    uiState.hasAudioPermission &&
                    miniTrack != null &&
                    !showNowPlaying &&
                    !audioSelectionActive
                ) {
                    // App theme colours (not the album's), so it sits calmly next to the bar.
                    AudioMiniPlayer(
                        track = miniTrack,
                        isPlaying = uiState.isAudioPlaying,
                        onOpen = { showNowPlaying = true },
                        onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                        onSkipPrevious = { viewModel.skipAudioPrevious() },
                        onSkipNext = { viewModel.skipAudioNext() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }

                // Selected videos: official floating toolbar (replaces the navigation bar)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isSelectionMode && uiState.bottomTab == BottomTab.VIDEOS,
                    enter = fadeIn(motionScheme.fastEffectsSpec()) +
                        slideInVertically(motionScheme.defaultSpatialSpec()) { it },
                    exit = fadeOut(motionScheme.fastEffectsSpec()) +
                        slideOutVertically(motionScheme.fastSpatialSpec()) { it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 16.dp)
                ) {
                    SelectionToolbar(
                        onShare = {
                            val selectedVideos = uiState.videos.filter { it.id in uiState.selectedIds }
                            shareVideosContentUri(context = context, videos = selectedVideos)
                            viewModel.clearSelection()
                        },
                        onAddToPlaylist = { viewModel.openPlaylistSheetForIds(uiState.selectedIds) },
                        onDelete = {
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
                }

                // Short messages: official snackbar, above the navigation bar
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.toastMsg != null,
                    enter = fadeIn(motionScheme.fastEffectsSpec()) +
                        scaleIn(initialScale = 0.92f, animationSpec = motionScheme.fastSpatialSpec()),
                    exit = fadeOut(motionScheme.fastEffectsSpec()) +
                        scaleOut(targetScale = 0.92f, animationSpec = motionScheme.fastSpatialSpec()),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = if (uiState.isSelectionMode) 104.dp else 16.dp)
                ) {
                    Snackbar(modifier = Modifier.testTag("toast_banner")) {
                        Text(uiState.toastMsg.orEmpty())
                    }
                }
            }

            // 2. Official Material 3 Expressive navigation bar (hidden while selecting)
            AnimatedVisibility(
                visible = !uiState.isSelectionMode && !(audioSelectionActive && uiState.bottomTab == BottomTab.AUDIO),
                enter = expandVertically(motionScheme.defaultSpatialSpec()),
                exit = shrinkVertically(motionScheme.fastSpatialSpec())
            ) {
                MainNavigationBar(
                    bottomTab = uiState.bottomTab,
                    activeTabs = uiState.activeTabs,
                    onSelectTab = { viewModel.setBottomTab(it, context) },
                    onLongPress = { showTabsSheet = true },
                    musicPlaying = uiState.playingAudio != null && uiState.isAudioPlaying
                )
            }
        }

        // Status bar protection while the Videos header is scrolled away
        if (videosSearchHeader) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(outerChromeBg.copy(alpha = 0.92f))
            )
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
                    // Gradient, blurred cover and black are always dark: use the dark colours there.
                    dark = libraryPrefs.nowPlayingBackground != NowPlayingBackground.FOLLOW_THEME
                ) {
                NowPlayingScreen(
                    track = nowTrack,
                    isPlaying = uiState.isAudioPlaying,
                    positionMs = audioState.positionMs,
                    durationMs = audioState.durationMs,
                    shuffleEnabled = audioState.shuffleEnabled,
                    repeatMode = audioState.repeatMode,
                    trayActions = libraryPrefs.visibleTray,
                    onClose = { showNowPlaying = false },
                    onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                    onSeekTo = { viewModel.seekAudio(it) },
                    onSkipPrevious = { viewModel.skipAudioPrevious() },
                    onSkipNext = { viewModel.skipAudioNext() },
                    onToggleShuffle = { audioManager.toggleShuffle() },
                    onCycleRepeat = { audioManager.cycleRepeat() },
                    onLyrics = { showLyrics = true },
                    onOpenAmbient = { ambientRequested = true },
                    sleepTimerRemainingMs = audioState.sleepTimerRemainingMs,
                    sleepAtEndOfSong = audioState.sleepAtEndOfSong,
                    onStartSleepTimer = { minutes -> audioManager.startSleepTimer(minutes) },
                    onSleepAtEndOfSong = { audioManager.startSleepAtEndOfSong() },
                    onCancelSleepTimer = { audioManager.cancelSleepTimer() },
                    queue = audioState.queue,
                    queueIndex = audioState.queueIndex,
                    onSkipToQueueItem = { audioManager.skipToQueueItem(it) },
                    onMoveQueueItem = { from, to -> audioManager.moveQueueItem(from, to) },
                    onRemoveQueueItem = { audioManager.removeQueueItem(it) },
                    onAddToPlaylist = { nowPlayingAddToPlaylist = nowTrack },
                    onShare = { com.example.ui.components.shareTrack(context, nowTrack) },
                    onSongInfo = { nowPlayingInfoTrack = nowTrack },
                    onGoToAlbum = if (LibraryTab.ALBUMS in libraryPrefs.visibleTabs) {
                        {
                            libraryOpenRequest = LibraryOpenRequest(LibraryTab.ALBUMS, com.example.ui.components.albumKeyOf(nowTrack))
                            showNowPlaying = false
                            viewModel.setBottomTab(BottomTab.AUDIO, context)
                        }
                    } else null,
                    onGoToArtist = if (LibraryTab.ARTISTS in libraryPrefs.visibleTabs && nowTrack.artist.isNotBlank()) {
                        {
                            libraryOpenRequest = LibraryOpenRequest(LibraryTab.ARTISTS, nowTrack.artist.lowercase())
                            showNowPlaying = false
                            viewModel.setBottomTab(BottomTab.AUDIO, context)
                        }
                    } else null,
                    background = libraryPrefs.nowPlayingBackground
                )
                }
            }
        }

        // 3d. Lyrics (over Now Playing)
        AnimatedVisibility(
            visible = showLyrics && showNowPlaying && miniTrack != null,
            enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val lyricsTrack = miniTrack ?: lastNowPlayingTrack
            if (lyricsTrack != null) {
                // Same colours and background as Now Playing.
                AlbumArtTheme(
                    track = lyricsTrack,
                    applyScheme = libraryPrefs.nowPlayingColorSource == NowPlayingColorSource.ALBUM_ART,
                    // Gradient, blurred cover and black are always dark: use the dark colours there.
                    dark = libraryPrefs.nowPlayingBackground != NowPlayingBackground.FOLLOW_THEME
                ) {
                    LyricsScreen(
                        track = lyricsTrack,
                        isPlaying = uiState.isAudioPlaying,
                        positionMs = audioState.positionMs,
                        settings = libraryPrefs.lyrics,
                        background = libraryPrefs.nowPlayingBackground,
                        onSettingsChange = { value ->
                            audioUiScope.launch { AppPreferencesRepository.updateLyricsSettings(context, value) }
                        },
                        onSeekTo = { audioManager.seekTo(it) },
                        onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                        onSkipNext = { viewModel.skipAudioNext() },
                        onSkipPrevious = { viewModel.skipAudioPrevious() },
                        onClose = { showLyrics = false }
                    )
                }
            }
        }

        // 3e. Ambient mode (over Now Playing): from the tray button, or by turning the phone sideways.
        AnimatedVisibility(
            visible = showAmbient,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val ambientTrack = miniTrack ?: lastNowPlayingTrack
            if (ambientTrack != null) {
                // Ambient's own "Black background" (for the night) wins over the Now Playing one.
                val ambientBackground = if (libraryPrefs.ambient.black) NowPlayingBackground.AMOLED
                else libraryPrefs.nowPlayingBackground
                AlbumArtTheme(
                    track = ambientTrack,
                    applyScheme = libraryPrefs.nowPlayingColorSource == NowPlayingColorSource.ALBUM_ART,
                    dark = ambientBackground != NowPlayingBackground.FOLLOW_THEME
                ) {
                AmbientModeScreen(
                    track = ambientTrack,
                    isPlaying = uiState.isAudioPlaying,
                    queue = audioState.queue,
                    queueIndex = audioState.queueIndex,
                    settings = libraryPrefs.ambient,
                    background = ambientBackground,
                    // From the button it turns sideways; turned by hand it just follows the phone.
                    forceLandscape = ambientRequested,
                    onSettingsChange = { value ->
                        audioUiScope.launch { AppPreferencesRepository.updateAmbientSettings(context, value) }
                    },
                    onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                    onSkipToQueueItem = { audioManager.skipToQueueItem(it) },
                    onSkipNext = { viewModel.skipAudioNext() },
                    onSkipPrevious = { viewModel.skipAudioPrevious() },
                    onClose = {
                        ambientRequested = false
                        if (isLandscape) ambientDismissedSideways = true
                    },
                    positionMs = audioState.positionMs,
                    lyricsSettings = libraryPrefs.lyrics,
                    onSeekTo = { audioManager.seekTo(it) }
                )
                }
            }
        }

        // A song that couldn't play: the full reason, with Copy.
        playbackError?.let { error ->
            com.example.ui.components.PlaybackErrorDialog(error = error, onDismiss = { audioManager.clearError() })
        }

        nowPlayingInfoTrack?.let { infoTrack ->
            SongInfoSheet(track = infoTrack, onDismiss = { nowPlayingInfoTrack = null })
        }
        nowPlayingAddToPlaylist?.let { song ->
            com.example.ui.components.AddToPlaylistSheet(
                playlists = libraryPrefs.playlists,
                songCount = 1,
                onCreateNew = {
                    nowPlayingAddToPlaylist = null
                    nowPlayingNewPlaylistFor = song
                },
                onPick = { playlist ->
                    nowPlayingAddToPlaylist = null
                    audioUiScope.launch { AppPreferencesRepository.addToAudioPlaylist(context, playlist.id, listOf(song.id)) }
                    Toast.makeText(context, "Added to “${playlist.name}”", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { nowPlayingAddToPlaylist = null }
            )
        }
        nowPlayingNewPlaylistFor?.let { song ->
            com.example.ui.components.PlaylistNameDialog(
                title = "New playlist",
                initialName = "",
                confirmLabel = "Create",
                onConfirm = { name ->
                    nowPlayingNewPlaylistFor = null
                    audioUiScope.launch { AppPreferencesRepository.createAudioPlaylist(context, name, listOf(song.id)) }
                    Toast.makeText(context, "Added to “$name”", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { nowPlayingNewPlaylistFor = null }
            )
        }

        // 4. Overlays, Menus, Bottom Sheets & Dialogs

        if (showTabsSheet) {
            ChooseTabsSheet(
                activeTabs = uiState.activeTabs,
                onSetTabActive = { tab, active -> viewModel.setTabActive(tab, active, context) },
                onDismiss = { showTabsSheet = false }
            )
        }

        if (uiState.sortSheetOpen && uiState.bottomTab == BottomTab.VIDEOS) {
            SortAndViewBottomSheet(
                smartGroups = uiState.smartGroups,
                viewMode = uiState.viewMode,
                layoutMode = uiState.layoutMode,
                onSelectViewMode = { viewModel.setViewMode(it, context) },
                onSelectLayoutMode = { viewModel.setLayoutMode(it, context) },
                sortBy = uiState.sortBy,
                sortDir = uiState.sortDir,
                onToggleSmartGroups = { viewModel.toggleSmartGroups(context) },
                onSelectSortField = { viewModel.setSortField(it, context) },
                onSelectSortDir = { viewModel.setSortDir(it, context) },
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        if (uiState.detailsVideo != null) {
            VideoDetailsSheet(
                video = uiState.detailsVideo,
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        if (uiState.renameTarget != null) {
            RenameVideoDialog(
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
            AddVideosToPlaylistSheet(
                playlists = uiState.playlists,
                onSelectPlaylistName = { plName -> viewModel.addTargetsToPlaylist(plName, context) },
                onDismiss = { viewModel.clearAllOverlays() }
            )
        }

        // 4b. Full-screen video search (official Material 3 search view)
        if (uiState.bottomTab == BottomTab.VIDEOS && uiState.hasVideoPermission) {
            VideoSearchView(
                state = videoSearchState,
                textFieldState = videoSearchText,
                videos = uiState.videos,
                onPlay = { video ->
                    headerScope.launch { videoSearchState.animateToCollapsed() }
                    viewModel.playVideo(video, context)
                }
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
                resumedFromMs = uiState.resumedFromMs,
                nextVideo = remember(activePlaying.id, uiState.videos, uiState.sortBy, uiState.sortDir) {
                    viewModel.peekNextInFolder()
                },
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
                onPlaybackEnded = { viewModel.playNextInFolder(context) },
                onUpdatePlaybackSettings = { transform -> viewModel.updatePlaybackSettings(context, transform) },
                onClose = { viewModel.closeVideoPlayer(context) }
            )
        }
    }
}

/**
 * The header above the tabs, all official Material 3 Expressive app bars:
 * - Videos: the search app bar ("Search videos" with the sort button), which scrolls away.
 * - Selecting videos: a pinned bar with ✕, the count and Select all.
 * - Inside a folder or playlist: a pinned bar with back, the name and the number of videos.
 * - Me, or a tab still waiting for permission: a plain bar with the tab's name.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainHeader(
    uiState: VideoUiState,
    containerColor: Color,
    videosSearchHeader: Boolean,
    searchState: SearchBarState,
    searchText: androidx.compose.foundation.text.input.TextFieldState,
    searchScrollBehavior: SearchBarScrollBehavior,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onClearSubFilter: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sortButton: @Composable () -> Unit = {
        TooltipIconButton(
            label = "View and sort",
            icon = Symbols.Tune,
            onClick = onOpenSortSheet,
            modifier = Modifier.testTag("header_sort_btn")
        )
    }
    when {
        videosSearchHeader -> AppBarWithSearch(
            state = searchState,
            inputField = { LibrarySearchInputField(searchState, searchText) },
            actions = { sortButton() },
            colors = SearchBarDefaults.appBarWithSearchColors(
                appBarContainerColor = containerColor,
                scrolledAppBarContainerColor = containerColor
            ),
            scrollBehavior = searchScrollBehavior,
            modifier = Modifier.testTag("videos_search_app_bar")
        )

        uiState.isSelectionMode -> {
            val allSelected = uiState.selectedIds.size == uiState.filteredAndSortedVideos.size &&
                uiState.filteredAndSortedVideos.isNotEmpty()
            TopAppBar(
                title = {
                    Text(
                        "${uiState.selectedIds.size} selected",
                        modifier = Modifier.testTag("selection_mode_header")
                    )
                },
                navigationIcon = {
                    TooltipIconButton(
                        label = "Clear selection",
                        icon = Symbols.Close,
                        onClick = onClearSelection,
                        modifier = Modifier.testTag("clear_selection_btn")
                    )
                },
                actions = {
                    TextButton(
                        onClick = onToggleSelectAll,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("select_all_btn")
                    ) { Text(if (allSelected) "Deselect all" else "Select all") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceContainer)
            )
        }

        uiState.bottomTab == BottomTab.VIDEOS &&
            (uiState.selectedFolderFilter != null || uiState.selectedPlaylistFilter != null) -> {
            val title = uiState.selectedFolderFilter?.substringAfterLast('/')
                ?: uiState.playlists.find { it.id == uiState.selectedPlaylistFilter }?.name
                ?: "Videos"
            val count = uiState.filteredAndSortedVideos.size
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(if (count == 1) "1 video" else "$count videos") },
                navigationIcon = {
                    TooltipIconButton(
                        label = "Back",
                        icon = Symbols.ArrowBack,
                        onClick = onClearSubFilter,
                        modifier = Modifier.testTag("back_from_folder_btn")
                    )
                },
                actions = { sortButton() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
            )
        }

        else -> TopAppBar(
            title = {
                Text(
                    when (uiState.bottomTab) {
                        BottomTab.AUDIO -> "Music"
                        BottomTab.ME -> "Me"
                        BottomTab.VIDEOS -> "Videos"
                    },
                    style = MaterialTheme.typography.headlineLargeEmphasized,
                    maxLines = 1
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideosContentListOrGrid(
    uiState: VideoUiState,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onVideoMenuClick: (VideoItem) -> Unit,
    menu: @Composable (VideoItem) -> Unit,
    onRescan: () -> Unit
) {
    val videos = uiState.filteredAndSortedVideos

    if (videos.isEmpty()) {
        if (!uiState.videosLoaded) {
            // First scan still running: a single calm indicator instead of an empty message.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return
        }
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
                    .size(72.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Symbols.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("No videos found", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Videos saved on your phone show up here automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onRescan,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("rescan_media_btn")
            ) {
                Icon(Symbols.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Scan again")
            }
        }
        return
    }

    val seriesData = uiState.smartSeriesData
    val grouped = uiState.smartGroups && seriesData.groups.isNotEmpty()
    val selecting = uiState.isSelectionMode
    // Room for the floating selection toolbar while selecting.
    val bottomPad = if (selecting) {
        96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else 16.dp

    if (uiState.layoutMode == LayoutMode.GRID) {
        // Adaptive: two columns on a phone held upright, more when there's room.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("videos_grid_container"),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = bottomPad),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            fun gridItems(list: List<VideoItem>, episodes: Boolean) {
                items(list, key = { it.id }, contentType = { "video" }) { v ->
                    VideoGridItem(
                        video = v,
                        isSelected = v.id in uiState.selectedIds,
                        isSelectionMode = selecting,
                        episodeLabel = if (episodes) v.episode?.let { "EP $it" } else null,
                        onClick = { onVideoClick(v) },
                        onLongClick = { onVideoLongClick(v) },
                        onMenuClick = { onVideoMenuClick(v) },
                        menu = { menu(v) }
                    )
                }
            }
            if (grouped) {
                seriesData.groups.forEach { (seriesTitle, eps) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_$seriesTitle", contentType = "header") {
                        SeriesGroupHeader(seriesTitle = seriesTitle)
                    }
                    gridItems(eps, episodes = true)
                }
                if (seriesData.standalone.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_standalone", contentType = "header") {
                        SeriesGroupHeader(seriesTitle = "Other videos", icon = Symbols.VideoLibrary)
                    }
                    gridItems(seriesData.standalone, episodes = false)
                }
            } else {
                gridItems(videos, episodes = false)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("videos_list_container"),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = bottomPad),
            // The Expressive list gap: selected rows stay separate rounded tiles instead of merging.
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            fun listItems(list: List<VideoItem>, episodes: Boolean) {
                items(list, key = { it.id }, contentType = { "video" }) { v ->
                    VideoListItem(
                        video = v,
                        isSelected = v.id in uiState.selectedIds,
                        isSelectionMode = selecting,
                        episodeLabel = if (episodes) v.episode?.let { "EP $it" } else null,
                        onClick = { onVideoClick(v) },
                        onLongClick = { onVideoLongClick(v) },
                        onMenuClick = { onVideoMenuClick(v) },
                        menu = { menu(v) }
                    )
                }
            }
            if (grouped) {
                seriesData.groups.forEach { (seriesTitle, eps) ->
                    item(key = "series_header_$seriesTitle", contentType = "header") {
                        SeriesGroupHeader(seriesTitle = seriesTitle, modifier = Modifier.padding(top = 8.dp))
                    }
                    listItems(eps, episodes = true)
                }
                if (seriesData.standalone.isNotEmpty()) {
                    item(key = "standalone_header", contentType = "header") {
                        SeriesGroupHeader(
                            seriesTitle = "Other videos",
                            icon = Symbols.VideoLibrary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    listItems(seriesData.standalone, episodes = false)
                }
            } else {
                listItems(videos, episodes = false)
            }
        }
    }
}

@Composable
private fun SeriesGroupHeader(
    seriesTitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Symbols.Layers
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(seriesTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Actions for the selected videos: the official floating toolbar in its vibrant colours, icon
 * buttons with their names in tooltips.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionToolbar(
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = Modifier.testTag("multi_select_pill_toolbar")
    ) {
        TooltipIconButton("Share", Symbols.Share, onShare, Modifier.testTag("pill_share_btn"), tooltipAbove = true)
        TooltipIconButton("Add to playlist", Symbols.PlaylistAdd, onAddToPlaylist, Modifier.testTag("pill_add_to_btn"), tooltipAbove = true)
        TooltipIconButton("Delete", Symbols.DeleteOutline, onDelete, Modifier.testTag("pill_delete_btn"), tooltipAbove = true)
    }
}

private val NavTabs = listOf(
    Triple(BottomTab.AUDIO, "Music", Symbols.MusicNote),
    Triple(BottomTab.VIDEOS, "Videos", Symbols.PlayCircle),
    Triple(BottomTab.ME, "Me", Symbols.Person)
)

/**
 * The tabs: official Material 3 Expressive short navigation bar. Long-press anywhere on it to
 * choose which tabs show.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainNavigationBar(
    bottomTab: BottomTab,
    activeTabs: List<BottomTab>,
    onSelectTab: (BottomTab) -> Unit,
    onLongPress: () -> Unit,
    musicPlaying: Boolean = false
) {
    val tabs = NavTabs.filter { it.first in activeTabs }.ifEmpty { NavTabs.filter { it.first == BottomTab.VIDEOS } }
    val haptics = LocalHapticFeedback.current
    val longPressLatest by rememberUpdatedState(onLongPress)

    ShortNavigationBar(
        modifier = Modifier
            .testTag("main_navigation_bar")
            // Watches for a long press before the tab buttons see it; once it fires, the rest of
            // that touch is used up so the tab under the finger doesn't also get tapped.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                        }
                        true
                    }
                    if (released == null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        longPressLatest()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
    ) {
        tabs.forEach { (tab, label, icon) ->
            ShortNavigationBarItem(
                selected = bottomTab == tab,
                onClick = { onSelectTab(tab) },
                icon = {
                    // From another tab, the Music icon turns into moving sound bars while a song
                    // plays. Nothing when it's paused, and nothing on the Music tab itself.
                    if (tab == BottomTab.AUDIO && musicPlaying && bottomTab != BottomTab.AUDIO) {
                        PlayingBarsIcon(contentDescription = "Music is playing")
                    } else {
                        Icon(icon, contentDescription = null)
                    }
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_tab_${tab.id}")
            )
        }
    }
}

/** The equaliser icon, its bars gently rising and falling (official Material Symbol, animated). */
@Composable
private fun PlayingBarsIcon(contentDescription: String) {
    val pulse = rememberInfiniteTransition(label = "navMusicBars").animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 550), RepeatMode.Reverse),
        label = "navMusicBarsScale"
    )
    Icon(
        Symbols.GraphicEq,
        contentDescription = contentDescription,
        modifier = Modifier
            .testTag("music_playing_icon")
            .graphicsLayer { scaleY = pulse.value }
    )
}

/** Long-press on the navigation bar opens this: a switch per tab (at least one stays on). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChooseTabsSheet(
    activeTabs: List<BottomTab>,
    onSetTabActive: (BottomTab, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("choose_tabs_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Tabs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            Text(
                "Choose what shows in the bar. Long-press the bar again to change it.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                NavTabs.forEachIndexed { i, (tab, label, icon) ->
                    val on = tab in activeTabs
                    val isLast = on && activeTabs.size <= 1
                    SegmentedListItem(
                        checked = on,
                        onCheckedChange = { checked -> onSetTabActive(tab, checked) },
                        enabled = !isLast,
                        shapes = ListItemDefaults.segmentedShapes(index = i, count = NavTabs.size),
                        leadingContent = { Icon(icon, contentDescription = null) },
                        supportingContent = if (isLast) {
                            { Text("At least one tab stays on") }
                        } else null,
                        trailingContent = { Switch(checked = on, onCheckedChange = null, enabled = !isLast) },
                        colors = ListItemDefaults.segmentedColors(containerColor = scheme.surfaceContainerHigh),
                        modifier = Modifier.testTag("tab_switch_${tab.id}")
                    ) { Text(label) }
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
