package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AmbientSettings
import com.example.model.BottomTab
import com.example.model.DecoderMode
import com.example.model.LayoutMode
import com.example.model.AudioPlaylist
import com.example.model.AudioTrackItem
import com.example.model.BlacklistedSong
import com.example.model.LibraryPreferences
import com.example.model.LibrarySongSort
import com.example.model.LibraryTab
import com.example.model.NowPlayingBackground
import com.example.model.NowPlayingColorSource
import com.example.model.NowPlayingAction
import com.example.model.MAX_HISTORY_ITEMS
import com.example.model.PlaybackSettings
import com.example.model.PlaylistItem
import com.example.model.ScreenOrientationMode
import com.example.model.SortBy
import com.example.model.SortDir
import com.example.model.ThemeMode
import com.example.model.VideoHistoryEntry
import com.example.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "videos_app_datastore")

data class StoredAppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val colorTheme: String = "indigo",
    val customColorArgb: Int = 0xFF6366F1.toInt(),
    val amoledBlack: Boolean = false,
    val activeTabs: List<BottomTab> = BottomTab.entries,
    val lastBottomTab: BottomTab = BottomTab.VIDEOS,
    val smartGroups: Boolean = false,
    val viewMode: ViewMode = ViewMode.VIDEOS,
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val sortBy: SortBy = SortBy.DATE,
    val sortDir: SortDir = SortDir.DESC,
    val playbackSettings: PlaybackSettings = PlaybackSettings(),
    val playlists: List<PlaylistItem> = emptyList(),
    val historyEntries: List<VideoHistoryEntry> = emptyList()
)

object AppPreferencesRepository {
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    private val KEY_COLOR_THEME = stringPreferencesKey("color_theme")
    private val KEY_CUSTOM_COLOR_ARGB = intPreferencesKey("custom_color_argb")
    private val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
    private val KEY_ACTIVE_TABS = stringPreferencesKey("active_bottom_tabs")
    private val KEY_LAST_BOTTOM_TAB = stringPreferencesKey("last_bottom_tab")
    private val KEY_SMART_GROUPS = booleanPreferencesKey("smart_groups")
    private val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
    private val KEY_LAYOUT_MODE = stringPreferencesKey("layout_mode")
    private val KEY_SORT_BY = stringPreferencesKey("sort_by")
    private val KEY_SORT_DIR = stringPreferencesKey("sort_dir")

    // Playback settings keys
    private val KEY_DOUBLE_TAP_SEEK = booleanPreferencesKey("pb_double_tap_seek")
    private val KEY_SKIP_DURATION = intPreferencesKey("pb_skip_duration_sec")
    private val KEY_LONG_PRESS_2X = booleanPreferencesKey("pb_long_press_2x")
    private val KEY_DOUBLE_TAP_PAUSE = booleanPreferencesKey("pb_double_tap_pause")
    private val KEY_HORIZ_SWIPE_SEEK = booleanPreferencesKey("pb_horiz_swipe_seek")
    private val KEY_RIGHT_SWIPE_VOL = booleanPreferencesKey("pb_right_swipe_vol")
    private val KEY_LEFT_SWIPE_BRIGHT = booleanPreferencesKey("pb_left_swipe_bright")
    private val KEY_FORCE_ORIENTATION = stringPreferencesKey("pb_force_orientation")
    private val KEY_DEFAULT_DECODER = stringPreferencesKey("pb_default_decoder")
    private val KEY_SHOW_REMAINING_TIME = booleanPreferencesKey("pb_show_remaining_time")
    private val KEY_AUTOPLAY_NEXT = booleanPreferencesKey("pb_autoplay_next")
    private val KEY_REMEMBER_POSITION = booleanPreferencesKey("pb_remember_position")
    private val KEY_PREF_AUDIO_LANG = stringPreferencesKey("pb_pref_audio_lang")
    private val KEY_PREF_SUB_LANG = stringPreferencesKey("pb_pref_sub_lang")
    private val KEY_COLORFUL_CONTROLS = booleanPreferencesKey("pb_colorful_controls")
    private val KEY_PIP = booleanPreferencesKey("pb_picture_in_picture")
    private val KEY_SUB_SIZE = stringPreferencesKey("pb_subtitle_size")
    private val KEY_SUB_BG = booleanPreferencesKey("pb_subtitle_background")

