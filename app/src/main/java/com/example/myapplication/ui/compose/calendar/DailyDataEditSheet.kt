package com.example.myapplication.ui.compose.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDataEditSheet(
    dailyData: DailyData,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (DailyData) -> Unit,
    onDelete: (DailyData) -> Unit
) {
    var amountText by remember { mutableStateOf(dailyData.amount.toString()) }
    var memoText by remember { mutableStateOf(dailyData.memo) }
    var selectedCategoryId by remember { mutableStateOf(dailyData.categoryId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("編集", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("金額") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = memoText,
                onValueChange = { memoText = it },
                label = { Text("メモ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("カテゴリ", fontWeight = FontWeight.SemiBold)
            
            // Simplified category selection for edit sheet
            val filteredCategories = categories.filter { it.type == dailyData.type }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                items(filteredCategories) { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategoryId = cat.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { selectedCategoryId = cat.id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat.name)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        onDelete(dailyData)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("削除")
                }

                Button(
                    onClick = {
                        val newAmount = amountText.toLongOrNull() ?: dailyData.amount
                        onSave(dailyData.copy(amount = newAmount, memo = memoText, categoryId = selectedCategoryId))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}
