// Root build file. Declares plugin versions once so ":app" applies them
// without repeating a version number.
// AGP 9+ compiles Kotlin itself ("built-in Kotlin") — the separate
// org.jetbrains.kotlin.android plugin is not applied at all.
plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
}