    // Audio Library screen
    private val KEY_LIB_TAB_ORDER = stringPreferencesKey("lib_tab_order")
    private val KEY_LIB_HIDDEN_TABS = stringPreferencesKey("lib_hidden_tabs")
    private val KEY_LIB_SONG_SORT = stringPreferencesKey("lib_song_sort")
    private val KEY_LIB_SONG_ASC = booleanPreferencesKey("lib_song_asc")
    private val KEY_LIB_GROUP_ASC = booleanPreferencesKey("lib_group_asc")
    private val KEY_LIB_TRAY_ORDER = stringPreferencesKey("lib_tray_order")
    private val KEY_LIB_TRAY_HIDDEN = stringPreferencesKey("lib_tray_hidden")
    private val KEY_LIB_LIKED_IDS = stringSetPreferencesKey("lib_liked_ids")
    private val KEY_LIB_HIDE_SHORT = booleanPreferencesKey("lib_hide_short_audio")
    private val KEY_LIB_HIDE_RECORDINGS = booleanPreferencesKey("lib_hide_recordings")
    private val KEY_NP_COLOR_SOURCE = stringPreferencesKey("np_color_source")
    private val KEY_NP_BACKGROUND = stringPreferencesKey("np_background")
    // New key: online search is on for everyone by default now (it can still be turned off).
    private val KEY_LYR_AUTO = booleanPreferencesKey("lyrics_auto_search_v2")
    private val KEY_LYR_SIZE = stringPreferencesKey("lyrics_text_size")
    private val KEY_LYR_CENTER = booleanPreferencesKey("lyrics_align_center")
    private val KEY_LYR_BLUR = booleanPreferencesKey("lyrics_blur")
    private val KEY_LYR_ROMAN = booleanPreferencesKey("lyrics_romanize")
    private val KEY_AMB_ART = booleanPreferencesKey("ambient_show_art")
    private val KEY_AMB_TITLE = booleanPreferencesKey("ambient_show_title")
    private val KEY_AMB_ARTIST = booleanPreferencesKey("ambient_show_artist")
    private val KEY_AMB_LYRICS = booleanPreferencesKey("ambient_show_lyrics")
    private val KEY_AMB_ROTATE = booleanPreferencesKey("ambient_open_on_rotate")
    private val KEY_NP_KEEP_SCREEN_ON = booleanPreferencesKey("now_playing_keep_screen_on")
    private val KEY_AMB_BLACK = booleanPreferencesKey("ambient_black")
    private val KEY_LIB_BLACKLIST_SONGS = stringPreferencesKey("lib_blacklist_songs_json")
    private val KEY_LIB_BLACKLIST_FOLDERS = stringSetPreferencesKey("lib_blacklist_folders")
    private val KEY_LIB_PLAYLISTS = stringPreferencesKey("lib_audio_playlists_json")

    // Playlists & History
    private val KEY_PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
    private val KEY_HISTORY_JSON = stringPreferencesKey("history_entries_json")

