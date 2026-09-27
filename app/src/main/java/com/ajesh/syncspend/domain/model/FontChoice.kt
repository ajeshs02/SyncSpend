package com.ajesh.syncspend.domain.model

/**
 * The bundled typeface family used everywhere text renders — Compose screens and the
 * home-screen widget alike. Persisted in DataStore.
 *
 * Temporary "theme lab" option surfaced in Settings so the user can audition
 * combinations before one is finalized — see the Settings screen's font picker.
 */
enum class FontChoice(val label: String) {
    ARCHIVO("Archivo"),
    INTER("Inter"),
    MANROPE("Manrope"),
    JAKARTA("Plus Jakarta Sans"),
    OUTFIT("Outfit"),
}
