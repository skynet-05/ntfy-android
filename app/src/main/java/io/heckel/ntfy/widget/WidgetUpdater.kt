package io.heckel.ntfy.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import io.heckel.ntfy.R

object WidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, NotificationsWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) {
            return
        }
        for (widgetId in widgetIds) {
            NotificationsWidgetProvider.updateAppWidget(appContext, manager, widgetId)
            manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
        }
    }
}
