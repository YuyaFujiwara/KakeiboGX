package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.data.dao.CategoryDao
import com.example.myapplication.data.dao.DailyDataDao
import com.example.myapplication.data.dao.FixedCostSettingDao
import com.example.myapplication.data.dao.QuotaSettingDao
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.data.entity.QuotaSetting

@Database(entities = [
    Category::class,
    DailyData::class,
    FixedCostSetting::class,
    QuotaSetting::class
], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun dailyDataDao(): DailyDataDao
    abstract fun fixedCostSettingDao(): FixedCostSettingDao
    abstract fun quotaSettingDao(): QuotaSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "household_account_database"
                )
                // 初回起動時用の初期データとしてカテゴリ等を入れる場合は addCallback を利用可能だが
                // 今回は簡単のため最初は空とする（後続の処理またはUIから追加できるようにする）
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
