package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

const val MAX_HISTORY_ITEMS = 300

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
) {
    /** The folder the file is in, as a full path. Two folders with the same name stay apart. */
    val folderPath: String
        get() = path.substringBeforeLast('/', folder).trimEnd('/')
}

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
    val path: String = "",
    /** MediaStore IS_RECORDING (Android 12+): the file was made by a recorder app. */
    val isRecording: Boolean = false
) {
    /** The folder the file sits in, e.g. "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio". */
    val folderPath: String
        get() = path.substringBeforeLast('/', "").trimEnd('/')

    /** Voice/call recordings: flagged by MediaStore or stored in a Recordings folder. */
    val looksLikeRecording: Boolean
        get() {
            if (isRecording) return true
            val p = path.lowercase()
            return p.contains("/recordings/") || p.startsWith("recordings/") ||
                p.contains("/call recordings/") || p.contains("/recorder/") ||
                p.contains("/voice recorder/") || p.contains("/sound recordings/")
        }
}

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

/** The order most people want for each sort field: names A to Z; everything else biggest/newest first. */
val SortBy.naturalDir: SortDir
    get() = if (this == SortBy.NAME) SortDir.ASC else SortDir.DESC

/** How the video fills the screen. */
enum class VideoAspectMode(val label: String) {
    FIT("Fit"),
    CROP("Crop"),
    STRETCH("Stretch")
}

/** Subtitle text size, as a fraction of the video height (Media3 SubtitleView). */
enum class SubtitleSize(val id: String, val label: String, val fraction: Float) {
    SMALL("small", "Small", 0.045f),
    MEDIUM("medium", "Medium", 0.0533f),
    LARGE("large", "Large", 0.066f)
}

enum class BottomTab(val id: String, val label: String) {
    AUDIO("audio", "Music"),
    VIDEOS("videos", "Videos"),
    ME("me", "Me")
}

enum class MeSubPage(val id: String, val title: String) {
    NONE("none", "Me"),
    PLAYLISTS("playlists", "Video Playlist"),
    HISTORY("history", "Video History"),
    APPEARANCE("appearance", "Appearance"),
    VIDEO_SETTINGS("video_settings", "Video"),
    MUSIC_SETTINGS("music_settings", "Music"),
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
    val showRemainingTime: Boolean = false,
    /** When a video ends, play the next one from the same folder. */
    val autoplayNext: Boolean = true,
    /** Reopening a video carries on where it was left (with a "Start over" option). */
    val rememberPosition: Boolean = true,
    /** Keep playing in a small window when leaving the app during a video. */
    val pictureInPicture: Boolean = true,
    val subtitleSize: SubtitleSize = SubtitleSize.MEDIUM,
    /** Dark box behind subtitles (off = outlined text only). */
    val subtitleBackground: Boolean = true
)

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
