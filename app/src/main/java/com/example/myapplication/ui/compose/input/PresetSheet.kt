package com.example.myapplication.ui.compose.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.entity.Preset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSheet(
    presets: List<Preset>,
    onDismiss: () -> Unit,
    onPresetSelected: (Preset) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "プリセット",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (presets.isEmpty()) {
                Text("プリセットがありません", color = Color.Gray)
            } else {
                LazyColumn {
                    items(presets) { preset ->
                        ListItem(
                            headlineContent = { Text(preset.memo, fontWeight = FontWeight.SemiBold) },
                            trailingContent = {
                                if (preset.amount > 0) {
                                    Text("¥${preset.amount}", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.clickable {
                                onPresetSelected(preset)
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
