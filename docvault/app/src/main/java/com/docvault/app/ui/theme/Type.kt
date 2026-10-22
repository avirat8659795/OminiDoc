package com.docvault.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

val AppTypography = Typography(
    displaySmall = TextStyle(Sans, FontWeight.Bold, 30.sp, 36.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(Sans, FontWeight.Bold, 25.sp, 31.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(Sans, FontWeight.SemiBold, 21.sp, 27.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(Sans, FontWeight.SemiBold, 19.sp, 25.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(Sans, FontWeight.SemiBold, 16.sp, 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(Sans, FontWeight.SemiBold, 14.sp, 20.sp),
    bodyLarge = TextStyle(Sans, FontWeight.Normal, 15.5.sp, 23.sp),
    bodyMedium = TextStyle(Sans, FontWeight.Normal, 14.sp, 20.sp),
    bodySmall = TextStyle(Sans, FontWeight.Normal, 12.5.sp, 18.sp),
    labelLarge = TextStyle(Sans, FontWeight.SemiBold, 13.5.sp, 18.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(Sans, FontWeight.Medium, 12.sp, 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(Sans, FontWeight.Medium, 11.sp, 15.sp, letterSpacing = 0.3.sp),
)