package com.ajesh.syncspend.widget

import com.ajesh.syncspend.R
import com.ajesh.syncspend.domain.model.ColorPalette

/**
 * Mirrors the Settings "theme lab" [ColorPalette] pick onto the widget's "+" FAB — the only widget
 * element that carries an accent color; the rest of the widget stays monochrome (see the 4
 * `colors.xml` variants). RemoteViews can't read the app's Compose theme, so this is a small,
 * separate lookup rather than a shared one.
 */
internal object WidgetPalette {
    /** Forest reuses the existing, light/dark/Material-You-aware `widget_fab_bg` — no change from before this picker existed. */
    fun fabBackgroundRes(palette: ColorPalette): Int = when (palette) {
        ColorPalette.FOREST -> R.drawable.widget_fab_bg
        ColorPalette.NEO_MINT -> R.drawable.widget_fab_bg_neo_mint
        ColorPalette.OCEAN -> R.drawable.widget_fab_bg_ocean
        ColorPalette.VIOLET -> R.drawable.widget_fab_bg_violet
    }

    /** Null for Forest: its "+" glyph keeps the XML-declared, light/dark/Material-You-aware `widget_on_fab` tint. */
    fun onFabColor(palette: ColorPalette): Int? = when (palette) {
        ColorPalette.FOREST -> null
        ColorPalette.NEO_MINT -> 0xFF04140D.toInt()
        ColorPalette.OCEAN -> 0xFF071226.toInt()
        ColorPalette.VIOLET -> 0xFF140B2E.toInt()
    }
}
