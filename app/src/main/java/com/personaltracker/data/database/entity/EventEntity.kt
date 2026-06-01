package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val eventDate: LocalDate,
    val reminderDate: LocalDate? = null,
    val category: String = "General",
    val priority: String = "MEDIUM",        // LOW | MEDIUM | HIGH
    val repeatType: String = "NONE",        // NONE | DAILY | WEEKLY | MONTHLY | YEARLY
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
