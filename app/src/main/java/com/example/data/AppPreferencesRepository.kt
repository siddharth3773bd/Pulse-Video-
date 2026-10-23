package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.BottomTab
import com.example.model.DecoderMode
import com.example.model.LayoutMode
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
    val showResolutionTier: Boolean = true,
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
    private val KEY_SHOW_RESOLUTION = booleanPreferencesKey("show_resolution_tier")
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
            val showResolution = prefs[KEY_SHOW_RESOLUTION] ?: true
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
                showRemainingTime = prefs[KEY_SHOW_REMAINING_TIME] ?: false
            )

            StoredAppPreferences(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                colorTheme = colorTheme,
                customColorArgb = customColorArgb,
                amoledBlack = amoledBlack,
                showResolutionTier = showResolution,
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

    private fun parseActiveTabs(raw: String?): List<BottomTab> {
        if (raw.isNullOrBlank()) return BottomTab.entries
        val ids = raw.split(",").map { it.trim() }.toSet()
        // Always preserve fixed order: Folders, Audio, Videos, Me
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

    suspend fun updateShowResolutionTier(context: Context, enabled: Boolean) {
        context.appDataStore.edit { it[KEY_SHOW_RESOLUTION] = enabled }
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
