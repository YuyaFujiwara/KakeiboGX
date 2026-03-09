package com.example.myapplication.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.ItemCategoryEditBinding

class CategoryEditAdapter(
    private val onClick: (Category) -> Unit
) : ListAdapter<Category, CategoryEditAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCategoryEditBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Category) {
            binding.tvCategoryName.text = item.name
            binding.tvCategoryType.text = if (item.type == TransactionType.INCOME) "収入" else "支出"
            
            try {
                val color = Color.parseColor("#${item.colorCode}")
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                binding.vColor.background = drawable
            } catch (e: Exception) {
                // Ignore parse errors, fallback to default color
            }

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
