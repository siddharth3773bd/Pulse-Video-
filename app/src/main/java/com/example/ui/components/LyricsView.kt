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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.example.lyrics.LyricsAlign
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.exp
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.gestures.scrollBy
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
 * Lyrics with "depth of field": the current line is full size and bright; the lines next to it
 * stay sharp but dimmer; lines further away blur and fade. When the song moves on, the lines glide
 * up one after another (each a moment after the one above it), like a soft ripple, keeping the
 * current line about a third of the way down.
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
    horizontalPadding: Int = 24
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
    val waves = remember { LyricWaves() }

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
            val delta = info.offset.toFloat()
            if (delta == 0f) return@LaunchedEffect
            // The list moves at once; each line then glides from where it was to its new place.
            val now = withFrameNanos { it }
            val moved = listState.scrollBy(delta)
            if (moved == 0f) return@LaunchedEffect
            waves.start(moved, target, now)
            if (!waves.running) {
                waves.running = true
                scope.launch {
                    try {
                        while (waves.isActive()) withFrameNanos { waves.tick(it) }
                    } finally {
                        waves.running = false
                    }
                }
            }
        } else {
            listState.animateScrollToItem(target)
        }
    }

    val align = settings.align.textAlign()
    val origin = TransformOrigin(settings.align.originX(), 0.5f)

    BoxWithConstraints(modifier = modifier.testTag("lyrics_view")) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = horizontalPadding.dp,
                end = horizontalPadding.dp,
                top = maxHeight * 0.3f,
                bottom = maxHeight * 0.6f
            ),
            modifier = Modifier
                .fillMaxSize()
                // Lines dissolve softly at the top and bottom edges instead of being cut off.
                .softEdges()
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.timeMs}#$index" }) { index, line ->
                val isActive = index == currentIndex
                val distance = if (currentIndex < 0) index + 1 else abs(index - currentIndex)
                SyncedLine(
                    line = line,
                    nextTimeMs = lines.getOrNull(index + 1)?.timeMs,
                    isActive = isActive,
                    distance = distance,
                    live = live,
                    offsetMs = offsetMs,
                    settings = settings,
                    align = align,
                    origin = origin,
                    color = contentColor,
                    isPlaying = isPlaying,
                    glide = { waves.offset(index) },
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
                .padding(bottom = 16.dp)
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
    nextTimeMs: Long?,
    isActive: Boolean,
    distance: Int,
    live: State<Long>,
    offsetMs: Long,
    settings: LyricsSettings,
    align: TextAlign,
    origin: TransformOrigin,
    color: Color,
    isPlaying: Boolean,
    /** How far the line still is from its place while the lines glide (read while drawing). */
    glide: () -> Float,
    onClick: () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.55f
            distance == 2 -> 0.42f
            else -> 0.32f
        },
        animationSpec = motion.slowEffectsSpec(),
        label = "lyricAlpha"
    )
    // The line being sung grows in with a soft spring (Material 3 Expressive spatial motion).
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        animationSpec = motion.slowSpatialSpec(),
        label = "lyricScale"
    )
    // Near lines stay sharp; blur starts three lines away and grows gently (depth of field).
    val blurTarget = if (!settings.blur || isActive) 0f else when (distance) {
        0, 1, 2 -> 0f
        3 -> 1.5f
        4 -> 3f
        else -> 4.5f
    }
    val blur by animateFloatAsState(
        targetValue = blurTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "lyricBlur"
    )

    val size = settings.textSize.activeSp
    val style = TextStyle(
        fontFamily = MaterialTheme.typography.displaySmall.fontFamily,
        fontSize = size.sp,
        lineHeight = 1.22.em,
        // One weight for every line, so a line doesn't re-wrap when it becomes the current one.
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.01).em,
        textAlign = align,
        color = color
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
                translationY = glide()
            }
            .then(if (blur > 0.05f) Modifier.blur(blur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        if (line.text.isBlank()) {
            // A music-only part (intro, solo): three dots that breathe while it plays.
            BreathingDots(color = color, active = isActive && isPlaying, size = size)
        } else if (isActive) {
            Column {
                SungLine(line = line, nextTimeMs = nextTimeMs, live = live, offsetMs = offsetMs, style = style)
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

/**
 * The line being sung: the colour sweeps through it as it is sung, karaoke style, with a soft
 * edge. With word timings (enhanced lyrics) it follows each word; otherwise it moves evenly
 * through the line in the time until the next one. The sweep is drawn, not recomposed, so it
 * stays smooth.
 */
@Composable
private fun SungLine(
    line: LyricLine,
    nextTimeMs: Long?,
    live: State<Long>,
    offsetMs: Long,
    style: TextStyle
) {
    val text = line.text
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val feather = with(LocalDensity.current) { 28.dp.toPx() }
    // When the line has finished being sung (an estimate when there are no word timings).
    val endMs = remember(line, nextTimeMs) {
        val byLength = line.timeMs + (text.length * 140L).coerceIn(1_500L, 9_000L)
        val byNext = nextTimeMs?.let { it - 150L } ?: byLength
        minOf(byNext, byLength).coerceAtLeast(line.timeMs + 400L)
    }
    val wordsMatch = remember(line) { line.words.isNotEmpty() && line.words.joinToString("") { it.text } == text }

    fun sungChars(): Float {
        val pos = live.value - offsetMs
        if (wordsMatch) {
            var chars = 0f
            for (word in line.words) {
                when {
                    pos >= word.endMs -> chars += word.text.length
                    pos > word.startMs -> {
                        val span = (word.endMs - word.startMs).coerceAtLeast(1L)
                        chars += word.text.length * ((pos - word.startMs).toFloat() / span)
                        return chars
                    }
                    else -> return chars
                }
            }
            return chars
        }
        val fraction = ((pos - line.timeMs).toFloat() / (endMs - line.timeMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
        return fraction * text.length
    }

    Box {
        // Not yet sung: quieter.
        Text(
            text = text,
            style = style.copy(color = style.color.copy(alpha = 0.42f)),
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth()
        )
        // Sung so far: bright, with a soft glow, revealed up to the singing point.
        Text(
            text = text,
            style = style.copy(shadow = Shadow(color = style.color.copy(alpha = 0.35f), blurRadius = 28f)),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    val l = layout ?: return@drawWithContent
                    val upTo = sungChars()
                    if (upTo <= 0f) return@drawWithContent
                    if (upTo >= text.length) {
                        drawContent()
                        return@drawWithContent
                    }
                    val reveal = sweepReveal(l, upTo, feather) ?: run {
                        drawContent()
                        return@drawWithContent
                    }
                    clipPath(reveal.path) { this@drawWithContent.drawContent() }
                    // Soft edge where the singing is.
                    val brush = if (reveal.rtl) {
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Black),
                            startX = reveal.x - feather,
                            endX = reveal.x + feather
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startX = reveal.x - feather,
                            endX = reveal.x + feather
                        )
                    }
                    drawRect(
                        brush = brush,
                        topLeft = Offset(reveal.x - feather, reveal.top),
                        size = Size(feather * 2f, reveal.bottom - reveal.top),
                        blendMode = BlendMode.DstIn
                    )
                }
        )
    }
}

/** Where the sweep is: the sung area (whole lines, plus the current line up to just past [x]). */
private class SweepReveal(val path: Path, val x: Float, val top: Float, val bottom: Float, val rtl: Boolean)

private fun sweepReveal(layout: TextLayoutResult, upTo: Float, feather: Float): SweepReveal? {
    val length = layout.layoutInput.text.length
    val index = upTo.toInt().coerceIn(0, length)
    val partOfChar = upTo - index
    val path = Path()
    for (lineIndex in 0 until layout.lineCount) {
        val start = layout.getLineStart(lineIndex)
        val end = layout.getLineEnd(lineIndex, visibleEnd = true)
        val top = layout.getLineTop(lineIndex)
        val bottom = layout.getLineBottom(lineIndex)
        val left = layout.getLineLeft(lineIndex)
        val right = layout.getLineRight(lineIndex)
        if (index >= end) {
            path.addRect(Rect(left, top, right, bottom))
            continue
        }
        if (index < start) return null
        val rtl = layout.getBidiRunDirection(index.coerceAtMost(length - 1)) == ResolvedTextDirection.Rtl
        val x0 = layout.getHorizontalPosition(index, usePrimaryDirection = true)
        val x1 = if (index + 1 <= end) layout.getHorizontalPosition(index + 1, usePrimaryDirection = true) else x0
        val x = x0 + (x1 - x0) * partOfChar
        if (rtl) {
            path.addRect(Rect((x - feather).coerceAtLeast(left), top, right, bottom))
        } else {
            path.addRect(Rect(left, top, (x + feather).coerceAtMost(right), bottom))
        }
        return SweepReveal(path, x, top, bottom, rtl)
    }
    return null
}

/** Fades content out at its top and bottom edges. */
private fun Modifier.softEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.08f to Color.Black,
                0.86f to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

private fun LyricsAlign.textAlign(): TextAlign = when (this) {
    LyricsAlign.LEFT -> TextAlign.Start
    LyricsAlign.CENTER -> TextAlign.Center
    LyricsAlign.RIGHT -> TextAlign.End
}

private fun LyricsAlign.originX(): Float = when (this) {
    LyricsAlign.LEFT -> 0f
    LyricsAlign.CENTER -> 0.5f
    LyricsAlign.RIGHT -> 1f
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
    // Only the dots being played move (still dots don't keep the screen redrawing).
    val phase = if (active) {
        rememberInfiniteTransition(label = "lyricDots").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1_800, easing = LinearEasing)),
            label = "lyricDotsPhase"
        )
    } else null
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
                        val wave = if (phase != null) {
                            (kotlin.math.sin((phase.value - i / 3f) * 2f * Math.PI.toFloat()) + 1f) / 2f
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
    val align = settings.align.textAlign()
    val fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
    LazyColumn(
        contentPadding = PaddingValues(horizontal = (horizontalPadding + 8).dp, vertical = 32.dp),
        modifier = modifier
            .softEdges()
            .testTag("lyrics_plain")
    ) {
        itemsIndexed(lyrics.lines) { _, line ->
            val romanized = if (settings.romanize) remember(line.text) { toEnglishLetters(line.text) } else null
            Text(
                text = if (romanized != null) "${line.text}\n$romanized" else line.text,
                style = TextStyle(
                    fontFamily = fontFamily,
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

/**
 * The ripple when the lyrics move on to the next line. The list itself jumps to its new place at
 * once; each line is then drawn shifted back to where it was and glides into place with a soft,
 * no-bounce spring. Lines further down start a little later, so they follow one after another.
 * Several ripples can overlap (fast songs); their shifts add up, so nothing ever jumps.
 */
private class LyricWaves {
    private class Wave(val delta: Float, val anchor: Int, val startNanos: Long)

    private val waves = ArrayList<Wave>()
    /** The frame time; drawing reads it, so lines redraw (only redraw) each frame of a ripple. */
    private val clock = mutableLongStateOf(0L)
    var running = false

    fun start(delta: Float, anchor: Int, nowNanos: Long) {
        waves.add(Wave(delta, anchor, nowNanos))
        clock.longValue = nowNanos
    }

    fun isActive(): Boolean = waves.isNotEmpty()

    fun tick(nowNanos: Long) {
        clock.longValue = nowNanos
        waves.removeAll { (nowNanos - it.startNanos) / 1_000_000L > WaveLifeMs }
    }

    fun offset(index: Int): Float {
        val now = clock.longValue
        if (waves.isEmpty()) return 0f
        var sum = 0f
        for (wave in waves) {
            val lag = (index - wave.anchor + 1).coerceIn(0, WaveMaxSteps) * WaveStepMs
            val t = (now - wave.startNanos) / 1_000_000f - lag
            sum += if (t <= 0f) wave.delta else wave.delta * (1f - settle(t))
        }
        return sum
    }

    /** A critically damped spring (no bounce) from 0 to 1. */
    private fun settle(ms: Float): Float {
        val x = ms / 1000f * WaveSpeed
        return (1f - (1f + x) * exp(-x)).coerceIn(0f, 1f)
    }

    private companion object {
        const val WaveStepMs = 45f
        const val WaveMaxSteps = 6
        const val WaveSpeed = 11f
        const val WaveLifeMs = 1_200L
    }
}
