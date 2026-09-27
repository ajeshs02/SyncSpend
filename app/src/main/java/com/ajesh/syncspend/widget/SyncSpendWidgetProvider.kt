package com.ajesh.syncspend.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.ajesh.syncspend.R
import com.ajesh.syncspend.domain.model.ColorPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Plain RemoteViews widget (no Glance): a live "today's expense" tile that doubles as an
 * expense-only quick-add shortcut. Two layouts — compact amount only, or a caption + amount —
 * picked from the current width so it resizes cleanly between them.
 */
class SyncSpendWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = refresh(context)

    /** [goAsync] extends this receiver's lifetime past `onReceive` so the suspend DB read is safe to await. */
    private fun refresh(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetRefresher.refreshNow(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** ~1 launcher cell is under this; 2+ cells is wide enough for the caption. */
        private const val WIDE_THRESHOLD_DP = 110

        /** Builds and pushes one widget instance's RemoteViews, showing [amountText] with the [palette]'s FAB accent. */
        fun push(context: Context, manager: AppWidgetManager, id: Int, amountText: String, palette: ColorPalette) {
            val minWidthDp = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val layout = if (minWidthDp in 1 until WIDE_THRESHOLD_DP) R.layout.widget_small else R.layout.widget_wide
            val views = RemoteViews(context.packageName, layout)
            views.setTextViewText(R.id.widget_amount, amountText)
            views.setInt(R.id.widget_fab, "setBackgroundResource", WidgetPalette.fabBackgroundRes(palette))
            WidgetPalette.onFabColor(palette)?.let { views.setInt(R.id.widget_fab_icon, "setColorFilter", it) }
            val open = Intent(context, QuickAddActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
            manager.updateAppWidget(id, views)
        }
    }
}
