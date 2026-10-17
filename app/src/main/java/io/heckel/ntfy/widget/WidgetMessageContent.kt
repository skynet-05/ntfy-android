package io.heckel.ntfy.widget

import io.heckel.ntfy.db.Attachment
import io.heckel.ntfy.db.Notification

/**
 * Pure helpers for widget row text (unit-testable).
 */
object WidgetMessageContent {
    const val MAX_TITLE_LENGTH = 120

    fun displayTitle(
        notification: Notification,
        decodedMessage: String,
        untitledLabel: String,
        attachmentLabel: String,
    ): String {
        val raw = when {
            notification.title.isNotBlank() -> notification.title
            decodedMessage.isNotBlank() -> decodedMessage
            notification.attachment != null -> attachmentDisplayName(notification.attachment, attachmentLabel)
            else -> untitledLabel
        }
        return truncate(raw, MAX_TITLE_LENGTH)
    }

    fun attachmentDisplayName(attachment: Attachment?, fallback: String): String {
        if (attachment == null) {
            return fallback
        }
        return attachment.name.ifBlank { fallback }
    }

    fun truncate(text: String, maxLength: Int): String {
        if (text.length <= maxLength) {
            return text
        }
        return text.take(maxLength - 1) + "…"
    }

    fun isClickUrlOpenable(click: String?): Boolean {
        if (click.isNullOrBlank()) {
            return false
        }
        val trimmed = click.trim()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("ntfy:")
    }
}
