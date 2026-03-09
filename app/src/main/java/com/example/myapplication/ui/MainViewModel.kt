package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.data.entity.QuotaSetting
import com.example.myapplication.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    val allCategories: StateFlow<List<Category>>
    val allDailyData: StateFlow<List<DailyData>>
    val allFixedCostSettings: StateFlow<List<FixedCostSetting>>
    val allQuotaSettings: StateFlow<List<QuotaSetting>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(
            database.categoryDao(),
            database.dailyDataDao(),
            database.fixedCostSettingDao(),
            database.quotaSettingDao()
        )

        allCategories = repository.allCategories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allDailyData = repository.allDailyData.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        
        allFixedCostSettings = repository.allFixedCostSettings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allQuotaSettings = repository.allQuotaSettings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    // --- Category ---
    fun insertCategory(category: Category) = viewModelScope.launch { repository.insertCategory(category) }
    fun updateCategory(category: Category) = viewModelScope.launch { repository.updateCategory(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch { repository.deleteCategory(category) }

    // --- DailyData ---
    fun getDailyDataByMonth(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyData>> = repository.getDailyDataByMonth(startDate, endDate)
    fun getDailyDataByDate(date: LocalDate): Flow<List<DailyData>> = repository.getDailyDataByDate(date)
    fun insertDailyData(data: DailyData) = viewModelScope.launch { repository.insertDailyData(data) }
    fun updateDailyData(data: DailyData) = viewModelScope.launch { repository.updateDailyData(data) }
    fun deleteDailyData(data: DailyData) = viewModelScope.launch { repository.deleteDailyData(data) }

    // --- FixedCostSetting ---
    fun insertFixedCostSetting(setting: FixedCostSetting) = viewModelScope.launch { repository.insertFixedCostSetting(setting) }
    fun updateFixedCostSetting(setting: FixedCostSetting) = viewModelScope.launch { repository.updateFixedCostSetting(setting) }
    fun deleteFixedCostSetting(setting: FixedCostSetting) = viewModelScope.launch { repository.deleteFixedCostSetting(setting) }

    // --- QuotaSetting ---
    fun insertQuotaSetting(setting: QuotaSetting) = viewModelScope.launch { repository.insertQuotaSetting(setting) }
    fun updateQuotaSetting(setting: QuotaSetting) = viewModelScope.launch { repository.updateQuotaSetting(setting) }
    fun deleteQuotaSetting(setting: QuotaSetting) = viewModelScope.launch { repository.deleteQuotaSetting(setting) }

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

    fun importCsv(uri: android.net.Uri, context: android.content.Context) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
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
