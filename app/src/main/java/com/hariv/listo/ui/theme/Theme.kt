package com.hariv.listo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariv.listo.R

// "Slate & stone": cool limestone and paper, slate ink, one verdigris accent for recurring items.

val LightScheme = lightColorScheme(
    primary = Color(0xFF22302D),
    onPrimary = Color(0xFFF4F6F4),
    primaryContainer = Color(0xFFD7E0DC),
    onPrimaryContainer = Color(0xFF1C2422),
    secondary = Color(0xFF4E5A57),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE3E0),
    onSecondaryContainer = Color(0xFF1C2422),
    tertiary = Color(0xFF3F6E64),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD5E7E1),
    onTertiaryContainer = Color(0xFF12332D),
    error = Color(0xFFA3443B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF3DCD8),
    onErrorContainer = Color(0xFF47120D),
    background = Color(0xFFEEF0EE),
    onBackground = Color(0xFF1C2422),
    surface = Color(0xFFFAFBFA),
    onSurface = Color(0xFF1C2422),
    surfaceVariant = Color(0xFFE3E7E4),
    onSurfaceVariant = Color(0xFF5A6461),
    outline = Color(0xFF9AA39F),
    outlineVariant = Color(0xFFD3D9D6),
    inverseSurface = Color(0xFF2A3230),
    inverseOnSurface = Color(0xFFEEF0EE),
    inversePrimary = Color(0xFFD3DCD8),
    surfaceTint = Color(0xFF22302D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAFBFA),
    surfaceDim = Color(0xFFDCE0DD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8F7),
    surfaceContainer = Color(0xFFF2F4F2),
    surfaceContainerHigh = Color(0xFFEAEDEB),
    surfaceContainerHighest = Color(0xFFE3E7E4),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFFD3DCD8),
    onPrimary = Color(0xFF172120),
    primaryContainer = Color(0xFF2E3A37),
    onPrimaryContainer = Color(0xFFE2E7E4),
    secondary = Color(0xFFB7C2BE),
    onSecondary = Color(0xFF1E2726),
    secondaryContainer = Color(0xFF2C3533),
    onSecondaryContainer = Color(0xFFE2E7E4),
    tertiary = Color(0xFF86BBAE),
    onTertiary = Color(0xFF0E2B25),
    tertiaryContainer = Color(0xFF24453E),
    onTertiaryContainer = Color(0xFFCFE8E1),
    error = Color(0xFFE3958B),
    onError = Color(0xFF3B0B06),
    errorContainer = Color(0xFF5A2019),
    onErrorContainer = Color(0xFFF6D9D4),
    background = Color(0xFF121615),
    onBackground = Color(0xFFE2E7E4),
    surface = Color(0xFF1A1F1E),
    onSurface = Color(0xFFE2E7E4),
    surfaceVariant = Color(0xFF262D2B),
    onSurfaceVariant = Color(0xFF9CA6A2),
    outline = Color(0xFF66706C),
    outlineVariant = Color(0xFF323A38),
    inverseSurface = Color(0xFFE2E7E4),
    inverseOnSurface = Color(0xFF1B2120),
    inversePrimary = Color(0xFF22302D),
    surfaceTint = Color(0xFFD3DCD8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2E3533),
    surfaceDim = Color(0xFF121615),
    surfaceContainerLowest = Color(0xFF0E1211),
    surfaceContainerLow = Color(0xFF161B1A),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF232A28),
    surfaceContainerHighest = Color(0xFF2B3331),
)

/** Muted marker colours for categories, by position: slate blue, sage, ochre, clay, dusk, steel. */
val CategoryColorsLight = listOf(
    Color(0xFF5B7083), Color(0xFF6E8159), Color(0xFF9C7C45),
    Color(0xFF9A6762), Color(0xFF76698F), Color(0xFF4A7676),
)
val CategoryColorsDark = listOf(
    Color(0xFF9DB0C2), Color(0xFFA9BC92), Color(0xFFD2B47E),
    Color(0xFFD3A19B), Color(0xFFB3A7CC), Color(0xFF8CBDBD),
)

/** Colour for a category name; unknown names (e.g. on archived tasks) get a neutral tone. */
@Composable
@ReadOnlyComposable
fun categoryColor(categories: List<String>, name: String): Color {
    val index = categories.indexOf(name)
    if (index < 0) return MaterialTheme.colorScheme.outline
    val palette = if (isSystemInDarkTheme()) CategoryColorsDark else CategoryColorsLight
    return palette[index % palette.size]
}

val Hanken = FontFamily(
    Font(R.font.hanken_light, FontWeight.Light),
    Font(R.font.hanken_regular, FontWeight.Normal),
    Font(R.font.hanken_medium, FontWeight.Medium),
    Font(R.font.hanken_semibold, FontWeight.SemiBold),
)

private fun TextStyle.hanken(weight: FontWeight, size: Int? = null, line: Int? = null, tracking: Double = 0.0) = copy(
    fontFamily = Hanken,
    fontWeight = weight,
    fontSize = size?.sp ?: fontSize,
    lineHeight = line?.sp ?: lineHeight,
    letterSpacing = tracking.sp,
)

private val base = Typography()

val ListoTypography = Typography(
    displayLarge = base.displayLarge.hanken(FontWeight.Light),
    displayMedium = base.displayMedium.hanken(FontWeight.Light),
    // The day name heading the task list.
    displaySmall = base.displaySmall.hanken(FontWeight.Light, size = 40, line = 46, tracking = -0.8),
    headlineLarge = base.headlineLarge.hanken(FontWeight.Light),
    headlineMedium = base.headlineMedium.hanken(FontWeight.Light, size = 30, line = 36, tracking = -0.4),
    headlineSmall = base.headlineSmall.hanken(FontWeight.Normal, size = 22, line = 28, tracking = -0.2),
    titleLarge = base.titleLarge.hanken(FontWeight.Medium, size = 20, line = 26),
    titleMedium = base.titleMedium.hanken(FontWeight.Medium, size = 16, line = 22),
    titleSmall = base.titleSmall.hanken(FontWeight.Medium, size = 14, line = 20),
    bodyLarge = base.bodyLarge.hanken(FontWeight.Normal, size = 16, line = 22),
    bodyMedium = base.bodyMedium.hanken(FontWeight.Normal, size = 14, line = 20),
    bodySmall = base.bodySmall.hanken(FontWeight.Normal, size = 12, line = 16),
    labelLarge = base.labelLarge.hanken(FontWeight.Medium, size = 14, line = 20),
    labelMedium = base.labelMedium.hanken(FontWeight.Medium, size = 12, line = 16, tracking = 0.1),
    labelSmall = base.labelSmall.hanken(FontWeight.Medium, size = 11, line = 16, tracking = 0.2),
)

val ListoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun ListoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        typography = ListoTypography,
        shapes = ListoShapes,
        content = content,
    )
}
