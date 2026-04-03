package com.example.myapplication.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentCategoryReportBinding
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.calendar.DailyDataEditBottomSheet
import com.example.myapplication.ui.calendar.DailyListAdapter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CategoryReportFragment : Fragment() {

    private var _binding: FragmentCategoryReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var dailyListAdapter: DailyListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryId = arguments?.getInt("categoryId") ?: 0
        val categoryName = arguments?.getString("categoryName") ?: ""
        val currentMonthStr = arguments?.getString("currentMonth") ?: ""
        val currentMonth = if (currentMonthStr.isNotEmpty()) {
            YearMonth.parse(currentMonthStr)
        } else {
            YearMonth.now()
        }

        binding.toolbar.title = "$categoryName のレポート"
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        dailyListAdapter = DailyListAdapter { data ->
            val dialog = DailyDataEditBottomSheet(
                dailyData = data,
                categories = viewModel.allCategories.value,
                onSave = { updatedData -> viewModel.updateDailyData(updatedData) },
                onDelete = { deletedData -> viewModel.deleteDailyData(deletedData) }
            )
            dialog.show(parentFragmentManager, "DailyDataEditBottomSheet")
        }
        
        binding.rvDailyList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDailyList.adapter = dailyListAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collectLatest { categories ->
                        val map = categories.associate { it.id to it.name }
                        dailyListAdapter.setCategories(map)
                    }
                }
                launch {
                    viewModel.allDailyData.collectLatest { allData ->
                        val allCatData = allData.filter { it.categoryId == categoryId }
                        
                        // 全期間のデータをリストに表示（日付降順）
                        dailyListAdapter.submitGroupedList(allCatData.sortedByDescending { it.date })
                        
                        // 今月のヘッダー位置にスクロール
                        val monthStart = currentMonth.atDay(1)
                        val monthEnd = currentMonth.atEndOfMonth()
                        binding.rvDailyList.post {
                            val pos = dailyListAdapter.currentList.indexOfFirst {
                                it is com.example.myapplication.ui.calendar.DailyListItem.DateHeader &&
                                !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd)
                            }
                            if (pos >= 0) {
                                (binding.rvDailyList.layoutManager as? LinearLayoutManager)
                                    ?.scrollToPositionWithOffset(pos, 0)
                            }
                        }

                        // チャートの更新
                        val monthlySums = allCatData.groupBy { YearMonth.from(it.date) }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }
                            .toSortedMap()

                        if (monthlySums.isNotEmpty()) {
                            val entries = ArrayList<BarEntry>()
                            val labels = ArrayList<String>()
                            var index = 0f
                            var currentMonthIndex = 0f
                            
                            val formatter = DateTimeFormatter.ofPattern("yyyy/M月")
                            for ((month, sum) in monthlySums) {
                                if (month == currentMonth) {
                                    currentMonthIndex = index
                                }
                                entries.add(BarEntry(index, sum.toFloat()))
                                labels.add(month.format(formatter))
                                index++
                            }

                            val dataSet = BarDataSet(entries, "月間合計")
                            dataSet.color = 0xFF2196F3.toInt()
                            dataSet.setDrawValues(true)

                            val barData = BarData(dataSet)
                            binding.barChart.data = barData
                            binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                            binding.barChart.xAxis.granularity = 1f
                            binding.barChart.xAxis.labelCount = labels.size
                            binding.barChart.description.isEnabled = false
                            binding.barChart.legend.isEnabled = false
                            binding.barChart.axisRight.isEnabled = false
                            binding.barChart.axisLeft.axisMinimum = 0f
                            
                            binding.barChart.setVisibleXRangeMaximum(6f)
                            // 今月が真ん中に来るようにスクロール (6本表示なので -3)
                            binding.barChart.moveViewToX((currentMonthIndex - 3f).coerceAtLeast(0f))
                            binding.barChart.invalidate()
                        } else {
                            binding.barChart.clear()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
