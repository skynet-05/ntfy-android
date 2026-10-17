package io.heckel.ntfy.widget

import android.content.Context
import io.heckel.ntfy.db.Notification
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.db.Subscription
import io.heckel.ntfy.util.decodeMessage
import io.heckel.ntfy.util.displayName
import io.heckel.ntfy.util.formatDateShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

data class WidgetListItem(
    val notificationId: String,
    val subscriptionId: Long,
    val title: String,
    val topicLabel: String,
    val timeLabel: String,
    val priority: Int,
    val clickUrl: String,
    val priorityIconRes: Int,
)

object WidgetDataProvider {
    /**
     * Loads widget rows. Room access runs on a background dispatcher because this is
     * called from AppWidgetProvider (main thread) and from RemoteViewsService.
     */
    fun loadItems(context: Context, appWidgetId: Int): List<WidgetListItem> {
        return runBlocking(Dispatchers.IO) {
            loadItemsBlocking(context, appWidgetId)
        }
    }

    internal fun loadItemsBlocking(context: Context, appWidgetId: Int): List<WidgetListItem> {
        val config = WidgetConfigStore.load(context, appWidgetId)
        val enabledIds = config.topics.filter { it.enabled }.map { it.subscriptionId }
        if (enabledIds.isEmpty()) {
            return emptyList()
        }
        val repository = Repository.getInstance(context)
        val appBaseUrl = context.getString(io.heckel.ntfy.R.string.app_base_url)
        val notifications = repository.getActiveNotificationsForSubscriptions(enabledIds)
        val sorted = WidgetMessageSorter.filterAndSort(notifications, config.topics)
        val subscriptionById = enabledIds.associateWith { id -> repository.getSubscription(id) }

        return sorted.map { notification ->
            toListItem(context, appBaseUrl, notification, subscriptionById[notification.subscriptionId])
        }
    }

    private fun toListItem(
        context: Context,
        appBaseUrl: String,
        notification: Notification,
        subscription: Subscription?,
    ): WidgetListItem {
        val decodedMessage = decodeMessage(notification)
        val title = when {
            notification.title.isNotBlank() -> notification.title
            decodedMessage.isNotBlank() -> decodedMessage
            else -> context.getString(io.heckel.ntfy.R.string.widget_untitled_message)
        }.let { if (it.length > 120) it.take(117) + "…" else it }

        val topicLabel = subscription?.let { displayName(appBaseUrl, it) }
            ?: context.getString(io.heckel.ntfy.R.string.widget_unknown_topic)

        val priorityIcon = WidgetTheme.priorityIconRes(notification.priority)

        return WidgetListItem(
            notificationId = notification.id,
            subscriptionId = notification.subscriptionId,
            title = title,
            topicLabel = topicLabel,
            timeLabel = formatDateShort(notification.timestamp),
            priority = notification.priority,
            clickUrl = notification.click,
            priorityIconRes = priorityIcon,
        )
    }
}
