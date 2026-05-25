package com.personaltracker.data.database.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter fun fromLocalDate(date: LocalDate?): String? = date?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
    @TypeConverter fun fromLocalDateTime(dt: LocalDateTime?): String? = dt?.toString()
    @TypeConverter fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }
}
