package com.ajesh.syncspend.domain.model

/**
 * The accent color family layered onto the app's shared monochrome base (see
 * [com.ajesh.syncspend.ui.theme.accentsFor]). Persisted in DataStore.
 *
 * Temporary "theme lab" option surfaced in Settings so the user can audition
 * combinations before one is finalized — see the Settings screen's palette picker.
 */
enum class ColorPalette(val label: String) {
    FOREST("Forest"),
    NEO_MINT("Neo Mint"),
    OCEAN("Ocean"),
    VIOLET("Violet"),
}
