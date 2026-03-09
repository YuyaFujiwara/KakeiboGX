package com.example.myapplication.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.ItemDailyDataBinding
import java.time.format.DateTimeFormatter

class DailyListAdapter(
    private val onClick: (DailyData) -> Unit
) : ListAdapter<DailyData, DailyListAdapter.DailyDataViewHolder>(DailyDataDiffCallback()) {

    // カテゴリ名参照用（簡易実装）
    private var categoryMap: Map<Int, String> = emptyMap()

    fun setCategories(map: Map<Int, String>) {
        categoryMap = map
        notifyDataSetChanged()
    }

    inner class DailyDataViewHolder(private val binding: ItemDailyDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: DailyData) {
            val formatter = DateTimeFormatter.ofPattern("M月d日 (E)")
            binding.tvListDate.text = data.date.format(formatter)
            
            // 本当は同じ日の情報をグルーピングしてヘッダー表示するのが望ましいが
            // ここでは1件ごとに日付＋データで表示する簡易実装
            val sign = if (data.type == TransactionType.INCOME) "+" else "-"
            binding.tvListTotal.text = "" // グループごとの合計を表示する場合に使用

            binding.tvListCategoryName.text = categoryMap[data.categoryId] ?: "不明"
            binding.tvListMemo.text = data.memo
            
            if (data.type == TransactionType.INCOME) {
                binding.tvListAmount.text = "+¥%,d".format(data.amount)
                binding.tvListAmount.setTextColor(0xFF2196F3.toInt()) // 青
            } else {
                binding.tvListAmount.text = "-¥%,d".format(data.amount)
                binding.tvListAmount.setTextColor(0xFFF44336.toInt()) // 赤
            }

            binding.root.setOnClickListener {
                onClick(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyDataViewHolder {
        val binding = ItemDailyDataBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DailyDataViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyDataViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DailyDataDiffCallback : DiffUtil.ItemCallback<DailyData>() {
    override fun areItemsTheSame(oldItem: DailyData, newItem: DailyData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DailyData, newItem: DailyData): Boolean {
        return oldItem == newItem
    }
}
