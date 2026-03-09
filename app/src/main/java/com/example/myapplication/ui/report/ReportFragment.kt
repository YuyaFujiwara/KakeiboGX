package com.example.myapplication.ui.report

import android.graphics.Color
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
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.FragmentReportBinding
import com.example.myapplication.ui.MainViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var reportAdapter: CategoryReportAdapter
    private var currentMonth = YearMonth.now()
    private var currentType = TransactionType.EXPENSE
    
    // キャッシュ用データ
    private var currentCategories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        reportAdapter = CategoryReportAdapter { item ->
            Toast.makeText(requireContext(), "${item.categoryName}の月間レポート(未実装)", Toast.LENGTH_SHORT).show()
        }
        binding.rvCategoryReport.adapter = reportAdapter

        updateMonthText()

        binding.btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateMonthText()
            updateReportData()
        }

        binding.btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateMonthText()
            updateReportData()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = if (tab?.position == 0) TransactionType.EXPENSE else TransactionType.INCOME
                updateReportData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        setupPieChart()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            isRotationEnabled = false
            isHighlightPerTapEnabled = true
            legend.isEnabled = false // 凡例は自作のリストで表示するので消す
        }
    }

    private fun updateMonthText() {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM")
        binding.tvMonth.text = currentMonth.format(formatter)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collectLatest { categories ->
                        currentCategories = categories
                        updateReportData()
                    }
                }
                
                launch {
                    viewModel.allDailyData.collectLatest {
                        updateReportData()
                    }
                }
            }
        }
    }

    private fun updateReportData() {
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        
        val data = viewModel.allDailyData.value.filter {
            !it.date.isBefore(startDate) && !it.date.isAfter(endDate)
        }

        val incomeList = data.filter { it.type == TransactionType.INCOME }
        val expenseList = data.filter { it.type == TransactionType.EXPENSE }

        val totalIncome = incomeList.sumOf { it.amount }
        val totalExpense = expenseList.sumOf { it.amount }

        binding.tvSummaryIncome.text = "¥%,d".format(totalIncome)
        binding.tvSummaryExpense.text = "-¥%,d".format(totalExpense)
        
        val total = totalIncome - totalExpense
        binding.tvSummaryTotal.text = if (total >= 0) "+¥%,d".format(total) else "-¥%,d".format(Math.abs(total))

        val targetList = if (currentType == TransactionType.INCOME) incomeList else expenseList
        val totalAmount = if (currentType == TransactionType.INCOME) totalIncome.toFloat() else totalExpense.toFloat()

        // カテゴリごとの集計
        val categoryMap = targetList.groupBy { it.categoryId }.mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val reportItems = categoryMap.mapNotNull { (catId, amount) ->
            val category = currentCategories.find { it.id == catId } ?: return@mapNotNull null
            val percent = if (totalAmount > 0) amount / totalAmount else 0f
            CategoryReportItem(catId, category.name, category.colorCode, amount, percent)
        }.sortedByDescending { it.amount }

        reportAdapter.submitList(reportItems)
        updateChartData(reportItems, totalAmount)
    }

    private fun updateChartData(items: List<CategoryReportItem>, totalAmount: Float) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        for (item in items) {
            entries.add(PieEntry(item.amount.toFloat(), item.categoryName))
            try {
                colors.add(Color.parseColor("#${item.colorCode}"))
            } catch (e: Exception) {
                colors.add(Color.LTGRAY)
            }
        }

        val dataSet = PieDataSet(entries, "カテゴリ別")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.WHITE)

        binding.pieChart.data = data
        binding.pieChart.centerText = "¥%,d".format(totalAmount.toLong())
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
