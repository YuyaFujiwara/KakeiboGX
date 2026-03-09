package com.example.myapplication.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemCalendarDayBinding
import java.time.LocalDate

data class CalendarDayItem(
    val date: LocalDate?, // nullの場合は空白セル
    val income: Long,
    val expense: Long
)

class CalendarAdapter(
    private val onClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val items = mutableListOf<CalendarDayItem>()
    private var selectedDate: LocalDate? = null

    fun submitList(newItems: List<CalendarDayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    fun setSelectedDate(date: LocalDate) {
        val oldIndex = items.indexOfFirst { it.date == selectedDate }
        val newIndex = items.indexOfFirst { it.date == date }
        
        selectedDate = date
        
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class CalendarViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CalendarDayItem) {
            if (item.date == null) {
                binding.tvDayNumber.text = ""
                binding.tvIncome.text = ""
                binding.tvExpense.text = ""
                binding.root.setOnClickListener(null)
                binding.root.setBackgroundResource(android.R.color.transparent)
                return
            }

            binding.tvDayNumber.text = item.date.dayOfMonth.toString()

            if (item.income > 0) {
                binding.tvIncome.text = "¥${item.income}"
            } else {
                binding.tvIncome.text = ""
            }

            if (item.expense > 0) {
                binding.tvExpense.text = "-¥${item.expense}"
            } else {
                binding.tvExpense.text = ""
            }

            // 選択状態の強調表示
            if (item.date == selectedDate) {
                binding.root.setBackgroundColor(0x330000FF) // 青みがかった背景
            } else {
                binding.root.setBackgroundResource(android.R.color.transparent)
            }

            binding.root.setOnClickListener {
                onClick(item.date)
                setSelectedDate(item.date)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        // 画面の高さにあわせて高さを調整
        val lp = binding.root.layoutParams
        lp.height = parent.measuredHeight / 6 // 最大6週
        binding.root.layoutParams = lp
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
