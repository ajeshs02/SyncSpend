package com.ajesh.syncspend.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * A monochrome base (no hue tint anywhere in ink/sub/card/line/tile/sheet/background) with one
 * green accent used purposefully — cards, charts, selections and icons. [Accent] (`#D1ECCF`, a
 * pale sage/mint the user extracted directly from their own reference image) is flat: the exact
 * same value in light and dark theme, everywhere it's used — the Expense/Income toggle proved this
 * reads better than the dark-tinted-container variant an earlier round tried for cards.
 *
 * Every token that used to carry a real multi-stop [Brush.linearGradient] (card/chart/selection
 * fills) is a flat [SolidColor] — this app renders no stylistic color gradients anywhere. The two
 * remaining `Brush.*Gradient` calls elsewhere in the app ([SyncSpendPalette] has none of its own)
 * are functional edge-fades (a scroll mask, a fading divider line), not color gradients, and are
 * left alone.
 *
 * Contrast note: [Accent] is too pale to use directly as small inline text on a white card.
 * [LightPos] is therefore a darkened, still-clearly-green tone tuned for text; [DarkPos] stays
 * legible because it's only ever used on the app's plain dark card/sheet surfaces, never on the
 * (also pale) accent fill itself — content sitting on the accent fill uses [LightMink]/[DarkMink]
 * instead. See [LightPos]/[DarkPos] and [LightMink]/[DarkMink] below.
 */
object SyncSpendPalette {

    /** The single accent color, identical in both themes — cards, charts, selections, icons. */
    val Accent = Color(0xFFD1ECCF)

    /**
     * A deep forest green used only as a glyph/icon tint (the Income arrow icon) — never a
     * background or fill. One fixed value in both themes: visibly deeper than the pale
     * [LightAcc]/[DarkAcc] fill it also sits on top of (a selected chip, the segmented control),
     * so the icon never blends into its own background.
     */
    val BrandGreen = Color(0xFF2E7D4F)

    // ---- Light: true neutral grayscale, no hue tint ----
    val LightInk = Color(0xFF141416)
    val LightSub = Color(0xFF6C7075)
    val LightCard = Color(0xFFFFFFFF)
    val LightLine = Color(0x14141416) // rgba(20,20,22,.08)
    val LightDark = Color(0xFF17181A)
    val LightAcc = Accent
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
    /** Flat: the Stats "vs previous period" comparison card is always a neutral dark panel, regardless of theme. */
    val LightDarkGradient: Brush = SolidColor(LightDark)
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
    val DarkAcc = Accent
    val DarkAcc2 = Color(0xFFB8DEB3)
    /** Light text on the app's plain dark card/sheet surfaces has no contrast problem. */
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

    /** Flat: the same accent as light theme, not a darkened container — matches the toggle's fill. */
    val DarkCardGradient: Brush = SolidColor(DarkAcc)
    /** Flat: the Stats "vs previous period" comparison card is always a neutral dark panel, regardless of theme. */
    val DarkDarkGradient: Brush = SolidColor(DarkDark)
    val DarkMintGradient: Brush = SolidColor(DarkAcc)
    val DarkChartFill: Brush = SolidColor(DarkAcc)
    val DarkChartFillDim: Brush = SolidColor(Color(0xFF23392A))

    val DarkSelectedBrush: Brush = SolidColor(DarkAcc)
    val DarkOnSelected = Color(0xFF1A1A1A)
    val DarkButton = DarkAcc
    val DarkOnButton = Color(0xFF1A1A1A)
}
