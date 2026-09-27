package com.ajesh.syncspend.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * A monochrome base (no hue tint anywhere in ink/sub/card/line/tile/sheet/background) with one
 * blue accent used purposefully — cards, charts, selections and icons. The accent is grounded in
 * Apple's `systemBlue` (`#007AFF` light / `#0A84FF` dark), the best-tested light/dark blue pair
 * available, not an arbitrary guess.
 *
 * Every token that used to carry a real multi-stop [Brush.linearGradient] (card/chart/selection
 * fills) is now a flat [SolidColor] — this app renders no stylistic color gradients anywhere.
 * The two remaining `Brush.*Gradient` calls elsewhere in the app ([SyncSpendPalette] has none of
 * its own) are functional edge-fades (a scroll mask, a fading divider line), not color gradients,
 * and are left alone.
 *
 * Contrast note: like the old gold accent, this blue is a touch too light to use directly as
 * small inline text on a light card (fails contrast). [LightPos] is therefore a darkened,
 * still-clearly-blue tone tuned for text; [DarkPos] can safely be the accent itself since light
 * text on a dark background has no such problem. See [LightPos]/[DarkPos] below.
 */
object SyncSpendPalette {

    /**
     * A deeper mid-tone blue used only as a glyph/icon tint (the nav's active icon, the Income
     * arrow icon) — never a background or fill. One fixed value in both themes: light enough to
     * read on the bottom nav's near-black pill, and visibly deeper than the bright [LightAcc]/
     * [DarkAcc] fill it also sits on top of (a selected chip, the segmented control), so the icon
     * never blends into its own background.
     */
    val BrandBlue = Color(0xFF1565C0)

    // ---- Light: true neutral grayscale, no hue tint ----
    val LightInk = Color(0xFF141416)
    val LightSub = Color(0xFF6C7075)
    val LightCard = Color(0xFFFFFFFF)
    val LightLine = Color(0x14141416) // rgba(20,20,22,.08)
    val LightDark = Color(0xFF17181A)
    val LightAcc = Color(0xFF007AFF)
    val LightAcc2 = Color(0xFF0059C7)
    /** Darkened blue, not the bright accent: readable as small text (income amounts) on a white card. */
    val LightPos = Color(0xFF0059C7)
    val LightNeg = Color(0xFFC0392B)
    val LightPill = Color(0xFFF0F1F2)
    val LightTile = Color(0xFFF1F1F2)
    val LightMink = Color(0xFF0B1E33)
    val LightMsub = Color(0x990B1E33) // rgba(11,30,51,.6)
    val LightDim = Color(0x2E141416) // rgba(20,20,22,.18)
    val LightOnAcc = Color(0xFF1A1A1A)
    val LightSheet = Color(0xFFFFFFFF)
    val LightScrEnd = Color(0xFFFAFAFA)

    /** Flat, not a gradient: the monochrome background carries no tint at all. */
    val LightScreenGradient: Brush = SolidColor(LightScrEnd)

    /** Flat: a whisper of blue tint for "highlighted" cards, not a gradient. */
    val LightCardGradient: Brush = SolidColor(Color(0xFFF2F7FF))

    // Selected segment / chip / calendar-day fill and its text, and the solid "Apply"/"OK" button
    // pair carry the blue accent — flat now, no gradient.
    val LightSelectedBrush: Brush = SolidColor(LightAcc)
    val LightOnSelected = Color(0xFF1A1A1A)
    val LightButton = LightAcc
    val LightOnButton = Color(0xFF1A1A1A)
    /** Flat: the Stats "vs previous period" comparison card is always a dark panel, regardless of theme. */
    val LightDarkGradient: Brush = SolidColor(Color(0xFF121A26))
    val LightMintGradient: Brush = SolidColor(Color(0xFFE3F0FF))
    // Chart/bar fills: the blue accent itself, and a paler flat blue for de-emphasised bars.
    val LightChartFill: Brush = SolidColor(LightAcc)
    val LightChartFillDim: Brush = SolidColor(Color(0xFFB3D7FF))

    // ---- Dark: true neutral grayscale, no hue tint ----
    val DarkInk = Color(0xFFF1F1F2)
    val DarkSub = Color(0xFF93989E)
    val DarkCard = Color(0xFF1E1F21)
    val DarkLine = Color(0x24FFFFFF) // rgba(255,255,255,.14)
    val DarkDark = Color(0xFF0C0C0D)
    val DarkAcc = Color(0xFF0A84FF)
    val DarkAcc2 = Color(0xFF409CFF)
    /** Light text on a dark background has no contrast problem, so the accent itself works here. */
    val DarkPos = Color(0xFF0A84FF)
    val DarkNeg = Color(0xFFF08579)
    val DarkPill = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val DarkTile = Color(0xFF26282B)
    // Dark theme's chart/selected fills are still a bright-enough blue that text on them needs a
    // dark ink — the fill's brightness decides this, not the overall theme.
    val DarkMink = Color(0xFF0A1622)
    val DarkMsub = Color(0x990A1622) // rgba(10,22,34,.6)
    val DarkDim = Color(0x42FFFFFF) // rgba(255,255,255,.26)
    val DarkOnAcc = Color(0xFF1A1A1A)
    val DarkSheet = Color(0xFF1C1D1F)
    val DarkScrEnd = Color(0xFF121212)

    val DarkScreenGradient: Brush = SolidColor(DarkScrEnd)

    val DarkCardGradient: Brush = SolidColor(Color(0xFF19222E))
    val DarkDarkGradient: Brush = SolidColor(Color(0xFF0E141F))
    val DarkMintGradient: Brush = SolidColor(Color(0xFF1E2D3F))
    val DarkChartFill: Brush = SolidColor(DarkAcc)
    val DarkChartFillDim: Brush = SolidColor(Color(0xFF3A5A7A))

    val DarkSelectedBrush: Brush = SolidColor(DarkAcc)
    val DarkOnSelected = Color(0xFF1A1A1A)
    val DarkButton = DarkAcc
    val DarkOnButton = Color(0xFF1A1A1A)

    // ---- Home hero card: a debit-card-style fill, fixed regardless of theme ----
    /** Deep enough that white text/icons clear contrast on it directly (unlike the bright accent). */
    val HeroGreen = Color(0xFF1B5E44)
    /** Bright systemGreen, used only for small highlight details (chip icon, brand mark) on top of [HeroGreen]. */
    val HeroGreenAccent = Color(0xFF34C759)
    val HeroOnGreen = Color(0xFFFFFFFF)
}
