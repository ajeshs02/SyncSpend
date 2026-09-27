package com.ajesh.syncspend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ajesh.syncspend.domain.model.ThemeMode

/**
 * The design's semantic tokens (ink/sub/card/tile/acc/pos/neg/...), as a single
 * immutable holder so screens read `SyncSpendTheme.colors.acc` etc. instead of
 * branching on isSystemInDarkTheme() everywhere. Mirrors the `--ink`, `--sub`,
 * ... CSS custom properties in the design 1:1.
 */
@androidx.compose.runtime.Immutable
data class SyncSpendColors(
    val ink: Color,
    val sub: Color,
    val card: Color,
    val cardGradient: Brush,
    val line: Color,
    val dark: Color,
    val darkGradient: Brush,
    val mintGradient: Brush,
    /** Gold fill for bars, share bars, dots and chips: the mint gradient (lifted in dark mode). */
    val chartFill: Brush,
    /** A paler [chartFill] for de-emphasised bars and projections. */
    val chartFillDim: Brush,
    val acc: Color,
    val acc2: Color,
    val pos: Color,
    val neg: Color,
    val pill: Color,
    val tile: Color,
    val navGradient: Brush,
    val mink: Color,
    val msub: Color,
    val dim: Color,
    val onAcc: Color,
    val sheet: Color,
    val screenGradient: Brush,
    val scrEnd: Color,
    val selectedBrush: Brush,
    val onSelected: Color,
    val button: Color,
    val onButton: Color,
    /** Expense arrow tint when sitting on the selected (inverted) indicator. */
    val expenseOnSelected: Color,
    /** [SyncSpendPalette.BrandGold]: for the nav's selected icon and Income arrow icons only, never a fill. */
    val brand: Color,
    val isDark: Boolean,
)

private val LightColors = SyncSpendColors(
    ink = SyncSpendPalette.LightInk,
    sub = SyncSpendPalette.LightSub,
    card = SyncSpendPalette.LightCard,
    cardGradient = SyncSpendPalette.LightCardGradient,
    line = SyncSpendPalette.LightLine,
    dark = SyncSpendPalette.LightDark,
    darkGradient = SyncSpendPalette.LightDarkGradient,
    mintGradient = SyncSpendPalette.LightMintGradient,
    chartFill = SyncSpendPalette.LightChartFill,
    chartFillDim = SyncSpendPalette.LightChartFillDim,
    acc = SyncSpendPalette.LightAcc,
    acc2 = SyncSpendPalette.LightAcc2,
    pos = SyncSpendPalette.LightPos,
    neg = SyncSpendPalette.LightNeg,
    pill = SyncSpendPalette.LightPill,
    tile = SyncSpendPalette.LightTile,
    navGradient = SyncSpendPalette.LightNavGradient,
    mink = SyncSpendPalette.LightMink,
    msub = SyncSpendPalette.LightMsub,
    dim = SyncSpendPalette.LightDim,
    onAcc = SyncSpendPalette.LightOnAcc,
    sheet = SyncSpendPalette.LightSheet,
    screenGradient = SyncSpendPalette.LightScreenGradient,
    scrEnd = SyncSpendPalette.LightScrEnd,
    selectedBrush = SyncSpendPalette.LightSelectedBrush,
    onSelected = SyncSpendPalette.LightOnSelected,
    button = SyncSpendPalette.LightButton,
    onButton = SyncSpendPalette.LightOnButton,
    // Both themes' selectedBrush is now light gold, so the red that reads on it is the same in
    // both themes too (previously this flipped between a lighter/darker red to match a near-black
    // vs near-white selected background — that inversion no longer applies).
    expenseOnSelected = Color(0xFFC0392B),
    brand = SyncSpendPalette.BrandGold,
    isDark = false,
)

private val DarkColors = SyncSpendColors(
    ink = SyncSpendPalette.DarkInk,
    sub = SyncSpendPalette.DarkSub,
    card = SyncSpendPalette.DarkCard,
    cardGradient = SyncSpendPalette.DarkCardGradient,
    line = SyncSpendPalette.DarkLine,
    dark = SyncSpendPalette.DarkDark,
    darkGradient = SyncSpendPalette.DarkDarkGradient,
    mintGradient = SyncSpendPalette.DarkMintGradient,
    chartFill = SyncSpendPalette.DarkChartFill,
    chartFillDim = SyncSpendPalette.DarkChartFillDim,
    acc = SyncSpendPalette.DarkAcc,
    acc2 = SyncSpendPalette.DarkAcc2,
    pos = SyncSpendPalette.DarkPos,
    neg = SyncSpendPalette.DarkNeg,
    pill = SyncSpendPalette.DarkPill,
    tile = SyncSpendPalette.DarkTile,
    navGradient = SyncSpendPalette.DarkNavGradient,
    mink = SyncSpendPalette.DarkMink,
    msub = SyncSpendPalette.DarkMsub,
    dim = SyncSpendPalette.DarkDim,
    onAcc = SyncSpendPalette.DarkOnAcc,
    sheet = SyncSpendPalette.DarkSheet,
    screenGradient = SyncSpendPalette.DarkScreenGradient,
    scrEnd = SyncSpendPalette.DarkScrEnd,
    selectedBrush = SyncSpendPalette.DarkSelectedBrush,
    onSelected = SyncSpendPalette.DarkOnSelected,
    button = SyncSpendPalette.DarkButton,
    onButton = SyncSpendPalette.DarkOnButton,
    expenseOnSelected = Color(0xFFC0392B),
    brand = SyncSpendPalette.BrandGold,
    isDark = true,
)

private val LocalSyncSpendColors = staticCompositionLocalOf { LightColors }

object SyncSpendTheme {
    val colors: SyncSpendColors
        @Composable
        get() = LocalSyncSpendColors.current
}

@Composable
fun SyncSpendTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = if (dark) DarkColors else LightColors

    // A Material3 scheme is still provided underneath so stock M3 components
    // (ripples, text selection handles, default TextField colors before we
    // style them) look coherent rather than defaulting to purple.
    val materialScheme = if (dark) {
        darkColorScheme(
            primary = tokens.acc,
            onPrimary = tokens.onAcc,
            background = tokens.scrEnd,
            onBackground = tokens.ink,
            surface = tokens.card,
            onSurface = tokens.ink,
            error = tokens.neg,
        )
    } else {
        lightColorScheme(
            primary = tokens.acc,
            onPrimary = tokens.onAcc,
            background = tokens.scrEnd,
            onBackground = tokens.ink,
            surface = tokens.card,
            onSurface = tokens.ink,
            error = tokens.neg,
        )
    }

    CompositionLocalProvider(LocalSyncSpendColors provides tokens) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = SyncSpendTypography,
            shapes = SyncSpendShapes,
            content = content,
        )
    }
}
