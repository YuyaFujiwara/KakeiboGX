package com.example.myapplication.ui.compose.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    initialAmount: Long,
    onDismiss: () -> Unit,
    onResult: (Long) -> Unit
) {
    var displayValue by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "0") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¥${displayValue}",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            val buttons = listOf(
                "7", "8", "9", "C",
                "4", "5", "6", "DEL",
                "1", "2", "3", "OK",
                "00", "0", "", ""
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(buttons) { btn ->
                    if (btn.isNotEmpty()) {
                        CalculatorButton(
                            text = btn,
                            onClick = {
                                when (btn) {
                                    "C" -> displayValue = "0"
                                    "DEL" -> {
                                        if (displayValue.length > 1) {
                                            displayValue = displayValue.dropLast(1)
                                        } else {
                                            displayValue = "0"
                                        }
                                    }
                                    "OK" -> {
                                        onResult(displayValue.toLongOrNull() ?: 0L)
                                        onDismiss()
                                    }
                                    else -> {
                                        if (displayValue == "0") {
                                            displayValue = btn
                                        } else {
                                            if (displayValue.length < 10) {
                                                displayValue += btn
                                            }
                                        }
                                    }
                                }
                            },
                            isAction = btn == "C" || btn == "DEL" || btn == "OK"
                        )
                    } else {
                        Spacer(modifier = Modifier.size(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(text: String, onClick: () -> Unit, isAction: Boolean) {
    val bgColor = if (isAction) Color(0xFFE5E5EA) else Color.White
    val textColor = if (isAction) Color.Red.takeIf { text == "C" } ?: Color.Black else Color.Black
    val elevation = if (isAction) 0.dp else 4.dp

    Card(
        modifier = Modifier
            .size(72.dp)
            .clickable { onClick() },
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}
