package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

/**
 * A preset accent for when "Dynamic colour" is off. Only its seed matters: the app's colours are
 * made from it by Google's Material You method (Tonal Spot), the same way wallpaper colours are.
 */
@Immutable
data class AppColorPreset(val id: String, val label: String, val seed: Color)

/** The presets on the Appearance page (plus "Custom"). */
val AppColorPresets: List<AppColorPreset> = listOf(
    AppColorPreset(id = "indigo", label = "Indigo", seed = Color(0xFFA8C7FA)),
    AppColorPreset(id = "amber", label = "Amber", seed = Color(0xFFFFB951)),
    AppColorPreset(id = "slate", label = "Slate", seed = Color(0xFFB5C8DC)),
    AppColorPreset(id = "forest", label = "Forest", seed = Color(0xFF8FD694)),
    AppColorPreset(id = "ocean", label = "Ocean", seed = Color(0xFF9BD5EE))
)

/** Id of the "Custom" choice; its seed is the colour picked in the custom colour dialog. */
const val CustomColorId = "custom"

/** The seed colour for a preset id (unknown ids fall back to the first preset). */
fun seedFor(presetId: String, customColorArgb: Int): Int =
    if (presetId == CustomColorId) {
        customColorArgb
    } else {
        (AppColorPresets.firstOrNull { it.id == presetId } ?: AppColorPresets.first()).seed.toArgb()
    }

fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format(Locale.US, "#%06X", 0xFFFFFF and argb)
}

fun parseHexColorOrNull(hexInput: String): Color? {
    val cleaned = hexInput.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    return try {
        val rgb = cleaned.toInt(16)
        Color(0xFF000000.toInt() or rgb)
    } catch (_: Exception) {
        null
    }
}
