package io.heckel.ntfy.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R
import io.heckel.ntfy.ui.PriorityAdapter
import io.heckel.ntfy.ui.PriorityItem

class WidgetConfigureAdapter(
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
) : RecyclerView.Adapter<WidgetConfigureAdapter.ViewHolder>() {

    private val rows = mutableListOf<WidgetConfigureRow>()

    fun submit(newRows: List<WidgetConfigureRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    fun currentRows(): List<WidgetConfigureRow> = rows.toList()

    fun moveItem(from: Int, to: Int) {
        if (from == to || from < 0 || to < 0 || from >= rows.size || to >= rows.size) {
            return
        }
        val item = rows.removeAt(from)
        rows.add(to, item)
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.widget_configure_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position], position, rows.size)
    }

    override fun getItemCount(): Int = rows.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.widget_config_enabled)
        private val label: TextView = itemView.findViewById(R.id.widget_config_label)
        private val priorityDropdown: AutoCompleteTextView = itemView.findViewById(R.id.widget_config_priority)
        private val moveUp: ImageButton = itemView.findViewById(R.id.widget_config_move_up)
        private val moveDown: ImageButton = itemView.findViewById(R.id.widget_config_move_down)

        private var priorityItems: List<PriorityItem> = emptyList()
        private var row: WidgetConfigureRow? = null

        init {
            val context = itemView.context
            priorityItems = PriorityAdapter.createPriorityItems(context)
            val adapter = PriorityAdapter(context, priorityItems)
            priorityDropdown.setAdapter(adapter)
            priorityDropdown.setOnItemClickListener { _, _, index, _ ->
                row?.minPriority = priorityItems[index].priority
                priorityDropdown.setText(priorityItems[index].label, false)
            }
            checkBox.setOnCheckedChangeListener { _, checked ->
                row?.enabled = checked
            }
        }

        fun bind(row: WidgetConfigureRow, position: Int, size: Int) {
            this.row = row
            label.text = row.label
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = row.enabled
            checkBox.setOnCheckedChangeListener { _, checked ->
                row.enabled = checked
            }

            val priorityIndex = priorityItems.indexOfFirst { it.priority == row.minPriority }.coerceAtLeast(0)
            priorityDropdown.setText(priorityItems[priorityIndex].label, false)

            moveUp.isEnabled = position > 0
            moveDown.isEnabled = position < size - 1
            moveUp.setOnClickListener { onMoveUp(position) }
            moveDown.setOnClickListener { onMoveDown(position) }
        }
    }
}
