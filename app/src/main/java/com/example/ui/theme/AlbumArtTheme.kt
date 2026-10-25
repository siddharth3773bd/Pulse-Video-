@file:SuppressLint("RestrictedApi")
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.model.AudioTrackItem
import com.example.ui.components.loadAudioArtBitmap
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Colours extracted from one song's album art. */
data class AlbumArtColors(
    val light: ColorScheme,
    val dark: ColorScheme,
    /** 2–3 colours for the Gradient background, darkened so white text stays readable. */
    val gradient: List<Color>
)

/** Gradient colours of the current song for the Now Playing background (empty when none). */
val LocalAlbumArtGradient = compositionLocalOf<List<Color>> { emptyList() }

private object AlbumArtColorCache {
    private val cache = LruCache<String, AlbumArtColors>(48)
    private val missing = HashSet<String>()
    fun get(key: String): AlbumArtColors? = cache.get(key)
    fun put(key: String, value: AlbumArtColors) { cache.put(key, value) }
    fun isMissing(key: String) = synchronized(missing) { key in missing }
    fun markMissing(key: String) = synchronized(missing) { missing.add(key) }
}

/** Album-art colours for [track] (cached); null when the song has no cover. */
suspend fun albumArtColorsFor(context: Context, track: AudioTrackItem): AlbumArtColors? {
    val key = track.id
    AlbumArtColorCache.get(key)?.let { return it }
    if (AlbumArtColorCache.isMissing(key)) return null
    val bitmap = loadAudioArtBitmap(context, track.contentUri)
    val colors = bitmap?.let { withContext(Dispatchers.Default) { runCatching { extractAlbumArtColors(it) }.getOrNull() } }
    if (colors != null) AlbumArtColorCache.put(key, colors) else AlbumArtColorCache.markMissing(key)
    return colors
}

/**
 * Same pipeline Android uses for wallpaper colours: quantize the image (Celebi), score the
 * candidates to pick a seed, then build a Tonal Spot Material 3 scheme from that seed.
 */
private fun extractAlbumArtColors(source: Bitmap): AlbumArtColors {
    val software = if (source.config == Bitmap.Config.HARDWARE) {
        source.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        source
    }
    val width = 128
    val height = (128f * software.height / software.width.coerceAtLeast(1)).toInt().coerceIn(1, 256)
    val small = Bitmap.createScaledBitmap(software, width, height, true)
    val pixels = IntArray(width * height)
    small.getPixels(pixels, 0, width, 0, 0, width, height)

    val quantized = QuantizerCelebi.quantize(pixels, 128)
    val seed = Score.score(quantized).firstOrNull() ?: 0xFF4285F4.toInt()
    val hct = Hct.fromInt(seed)

    val light = SchemeTonalSpot(hct, false, 0.0).toComposeColorScheme(isDark = false)
    val dark = SchemeTonalSpot(hct, true, 0.0).toComposeColorScheme(isDark = true)

    return AlbumArtColors(light = light, dark = dark, gradient = gradientColors(small, seed))
}

/** Echo-style gradient: the strongest swatches of the cover, darkened for white text. */
private fun gradientColors(bitmap: Bitmap, seed: Int): List<Color> {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val candidates = listOfNotNull(
        palette.vibrantSwatch,
        palette.dominantSwatch,
        palette.darkVibrantSwatch,
        palette.mutedSwatch,
        palette.darkMutedSwatch
    ).distinctBy { it.rgb }.sortedByDescending { it.population }.map { it.rgb }
    val picked = (candidates.ifEmpty { listOf(seed) }).take(3)
    fun darken(argb: Int, maxLightness: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[2] = hsl[2].coerceAtMost(maxLightness)
        return Color(ColorUtils.HSLToColor(hsl))
    }
    return when (picked.size) {
        1 -> listOf(darken(picked[0], 0.42f), darken(picked[0], 0.22f), Color.Black)
        2 -> listOf(darken(picked[0], 0.42f), darken(picked[1], 0.28f), Color.Black)
        else -> listOf(darken(picked[0], 0.42f), darken(picked[1], 0.30f), darken(picked[2], 0.14f))
    }
}