    fun observePreferences(context: Context): Flow<StoredAppPreferences> {
        return context.appDataStore.data.map { prefs ->
            val themeMode = ThemeMode.entries.find { it.id == prefs[KEY_THEME_MODE] } ?: ThemeMode.SYSTEM
            val dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true
            val colorTheme = prefs[KEY_COLOR_THEME] ?: "indigo"
            val customColorArgb = prefs[KEY_CUSTOM_COLOR_ARGB] ?: 0xFF6366F1.toInt()
            val amoledBlack = prefs[KEY_AMOLED_BLACK] ?: false
            val activeTabs = parseActiveTabs(prefs[KEY_ACTIVE_TABS])
            val lastBottomTab = BottomTab.entries.find { it.id == prefs[KEY_LAST_BOTTOM_TAB] } ?: BottomTab.VIDEOS
            val smartGroups = prefs[KEY_SMART_GROUPS] ?: false
            val viewMode = ViewMode.entries.find { it.id == prefs[KEY_VIEW_MODE] } ?: ViewMode.VIDEOS
            val layoutMode = LayoutMode.entries.find { it.id == prefs[KEY_LAYOUT_MODE] } ?: LayoutMode.LIST
            val sortBy = SortBy.entries.find { it.id == prefs[KEY_SORT_BY] } ?: SortBy.DATE
            val sortDir = SortDir.entries.find { it.name == prefs[KEY_SORT_DIR] } ?: SortDir.DESC

            val rawSkip = prefs[KEY_SKIP_DURATION] ?: 10
            val snappedSkip = ((rawSkip + 2) / 5 * 5).coerceIn(5, 30)

            val playbackSettings = PlaybackSettings(
                doubleTapToSeek = prefs[KEY_DOUBLE_TAP_SEEK] ?: true,
                skipDurationSec = snappedSkip,
                longPressFor2xSpeed = prefs[KEY_LONG_PRESS_2X] ?: true,
                doubleTapToPause = prefs[KEY_DOUBLE_TAP_PAUSE] ?: true,
                horizontalSwipeToSeek = prefs[KEY_HORIZ_SWIPE_SEEK] ?: true,
                rightSwipeForVolume = prefs[KEY_RIGHT_SWIPE_VOL] ?: true,
                leftSwipeForBrightness = prefs[KEY_LEFT_SWIPE_BRIGHT] ?: true,
                forceOrientation = ScreenOrientationMode.entries.find { it.id == prefs[KEY_FORCE_ORIENTATION] } ?: ScreenOrientationMode.SYSTEM,
                defaultDecoder = DecoderMode.entries.find { it.id == prefs[KEY_DEFAULT_DECODER] } ?: DecoderMode.AUTO,
                showRemainingTime = prefs[KEY_SHOW_REMAINING_TIME] ?: false,
                autoplayNext = prefs[KEY_AUTOPLAY_NEXT] ?: true,
                rememberPosition = prefs[KEY_REMEMBER_POSITION] ?: true,
                preferredAudioLanguage = prefs[KEY_PREF_AUDIO_LANG] ?: "",
                preferredSubtitleLanguage = prefs[KEY_PREF_SUB_LANG] ?: "",
                colorfulControls = prefs[KEY_COLORFUL_CONTROLS] ?: false,
                pictureInPicture = prefs[KEY_PIP] ?: true,
                subtitleSize = com.example.model.SubtitleSize.entries.find { it.id == prefs[KEY_SUB_SIZE] }
                    ?: com.example.model.SubtitleSize.MEDIUM,
                subtitleBackground = prefs[KEY_SUB_BG] ?: true
            )

            StoredAppPreferences(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                colorTheme = colorTheme,
                customColorArgb = customColorArgb,
                amoledBlack = amoledBlack,
                activeTabs = activeTabs,
                lastBottomTab = lastBottomTab,
                smartGroups = smartGroups,
                viewMode = viewMode,
                layoutMode = layoutMode,
                sortBy = sortBy,
                sortDir = sortDir,
                playbackSettings = playbackSettings,
                playlists = deserializePlaylists(prefs[KEY_PLAYLISTS_JSON]),
                historyEntries = deserializeHistory(prefs[KEY_HISTORY_JSON])
            )
        }
    }

