package com.example.myapplication.data.sync

import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.entity.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Room DB ↔ JSON (SyncData) の変換 + マージエンジン。
 *
 * マージ戦略: レコード単位の last-write-wins (updatedAtが新しい方を採用)
 */
class SyncEngine(private val repository: AppRepository) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ==================== エクスポート (Room → SyncData) ====================

    /**
     * Room DB の全データを SyncData に変換する
     */
    suspend fun exportToSyncData(): SyncData {
        val categories = repository.allCategories.first()
        val dailyData = repository.allDailyData.first()
        val fixedCosts = repository.allFixedCostSettings.first()
        val presets = repository.allPresets.first()
        val quotas = repository.allQuotaSettings.first()

        // categoryId (int) → syncId のマッピング
        val catIdToSyncId = categories.associate { it.id to it.syncId }

        return SyncData(
            version = 1,
            lastModified = System.currentTimeMillis(),
            categories = categories.map { it.toSync() },
            dailyData = dailyData.map { it.toSync(catIdToSyncId) },
            fixedCostSettings = fixedCosts.map { it.toSync(catIdToSyncId) },
            presets = presets.map { it.toSync(catIdToSyncId) },
            quotaSettings = quotas.map { it.toSync(catIdToSyncId) }
        )
    }

    fun toJson(syncData: SyncData): String = gson.toJson(syncData)

    fun fromJson(json: String): SyncData = gson.fromJson(json, SyncData::class.java)

    // ==================== インポート (SyncData → Room) マージ ====================

    /**
     * リモートの SyncData をローカル Room DB とマージする。
     * 各レコードを syncId で照合し、updatedAt が新しい方を採用。
     */
    suspend fun mergeFromRemote(remote: SyncData) {
        // -- Categories --
        mergeCategories(remote.categories)

        // カテゴリのsyncId→ローカルIDマッピングを再取得（マージ後）
        val categories = repository.allCategories.first()
        val catSyncIdToId = categories.associate { it.syncId to it.id }

        // -- DailyData --
        mergeDailyData(remote.dailyData, catSyncIdToId)

        // -- FixedCostSettings --
        mergeFixedCostSettings(remote.fixedCostSettings, catSyncIdToId)

        // -- Presets --
        mergePresets(remote.presets, catSyncIdToId)

        // -- QuotaSettings --
        mergeQuotaSettings(remote.quotaSettings, catSyncIdToId)
    }

    private suspend fun mergeCategories(remoteList: List<SyncCategory>) {
        val localList = repository.allCategories.first()
        val localMap = localList.associateBy { it.syncId }

        for (remote in remoteList) {
            val local = localMap[remote.syncId]
            if (local == null) {
                // ローカルに存在しない → 新規挿入
                if (!remote.isDeleted) {
                    repository.insertCategory(remote.toEntity())
                }
            } else {
                // 存在する → updatedAt が新しい方を採用
                if (remote.updatedAt > local.updatedAt) {
                    repository.updateCategory(remote.toEntity(local.id))
                }
            }
        }
    }

    private suspend fun mergeDailyData(remoteList: List<SyncDailyData>, catSyncIdToId: Map<String, Int>) {
        val localList = repository.allDailyData.first()
        val localMap = localList.associateBy { it.syncId }

        for (remote in remoteList) {
            val categoryId = catSyncIdToId[remote.categorySyncId] ?: continue
            val local = localMap[remote.syncId]
            if (local == null) {
                if (!remote.isDeleted) {
                    repository.insertDailyData(remote.toEntity(categoryId))
                }
            } else {
                if (remote.updatedAt > local.updatedAt) {
                    repository.updateDailyData(remote.toEntity(categoryId, local.id))
                }
            }
        }
    }

    private suspend fun mergeFixedCostSettings(remoteList: List<SyncFixedCostSetting>, catSyncIdToId: Map<String, Int>) {
        val localList = repository.allFixedCostSettings.first()
        val localMap = localList.associateBy { it.syncId }

        for (remote in remoteList) {
            val categoryId = catSyncIdToId[remote.categorySyncId] ?: continue
            val local = localMap[remote.syncId]
            if (local == null) {
                if (!remote.isDeleted) {
                    repository.insertFixedCostSetting(remote.toEntity(categoryId))
                }
            } else {
                if (remote.updatedAt > local.updatedAt) {
                    repository.updateFixedCostSetting(remote.toEntity(categoryId, local.id))
                }
            }
        }
    }

    private suspend fun mergePresets(remoteList: List<SyncPreset>, catSyncIdToId: Map<String, Int>) {
        val localList = repository.allPresets.first()
        val localMap = localList.associateBy { it.syncId }

        for (remote in remoteList) {
            val categoryId = remote.categorySyncId?.let { catSyncIdToId[it] }
            val local = localMap[remote.syncId]
            if (local == null) {
                if (!remote.isDeleted) {
                    repository.insertPreset(remote.toEntity(categoryId))
                }
            } else {
                if (remote.updatedAt > local.updatedAt) {
                    repository.updatePreset(remote.toEntity(categoryId, local.id))
                }
            }
        }
    }

    private suspend fun mergeQuotaSettings(remoteList: List<SyncQuotaSetting>, catSyncIdToId: Map<String, Int>) {
        val localList = repository.allQuotaSettings.first()
        val localMap = localList.associateBy { it.syncId }

        for (remote in remoteList) {
            val categoryId = catSyncIdToId[remote.categorySyncId] ?: continue
            val local = localMap[remote.syncId]
            if (local == null) {
                if (!remote.isDeleted) {
                    repository.insertQuotaSetting(remote.toEntity(categoryId))
                }
            } else {
                if (remote.updatedAt > local.updatedAt) {
                    repository.updateQuotaSetting(remote.toEntity(categoryId, local.id))
                }
            }
        }
    }

    // ==================== 変換ヘルパー ====================

    // --- Room Entity → Sync ---

    private fun Category.toSync() = SyncCategory(
        syncId = syncId, name = name, type = type.name,
        iconName = iconName, colorCode = colorCode,
        displayOrder = displayOrder, isDeleted = isDeleted, updatedAt = updatedAt
    )

    private fun DailyData.toSync(catIdToSyncId: Map<Int, String>) = SyncDailyData(
        syncId = syncId, date = date.toString(), amount = amount, memo = memo,
        type = type.name, categorySyncId = catIdToSyncId[categoryId] ?: "",
        fixedCostSettingSyncId = null, // TODO: fixedCostSettingのsyncId参照
        isDeleted = isDeleted, updatedAt = updatedAt
    )

    private fun FixedCostSetting.toSync(catIdToSyncId: Map<Int, String>) = SyncFixedCostSetting(
        syncId = syncId, name = name, amount = amount, type = type.name,
        categorySyncId = catIdToSyncId[categoryId] ?: "",
        frequency = frequency.name, dayOfMonth = dayOfMonth, dayOfWeek = dayOfWeek,
        startDate = startDate.toString(), endDate = endDate?.toString(),
        dayOffOption = dayOffOption.name,
        lastInsertedToDailyData = lastInsertedToDailyData?.toString(),
        isDeleted = isDeleted, updatedAt = updatedAt
    )

    private fun Preset.toSync(catIdToSyncId: Map<Int, String>) = SyncPreset(
        syncId = syncId, memo = memo, amount = amount,
        categorySyncId = categoryId?.let { catIdToSyncId[it] },
        type = type.name, usageCount = usageCount, displayOrder = displayOrder,
        isDeleted = isDeleted, updatedAt = updatedAt
    )

    private fun QuotaSetting.toSync(catIdToSyncId: Map<Int, String>) = SyncQuotaSetting(
        syncId = syncId, categorySyncId = catIdToSyncId[categoryId] ?: "",
        amount = amount, isMonthly = isMonthly,
        isDeleted = isDeleted, updatedAt = updatedAt
    )

    // --- Sync → Room Entity ---

    private fun SyncCategory.toEntity(localId: Int = 0) = Category(
        id = localId, name = name, type = TransactionType.valueOf(type),
        iconName = iconName, colorCode = colorCode, displayOrder = displayOrder,
        syncId = syncId, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun SyncDailyData.toEntity(categoryId: Int, localId: Long = 0) = DailyData(
        id = localId, date = LocalDate.parse(date), amount = amount, memo = memo,
        type = TransactionType.valueOf(type), categoryId = categoryId,
        syncId = syncId, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun SyncFixedCostSetting.toEntity(categoryId: Int, localId: Long = 0) = FixedCostSetting(
        id = localId, name = name, amount = amount,
        type = TransactionType.valueOf(type), categoryId = categoryId,
        frequency = Frequency.valueOf(frequency), dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek, startDate = LocalDate.parse(startDate),
        endDate = endDate?.let { LocalDate.parse(it) },
        dayOffOption = DayOffOption.valueOf(dayOffOption),
        lastInsertedToDailyData = lastInsertedToDailyData?.let { LocalDate.parse(it) },
        syncId = syncId, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun SyncPreset.toEntity(categoryId: Int?, localId: Int = 0) = Preset(
        id = localId, memo = memo, amount = amount, categoryId = categoryId,
        type = TransactionType.valueOf(type), usageCount = usageCount,
        displayOrder = displayOrder,
        syncId = syncId, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun SyncQuotaSetting.toEntity(categoryId: Int, localId: Long = 0) = QuotaSetting(
        id = localId, categoryId = categoryId, amount = amount, isMonthly = isMonthly,
        syncId = syncId, updatedAt = updatedAt, isDeleted = isDeleted
    )
}
