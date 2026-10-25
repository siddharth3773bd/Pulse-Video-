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

    val baseScheme: ColorScheme
    val basePalette: M3Palette

    if (dynamicColor) {
        val dynScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        baseScheme = if (applyAmoled) {
            dynScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceContainerLowest = Color.Black
            )
        } else {
            dynScheme
        }
        basePalette = M3Palette(
            id = "dynamic",
            name = "Dynamic",
            primary = baseScheme.primary,
            onPrimary = baseScheme.onPrimary,
            primaryContainer = baseScheme.primaryContainer,
            onPrimaryContainer = baseScheme.onPrimaryContainer,
            secondaryContainer = baseScheme.secondaryContainer,
            onSecondaryContainer = baseScheme.onSecondaryContainer,
            surfaceDim = if (applyAmoled) Color.Black else baseScheme.surfaceDim,
            surface = if (applyAmoled) Color.Black else baseScheme.surface,
            surfaceContainerLowest = if (applyAmoled) Color.Black else baseScheme.surfaceContainerLowest,
            surfaceContainerLow = if (applyAmoled) Color.Black else baseScheme.surfaceContainerLow,
            surfaceContainer = baseScheme.surfaceContainer,
            surfaceContainerHigh = baseScheme.surfaceContainerHigh,
            surfaceContainerHighest = baseScheme.surfaceContainerHighest,
            onSurface = baseScheme.onSurface,
            onSurfaceVariant = baseScheme.onSurfaceVariant,
            outline = baseScheme.outline,
            outlineVariant = baseScheme.outlineVariant,
            error = baseScheme.error,
            errorContainer = baseScheme.errorContainer,
            onErrorContainer = baseScheme.onErrorContainer
        )
    } else {
        val rawPalette = when {
            paletteId == "custom" -> generateM3PaletteFromSeed(Color(customColorArgb), isDark = isDark)
            isDark -> M3PalettesById[paletteId] ?: IndigoDarkPalette
            else -> M3LightPalettesById[paletteId] ?: IndigoLightPalette
        }
        basePalette = if (applyAmoled) {
            rawPalette.copy(
                surfaceDim = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color.Black
            )
        } else {
            rawPalette
        }
        baseScheme = if (isDark) {
            darkColorScheme(
                primary = basePalette.primary,
                onPrimary = basePalette.onPrimary,
                primaryContainer = basePalette.primaryContainer,
                onPrimaryContainer = basePalette.onPrimaryContainer,
                secondary = basePalette.primary,
                onSecondary = basePalette.onPrimary,
                secondaryContainer = basePalette.secondaryContainer,
                onSecondaryContainer = basePalette.onSecondaryContainer,
                background = if (applyAmoled) Color.Black else basePalette.surfaceDim,
                onBackground = basePalette.onSurface,
                surface = if (applyAmoled) Color.Black else basePalette.surface,
                onSurface = basePalette.onSurface,
                surfaceVariant = basePalette.surfaceContainerLow,
                onSurfaceVariant = basePalette.onSurfaceVariant,
                surfaceDim = basePalette.surfaceDim,
                surfaceContainerLowest = basePalette.surfaceContainerLowest,
                surfaceContainerLow = basePalette.surfaceContainerLow,
                surfaceContainer = basePalette.surfaceContainer,
                surfaceContainerHigh = basePalette.surfaceContainerHigh,
                surfaceContainerHighest = basePalette.surfaceContainerHighest,
                outline = basePalette.outline,
                outlineVariant = basePalette.outlineVariant,
                error = basePalette.error,
                errorContainer = basePalette.errorContainer,
                onErrorContainer = basePalette.onErrorContainer
            )
        } else {
            lightColorScheme(
                primary = basePalette.primary,
                onPrimary = basePalette.onPrimary,
                primaryContainer = basePalette.primaryContainer,
                onPrimaryContainer = basePalette.onPrimaryContainer,
                secondary = basePalette.primary,
                onSecondary = basePalette.onPrimary,
                secondaryContainer = basePalette.secondaryContainer,
                onSecondaryContainer = basePalette.onSecondaryContainer,
                background = basePalette.surface,
                onBackground = basePalette.onSurface,
                surface = basePalette.surface,
                onSurface = basePalette.onSurface,
                surfaceVariant = basePalette.surfaceContainerLow,
                onSurfaceVariant = basePalette.onSurfaceVariant,
                surfaceDim = basePalette.surfaceDim,
                surfaceContainerLowest = basePalette.surfaceContainerLowest,
                surfaceContainerLow = basePalette.surfaceContainerLow,
                surfaceContainer = basePalette.surfaceContainer,
                surfaceContainerHigh = basePalette.surfaceContainerHigh,
                surfaceContainerHighest = basePalette.surfaceContainerHighest,
                outline = basePalette.outline,
                outlineVariant = basePalette.outlineVariant,
                error = basePalette.error,
                errorContainer = basePalette.errorContainer,
                onErrorContainer = basePalette.onErrorContainer
            )
        }
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
