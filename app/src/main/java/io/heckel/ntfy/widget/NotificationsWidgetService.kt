package io.heckel.ntfy.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.heckel.ntfy.R
import io.heckel.ntfy.util.Log

class NotificationsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return NotificationsRemoteViewsFactory(applicationContext, appWidgetId)
    }
}

private class NotificationsRemoteViewsFactory(
    private val context: android.content.Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetListItem> = emptyList()

    override fun onCreate() {
        // Nothing
    }

    override fun onDataSetChanged() {
        try {
            items = WidgetDataProvider.loadItems(context, appWidgetId)
        } catch (e: Exception) {
            Log.w(TAG, "Widget list load failed for id=$appWidgetId: ${e.message}", e)
            items = emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return loadingRow()
        }
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_notifications_item)
        try {
            views.setTextViewText(R.id.widget_item_title, item.title)
            views.setTextViewText(R.id.widget_item_topic, item.topicLabel)
            views.setTextViewText(R.id.widget_item_time, item.timeLabel)

            if (item.priorityIconRes != 0) {
                views.setViewVisibility(R.id.widget_item_priority, android.view.View.VISIBLE)
                views.setImageViewResource(R.id.widget_item_priority, item.priorityIconRes)
            } else {
                views.setViewVisibility(R.id.widget_item_priority, android.view.View.GONE)
            }

            val fillInIntent = Intent().apply {
                putExtra(NotificationsWidgetProvider.EXTRA_NOTIFICATION_ID, item.notificationId)
                putExtra(NotificationsWidgetProvider.EXTRA_CLICK_URL, item.clickUrl)
                putExtra(NotificationsWidgetProvider.EXTRA_SUBSCRIPTION_ID, item.subscriptionId)
            }
            views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Widget row bind failed at $position: ${e.message}", e)
            return loadingRow()
        }
        return views
    }

    override fun getLoadingView(): RemoteViews = loadingRow()

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= items.size) {
            return position.toLong()
        }
        return items[position].notificationId.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean = true

    private fun loadingRow(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_notifications_item_loading)
    }

    companion object {
        private const val TAG = "NtfyWidgetFactory"
    }
}
