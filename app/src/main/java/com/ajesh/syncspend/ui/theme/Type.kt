package com.ajesh.syncspend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.R
import com.ajesh.syncspend.domain.model.FontChoice

val ArchivoFontFamily = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
)

/** One weight axis, four instances: each [Font] entry points at the same variable-font resource. */
private fun variableFamily(res: Int) = FontFamily(
    Font(res, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(res, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(res, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(res, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val InterFontFamily = variableFamily(R.font.inter_variable)
private val ManropeFontFamily = variableFamily(R.font.manrope_variable)
private val JakartaFontFamily = variableFamily(R.font.jakarta_variable)
private val OutfitFontFamily = variableFamily(R.font.outfit_variable)

fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.ARCHIVO -> ArchivoFontFamily
    FontChoice.INTER -> InterFontFamily
    FontChoice.MANROPE -> ManropeFontFamily
    FontChoice.JAKARTA -> JakartaFontFamily
    FontChoice.OUTFIT -> OutfitFontFamily
}

/**
 * Sizes mirror the design's literal px values (SyncSpendPhone.dc.html renders
 * at a 372px-wide phone canvas, so its px map ~1:1 onto dp/sp here).
 */
fun typographyFor(choice: FontChoice): Typography {
    val family = fontFamilyFor(choice)
    return Typography(
        displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 44.sp, letterSpacing = (-0.03).sp),
        headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.02).sp),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 11.5.sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
    )
}

/** The design's original, locked typography — Archivo at every role. Default when no [FontChoice] is threaded in. */
val SyncSpendTypography = typographyFor(FontChoice.ARCHIVO)

/**
 * Every top-level screen title (Settings, Categories, Stats, Transactions, ...) reads at this
 * exact size — one named style instead of each screen repeating its own `headlineSmall.copy(...)`.
 */
val ScreenTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp)
