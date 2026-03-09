package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Frequency {
    MONTHLY, WEEKLY, YEARLY
}

enum class DayOffOption {
    NONE, BEFORE, AFTER // 休日だった場合の処理：なし、前倒し、後倒し
}

@Entity(tableName = "fixed_cost_settings")
data class FixedCostSetting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Int,
    val frequency: Frequency,
    val dayOfMonth: Int, // 毎月何日か(MONTHLYの場合)
    val dayOfWeek: Int = -1, // 曜日(WEEKLYの場合)
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val dayOffOption: DayOffOption = DayOffOption.NONE,
    val lastInsertedToDailyData: LocalDate? = null // 最後にDailyDataに自動挿入した日
)
