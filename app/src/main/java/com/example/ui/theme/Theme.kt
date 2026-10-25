package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.example.model.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DopplerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    paletteId: String = "indigo",
    customColorArgb: Int = 0xFF6366F1.toInt(),
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val applyAmoled = isDark && amoledBlack

    // One colour system: everything comes from a Material 3 ColorScheme. With "Dynamic colour" it
    // is the wallpaper scheme; otherwise the chosen preset (or custom colour) is the seed for
    // Google's own Material You algorithm (SchemeTonalSpot), so every role, tertiary included,
    // is a real Material colour. LocalM3Palette is just a view of that same scheme.
    val rawScheme: ColorScheme = if (dynamicColor) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val seed = when (paletteId) {
            "custom" -> customColorArgb
            else -> (M3PalettesById[paletteId] ?: IndigoDarkPalette).primary.toArgb()
        }
        SchemeTonalSpot(Hct.fromInt(seed), isDark, 0.0).toComposeColorScheme(isDark)
    }
    val baseScheme = if (applyAmoled) {
        rawScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black
        )
    } else rawScheme
    val template = if (dynamicColor) IndigoDarkPalette.copy(id = "dynamic", name = "Dynamic")
    else (M3PalettesById[paletteId] ?: IndigoDarkPalette)
    val basePalette = baseScheme.toM3Palette(template).let {
        if (applyAmoled) it.copy(surfaceContainerLow = Color.Black) else it
    }

    CompositionLocalProvider(LocalM3Palette provides basePalette) {
        MaterialExpressiveTheme(
            colorScheme = baseScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = Shapes(),
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    DopplerTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
