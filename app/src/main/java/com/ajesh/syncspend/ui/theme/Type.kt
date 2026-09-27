package com.ajesh.syncspend.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.R

val ArchivoFontFamily = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
)

/**
 * Sizes mirror the design's literal px values (SyncSpendPhone.dc.html renders
 * at a 372px-wide phone canvas, so its px map ~1:1 onto dp/sp here).
 */
val SyncSpendTypography = Typography(
    displayMedium = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.Medium, fontSize = 44.sp, letterSpacing = (-0.03).sp),
    headlineLarge = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.02).sp),
    headlineSmall = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleMedium = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp),
    titleSmall = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
    bodyLarge = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.5.sp),
    labelLarge = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
    labelMedium = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    labelSmall = TextStyle(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
)
