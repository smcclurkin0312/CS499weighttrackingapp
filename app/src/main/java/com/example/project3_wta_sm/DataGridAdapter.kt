package com.example.project3_wta_sm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

// Displaying weight data in RecyclerView
class DataGridAdapter(
    private val dataGridItems: MutableList<DataGridItem>,
    private val onDeleteClickListener: OnDeleteClickListener
) : RecyclerView.Adapter<DataGridAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate layout for items in RecyclerView
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataGridItem = dataGridItems[position]

        // Set date and weight text
        holder.dateTextView.text = dataGridItem.date
        holder.weightTextView.text = String.format(
            Locale.getDefault(),
            "%.2f", // Weight to 2 decimal places
            dataGridItem.weight
        )

        // Delete button click for removing weight entry
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener.onDeleteClick(dataGridItem.date, dataGridItem.weight)
            removeItem(position)
        }
    }

    override fun getItemCount(): Int {
        return dataGridItems.size
    }

    // Removes item from list and updates RecyclerView
    private fun removeItem(position: Int) {
        if (position in dataGridItems.indices) {
            dataGridItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, dataGridItems.size)
        }
    }

    // ViewHolder class for managing items
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val weightTextView: TextView = itemView.findViewById(R.id.weightTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    // Interface for delete button click events
    interface OnDeleteClickListener {
        fun onDeleteClick(date: String, weight: Double)
    }
}