    fun observeLibraryPreferences(context: Context): Flow<LibraryPreferences> {
        return context.appDataStore.data.map { prefs ->
            val savedOrder = prefs[KEY_LIB_TAB_ORDER]
                ?.split(",")
                ?.mapNotNull { id -> LibraryTab.entries.find { it.id == id.trim() } }
                ?.distinct()
                .orEmpty()
            // Keep the saved order, then append any tab that is missing so nothing is ever lost.
            val order = savedOrder + LibraryTab.entries.filter { it !in savedOrder }
            val hidden = prefs[KEY_LIB_HIDDEN_TABS]
                ?.split(",")
                ?.mapNotNull { id -> LibraryTab.entries.find { it.id == id.trim() } }
                ?.toSet()
                .orEmpty()
            // At least one tab must stay visible.
            val safeHidden = if (order.all { it in hidden }) emptySet() else hidden
            val savedTray = prefs[KEY_LIB_TRAY_ORDER]
                ?.split(",")
                ?.mapNotNull { id -> NowPlayingAction.entries.find { it.id == id.trim() } }
                ?.distinct()
                .orEmpty()
            val trayOrder = savedTray + NowPlayingAction.entries.filter { it !in savedTray }
            val trayHidden = prefs[KEY_LIB_TRAY_HIDDEN]
                ?.split(",")
                ?.mapNotNull { id -> NowPlayingAction.entries.find { it.id == id.trim() } }
                ?.toSet()
                .orEmpty()
            LibraryPreferences(
                tabOrder = order,
                hiddenTabs = safeHidden,
                songSort = LibrarySongSort.entries.find { it.id == prefs[KEY_LIB_SONG_SORT] } ?: LibrarySongSort.DATE_ADDED,
                // "Default" (scan order = newest first) became Date added; its direction is flipped.
                songAscending = when {
                    prefs[KEY_LIB_SONG_SORT] == null -> false
                    LibrarySongSort.entries.none { it.id == prefs[KEY_LIB_SONG_SORT] } -> !(prefs[KEY_LIB_SONG_ASC] ?: true)
                    else -> prefs[KEY_LIB_SONG_ASC] ?: true
                },
                groupAscending = prefs[KEY_LIB_GROUP_ASC] ?: true,
                trayOrder = trayOrder,
                hiddenTray = trayHidden,
                likedIds = prefs[KEY_LIB_LIKED_IDS] ?: emptySet(),
                hideShortAudio = prefs[KEY_LIB_HIDE_SHORT] ?: true,
                hideRecordings = prefs[KEY_LIB_HIDE_RECORDINGS] ?: true,
                nowPlayingColorSource = NowPlayingColorSource.entries.find { it.id == prefs[KEY_NP_COLOR_SOURCE] }
                    ?: NowPlayingColorSource.ALBUM_ART,
                nowPlayingBackground = NowPlayingBackground.entries.find { it.id == prefs[KEY_NP_BACKGROUND] }
                    ?: NowPlayingBackground.GRADIENT,
                blacklistedSongs = parseBlacklistedSongs(prefs[KEY_LIB_BLACKLIST_SONGS]),
                blacklistedFolders = prefs[KEY_LIB_BLACKLIST_FOLDERS] ?: emptySet(),
                playlists = parseAudioPlaylists(prefs[KEY_LIB_PLAYLISTS]),
                ambient = AmbientSettings(
                    showArtwork = prefs[KEY_AMB_ART] ?: true,
                    showTitle = prefs[KEY_AMB_TITLE] ?: true,
                    showArtist = prefs[KEY_AMB_ARTIST] ?: true,
                    showLyrics = prefs[KEY_AMB_LYRICS] ?: true,
                    black = prefs[KEY_AMB_BLACK] ?: false,
                    openOnRotate = prefs[KEY_AMB_ROTATE] ?: false
                ),
                keepScreenOn = prefs[KEY_NP_KEEP_SCREEN_ON] ?: false,
                lyrics = com.example.lyrics.LyricsSettings(
                    autoSearch = prefs[KEY_LYR_AUTO] ?: true,
                    textSize = com.example.lyrics.LyricsTextSize.entries.find { it.id == prefs[KEY_LYR_SIZE] }
                        ?: com.example.lyrics.LyricsTextSize.MEDIUM,
                    alignCenter = prefs[KEY_LYR_CENTER] ?: false,
                    blur = prefs[KEY_LYR_BLUR] ?: true,
                    romanize = prefs[KEY_LYR_ROMAN] ?: false
                )
            )
        }
    }

    suspend fun updateLibraryTabs(context: Context, order: List<LibraryTab>, hidden: Set<LibraryTab>) {
        val safeHidden = if (order.all { it in hidden }) emptySet() else hidden
        context.appDataStore.edit {
            it[KEY_LIB_TAB_ORDER] = order.joinToString(",") { tab -> tab.id }
            it[KEY_LIB_HIDDEN_TABS] = safeHidden.joinToString(",") { tab -> tab.id }
        }
    }

    suspend fun updateLibraryTray(context: Context, order: List<NowPlayingAction>, hidden: Set<NowPlayingAction>) {
        context.appDataStore.edit {
            it[KEY_LIB_TRAY_ORDER] = order.joinToString(",") { action -> action.id }
            it[KEY_LIB_TRAY_HIDDEN] = hidden.joinToString(",") { action -> action.id }
        }
    }

    suspend fun setTrackLiked(context: Context, trackId: String, liked: Boolean) {
        context.appDataStore.edit {
            val current = it[KEY_LIB_LIKED_IDS] ?: emptySet()
            it[KEY_LIB_LIKED_IDS] = if (liked) current + trackId else current - trackId
        }
    }

    suspend fun setTracksLiked(context: Context, trackIds: Collection<String>, liked: Boolean) {
        context.appDataStore.edit {
            val current = it[KEY_LIB_LIKED_IDS] ?: emptySet()
            it[KEY_LIB_LIKED_IDS] = if (liked) current + trackIds else current - trackIds.toSet()
        }
    }

