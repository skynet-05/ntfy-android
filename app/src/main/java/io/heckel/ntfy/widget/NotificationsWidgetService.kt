package io.heckel.ntfy.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.heckel.ntfy.R

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
        items = WidgetDataProvider.loadItems(context, appWidgetId)
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_notifications_item)
        views.setTextViewText(R.id.widget_item_title, item.title)
        views.setTextViewText(R.id.widget_item_topic, item.topicLabel)
        views.setTextViewText(R.id.widget_item_time, item.timeLabel)

        if (item.priorityIconRes != 0) {
            views.setViewVisibility(R.id.widget_item_priority, android.view.View.VISIBLE)
            views.setImageViewResource(R.id.widget_item_priority, item.priorityIconRes)
        } else {
            views.setViewVisibility(R.id.widget_item_priority, android.view.View.GONE)
        }

        WidgetTheme.applyListItemColors(context, views, R.id.widget_item_root)

        val fillInIntent = Intent().apply {
            putExtra(NotificationsWidgetProvider.EXTRA_NOTIFICATION_ID, item.notificationId)
            putExtra(NotificationsWidgetProvider.EXTRA_CLICK_URL, item.clickUrl)
            putExtra(NotificationsWidgetProvider.EXTRA_SUBSCRIPTION_ID, item.subscriptionId)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].notificationId.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}
