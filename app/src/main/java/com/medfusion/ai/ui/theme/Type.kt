package com.medfusion.ai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tuned for readability and a professional healthcare feel. Uses the
 * platform default font family (SF-like on newer devices) for a clean, neutral
 * tone; weights and tracking are set for calm hierarchy rather than flashiness.
 */
private val Default = FontFamily.Default

val MedFusionTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    ),
)
