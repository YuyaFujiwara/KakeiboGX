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
import java.util.UUID

@Database(entities = [
    Category::class,
    DailyData::class,
    FixedCostSetting::class,
    QuotaSetting::class,
    Preset::class
], version = 3, exportSchema = false)
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 全テーブルに syncId, updatedAt, isDeleted カラムを追加
                val tables = listOf("categories", "daily_data", "fixed_cost_settings", "presets", "quota_settings")
                for (table in tables) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE $table ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                }
                // 既存レコードにUUIDを割り振る
                for (table in tables) {
                    val cursor = db.query("SELECT id FROM $table")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val uuid = UUID.randomUUID().toString()
                        db.execSQL("UPDATE $table SET syncId = ?, updatedAt = ? WHERE id = ?",
                            arrayOf(uuid, System.currentTimeMillis(), id))
                    }
                    cursor.close()
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "household_account_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

