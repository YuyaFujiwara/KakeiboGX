package com.example.myapplication.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.FragmentCalendarBinding
import com.example.myapplication.ui.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var dailyListAdapter: DailyListAdapter

    private var currentMonth = YearMonth.now()
    private var selectedDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        calendarAdapter = CalendarAdapter { date ->
            selectedDate = date
            updateDailyListForSelectedDate()
        }
        binding.rvCalendar.adapter = calendarAdapter

        dailyListAdapter = DailyListAdapter { data ->
            val dialog = DailyDataEditBottomSheet(
                dailyData = data,
                categories = viewModel.allCategories.value,
                onSave = { updatedData -> viewModel.updateDailyData(updatedData) },
                onDelete = { deletedData -> viewModel.deleteDailyData(deletedData) }
            )
            dialog.show(parentFragmentManager, "DailyDataEditBottomSheet")
        }
        binding.rvDailyList.adapter = dailyListAdapter

        updateMonthText()

        binding.btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateMonthText()
            updateCalendarData()
        }

        binding.btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateMonthText()
            updateCalendarData()
        }
    }

    private fun updateMonthText() {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM")
        binding.tvMonth.text = currentMonth.format(formatter)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // カテゴリ情報をリスト表示用に取得
                launch {
                    viewModel.allCategories.collectLatest { categories ->
                        val map = categories.associate { it.id to it.name }
                        dailyListAdapter.setCategories(map)
                    }
                }
                
                // データ更新が行われたらカレンダーとリストを再描画
                launch {
                    viewModel.allDailyData.collectLatest {
                        updateCalendarData()
                        updateDailyListForSelectedDate()
                    }
                }
            }
        }
    }

    private fun updateCalendarData() {
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        
        val monthlyData = viewModel.allDailyData.value.filter {
            !it.date.isBefore(startDate) && !it.date.isAfter(endDate)
        }

        val daysInMonth = currentMonth.lengthOfMonth()
        val startDayOffset = if (startDate.dayOfWeek.value == 7) 0 else startDate.dayOfWeek.value
        val calendarItems = mutableListOf<CalendarDayItem>()

        for (i in 0 until startDayOffset) {
            calendarItems.add(CalendarDayItem(null, 0, 0))
        }

        var totalIncome = 0L
        var totalExpense = 0L

        for (day in 1..daysInMonth) {
            val date = currentMonth.atDay(day)
            val dataForDay = monthlyData.filter { it.date == date }
            
            val income = dataForDay.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = dataForDay.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            totalIncome += income
            totalExpense += expense

            calendarItems.add(CalendarDayItem(date, income, expense))
        }
        
        binding.tvMonthlyIncome.text = "¥%,d".format(totalIncome)
        binding.tvMonthlyExpense.text = "-¥%,d".format(totalExpense)
        val total = totalIncome - totalExpense
        binding.tvMonthlyTotal.text = if (total >= 0) "+¥%,d".format(total) else "-¥%,d".format(Math.abs(total))

        calendarAdapter.submitList(calendarItems)
        calendarAdapter.setSelectedDate(selectedDate)
    }

    private fun updateDailyListForSelectedDate() {
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        
        val data = viewModel.allDailyData.value.filter {
            !it.date.isBefore(startDate) && !it.date.isAfter(endDate)
        }
        dailyListAdapter.submitGroupedList(data)

        // 選択した日付のヘッダー位置にスクロール
        binding.rvDailyList.post {
            val position = dailyListAdapter.currentList.indexOfFirst {
                it is DailyListItem.DateHeader && it.date == selectedDate
            }
            if (position >= 0) {
                (binding.rvDailyList.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)
                    ?.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
