package com.example.myapplication.ui.report

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemCategoryReportBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CategoryReportItem(
    val categoryId: Int,
    val categoryName: String,
    val colorCode: String,
    val amount: Long,
    val percent: Float,
    val quotaAmount: Long = 0L, // 予算額（0 = 未設定）
    val currentMonth: YearMonth = YearMonth.now()
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

            // クォータ表示
            if (item.quotaAmount > 0) {
                val remaining = item.quotaAmount - item.amount
                val today = LocalDate.now()
                val endOfMonth = item.currentMonth.atEndOfMonth()
                val daysLeft = if (!today.isAfter(endOfMonth)) {
                    ChronoUnit.DAYS.between(today, endOfMonth).toInt() + 1
                } else {
                    0
                }
                val perDay = if (daysLeft > 0 && remaining > 0) remaining / daysLeft else 0L

                val quotaText = if (remaining >= 0) {
                    "予算 ¥%,d / 残 ¥%,d / 日¥%,d".format(item.quotaAmount, remaining, perDay)
                } else {
                    "予算 ¥%,d / 超過 ¥%,d".format(item.quotaAmount, Math.abs(remaining))
                }
                binding.tvQuotaInfo.text = quotaText
                binding.tvQuotaInfo.visibility = View.VISIBLE

                // 超過時は赤文字
                if (remaining < 0) {
                    binding.tvQuotaInfo.setTextColor(0xFFF44336.toInt())
                } else {
                    binding.tvQuotaInfo.setTextColor(0xFF888888.toInt())
                }
            } else {
                binding.tvQuotaInfo.visibility = View.GONE
            }

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
