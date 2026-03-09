package com.example.myapplication.ui.report

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemCategoryReportBinding

data class CategoryReportItem(
    val categoryId: Int,
    val categoryName: String,
    val colorCode: String,
    val amount: Long,
    val percent: Float
)

class CategoryReportAdapter(
    private val onClick: (CategoryReportItem) -> Unit
) : ListAdapter<CategoryReportItem, CategoryReportAdapter.CategoryReportViewHolder>(CategoryReportDiffCallback()) {

    inner class CategoryReportViewHolder(private val binding: ItemCategoryReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryReportItem) {
            binding.tvCatName.text = item.categoryName
            binding.tvCatAmount.text = "¥%,d".format(item.amount)
            binding.tvCatPercent.text = String.format("%.1f%%", item.percent * 100)

            val color = try {
                Color.parseColor("#${item.colorCode}")
            } catch (e: Exception) {
                Color.LTGRAY
            }
            
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(color)
            binding.ivCatIcon.background = bg
            binding.ivCatIcon.setColorFilter(Color.WHITE) // アイコンの色を白に

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryReportViewHolder {
        val binding = ItemCategoryReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CategoryReportDiffCallback : DiffUtil.ItemCallback<CategoryReportItem>() {
    override fun areItemsTheSame(
        oldItem: CategoryReportItem,
        newItem: CategoryReportItem
    ): Boolean {
        return oldItem.categoryId == newItem.categoryId
    }

    override fun areContentsTheSame(
        oldItem: CategoryReportItem,
        newItem: CategoryReportItem
    ): Boolean {
        return oldItem == newItem
    }
}