internal fun DynamicScheme.toComposeColorScheme(isDark: Boolean): ColorScheme {
    val m = MaterialDynamicColors()
    fun c(color: DynamicColor) = Color(color.getArgb(this))
    return if (isDark) {
        darkColorScheme(
            primary = c(m.primary()), onPrimary = c(m.onPrimary()),
            primaryContainer = c(m.primaryContainer()), onPrimaryContainer = c(m.onPrimaryContainer()),
            inversePrimary = c(m.inversePrimary()),
            secondary = c(m.secondary()), onSecondary = c(m.onSecondary()),
            secondaryContainer = c(m.secondaryContainer()), onSecondaryContainer = c(m.onSecondaryContainer()),
            tertiary = c(m.tertiary()), onTertiary = c(m.onTertiary()),
            tertiaryContainer = c(m.tertiaryContainer()), onTertiaryContainer = c(m.onTertiaryContainer()),
            background = c(m.background()), onBackground = c(m.onBackground()),
            surface = c(m.surface()), onSurface = c(m.onSurface()),
            surfaceVariant = c(m.surfaceVariant()), onSurfaceVariant = c(m.onSurfaceVariant()),
            surfaceTint = c(m.surfaceTint()),
            inverseSurface = c(m.inverseSurface()), inverseOnSurface = c(m.inverseOnSurface()),
            error = c(m.error()), onError = c(m.onError()),
            errorContainer = c(m.errorContainer()), onErrorContainer = c(m.onErrorContainer()),
            outline = c(m.outline()), outlineVariant = c(m.outlineVariant()), scrim = c(m.scrim()),
            surfaceBright = c(m.surfaceBright()), surfaceDim = c(m.surfaceDim()),
            surfaceContainer = c(m.surfaceContainer()), surfaceContainerHigh = c(m.surfaceContainerHigh()),
            surfaceContainerHighest = c(m.surfaceContainerHighest()), surfaceContainerLow = c(m.surfaceContainerLow()),
            surfaceContainerLowest = c(m.surfaceContainerLowest())
        )
    } else {
        lightColorScheme(
            primary = c(m.primary()), onPrimary = c(m.onPrimary()),
            primaryContainer = c(m.primaryContainer()), onPrimaryContainer = c(m.onPrimaryContainer()),
            inversePrimary = c(m.inversePrimary()),
            secondary = c(m.secondary()), onSecondary = c(m.onSecondary()),
            secondaryContainer = c(m.secondaryContainer()), onSecondaryContainer = c(m.onSecondaryContainer()),
            tertiary = c(m.tertiary()), onTertiary = c(m.onTertiary()),
            tertiaryContainer = c(m.tertiaryContainer()), onTertiaryContainer = c(m.onTertiaryContainer()),
            background = c(m.background()), onBackground = c(m.onBackground()),
            surface = c(m.surface()), onSurface = c(m.onSurface()),
            surfaceVariant = c(m.surfaceVariant()), onSurfaceVariant = c(m.onSurfaceVariant()),
            surfaceTint = c(m.surfaceTint()),
            inverseSurface = c(m.inverseSurface()), inverseOnSurface = c(m.inverseOnSurface()),
            error = c(m.error()), onError = c(m.onError()),
            errorContainer = c(m.errorContainer()), onErrorContainer = c(m.onErrorContainer()),
            outline = c(m.outline()), outlineVariant = c(m.outlineVariant()), scrim = c(m.scrim()),
            surfaceBright = c(m.surfaceBright()), surfaceDim = c(m.surfaceDim()),
            surfaceContainer = c(m.surfaceContainer()), surfaceContainerHigh = c(m.surfaceContainerHigh()),
            surfaceContainerHighest = c(m.surfaceContainerHighest()), surfaceContainerLow = c(m.surfaceContainerLow()),
            surfaceContainerLowest = c(m.surfaceContainerLowest())
        )
    }
}

/** Smoothly fades every role when the song (and so the scheme) changes. */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 450)
    @Composable
    fun a(color: Color) = animateColorAsState(color, spec, label = "albumArtColor").value
    return target.copy(
        primary = a(target.primary), onPrimary = a(target.onPrimary),
        primaryContainer = a(target.primaryContainer), onPrimaryContainer = a(target.onPrimaryContainer),
        secondary = a(target.secondary), onSecondary = a(target.onSecondary),
        secondaryContainer = a(target.secondaryContainer), onSecondaryContainer = a(target.onSecondaryContainer),
        tertiary = a(target.tertiary), onTertiary = a(target.onTertiary),
        tertiaryContainer = a(target.tertiaryContainer), onTertiaryContainer = a(target.onTertiaryContainer),
        surface = a(target.surface), onSurface = a(target.onSurface),
        onSurfaceVariant = a(target.onSurfaceVariant),
        surfaceContainerLowest = a(target.surfaceContainerLowest), surfaceContainerLow = a(target.surfaceContainerLow),
        surfaceContainer = a(target.surfaceContainer), surfaceContainerHigh = a(target.surfaceContainerHigh),
        surfaceContainerHighest = a(target.surfaceContainerHighest),
        outline = a(target.outline), outlineVariant = a(target.outlineVariant)
    )
}


/**
 * Themes its [content] (the Now Playing screen and the mini player) from [track]'s album art.
 *
 * @param applyScheme when true the whole Material 3 scheme comes from the cover (Pixel Player
 *   style); when false the app's wallpaper / theme colours stay.
 * @param extractColors also run extraction when only the gradient colours are needed.
 */
@Composable
fun AlbumArtTheme(
    track: AudioTrackItem?,
    applyScheme: Boolean,
    extractColors: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val base = MaterialTheme.colorScheme
    val isDark = base.surface.luminance() < 0.5f
    val needColors = applyScheme || extractColors

    // Keep the previous song's colours until the next ones are ready, so nothing flashes.
    var colors by remember { mutableStateOf<AlbumArtColors?>(null) }
    LaunchedEffect(track?.id, needColors) {
        colors = if (needColors && track != null) albumArtColorsFor(context, track) else null
    }

    val albumScheme = colors?.let { if (isDark) it.dark else it.light }
    val useAlbum = applyScheme && albumScheme != null
    val scheme = animateColorScheme(if (useAlbum) albumScheme!! else base)
    CompositionLocalProvider(LocalAlbumArtGradient provides colors?.gradient.orEmpty()) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}
