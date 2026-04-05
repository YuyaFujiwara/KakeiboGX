package com.example.myapplication.data.sync

/**
 * Google Drive上の kakeibo_sync.json のデータ構造。
 * PC版(Python)と共通のフォーマット。
 */
data class SyncData(
    val version: Int = 1,
    val lastModified: Long = System.currentTimeMillis(),
    val categories: List<SyncCategory> = emptyList(),
    val dailyData: List<SyncDailyData> = emptyList(),
    val fixedCostSettings: List<SyncFixedCostSetting> = emptyList(),
    val presets: List<SyncPreset> = emptyList(),
    val quotaSettings: List<SyncQuotaSetting> = emptyList()
)

data class SyncCategory(
    val syncId: String,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val iconName: String,
    val colorCode: String,
    val displayOrder: Int,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

data class SyncDailyData(
    val syncId: String,
    val date: String, // "yyyy-MM-dd"
    val amount: Long,
    val memo: String,
    val type: String,
    val categorySyncId: String, // Category の syncId で参照
    val fixedCostSettingSyncId: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

data class SyncFixedCostSetting(
    val syncId: String,
    val name: String,
    val amount: Long,
    val type: String,
    val categorySyncId: String,
    val frequency: String,
    val dayOfMonth: Int,
    val dayOfWeek: Int = -1,
    val startDate: String,
    val endDate: String? = null,
    val dayOffOption: String = "NONE",
    val lastInsertedToDailyData: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

data class SyncPreset(
    val syncId: String,
    val memo: String,
    val amount: Long = 0,
    val categorySyncId: String? = null,
    val type: String = "EXPENSE",
    val usageCount: Int = 0,
    val displayOrder: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

data class SyncQuotaSetting(
    val syncId: String,
    val categorySyncId: String,
    val amount: Long,
    val isMonthly: Boolean = true,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)
