package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.data.entity.Preset
import com.example.myapplication.data.entity.QuotaSetting
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.data.sync.DriveHelper
import com.example.myapplication.data.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    val syncEngine: SyncEngine

    val allCategories: StateFlow<List<Category>>
    val allDailyData: StateFlow<List<DailyData>>
    val allFixedCostSettings: StateFlow<List<FixedCostSetting>>
    val allQuotaSettings: StateFlow<List<QuotaSetting>>
    val allPresets: StateFlow<List<Preset>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(
            database.categoryDao(),
            database.dailyDataDao(),
            database.fixedCostSettingDao(),
            database.quotaSettingDao(),
            database.presetDao()
        )

        allCategories = repository.allCategories
            .map { list -> list.filter { !it.isDeleted } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        allDailyData = repository.allDailyData
            .map { list -> list.filter { !it.isDeleted } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
        
        allFixedCostSettings = repository.allFixedCostSettings
            .map { list -> list.filter { !it.isDeleted } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        allQuotaSettings = repository.allQuotaSettings
            .map { list -> list.filter { !it.isDeleted } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        allPresets = repository.allPresets
            .map { list -> list.filter { !it.isDeleted } }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

        syncEngine = SyncEngine(repository)
    }

    // --- Category ---
    fun insertCategory(category: Category) = viewModelScope.launch { repository.insertCategory(category) }
    fun updateCategory(category: Category) = viewModelScope.launch { repository.updateCategory(category.copy(updatedAt = System.currentTimeMillis())) }
    fun deleteCategory(category: Category) = viewModelScope.launch { repository.updateCategory(category.copy(isDeleted = true, updatedAt = System.currentTimeMillis())) }
    fun updateCategoryOrder(categories: List<Category>) = viewModelScope.launch { 
        val updated = categories.map { it.copy(updatedAt = System.currentTimeMillis()) }
        repository.updateAllCategories(updated) 
    }

    // --- DailyData ---
    fun getDailyDataByMonth(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyData>> = repository.getDailyDataByMonth(startDate, endDate).map { list -> list.filter { !it.isDeleted } }
    fun getDailyDataByDate(date: LocalDate): Flow<List<DailyData>> = repository.getDailyDataByDate(date).map { list -> list.filter { !it.isDeleted } }
    fun insertDailyData(data: DailyData) = viewModelScope.launch { repository.insertDailyData(data) }
    fun updateDailyData(data: DailyData) = viewModelScope.launch { repository.updateDailyData(data.copy(updatedAt = System.currentTimeMillis())) }
    fun deleteDailyData(data: DailyData) = viewModelScope.launch { repository.updateDailyData(data.copy(isDeleted = true, updatedAt = System.currentTimeMillis())) }
    fun deleteAllDailyData() = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { repository.deleteAllDailyData() }

    // --- FixedCostSetting ---
    fun insertFixedCostSetting(setting: FixedCostSetting) = viewModelScope.launch { repository.insertFixedCostSetting(setting) }
    fun updateFixedCostSetting(setting: FixedCostSetting) = viewModelScope.launch { repository.updateFixedCostSetting(setting.copy(updatedAt = System.currentTimeMillis())) }
    fun deleteFixedCostSetting(setting: FixedCostSetting) = viewModelScope.launch { repository.updateFixedCostSetting(setting.copy(isDeleted = true, updatedAt = System.currentTimeMillis())) }

    // --- QuotaSetting ---
    fun insertQuotaSetting(setting: QuotaSetting) = viewModelScope.launch { repository.insertQuotaSetting(setting) }
    fun updateQuotaSetting(setting: QuotaSetting) = viewModelScope.launch { repository.updateQuotaSetting(setting.copy(updatedAt = System.currentTimeMillis())) }
    fun deleteQuotaSetting(setting: QuotaSetting) = viewModelScope.launch { repository.updateQuotaSetting(setting.copy(isDeleted = true, updatedAt = System.currentTimeMillis())) }

    // --- Preset ---
    fun insertPreset(preset: Preset) = viewModelScope.launch { repository.insertPreset(preset) }
    fun updatePreset(preset: Preset) = viewModelScope.launch { repository.updatePreset(preset.copy(updatedAt = System.currentTimeMillis())) }
    fun deletePreset(preset: Preset) = viewModelScope.launch { repository.updatePreset(preset.copy(isDeleted = true, updatedAt = System.currentTimeMillis())) }
    fun incrementPresetUsageCount(id: Int) = viewModelScope.launch { repository.incrementPresetUsageCount(id) }

    // --- Sync ---
    fun performSync(driveHelper: DriveHelper, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Drive からリモートデータを読み込み
                val remoteJson = driveHelper.readSyncFile()

                // 2. リモートが存在し、かつ空っぽでない場合はマージ
                if (!remoteJson.isNullOrBlank()) {
                    val remoteData = syncEngine.fromJson(remoteJson)
                    syncEngine.mergeFromRemote(remoteData)
                }

                // 3. マージ後のローカルデータを Drive に書き戻し
                val localData = syncEngine.exportToSyncData()
                val json = syncEngine.toJson(localData)
                driveHelper.writeSyncFile(json) // これが失敗すればExceptionが飛ぶ
                onComplete(true, "")
            } catch (e: Exception) {
                e.printStackTrace()
                // 例外のメッセージを取り出して表示に含める
                val errorMsg = e.message ?: e.javaClass.simpleName
                onComplete(false, "詳細: $errorMsg")
            }
        }
    }

    fun exportCsvToDrive(driveHelper: DriveHelper, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dailyData = repository.allDailyData.first().sortedBy { it.date }
                val categories = repository.allCategories.first().associateBy { it.id }

                val stringBuilder = StringBuilder()
                stringBuilder.append("Date,Category,Type,Amount,Memo\n")

                for (data in dailyData) {
                    val categoryName = categories[data.categoryId]?.name ?: "不明"
                    val memoSafe = data.memo.replace("\"", "\"\"")
                    val line = "${data.date},${categoryName},${data.type.name},${data.amount},\"${memoSafe}\"\n"
                    stringBuilder.append(line)
                }

                // Excel等で文字化けしないようBOMを付与
                val csvContent = "\uFEFF" + stringBuilder.toString()
                
                val writeSuccess = driveHelper.writeCsvFile(csvContent)
                if (writeSuccess) {
                    onComplete(true, "")
                } else {
                    onComplete(false, "Drive 書き込み失敗")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "詳細: " + (e.message ?: e.javaClass.simpleName))
            }
        }
    }

    // --- CSV Export / Import ---
    fun exportCsv(uri: android.net.Uri, context: android.content.Context) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val data = allDailyData.value
            context.contentResolver.openOutputStream(uri)?.use { output ->
                val writer = output.writer()
                writer.write("Date,CategoryId,Type,Amount,Memo\n")
                data.forEach { d ->
                    writer.write("${d.date},${d.categoryId},${d.type.name},${d.amount},${d.memo}\n")
                }
                writer.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importCsv(uri: android.net.Uri, context: android.content.Context, clearFirst: Boolean = false) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (clearFirst) {
                repository.deleteAllDailyData()
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                val reader = java.io.BufferedReader(java.io.InputStreamReader(input))
                val headerLine = reader.readLine() // ヘッダー
                val isSekkeiFormat = headerLine?.startsWith("inputDate", ignoreCase = true) == true
                
                val currentCategoriesMap = allCategories.value.associateBy { it.name }.toMutableMap()
                
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // split with regex to handle quotes correctly: match commas not inside quotes (simple approximation or just remove surrounding quotes)
                    val tokens = line!!.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }
                    if (tokens.size >= 5) {
                        try {
                            val d = if (isSekkeiFormat) {
                                // sekkei format: inputDate,amount,memo,type,categoryId...
                                val parts = tokens[0].split("/")
                                val date = if (parts.size == 3) {
                                    LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                                } else {
                                    LocalDate.parse(tokens[0])
                                }
                                val amount = tokens[1].toLong()
                                val memo = tokens[2]
                                val type = if (tokens[3].equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                                val categoryName = tokens[5]
                                
                                var category = currentCategoriesMap[categoryName]
                                val categoryId = if (category != null) {
                                    category.id
                                } else {
                                    val newCategory = Category(
                                        name = categoryName,
                                        type = type,
                                        colorCode = tokens[7],
                                        iconName = "ic_category_default",
                                        displayOrder = tokens[8].toIntOrNull() ?: 1
                                    )
                                    val newId = repository.insertCategory(newCategory).toInt()
                                    currentCategoriesMap[categoryName] = newCategory.copy(id = newId)
                                    newId
                                }
                                DailyData(date = date, amount = amount, memo = memo, type = type, categoryId = categoryId)
                            } else {
                                // my export format: Date,CategoryId,Type,Amount,Memo
                                DailyData(
                                    date = LocalDate.parse(tokens[0]),
                                    categoryId = tokens[1].toInt(),
                                    type = TransactionType.valueOf(tokens[2]),
                                    amount = tokens[3].toLong(),
                                    memo = tokens[4]
                                )
                            }
                            repository.insertDailyData(d)
                        } catch (e: Exception) {
                            // パースエラーの行はスキップ
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
