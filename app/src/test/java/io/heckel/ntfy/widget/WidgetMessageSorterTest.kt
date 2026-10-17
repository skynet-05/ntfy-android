package io.heckel.ntfy.widget

import io.heckel.ntfy.db.Notification
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetMessageSorterTest {

    @Test
    fun filterAndSort_ordersByTopicThenTimestamp() {
        val notifications = listOf(
            notification(id = "a", subscriptionId = 1L, timestamp = 100, priority = 3),
            notification(id = "b", subscriptionId = 2L, timestamp = 200, priority = 3),
            notification(id = "c", subscriptionId = 1L, timestamp = 300, priority = 3),
            notification(id = "d", subscriptionId = 2L, timestamp = 50, priority = 1),
        )
        val config = listOf(
            WidgetTopicConfig(subscriptionId = 2L, enabled = true, minPriority = 1),
            WidgetTopicConfig(subscriptionId = 1L, enabled = true, minPriority = 1),
        )
        val result = WidgetMessageSorter.filterAndSort(notifications, config)
        assertEquals(listOf("b", "d", "c", "a"), result.map { it.id })
    }

    @Test
    fun filterAndSort_appliesMinPriorityPerTopic() {
        val notifications = listOf(
            notification(id = "low", subscriptionId = 1L, timestamp = 100, priority = 2),
            notification(id = "ok", subscriptionId = 1L, timestamp = 90, priority = 4),
        )
        val config = listOf(
            WidgetTopicConfig(subscriptionId = 1L, enabled = true, minPriority = 3),
        )
        val result = WidgetMessageSorter.filterAndSort(notifications, config)
        assertEquals(listOf("ok"), result.map { it.id })
    }

    @Test
    fun filterAndSort_ignoresDisabledTopics() {
        val notifications = listOf(
            notification(id = "x", subscriptionId = 1L, timestamp = 100, priority = 5),
        )
        val config = listOf(
            WidgetTopicConfig(subscriptionId = 1L, enabled = false, minPriority = 1),
        )
        val result = WidgetMessageSorter.filterAndSort(notifications, config)
        assertEquals(0, result.size)
    }

    private fun notification(
        id: String,
        subscriptionId: Long,
        timestamp: Long,
        priority: Int,
    ): Notification {
        return Notification(
            id = id,
            subscriptionId = subscriptionId,
            timestamp = timestamp,
            sequenceId = id,
            title = "title",
            message = "message",
            contentType = "",
            encoding = "",
            notificationId = 0,
            priority = priority,
            tags = "",
            click = "",
            icon = null,
            actions = null,
            attachment = null,
            deleted = false,
        )
    }
}
