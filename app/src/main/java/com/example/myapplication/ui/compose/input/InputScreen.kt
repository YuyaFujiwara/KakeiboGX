package com.example.myapplication.ui.compose.input

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.ui.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InputScreen(
    navController: NavController? = null,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()
    val presets by viewModel.allPresets.collectAsState()

    // 「今日」を状態として保持し、日付を跨いだときに追従できるようにする
    var today by remember { mutableStateOf(LocalDate.now()) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var currentType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var currentAmount by remember { mutableStateOf(0L) }
    var memo by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var isCardPayment by remember { mutableStateOf(true) }

    // アプリを開いたまま日付が変わった場合、「今日」を見ていたときだけ新しい今日へ移す。
    // 過去日を明示的に選んでいる場合はその日付を維持する。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = LocalDate.now()
                if (now != today) {
                    if (currentDate == today) currentDate = now
                    today = now
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showCalculator by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == currentType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        // Tabs
        TabRow(
            selectedTabIndex = if (currentType == TransactionType.EXPENSE) 0 else 1,
            containerColor = Color.White
        ) {
            Tab(
                selected = currentType == TransactionType.EXPENSE,
                onClick = {
                    currentType = TransactionType.EXPENSE
                    selectedCategoryId = null
                },
                text = { Text("支出", fontWeight = FontWeight.Bold) },
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Gray
            )
            Tab(
                selected = currentType == TransactionType.INCOME,
                onClick = {
                    currentType = TransactionType.INCOME
                    selectedCategoryId = null
                },
                text = { Text("収入", fontWeight = FontWeight.Bold) },
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Gray
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Date Switcher
            // DatePickerDialog は再コンポーズごとに作り直さず一度だけ生成し、
            // 表示前に updateDate で現在の日付に合わせる。
            val datePickerDialog = remember {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        currentDate = LocalDate.of(year, month + 1, dayOfMonth)
                    },
                    currentDate.year,
                    currentDate.monthValue - 1,
                    currentDate.dayOfMonth
                )
            }
            val openDatePicker = {
                datePickerDialog.updateDate(
                    currentDate.year,
                    currentDate.monthValue - 1,
                    currentDate.dayOfMonth
                )
                datePickerDialog.show()
            }

            val isToday = currentDate == today

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentDate = currentDate.minusDays(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前日")
                }

                // 日付表示そのものをタップ領域にする（アイコンだけより当てやすく、
                // 「カレンダー画面へ遷移」と誤解されない）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = openDatePicker)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")),
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color.Unspecified else Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "日付を選択",
                            modifier = Modifier.size(16.dp),
                            tint = if (isToday) Color.Gray else Color(0xFFD97706)
                        )
                    }
                    // 今日以外の日付を入力しようとしていることを明示する
                    if (!isToday) {
                        Text(
                            text = "今日ではありません",
                            fontSize = 11.sp,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                IconButton(onClick = { currentDate = currentDate.plusDays(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "翌日")
                }
            }

            // 今日から離れているときだけ、今日へ戻る導線を出す
            if (!isToday) {
                TextButton(
                    onClick = { currentDate = today },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                ) {
                    Text("今日 (${today.format(DateTimeFormatter.ofPattern("M/d"))}) に戻る", fontSize = 13.sp)
                }
            }

            // Memo + Preset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ (何に使ったか)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showPresets = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("⚡", fontSize = 20.sp)
                }
            }

            // Amount Fluid Card
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .scale(scale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                                showCalculator = true
                            }
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = "¥%,d".format(currentAmount),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }

            // Categories
            Text("カテゴリ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories) { cat ->
                    val isSelected = selectedCategoryId == cat.id

                    val colorValue = try {
                        android.graphics.Color.parseColor("#${cat.colorCode}")
                    } catch (e: Exception) {
                        android.graphics.Color.GRAY
                    }
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(colorValue).copy(alpha = 0.2f) else Color.White)
                            .clickable { selectedCategoryId = cat.id }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(colorValue)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(colorValue) else Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Payment Method（収入では意味を持たないため支出のときだけ表示する）
            if (currentType == TransactionType.EXPENSE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCardPayment,
                        onClick = { isCardPayment = false },
                        label = { Text("💴 現金") },
                        shape = CircleShape
                    )
                    FilterChip(
                        selected = isCardPayment,
                        onClick = { isCardPayment = true },
                        label = { Text("💳 カード") },
                        shape = CircleShape
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (selectedCategoryId == null) {
                        Toast.makeText(context, "カテゴリを選択してください", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val dailyData = DailyData(
                        date = currentDate,
                        amount = currentAmount,
                        memo = memo,
                        type = currentType,
                        categoryId = selectedCategoryId!!,
                        paymentMethod = if (currentType == TransactionType.EXPENSE) {
                            if (isCardPayment) "CARD" else "CASH"
                        } else {
                            null
                        }
                    )
                    viewModel.insertDailyData(dailyData)
                    Toast.makeText(context, "登録しました", Toast.LENGTH_SHORT).show()

                    // Reset
                    currentAmount = 0L
                    memo = ""
                    selectedCategoryId = null
                    isCardPayment = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("登録する", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCalculator) {
        CalculatorSheet(
            initialAmount = currentAmount,
            onDismiss = { showCalculator = false },
            onResult = { currentAmount = it }
        )
    }

    if (showPresets) {
        PresetSheet(
            presets = presets.filter { it.type == currentType },
            onDismiss = { showPresets = false },
            onPresetSelected = { preset ->
                memo = preset.memo
                if (preset.amount > 0) currentAmount = preset.amount
                if (preset.categoryId != null) selectedCategoryId = preset.categoryId
                viewModel.incrementPresetUsageCount(preset.id)
            }
        )
    }
}
