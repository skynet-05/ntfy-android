package io.heckel.ntfy.widget

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Per-topic settings for a single widget instance.
 * List order defines topic priority (first = highest).
 */
data class WidgetTopicConfig(
    val subscriptionId: Long,
    val enabled: Boolean = true,
    val minPriority: Int = 1,
)

data class WidgetInstanceConfig(
    val topics: List<WidgetTopicConfig> = emptyList(),
)

object WidgetConfigStore {
    private const val PREFS_NAME = "WidgetConfig"
    private val gson = Gson()

    fun load(context: Context, appWidgetId: Int): WidgetInstanceConfig {
        val json = prefs(context).getString(key(appWidgetId), null) ?: return WidgetInstanceConfig()
        val type = object : TypeToken<WidgetInstanceConfig>() {}.type
        return gson.fromJson(json, type) ?: WidgetInstanceConfig()
    }

    fun save(context: Context, appWidgetId: Int, config: WidgetInstanceConfig) {
        prefs(context).edit {
            putString(key(appWidgetId), gson.toJson(config))
        }
    }

    fun delete(context: Context, appWidgetId: Int) {
        prefs(context).edit {
            remove(key(appWidgetId))
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(appWidgetId: Int) = "widget_$appWidgetId"
}
