package com.ajesh.syncspend.domain.state

import com.ajesh.syncspend.domain.model.AppLink
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The design renders Home and Transactions off one shared Component state —
 * switching the month/scope or the Expense/Income flow on either screen
 * moves the other one too. This holder (one instance, owned by
 * [com.ajesh.syncspend.di.AppContainer]) reproduces that: both screens'
 * ViewModels read and write the same [scope]/[flow], instead of each keeping
 * an independent local copy that would silently drift apart.
 */
class SharedSelectionState {
    val scope = MutableStateFlow<ScopePeriod>(ScopePeriod.Month(YearMonth.now()))
    val flow = MutableStateFlow(FlowType.EXPENSE)

    /**
     * Which transaction the Edit Entry sheet is open for (null = closed). Home's
     * recent rows and Transactions' entry rows both set this; one host at the
     * app root renders the sheet so it paints above the floating nav.
     */
    val editingTransactionId = MutableStateFlow<Long?>(null)

    /** Which transfer the Edit Transfer sheet is open for (null = closed) — the Transactions page's Savings filter sets this, never [editingTransactionId], since a transfer is not a [com.ajesh.syncspend.data.db.entity.TransactionEntity]. */
    val editingTransferId = MutableStateFlow<Long?>(null)

    /**
     * A page a tapped notification asked for. MainActivity sets it (also on a cold start, before anything
     * is composed); the navigation root follows it once and clears it.
     */
    val pendingLink = MutableStateFlow<AppLink?>(null)
}
