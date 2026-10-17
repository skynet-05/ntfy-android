package io.heckel.ntfy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.ui.Colors
import io.heckel.ntfy.ui.DetailActivity
import io.heckel.ntfy.ui.MainActivity
import io.heckel.ntfy.util.displayName
import io.heckel.ntfy.util.isDarkThemeOn

class NotificationsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetConfigStore.delete(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_ITEM_CLICK) {
            handleItemClick(context, intent)
            return
        }
        super.onReceive(context, intent)
    }

    private fun handleItemClick(context: Context, intent: Intent) {
        val clickUrl = intent.getStringExtra(EXTRA_CLICK_URL) ?: ""
        val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, 0L)
        if (clickUrl.isNotBlank()) {
            val viewIntent = Intent(Intent.ACTION_VIEW, clickUrl.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(viewIntent)
            return
        }
        val repository = Repository.getInstance(context)
        val subscription = repository.getSubscription(subscriptionId)
        if (subscription == null) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mainIntent)
            return
        }
        val appBaseUrl = context.getString(R.string.app_base_url)
        val detailIntent = Intent(context, DetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
            putExtra(MainActivity.EXTRA_SUBSCRIPTION_BASE_URL, subscription.baseUrl)
            putExtra(MainActivity.EXTRA_SUBSCRIPTION_TOPIC, subscription.topic)
            putExtra(MainActivity.EXTRA_SUBSCRIPTION_DISPLAY_NAME, displayName(appBaseUrl, subscription))
            putExtra(MainActivity.EXTRA_SUBSCRIPTION_INSTANT, subscription.instant)
            putExtra(MainActivity.EXTRA_SUBSCRIPTION_MUTED_UNTIL, subscription.mutedUntil)
        }
        context.startActivity(detailIntent)
    }

    companion object {
        const val ACTION_WIDGET_ITEM_CLICK = "io.heckel.ntfy.widget.ACTION_ITEM_CLICK"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_CLICK_URL = "clickUrl"
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val items = WidgetDataProvider.loadItems(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_notifications)

            val themed = WidgetTheme.widgetContext(context)
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
                themed,
                com.google.android.material.R.attr.colorSurfaceContainer,
                0
            )
            views.setInt(R.id.widget_root, "setBackgroundColor", surfaceColor)

            if (items.isEmpty()) {
                views.setViewVisibility(R.id.widget_list, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                WidgetTheme.applyEmptyStateColors(context, views)
                views.setInt(
                    R.id.widget_empty_icon,
                    "setColorFilter",
                    WidgetTheme.emptyIconTint(context)
                )
            } else {
                views.setViewVisibility(R.id.widget_list, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, View.GONE)

                val serviceIntent = Intent(context, NotificationsWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_list, serviceIntent)

                val clickIntent = Intent(context, NotificationsWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_ITEM_CLICK
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
                views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)
            }

            val repository = Repository.getInstance(context)
            val dynamicColors = repository.getDynamicColorsEnabled()
            val darkMode = isDarkThemeOn(context)
            val headerColor = Colors.statusBarNormal(context, dynamicColors, darkMode)
            views.setInt(R.id.widget_header, "setBackgroundColor", headerColor)

            val configureIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val configurePending = PendingIntent.getActivity(
                context,
                appWidgetId + 10000,
                configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, configurePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