    // ---- Blacklist -------------------------------------------------------------------------

    suspend fun blacklistSongs(context: Context, tracks: List<AudioTrackItem>) {
        context.appDataStore.edit {
            val current = parseBlacklistedSongs(it[KEY_LIB_BLACKLIST_SONGS])
            val known = current.mapTo(HashSet()) { s -> s.id }
            val added = tracks.filter { t -> t.id !in known }
                .map { t -> BlacklistedSong(id = t.id, title = t.title, artist = t.artist) }
            it[KEY_LIB_BLACKLIST_SONGS] = serializeBlacklistedSongs(current + added)
        }
    }

    suspend fun removeBlacklistedSong(context: Context, id: String) {
        context.appDataStore.edit {
            val current = parseBlacklistedSongs(it[KEY_LIB_BLACKLIST_SONGS])
            it[KEY_LIB_BLACKLIST_SONGS] = serializeBlacklistedSongs(current.filter { s -> s.id != id })
        }
    }

    suspend fun setFolderBlacklisted(context: Context, folderPath: String, blacklisted: Boolean) {
        if (folderPath.isBlank()) return
        context.appDataStore.edit {
            val current = it[KEY_LIB_BLACKLIST_FOLDERS] ?: emptySet()
            it[KEY_LIB_BLACKLIST_FOLDERS] = if (blacklisted) current + folderPath else current - folderPath
        }
    }

    private fun serializeBlacklistedSongs(songs: List<BlacklistedSong>): String {
        val arr = JSONArray()
        songs.forEach { song ->
            arr.put(JSONObject().put("id", song.id).put("title", song.title).put("artist", song.artist))
        }
        return arr.toString()
    }

    private fun parseBlacklistedSongs(json: String?): List<BlacklistedSong> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id")
                if (id.isBlank()) null
                else BlacklistedSong(id = id, title = obj.optString("title", "Unknown"), artist = obj.optString("artist", ""))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---- Music playlists -------------------------------------------------------------------

    private suspend fun editAudioPlaylists(context: Context, transform: (List<AudioPlaylist>) -> List<AudioPlaylist>) {
        context.appDataStore.edit {
            val current = parseAudioPlaylists(it[KEY_LIB_PLAYLISTS])
            it[KEY_LIB_PLAYLISTS] = serializeAudioPlaylists(transform(current))
        }
    }

    /** Creates a playlist holding [trackIds] and returns its id. */
    suspend fun createAudioPlaylist(context: Context, name: String, trackIds: List<String> = emptyList()): String {
        val id = "apl_${System.currentTimeMillis()}"
        editAudioPlaylists(context) { it + AudioPlaylist(id = id, name = name.trim().ifBlank { "Playlist" }, trackIds = trackIds.distinct()) }
        return id
    }

