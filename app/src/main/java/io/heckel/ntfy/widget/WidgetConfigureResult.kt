package io.heckel.ntfy.widget

import android.appwidget.AppWidgetManager
import android.content.Intent

/**
 * Result contract for [WidgetConfigureActivity].
 *
 * Add-widget flow (launcher / One UI host):
 * 1. User picks widget -> host starts APPWIDGET_CONFIGURE with EXTRA_APPWIDGET_ID.
 * 2. Activity must call setResult(RESULT_CANCELED) until config succeeds (we do in onCreate).
 * 3. On success: setResult(RESULT_OK, Intent with EXTRA_APPWIDGET_ID) then finish().
 * 4. If the activity finishes with RESULT_CANCELED, the host removes the pending widget.
 */
object WidgetConfigureResult {
    fun success(appWidgetId: Int): Intent {
        return Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
}
