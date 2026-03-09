package com.example.myapplication.data

import androidx.room.TypeConverter
import com.example.myapplication.data.entity.DayOffOption
import com.example.myapplication.data.entity.Frequency
import com.example.myapplication.data.entity.TransactionType
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toTransactionType(value: String) = enumValueOf<TransactionType>(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType) = value.name

    @TypeConverter
    fun toFrequency(value: String) = enumValueOf<Frequency>(value)

    @TypeConverter
    fun fromFrequency(value: Frequency) = value.name

    @TypeConverter
    fun toDayOffOption(value: String) = enumValueOf<DayOffOption>(value)

    @TypeConverter
    fun fromDayOffOption(value: DayOffOption) = value.name
}
