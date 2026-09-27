package com.ajesh.syncspend.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ajesh.syncspend.domain.model.ColorPalette

/**
 * Just the accent-bearing slice of [SyncSpendColors] — everything a [ColorPalette] choice can
 * change. Neutrals (ink/sub/card/line/tile/sheet/scrEnd/selectedBrush/button/...) stay identical
 * across every palette; only these fields differ, and [SyncSpendTheme] layers them onto the
 * shared light/dark base via `copy(...)`.
 */
internal data class AccentTokens(
    val acc: Color,
    val acc2: Color,
    val brand: Color,
    val onAcc: Color,
    val darkGradient: Brush,
    val mintGradient: Brush,
    val chartFill: Brush,
    val chartFillDim: Brush,
)

internal fun accentsFor(palette: ColorPalette, dark: Boolean): AccentTokens = when (palette) {
    ColorPalette.FOREST -> if (dark) {
        AccentTokens(
            acc = SyncSpendPalette.DarkAcc,
            acc2 = SyncSpendPalette.DarkAcc2,
            brand = SyncSpendPalette.BrandGreen,
            onAcc = SyncSpendPalette.DarkOnAcc,
            darkGradient = SyncSpendPalette.DarkDarkGradient,
            mintGradient = SyncSpendPalette.DarkMintGradient,
            chartFill = SyncSpendPalette.DarkChartFill,
            chartFillDim = SyncSpendPalette.DarkChartFillDim,
        )
    } else {
        AccentTokens(
            acc = SyncSpendPalette.LightAcc,
            acc2 = SyncSpendPalette.LightAcc2,
            brand = SyncSpendPalette.BrandGreen,
            onAcc = SyncSpendPalette.LightOnAcc,
            darkGradient = SyncSpendPalette.LightDarkGradient,
            mintGradient = SyncSpendPalette.LightMintGradient,
            chartFill = SyncSpendPalette.LightChartFill,
            chartFillDim = SyncSpendPalette.LightChartFillDim,
        )
    }
    ColorPalette.NEO_MINT -> if (dark) {
        AccentTokens(
            acc = SyncSpendPalette.NeoMintDarkAcc,
            acc2 = SyncSpendPalette.NeoMintDarkAcc2,
            brand = SyncSpendPalette.NeoMintDarkBrand,
            onAcc = SyncSpendPalette.NeoMintDarkOnAcc,
            darkGradient = SyncSpendPalette.NeoMintDarkDarkGradient,
            mintGradient = SyncSpendPalette.NeoMintDarkMintGradient,
            chartFill = SyncSpendPalette.NeoMintDarkChartFill,
            chartFillDim = SyncSpendPalette.NeoMintDarkChartFillDim,
        )
    } else {
        AccentTokens(
            acc = SyncSpendPalette.NeoMintLightAcc,
            acc2 = SyncSpendPalette.NeoMintLightAcc2,
            brand = SyncSpendPalette.NeoMintLightBrand,
            onAcc = SyncSpendPalette.NeoMintLightOnAcc,
            darkGradient = SyncSpendPalette.NeoMintLightDarkGradient,
            mintGradient = SyncSpendPalette.NeoMintLightMintGradient,
            chartFill = SyncSpendPalette.NeoMintLightMintGradient,
            chartFillDim = SyncSpendPalette.NeoMintLightChartFillDim,
        )
    }
    ColorPalette.OCEAN -> if (dark) {
        AccentTokens(
            acc = SyncSpendPalette.OceanDarkAcc,
            acc2 = SyncSpendPalette.OceanDarkAcc2,
            brand = SyncSpendPalette.OceanDarkBrand,
            onAcc = SyncSpendPalette.OceanDarkOnAcc,
            darkGradient = SyncSpendPalette.OceanDarkDarkGradient,
            mintGradient = SyncSpendPalette.OceanDarkMintGradient,
            chartFill = SyncSpendPalette.OceanDarkChartFill,
            chartFillDim = SyncSpendPalette.OceanDarkChartFillDim,
        )
    } else {
        AccentTokens(
            acc = SyncSpendPalette.OceanLightAcc,
            acc2 = SyncSpendPalette.OceanLightAcc2,
            brand = SyncSpendPalette.OceanLightBrand,
            onAcc = SyncSpendPalette.OceanLightOnAcc,
            darkGradient = SyncSpendPalette.OceanLightDarkGradient,
            mintGradient = SyncSpendPalette.OceanLightMintGradient,
            chartFill = SyncSpendPalette.OceanLightMintGradient,
            chartFillDim = SyncSpendPalette.OceanLightChartFillDim,
        )
    }
    ColorPalette.VIOLET -> if (dark) {
        AccentTokens(
            acc = SyncSpendPalette.VioletDarkAcc,
            acc2 = SyncSpendPalette.VioletDarkAcc2,
            brand = SyncSpendPalette.VioletDarkBrand,
            onAcc = SyncSpendPalette.VioletDarkOnAcc,
            darkGradient = SyncSpendPalette.VioletDarkDarkGradient,
            mintGradient = SyncSpendPalette.VioletDarkMintGradient,
            chartFill = SyncSpendPalette.VioletDarkChartFill,
            chartFillDim = SyncSpendPalette.VioletDarkChartFillDim,
        )
    } else {
        AccentTokens(
            acc = SyncSpendPalette.VioletLightAcc,
            acc2 = SyncSpendPalette.VioletLightAcc2,
            brand = SyncSpendPalette.VioletLightBrand,
            onAcc = SyncSpendPalette.VioletLightOnAcc,
            darkGradient = SyncSpendPalette.VioletLightDarkGradient,
            mintGradient = SyncSpendPalette.VioletLightMintGradient,
            chartFill = SyncSpendPalette.VioletLightMintGradient,
            chartFillDim = SyncSpendPalette.VioletLightChartFillDim,
        )
    }
}

/** A palette's accent color at a glance, for the Settings picker's swatch dot — always the light-mode accent. */
internal fun swatchFor(palette: ColorPalette): Color = accentsFor(palette, dark = false).acc
