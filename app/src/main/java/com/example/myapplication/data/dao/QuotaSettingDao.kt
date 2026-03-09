package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.QuotaSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotaSettingDao {
    @Query("SELECT * FROM quota_settings ORDER BY id ASC")
    fun getAllQuotaSettings(): Flow<List<QuotaSetting>>

    @Query("SELECT * FROM quota_settings WHERE categoryId = :categoryId LIMIT 1")
    fun getQuotaSettingByCategoryId(categoryId: Int): Flow<QuotaSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotaSetting(setting: QuotaSetting): Long

    @Update
    suspend fun updateQuotaSetting(setting: QuotaSetting)

    @Delete
    suspend fun deleteQuotaSetting(setting: QuotaSetting)
}
