package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Clears text-field focus and hides the system IME. Call this before opening any sheet/dialog
 * (category picker, date picker, the custom amount keypad) so the system keyboard never lingers
 * behind it or collides with a custom input surface. Instant — no wait for the IME's own close
 * animation, which is fine for the amount keypad (its layout never depended on IME visibility) but
 * leaves a popup's height visibly jumping if one is about to open; see [rememberDismissKeyboardThen]
 * for that case.
 */
@Composable
fun rememberKeyboardDismisser(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return { focusManager.clearFocus(); keyboard?.hide() }
}

/**
 * Dismisses the keyboard, then runs [action] (typically opening a category/date popup) only once the
 * IME has genuinely, stably finished closing.
 *
 * Why this isn't just "wait, then open": the popups this guards are Material3 `ModalBottomSheet`s
 * (see `DesignSheet.kt`), and `ModalBottomSheet` creates a **brand new Android window** every time it's
 * shown, sizing itself from an internal, non-overridable `imePadding()` inside *that* window. A plain
 * boolean "is the keyboard visible" check in the host window can flip to false a frame or two before
 * the real on-screen close animation has actually finished, which is exactly the gap that let the new
 * sheet's window briefly measure itself against a shrunken height and visibly "jump" once it corrects.
 * So this waits on the raw [WindowInsets.ime] inset reaching **and holding** exactly zero across two
 * real composition frames, not a one-shot boolean — the only lever available from application code,
 * since the second window's own inset timeline can't be observed before it exists.
 *
 * - One coroutine is launched per trigger (via [rememberCoroutineScope]), and it captures [action] in
 *   its own closure — never in a `mutableState` a later recomposition could overwrite — so the pending
 *   action can't be silently lost.
 * - [kotlinx.coroutines.flow] + `snapshotFlow` is used to observe [WindowInsets.ime] reactively, not a
 *   one-off read — a bare read inside a coroutine would never see the inset continue animating.
 * - A second trigger before the first resolves cancels the outstanding job outright, so rapid
 *   double-taps across two different fields can never open two stacked popups — only the most recent
 *   request ever reaches `action()`.
 * - The wait is bounded (`withTimeoutOrNull(600)`): if the inset never reports a stable zero, [action]
 *   still runs after 600ms rather than blocking the UI forever. The whole coroutine lives in the
 *   calling composable's own [rememberCoroutineScope], which Compose cancels automatically when that
 *   composable leaves composition — navigating away before it fires means it simply never fires.
 * - If the keyboard was already closed, the loop below resolves within a couple of frame callbacks —
 *   no perceptible wait in the common case.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberDismissKeyboardThen(): (() -> Unit) -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    // WindowInsets.ime's getter is @Composable (it looks up the current insets holder), but the
    // WindowInsets instance it returns is a live, mutable-backed value holder whose own getBottom(...)
    // is a plain function — so it's captured once here, in composable scope, then safely read from the
    // coroutine below as the IME inset actually changes over time.
    val imeInsets = WindowInsets.ime
    val scope = rememberCoroutineScope()
    var pendingJob by remember { mutableStateOf<Job?>(null) }

    return { action ->
        focusManager.clearFocus()
        keyboard?.hide()
        pendingJob?.cancel()
        pendingJob = scope.launch {
            withTimeoutOrNull(600) {
                while (true) {
                    snapshotFlow { imeInsets.getBottom(density) }.first { it == 0 }
                    withFrameNanos {}
                    if (imeInsets.getBottom(density) != 0) continue // bounced back — re-wait
                    withFrameNanos {}
                    if (imeInsets.getBottom(density) != 0) continue
                    break // zero, and held for two real frames
                }
            }
            action()
        }
    }
}
