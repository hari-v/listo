package com.hariv.listo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariv.listo.R

// "Ink & signal": a clean neutral base with deep ink text for contrast, one cobalt brand colour for
// actions, and clear category colours that do the popping. Every text colour meets WCAG AA (4.5:1)
// on its background; check rings and dots meet 3:1.

val LightScheme = lightColorScheme(
    primary = Color(0xFF3155E6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E9FF),
    onPrimaryContainer = Color(0xFF0E2273),
    secondary = Color(0xFF4A5162),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7E9F0),
    onSecondaryContainer = Color(0xFF1A1E27),
    // Tertiary marks things that are due soon.
    tertiary = Color(0xFFB4530A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFEBD9),
    onTertiaryContainer = Color(0xFF5C2A00),
    error = Color(0xFFD0342C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4E2),
    onErrorContainer = Color(0xFF5F0F0B),
    background = Color(0xFFF2F3F7),
    onBackground = Color(0xFF12151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12151C),
    surfaceVariant = Color(0xFFE7E9EF),
    onSurfaceVariant = Color(0xFF555B69),
    outline = Color(0xFF8B91A0),
    outlineVariant = Color(0xFFE1E4EA),
    inverseSurface = Color(0xFF252A35),
    inverseOnSurface = Color(0xFFF2F3F7),
    inversePrimary = Color(0xFF93A8FF),
    surfaceTint = Color(0xFF3155E6),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDE0E7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFF1F2F6),
    surfaceContainerHigh = Color(0xFFEAECF1),
    surfaceContainerHighest = Color(0xFFE3E6EC),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFF93A8FF),
    onPrimary = Color(0xFF0B1A5E),
    primaryContainer = Color(0xFF263A8C),
    onPrimaryContainer = Color(0xFFE0E6FF),
    secondary = Color(0xFFB9BECB),
    onSecondary = Color(0xFF1C2029),
    secondaryContainer = Color(0xFF2A2F39),
    onSecondaryContainer = Color(0xFFE3E6EE),
    tertiary = Color(0xFFFFB067),
    onTertiary = Color(0xFF4A2300),
    tertiaryContainer = Color(0xFF4D2A06),
    onTertiaryContainer = Color(0xFFFFE0C4),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0A05),
    errorContainer = Color(0xFF5C1814),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFEDEFF4),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFEDEFF4),
    surfaceVariant = Color(0xFF262A32),
    onSurfaceVariant = Color(0xFFA6ACBA),
    outline = Color(0xFF6F7584),
    outlineVariant = Color(0xFF2C3039),
    inverseSurface = Color(0xFFEDEFF4),
    inverseOnSurface = Color(0xFF1B1E25),
    inversePrimary = Color(0xFF3155E6),
    surfaceTint = Color(0xFF93A8FF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2E323B),
    surfaceDim = Color(0xFF0E1014),
    surfaceContainerLowest = Color(0xFF0A0C0F),
    surfaceContainerLow = Color(0xFF13161B),
    surfaceContainer = Color(0xFF1B1E25),
    surfaceContainerHigh = Color(0xFF22262E),
    surfaceContainerHighest = Color(0xFF2A2F38),
)

/** Category colours by position: tangerine, jade, grape, rose, sky, gold. */
private data class CategoryShade(val lightStrong: Color, val lightInk: Color, val dark: Color)

private val CategoryShades = listOf(
    CategoryShade(Color(0xFFE8650F), Color(0xFFA84605), Color(0xFFFF9350)),
    CategoryShade(Color(0xFF0F9960), Color(0xFF0A6E46), Color(0xFF3FD19A)),
    CategoryShade(Color(0xFF8048E0), Color(0xFF6331C0), Color(0xFFB898FF)),
    CategoryShade(Color(0xFFDE3B66), Color(0xFFB0234C), Color(0xFFFF7FA0)),
    CategoryShade(Color(0xFF1489C4), Color(0xFF0A6694), Color(0xFF5CC4F5)),
    CategoryShade(Color(0xFFB98400), Color(0xFF7F5B00), Color(0xFFF2C548)),
)

/** Bright shades for dots, rings and the widget. */
val CategoryColorsLight: List<Color> = CategoryShades.map { it.lightStrong }
val CategoryColorsDark: List<Color> = CategoryShades.map { it.dark }

/** Deeper shades for text and filled chips in light mode. */
val CategoryInkLight: List<Color> = CategoryShades.map { it.lightInk }

/**
 * The colours for one category.
 * [strong]: dots and check rings. [text]: category name. [fill]/[onFill]: a selected chip. [tint]: a soft background.
 */
@Immutable
data class Tone(val strong: Color, val text: Color, val fill: Color, val onFill: Color, val tint: Color)

@Composable
@ReadOnlyComposable
fun categoryTone(categories: List<String>, name: String): Tone {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val index = categories.indexOf(name)
    if (index < 0) {
        // A label that no longer exists (e.g. on an archived task): neutral.
        return Tone(scheme.outline, scheme.onSurfaceVariant, scheme.onSurfaceVariant, scheme.surface, scheme.surfaceContainerHigh)
    }
    val shade = CategoryShades[index % CategoryShades.size]
    return if (dark) {
        Tone(shade.dark, shade.dark, shade.dark, DarkScheme.background, shade.dark.copy(alpha = 0.16f))
    } else {
        Tone(shade.lightStrong, shade.lightInk, shade.lightInk, Color.White, shade.lightStrong.copy(alpha = 0.12f))
    }
}

/** The bright colour for a category (dots and rings). */
@Composable
@ReadOnlyComposable
fun categoryColor(categories: List<String>, name: String): Color = categoryTone(categories, name).strong

/** The neutral "All" chip: ink fill when selected. */
@Composable
@ReadOnlyComposable
fun neutralTone(): Tone {
    val scheme = MaterialTheme.colorScheme
    return Tone(scheme.onSurfaceVariant, scheme.onSurface, scheme.onSurface, scheme.surface, scheme.surfaceContainerHigh)
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
    displaySmall = base.displaySmall.hanken(FontWeight.Normal, size = 38, line = 44, tracking = -0.9),
    headlineLarge = base.headlineLarge.hanken(FontWeight.Normal),
    headlineMedium = base.headlineMedium.hanken(FontWeight.Normal, size = 30, line = 36, tracking = -0.6),
    headlineSmall = base.headlineSmall.hanken(FontWeight.Medium, size = 22, line = 28, tracking = -0.2),
    titleLarge = base.titleLarge.hanken(FontWeight.SemiBold, size = 20, line = 26),
    titleMedium = base.titleMedium.hanken(FontWeight.SemiBold, size = 16, line = 22),
    titleSmall = base.titleSmall.hanken(FontWeight.SemiBold, size = 14, line = 20),
    bodyLarge = base.bodyLarge.hanken(FontWeight.Normal, size = 16, line = 22),
    bodyMedium = base.bodyMedium.hanken(FontWeight.Normal, size = 14, line = 20),
    bodySmall = base.bodySmall.hanken(FontWeight.Normal, size = 12, line = 16),
    labelLarge = base.labelLarge.hanken(FontWeight.SemiBold, size = 14, line = 20),
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
