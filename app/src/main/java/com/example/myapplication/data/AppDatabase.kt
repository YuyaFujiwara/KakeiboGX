package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(entities = [
    Category::class,
    DailyData::class,
    FixedCostSetting::class,
    QuotaSetting::class,
    Preset::class
], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun dailyDataDao(): DailyDataDao
    abstract fun fixedCostSettingDao(): FixedCostSettingDao
    abstract fun quotaSettingDao(): QuotaSettingDao
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS presets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    memo TEXT NOT NULL,
                    amount INTEGER NOT NULL DEFAULT 0,
                    categoryId INTEGER,
                    type TEXT NOT NULL DEFAULT 'EXPENSE',
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    displayOrder INTEGER NOT NULL DEFAULT 0
                )""")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "household_account_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
