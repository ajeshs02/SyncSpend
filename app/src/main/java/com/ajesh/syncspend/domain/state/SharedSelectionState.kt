package com.ajesh.syncspend.domain.state

import com.ajesh.syncspend.domain.model.AppLink
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Home and Transactions each have their own independent date-range scope and Expense/Income flow
 * selection — [homeScope]/[homeFlow] for Home (also used by Settings' export-range default and by
 * Add Entry, which mirrors Home's own toggle), [transactionsScope]/[transactionsFlow] for
 * Transactions (also used by Stats, which shares Transactions' own scope/flow rather than Home's).
 * This holder (one instance, owned by [com.ajesh.syncspend.di.AppContainer]) still survives
 * navigation but not process death, same as before the split.
 */
class SharedSelectionState {
    val homeScope = MutableStateFlow<ScopePeriod>(ScopePeriod.Month(YearMonth.now()))
    val homeFlow = MutableStateFlow(FlowType.EXPENSE)

    val transactionsScope = MutableStateFlow<ScopePeriod>(ScopePeriod.Month(YearMonth.now()))
    val transactionsFlow = MutableStateFlow(FlowType.EXPENSE)

    /**
     * Which transaction the Edit Entry sheet is open for (null = closed). Home's
     * recent rows and Transactions' entry rows both set this; one host at the
     * app root renders the sheet so it paints above the floating nav.
     */
    val editingTransactionId = MutableStateFlow<Long?>(null)

    /**
     * A page a tapped notification asked for. MainActivity sets it (also on a cold start, before anything
     * is composed); the navigation root follows it once and clears it.
     */
    val pendingLink = MutableStateFlow<AppLink?>(null)
}
