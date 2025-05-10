package com.example.platerecognitionapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.databinding.PlateHistoryItemBinding
import java.time.format.DateTimeFormatter

class PlateHistoryAdapter : ListAdapter<Plate, PlateHistoryAdapter.PlateViewHolder>(PlateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlateViewHolder {
        val binding = PlateHistoryItemBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return PlateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlateViewHolder(
        private val binding: PlateHistoryItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plate: Plate) {
            binding.plateNumberTextView.text = plate.plateNumber
            binding.capturedAtTextView.text = plate.capturedAt.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )
        }
    }

    class PlateDiffCallback : DiffUtil.ItemCallback<Plate>() {
        override fun areItemsTheSame(oldItem: Plate, newItem: Plate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Plate, newItem: Plate): Boolean {
            return oldItem == newItem
        }
    }
}
