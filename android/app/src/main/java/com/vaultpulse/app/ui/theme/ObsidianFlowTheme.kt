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
    // OLED-first Surface Architecture
    val Canvas = Color(0xFF0B0C0E)
    val Surface = Color(0xFF121316)
    val SurfaceDim = Color(0xFF121316)
    val SurfaceBright = Color(0xFF38393C)
    val SurfaceContainerLowest = Color(0xFF0D0E11)
    val SurfaceContainerLow = Color(0xFF1B1B1F)
    val SurfaceContainer = Color(0xFF1F1F23)
    val SurfaceContainerHigh = Color(0xFF292A2D)
    val SurfaceContainerHighest = Color(0xFF343538)

    // Text & Outlines
    val OnSurface = Color(0xFFE3E2E6)
    val OnSurfaceVariant = Color(0xFFC6C5D5)
    val InverseSurface = Color(0xFFE3E2E6)
    val InverseOnSurface = Color(0xFF2F3034)
    val Outline = Color(0xFF908F9E)
    val OutlineVariant = Color(0xFF454653)

    // Brand Core Accents
    val Primary = Color(0xFFBDC2FF)
    val PrimaryContainer = Color(0xFF818CF8)
    val OnPrimary = Color(0xFF131E8C)
    val OnPrimaryContainer = Color(0xFF101B8A)
    val PrimaryIndigo = Color(0xFF818CF8)
    val PrimaryFixed = Color(0xFFE0E0FF)

    // Secondary Accents (Electric Teal)
    val Secondary = Color(0xFF44E2CD)
    val SecondaryContainer = Color(0xFF03C6B2)
    val OnSecondary = Color(0xFF003731)
    val SecondaryTeal = Color(0xFF2DD4BF)
    val SecondaryFixed = Color(0xFF62FAE3)

    // Tertiary Accents (Emerald Mint / Positive Inflow)
    val Tertiary = Color(0xFF4DE082)
    val TertiaryContainer = Color(0xFF00AC58)
    val OnTertiary = Color(0xFF003919)
    val PositiveMint = Color(0xFF4ADE80)
    val PositiveFixed = Color(0xFF6DFE9C)

    // Semantic Status
    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val OnError = Color(0xFF690005)
    val ExpenseRose = Color(0xFFF87171)
    val CautionAmber = Color(0xFFFBBF24)

    // Ghost Borders
    val HairlineBorder = Color(0x0FFFFFFF)
}

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianColors.PrimaryContainer,
    onPrimary = ObsidianColors.OnPrimary,
    primaryContainer = ObsidianColors.PrimaryContainer,
    onPrimaryContainer = ObsidianColors.OnPrimaryContainer,
    secondary = ObsidianColors.Secondary,
    onSecondary = ObsidianColors.OnSecondary,
    secondaryContainer = ObsidianColors.SecondaryContainer,
    onSecondaryContainer = ObsidianColors.OnSecondary,
    tertiary = ObsidianColors.Tertiary,
    onTertiary = ObsidianColors.OnTertiary,
    tertiaryContainer = ObsidianColors.TertiaryContainer,
    background = ObsidianColors.Canvas,
    onBackground = ObsidianColors.OnSurface,
    surface = ObsidianColors.Surface,
    onSurface = ObsidianColors.OnSurface,
    surfaceVariant = ObsidianColors.SurfaceContainerHighest,
    onSurfaceVariant = ObsidianColors.OnSurfaceVariant,
    error = ObsidianColors.ExpenseRose,
    outline = ObsidianColors.Outline,
    outlineVariant = ObsidianColors.OutlineVariant
)

private val ObsidianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.03).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.01).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.01.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.02.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.03.sp
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
