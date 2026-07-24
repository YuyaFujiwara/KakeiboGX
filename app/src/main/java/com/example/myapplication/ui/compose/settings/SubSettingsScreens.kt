package com.example.myapplication.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.data.entity.Frequency
import com.example.myapplication.data.entity.QuotaSetting
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.ui.MainViewModel
import java.time.LocalDate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Check
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    navController: NavController? = null,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("カテゴリ編集") },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "追加")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(categories) { cat ->
                ListItem(
                    headlineContent = { Text(cat.name) },
                    supportingContent = { Text(if (cat.type == TransactionType.EXPENSE) "支出" else "収入") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                            Icon(Icons.Default.Delete, "削除", tint = Color.Red)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var isExpense by remember { mutableStateOf(true) }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("カテゴリ追加") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("カテゴリ名") },
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isExpense, onClick = { isExpense = true })
                        Text("支出")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !isExpense, onClick = { isExpense = false })
                        Text("収入")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotEmpty()) {
                        viewModel.insertCategory(
                            Category(
                                name = name,
                                type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                                colorCode = if (isExpense) "2196F3" else "4CAF50",
                                iconName = "ic_category",
                                displayOrder = categories.size + 1
                            )
                        )
                        showAddDialog = false
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("キャンセル") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedCostScreen(
    navController: NavController? = null,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val fixedCosts by viewModel.allFixedCostSettings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFixedCost by remember { mutableStateOf<FixedCostSetting?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("固定収支の設定") },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "追加")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(fixedCosts) { fc ->
                val catName = categories.find { it.id == fc.categoryId }?.name ?: "不明"
                ListItem(
                    modifier = Modifier.clickable { editingFixedCost = fc },
                    headlineContent = { Text(fc.name.ifEmpty { "名称未設定" }) },
                    supportingContent = { 
                        Column {
                            Text("$catName / 毎月 ${fc.dayOfMonth}日")
                            val start = fc.startDate.toString()
                            val end = fc.endDate?.toString() ?: "無期限"
                            Text("期間: $start 〜 $end", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("¥%,d".format(fc.amount), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteFixedCostSetting(fc) }) {
                                Icon(Icons.Default.Delete, "削除", tint = Color.Red)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog || editingFixedCost != null) {
        var memo by remember { mutableStateOf(editingFixedCost?.name ?: "") }
        var amount by remember { mutableStateOf(editingFixedCost?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
        var day by remember { mutableStateOf(editingFixedCost?.dayOfMonth?.toString() ?: "1") }
        var selectedCat by remember { mutableStateOf(categories.find { it.id == editingFixedCost?.categoryId } ?: categories.firstOrNull()) }
        var startDateStr by remember { mutableStateOf(editingFixedCost?.startDate?.toString() ?: LocalDate.now().toString()) }
        var endDateStr by remember { mutableStateOf(editingFixedCost?.endDate?.toString() ?: "") }
        var expanded by remember { mutableStateOf(false) }

        val closeDialog = {
            showAddDialog = false
            editingFixedCost = null
        }

        AlertDialog(
            onDismissRequest = closeDialog,
            title = { Text(if (editingFixedCost != null) "固定費の編集" else "固定費の追加") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCat?.name ?: "カテゴリ選択",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("カテゴリ") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCat = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("メモ") }) }
                    item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金額") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                    item { OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("引落し日 (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                    item { OutlinedTextField(value = startDateStr, onValueChange = { startDateStr = it }, label = { Text("開始日 (YYYY-MM-DD)") }) }
                    item { OutlinedTextField(value = endDateStr, onValueChange = { endDateStr = it }, label = { Text("終了日 (YYYY-MM-DD / 空白可)") }, placeholder = { Text("例: 2030-12-31") }) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = amount.toLongOrNull() ?: 0L
                    val d = day.toIntOrNull()?.coerceIn(1, 31) ?: 1
                    val sDate = try { LocalDate.parse(startDateStr) } catch (e: Exception) { null }
                    val eDate = if (endDateStr.isNotBlank()) {
                        try { LocalDate.parse(endDateStr) } catch (e: Exception) { null }
                    } else null
                    
                    if (amt > 0 && selectedCat != null && sDate != null) {
                        if (editingFixedCost != null) {
                            viewModel.updateFixedCostSetting(
                                editingFixedCost!!.copy(
                                    name = memo,
                                    type = selectedCat!!.type,
                                    categoryId = selectedCat!!.id,
                                    amount = amt,
                                    dayOfMonth = d,
                                    startDate = sDate,
                                    endDate = eDate
                                )
                            )
                        } else {
                            viewModel.insertFixedCostSetting(
                                FixedCostSetting(
                                    name = memo,
                                    type = selectedCat!!.type,
                                    categoryId = selectedCat!!.id,
                                    amount = amt,
                                    frequency = Frequency.MONTHLY,
                                    dayOfMonth = d,
                                    startDate = sDate,
                                    endDate = eDate
                                )
                            )
                        }
                        closeDialog()
                    } else {
                        Toast.makeText(context, "入力内容に誤りがあります", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = closeDialog) { Text("キャンセル") }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotaScreen(
    navController: NavController? = null,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val quotas by viewModel.allQuotaSettings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    
    val inputValues = remember { mutableStateMapOf<Int, String>() }
    
    LaunchedEffect(quotas, categories) {
        if (inputValues.isEmpty() && categories.isNotEmpty()) {
            categories.filter { it.type == TransactionType.EXPENSE }.forEach { cat ->
                val quota = quotas.find { it.categoryId == cat.id }
                if (quota != null && quota.amount > 0) {
                    inputValues[cat.id] = quota.amount.toString()
                } else {
                    inputValues[cat.id] = ""
                }
            }
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("予算(クォータ)設定") },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                var hasChanges = false
                inputValues.forEach { (catId, amountStr) ->
                    val amt = amountStr.toLongOrNull() ?: 0L
                    val existing = quotas.find { it.categoryId == catId }
                    if (existing != null) {
                        if (amt > 0) {
                            if (existing.amount != amt) {
                                viewModel.updateQuotaSetting(existing.copy(amount = amt))
                                hasChanges = true
                            }
                        } else {
                            viewModel.deleteQuotaSetting(existing)
                            hasChanges = true
                        }
                    } else if (amt > 0) {
                        viewModel.insertQuotaSetting(QuotaSetting(categoryId = catId, amount = amt))
                        hasChanges = true
                    }
                }
                if (hasChanges) {
                    Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
                }
                navController?.navigateUp()
            }) {
                Icon(Icons.Default.Check, contentDescription = "設定を保存")
            }
        }
    ) { padding ->
        val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
        
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(expenseCategories) { cat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cat.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp
                    )
                    Text("¥", fontSize = 18.sp, modifier = Modifier.padding(end = 4.dp))
                    OutlinedTextField(
                        value = inputValues[cat.id] ?: "",
                        onValueChange = { inputValues[cat.id] = it },
                        modifier = Modifier.width(150.dp),
                        placeholder = { Text("予算未設定") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