    suspend fun renameAudioPlaylist(context: Context, id: String, name: String) {
        editAudioPlaylists(context) { list ->
            list.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it }
        }
    }

    suspend fun deleteAudioPlaylist(context: Context, id: String) {
        editAudioPlaylists(context) { list -> list.filter { it.id != id } }
    }

    suspend fun addToAudioPlaylist(context: Context, id: String, trackIds: List<String>) {
        editAudioPlaylists(context) { list ->
            list.map { if (it.id == id) it.copy(trackIds = (it.trackIds + trackIds).distinct()) else it }
        }
    }

    suspend fun removeFromAudioPlaylist(context: Context, id: String, trackIds: Collection<String>) {
        val remove = trackIds.toSet()
        editAudioPlaylists(context) { list ->
            list.map { if (it.id == id) it.copy(trackIds = it.trackIds.filter { t -> t !in remove }) else it }
        }
    }

    private fun serializeAudioPlaylists(playlists: List<AudioPlaylist>): String {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val ids = JSONArray()
            pl.trackIds.forEach { ids.put(it) }
            arr.put(JSONObject().put("id", pl.id).put("name", pl.name).put("tracks", ids))
        }
        return arr.toString()
    }

    private fun parseAudioPlaylists(json: String?): List<AudioPlaylist> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val ids = obj.optJSONArray("tracks") ?: JSONArray()
                AudioPlaylist(
                    id = obj.optString("id", "apl_$i"),
                    name = obj.optString("name", "Playlist"),
                    trackIds = (0 until ids.length()).map { j -> ids.getString(j) }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun setNowPlayingColorSource(context: Context, value: NowPlayingColorSource) {
        context.appDataStore.edit { it[KEY_NP_COLOR_SOURCE] = value.id }
    }

    suspend fun setNowPlayingBackground(context: Context, value: NowPlayingBackground) {
        context.appDataStore.edit { it[KEY_NP_BACKGROUND] = value.id }
    }

    suspend fun setLibraryHideShortAudio(context: Context, hide: Boolean) {
        context.appDataStore.edit { it[KEY_LIB_HIDE_SHORT] = hide }
    }

    suspend fun setLibraryHideRecordings(context: Context, hide: Boolean) {
        context.appDataStore.edit { it[KEY_LIB_HIDE_RECORDINGS] = hide }
    }

    suspend fun updateLibrarySongSort(context: Context, sort: LibrarySongSort, ascending: Boolean) {
        context.appDataStore.edit {
            it[KEY_LIB_SONG_SORT] = sort.id
            it[KEY_LIB_SONG_ASC] = ascending
        }
    }

    suspend fun updateLyricsSettings(context: Context, value: com.example.lyrics.LyricsSettings) {
        context.appDataStore.edit {
            it[KEY_LYR_AUTO] = value.autoSearch
            it[KEY_LYR_SIZE] = value.textSize.id
            it[KEY_LYR_CENTER] = value.alignCenter
            it[KEY_LYR_BLUR] = value.blur
            it[KEY_LYR_ROMAN] = value.romanize
        }
    }

    suspend fun updateAmbientSettings(context: Context, value: AmbientSettings) {
        context.appDataStore.edit {
            it[KEY_AMB_ART] = value.showArtwork
            it[KEY_AMB_TITLE] = value.showTitle
            it[KEY_AMB_ARTIST] = value.showArtist
            it[KEY_AMB_LYRICS] = value.showLyrics
            it[KEY_AMB_BLACK] = value.black
            it[KEY_AMB_ROTATE] = value.openOnRotate
        }
    }

    suspend fun setNowPlayingKeepScreenOn(context: Context, on: Boolean) {
        context.appDataStore.edit { it[KEY_NP_KEEP_SCREEN_ON] = on }
    }

    suspend fun updateLibraryGroupAscending(context: Context, ascending: Boolean) {
        context.appDataStore.edit { it[KEY_LIB_GROUP_ASC] = ascending }
    }

    private fun parseActiveTabs(raw: String?): List<BottomTab> {
        if (raw.isNullOrBlank()) return BottomTab.entries
        val ids = raw.split(",").map { it.trim() }.toSet()
        // Always preserve fixed order: Audio, Videos, Me
        val ordered = BottomTab.entries.filter { it.id in ids }
        return ordered.ifEmpty { BottomTab.entries }
    }

    suspend fun updateThemeMode(context: Context, mode: ThemeMode) {
        context.appDataStore.edit { it[KEY_THEME_MODE] = mode.id }
    }

    suspend fun updateDynamicColor(context: Context, enabled: Boolean) {
        context.appDataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateColorTheme(context: Context, themeId: String) {
        context.appDataStore.edit { it[KEY_COLOR_THEME] = themeId }
    }

    suspend fun updateCustomColor(context: Context, argb: Int) {
        context.appDataStore.edit {
            it[KEY_CUSTOM_COLOR_ARGB] = argb
            it[KEY_COLOR_THEME] = "custom"
        }
    }

    suspend fun updateAmoledBlack(context: Context, enabled: Boolean) {
        context.appDataStore.edit { it[KEY_AMOLED_BLACK] = enabled }
    }

    suspend fun updateActiveTabs(context: Context, tabs: List<BottomTab>) {
        val ordered = BottomTab.entries.filter { it in tabs }.ifEmpty { listOf(BottomTab.VIDEOS) }
        context.appDataStore.edit {
            it[KEY_ACTIVE_TABS] = ordered.joinToString(",") { tab -> tab.id }
        }
    }

    suspend fun updateLastBottomTab(context: Context, tab: BottomTab) {
        context.appDataStore.edit { it[KEY_LAST_BOTTOM_TAB] = tab.id }
    }

    suspend fun updateSmartGroups(context: Context, enabled: Boolean) {
        context.appDataStore.edit { it[KEY_SMART_GROUPS] = enabled }
    }

    suspend fun updateViewMode(context: Context, mode: ViewMode) {
        context.appDataStore.edit { it[KEY_VIEW_MODE] = mode.id }
    }

    suspend fun updateLayoutMode(context: Context, mode: LayoutMode) {
        context.appDataStore.edit { it[KEY_LAYOUT_MODE] = mode.id }
    }

    suspend fun updateSort(context: Context, sortBy: SortBy, sortDir: SortDir) {
        context.appDataStore.edit {
            it[KEY_SORT_BY] = sortBy.id
            it[KEY_SORT_DIR] = sortDir.name
        }
    }

    suspend fun updatePlaybackSettings(context: Context, settings: PlaybackSettings) {
        context.appDataStore.edit {
            it[KEY_DOUBLE_TAP_SEEK] = settings.doubleTapToSeek
            it[KEY_SKIP_DURATION] = settings.skipDurationSec
            it[KEY_LONG_PRESS_2X] = settings.longPressFor2xSpeed
            it[KEY_DOUBLE_TAP_PAUSE] = settings.doubleTapToPause
            it[KEY_HORIZ_SWIPE_SEEK] = settings.horizontalSwipeToSeek
            it[KEY_RIGHT_SWIPE_VOL] = settings.rightSwipeForVolume
            it[KEY_LEFT_SWIPE_BRIGHT] = settings.leftSwipeForBrightness
            it[KEY_FORCE_ORIENTATION] = settings.forceOrientation.id
            it[KEY_DEFAULT_DECODER] = settings.defaultDecoder.id
            it[KEY_SHOW_REMAINING_TIME] = settings.showRemainingTime
            it[KEY_AUTOPLAY_NEXT] = settings.autoplayNext
            it[KEY_REMEMBER_POSITION] = settings.rememberPosition
            it[KEY_PREF_AUDIO_LANG] = settings.preferredAudioLanguage
            it[KEY_PREF_SUB_LANG] = settings.preferredSubtitleLanguage
            it[KEY_COLORFUL_CONTROLS] = settings.colorfulControls
            it[KEY_PIP] = settings.pictureInPicture
            it[KEY_SUB_SIZE] = settings.subtitleSize.id
            it[KEY_SUB_BG] = settings.subtitleBackground
        }
    }

    suspend fun updatePlaylists(context: Context, playlists: List<PlaylistItem>) {
        context.appDataStore.edit {
            it[KEY_PLAYLISTS_JSON] = serializePlaylists(playlists)
        }
    }

    suspend fun updateHistory(context: Context, entries: List<VideoHistoryEntry>) {
        context.appDataStore.edit {
            it[KEY_HISTORY_JSON] = serializeHistory(entries.take(MAX_HISTORY_ITEMS))
        }
    }

    private fun serializePlaylists(playlists: List<PlaylistItem>): String {
        val arr = JSONArray()
        for (pl in playlists) {
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            val uris = JSONArray()
            pl.mediaUris.forEach { uris.put(it) }
            obj.put("uris", uris)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializePlaylists(json: String?): List<PlaylistItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<PlaylistItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "pl_$i")
                val name = obj.optString("name", "Playlist")
                val urisArr = obj.optJSONArray("uris") ?: JSONArray()
                val uris = mutableListOf<String>()
                for (j in 0 until urisArr.length()) {
                    uris.add(urisArr.getString(j))
                }
                list.add(PlaylistItem(id = id, name = name, mediaUris = uris))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeHistory(entries: List<VideoHistoryEntry>): String {
        val arr = JSONArray()
        for (entry in entries.take(MAX_HISTORY_ITEMS)) {
            val obj = JSONObject()
            obj.put("uri", entry.mediaUri)
            obj.put("pos", entry.positionMs)
            obj.put("dur", entry.durationMs)
            obj.put("ts", entry.lastPlayedTimestamp)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeHistory(json: String?): List<VideoHistoryEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<VideoHistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val uri = obj.optString("uri", "")
                if (uri.isNotBlank()) {
                    list.add(
                        VideoHistoryEntry(
                            mediaUri = uri,
                            positionMs = obj.optLong("pos", 0L),
                            durationMs = obj.optLong("dur", 0L),
                            lastPlayedTimestamp = obj.optLong("ts", 0L)
                        )
                    )
                }
            }
            list
                .sortedByDescending { it.lastPlayedTimestamp }
                .distinctBy { it.mediaUri }
                .take(MAX_HISTORY_ITEMS)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
