package com.example.myapplication.ui.compose.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.ui.MainViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)) {
    val allDailyData by viewModel.allDailyData.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var editingData by remember { mutableStateOf<DailyData?>(null) }

    val listState = rememberLazyListState()

    // Filter data for current month
    val monthlyData = remember(currentMonth, allDailyData) {
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        allDailyData.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
    }

    // Scroll to selected date in the list when it changes
    LaunchedEffect(selectedDate, monthlyData) {
        val sortedDates = monthlyData.map { it.date }.distinct().sortedDescending()
        val index = sortedDates.indexOf(selectedDate)
        if (index >= 0) {
            // Find actual item index considering headers
            var itemIndex = 0
            for (date in sortedDates) {
                if (date == selectedDate) break
                val itemsForDate = monthlyData.filter { it.date == date }
                itemIndex += 1 + itemsForDate.size // 1 for header + items
            }
            listState.animateScrollToItem(itemIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
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

        // Days of week header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            val days = listOf("月", "火", "水", "木", "金", "土", "日")
            days.forEachIndexed { index, day ->
                val color = when (index) {
                    5 -> Color(0xFF2196F3) // Sat
                    6 -> Color(0xFFF44336) // Sun
                    else -> Color.Black
                }
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = color
                )
            }
        }

        // Calendar Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp)
        ) {
            val startDate = currentMonth.atDay(1)
            val startDayOffset = startDate.dayOfWeek.value - 1 // Mon=0, Sun=6
            val daysInMonth = currentMonth.lengthOfMonth()

            var dayCounter = 1 - startDayOffset
            for (row in 0..5) {
                if (dayCounter > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        if (dayCounter in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayCounter)
                            val isSelected = date == selectedDate
                            val dataForDay = monthlyData.filter { it.date == date }
                            val income = dataForDay.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                            val expense = dataForDay.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                            
                            val bgColor by animateColorAsState(if (isSelected) Color(0x33007AFF) else Color.Transparent)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable { selectedDate = date }
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayCounter.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (date == LocalDate.now()) Color(0xFF007AFF) else Color.Black
                                )
                                if (income > 0) {
                                    Text(
                                        text = "+%,d".format(income),
                                        fontSize = 8.sp,
                                        color = Color(0xFF2196F3),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (expense > 0) {
                                    Text(
                                        text = "-%,d".format(expense),
                                        fontSize = 8.sp,
                                        color = Color(0xFFF44336),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1.2f))
                        }
                        dayCounter++
                    }
                }
            }
        }

        // Monthly Summary
        val totalIncome = monthlyData.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = monthlyData.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val total = totalIncome - totalExpense

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("収入", fontSize = 12.sp, color = Color.Gray)
                Text("¥%,d".format(totalIncome), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("支出", fontSize = 12.sp, color = Color.Gray)
                Text("-¥%,d".format(totalExpense), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("収支", fontSize = 12.sp, color = Color.Gray)
                val totalStr = if (total >= 0) "+¥%,d".format(total) else "-¥%,d".format(Math.abs(total))
                Text(totalStr, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider()

        // Daily List
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            val groupedByDate = monthlyData.groupBy { it.date }.toSortedMap(compareByDescending { it })
            
            groupedByDate.forEach { (date, items) ->
                item {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MM/dd (E)")),
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(items) { data ->
                    val categoryName = categories.find { it.id == data.categoryId }?.name ?: "不明"
                    val isIncome = data.type == TransactionType.INCOME
                    val amountStr = (if (isIncome) "+" else "-") + "¥%,d".format(data.amount)
                    val amountColor = if (isIncome) Color(0xFF2196F3) else Color.Black
                    
                    ListItem(
                        headlineContent = { Text(categoryName, fontWeight = FontWeight.Medium) },
                        supportingContent = { if (data.memo.isNotEmpty()) Text(data.memo, fontSize = 12.sp) },
                        trailingContent = { Text(amountStr, fontWeight = FontWeight.Bold, color = amountColor) },
                        modifier = Modifier.clickable { editingData = data },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
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
