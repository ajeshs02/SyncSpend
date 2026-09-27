package com.ajesh.syncspend.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * A monochrome base (no hue tint anywhere in ink/sub/card/line/tile/sheet/background) with one
 * green accent used purposefully — cards, charts, selections and icons. The accent (`#D1ECCF`, a
 * pale sage/mint) and the dark-mode tinted container (`#16291C`) are colors the user extracted
 * directly from their own reference image, not a guess.
 *
 * Every token that used to carry a real multi-stop [Brush.linearGradient] (card/chart/selection
 * fills) is a flat [SolidColor] — this app renders no stylistic color gradients anywhere. The two
 * remaining `Brush.*Gradient` calls elsewhere in the app ([SyncSpendPalette] has none of its own)
 * are functional edge-fades (a scroll mask, a fading divider line), not color gradients, and are
 * left alone.
 *
 * Contrast note: `#D1ECCF` is too pale to use directly as small inline text on a white card.
 * [LightPos] is therefore a darkened, still-clearly-green tone tuned for text; [DarkPos] can
 * safely be the accent itself since it's used on the dark tinted container, not on a bright fill.
 * See [LightPos]/[DarkPos] below.
 */
object SyncSpendPalette {

    /**
     * A deep forest green used only as a glyph/icon tint (the nav's active icon, the Income arrow
     * icon) — never a background or fill. One fixed value in both themes: light enough to read on
     * the bottom nav's near-black pill, and visibly deeper than the pale [LightAcc]/[DarkAcc] fill
     * it also sits on top of (a selected chip, the segmented control), so the icon never blends
     * into its own background.
     */
    val BrandGreen = Color(0xFF2E7D4F)

    // ---- Light: true neutral grayscale, no hue tint ----
    val LightInk = Color(0xFF141416)
    val LightSub = Color(0xFF6C7075)
    val LightCard = Color(0xFFFFFFFF)
    val LightLine = Color(0x14141416) // rgba(20,20,22,.08)
    val LightDark = Color(0xFF17181A)
    val LightAcc = Color(0xFFD1ECCF)
    val LightAcc2 = Color(0xFFB8DEB3)
    /** Darkened forest green, not the pale accent: readable as small text (income amounts) on a white card. */
    val LightPos = Color(0xFF2E7D4F)
    val LightNeg = Color(0xFFC0392B)
    val LightPill = Color(0xFFF0F1F2)
    val LightTile = Color(0xFFF1F1F2)
    val LightMink = Color(0xFF2E7D4F)
    val LightMsub = Color(0x992E7D4F) // rgba(46,125,79,.6)
    val LightDim = Color(0x2E141416) // rgba(20,20,22,.18)
    val LightOnAcc = Color(0xFF1A1A1A)
    val LightSheet = Color(0xFFFFFFFF)
    val LightScrEnd = Color(0xFFFAFAFA)

    /** Flat, not a gradient: the monochrome background carries no tint at all. */
    val LightScreenGradient: Brush = SolidColor(LightScrEnd)

    /** Flat: the "Mint/Sage Green Card" tone extracted straight from the reference. */
    val LightCardGradient: Brush = SolidColor(LightAcc)

    // Selected segment / chip / calendar-day fill and its text, and the solid "Apply"/"OK" button
    // pair carry the green accent — flat, no gradient.
    val LightSelectedBrush: Brush = SolidColor(LightAcc)
    val LightOnSelected = Color(0xFF1A1A1A)
    val LightButton = LightAcc
    val LightOnButton = Color(0xFF1A1A1A)
    /** Flat: the Stats "vs previous period" comparison card is always a dark tinted panel, regardless of theme. */
    val LightDarkGradient: Brush = SolidColor(Color(0xFF16291C))
    val LightMintGradient: Brush = SolidColor(LightAcc)
    // Chart/bar fills: the green accent itself, and a paler flat tint for de-emphasised bars.
    val LightChartFill: Brush = SolidColor(LightAcc)
    val LightChartFillDim: Brush = SolidColor(Color(0xFFE9F6E6))

    // ---- Dark: true neutral grayscale, no hue tint ----
    val DarkInk = Color(0xFFF1F1F2)
    val DarkSub = Color(0xFF93989E)
    val DarkCard = Color(0xFF1E1F21)
    val DarkLine = Color(0x24FFFFFF) // rgba(255,255,255,.14)
    val DarkDark = Color(0xFF0C0C0D)
    val DarkAcc = Color(0xFFD1ECCF)
    val DarkAcc2 = Color(0xFFB8DEB3)
    /** Light text on a dark tinted container has no contrast problem, so the accent itself works here. */
    val DarkPos = Color(0xFFD1ECCF)
    val DarkNeg = Color(0xFFF08579)
    val DarkPill = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val DarkTile = Color(0xFF26282B)
    // Dark theme's chart/selected fills are still a pale, bright color that text on them needs a
    // dark ink — the fill's brightness decides this, not the overall theme.
    val DarkMink = Color(0xFF1A1A1A)
    val DarkMsub = Color(0x991A1A1A) // rgba(26,26,26,.6)
    val DarkDim = Color(0x42FFFFFF) // rgba(255,255,255,.26)
    val DarkOnAcc = Color(0xFF1A1A1A)
    val DarkSheet = Color(0xFF1C1D1F)
    val DarkScrEnd = Color(0xFF121212)

    val DarkScreenGradient: Brush = SolidColor(DarkScrEnd)

    /** The user's own dark-mode container: `#D1ECCF` blended down over a dark base. */
    val DarkCardGradient: Brush = SolidColor(Color(0xFF16291C))
    val DarkDarkGradient: Brush = SolidColor(Color(0xFF16291C))
    val DarkMintGradient: Brush = SolidColor(Color(0xFF16291C))
    val DarkChartFill: Brush = SolidColor(DarkAcc)
    val DarkChartFillDim: Brush = SolidColor(Color(0xFF23392A))

    val DarkSelectedBrush: Brush = SolidColor(DarkAcc)
    val DarkOnSelected = Color(0xFF1A1A1A)
    val DarkButton = DarkAcc
    val DarkOnButton = Color(0xFF1A1A1A)
}
