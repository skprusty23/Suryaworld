package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val budget: Double? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "travel_expenses")
data class TravelExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val category: String,
    val amount: Double,
    val date: LocalDate,
    val description: String? = null,
    val paymentMethod: String = "Cash",
    val receiptUri: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
