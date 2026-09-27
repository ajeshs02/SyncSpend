package com.ajesh.syncspend.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Every color token below is copied verbatim (same hex/alpha values) from the
 * approved SyncSpend design (Claude Design canvas "ExpenseTrackerV2",
 * SyncSpendPhone.dc.html) so the app matches the mock pixel-for-pixel. Do not
 * "improve" these — if a value looks off, re-check the design file, don't
 * eyeball a replacement.
 */
object SyncSpendPalette {

    /**
     * The vibrant green of the active nav icon and the Income arrows. It is only ever a glyph/icon
     * tint — never a background or fill; fills use the mint gradient ([LightMintGradient]/[DarkMintGradient]).
     */
    val BrandGreen = Color(0xFF8ECF63)

    // ---- Light ----
    val LightInk = Color(0xFF151A12)
    val LightSub = Color(0xFF7D867A)
    val LightCard = Color(0xFFFFFFFF)
    val LightLine = Color(0x1A151A12) // rgba(21,26,18,.10)
    val LightDark = Color(0xFF0F1A12)
    val LightAcc = Color(0xFF2F7A45)
    val LightAcc2 = Color(0xFF5FBF7D)
    val LightPos = Color(0xFF2F7A45)
    val LightNeg = Color(0xFFC0392B)
    val LightPill = Color(0xB8FFFFFF) // rgba(255,255,255,.72)
    val LightTile = Color(0xFFF1F4EF)
    val LightMink = Color(0xFF15241A)
    val LightMsub = Color(0x99151A12) // rgba(21,26,18,.6)
    val LightDim = Color(0x2E151A12) // rgba(21,26,18,.18)
    val LightOnAcc = Color(0xFFFFFFFF)
    val LightSheet = Color(0xFFFFFFFF)
    val LightScrEnd = Color(0xFFEAF0EC)

    private val lightScrStops = arrayOf(
        0.00f to Color(0xFFE8EFEA),
        0.07f to Color(0xFFF2F6F1),
        0.15f to Color(0xFFFAFCF9),
        // The bottom band is shorter than the top one (it sits behind the nav): it starts later.
        0.88f to Color(0xFFFAFCF9),
        0.95f to Color(0xFFF2F6F1),
        1.00f to Color(0xFFEAF0EC),
    )
    val LightScreenGradient = Brush.verticalGradient(colorStops = lightScrStops)

