package com.example.viewmodel

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferencesRepository
import com.example.data.MediaStoreScanner
import com.example.model.AudioTrackItem
import com.example.model.BottomTab
import com.example.model.DecoderMode
import com.example.model.LayoutMode
import com.example.model.MAX_HISTORY_ITEMS
import com.example.model.MeSubPage
import com.example.model.PlaybackSettings
import com.example.model.PlaylistItem
import com.example.model.ResumePromptTarget
import com.example.model.SortBy
import com.example.model.SortDir
import com.example.model.ThemeMode
import com.example.model.VideoHistoryEntry
import com.example.model.VideoItem
import com.example.model.ViewMode
import com.example.player.AudioPlaybackManager
import com.example.player.ExoPlayerController
import com.example.player.PlayerController
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SmartSeriesData(
    val groups: Map<String, List<VideoItem>>,
    val standalone: List<VideoItem>
)

enum class PendingMediaWriteAction {
    NONE, RENAME, DELETE
}

data class VideoUiState(
    val videos: List<VideoItem> = emptyList(),
    val audioTracks: List<AudioTrackItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val historyEntries: List<VideoHistoryEntry> = emptyList(),
    val progressHistory: Map<String, Float> = emptyMap(),
    val hasVideoPermission: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val videoPermissionAsked: Boolean = false,
    val audioPermissionAsked: Boolean = false,
    val isRefreshing: Boolean = false,
    val smartGroups: Boolean = false,
    val viewMode: ViewMode = ViewMode.VIDEOS,
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val sortBy: SortBy = SortBy.DATE,
    val sortDir: SortDir = SortDir.DESC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val colorTheme: String = "indigo",
    val customColorArgb: Int = 0xFF6366F1.toInt(),
    val amoledBlack: Boolean = false,
    val showResolutionTier: Boolean = true,
    val activeTabs: List<BottomTab> = BottomTab.entries,
    val isPillEditMode: Boolean = false,
    val meSubPage: MeSubPage = MeSubPage.NONE,
    val playbackSettings: PlaybackSettings = PlaybackSettings(),
    val bottomTab: BottomTab = BottomTab.VIDEOS,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedIds: List<String> = emptyList(),
    val selectedFolderFilter: String? = null,
    val selectedPlaylistFilter: String? = null,
    val expandedTreeNodes: Set<String> = emptySet(),
    val activeMenuVideo: VideoItem? = null,
    val detailsVideo: VideoItem? = null,
    val renameTarget: VideoItem? = null,
    val renameValue: String = "",
    val playlistTargetIds: List<String> = emptyList(),
    val sortSheetOpen: Boolean = false,
    val resumePromptTarget: ResumePromptTarget? = null,
    val playingVideo: VideoItem? = null,
    val playingAudio: AudioTrackItem? = null,
    val isAudioPlaying: Boolean = false,
    val audioProgress: Float = 0f,
    val toastMsg: String? = null
) {
    val isSelectionMode: Boolean
        get() = selectedIds.isNotEmpty()

    val filteredAndSortedVideos: List<VideoItem>
        get() {
            var list = videos
            if (selectedFolderFilter != null) {
                list = list.filter { it.folder == selectedFolderFilter }
            }
            if (selectedPlaylistFilter != null) {
                val pl = playlists.find { it.id == selectedPlaylistFilter }
                if (pl != null) {
                    val validSet = pl.mediaUris.toSet()
                    list = list.filter { it.id in validSet }
                } else {
                    list = emptyList()
                }
            }
            val q = searchQuery.trim().lowercase()
            if (q.isNotEmpty()) {
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                        it.folder.lowercase().contains(q) ||
                        (it.series?.lowercase()?.contains(q) == true)
                }
            }
            val comparator = Comparator<VideoItem> { a, b ->
                when (sortBy) {
                    SortBy.NAME -> a.title.compareTo(b.title, ignoreCase = true)
                    SortBy.DATE -> a.timestamp.compareTo(b.timestamp)
                    SortBy.DURATION -> a.durationMs.compareTo(b.durationMs)
                    SortBy.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                }
            }
            return if (sortDir == SortDir.ASC) {
                list.sortedWith(comparator)
            } else {
                list.sortedWith(comparator.reversed())
            }
        }

    val smartSeriesData: SmartSeriesData
        get() {
            val groups = linkedMapOf<String, MutableList<VideoItem>>()
            val standalone = mutableListOf<VideoItem>()
            for (v in filteredAndSortedVideos) {
                if (v.series != null) {
                    groups.getOrPut(v.series) { mutableListOf() }.add(v)
                } else {
                    standalone.add(v)
                }
            }
            val sortedGroups = groups.mapValues { (_, eps) ->
                eps.sortedBy { it.episode ?: 0 }
            }
            return SmartSeriesData(sortedGroups, standalone)
        }
}

class VideoPlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null
    private var scanJob: Job? = null
    private var audioProgressJob: Job? = null
    private var videoPlayerController: ExoPlayerController? = null
    private var audioPlaybackManager: AudioPlaybackManager? = null
    private var prefsInitialized = false
    private var savedAppContext: Context? = null
    private var lastSavedHistoryMs: Long = 0L

    private var pendingAction: PendingMediaWriteAction = PendingMediaWriteAction.NONE
    private var pendingRenameUri: Uri? = null
    private var pendingRenameDisplayName: String? = null
    private var pendingDeleteUris: List<Uri> = emptyList()

    fun getOrCreatePlayerController(context: Context): PlayerController {
        val existing = videoPlayerController
        if (existing != null) return existing
        return ExoPlayerController(context.applicationContext).also {
            videoPlayerController = it
        }
    }

    private fun getOrCreateAudioPlaybackManager(context: Context): AudioPlaybackManager {
        val existing = audioPlaybackManager
        if (existing != null) return existing
        return AudioPlaybackManager.getInstance(context.applicationContext).also { manager ->
            audioPlaybackManager = manager
            observeAudioPlaybackManager(manager)
        }
    }

    fun initializeAndScan(context: Context) {
        val appContext = context.applicationContext
        savedAppContext = appContext
        getOrCreatePlayerController(appContext)
        getOrCreateAudioPlaybackManager(appContext)
        if (!prefsInitialized) {
            prefsInitialized = true
            viewModelScope.launch {
                val stored = AppPreferencesRepository.observePreferences(appContext).first()
                val cappedHistory = stored.historyEntries.take(MAX_HISTORY_ITEMS)
                val progressMap = cappedHistory.associate { it.mediaUri to it.progress }
                val validTabs = stored.activeTabs.ifEmpty { BottomTab.entries }
                val startTab = if (stored.lastBottomTab in validTabs) stored.lastBottomTab else validTabs.first()

                _uiState.update { state ->
                    state.copy(
                        themeMode = stored.themeMode,
                        dynamicColor = stored.dynamicColor,
                        colorTheme = stored.colorTheme,
                        customColorArgb = stored.customColorArgb,
                        amoledBlack = stored.amoledBlack,
                        showResolutionTier = stored.showResolutionTier,
                        activeTabs = validTabs,
                        bottomTab = startTab,
                        smartGroups = stored.smartGroups,
                        viewMode = stored.viewMode,
                        layoutMode = stored.layoutMode,
                        sortBy = stored.sortBy,
                        sortDir = stored.sortDir,
                        playbackSettings = stored.playbackSettings,
                        playlists = stored.playlists,
                        historyEntries = cappedHistory,
                        progressHistory = progressMap,
                        videos = state.videos.map { v ->
                            v.copy(progress = progressMap[v.id] ?: 0f)
                        }
                    )
                }
                refreshPermissionsAndScan(appContext, showSpinner = false)
            }
        } else {
            refreshPermissionsAndScan(appContext, showSpinner = false)
        }
    }

    fun refreshPermissionsAndScan(context: Context, showSpinner: Boolean = false) {
        val hasVideo = MediaStoreScanner.hasVideoPermission(context)
        val hasAudio = MediaStoreScanner.hasAudioPermission(context)
        _uiState.update {
            it.copy(
                hasVideoPermission = hasVideo,
                hasAudioPermission = hasAudio,
                isRefreshing = if (showSpinner) true else it.isRefreshing
            )
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val history = _uiState.value.progressHistory
            val scannedVideos = if (hasVideo) {
                MediaStoreScanner.scanVideos(context, history)
            } else {
                emptyList()
            }
            val scannedAudio = if (hasAudio) {
                MediaStoreScanner.scanAudio(context)
            } else {
                emptyList()
            }
            if (scannedAudio.isNotEmpty()) {
                getOrCreateAudioPlaybackManager(context).registerTracksCatalog(scannedAudio)
            }
            val allFolders = scannedVideos
                .map { it.path.substringBeforeLast("/", it.folder) }
                .distinct()
                .take(4)
                .toSet()

            _uiState.update { state ->
                val validIds = scannedVideos.map { it.id }.toSet()
                state.copy(
                    videos = scannedVideos,
                    audioTracks = scannedAudio,
                    selectedIds = state.selectedIds.filter { it in validIds },
                    expandedTreeNodes = if (state.expandedTreeNodes.isEmpty()) allFolders else state.expandedTreeNodes,
                    isRefreshing = false
                )
            }
        }
    }

    fun markVideoPermissionAsked() {
        _uiState.update { it.copy(videoPermissionAsked = true) }
    }

    fun markAudioPermissionAsked() {
        _uiState.update { it.copy(audioPermissionAsked = true) }
    }

    fun triggerToast(msg: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMsg = msg) }
        toastJob = viewModelScope.launch {
            delay(2400L)
            _uiState.update { it.copy(toastMsg = null) }
        }
    }

    fun clearAllOverlays() {
        _uiState.update {
            it.copy(
                activeMenuVideo = null,
                detailsVideo = null,
                renameTarget = null,
                playlistTargetIds = emptyList(),
                sortSheetOpen = false,
                resumePromptTarget = null
            )
        }
    }

    fun dismissResumePrompt() {
        _uiState.update { it.copy(resumePromptTarget = null) }
    }

    fun toggleSmartGroups(context: Context? = null) {
        val next = !_uiState.value.smartGroups
        _uiState.update { it.copy(smartGroups = next) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateSmartGroups(appCtx, next) }
        }
    }

    fun setViewMode(mode: ViewMode, context: Context? = null) {
        _uiState.update {
            it.copy(
                viewMode = mode,
                selectedFolderFilter = null,
                selectedPlaylistFilter = null
            )
        }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateViewMode(appCtx, mode) }
        }
    }

    fun setLayoutMode(mode: LayoutMode, context: Context? = null) {
        _uiState.update { it.copy(layoutMode = mode) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateLayoutMode(appCtx, mode) }
        }
    }

    fun handleSortOptionClick(clickedSort: SortBy, context: Context? = null) {
        var nextSortBy = clickedSort
        var nextSortDir = SortDir.DESC
        _uiState.update { state ->
            if (state.sortBy == clickedSort) {
                val toggledDir = if (state.sortDir == SortDir.ASC) SortDir.DESC else SortDir.ASC
                nextSortBy = state.sortBy
                nextSortDir = toggledDir
                state.copy(sortDir = toggledDir)
            } else {
                val defaultDir = if (clickedSort == SortBy.NAME) SortDir.ASC else SortDir.DESC
                nextSortBy = clickedSort
                nextSortDir = defaultDir
                state.copy(sortBy = clickedSort, sortDir = defaultDir)
            }
        }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateSort(appCtx, nextSortBy, nextSortDir) }
        }
    }

    fun setThemeMode(mode: ThemeMode, context: Context? = null) {
        _uiState.update { it.copy(themeMode = mode) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateThemeMode(appCtx, mode) }
        }
    }

    fun setDynamicColor(enabled: Boolean, context: Context? = null) {
        _uiState.update { it.copy(dynamicColor = enabled) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateDynamicColor(appCtx, enabled) }
        }
    }

    fun setColorTheme(themeId: String, context: Context? = null) {
        _uiState.update { it.copy(colorTheme = themeId) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateColorTheme(appCtx, themeId) }
        }
    }

    fun setCustomColor(argb: Int, context: Context? = null) {
        _uiState.update {
            it.copy(
                customColorArgb = argb,
                colorTheme = "custom"
            )
        }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateCustomColor(appCtx, argb) }
        }
    }

    fun toggleAmoledBlack(context: Context? = null) {
        val next = !_uiState.value.amoledBlack
        _uiState.update { it.copy(amoledBlack = next) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateAmoledBlack(appCtx, next) }
        }
    }

    fun toggleShowResolutionTier(context: Context? = null) {
        val next = !_uiState.value.showResolutionTier
        _uiState.update { it.copy(showResolutionTier = next) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updateShowResolutionTier(appCtx, next) }
        }
    }

    fun setMeSubPage(subPage: MeSubPage) {
        _uiState.update { it.copy(meSubPage = subPage) }
    }

    fun setPillEditMode(editing: Boolean) {
        _uiState.update { it.copy(isPillEditMode = editing) }
    }

    /**
     * Enforces the MINIMUM ONE TAB RULE and fixed tab order (Audio, Videos, Me).
     */
    fun setTabActive(tab: BottomTab, active: Boolean, context: Context? = null) {
        var updatedList: List<BottomTab> = emptyList()
        var changed = false
        _uiState.update { state ->
            val currentSet = state.activeTabs.toSet()
            if (!active && currentSet.size <= 1 && tab in currentSet) {
                // Minimum one tab rule: cannot remove the last active tab
                return@update state
            }
            val nextSet = if (active) currentSet + tab else currentSet - tab
            val nextOrdered = BottomTab.entries.filter { it in nextSet }
            if (nextOrdered.isEmpty()) return@update state
            changed = nextOrdered != state.activeTabs
            updatedList = nextOrdered

            val nextBottomTab = if (state.bottomTab in nextOrdered) {
                state.bottomTab
            } else {
                nextOrdered.first()
            }
            val nextMeSubPage = if (nextBottomTab != BottomTab.ME) MeSubPage.NONE else state.meSubPage
            state.copy(
                activeTabs = nextOrdered,
                bottomTab = nextBottomTab,
                meSubPage = nextMeSubPage
            )
        }
        val appCtx = context?.applicationContext ?: savedAppContext
        if (changed && appCtx != null) {
            val currentBottomTab = _uiState.value.bottomTab
            viewModelScope.launch {
                AppPreferencesRepository.updateActiveTabs(appCtx, updatedList)
                AppPreferencesRepository.updateLastBottomTab(appCtx, currentBottomTab)
            }
        }
    }

    fun updatePlaybackSettings(context: Context? = null, transform: (PlaybackSettings) -> PlaybackSettings) {
        val next = transform(_uiState.value.playbackSettings)
        _uiState.update { it.copy(playbackSettings = next) }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch { AppPreferencesRepository.updatePlaybackSettings(appCtx, next) }
        }
    }

    fun setBottomTab(tab: BottomTab, context: Context? = null) {
        if (tab !in _uiState.value.activeTabs) return
        _uiState.update {
            it.copy(
                bottomTab = tab,
                meSubPage = MeSubPage.NONE,
                selectedFolderFilter = null,
                selectedPlaylistFilter = null,
                sortSheetOpen = false
            )
        }
        val appCtx = context?.applicationContext ?: savedAppContext
        if (appCtx != null) {
            viewModelScope.launch {
                AppPreferencesRepository.updateLastBottomTab(appCtx, tab)
            }
        }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update {
            it.copy(
                searchOpen = open,
                searchQuery = if (!open) "" else it.searchQuery
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSelectId(id: String) {
        _uiState.update { state ->
            val updated = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptyList()) }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            val visibleIds = state.filteredAndSortedVideos.map { it.id }
            val nextSelected = if (state.selectedIds.size == visibleIds.size && visibleIds.isNotEmpty()) {
                emptyList()
            } else {
                visibleIds
            }
            state.copy(selectedIds = nextSelected)
        }
    }

    fun openVideoMenu(video: VideoItem) {
        clearAllOverlays()
        _uiState.update { it.copy(activeMenuVideo = video) }
    }

    fun openSortSheet() {
        clearAllOverlays()
        _uiState.update { it.copy(sortSheetOpen = true) }
    }

    fun openDetailsSheet(video: VideoItem) {
        _uiState.update {
            it.copy(
                activeMenuVideo = null,
                detailsVideo = video
            )
        }
    }

    fun openRenameDialog(video: VideoItem) {
        _uiState.update {
            it.copy(
                activeMenuVideo = null,
                renameTarget = video,
                renameValue = video.title
            )
        }
    }

    fun updateRenameValue(newVal: String) {
        _uiState.update { it.copy(renameValue = newVal) }
    }

    /**
     * Initiates a real MediaStore rename using system confirmation dialogs.
     */
    fun submitRenameRequest(
        context: Context,
        onLaunchIntentSender: (IntentSender) -> Unit,
        onRequestLegacyWritePermission: () -> Unit
    ) {
        val target = _uiState.value.renameTarget ?: return
        val newTitle = _uiState.value.renameValue.trim()
        if (newTitle.isEmpty() || newTitle == target.title) {
            clearAllOverlays()
            return
        }
        val newDisplayName = "$newTitle.${target.ext}"
        pendingAction = PendingMediaWriteAction.RENAME
        pendingRenameUri = target.contentUri
        pendingRenameDisplayName = newDisplayName
        _uiState.update { it.copy(renameTarget = null, activeMenuVideo = null) }

        try {
            val writeRequest = MediaStore.createWriteRequest(
                context.contentResolver,
                listOf(target.contentUri)
            )
            onLaunchIntentSender(writeRequest.intentSender)
        } catch (_: Exception) {
            triggerToast("Could not request rename permission")
        }
    }

    private fun executeRenameOnConfirmed(
        context: Context,
        onLaunchIntentSender: (IntentSender) -> Unit
    ) {
        val uri = pendingRenameUri ?: return
        val displayName = pendingRenameDisplayName ?: return
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.TITLE, displayName.substringBeforeLast('.'))
                    }
                    context.contentResolver.update(uri, values, null, null)
                }
                if (updated > 0) {
                    MediaStoreScanner.evictThumbnail(uri.toString())
                    clearPendingWriteState()
                    refreshPermissionsAndScan(context)
                    triggerToast("File renamed")
                } else {
                    clearPendingWriteState()
                    triggerToast("Could not rename file")
                }
            } catch (secEx: SecurityException) {
                if (secEx is RecoverableSecurityException) {
                    onLaunchIntentSender(secEx.userAction.actionIntent.intentSender)
                } else {
                    clearPendingWriteState()
                    triggerToast("Rename denied by system")
                }
            } catch (_: Exception) {
                clearPendingWriteState()
                triggerToast("Rename failed")
            }
        }
    }

    /**
     * Initiates a real MediaStore delete using system confirmation dialogs.
     */
    fun requestDeleteVideos(
        context: Context,
        targetVideos: List<VideoItem>,
        onLaunchIntentSender: (IntentSender) -> Unit,
        onRequestLegacyWritePermission: () -> Unit
    ) {
        if (targetVideos.isEmpty()) return
        val uris = targetVideos.map { it.contentUri }
        pendingAction = PendingMediaWriteAction.DELETE
        pendingDeleteUris = uris
        clearAllOverlays()

        try {
            val deleteRequest = MediaStore.createDeleteRequest(
                context.contentResolver,
                uris
            )
            onLaunchIntentSender(deleteRequest.intentSender)
        } catch (_: Exception) {
            clearPendingWriteState()
            triggerToast("Could not open system delete dialog")
        }
    }

    fun onMediaIntentSenderResult(
        context: Context,
        confirmed: Boolean,
        onLaunchIntentSender: (IntentSender) -> Unit
    ) {
        val action = pendingAction
        if (!confirmed) {
            clearPendingWriteState()
            val msg = if (action == PendingMediaWriteAction.RENAME) {
                "Rename declined"
            } else {
                "Delete declined"
            }
            triggerToast(msg)
            return
        }

        when (action) {
            PendingMediaWriteAction.RENAME -> {
                executeRenameOnConfirmed(context, onLaunchIntentSender)
            }

            PendingMediaWriteAction.DELETE -> {
                val count = pendingDeleteUris.size
                pendingDeleteUris.forEach { MediaStoreScanner.evictThumbnail(it.toString()) }
                clearPendingWriteState()
                _uiState.update { it.copy(selectedIds = emptyList()) }
                refreshPermissionsAndScan(context)
                triggerToast("Deleted $count video${if (count == 1) "" else "s"}")
            }

            PendingMediaWriteAction.NONE -> Unit
        }
    }

    fun onLegacyWritePermissionResult(
        context: Context,
        granted: Boolean,
        onLaunchIntentSender: (IntentSender) -> Unit
    ) {
        if (!granted) {
            clearPendingWriteState()
            triggerToast("Storage write permission declined")
        }
    }

    private fun clearPendingWriteState() {
        pendingAction = PendingMediaWriteAction.NONE
        pendingRenameUri = null
        pendingRenameDisplayName = null
        pendingDeleteUris = emptyList()
    }

    fun selectSingleFromMenu(video: VideoItem) {
        clearAllOverlays()
        _uiState.update { it.copy(selectedIds = listOf(video.id)) }
    }

    fun openPlaylistSheetForIds(ids: List<String>) {
        _uiState.update {
            it.copy(
                activeMenuVideo = null,
                playlistTargetIds = ids
            )
        }
    }

    private fun persistPlaylists(context: Context?, playlists: List<PlaylistItem>) {
        val ctx = context?.applicationContext ?: return
        viewModelScope.launch {
            AppPreferencesRepository.updatePlaylists(ctx, playlists)
        }
    }

    fun addTargetsToPlaylist(playlistName: String, context: Context? = null) {
        val targetIds = _uiState.value.playlistTargetIds
        if (targetIds.isEmpty() || playlistName.isBlank()) return
        val cleanName = playlistName.trim()
        var nextPlaylists: List<PlaylistItem> = emptyList()
        _uiState.update { state ->
            val exists = state.playlists.any { it.name.equals(cleanName, ignoreCase = true) }
            val updatedPlaylists = if (exists) {
                state.playlists.map { pl ->
                    if (pl.name.equals(cleanName, ignoreCase = true)) {
                        pl.copy(mediaUris = (pl.mediaUris + targetIds).distinct())
                    } else {
                        pl
                    }
                }
            } else {
                state.playlists + PlaylistItem(
                    id = "pl_${System.currentTimeMillis()}",
                    name = cleanName,
                    mediaUris = targetIds.distinct()
                )
            }
            nextPlaylists = updatedPlaylists
            state.copy(
                playlists = updatedPlaylists,
                selectedIds = emptyList(),
                playlistTargetIds = emptyList(),
                activeMenuVideo = null
            )
        }
        persistPlaylists(context, nextPlaylists)
        triggerToast("Added to \"$cleanName\"")
    }

    fun createPlaylist(name: String, context: Context? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        var nextPlaylists: List<PlaylistItem> = emptyList()
        var created = false
        _uiState.update { state ->
            if (state.playlists.any { it.name.equals(clean, ignoreCase = true) }) {
                nextPlaylists = state.playlists
                state
            } else {
                created = true
                val updated = state.playlists + PlaylistItem(
                    id = "pl_${System.currentTimeMillis()}",
                    name = clean,
                    mediaUris = emptyList()
                )
                nextPlaylists = updated
                state.copy(playlists = updated)
            }
        }
        if (created) {
            persistPlaylists(context, nextPlaylists)
            triggerToast("Created playlist \"$clean\"")
        } else {
            triggerToast("Playlist \"$clean\" already exists")
        }
    }

    fun renamePlaylist(playlistId: String, newName: String, context: Context? = null) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        var nextPlaylists: List<PlaylistItem> = emptyList()
        _uiState.update { state ->
            val updated = state.playlists.map { pl ->
                if (pl.id == playlistId) pl.copy(name = clean) else pl
            }
            nextPlaylists = updated
            state.copy(playlists = updated)
        }
        persistPlaylists(context, nextPlaylists)
        triggerToast("Playlist renamed")
    }

    fun deletePlaylist(playlistId: String, context: Context? = null) {
        var nextPlaylists: List<PlaylistItem> = emptyList()
        _uiState.update { state ->
            val updated = state.playlists.filterNot { it.id == playlistId }
            nextPlaylists = updated
            state.copy(
                playlists = updated,
                selectedPlaylistFilter = if (state.selectedPlaylistFilter == playlistId) null else state.selectedPlaylistFilter
            )
        }
        persistPlaylists(context, nextPlaylists)
        triggerToast("Playlist deleted")
    }

    fun addVideosToPlaylistById(playlistId: String, videoIds: List<String>, context: Context? = null) {
        if (videoIds.isEmpty()) return
        var nextPlaylists: List<PlaylistItem> = emptyList()
        _uiState.update { state ->
            val updated = state.playlists.map { pl ->
                if (pl.id == playlistId) {
                    pl.copy(mediaUris = (pl.mediaUris + videoIds).distinct())
                } else {
                    pl
                }
            }
            nextPlaylists = updated
            state.copy(playlists = updated)
        }
        persistPlaylists(context, nextPlaylists)
        triggerToast("Updated playlist")
    }

    fun removeVideoFromPlaylist(playlistId: String, videoId: String, context: Context? = null) {
        var nextPlaylists: List<PlaylistItem> = emptyList()
        _uiState.update { state ->
            val updated = state.playlists.map { pl ->
                if (pl.id == playlistId) {
                    pl.copy(mediaUris = pl.mediaUris - videoId)
                } else {
                    pl
                }
            }
            nextPlaylists = updated
            state.copy(playlists = updated)
        }
        persistPlaylists(context, nextPlaylists)
        triggerToast("Removed from playlist")
    }

    fun selectFolderFilter(folder: String?) {
        _uiState.update {
            it.copy(
                selectedFolderFilter = folder,
                selectedPlaylistFilter = null
            )
        }
    }

    fun selectPlaylistFilter(playlistId: String?) {
        _uiState.update {
            it.copy(
                selectedPlaylistFilter = playlistId,
                selectedFolderFilter = null
            )
        }
    }

    fun toggleTreeNode(path: String) {
        _uiState.update { state ->
            val next = if (path in state.expandedTreeNodes) {
                state.expandedTreeNodes - path
            } else {
                state.expandedTreeNodes + path
            }
            state.copy(expandedTreeNodes = next)
        }
    }

    /**
     * Called when opening ANY video from Videos, Playlists, or History.
     * Shows the resume bottom sheet if the video has a saved position between 5s and 95% of duration;
     * otherwise skips the sheet and starts from 0:00.
     */
    fun playVideo(video: VideoItem, context: Context? = null) {
        val state = _uiState.value
        val existingEntry = state.historyEntries.find { it.mediaUri == video.id }
        val durMs = (existingEntry?.durationMs?.takeIf { it > 0L } ?: video.durationMs).coerceAtLeast(1L)
        val savedPosMs = existingEntry?.positionMs ?: 0L
        val fraction = savedPosMs.toFloat() / durMs.toFloat()

        if (existingEntry != null && savedPosMs >= 5_000L && fraction <= 0.95f) {
            _uiState.update {
                it.copy(
                    resumePromptTarget = ResumePromptTarget(
                        video = video,
                        resumePositionMs = savedPosMs
                    )
                )
            }
        } else {
            startVideoPlaybackAt(video = video, startPositionMs = 0L, context = context)
        }
    }

    fun startVideoPlaybackAt(
        video: VideoItem,
        startPositionMs: Long,
        context: Context? = null
    ) {
        pauseAudioSession()
        val state = _uiState.value
        if (context != null) {
            val controller = getOrCreatePlayerController(context)
            val pb = state.playbackSettings
            controller.prepareMedia(
                uri = video.contentUri,
                filePath = video.path,
                startPositionMs = startPositionMs,
                autoPlay = true,
                decoderMode = pb.defaultDecoder
            )
            recordVideoPlaybackHistory(
                videoId = video.id,
                positionMs = startPositionMs,
                durationMs = video.durationMs,
                forcePersist = true,
                moveToTop = true,
                context = context
            )
        }
        _uiState.update {
            it.copy(
                resumePromptTarget = null,
                playingVideo = video
            )
        }
    }

    /**
     * Records playback position and enforces the 25-item history limit.
     * Watching a video again moves it to the top without duplicates.
     * Any video evicted beyond 25 items automatically loses its saved resume point.
     */
    fun recordVideoPlaybackHistory(
        videoId: String,
        positionMs: Long,
        durationMs: Long,
        forcePersist: Boolean = false,
        moveToTop: Boolean = false,
        context: Context? = null
    ) {
        val safeDur = durationMs.coerceAtLeast(1L)
        val safePos = positionMs.coerceIn(0L, safeDur)
        val now = System.currentTimeMillis()

        var nextEntries: List<VideoHistoryEntry> = emptyList()
        _uiState.update { state ->
            val prevEntry = state.historyEntries.find { it.mediaUri == videoId }
            val timestampToUse = if (moveToTop || forcePersist || prevEntry == null) {
                now
            } else {
                prevEntry.lastPlayedTimestamp
            }
            val updatedEntry = VideoHistoryEntry(
                mediaUri = videoId,
                positionMs = safePos,
                durationMs = safeDur,
                lastPlayedTimestamp = timestampToUse
            )
            val entries = (listOf(updatedEntry) + state.historyEntries.filterNot { it.mediaUri == videoId })
                .sortedByDescending { it.lastPlayedTimestamp }
                .take(MAX_HISTORY_ITEMS)

            nextEntries = entries
            val updatedProgressMap = entries.associate { it.mediaUri to it.progress }
            val updatedVideos = state.videos.map { v ->
                v.copy(progress = updatedProgressMap[v.id] ?: 0f)
            }
            val updatedPlaying = state.playingVideo?.let { pv ->
                pv.copy(progress = updatedProgressMap[pv.id] ?: 0f)
            }
            state.copy(
                videos = updatedVideos,
                playingVideo = updatedPlaying,
                historyEntries = entries,
                progressHistory = updatedProgressMap
            )
        }

        if (context != null && (forcePersist || abs(now - lastSavedHistoryMs) >= 2500L)) {
            lastSavedHistoryMs = now
            val appCtx = context.applicationContext
            viewModelScope.launch {
                AppPreferencesRepository.updateHistory(appCtx, nextEntries)
            }
        }
    }

    fun clearVideoHistory(context: Context? = null) {
        _uiState.update { state ->
            state.copy(
                historyEntries = emptyList(),
                progressHistory = emptyMap(),
                videos = state.videos.map { it.copy(progress = 0f) }
            )
        }
        if (context != null) {
            val appCtx = context.applicationContext
            viewModelScope.launch {
                AppPreferencesRepository.updateHistory(appCtx, emptyList())
            }
        }
        triggerToast("Watch history cleared")
    }

    fun closeVideoPlayer(context: Context? = null) {
        val currentVideo = _uiState.value.playingVideo
        val controller = videoPlayerController
        if (currentVideo != null && controller != null) {
            val pState = controller.state.value
            val dur = pState.durationMs.takeIf { it > 0L } ?: currentVideo.durationMs
            recordVideoPlaybackHistory(
                videoId = currentVideo.id,
                positionMs = pState.positionMs,
                durationMs = dur,
                forcePersist = true,
                moveToTop = false,
                context = context
            )
        }
        controller?.release()
        _uiState.update { it.copy(playingVideo = null) }
    }

    fun playAdjacentVideo(offset: Int, context: Context? = null) {
        val state = _uiState.value
        val current = state.playingVideo ?: return
        val controller = videoPlayerController
        if (controller != null) {
            val pState = controller.state.value
            val dur = pState.durationMs.takeIf { it > 0L } ?: current.durationMs
            recordVideoPlaybackHistory(
                videoId = current.id,
                positionMs = pState.positionMs,
                durationMs = dur,
                forcePersist = true,
                moveToTop = false,
                context = context
            )
        }
        val list = state.filteredAndSortedVideos
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == current.id }
        if (idx == -1) return
        val nextIdx = (idx + offset).mod(list.size)
        startVideoPlaybackAt(list[nextIdx], startPositionMs = 0L, context = context)
    }

    fun playAudioTrack(context: Context, track: AudioTrackItem) {
        if (_uiState.value.playingAudio?.id == track.id) {
            toggleAudioPlayPause()
            return
        }
        val visibleTracks = _uiState.value.audioTracks.ifEmpty { listOf(track) }
        val startIndex = visibleTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playAudioQueue(
            context = context,
            tracks = visibleTracks,
            startIndex = startIndex,
            shuffle = false
        )
    }

    fun playAudioQueue(
        context: Context,
        tracks: List<AudioTrackItem>,
        startIndex: Int,
        shuffle: Boolean = false
    ) {
        if (tracks.isEmpty()) return
        try {
            val manager = getOrCreateAudioPlaybackManager(context)
            val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
            val targetTrack = tracks[safeIndex]
            _uiState.update {
                it.copy(
                    playingAudio = targetTrack,
                    isAudioPlaying = true,
                    audioProgress = 0f
                )
            }
            manager.playQueue(
                tracks = tracks,
                startIndex = safeIndex,
                shuffle = shuffle
            )
        } catch (_: Exception) {
            triggerToast("Unable to play audio track")
        }
    }

    private fun observeAudioPlaybackManager(manager: AudioPlaybackManager) {
        audioProgressJob?.cancel()
        audioProgressJob = viewModelScope.launch {
            manager.state.collect { aState ->
                val currentTrack = aState.currentTrack
                if (currentTrack != null) {
                    val dur = aState.durationMs.takeIf { it > 0L } ?: currentTrack.durationMs.coerceAtLeast(1L)
                    val prog = if (aState.isEnded) {
                        1f
                    } else {
                        (aState.positionMs.toFloat() / dur.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    }
                    _uiState.update {
                        it.copy(
                            playingAudio = currentTrack,
                            isAudioPlaying = aState.isPlaying && !aState.isEnded,
                            audioProgress = prog
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            playingAudio = null,
                            isAudioPlaying = false,
                            audioProgress = 0f
                        )
                    }
                }
            }
        }
    }

    fun toggleAudioPlayPause() {
        val manager = audioPlaybackManager ?: savedAppContext?.let { getOrCreateAudioPlaybackManager(it) } ?: return
        manager.togglePlayPause()
    }

    fun seekAudio(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        val manager = audioPlaybackManager ?: savedAppContext?.let { getOrCreateAudioPlaybackManager(it) }
        if (manager != null) {
            val dur = manager.state.value.durationMs.takeIf { it > 0L }
                ?: _uiState.value.playingAudio?.durationMs
                ?: 1L
            manager.seekTo((clamped * dur.coerceAtLeast(1L)).toLong())
        }
        _uiState.update { it.copy(audioProgress = clamped) }
    }

    fun skipAudioNext() {
        val manager = audioPlaybackManager ?: savedAppContext?.let { getOrCreateAudioPlaybackManager(it) } ?: return
        manager.skipNext()
    }

    fun skipAudioPrevious() {
        val manager = audioPlaybackManager ?: savedAppContext?.let { getOrCreateAudioPlaybackManager(it) } ?: return
        manager.skipPrevious()
    }

    private fun pauseAudioSession() {
        audioPlaybackManager?.pause()
        _uiState.update { it.copy(isAudioPlaying = false) }
    }

    override fun onCleared() {
        super.onCleared()
        audioProgressJob?.cancel()
        videoPlayerController?.destroy()
        videoPlayerController = null
    }
}
