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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.FragmentReportBinding
import com.example.myapplication.ui.MainViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.navigation.fragment.findNavController

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var reportAdapter: CategoryReportAdapter
    private var currentMonth = YearMonth.now()
    private var currentType = TransactionType.EXPENSE
    
    // キャッシュ用データ
    private var currentCategories: List<Category> = emptyList()
    private var currentTotalAmount: Float = 0f

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
            val bundle = android.os.Bundle().apply {
                putInt("categoryId", item.categoryId)
                putString("categoryName", item.categoryName)
                putString("currentMonth", currentMonth.toString())
            }
            findNavController().navigate(com.example.myapplication.R.id.action_report_to_category_report, bundle)
        }
        binding.rvCategoryReport.adapter = reportAdapter

        // ドラッグ＆ドロップ並べ替え
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    reportAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    // ドロップ完了時に並び順を保存
                    val orderedIds = reportAdapter.getReorderedCategoryIds()
                    val updatedCategories = orderedIds.mapIndexedNotNull { index, catId ->
                        currentCategories.find { it.id == catId }?.copy(displayOrder = index)
                    }
                    viewModel.updateCategoryOrder(updatedCategories)
                }

                override fun isLongPressDragEnabled(): Boolean = false
            }
        )
        itemTouchHelper.attachToRecyclerView(binding.rvCategoryReport)

        reportAdapter.onStartDrag = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

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
            
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            extraTopOffset = 20f
            extraBottomOffset = 20f
            extraLeftOffset = 20f
            extraRightOffset = 20f

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val pieEntry = e as? PieEntry ?: return
                    val amount = pieEntry.value.toLong()
                    val label = pieEntry.label ?: ""
                    val percent = if (currentTotalAmount > 0) pieEntry.value / currentTotalAmount * 100 else 0f
                    centerText = "$label\n¥%,d\n%.1f%%".format(amount, percent)
                }
                override fun onNothingSelected() {
                    centerText = "¥%,d".format(currentTotalAmount.toLong())
                }
            })
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

                launch {
                    viewModel.allQuotaSettings.collectLatest {
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
        val today = java.time.LocalDate.now()
        val todayMap = targetList.filter { it.date == today }.groupBy { it.categoryId }.mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val reportItems = categoryMap.mapNotNull { (catId, amount) ->
            val category = currentCategories.find { it.id == catId } ?: return@mapNotNull null
            val percent = if (totalAmount > 0) amount / totalAmount else 0f
            val quota = viewModel.allQuotaSettings.value.find { it.categoryId == catId }
            val todayAmount = todayMap[catId] ?: 0L
            CategoryReportItem(catId, category.name, category.colorCode, amount, percent, quota?.amount ?: 0L, currentMonth, category.displayOrder, todayAmount)
        }.sortedBy { it.displayOrder }

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
        dataSet.selectionShift = 8f

        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.5f
        dataSet.valueLinePart2Length = 0.5f
        dataSet.valueLineColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueFormatter(com.github.mikephil.charting.formatter.PercentFormatter(binding.pieChart))
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.BLACK)

        currentTotalAmount = totalAmount
        binding.pieChart.data = data
        binding.pieChart.centerText = "¥%,d".format(totalAmount.toLong())
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
