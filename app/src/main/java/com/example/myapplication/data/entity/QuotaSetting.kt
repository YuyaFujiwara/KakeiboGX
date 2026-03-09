package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quota_settings")
data class QuotaSetting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Int,
    val amount: Long, // 予算額
    val isMonthly: Boolean = true // 月間予算かどうか
)
