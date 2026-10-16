package io.heckel.ntfy.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.util.isDarkThemeOn

object WidgetTheme {
    fun widgetContext(base: Context): Context {
        val repository = Repository.getInstance(base)
        var context = base.applicationContext
        if (repository.getDynamicColorsEnabled()) {
            context = DynamicColors.wrapContextIfAvailable(context)
        }
        return context
    }

    fun applyListItemColors(context: Context, views: RemoteViews, viewId: Int) {
        val themed = widgetContext(context)
        val surface = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorSurface, 0)
        val onSurface = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorOnSurface, 0)
        val onSurfaceVariant = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val outline = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorOutlineVariant, 0)

        views.setInt(viewId, "setBackgroundColor", surface)
        views.setTextColor(R.id.widget_item_title, onSurface)
        views.setTextColor(R.id.widget_item_topic, onSurfaceVariant)
        views.setTextColor(R.id.widget_item_time, onSurfaceVariant)
        views.setInt(R.id.widget_item_divider, "setBackgroundColor", outline)
    }

    fun createWidgetBackground(context: Context): GradientDrawable {
        val themed = widgetContext(context)
        val surface = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorSurfaceContainer, 0)
        val radius = widgetCornerRadiusPx(context)
        return GradientDrawable().apply {
            setColor(surface)
            cornerRadius = radius
        }
    }

    fun applyEmptyStateColors(context: Context, views: RemoteViews) {
        val themed = widgetContext(context)
        val onSurfaceVariant = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        views.setTextColor(R.id.widget_empty_text, onSurfaceVariant)
    }

    fun emptyIconTint(context: Context): Int {
        val themed = widgetContext(context)
        return MaterialColors.getColor(themed, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
    }

    fun widgetCornerRadiusPx(context: Context): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val resId = context.resources.getIdentifier(
                "system_app_widget_background_radius",
                "dimen",
                "android"
            )
            if (resId != 0) {
                return context.resources.getDimension(resId)
            }
        }
        return context.resources.getDimension(R.dimen.widget_corner_radius_fallback)
    }

    fun isDark(context: Context): Boolean = isDarkThemeOn(context)

    fun priorityIconRes(priority: Int): Int {
        return when (priority) {
            1 -> R.drawable.ic_priority_1_24dp
            2 -> R.drawable.ic_priority_2_24dp
            4 -> R.drawable.ic_priority_4_24dp
            5 -> R.drawable.ic_priority_5_24dp
            else -> 0
        }
    }
}
