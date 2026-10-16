package io.heckel.ntfy.widget

import io.heckel.ntfy.db.Notification

/**
 * Filters and sorts notifications for the home screen widget.
 */
object WidgetMessageSorter {
    const val DEFAULT_MAX_ITEMS = 50

    fun filterAndSort(
        notifications: List<Notification>,
        topicConfigs: List<WidgetTopicConfig>,
        maxItems: Int = DEFAULT_MAX_ITEMS,
    ): List<Notification> {
        val enabledConfigs = topicConfigs.filter { it.enabled }
        if (enabledConfigs.isEmpty()) {
            return emptyList()
        }
        val orderBySubscription = enabledConfigs.withIndex().associate { (index, cfg) -> cfg.subscriptionId to index }
        val minPriorityBySubscription = enabledConfigs.associate { it.subscriptionId to it.minPriority }

        return notifications
            .filter { notification ->
                val minPriority = minPriorityBySubscription[notification.subscriptionId] ?: return@filter false
                notification.priority >= minPriority
            }
            .sortedWith(
                compareBy<Notification> { orderBySubscription[it.subscriptionId] ?: Int.MAX_VALUE }
                    .thenByDescending { it.timestamp }
            )
            .take(maxItems)
    }
}
