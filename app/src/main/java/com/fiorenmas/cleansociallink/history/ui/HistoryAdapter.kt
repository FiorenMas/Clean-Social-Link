package com.fiorenmas.cleansociallink.history.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fiorenmas.cleansociallink.R
import com.fiorenmas.cleansociallink.history.data.HistoryEntry
import com.fiorenmas.cleansociallink.history.preview.HistoryImageLoader

class HistoryAdapter(
    private val onItemOpen: (HistoryEntry) -> Unit,
    private val onPreviewOpen: (String) -> Unit,
    private val onSelectionChanged: (selectedCount: Int, totalCount: Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<HistoryEntry>()
    private val selectedIds = mutableSetOf<String>()

    fun submit(newItems: List<HistoryEntry>) {
        items.clear()
        items.addAll(newItems)

        val validIds = items.mapTo(mutableSetOf()) { it.id }
        selectedIds.retainAll(validIds)

        notifyDataSetChanged()
        notifySelection()
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    fun setAllSelected(selected: Boolean) {
        selectedIds.clear()
        if (selected) {
            selectedIds.addAll(items.map { it.id })
        }
        notifyDataSetChanged()
        notifySelection()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        val selected = selectedIds.contains(item.id)
        holder.bind(item, selected, onPreviewOpen)

        holder.checkSelect.setOnCheckedChangeListener(null)
        holder.checkSelect.isChecked = selected
        holder.checkSelect.setOnCheckedChangeListener { _, checked ->
            toggleSelection(item.id, checked)
        }

        holder.itemView.setOnClickListener {
            onItemOpen(item)
        }
    }

    override fun onViewAttachedToWindow(holder: HistoryViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.loadPreviewIfNeeded()
    }

    private fun toggleSelection(id: String, selected: Boolean) {
        if (selected) {
            selectedIds.add(id)
        } else {
            selectedIds.remove(id)
        }
        notifySelection()
    }

    private fun notifySelection() {
        onSelectionChanged(selectedIds.size, items.size)
    }

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkSelect: CheckBox = view.findViewById(R.id.checkSelect)
        private val imagePreview: ImageView = view.findViewById(R.id.imagePreview)
        private val textUrl: TextView = view.findViewById(R.id.textUrl)
        private val textDate: TextView = view.findViewById(R.id.textDate)
        private var pendingPreviewUrl: String? = null

        fun bind(item: HistoryEntry, selected: Boolean, onPreviewOpen: (String) -> Unit) {
            checkSelect.isChecked = selected
            textUrl.text = item.cleanUrl
            textDate.text = item.storedAtLocalText

            pendingPreviewUrl = item.metadata.image
            imagePreview.setOnClickListener(null)
            if (pendingPreviewUrl.isNullOrBlank()) {
                imagePreview.setImageResource(android.R.drawable.ic_menu_report_image)
            } else {
                imagePreview.setOnClickListener { onPreviewOpen(pendingPreviewUrl.orEmpty()) }
                if (itemView.isAttachedToWindow) {
                    loadPreviewIfNeeded()
                } else {
                    imagePreview.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }
        }

        fun loadPreviewIfNeeded() {
            val previewUrl = pendingPreviewUrl
            if (previewUrl.isNullOrBlank()) return
            HistoryImageLoader.loadInto(imagePreview, previewUrl)
        }
    }
}

