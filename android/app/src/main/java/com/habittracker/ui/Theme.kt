package com.habittracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Violet = Color(0xFF5A4FE0)
private val Teal = Color(0xFF00A88E)
private val Coral = Color(0xFFF1745B)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF160B6B),
    secondary = Teal,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC7F4E7),
    onSecondaryContainer = Color(0xFF00332A),
    tertiary = Coral,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDFD6),
    onTertiaryContainer = Color(0xFF541B0C),
    background = Color(0xFFF4F5FC),
    onBackground = Color(0xFF191A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191A2E),
    surfaceVariant = Color(0xFFECEEF9),
    onSurfaceVariant = Color(0xFF575B76),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFF),
    surfaceContainer = Color(0xFFF1F2FB),
    surfaceContainerHigh = Color(0xFFECEEF8),
    surfaceContainerHighest = Color(0xFFE6E8F5),
    outline = Color(0xFFC3C6DC),
    outlineVariant = Color(0xFFE2E4F1),
    error = Color(0xFFDC4C4C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBDB6FF),
    onPrimary = Color(0xFF21188C),
    primaryContainer = Color(0xFF4237B8),
    onPrimaryContainer = Color(0xFFE5E1FF),
    secondary = Color(0xFF5FDCC2),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF00513D),
    onSecondaryContainer = Color(0xFFC7F4E7),
    tertiary = Color(0xFFFFB4A0),
    onTertiary = Color(0xFF5A1B0C),
    tertiaryContainer = Color(0xFF7C3221),
    onTertiaryContainer = Color(0xFFFFDAD0),
    background = Color(0xFF12121F),
    onBackground = Color(0xFFE6E5F2),
    surface = Color(0xFF1B1B2B),
    onSurface = Color(0xFFE6E5F2),
    surfaceVariant = Color(0xFF2A2B40),
    onSurfaceVariant = Color(0xFFB8BAD0),
    surfaceContainerLowest = Color(0xFF0D0D18),
    surfaceContainerLow = Color(0xFF171726),
    surfaceContainer = Color(0xFF1E1E30),
    surfaceContainerHigh = Color(0xFF26263A),
    surfaceContainerHighest = Color(0xFF2E2E44),
    outline = Color(0xFF4A4C63),
    outlineVariant = Color(0xFF33344A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val HabitShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val HabitPalette = listOf(
    Color(0xFF6C5CE7),
    Color(0xFF00A88E),
    Color(0xFF0C82E0),
    Color(0xFFE5673F),
    Color(0xFFE24A93),
    Color(0xFF12A5A5),
    Color(0xFFC98A00),
    Color(0xFF7B4DFF),
)

fun habitColor(key: String): Color =
    if (key.isBlank()) HabitPalette.first()
    else HabitPalette[Math.floorMod(key.hashCode(), HabitPalette.size)]

fun habitColorOf(category: String, name: String): Color =
    habitColor(category.trim().ifBlank { name.trim() })

val HeatRamp = listOf(
    Color(0xFF00A88E),
    Color(0xFF0C82E0),
    Color(0xFF6C5CE7),
    Color(0xFF9B4DE0),
    Color(0xFFE24A93),
)

fun progressColor(ratio: Float): Color =
    androidx.compose.ui.graphics.lerp(HeatRamp.first(), HeatRamp[2], ratio.coerceIn(0f, 1f))

fun heatColor(ratio: Float): Color {
    val level = (ratio * HeatRamp.size).toInt().coerceIn(0, HeatRamp.size - 1)
    return HeatRamp[level]
}

@Composable
fun HabitTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = HabitShapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
