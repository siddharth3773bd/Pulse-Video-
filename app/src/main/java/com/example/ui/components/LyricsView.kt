@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.lyrics.LyricLine
import com.example.lyrics.Lyrics
import com.example.lyrics.LyricsRepository
import com.example.lyrics.LyricsSettings
import com.example.lyrics.LyricsState
import com.example.model.AudioTrackItem
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Current lyrics for [track] plus a way to look again ([reload] with forceOnline = search LRCLIB now). */
class LyricsHandle(val state: LyricsState, val reload: (forceOnline: Boolean) -> Unit)

/** Loads lyrics for [track] following the user's rules, and reloads when asked. */
@Composable
fun rememberLyricsState(track: AudioTrackItem, settings: LyricsSettings): LyricsHandle {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var force by remember(track.id) { mutableStateOf(false) }
    var state by remember(track.id) { mutableStateOf<LyricsState>(LyricsState.Loading) }
    LaunchedEffect(track.id, settings.autoSearch, version) {
        // Keep showing loaded lyrics while re-checking (e.g. after a sync change), so nothing flickers.
        if (state !is LyricsState.Loaded || force) state = LyricsState.Loading
        state = LyricsRepository.resolve(context, track, settings, forceOnline = force)
        force = false
    }
    return LyricsHandle(state) { forceOnline ->
        force = forceOnline
        version++
    }
}

/** Playback position that moves smoothly between the player's updates. */
@Composable
fun rememberLivePosition(positionMs: Long, isPlaying: Boolean): State<Long> {
    val live = remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs, isPlaying) {
        live.longValue = positionMs
        if (isPlaying) {
            val start = SystemClock.elapsedRealtime()
            while (true) {
                withFrameMillis { live.longValue = positionMs + (SystemClock.elapsedRealtime() - start) }
            }
        }
    }
    return live
}

/**
 * Lyrics, Echo-style "depth of field": the current line is big, bold and bright; the lines next to
 * it stay sharp but dimmer; lines further away blur and fade. The list glides with a slow,
 * no-bounce spring, keeping the current line about a third of the way down.
 * Tap a line to jump there. Scrolling by hand pauses following for a few seconds.
 */
@Composable
fun LyricsView(
    lyrics: Lyrics,
    positionMs: Long,
    isPlaying: Boolean,
    offsetMs: Long,
    settings: LyricsSettings,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    horizontalPadding: Int = 24,
    /** Room kept free at the bottom (e.g. for player controls) for the "Back to current line" button. */
    bottomInset: Dp = 0.dp
) {
    if (!lyrics.synced) {
        PlainLyrics(lyrics, settings, contentColor, horizontalPadding, modifier)
        return
    }
    val live = rememberLivePosition(positionMs, isPlaying)
    // A long intro gets its own "music only" row with breathing dots before the first line.
    val lines = remember(lyrics) {
        val first = lyrics.lines.firstOrNull()
        if (first != null && first.timeMs >= IntroGapMs && first.text.isNotBlank()) {
            listOf(LyricLine(timeMs = 0L, text = "")) + lyrics.lines
        } else {
            lyrics.lines
        }
    }
    val currentIndex by remember(lines, offsetMs) {
        derivedStateOf {
            val pos = live.value - offsetMs
            lines.indexOfLast { it.timeMs <= pos }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var following by remember { mutableStateOf(true) }
    var firstScroll by remember(lines) { mutableStateOf(true) }

    // Hand-scrolling pauses following; it picks up again by itself a few seconds later.
    LaunchedEffect(dragged) {
        if (dragged) following = false
        else if (!following) {
            delay(3500)
            following = true
        }
    }
    LaunchedEffect(currentIndex, following, lines) {
        if (!following) return@LaunchedEffect
        val target = currentIndex.coerceAtLeast(0)
        if (firstScroll) {
            listState.scrollToItem(target)
            firstScroll = false
            return@LaunchedEffect
        }
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }
        if (info != null) {
            listState.animateScrollBy(
                info.offset.toFloat(),
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
            )
        } else {
            listState.animateScrollToItem(target)
        }
    }

    val align = if (settings.alignCenter) TextAlign.Center else TextAlign.Start
    val origin = TransformOrigin(if (settings.alignCenter) 0.5f else 0f, 0.5f)

    BoxWithConstraints(modifier = modifier.testTag("lyrics_view")) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = horizontalPadding.dp,
                end = horizontalPadding.dp,
                top = maxHeight * 0.3f,
                bottom = maxHeight * 0.6f
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.timeMs}#$index" }) { index, line ->
                val isActive = index == currentIndex
                val distance = if (currentIndex < 0) index + 1 else abs(index - currentIndex)
                SyncedLine(
                    line = line,
                    isActive = isActive,
                    distance = distance,
                    live = live,
                    offsetMs = offsetMs,
                    settings = settings,
                    align = align,
                    origin = origin,
                    color = contentColor,
                    isPlaying = isPlaying,
                    onClick = {
                        onSeekTo((line.timeMs + offsetMs).coerceAtLeast(0L))
                        following = true
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = !following,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp + bottomInset)
        ) {
            FilledTonalButton(
                onClick = {
                    following = true
                    scope.launch {
                        listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
                    }
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = contentColor.copy(alpha = 0.18f),
                    contentColor = contentColor
                ),
                modifier = Modifier.testTag("lyrics_back_to_current")
            ) {
                Icon(Symbols.VerticalAlignCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Back to current line")
            }
        }
    }
}

