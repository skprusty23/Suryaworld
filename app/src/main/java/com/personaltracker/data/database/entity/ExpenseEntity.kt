package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val subCategory: String? = null,
    val description: String? = null,
    val date: LocalDate,
    val paymentMethod: String = "Cash",
    val expenseType: String = "Home",
    val notes: String? = null,
    val receiptUri: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
