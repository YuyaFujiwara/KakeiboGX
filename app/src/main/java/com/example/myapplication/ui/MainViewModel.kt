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
                reader.readLine() // ヘッダーをスキップ
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 5) {
                        try {
                            val d = DailyData(
                                date = LocalDate.parse(tokens[0]),
                                categoryId = tokens[1].toInt(),
                                type = TransactionType.valueOf(tokens[2]),
                                amount = tokens[3].toLong(),
                                memo = tokens[4]
                            )
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
