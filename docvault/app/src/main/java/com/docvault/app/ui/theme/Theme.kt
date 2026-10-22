package com.docvault.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AppColors(
    val isDark: Boolean,
    val surface: Color,
    val surfaceAlt: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val brand: Color,
    val brandGhost: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(false, Paper, Card, Card, Line, Ink900, Ink500, Ink400, Brand, BrandGhost, Rose, Emerald, Amber)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LightScheme = lightColorScheme(
    primary = Brand, onPrimary = Color.White,
    primaryContainer = BrandSoft, onPrimaryContainer = BrandDark,
    secondary = Violet, onSecondary = Color.White,
    tertiary = Amber,
    background = Paper, onBackground = Ink900,
    surface = Paper, onSurface = Ink900,
    surfaceVariant = LineSoft, onSurfaceVariant = Ink500,
    outline = Line, outlineVariant = LineSoft,
    error = Rose, onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FB4FF), onPrimary = Color(0xFF06204F),
    primaryContainer = Color(0xFF1B3A72), onPrimaryContainer = Color(0xFFD9E5FF),
    secondary = Color(0xFFB9A8FF), onSecondary = Color(0xFF1C1140),
    tertiary = Amber,
    background = DarkBg, onBackground = DarkInk,
    surface = DarkBg, onSurface = DarkInk,
    surfaceVariant = DarkCard, onSurfaceVariant = DarkInkDim,
    outline = DarkLine, outlineVariant = DarkLine,
    error = Color(0xFFFF8A8D), onError = Color(0xFF3A0A0C),
)

@Composable
fun DocVaultTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val extra = if (dark) AppColors(
        isDark = true, surface = DarkBg, surfaceAlt = DarkSurface, card = DarkCard,
        border = DarkLine, textPrimary = DarkInk, textSecondary = DarkInkDim,
        textTertiary = Color(0xFF6E7683), brand = Color(0xFF8FB4FF),
        brandGhost = Color(0xFF1B3A72), danger = Color(0xFFFF8A8D),
        success = Color(0xFF6DD39A), warning = Amber,
    ) else AppColors(
        isDark = false, surface = Paper, surfaceAlt = Card, card = Card,
        border = Line, textPrimary = Ink900, textSecondary = Ink500,
        textTertiary = Ink400, brand = Brand, brandGhost = BrandGhost,
        danger = Rose, success = Emerald, warning = Amber,
    )

    CompositionLocalProvider(LocalAppColors provides extra) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}