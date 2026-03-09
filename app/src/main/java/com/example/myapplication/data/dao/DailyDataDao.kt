package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.DailyData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyDataDao {
    @Query("SELECT * FROM daily_data ORDER BY date DESC, id DESC")
    fun getAllDailyData(): Flow<List<DailyData>>

    @Query("SELECT * FROM daily_data WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, id DESC")
    fun getDailyDataByMonth(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyData>>
    
    @Query("SELECT * FROM daily_data WHERE date = :date ORDER BY id DESC")
    fun getDailyDataByDate(date: LocalDate): Flow<List<DailyData>>

    @Query("SELECT * FROM daily_data WHERE id = :id LIMIT 1")
    suspend fun getDailyDataById(id: Long): DailyData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyData(data: DailyData): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDailyData(dataList: List<DailyData>)

    @Update
    suspend fun updateDailyData(data: DailyData)

    @Delete
    suspend fun deleteDailyData(data: DailyData)
}
