package io.heckel.ntfy.widget

import io.heckel.ntfy.db.Attachment
import io.heckel.ntfy.db.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMessageContentTest {

    @Test
    fun displayTitle_prefersTitleOverMessage() {
        val n = notification(title = "Hello", message = "Body")
        assertEquals("Hello", WidgetMessageContent.displayTitle(n, "Body", "?", "Att"))
    }

    @Test
    fun displayTitle_usesMessageWhenTitleBlank() {
        val n = notification(title = "", message = "Body")
        assertEquals("Body", WidgetMessageContent.displayTitle(n, "Body", "?", "Att"))
    }

    @Test
    fun displayTitle_attachmentOnly() {
        val n = notification(
            title = "",
            message = "",
            attachment = Attachment("file.zip", "application/zip", 1L, null, "https://x/y"),
        )
        assertEquals("file.zip", WidgetMessageContent.displayTitle(n, "", "?", "Att"))
    }

    @Test
    fun displayTitle_truncatesLongText() {
        val long = "a".repeat(200)
        val n = notification(title = long, message = "")
        val result = WidgetMessageContent.displayTitle(n, "", "?", "Att")
        assertTrue(result.length <= WidgetMessageContent.MAX_TITLE_LENGTH)
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun isClickUrlOpenable_acceptsHttpAndNtfy() {
        assertTrue(WidgetMessageContent.isClickUrlOpenable("https://example.com"))
        assertTrue(WidgetMessageContent.isClickUrlOpenable("ntfy://topic"))
        assertFalse(WidgetMessageContent.isClickUrlOpenable(""))
        assertFalse(WidgetMessageContent.isClickUrlOpenable("not a url"))
    }

    private fun notification(
        title: String,
        message: String,
        attachment: Attachment? = null,
    ): Notification {
        return Notification(
            id = "1",
            subscriptionId = 1L,
            timestamp = 1L,
            sequenceId = "1",
            title = title,
            message = message,
            contentType = "",
            encoding = "",
            notificationId = 0,
            priority = 3,
            tags = "",
            click = "",
            icon = null,
            actions = null,
            attachment = attachment,
            deleted = false,
        )
    }
}
