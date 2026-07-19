package com.example.myapplication.ui.compose.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.compose.calendar.DailyDataEditSheet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryReportScreen(
    navController: NavController,
    categoryId: Int,
    categoryName: String,
    currentMonthStr: String,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val allDailyData by viewModel.allDailyData.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val currentMonth = try {
        YearMonth.parse(currentMonthStr)
    } catch (e: Exception) {
        YearMonth.now()
    }
    
    val catData = allDailyData.filter { it.categoryId == categoryId }.sortedByDescending { it.date }
    val currentMonthData = catData.filter { YearMonth.from(it.date) == currentMonth }

    var editingData by remember { mutableStateOf<DailyData?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        TopAppBar(
            title = { Text("$categoryName のレポート") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Bar Chart
        val monthlySums = catData.groupBy { YearMonth.from(it.date) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toSortedMap()

        if (monthlySums.isNotEmpty()) {
            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()
            var index = 0f
            val formatter = DateTimeFormatter.ofPattern("M月")
            
            var currentMonthIndex = 0f
            
            for ((month, sum) in monthlySums) {
                if (month == currentMonth) {
                    currentMonthIndex = index
                }
                entries.add(BarEntry(index, sum.toFloat()))
                labels.add(month.format(formatter))
                index++
            }

            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.White).padding(8.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            axisRight.isEnabled = false
                            axisLeft.axisMinimum = 0f
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.granularity = 1f
                            setVisibleXRangeMaximum(6f)
                        }
                    },
                    update = { chart ->
                        val dataSet = BarDataSet(entries, "").apply {
                            color = android.graphics.Color.parseColor("#2196F3")
                            setDrawValues(true)
                        }
                        chart.data = BarData(dataSet)
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        chart.xAxis.labelCount = labels.size
                        chart.moveViewToX((currentMonthIndex - 3f).coerceAtLeast(0f))
                        chart.invalidate()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History List (filtered to current month)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(currentMonthData) { data ->
                val isIncome = data.type == TransactionType.INCOME
                val amountStr = (if (isIncome) "+" else "-") + "¥%,d".format(data.amount)
                val amountColor = if (isIncome) Color(0xFF2196F3) else Color.Black
                
                ListItem(
                    headlineContent = { Text(data.date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")), fontWeight = FontWeight.Medium) },
                    supportingContent = { if (data.memo.isNotEmpty()) Text(data.memo, fontSize = 12.sp) },
                    trailingContent = { Text(amountStr, fontWeight = FontWeight.Bold, color = amountColor) },
                    modifier = Modifier.clickable { editingData = data },
                    colors = ListItemDefaults.colors(containerColor = Color.White)
                )
                HorizontalDivider()
            }
        }
    }

    editingData?.let { data ->
        DailyDataEditSheet(
            dailyData = data,
            categories = categories,
            onDismiss = { editingData = null },
            onSave = { viewModel.updateDailyData(it) },
            onDelete = { viewModel.deleteDailyData(it) }
        )
    }
}
