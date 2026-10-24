package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

const val MAX_HISTORY_ITEMS = 25

@Immutable
data class VideoItem(
    val mediaId: Long,
    val id: String,
    val contentUri: Uri,
    val title: String,
    val displayName: String,
    val ext: String,
    val series: String? = null,
    val episode: Int? = null,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val sizeMB: Double,
    val dateStr: String,
    val timestamp: Long,
    val durationMs: Long,
    val durationSec: Int,
    val durationLabel: String,
    val progress: Float,
    val folder: String,
    val path: String
)

@Immutable
data class AudioTrackItem(
    val mediaId: Long,
    val id: String,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val ext: String,
    val durationMs: Long,
    val durationSec: Int,
    val durationLabel: String,
    val sizeBytes: Long,
    val sizeMB: Double,
    val folder: String,
    val albumId: Long = 0L,
    val artistId: Long = 0L,
    val dateAddedSec: Long = 0L,
    val path: String = ""
)

@Immutable
data class PlaylistItem(
    val id: String,
    val name: String,
    val mediaUris: List<String>
)

@Immutable
data class VideoHistoryEntry(
    val mediaUri: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val hasValidResumePoint: Boolean
        get() = positionMs >= 5_000L && progress <= 0.95f
}

@Immutable
data class ResumePromptTarget(
    val video: VideoItem,
    val resumePositionMs: Long
)

enum class ViewMode(val id: String, val label: String) {
    VIDEOS("videos", "Videos"),
    FOLDERS("folders", "Folders"),
    TREE("tree", "Tree")
}

enum class LayoutMode(val id: String, val label: String) {
    LIST("list", "List"),
    GRID("grid", "Grid")
}

enum class SortBy(
    val id: String,
    val label: String,
    val ascShort: String,
    val descShort: String,
    val ascFull: String,
    val descFull: String
) {
    NAME("name", "Name", "A–Z", "Z–A", "A to Z", "Z to A"),
    DATE("date", "Date", "Oldest", "Newest", "Oldest first", "Newest first"),
    DURATION("duration", "Duration", "Shortest", "Longest", "Shortest first", "Longest first"),
    SIZE("size", "Size", "Smallest", "Largest", "Smallest first", "Largest first")
}

enum class SortDir {
    ASC, DESC
}

enum class BottomTab(val id: String, val label: String) {
    FOLDERS("folders", "Folders"),
    AUDIO("audio", "Audio"),
    VIDEOS("videos", "Videos"),
    ME("me", "Me")
}

enum class MeSubPage(val id: String, val title: String) {
    NONE("none", "Me"),
    PLAYLISTS("playlists", "Video Playlist"),
    HISTORY("history", "Video History"),
    APPEARANCE("appearance", "Appearance"),
    VIDEO_SETTINGS("video_settings", "Video"),
    NAVIGATION("navigation", "Navigation")
}

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark")
}

enum class ScreenOrientationMode(val id: String, val label: String) {
    SYSTEM("system", "System"),
    PORTRAIT("portrait", "Portrait"),
    LANDSCAPE("landscape", "Landscape")
}

enum class DecoderMode(val id: String, val label: String) {
    AUTO("auto", "Auto"),
    HARDWARE("hardware", "Hardware"),
    SOFTWARE("software", "Software")
}

@Immutable
data class PlaybackSettings(
    val doubleTapToSeek: Boolean = true,
    val skipDurationSec: Int = 10,
    val longPressFor2xSpeed: Boolean = true,
    val doubleTapToPause: Boolean = true,
    val horizontalSwipeToSeek: Boolean = true,
    val rightSwipeForVolume: Boolean = true,
    val leftSwipeForBrightness: Boolean = true,
    val forceOrientation: ScreenOrientationMode = ScreenOrientationMode.SYSTEM,
    val defaultDecoder: DecoderMode = DecoderMode.AUTO,
    val showRemainingTime: Boolean = false
)

/**
 * Shared resolution badge function used everywhere across the app.
 * Orientation never matters. Never shows raw dimensions. Returns null if width or height is 0 or missing.
 */
fun resolutionLabel(width: Int, height: Int): String? {
    if (width <= 0 || height <= 0) return null
    val shortest = min(width, height)
    val longest = max(width, height)
    val effective = max(shortest, longest * 9 / 16)
    return when {
        effective >= 4300 -> "8K"
        effective >= 2100 -> "4K"
        effective >= 1400 -> "1440p"
        effective >= 1000 -> "1080p"
        effective >= 700 -> "720p"
        else -> "SD"
    }
}

fun formatCompactDate(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    return dateStr
        .replace("January", "Jan")
        .replace("February", "Feb")
        .replace("March", "Mar")
        .replace("April", "Apr")
        .replace("June", "Jun")
        .replace("July", "Jul")
        .replace("August", "Aug")
        .replace("September", "Sept")
        .replace("October", "Oct")
        .replace("November", "Nov")
        .replace("December", "Dec")
}

fun formatLastWatchedDate(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return formatter.format(Date(timestampMs))
}

fun formatSizeLabel(sizeMB: Double): String {
    return if (sizeMB >= 1024.0) {
        String.format(Locale.US, "%.2f GB", sizeMB / 1024.0)
    } else if (sizeMB >= 10.0) {
        String.format(Locale.US, "%.1f MB", sizeMB)
    } else {
        String.format(Locale.US, "%.2f MB", sizeMB)
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}

private val sxxExxRegex = Regex("""(?i)^(.+?)[._\-\s]+S(\d{1,2})[._\-\s]*E(\d{1,3})\b""")
private val nxnnRegex = Regex("""(?i)^(.+?)[._\-\s]+(\d{1,2})x(\d{1,3})\b""")
private val epRegex = Regex("""(?i)^(.+?)[._\-\s]+(?:Ep|Episode|Part|Pt)[._\-\s]*(\d{1,3})\b""")

/**
 * Extracts TV series title and episode number from real video file names.
 */
fun parseSeriesAndEpisode(rawTitle: String): Pair<String, Int>? {
    sxxExxRegex.find(rawTitle)?.let { match ->
        val rawSeries = match.groupValues[1].replace(Regex("""[._]+"""), " ").trim()
        val seasonNum = match.groupValues[2].toIntOrNull() ?: 1
        val epNum = match.groupValues[3].toIntOrNull() ?: return null
        if (rawSeries.isNotEmpty()) {
            return "$rawSeries · Season $seasonNum" to epNum
        }
    }
    nxnnRegex.find(rawTitle)?.let { match ->
        val rawSeries = match.groupValues[1].replace(Regex("""[._]+"""), " ").trim()
        val seasonNum = match.groupValues[2].toIntOrNull() ?: 1
        val epNum = match.groupValues[3].toIntOrNull() ?: return null
        if (rawSeries.isNotEmpty()) {
            return "$rawSeries · Season $seasonNum" to epNum
        }
    }
    epRegex.find(rawTitle)?.let { match ->
        val rawSeries = match.groupValues[1].replace(Regex("""[._]+"""), " ").trim()
        val epNum = match.groupValues[2].toIntOrNull() ?: return null
        if (rawSeries.isNotEmpty()) {
            return rawSeries to epNum
        }
    }
    return null
}
