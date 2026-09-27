package com.ajesh.syncspend.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.ajesh.syncspend.SyncSpendApp
import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.util.CurrencyFormatter
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reads today's expense total and pushes it into every placed widget instance. Callable both as a
 * fire-and-forget request (after any transaction change — a plain background launch is enough
 * since the app process is already alive) and, via [refreshNow], from the widget's own lifecycle
 * callbacks (which need to wrap it in `goAsync()` instead).
 */
object WidgetRefresher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdate(context: Context) {
        scope.launch { refreshNow(context.applicationContext) }
    }

    suspend fun refreshNow(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SyncSpendWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val container = (context.applicationContext as SyncSpendApp).container
        val today = LocalDate.now()
        val total = container.transactionRepository.getAllOnce()
            .filter { it.date == today && it.amount < 0 }
            .sumOf { -it.amount }
        val symbol = container.preferencesRepository.current()?.currencyCode?.symbol ?: CurrencyCode.INR.symbol
        val amountText = symbol + CurrencyFormatter.amount(total)
        ids.forEach { id -> SyncSpendWidgetProvider.push(context, manager, id, amountText) }
    }
}
