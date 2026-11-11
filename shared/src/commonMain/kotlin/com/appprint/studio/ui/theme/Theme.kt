package com.appprint.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.AppTheme

/**
 * Brand identity taken from the approved HTML design («مطبعة البطاقات العراقية»):
 * deep print-shop ink, Iraqi green & red, antique gold, warm paper.
 *
 * Color selection (M3-compliant):
 *  - SYSTEM → dynamic Material You scheme on Android 12+ (wallpaper-derived, every
 *    on* pair tonally guaranteed to contrast), brand scheme as fallback elsewhere.
 *  - LIGHT / DARK → brand identity, fixed, always with correct M3 role pairing
 *    (every surface pairs with its explicit onSurface / onContainer color).
 */

private val Ink = androidx.compose.ui.graphics.Color(0xFF151816)
private val InkSurface = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
private val InkSurfaceContainer = androidx.compose.ui.graphics.Color(0xFF22241F)
private val InkSurfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF262824)
private val InkOnSurface = androidx.compose.ui.graphics.Color(0xFFF2F0E9)
private val InkOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB9B6AC)

private val Paper = androidx.compose.ui.graphics.Color(0xFFF4F1EA)
private val PaperSurface = androidx.compose.ui.graphics.Color(0xFFFDFAF1)
private val PaperSurfaceContainer = androidx.compose.ui.graphics.Color(0xFFEDE9DE)
private val PaperOnSurface = androidx.compose.ui.graphics.Color(0xFF1C1B18)
private val PaperOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF57534A)

private val Green = androidx.compose.ui.graphics.Color(0xFF007A3D)
private val GreenDarkTheme = androidx.compose.ui.graphics.Color(0xFF35C877)
private val Red = androidx.compose.ui.graphics.Color(0xFFC8102E)
private val RedDarkTheme = androidx.compose.ui.graphics.Color(0xFFFF6B7E)
private val Gold = androidx.compose.ui.graphics.Color(0xFFB98A1E)
private val GoldDarkTheme = androidx.compose.ui.graphics.Color(0xFFE8B92E)

private val DarkColors = darkColorScheme(
    primary = GreenDarkTheme,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00391E),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF0D5B3A),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFB7F0CD),
    secondary = RedDarkTheme,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF5C0011),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF7A2440),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9DD),
    tertiary = GoldDarkTheme,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF3A2C00),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF5C4300),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF5E6A8),
    background = Ink,
    onBackground = InkOnSurface,
    surface = InkSurface,
    onSurface = InkOnSurface,
    surfaceVariant = InkSurfaceContainerHigh,
    onSurfaceVariant = InkOnSurfaceVariant,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF101210),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF191B18),
    surfaceContainer = InkSurfaceContainer,
    surfaceContainerHigh = InkSurfaceContainerHigh,
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF2E2F2B),
    outline = androidx.compose.ui.graphics.Color(0xFF8F8C82),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF3A3B36),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFB9F2CF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF002912),
    secondary = Red,
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9DD),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF400010),
    tertiary = Gold,
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF5E6A8),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF241A00),
    background = Paper,
    onBackground = PaperOnSurface,
    surface = PaperSurface,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperSurfaceContainer,
    onSurfaceVariant = PaperOnSurfaceVariant,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF8F6EF),
    surfaceContainer = PaperSurfaceContainer,
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE7E4D8),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFE1DED2),
    outline = androidx.compose.ui.graphics.Color(0xFF79766C),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFCECCC1),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
)

/** M3 Expressive emphasized type (heavier weights on display/headline/title). */
private val AppTypography = Typography().let { base ->
    Typography(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.25).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Normal),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold),
    )
}

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun AppPrintTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val platform = rememberPlatformColorSchemes()
    val scheme = when {
        // Material You dynamic color when the device supports it (SYSTEM mode)
        theme == AppTheme.SYSTEM && platform != null -> if (dark) platform.second else platform.first
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
