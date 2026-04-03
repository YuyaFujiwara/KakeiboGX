package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memo: String,
    val amount: Long = 0L,         // 0 = メモだけプリセット
    val categoryId: Int? = null,    // null = カテゴリ未指定
    val type: TransactionType = TransactionType.EXPENSE,
    val usageCount: Int = 0,        // 使用回数（よく使うものを上に表示）
    val displayOrder: Int = 0
)
