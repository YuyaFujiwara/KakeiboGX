package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME, EXPENSE
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorCode: String,
    val displayOrder: Int
)
