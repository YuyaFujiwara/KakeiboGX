package com.example.myapplication.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.ItemDailyDataBinding
import com.example.myapplication.databinding.ItemDailyDateHeaderBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 日付ヘッダーとデータ行を統合するsealed class
sealed class DailyListItem {
    data class DateHeader(
        val date: LocalDate,
        val dailyTotal: Long // その日の収支合計（収入-支出）
    ) : DailyListItem()

    data class DataItem(val data: DailyData) : DailyListItem()
}

class DailyListAdapter(
    private val onClick: (DailyData) -> Unit
) : ListAdapter<DailyListItem, RecyclerView.ViewHolder>(DailyListDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATA = 1
    }

    // カテゴリ名参照用
    private var categoryMap: Map<Int, String> = emptyMap()

    fun setCategories(map: Map<Int, String>) {
        categoryMap = map
        notifyDataSetChanged()
    }

    /**
     * フラットなDailyDataリストをグループ化して submitList する。
     * 日付降順でグループ化し、各グループにヘッダーを挿入する。
     */
    fun submitGroupedList(flatList: List<DailyData>) {
        val grouped = mutableListOf<DailyListItem>()
        val sortedByDate = flatList.sortedByDescending { it.date }
        val groupedByDate = sortedByDate.groupBy { it.date }

        for ((date, items) in groupedByDate) {
            val income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val dailyTotal = income - expense
            grouped.add(DailyListItem.DateHeader(date, dailyTotal))
            for (item in items) {
                grouped.add(DailyListItem.DataItem(item))
            }
        }
        submitList(grouped)
    }

    // --- ViewHolders ---

    inner class DateHeaderViewHolder(private val binding: ItemDailyDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: DailyListItem.DateHeader) {
            val formatter = DateTimeFormatter.ofPattern("M月d日 (E)")
            binding.tvHeaderDate.text = header.date.format(formatter)

            val totalText = if (header.dailyTotal >= 0) {
                "+¥%,d".format(header.dailyTotal)
            } else {
                "-¥%,d".format(Math.abs(header.dailyTotal))
            }
            binding.tvHeaderTotal.text = totalText
        }
    }

    inner class DailyDataViewHolder(private val binding: ItemDailyDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: DailyData) {
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

    // --- Adapter overrides ---

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DailyListItem.DateHeader -> VIEW_TYPE_HEADER
            is DailyListItem.DataItem -> VIEW_TYPE_DATA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemDailyDateHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            DateHeaderViewHolder(binding)
        } else {
            val binding = ItemDailyDataBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            DailyDataViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DailyListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is DailyListItem.DataItem -> (holder as DailyDataViewHolder).bind(item.data)
        }
    }
}

class DailyListDiffCallback : DiffUtil.ItemCallback<DailyListItem>() {
    override fun areItemsTheSame(oldItem: DailyListItem, newItem: DailyListItem): Boolean {
        return when {
            oldItem is DailyListItem.DateHeader && newItem is DailyListItem.DateHeader ->
                oldItem.date == newItem.date
            oldItem is DailyListItem.DataItem && newItem is DailyListItem.DataItem ->
                oldItem.data.id == newItem.data.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: DailyListItem, newItem: DailyListItem): Boolean {
        return oldItem == newItem
    }
}
