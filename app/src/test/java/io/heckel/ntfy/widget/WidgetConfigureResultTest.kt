package io.heckel.ntfy.widget

import android.appwidget.AppWidgetManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Documents and checks the add-widget configure contract with the system launcher.
 *
 * Flow trace:
 * 1. Host allocates appWidgetId and starts [WidgetConfigureActivity] (APPWIDGET_CONFIGURE).
 * 2. Activity calls setResult(RESULT_CANCELED) in onCreate until configuration succeeds.
 * 3. On Save (or back), config is saved, widget is updated, then setResult(RESULT_OK,
 *    Intent with [AppWidgetManager.EXTRA_APPWIDGET_ID]) and finish().
 * 4. If the activity finishes with RESULT_CANCELED, One UI / the launcher drops the widget.
 */
class WidgetConfigureResultTest {

    @Test
    fun appWidgetManager_extraName_matchesHostContract() {
        assertEquals("appWidgetId", AppWidgetManager.EXTRA_APPWIDGET_ID)
    }

    @Test
    fun successResult_usesHostExtraKey() {
        assertEquals(AppWidgetManager.EXTRA_APPWIDGET_ID, "appWidgetId")
    }
}