    val LightCardGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFFFFFFF), 0.6f to Color(0xFFF3F7F1), 1f to Color(0xFFEAF1E6)),
    )

    // Selected segment / chip / calendar-day fill and its text, and the solid
    // "Apply"/"OK" button pair (design: seg(), chip(), applyBg/applyFg).
    val LightSelectedBrush = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF18251A), 1f to Color(0xFF0D150F)),
    )
    val LightOnSelected = Color(0xFFF4F7F1)
    val LightButton = Color(0xFF101710)
    val LightOnButton = Color(0xFFF4F7F1)
    val LightDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF0F1A12), 0.52f to Color(0xFF16331F), 1f to Color(0xFF0C1B11)),
    )
    val LightMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFCFE6CF), 0.45f to Color(0xFFBCDCBD), 1f to Color(0xFFA9D2AC)),
    )
    // Chart/bar fills: the mint gradient itself (Stats "Top spending" card, "Active reminders" header),
    // and a paler take on it for the de-emphasised bars.
    val LightChartFill = LightMintGradient
    val LightChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFE2F0E2), 0.45f to Color(0xFFD9EBDA), 1f to Color(0xFFCFE6CF)),
    )
    val LightNavGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF171D18), 1f to Color(0xFF0D120E)),
    )

    // ---- Dark ----
    val DarkInk = Color(0xFFEFF2EC)
    val DarkSub = Color(0xFF8A9187)
    val DarkCard = Color(0xFF1B201C)
    val DarkLine = Color(0x24FFFFFF) // rgba(255,255,255,.14)
    val DarkDark = Color(0xFF070A08)
    val DarkAcc = Color(0xFF7FD39A)
    val DarkAcc2 = Color(0xFF5FBF7D)
    val DarkPos = Color(0xFF7FD39A)
    val DarkNeg = Color(0xFFF08579)
    val DarkPill = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val DarkTile = Color(0xFF272C28)
    val DarkMink = Color(0xFFE8F4EA)
    val DarkMsub = Color(0xADE8F4EA) // rgba(232,244,234,.68)
    val DarkDim = Color(0x42FFFFFF) // rgba(255,255,255,.26)
    val DarkOnAcc = Color(0xFF07120B)
    val DarkSheet = Color(0xFF1C211D)
    val DarkScrEnd = Color(0xFF121A15)

    private val darkScrStops = arrayOf(
        0.00f to Color(0xFF121A15),
        0.07f to Color(0xFF0E120F),
        0.15f to Color(0xFF0D0F0E),
        0.88f to Color(0xFF0D0F0E),
        0.95f to Color(0xFF0E120F),
        1.00f to Color(0xFF121A15),
    )
    val DarkScreenGradient = Brush.verticalGradient(colorStops = darkScrStops)

    val DarkCardGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1E231E), 0.6f to Color(0xFF1B201C), 1f to Color(0xFF171B18)),
    )
    val DarkDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF0B140E), 0.52f to Color(0xFF153A22), 1f to Color(0xFF08100A)),
    )
    val DarkMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1C3A25), 0.45f to Color(0xFF16301D), 1f to Color(0xFF102316)),
    )
    // The same mint gradient, lifted in brightness: the card's own deep forest green would vanish on dark cards.
    val DarkChartFill = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF4A9A68), 0.45f to Color(0xFF3F8A5B), 1f to Color(0xFF347A4E)),
    )
    val DarkChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF2C5B3E), 0.45f to Color(0xFF264F36), 1f to Color(0xFF204530)),
    )
    val DarkNavGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1E231F), 1f to Color(0xFF101410)),
    )

    val DarkSelectedBrush = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFF2F5EF), 1f to Color(0xFFE0E5DC)),
    )
    val DarkOnSelected = Color(0xFF101710)
    val DarkButton = Color(0xFFF2F6EE)
    val DarkOnButton = Color(0xFF101710)

    // ---- Theme lab: alternate accent families (temporary — Settings lets the user audition these
    // against the approved Forest palette above; unlike Forest these are not locked to a design file
    // and are expected to be tuned from feedback before one is finalized). Only the accent-bearing
    // tokens differ per family — ink/sub/card/line/tile/sheet/screenGradient etc. stay identical
    // (monochrome) across every palette; see [ui.theme.accentsFor]. ----

    // Neo Mint: a brighter, more fluorescent spring-green/mint than Forest.
    val NeoMintLightAcc = Color(0xFF12B76A)
    val NeoMintLightAcc2 = Color(0xFF3DDC97)
    val NeoMintLightBrand = Color(0xFF6EE7B7)
    val NeoMintLightOnAcc = Color(0xFFFFFFFF)
    val NeoMintLightDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF0B1F16), 0.52f to Color(0xFF12472F), 1f to Color(0xFF081911)),
    )
    val NeoMintLightMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFD3F5E6), 0.45f to Color(0xFFB9EFD7), 1f to Color(0xFF9FE6C7)),
    )
    val NeoMintLightChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFE7F9EF), 0.45f to Color(0xFFDAF5E6), 1f to Color(0xFFC9EFDB)),
    )
    val NeoMintDarkAcc = Color(0xFF34E39C)
    val NeoMintDarkAcc2 = Color(0xFF6EE7B7)
    val NeoMintDarkBrand = Color(0xFF6EE7B7)
    val NeoMintDarkOnAcc = Color(0xFF04140D)
    val NeoMintDarkDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF071A11), 0.52f to Color(0xFF0F3D28), 1f to Color(0xFF05130C)),
    )
    val NeoMintDarkMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF163A28), 0.45f to Color(0xFF123020), 1f to Color(0xFF0D2418)),
    )
    val NeoMintDarkChartFill = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF38C98C), 0.45f to Color(0xFF2FB87C), 1f to Color(0xFF26A76C)),
    )
    val NeoMintDarkChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF245C3E), 0.45f to Color(0xFF1F5036), 1f to Color(0xFF19442D)),
    )

    // Ocean: a blue accent family.
    val OceanLightAcc = Color(0xFF2563EB)
    val OceanLightAcc2 = Color(0xFF60A5FA)
    val OceanLightBrand = Color(0xFF60A5FA)
    val OceanLightOnAcc = Color(0xFFFFFFFF)
    val OceanLightDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF0B1526), 0.52f to Color(0xFF163C6B), 1f to Color(0xFF081020)),
    )
    val OceanLightMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFD3E4FB), 0.45f to Color(0xFFBBD6F8), 1f to Color(0xFFA3C7F4)),
    )
    val OceanLightChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFE7F0FD), 0.45f to Color(0xFFDAE9FB), 1f to Color(0xFFCBDFF8)),
    )
    val OceanDarkAcc = Color(0xFF5B9DFF)
    val OceanDarkAcc2 = Color(0xFF93C5FD)
    val OceanDarkBrand = Color(0xFF93C5FD)
    val OceanDarkOnAcc = Color(0xFF071226)
    val OceanDarkDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF0A1526), 0.52f to Color(0xFF1B4A85), 1f to Color(0xFF071020)),
    )
    val OceanDarkMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF163050), 0.45f to Color(0xFF122841), 1f to Color(0xFF0D1F33)),
    )
    val OceanDarkChartFill = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF4E92E8), 0.45f to Color(0xFF4482D4), 1f to Color(0xFF3A72BF)),
    )
    val OceanDarkChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF274460), 0.45f to Color(0xFF213A53), 1f to Color(0xFF1A3046)),
    )

    // Violet: a purple accent family.
    val VioletLightAcc = Color(0xFF7C3AED)
    val VioletLightAcc2 = Color(0xFFA78BFA)
    val VioletLightBrand = Color(0xFFA78BFA)
    val VioletLightOnAcc = Color(0xFFFFFFFF)
    val VioletLightDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF180F2A), 0.52f to Color(0xFF3C216B), 1f to Color(0xFF120A20)),
    )
    val VioletLightMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFE5DBFB), 0.45f to Color(0xFFD6C5F8), 1f to Color(0xFFC7AFF4)),
    )
    val VioletLightChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFF0E9FD), 0.45f to Color(0xFFE7DCFB), 1f to Color(0xFFDCCEF8)),
    )
    val VioletDarkAcc = Color(0xFFB79CFF)
    val VioletDarkAcc2 = Color(0xFFD8C7FF)
    val VioletDarkBrand = Color(0xFFD8C7FF)
    val VioletDarkOnAcc = Color(0xFF140B2E)
    val VioletDarkDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF170F26), 0.52f to Color(0xFF402585), 1f to Color(0xFF110A1E)),
    )
    val VioletDarkMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF2E2050), 0.45f to Color(0xFF251A41), 1f to Color(0xFF1C1433)),
    )
    val VioletDarkChartFill = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF8B6FE8), 0.45f to Color(0xFF7C5FD4), 1f to Color(0xFF6D50BF)),
    )
    val VioletDarkChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF402F5C), 0.45f to Color(0xFF37294F), 1f to Color(0xFF2C2140)),
    )
}
