package com.ajesh.syncspend.domain.model

import java.time.YearMonth

/** Home's "Month/Year/All-time" scope selector and Transactions' scope are the
 * same underlying selection in the design (one shared Component state), so
 * this is read/written through [com.ajesh.syncspend.domain.state.SharedSelectionState]
 * rather than being screen-local. */
sealed class ScopePeriod {
    data class Month(val yearMonth: YearMonth) : ScopePeriod()
    data class Year(val year: Int) : ScopePeriod()
    data object AllTime : ScopePeriod()
}