@Composable
private fun SyncedLine(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    live: State<Long>,
    offsetMs: Long,
    settings: LyricsSettings,
    align: TextAlign,
    origin: TransformOrigin,
    color: Color,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.6f
            distance == 2 -> 0.42f
            else -> 0.3f
        },
        animationSpec = tween(350),
        label = "lyricAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "lyricScale"
    )
    // Near lines stay sharp; blur starts three lines away and grows gently (depth of field).
    val blurTarget = if (!settings.blur || isActive) 0f else when (distance) {
        0, 1, 2 -> 0f
        3 -> 2f
        4 -> 4f
        else -> 6f
    }
    val blur by animateFloatAsState(
        targetValue = blurTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "lyricBlur"
    )

    val size = settings.textSize.activeSp
    val style = TextStyle(
        fontSize = size.sp,
        lineHeight = 1.22.em,
        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
        textAlign = align,
        color = color,
        shadow = if (isActive) Shadow(color = color.copy(alpha = 0.35f), blurRadius = 28f) else null
    )
    // Lines in another script (Hindi, Urdu, Punjabi…) can also be shown in English letters.
    val romanized = if (settings.romanize && line.text.isNotBlank()) remember(line.text) { toEnglishLetters(line.text) } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                transformOrigin = origin
            }
            .then(if (blur > 0.05f) Modifier.blur(blur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        if (line.text.isBlank()) {
            // A music-only part (intro, solo): three dots that breathe while it plays.
            BreathingDots(color = color, active = isActive && isPlaying, size = size)
        } else if (isActive && line.words.isNotEmpty()) {
            // Karaoke: words light up as they are sung (only when the lyrics have word timings).
            val pos = live.value - offsetMs
            val annotated = buildAnnotatedString {
                line.words.forEach { word ->
                    val sung = word.startMs <= pos
                    withStyle(SpanStyle(color = color.copy(alpha = if (sung) 1f else 0.4f))) {
                        append(word.text)
                    }
                }
            }
            Column {
                Text(text = annotated, style = style, modifier = Modifier.fillMaxWidth())
                if (romanized != null) RomanizedLine(romanized, style)
            }
        } else {
            Column {
                Text(text = line.text, style = style, modifier = Modifier.fillMaxWidth())
                if (romanized != null) RomanizedLine(romanized, style)
            }
        }
    }
}

/** A gap longer than this before the first line gets a "music only" row. */
private const val IntroGapMs = 4_000L

/** The line again in English letters, smaller and quieter, under the original. */
@Composable
private fun RomanizedLine(text: String, style: TextStyle) {
    Text(
        text = text,
        style = style.copy(
            fontSize = style.fontSize * 0.56f,
            fontWeight = FontWeight.SemiBold,
            color = style.color.copy(alpha = 0.72f),
            shadow = null
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    )
}

/** Three dots for a music-only part; they swell one after another while the music plays. */
@Composable
private fun BreathingDots(color: Color, active: Boolean, size: Int) {
    val dot = (size * 0.36f).dp
    val transition = rememberInfiniteTransition(label = "lyricDots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_800, easing = LinearEasing)),
        label = "lyricDotsPhase"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(dot * 0.7f),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height((size * 1.22f).dp)
            .testTag("lyrics_music_dots")
    ) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(dot)
                    .graphicsLayer {
                        // Each dot peaks a third of a cycle after the one before.
                        val wave = if (active) {
                            (kotlin.math.sin((phase - i / 3f) * 2f * Math.PI.toFloat()) + 1f) / 2f
                        } else 0.4f
                        scaleX = 0.7f + 0.3f * wave
                        scaleY = 0.7f + 0.3f * wave
                        alpha = 0.45f + 0.55f * wave
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Android's own transliteration (ICU, offline): any script to plain English letters. */
private val englishLetters: android.icu.text.Transliterator? by lazy {
    runCatching { android.icu.text.Transliterator.getInstance("Any-Latin; Latin-ASCII") }.getOrNull()
}

/** The line in English letters, or null when it already is (or can't be converted). */
internal fun toEnglishLetters(text: String): String? {
    val needs = text.any { ch ->
        ch.isLetter() && Character.UnicodeScript.of(ch.code) != Character.UnicodeScript.LATIN
    }
    if (!needs) return null
    return englishLetters?.transliterate(text)?.takeIf { it.isNotBlank() && it != text }
}

/** Lyrics without timing: just clean, readable text. */
@Composable
private fun PlainLyrics(
    lyrics: Lyrics,
    settings: LyricsSettings,
    color: Color,
    horizontalPadding: Int,
    modifier: Modifier
) {
    val align = if (settings.alignCenter) TextAlign.Center else TextAlign.Start
    LazyColumn(
        contentPadding = PaddingValues(horizontal = (horizontalPadding + 8).dp, vertical = 32.dp),
        modifier = modifier.testTag("lyrics_plain")
    ) {
        itemsIndexed(lyrics.lines) { _, line ->
            val romanized = if (settings.romanize) remember(line.text) { toEnglishLetters(line.text) } else null
            Text(
                text = if (romanized != null) "${line.text}\n$romanized" else line.text,
                style = TextStyle(
                    fontSize = settings.textSize.plainSp.sp,
                    lineHeight = 1.35.em,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = align,
                    color = color.copy(alpha = 0.9f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (line.text.isBlank()) 8.dp else 3.dp)
            )
        }
    }
}
