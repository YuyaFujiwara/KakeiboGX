package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_data")
data class DailyData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val amount: Long,
    val memo: String,
    val type: TransactionType,
    val categoryId: Int, // CategoryのID
    val fixedCostSettingId: Long? = null // 固定費から生成された場合の設定ID
)
