package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MegaBrandColors(
    val orange: Color,
    val orangeBright: Color,
    val emerald: Color,
    val emeraldContainer: Color,
    val amber: Color,
    val crimson: Color,
    val terminalBg: Color,
    val cardBorder: Color
)

val LocalMegaColors = staticCompositionLocalOf {
    MegaBrandColors(
        orange = MegaOrange,
        orangeBright = MegaOrangeBright,
        emerald = SignalEmerald,
        emeraldContainer = SignalEmeraldContainer,
        amber = WarningAmber,
        crimson = AlertCrimson,
        terminalBg = ObsidianBlack,
        cardBorder = SteelOutline
    )
}

private val MegaDarkColorScheme = darkColorScheme(
    primary = MegaOrange,
    onPrimary = PureWhite,
    primaryContainer = MegaOrangeContainer,
    onPrimaryContainer = MegaOrangeBright,
    secondary = PureWhite,
    onSecondary = ObsidianBlack,
    secondaryContainer = GraphiteElevated,
    onSecondaryContainer = PureWhite,
    tertiary = SignalEmerald,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = CharcoalSurface,
    onSurface = PureWhite,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = MutedSilver,
    surfaceContainer = CarbonCard,
    surfaceContainerHigh = GraphiteElevated,
    outline = SteelOutline,
    outlineVariant = GraphiteElevated,
    error = AlertCrimson,
    onError = PureWhite,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = SoftWhite
)

private val MegaLightColorScheme = lightColorScheme(
    primary = MegaOrange,
    onPrimary = PureWhite,
    primaryContainer = MegaOrangeLightContainer,
    onPrimaryContainer = MegaOrangeDeep,
    secondary = ObsidianBlack,
    onSecondary = PureWhite,
    secondaryContainer = SoftWhite,
    onSecondaryContainer = ObsidianBlack,
    tertiary = SignalEmerald,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = DarkInk,
    surface = SoftWhite,
    onSurface = DarkInk,
    surfaceVariant = PureWhite,
    onSurfaceVariant = SteelOutline,
    surfaceContainer = SoftWhite,
    surfaceContainerHigh = LightBorder,
    outline = LightBorder,
    outlineVariant = SoftWhite,
    error = AlertCrimson,
    onError = PureWhite
)

private val MegaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DjTrustMegaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MegaDarkColorScheme else MegaLightColorScheme
    val brandColors = MegaBrandColors(
        orange = MegaOrange,
        orangeBright = MegaOrangeBright,
        emerald = SignalEmerald,
        emeraldContainer = if (darkTheme) SignalEmeraldContainer else Color(0xFFD1FAE5),
        amber = WarningAmber,
        crimson = AlertCrimson,
        terminalBg = if (darkTheme) ObsidianBlack else DarkInk,
        cardBorder = if (darkTheme) SteelOutline else LightBorder
    )

    CompositionLocalProvider(LocalMegaColors provides brandColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = MegaShapes,
            content = content
        )
    }
}
