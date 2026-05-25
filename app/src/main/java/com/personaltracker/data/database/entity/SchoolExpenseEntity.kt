package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "school_expenses")
data class SchoolExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childName: String,
    val schoolName: String? = null,
    val category: String,
    val amount: Double,
    val date: LocalDate,
    val academicYear: String,
    val description: String? = null,
    val receiptUri: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
