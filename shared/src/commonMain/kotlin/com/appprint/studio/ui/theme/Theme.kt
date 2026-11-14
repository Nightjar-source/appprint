package com.appprint.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.shared.generated.resources.Res
import com.appprint.studio.shared.generated.resources.cairo_black
import com.appprint.studio.shared.generated.resources.cairo_bold
import com.appprint.studio.shared.generated.resources.cairo_extrabold
import com.appprint.studio.shared.generated.resources.cairo_regular
import com.appprint.studio.shared.generated.resources.cairo_semibold
import com.appprint.studio.shared.generated.resources.reem_kufi_bold
import com.appprint.studio.shared.generated.resources.reem_kufi_semibold
import org.jetbrains.compose.resources.Font as ResFont

/**
 * «طبعتي — استوديو الطباعة الذكي 1:1»
 * Brand theme straight from the approved HTML design tokens:
 * deep-print-shop green (#006C47 / #70DBA7), tomato red (#B32638 / #FFB3B6)
 * on warm paper-ink neutrals. Material 3 Expressive throughout.
 */

private val Green = androidx.compose.ui.graphics.Color(0xFF006C47)
private val GreenDark = androidx.compose.ui.graphics.Color(0xFF70DBA7)
private val Red = androidx.compose.ui.graphics.Color(0xFFB32638)
private val RedDark = androidx.compose.ui.graphics.Color(0xFFFFB3B6)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF8CF7C2),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF002113),
    secondary = androidx.compose.ui.graphics.Color(0xFF4D6356),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFCFE9D8),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0A1F15),
    tertiary = Red,
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD9),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF40000B),
    background = androidx.compose.ui.graphics.Color(0xFFF3F7F4),
    onBackground = androidx.compose.ui.graphics.Color(0xFF161D19),
    surface = androidx.compose.ui.graphics.Color(0xFFF3F7F4),
    onSurface = androidx.compose.ui.graphics.Color(0xFF161D19),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE6ECE8),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF3F4943),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFECF1ED),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFE6ECE8),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE0E6E2),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFDAE0DC),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFF2B322E),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFFEAF0EB),
    outline = androidx.compose.ui.graphics.Color(0xFF707973),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFBFC9C1),
    error = Red,
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD9),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF40000B),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = GreenDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003823),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF005235),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF8CF7C2),
    secondary = androidx.compose.ui.graphics.Color(0xFFB4CCBC),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1F352A),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF354B3F),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFCFE9D8),
    tertiary = RedDark,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF680016),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF910824),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD9),
    background = androidx.compose.ui.graphics.Color(0xFF0E1512),
    onBackground = androidx.compose.ui.graphics.Color(0xFFDEE4DF),
    surface = androidx.compose.ui.graphics.Color(0xFF0E1512),
    onSurface = androidx.compose.ui.graphics.Color(0xFFDEE4DF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A211E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBFC9C1),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF090E0C),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF161D1A),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1A211E),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF242B28),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF2F3633),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFFDEE4DF),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF2B322E),
    outline = androidx.compose.ui.graphics.Color(0xFF89938C),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF3F4943),
    error = RedDark,
    onError = androidx.compose.ui.graphics.Color(0xFF680016),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF910824),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD9),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
)

/** Canvas backdrop behind the sheet (design: --md-canvas). */
val TabatiCanvasLight = androidx.compose.ui.graphics.Color(0xFFD8DFDA)
val TabatiCanvasDark = androidx.compose.ui.graphics.Color(0xFF070B09)

/** Cairo — body/UI family (Regular, SemiBold, Bold, ExtraBold, Black). */
val CairoFamily = FontFamily(
    ResFont(Res.font.cairo_regular, FontWeight.Normal),
    ResFont(Res.font.cairo_semibold, FontWeight.SemiBold),
    ResFont(Res.font.cairo_bold, FontWeight.Bold),
    ResFont(Res.font.cairo_extrabold, FontWeight.ExtraBold),
    ResFont(Res.font.cairo_black, FontWeight.Black),
)

/** Reem Kufi — the «طبعتي» wordmark and display caps. */
val ReemKufiFamily = FontFamily(
    ResFont(Res.font.reem_kufi_semibold, FontWeight.SemiBold),
    ResFont(Res.font.reem_kufi_bold, FontWeight.Bold),
)

private val TabatiTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = ReemKufiFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    displayMedium = Typography().displayMedium.copy(fontFamily = ReemKufiFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Black),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Black),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Black),
    titleLarge = Typography().titleLarge.copy(fontFamily = ReemKufiFamily, fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontFamily = CairoFamily, fontWeight = FontWeight.ExtraBold),
    titleSmall = Typography().titleSmall.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Bold),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Normal),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Normal),
    bodySmall = Typography().bodySmall.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Normal),
    labelLarge = Typography().labelLarge.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Bold),
    labelMedium = Typography().labelMedium.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Bold),
    labelSmall = Typography().labelSmall.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Bold),
)

private val TabatiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

fun colorSchemeFor(theme: AppTheme, systemDark: Boolean, platform: Pair<ColorScheme, ColorScheme>? = null): ColorScheme = when (theme) {
    // Material You dynamic scheme on Android 12+ when available; brand fallback everywhere else.
    AppTheme.SYSTEM -> if (platform != null) {
        if (systemDark) platform.second else platform.first
    } else if (systemDark) DarkColors else LightColors
    AppTheme.DARK -> DarkColors
    AppTheme.LIGHT -> LightColors
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TabatiTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val platform = rememberPlatformColorSchemes()
    val scheme = colorSchemeFor(theme, dark, platform)
    // RTL is forced for the whole app — the design is Arabic-first.
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
        LocalContentColor provides scheme.onSurface,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            typography = TabatiTypography,
            shapes = TabatiShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/** Canvas backdrop — brand tokens (see design: --md-canvas). */
@Composable
fun tabatiCanvasColor(dark: Boolean): androidx.compose.ui.graphics.Color =
    if (dark) TabatiCanvasDark else TabatiCanvasLight
