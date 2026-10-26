package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
    // is a real Material colour.
    val seed = when (paletteId) {
        "custom" -> customColorArgb
        else -> (M3PalettesById[paletteId] ?: IndigoDarkPalette).primary.toArgb()
    }
    val rawScheme: ColorScheme = if (dynamicColor) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        SchemeTonalSpot(Hct.fromInt(seed), isDark, 0.0).toComposeColorScheme(isDark)
    }
    // The dark version of the same colours, for screens that are always dark (Now Playing on a
    // gradient, blurred cover or black background) even when the app itself is light.
    val darkScheme: ColorScheme = when {
        isDark -> rawScheme
        dynamicColor -> dynamicDarkColorScheme(context)
        else -> SchemeTonalSpot(Hct.fromInt(seed), true, 0.0).toComposeColorScheme(true)
    }
    val baseScheme = if (applyAmoled) {
        rawScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black
        )
    } else rawScheme
    CompositionLocalProvider(LocalDarkColorScheme provides darkScheme) {
        MaterialExpressiveTheme(
            colorScheme = baseScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = Shapes(),
            typography = Typography,
            content = content
        )
    }
}

/** The app's colours in their dark version (wallpaper or chosen theme), whatever the app's mode. */
val LocalDarkColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

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
