package com.vaultpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ObsidianColors {
    val Canvas = Color(0xFF0B0C0E)
    val Surface = Color(0xFF121316)
    val SurfaceContainer = Color(0xFF1A1C1E)
    val SurfaceContainerHigh = Color(0xFF222428)
    val SurfaceContainerHighest = Color(0xFF2D3036)
    val PrimaryIndigo = Color(0xFF818CF8)
    val PrimaryFixed = Color(0xFFBDC2FF)
    val SecondaryTeal = Color(0xFF2DD4BF)
    val PositiveMint = Color(0xFF4DE082)
    val PositiveFixed = Color(0xFF6DFE9C)
    val ExpenseRose = Color(0xFFF87171)
    val CautionAmber = Color(0xFFFBBF24)
    val OnSurface = Color(0xFFE3E2E6)
    val OnSurfaceVariant = Color(0xFFC6C5D5)
    val HairlineBorder = Color(0x0FFFFFFF)
}

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianColors.PrimaryIndigo,
    onPrimary = Color(0xFF131E8C),
    primaryContainer = ObsidianColors.PrimaryFixed,
    secondary = ObsidianColors.SecondaryTeal,
    onSecondary = Color(0xFF003731),
    tertiary = ObsidianColors.PositiveMint,
    onTertiary = Color(0xFF003919),
    background = ObsidianColors.Canvas,
    onBackground = ObsidianColors.OnSurface,
    surface = ObsidianColors.Surface,
    onSurface = ObsidianColors.OnSurface,
    surfaceVariant = ObsidianColors.SurfaceContainerHighest,
    onSurfaceVariant = ObsidianColors.OnSurfaceVariant,
    error = ObsidianColors.ExpenseRose
)

private val ObsidianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun ObsidianFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = ObsidianTypography,
        content = content
    )
}
