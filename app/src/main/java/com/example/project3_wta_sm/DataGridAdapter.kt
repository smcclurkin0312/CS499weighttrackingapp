package com.example.project3_wta_sm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class DataGridAdapter(
    // Data items to be shown in RecyclerView
    private val dataGridItems: List<DataGridItem>,
    // Listener for delete button clicks
    private val onDeleteClickListener: OnDeleteClickListener
) : RecyclerView.Adapter<DataGridAdapter.ViewHolder>() {

    // Called for ViewHolder to display data
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_grid, parent, false)
        return ViewHolder(view)
    }

    // Binds data to ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataGridItem = dataGridItems[position]

        // Sets date and weight in TextViews
        holder.dateTextView.text = dataGridItem.date
        holder.weightTextView.text = String.format(
            Locale.getDefault(),
            "%.2f",
            dataGridItem.weight
        )

        // Delete button click listener
        holder.deleteButton.setOnClickListener { _: View? ->
            onDeleteClickListener.onDeleteClick(
                dataGridItem.date,
                dataGridItem.weight
            )
        }
    }

    // Returns total number of items in data list
    override fun getItemCount(): Int {
        return dataGridItems.size
    }

    // ViewHolder references to views for each item
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val weightTextView: TextView = itemView.findViewById(R.id.weightTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    // Interface for delete button clicks
    interface OnDeleteClickListener {
        fun onDeleteClick(date: String, weight: Double)
    }
}