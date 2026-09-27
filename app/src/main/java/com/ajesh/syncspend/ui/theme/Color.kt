package com.ajesh.syncspend.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * A monochrome base (no hue tint anywhere in ink/sub/card/line/tile/sheet/background) with one
 * gold/amber accent used purposefully — cards, charts, selections and icons — per the approved
 * redesign brief. The accent is grounded in Apple's `systemYellow` (`#FFCC00` light / `#FFD60A`
 * dark), the best-tested light/dark yellow pair available, not an arbitrary guess.
 *
 * Contrast note: unlike the old green accent, this yellow is too *light* to use directly as small
 * inline text on a light card (fails contrast). [LightPos] is therefore a darkened, still-clearly-
 * gold tone tuned for text; [DarkPos] can safely be the accent itself since light text on a dark
 * background has no such problem. See [LightPos]/[DarkPos] below.
 */
object SyncSpendPalette {

    /**
     * A mid-tone amber used only as a glyph/icon tint (the nav's active icon, the Income arrow
     * icon) — never a background or fill. One fixed value in both themes (the same structural
     * pattern the old `BrandGreen` used): light enough to read on the bottom nav's near-black
     * pill, dark enough to still read against the light gold `selectedBrush`/`sheet` surfaces the
     * Income arrow icon also sits on.
     */
    val BrandGold = Color(0xFFD4A017)

    // ---- Light: true neutral grayscale, no hue tint ----
    val LightInk = Color(0xFF141416)
    val LightSub = Color(0xFF6C7075)
    val LightCard = Color(0xFFFFFFFF)
    val LightLine = Color(0x14141416) // rgba(20,20,22,.08)
    val LightDark = Color(0xFF17181A)
    val LightAcc = Color(0xFFFFCC00)
    val LightAcc2 = Color(0xFFE6A700)
    /** Deep goldenrod, not the bright accent: readable as small text (income amounts) on a white card. */
    val LightPos = Color(0xFF8A6100)
    val LightNeg = Color(0xFFC0392B)
    val LightPill = Color(0xFFF0F1F2)
    val LightTile = Color(0xFFF1F1F2)
    val LightMink = Color(0xFF1E1A12)
    val LightMsub = Color(0x991E1A12) // rgba(30,26,18,.6)
    val LightDim = Color(0x2E141416) // rgba(20,20,22,.18)
    val LightOnAcc = Color(0xFF1A1A1A)
    val LightSheet = Color(0xFFFFFFFF)
    val LightScrEnd = Color(0xFFFAFAFA)

    /** Flat, not a gradient: the monochrome background carries no tint at all. */
    val LightScreenGradient: Brush = SolidColor(LightScrEnd)

    // A whisper of warmth (barely visible) for card elevation, not a colored background.
    val LightCardGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFFFFFFF), 0.6f to Color(0xFFFBF6E8), 1f to Color(0xFFF5EDD3)),
    )

    // Selected segment / chip / calendar-day fill and its text, and the solid "Apply"/"OK" button
    // pair now carry the gold accent (design: seg(), chip(), applyBg/applyFg) — the one deliberate
    // change from the old neutral near-black convention, matching the reference design's filled
    // gold selected pill.
    val LightSelectedBrush = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFFFCC00), 1f to Color(0xFFE6A700)),
    )
    val LightOnSelected = Color(0xFF1A1A1A)
    val LightButton = Color(0xFFFFCC00)
    val LightOnButton = Color(0xFF1A1A1A)
    // Hero card: a rich dark bronze/amber gradient, not black-and-white — cards are where the
    // accent is meant to show.
    val LightDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1A1006), 0.52f to Color(0xFF3D2B08), 1f to Color(0xFF140D04)),
    )
    val LightMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFFFF3C4), 0.45f to Color(0xFFFFE9A0), 1f to Color(0xFFFFDD7A)),
    )
    // Chart/bar fills: the gold gradient itself, and a paler take on it for de-emphasised bars.
    val LightChartFill = LightMintGradient
    val LightChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFFFF9E3), 0.45f to Color(0xFFFFF3CC), 1f to Color(0xFFFFEDB3)),
    )
    val LightNavGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF1E1F21), 1f to Color(0xFF141416)),
    )

    // ---- Dark: true neutral grayscale, no hue tint ----
    val DarkInk = Color(0xFFF1F1F2)
    val DarkSub = Color(0xFF93989E)
    val DarkCard = Color(0xFF1E1F21)
    val DarkLine = Color(0x24FFFFFF) // rgba(255,255,255,.14)
    val DarkDark = Color(0xFF0C0C0D)
    val DarkAcc = Color(0xFFFFD60A)
    val DarkAcc2 = Color(0xFFFFB800)
    /** Light text on a dark background has no contrast problem, so the accent itself works here. */
    val DarkPos = Color(0xFFFFD60A)
    val DarkNeg = Color(0xFFF08579)
    val DarkPill = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val DarkTile = Color(0xFF26282B)
    // Dark theme's chart/selected fills are bright gold (see DarkChartFill/DarkSelectedBrush below),
    // so text sitting on them still needs a dark ink — the fill's brightness decides this, not the
    // overall theme.
    val DarkMink = Color(0xFF1A1408)
    val DarkMsub = Color(0x991A1408) // rgba(26,20,8,.6)
    val DarkDim = Color(0x42FFFFFF) // rgba(255,255,255,.26)
    val DarkOnAcc = Color(0xFF1A1A1A)
    val DarkSheet = Color(0xFF1C1D1F)
    val DarkScrEnd = Color(0xFF121212)

    val DarkScreenGradient: Brush = SolidColor(DarkScrEnd)

    val DarkCardGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF242019), 0.6f to Color(0xFF1E1B16), 1f to Color(0xFF1A1712)),
    )
    val DarkDarkGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF140D04), 0.52f to Color(0xFF4A3510), 1f to Color(0xFF100A03)),
    )
    val DarkMintGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF3A2E12), 0.45f to Color(0xFF302610), 1f to Color(0xFF23190C)),
    )
    // The same gold, lifted in brightness: the card's own deep bronze would vanish on dark cards.
    val DarkChartFill = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFE8B93F), 0.45f to Color(0xFFD9A82E), 1f to Color(0xFFC99722)),
    )
    val DarkChartFillDim = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF6B551E), 0.45f to Color(0xFF5C481A), 1f to Color(0xFF4D3C15)),
    )
    val DarkNavGradient = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF26282B), 1f to Color(0xFF1A1B1D)),
    )

    val DarkSelectedBrush = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFFFFD60A), 1f to Color(0xFFFFB800)),
    )
    val DarkOnSelected = Color(0xFF1A1A1A)
    val DarkButton = Color(0xFFFFD60A)
    val DarkOnButton = Color(0xFF1A1A1A)
}
