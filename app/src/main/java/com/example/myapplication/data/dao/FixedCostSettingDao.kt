package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.FixedCostSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedCostSettingDao {
    @Query("SELECT * FROM fixed_cost_settings ORDER BY id ASC")
    fun getAllFixedCostSettings(): Flow<List<FixedCostSetting>>

    @Query("SELECT * FROM fixed_cost_settings WHERE id = :id LIMIT 1")
    suspend fun getFixedCostSettingById(id: Long): FixedCostSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedCostSetting(setting: FixedCostSetting): Long

    @Update
    suspend fun updateFixedCostSetting(setting: FixedCostSetting)

    @Delete
    suspend fun deleteFixedCostSetting(setting: FixedCostSetting)
}
