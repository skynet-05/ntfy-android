package io.heckel.ntfy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import io.heckel.ntfy.R

object WidgetRemoteViewsHelper {

    fun collectionServiceIntent(context: Context, appWidgetId: Int): Intent {
        return Intent(context, NotificationsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            component = ComponentName(context, NotificationsWidgetService::class.java)
            setPackage(context.packageName)
            data = Uri.withAppendedPath(
                Uri.parse("content://${context.packageName}.widget"),
                appWidgetId.toString(),
            )
        }
    }

    fun bindCollectionList(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        itemClickTemplate: PendingIntent,
    ) {
        val serviceIntent = collectionServiceIntent(context, appWidgetId)
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)
        views.setPendingIntentTemplate(R.id.widget_list, itemClickTemplate)
    }

    fun itemClickTemplate(context: Context, appWidgetId: Int): PendingIntent {
        val clickIntent = Intent(context, NotificationsWidgetProvider::class.java).apply {
            action = NotificationsWidgetProvider.ACTION_WIDGET_ITEM_CLICK
            setPackage(context.packageName)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
    }
}
