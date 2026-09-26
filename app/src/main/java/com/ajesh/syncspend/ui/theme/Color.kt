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
    val LightScrEnd = Color(0xFFDCEADE)

    private val lightScrStops = arrayOf(
        0.00f to Color(0xFFD7E8DA),
        0.07f to Color(0xFFEAF2E8),
        0.15f to Color(0xFFFAFCF9),
        0.85f to Color(0xFFFAFCF9),
        0.93f to Color(0xFFEAF2E8),
        1.00f to Color(0xFFDCEADE),
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
    val LightNavGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF171D18), 1f to Color(0xFF0D120E)),
    )

    // ---- Dark ----
    val DarkInk = Color(0xFFEEF3EA)
    val DarkSub = Color(0xFF868F82)
    val DarkCard = Color(0xFF1A211B)
    val DarkLine = Color(0x24FFFFFF) // rgba(255,255,255,.14)
    val DarkDark = Color(0xFF070A08)
    val DarkAcc = Color(0xFF7FD39A)
    val DarkAcc2 = Color(0xFF5FBF7D)
    val DarkPos = Color(0xFF7FD39A)
    val DarkNeg = Color(0xFFF08579)
    val DarkPill = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val DarkTile = Color(0xFF252D26)
    val DarkMink = Color(0xFFE8F4EA)
    val DarkMsub = Color(0xADE8F4EA) // rgba(232,244,234,.68)
    val DarkDim = Color(0x42FFFFFF) // rgba(255,255,255,.26)
    val DarkOnAcc = Color(0xFF07120B)
    val DarkSheet = Color(0xFF1B231C)
    val DarkScrEnd = Color(0xFF16301E)

    private val darkScrStops = arrayOf(
        0.00f to Color(0xFF16301E),
        0.07f to Color(0xFF101C13),
        0.15f to Color(0xFF0E130F),
        0.85f to Color(0xFF0E130F),
        0.93f to Color(0xFF101C13),
        1.00f to Color(0xFF16301E),
    )
    val DarkScreenGradient = Brush.verticalGradient(colorStops = darkScrStops)

    val DarkCardGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1F271F), 0.6f to Color(0xFF1A211B), 1f to Color(0xFF161C17)),
    )
    val DarkDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF0B140E), 0.52f to Color(0xFF153A22), 1f to Color(0xFF08100A)),
    )
    val DarkMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1C3A25), 0.45f to Color(0xFF16301D), 1f to Color(0xFF102316)),
    )
    val DarkNavGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1D251E), 1f to Color(0xFF10160F)),
    )

    val DarkSelectedBrush = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFF2F6EE), 1f to Color(0xFFDFE7DA)),
    )
    val DarkOnSelected = Color(0xFF101710)
    val DarkButton = Color(0xFFF2F6EE)
    val DarkOnButton = Color(0xFF101710)
}
