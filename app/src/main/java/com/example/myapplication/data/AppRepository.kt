package com.example.myapplication.data

import com.example.myapplication.data.dao.CategoryDao
import com.example.myapplication.data.dao.DailyDataDao
import com.example.myapplication.data.dao.FixedCostSettingDao
import com.example.myapplication.data.dao.PresetDao
import com.example.myapplication.data.dao.QuotaSettingDao
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.data.entity.Preset
import com.example.myapplication.data.entity.QuotaSetting
import com.example.myapplication.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AppRepository(
    private val categoryDao: CategoryDao,
    private val dailyDataDao: DailyDataDao,
    private val fixedCostSettingDao: FixedCostSettingDao,
    private val quotaSettingDao: QuotaSettingDao,
    private val presetDao: PresetDao
) {

    // --- Category ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    fun getCategoriesByType(type: TransactionType) = categoryDao.getCategoriesByType(type)
    suspend fun getCategoryById(id: Int) = categoryDao.getCategoryById(id)
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun updateAllCategories(categories: List<Category>) = categoryDao.updateAllCategories(categories)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // --- DailyData ---
    val allDailyData: Flow<List<DailyData>> = dailyDataDao.getAllDailyData()
    fun getDailyDataByMonth(startDate: LocalDate, endDate: LocalDate) = dailyDataDao.getDailyDataByMonth(startDate, endDate)
    fun getDailyDataByDate(date: LocalDate) = dailyDataDao.getDailyDataByDate(date)
    suspend fun getDailyDataById(id: Long) = dailyDataDao.getDailyDataById(id)
    suspend fun insertDailyData(data: DailyData) = dailyDataDao.insertDailyData(data)
    suspend fun insertAllDailyData(dataList: List<DailyData>) = dailyDataDao.insertAllDailyData(dataList)
    suspend fun updateDailyData(data: DailyData) = dailyDataDao.updateDailyData(data)
    suspend fun deleteDailyData(data: DailyData) = dailyDataDao.deleteDailyData(data)
    suspend fun deleteAllDailyData(timestamp: Long = System.currentTimeMillis()) = dailyDataDao.deleteAllDailyData(timestamp)

    // --- FixedCostSetting ---
    val allFixedCostSettings: Flow<List<FixedCostSetting>> = fixedCostSettingDao.getAllFixedCostSettings()
    suspend fun getFixedCostSettingById(id: Long) = fixedCostSettingDao.getFixedCostSettingById(id)
    suspend fun insertFixedCostSetting(setting: FixedCostSetting) = fixedCostSettingDao.insertFixedCostSetting(setting)
    suspend fun updateFixedCostSetting(setting: FixedCostSetting) = fixedCostSettingDao.updateFixedCostSetting(setting)
    suspend fun deleteFixedCostSetting(setting: FixedCostSetting) = fixedCostSettingDao.deleteFixedCostSetting(setting)

    // --- QuotaSetting ---
    val allQuotaSettings: Flow<List<QuotaSetting>> = quotaSettingDao.getAllQuotaSettings()
    fun getQuotaSettingByCategoryId(categoryId: Int) = quotaSettingDao.getQuotaSettingByCategoryId(categoryId)
    suspend fun insertQuotaSetting(setting: QuotaSetting) = quotaSettingDao.insertQuotaSetting(setting)
    suspend fun updateQuotaSetting(setting: QuotaSetting) = quotaSettingDao.updateQuotaSetting(setting)
    suspend fun deleteQuotaSetting(setting: QuotaSetting) = quotaSettingDao.deleteQuotaSetting(setting)

    // --- Preset ---
    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets()
    fun getPresetsByType(type: TransactionType) = presetDao.getPresetsByType(type)
    suspend fun insertPreset(preset: Preset) = presetDao.insertPreset(preset)
    suspend fun updatePreset(preset: Preset) = presetDao.updatePreset(preset)
    suspend fun deletePreset(preset: Preset) = presetDao.deletePreset(preset)
    suspend fun incrementPresetUsageCount(id: Int) = presetDao.incrementUsageCount(id)
}
