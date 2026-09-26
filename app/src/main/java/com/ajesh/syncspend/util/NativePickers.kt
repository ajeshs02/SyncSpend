package com.ajesh.syncspend.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import com.ajesh.syncspend.R
import java.time.LocalDate

/**
 * Platform (native Android) date/time picker dialogs. Used wherever the spec
 * calls for the native picker: Add Entry's date key, and the reminder /
 * subscription time & date fields. Themed to the app accent; light/dark is
 * chosen from the in-app theme, not the system, so it always matches.
 */
object NativePickers {
    private fun themeRes(dark: Boolean) =
        if (dark) R.style.Theme_SyncSpend_PickerDark else R.style.Theme_SyncSpend_PickerLight

    fun showDate(context: Context, initial: LocalDate, dark: Boolean, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            themeRes(dark),
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        ).show()
    }

    fun showTime(context: Context, minuteOfDay: Int, dark: Boolean, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            themeRes(dark),
            { _, h, m -> onPicked(h * 60 + m) },
            minuteOfDay / 60,
            minuteOfDay % 60,
            false,
        ).show()
    }
}
