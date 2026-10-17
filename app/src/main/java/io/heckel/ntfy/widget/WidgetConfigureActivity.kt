package io.heckel.ntfy.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import io.heckel.ntfy.R
import io.heckel.ntfy.app.Application
import io.heckel.ntfy.db.Subscription
import io.heckel.ntfy.ui.Colors
import io.heckel.ntfy.util.displayName
import io.heckel.ntfy.util.isDarkThemeOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigureActivity : AppCompatActivity() {

    private val repository by lazy { (application as Application).repository }
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var adapter: WidgetConfigureAdapter
    private lateinit var emptyView: View
    private lateinit var listView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_configure)

        val toolbarLayout = findViewById<View>(R.id.app_bar_drawer)
        val dynamicColors = repository.getDynamicColorsEnabled()
        val darkMode = isDarkThemeOn(this)
        val statusBarColor = Colors.statusBarNormal(this, dynamicColors, darkMode)
        val toolbarTextColor = Colors.toolbarTextColor(this, dynamicColors, darkMode)
        toolbarLayout.setBackgroundColor(statusBarColor)
        val toolbar = toolbarLayout.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setTitleTextColor(toolbarTextColor)
        toolbar.setNavigationIconTint(toolbarTextColor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.widget_configure_title)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
            Colors.shouldUseLightStatusBar(dynamicColors, darkMode)

        emptyView = findViewById(R.id.widget_configure_empty)
        listView = findViewById(R.id.widget_configure_list)
        val saveButton = findViewById<MaterialButton>(R.id.widget_configure_save)

        adapter = WidgetConfigureAdapter(
            onMoveUp = { position -> adapter.moveItem(position, position - 1) },
            onMoveDown = { position -> adapter.moveItem(position, position + 1) },
        )
        listView.layoutManager = LinearLayoutManager(this)
        listView.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        touchHelper.attachToRecyclerView(listView)

        saveButton.setOnClickListener { saveAndFinish() }

        onBackPressedDispatcher.addCallback(this) {
            saveAndFinish()
        }

        lifecycleScope.launch {
            loadTopics()
        }
    }

    private suspend fun loadTopics() {
        val saved = WidgetConfigStore.load(this, appWidgetId)
        val savedById = saved.topics.associateBy { it.subscriptionId }
        val subscriptions = withContext(Dispatchers.IO) { repository.getSubscriptions() }
            .filter { it.upAppId == null } // Regular ntfy topics only
            .sortedBy { it.topic }

        val appBaseUrl = getString(R.string.app_base_url)
        val rows = if (saved.topics.isNotEmpty()) {
            val ordered = saved.topics.mapNotNull { cfg ->
                subscriptions.find { it.id == cfg.subscriptionId }?.let { sub ->
                    toRow(sub, appBaseUrl, cfg.enabled, cfg.minPriority)
                }
            }
            val remaining = subscriptions
                .filter { sub -> savedById[sub.id] == null }
                .map { sub -> toRow(sub, appBaseUrl, false, 1) }
            ordered + remaining
        } else {
            subscriptions.map { sub -> toRow(sub, appBaseUrl, true, 1) }
        }

        adapter.submit(rows)
        emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        listView.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun toRow(
        subscription: Subscription,
        appBaseUrl: String,
        enabled: Boolean,
        minPriority: Int,
    ): WidgetConfigureRow {
        return WidgetConfigureRow(
            subscriptionId = subscription.id,
            label = displayName(appBaseUrl, subscription),
            enabled = enabled,
            minPriority = minPriority,
        )
    }

    private var finishingAfterSave = false

    private fun saveAndFinish() {
        if (finishingAfterSave) {
            return
        }
        finishingAfterSave = true

        val topics = adapter.currentRows().map { row ->
            WidgetTopicConfig(
                subscriptionId = row.subscriptionId,
                enabled = row.enabled,
                minPriority = row.minPriority,
            )
        }
        WidgetConfigStore.save(this, appWidgetId, WidgetInstanceConfig(topics))

        try {
            val manager = AppWidgetManager.getInstance(this)
            NotificationsWidgetProvider.updateAppWidget(this, manager, appWidgetId)
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        } catch (e: Exception) {
            // Still confirm widget placement to the host; it can refresh on next update.
            io.heckel.ntfy.util.Log.w(TAG, "Widget update failed during configure: ${e.message}", e)
        }

        setResult(RESULT_OK, WidgetConfigureResult.success(appWidgetId))
        Toast.makeText(this, R.string.widget_configure_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            saveAndFinish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "WidgetConfigure"
    }
}

data class WidgetConfigureRow(
    val subscriptionId: Long,
    val label: String,
    var enabled: Boolean,
    var minPriority: Int,
)
