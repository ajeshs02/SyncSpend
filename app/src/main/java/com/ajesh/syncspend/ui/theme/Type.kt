package com.ajesh.syncspend.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.R

/**
 * Cabin's `wght` axis covers 400–700, the full Normal/Medium/SemiBold/Bold range needed — each
 * [Font] entry below points at the same variable-font resource with a different weight instance.
 */
val CabinFontFamily = FontFamily(
    Font(R.font.cabin_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.cabin_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.cabin_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.cabin_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/**
 * Sizes mirror the design's original literal px values, bumped up a step (~+1 to +2sp per role,
 * proportions kept) for a larger, more confident, modern-minimal feel.
 */
val SyncSpendTypography = Typography(
    displayMedium = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.Medium, fontSize = 46.sp, letterSpacing = (-0.03).sp),
    headlineLarge = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 35.5.sp, letterSpacing = (-0.02).sp),
    headlineSmall = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.5.sp),
    titleLarge = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.5.sp),
    titleMedium = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.5.sp),
    bodyMedium = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
    bodySmall = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
    labelSmall = TextStyle(fontFamily = CabinFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
