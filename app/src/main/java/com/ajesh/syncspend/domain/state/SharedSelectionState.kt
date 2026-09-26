package com.ajesh.syncspend.domain.state

import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransactionsTab
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

    /**
     * A tab the Transactions page should switch to as soon as it appears (Home's hero card
     * sets it to Stats). Transactions consumes and clears it, so the tab pill visibly slides.
     */
    val pendingTransactionsTab = MutableStateFlow<TransactionsTab?>(null)

    /** The Home total counts up once per app launch; this flips to true as that animation starts. */
    @Volatile var launchCountUpDone: Boolean = false
}
