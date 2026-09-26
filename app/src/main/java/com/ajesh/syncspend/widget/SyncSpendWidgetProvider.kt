package com.ajesh.syncspend.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.ajesh.syncspend.R

/**
 * Plain RemoteViews widget (no Glance): an expense-only quick-add shortcut.
 * Two layouts — money glyph only, or glyph + "Expense +" — picked from the
 * current width so it resizes cleanly between them.
 */
class SyncSpendWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        update(context, manager, appWidgetId)
    }

    private fun update(context: Context, manager: AppWidgetManager, id: Int) {
        val minWidthDp = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val layout = if (minWidthDp in 1 until WIDE_THRESHOLD_DP) R.layout.widget_small else R.layout.widget_wide
        val views = RemoteViews(context.packageName, layout)
        val open = Intent(context, QuickAddActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        manager.updateAppWidget(id, views)
    }

    private companion object {
        /** ~1 launcher cell is under this; 2+ cells is wide enough for the text. */
        const val WIDE_THRESHOLD_DP = 110
    }
}
