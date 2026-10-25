package com.example.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

@Immutable
data class M3Palette(
    val id: String,
    val name: String,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surfaceDim: Color,
    val surface: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val DeleteButtonTextColor: Color = Color(0xFF490002),
    val DragHandleColor: Color = Color(0x808E9099),
    val PureBlack: Color = Color(0xFF000000)
)

@Immutable
data class AppearancePresetSwatch(
    val id: String,
    val label: String,
    val swatchColor: Color
)

// ==================== DARK PALETTES ====================

val IndigoDarkPalette = M3Palette(
    id = "indigo",
    name = "Indigo",
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondaryContainer = Color(0xFF384959),
    onSecondaryContainer = Color(0xFFD3E4F4),
    surfaceDim = Color(0xFF111318),
    surface = Color(0xFF13161B),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF1B1E24),
    surfaceContainer = Color(0xFF1F2329),
    surfaceContainerHigh = Color(0xFF292D34),
    surfaceContainerHighest = Color(0xFF343840),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val AmberDarkPalette = M3Palette(
    id = "amber",
    name = "Amber",
    primary = Color(0xFFFFB951),
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFF633F00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondaryContainer = Color(0xFF534432),
    onSecondaryContainer = Color(0xFFFBDEBC),
    surfaceDim = Color(0xFF15120D),
    surface = Color(0xFF181410),
    surfaceContainerLowest = Color(0xFF100D09),
    surfaceContainerLow = Color(0xFF201B16),
    surfaceContainer = Color(0xFF241F1A),
    surfaceContainerHigh = Color(0xFF2F2924),
    surfaceContainerHighest = Color(0xFF3A342E),
    onSurface = Color(0xFFECE0D9),
    onSurfaceVariant = Color(0xFFD3C4B4),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val SlateDarkPalette = M3Palette(
    id = "slate",
    name = "Slate",
    primary = Color(0xFFB5C8DC),
    onPrimary = Color(0xFF203242),
    primaryContainer = Color(0xFF364859),
    onPrimaryContainer = Color(0xFFD1E4F8),
    secondaryContainer = Color(0xFF3C4854),
    onSecondaryContainer = Color(0xFFD6E4F2),
    surfaceDim = Color(0xFF101418),
    surface = Color(0xFF12161A),
    surfaceContainerLowest = Color(0xFF0B0E11),
    surfaceContainerLow = Color(0xFF1A1E22),
    surfaceContainer = Color(0xFF1E2227),
    surfaceContainerHigh = Color(0xFF282D32),
    surfaceContainerHighest = Color(0xFF33383D),
    onSurface = Color(0xFFE1E2E5),
    onSurfaceVariant = Color(0xFFC2C7CE),
    outline = Color(0xFF8C9198),
    outlineVariant = Color(0xFF42474D),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val ForestDarkPalette = M3Palette(
    id = "forest",
    name = "Forest Green",
    primary = Color(0xFF8FD694),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF0B531B),
    onPrimaryContainer = Color(0xFFAAF2AE),
    secondaryContainer = Color(0xFF354B35),
    onSecondaryContainer = Color(0xFFCFE9CE),
    surfaceDim = Color(0xFF0E140E),
    surface = Color(0xFF101610),
    surfaceContainerLowest = Color(0xFF0A0F0A),
    surfaceContainerLow = Color(0xFF181E18),
    surfaceContainer = Color(0xFF1C221C),
    surfaceContainerHigh = Color(0xFF262D26),
    surfaceContainerHighest = Color(0xFF313831),
    onSurface = Color(0xFFE0E4DF),
    onSurfaceVariant = Color(0xFFC0C9BE),
    outline = Color(0xFF8A9389),
    outlineVariant = Color(0xFF40493F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val OceanDarkPalette = M3Palette(
    id = "ocean",
    name = "Ocean",
    primary = Color(0xFF9BD5EE),
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004D64),
    onPrimaryContainer = Color(0xFFC4E7FF),
    secondaryContainer = Color(0xFF334A56),
    onSecondaryContainer = Color(0xFFCFE6F2),
    surfaceDim = Color(0xFF0E1215),
    surface = Color(0xFF111417),
    surfaceContainerLowest = Color(0xFF0A0D0F),
    surfaceContainerLow = Color(0xFF191C1F),
    surfaceContainer = Color(0xFF1D2124),
    surfaceContainerHigh = Color(0xFF272B2F),
    surfaceContainerHighest = Color(0xFF32363A),
    onSurface = Color(0xFFE2E2E5),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41474D),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val VioletDarkPalette = M3Palette(
    id = "violet",
    name = "Royal Violet",
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    surfaceDim = Color(0xFF121016),
    surface = Color(0xFF141218),
    surfaceContainerLowest = Color(0xFF0D0B10),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val EmeraldDarkPalette = M3Palette(
    id = "emerald",
    name = "Emerald Mint",
    primary = Color(0xFF86EFAC),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00522B),
    onPrimaryContainer = Color(0xFFA2F6BF),
    secondaryContainer = Color(0xFF314D3B),
    onSecondaryContainer = Color(0xFFCBEAD4),
    surfaceDim = Color(0xFF0D130F),
    surface = Color(0xFF101512),
    surfaceContainerLowest = Color(0xFF090E0B),
    surfaceContainerLow = Color(0xFF181D19),
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerHigh = Color(0xFF262B27),
    surfaceContainerHighest = Color(0xFF313632),
    onSurface = Color(0xFFE0E4E0),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val RoseDarkPalette = M3Palette(
    id = "rose",
    name = "Sakura Rose",
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF541D32),
    primaryContainer = Color(0xFF703348),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondaryContainer = Color(0xFF573F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    surfaceDim = Color(0xFF161113),
    surface = Color(0xFF191315),
    surfaceContainerLowest = Color(0xFF110C0E),
    surfaceContainerLow = Color(0xFF211B1D),
    surfaceContainer = Color(0xFF261F21),
    surfaceContainerHigh = Color(0xFF31292C),
    surfaceContainerHighest = Color(0xFF3C3437),
    onSurface = Color(0xFFEFDFE1),
    onSurfaceVariant = Color(0xFFD5C2C6),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val TealDarkPalette = M3Palette(
    id = "teal",
    name = "Lagoon Teal",
    primary = Color(0xFF59D5C8),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005049),
    onPrimaryContainer = Color(0xFF79F2E4),
    secondaryContainer = Color(0xFF334B49),
    onSecondaryContainer = Color(0xFFCCE8E5),
    surfaceDim = Color(0xFF0E1413),
    surface = Color(0xFF101615),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF181E1D),
    surfaceContainer = Color(0xFF1C2221),
    surfaceContainerHigh = Color(0xFF262D2C),
    surfaceContainerHighest = Color(0xFF313837),
    onSurface = Color(0xFFE0E3E1),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF889391),
    outlineVariant = Color(0xFF3F4948),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val MatchaDarkPalette = M3Palette(
    id = "matcha",
    name = "Cyber Matcha",
    primary = Color(0xFFA7D472),
    onPrimary = Color(0xFF1E3700),
    primaryContainer = Color(0xFF2F4F00),
    onPrimaryContainer = Color(0xFFC2F08B),
    secondaryContainer = Color(0xFF3F4935),
    onSecondaryContainer = Color(0xFFDFEAE0),
    surfaceDim = Color(0xFF11140E),
    surface = Color(0xFF131610),
    surfaceContainerLowest = Color(0xFF0B0E09),
    surfaceContainerLow = Color(0xFF1B1F17),
    surfaceContainer = Color(0xFF1F231B),
    surfaceContainerHigh = Color(0xFF292E25),
    surfaceContainerHighest = Color(0xFF34392F),
    onSurface = Color(0xFFE2E3DD),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9287),
    outlineVariant = Color(0xFF43483E),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val TerracottaDarkPalette = M3Palette(
    id = "terracotta",
    name = "Warm Terracotta",
    primary = Color(0xFFFFB4A4),
    onPrimary = Color(0xFF5E1608),
    primaryContainer = Color(0xFF7D2C1B),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondaryContainer = Color(0xFF5D4039),
    onSecondaryContainer = Color(0xFFFFDAD3),
    surfaceDim = Color(0xFF181210),
    surface = Color(0xFF1A1412),
    surfaceContainerLowest = Color(0xFF120C0A),
    surfaceContainerLow = Color(0xFF241D1A),
    surfaceContainer = Color(0xFF28211E),
    surfaceContainerHigh = Color(0xFF342B28),
    surfaceContainerHighest = Color(0xFF3F3632),
    onSurface = Color(0xFFEFE0DB),
    onSurfaceVariant = Color(0xFFD8C2BC),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val MonoDarkPalette = M3Palette(
    id = "mono",
    name = "Pure Monochrome",
    primary = Color(0xFFC6C6C6),
    onPrimary = Color(0xFF303030),
    primaryContainer = Color(0xFF474747),
    onPrimaryContainer = Color(0xFFE2E2E2),
    secondaryContainer = Color(0xFF474747),
    onSecondaryContainer = Color(0xFFE2E2E2),
    surfaceDim = Color(0xFF141414),
    surface = Color(0xFF171717),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1C1C1C),
    surfaceContainer = Color(0xFF212121),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    surfaceContainerHighest = Color(0xFF363636),
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFC7C7C7),
    outline = Color(0xFF919191),
    outlineVariant = Color(0xFF474747),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ==================== LIGHT PALETTES ====================

val IndigoLightPalette = M3Palette(
    id = "indigo",
    name = "Indigo",
    primary = Color(0xFF1A56C4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    surfaceDim = Color(0xFFD8DAE0),
    surface = Color(0xFFF8F9FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEDF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E9),
    onSurface = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val AmberLightPalette = M3Palette(
    id = "amber",
    name = "Amber",
    primary = Color(0xFF825500),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF291800),
    secondaryContainer = Color(0xFFFBDEBC),
    onSecondaryContainer = Color(0xFF271904),
    surfaceDim = Color(0xFFE2D8D0),
    surface = Color(0xFFFFF8F4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF2EA),
    surfaceContainer = Color(0xFFF5ECE4),
    surfaceContainerHigh = Color(0xFFEFE6DF),
    surfaceContainerHighest = Color(0xFFE9E1D9),
    onSurface = Color(0xFF201B16),
    onSurfaceVariant = Color(0xFF4F4539),
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val SlateLightPalette = M3Palette(
    id = "slate",
    name = "Slate",
    primary = Color(0xFF4C6072),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4F8),
    onPrimaryContainer = Color(0xFF071D2C),
    secondaryContainer = Color(0xFFD6E4F2),
    onSecondaryContainer = Color(0xFF111D28),
    surfaceDim = Color(0xFFD8DADE),
    surface = Color(0xFFF7F9FC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3F6),
    surfaceContainer = Color(0xFFEBEEF1),
    surfaceContainerHigh = Color(0xFFE6E8EB),
    surfaceContainerHighest = Color(0xFFE0E2E6),
    onSurface = Color(0xFF181C1F),
    onSurfaceVariant = Color(0xFF42474D),
    outline = Color(0xFF72777E),
    outlineVariant = Color(0xFFC2C7CE),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val ForestLightPalette = M3Palette(
    id = "forest",
    name = "Forest Green",
    primary = Color(0xFF256B2E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAAF2AE),
    onPrimaryContainer = Color(0xFF002106),
    secondaryContainer = Color(0xFFCFE9CE),
    onSecondaryContainer = Color(0xFF0D1F10),
    surfaceDim = Color(0xFFD6DBD4),
    surface = Color(0xFFF6FBF3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5ED),
    surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE4EAE2),
    surfaceContainerHighest = Color(0xFFDFE4DC),
    onSurface = Color(0xFF171D17),
    onSurfaceVariant = Color(0xFF40493F),
    outline = Color(0xFF70796E),
    outlineVariant = Color(0xFFC0C9BE),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val OceanLightPalette = M3Palette(
    id = "ocean",
    name = "Ocean",
    primary = Color(0xFF006684),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFE9FF),
    onPrimaryContainer = Color(0xFF001F2A),
    secondaryContainer = Color(0xFFD0E6F2),
    onSecondaryContainer = Color(0xFF081E27),
    surfaceDim = Color(0xFFD7DADF),
    surface = Color(0xFFF6FAFE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFEAEEF2),
    surfaceContainerHigh = Color(0xFFE4E9ED),
    surfaceContainerHighest = Color(0xFFDFE3E7),
    onSurface = Color(0xFF181C1F),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CD),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val VioletLightPalette = M3Palette(
    id = "violet",
    name = "Royal Violet",
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    surfaceDim = Color(0xFFDED8E1),
    surface = Color(0xFFFEF7FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val EmeraldLightPalette = M3Palette(
    id = "emerald",
    name = "Emerald Mint",
    primary = Color(0xFF006C3B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9DF6B8),
    onPrimaryContainer = Color(0xFF00210E),
    secondaryContainer = Color(0xFFCFE9D5),
    onSecondaryContainer = Color(0xFF0B1F14),
    surfaceDim = Color(0xFFD6DBD6),
    surface = Color(0xFFF5FBF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5EF),
    surfaceContainer = Color(0xFFE9EFE9),
    surfaceContainerHigh = Color(0xFFE4EAE4),
    surfaceContainerHighest = Color(0xFFDEE4DE),
    onSurface = Color(0xFF171D19),
    onSurfaceVariant = Color(0xFF404942),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFC0C9C0),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val RoseLightPalette = M3Palette(
    id = "rose",
    name = "Sakura Rose",
    primary = Color(0xFF8E4960),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3A071D),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    surfaceDim = Color(0xFFE4D6D9),
    surface = Color(0xFFFFF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF0F2),
    surfaceContainer = Color(0xFFF7EAED),
    surfaceContainerHigh = Color(0xFFF1E5E7),
    surfaceContainerHighest = Color(0xFFECDFE1),
    onSurface = Color(0xFF22191B),
    onSurfaceVariant = Color(0xFF514347),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val TealLightPalette = M3Palette(
    id = "teal",
    name = "Lagoon Teal",
    primary = Color(0xFF006A62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF79F2E4),
    onPrimaryContainer = Color(0xFF00201D),
    secondaryContainer = Color(0xFFCCE8E5),
    onSecondaryContainer = Color(0xFF051F1E),
    surfaceDim = Color(0xFFD5DBD9),
    surface = Color(0xFFF4FBF9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5F3),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3E9E8),
    surfaceContainerHighest = Color(0xFFDDE4E2),
    onSurface = Color(0xFF161D1C),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C7),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val MatchaLightPalette = M3Palette(
    id = "matcha",
    name = "Cyber Matcha",
    primary = Color(0xFF436914),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2F08B),
    onPrimaryContainer = Color(0xFF0F2000),
    secondaryContainer = Color(0xFFDAE7CA),
    onSecondaryContainer = Color(0xFF141E0C),
    surfaceDim = Color(0xFFD8DBD2),
    surface = Color(0xFFF8FBF1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5EB),
    surfaceContainer = Color(0xFFECEFE5),
    surfaceContainerHigh = Color(0xFFE6E9E0),
    surfaceContainerHighest = Color(0xFFE1E4DA),
    onSurface = Color(0xFF191D17),
    onSurfaceVariant = Color(0xFF43483E),
    outline = Color(0xFF74796D),
    outlineVariant = Color(0xFFC3C8BC),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val TerracottaLightPalette = M3Palette(
    id = "terracotta",
    name = "Warm Terracotta",
    primary = Color(0xFF9C412E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF3E0500),
    secondaryContainer = Color(0xFFFFDAD3),
    onSecondaryContainer = Color(0xFF2C1511),
    surfaceDim = Color(0xFFE6D6D3),
    surface = Color(0xFFFFF8F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0ED),
    surfaceContainer = Color(0xFFFCEAE6),
    surfaceContainerHigh = Color(0xFFF6E4E0),
    surfaceContainerHighest = Color(0xFFF1DFDB),
    onSurface = Color(0xFF231917),
    onSurfaceVariant = Color(0xFF53433F),
    outline = Color(0xFF85736E),
    outlineVariant = Color(0xFFD8C2BC),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val MonoLightPalette = M3Palette(
    id = "mono",
    name = "Pure Monochrome",
    primary = Color(0xFF474747),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF1B1B1B),
    secondaryContainer = Color(0xFFE2E2E2),
    onSecondaryContainer = Color(0xFF1B1B1B),
    surfaceDim = Color(0xFFDADADA),
    surface = Color(0xFFF9F9F9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    onSurface = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFF474747),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFC7C7C7),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val AllM3Palettes: List<M3Palette> = listOf(
    IndigoDarkPalette,
    AmberDarkPalette,
    SlateDarkPalette,
    ForestDarkPalette,
    OceanDarkPalette,
    VioletDarkPalette,
    EmeraldDarkPalette,
    RoseDarkPalette,
    TealDarkPalette,
    MatchaDarkPalette,
    TerracottaDarkPalette,
    MonoDarkPalette
)

val AllM3LightPalettes: List<M3Palette> = listOf(
    IndigoLightPalette,
    AmberLightPalette,
    SlateLightPalette,
    ForestLightPalette,
    OceanLightPalette,
    VioletLightPalette,
    EmeraldLightPalette,
    RoseLightPalette,
    TealLightPalette,
    MatchaLightPalette,
    TerracottaLightPalette,
    MonoLightPalette
)

/**
 * The 5 named preset swatches for the Appearance page (+ "Custom").
 */
val PresetAppearanceSwatches: List<AppearancePresetSwatch> = listOf(
    AppearancePresetSwatch(id = "indigo", label = "Indigo", swatchColor = Color(0xFF4F83EE)),
    AppearancePresetSwatch(id = "amber", label = "Amber", swatchColor = Color(0xFFF59E0B)),
    AppearancePresetSwatch(id = "slate", label = "Slate", swatchColor = Color(0xFF64748B)),
    AppearancePresetSwatch(id = "forest", label = "Forest Green", swatchColor = Color(0xFF2E8B42)),
    AppearancePresetSwatch(id = "ocean", label = "Ocean", swatchColor = Color(0xFF0284C7))
)

val M3PalettesById: Map<String, M3Palette> = AllM3Palettes.associateBy { it.id }
val M3LightPalettesById: Map<String, M3Palette> = AllM3LightPalettes.associateBy { it.id }

/**
 * Builds a complete Light or Dark Material 3 tonal palette from any user-selected seed Color.
 */
fun generateM3PaletteFromSeed(seedColor: Color, isDark: Boolean): M3Palette {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seedColor.toArgb(), hsv)
    val hue = hsv[0]
    val sat = hsv[1].coerceIn(0.08f, 1.0f)

    fun toneColor(saturationScale: Float, value: Float): Color {
        val s = (sat * saturationScale).coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        return Color(AndroidColor.HSVToColor(floatArrayOf(hue, s, v)))
    }

    return if (isDark) {
        M3Palette(
            id = "custom",
            name = "Custom",
            primary = toneColor(0.45f, 0.96f),
            onPrimary = toneColor(0.85f, 0.25f),
            primaryContainer = toneColor(0.80f, 0.44f),
            onPrimaryContainer = toneColor(0.28f, 0.98f),
            secondaryContainer = toneColor(0.42f, 0.36f),
            onSecondaryContainer = toneColor(0.22f, 0.95f),
            surfaceDim = toneColor(0.20f, 0.08f),
            surface = toneColor(0.18f, 0.10f),
            surfaceContainerLowest = toneColor(0.20f, 0.06f),
            surfaceContainerLow = toneColor(0.18f, 0.13f),
            surfaceContainer = toneColor(0.18f, 0.16f),
            surfaceContainerHigh = toneColor(0.18f, 0.21f),
            surfaceContainerHighest = toneColor(0.18f, 0.26f),
            onSurface = toneColor(0.06f, 0.92f),
            onSurfaceVariant = toneColor(0.14f, 0.80f),
            outline = toneColor(0.16f, 0.58f),
            outlineVariant = toneColor(0.18f, 0.30f),
            error = Color(0xFFFFB4AB),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        M3Palette(
            id = "custom",
            name = "Custom",
            primary = toneColor(0.85f, 0.65f),
            onPrimary = Color.White,
            primaryContainer = toneColor(0.25f, 0.96f),
            onPrimaryContainer = toneColor(0.90f, 0.22f),
            secondaryContainer = toneColor(0.20f, 0.94f),
            onSecondaryContainer = toneColor(0.75f, 0.24f),
            surfaceDim = toneColor(0.08f, 0.87f),
            surface = toneColor(0.03f, 0.99f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = toneColor(0.04f, 0.96f),
            surfaceContainer = toneColor(0.06f, 0.94f),
            surfaceContainerHigh = toneColor(0.07f, 0.91f),
            surfaceContainerHighest = toneColor(0.08f, 0.89f),
            onSurface = toneColor(0.25f, 0.12f),
            onSurfaceVariant = toneColor(0.22f, 0.32f),
            outline = toneColor(0.18f, 0.48f),
            outlineVariant = toneColor(0.12f, 0.80f),
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
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
