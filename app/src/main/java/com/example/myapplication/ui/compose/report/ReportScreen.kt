package com.example.myapplication.ui.compose.report

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.ui.MainViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ReportScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val allDailyData by viewModel.allDailyData.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val monthlyData = remember(currentMonth, allDailyData) {
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        allDailyData.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
    }

    val typeData = monthlyData.filter { it.type == currentType }
    val totalAmount = typeData.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        // Tabs
        TabRow(
            selectedTabIndex = if (currentType == TransactionType.EXPENSE) 0 else 1,
            containerColor = Color.White
        ) {
            Tab(
                selected = currentType == TransactionType.EXPENSE,
                onClick = { currentType = TransactionType.EXPENSE },
                text = { Text("支出", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = currentType == TransactionType.INCOME,
                onClick = { currentType = TransactionType.INCOME },
                text = { Text("収入", fontWeight = FontWeight.Bold) }
            )
        }

        // Month Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前月")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy年 MM月")),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "翌月")
            }
        }

        // Pie Chart
        val typeCategories = categories.filter { it.type == currentType }
        val categorySums = typeData.groupBy { it.categoryId }.mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = typeCategories.mapNotNull { cat ->
            val amount = categorySums[cat.id] ?: 0L
            if (amount > 0) {
                PieEntry(amount.toFloat(), cat.name).apply {
                    data = cat.colorCode
                }
            } else null
        }

        Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.White)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        setHoleColor(AndroidColor.TRANSPARENT)
                        setDrawCenterText(true)
                        legend.isEnabled = false
                    }
                },
                update = { chart ->
                    if (entries.isNotEmpty()) {
                        val colors = entries.map {
                            try {
                                AndroidColor.parseColor("#${it.data as String}")
                            } catch (e: Exception) {
                                AndroidColor.LTGRAY
                            }
                        }
                        val dataSet = PieDataSet(entries, "").apply {
                            this.colors = colors
                            sliceSpace = 3f
                            valueTextSize = 12f
                        }
                        chart.data = PieData(dataSet)
                    } else {
                        chart.clear()
                    }
                    chart.centerText = "¥%,d".format(totalAmount)
                    chart.invalidate()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val quotas by viewModel.allQuotaSettings.collectAsState()
        val today = java.time.LocalDate.now()
        val todayData = typeData.filter { it.date == today }
        val todayCategorySums = todayData.groupBy { it.categoryId }.mapValues { entry -> entry.value.sumOf { it.amount } }

        // Category List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(typeCategories) { cat ->
                val amount = categorySums[cat.id] ?: 0L
                val quota = quotas.find { it.categoryId == cat.id }
                
                if (amount > 0 || (quota != null && quota.amount > 0)) {
                    var quotaText: String? = null
                    var isOver = false

                    if (quota != null && quota.amount > 0) {
                        val remaining = quota.amount - amount
                        val endOfMonth = currentMonth.atEndOfMonth()
                        val daysLeft = if (!today.isAfter(endOfMonth)) {
                            java.time.temporal.ChronoUnit.DAYS.between(today, endOfMonth).toInt() + 1
                        } else {
                            0
                        }

                        val todayAmount = todayCategorySums[cat.id] ?: 0L
                        val spentBeforeToday = amount - todayAmount
                        val remainingBeforeToday = quota.amount - spentBeforeToday
                        val dailyTarget = if (daysLeft > 0 && remainingBeforeToday > 0) remainingBeforeToday / daysLeft else 0L

                        if (remaining >= 0) {
                            val todayRemaining = (dailyTarget - todayAmount).coerceAtLeast(0)
                            quotaText = "残 ¥%,d / ¥%,d | 今日の目標 ¥%,d (使用 ¥%,d) あと¥%,d".format(
                                remaining, quota.amount, dailyTarget, todayAmount, todayRemaining
                            )
                        } else {
                            isOver = true
                            quotaText = "超過 ¥%,d / ¥%,d | 今日 ¥%,d".format(
                                Math.abs(remaining), quota.amount, todayAmount
                            )
                        }
                    }

                    ListItem(
                        headlineContent = { Text(cat.name, fontWeight = FontWeight.SemiBold) },
                        supportingContent = quotaText?.let {
                            { Text(it, color = if (isOver) Color(0xFFF44336) else Color(0xFF888888), fontSize = 12.sp) }
                        },
                        trailingContent = { Text("¥%,d".format(amount), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.clickable {
                            navController.navigate("category_report/${cat.id}/${cat.name}/${currentMonth}")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.White)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
