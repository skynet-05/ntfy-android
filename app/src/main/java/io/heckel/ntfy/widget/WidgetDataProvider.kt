package io.heckel.ntfy.widget

import android.content.Context
import android.os.Looper
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
     * Loads widget rows. Room runs on IO when called from the main thread (AppWidgetProvider).
     * RemoteViewsService already calls from a worker thread — avoid nested runBlocking there.
     */
    fun loadItems(context: Context, appWidgetId: Int): List<WidgetListItem> {
        return if (Looper.getMainLooper() == Looper.myLooper()) {
            runBlocking(Dispatchers.IO) { loadItemsBlocking(context, appWidgetId) }
        } else {
            loadItemsBlocking(context, appWidgetId)
        }
    }

    internal fun loadItemsBlocking(context: Context, appWidgetId: Int): List<WidgetListItem> {
        val config = WidgetConfigStore.load(context, appWidgetId)
        val enabledConfigs = config.topics.filter { it.enabled }
        if (enabledConfigs.isEmpty()) {
            return emptyList()
        }

        val repository = Repository.getInstance(context)
        val subscriptionById = enabledConfigs
            .map { it.subscriptionId }
            .distinct()
            .mapNotNull { id -> repository.getSubscription(id)?.let { sub -> id to sub } }
            .toMap()

        if (subscriptionById.isEmpty()) {
            return emptyList()
        }

        val activeSubscriptionIds = subscriptionById.keys.toList()
        val notifications = repository.getActiveNotificationsForSubscriptions(activeSubscriptionIds)
        val sorted = WidgetMessageSorter.filterAndSort(
            notifications,
            enabledConfigs.filter { subscriptionById.containsKey(it.subscriptionId) },
        )

        val appBaseUrl = context.getString(io.heckel.ntfy.R.string.app_base_url)
        val untitled = context.getString(io.heckel.ntfy.R.string.widget_untitled_message)
        val attachmentLabel = context.getString(io.heckel.ntfy.R.string.widget_attachment_row)
        val unknownTopic = context.getString(io.heckel.ntfy.R.string.widget_unknown_topic)

        return sorted.mapNotNull { notification ->
            val subscription = subscriptionById[notification.subscriptionId]
            if (subscription == null) {
                return@mapNotNull null
            }
            toListItem(context, appBaseUrl, notification, subscription, untitled, attachmentLabel, unknownTopic)
        }
    }

    private fun toListItem(
        context: Context,
        appBaseUrl: String,
        notification: Notification,
        subscription: Subscription,
        untitledLabel: String,
        attachmentLabel: String,
        unknownTopic: String,
    ): WidgetListItem {
        val decodedMessage = decodeMessage(notification)
        val title = WidgetMessageContent.displayTitle(
            notification,
            decodedMessage,
            untitledLabel,
            attachmentLabel,
        )

        val topicLabel = displayName(appBaseUrl, subscription).ifBlank { unknownTopic }
        val priorityIcon = WidgetTheme.priorityIconRes(notification.priority)
        val clickUrl = if (WidgetMessageContent.isClickUrlOpenable(notification.click)) {
            notification.click.trim()
        } else {
            ""
        }

        return WidgetListItem(
            notificationId = notification.id,
            subscriptionId = notification.subscriptionId,
            title = title,
            topicLabel = topicLabel,
            timeLabel = formatDateShort(notification.timestamp),
            priority = notification.priority,
            clickUrl = clickUrl,
            priorityIconRes = priorityIcon,
        )
    }
}
