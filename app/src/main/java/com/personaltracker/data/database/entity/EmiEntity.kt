package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "emis")
data class EmiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lenderName: String,
    val loanAmount: Double,
    val emiAmount: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val startDate: LocalDate,
    val dueDay: Int,
    val loanAccountNumber: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "emi_payments")
data class EmiPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emiId: Long,
    val month: Int,
    val year: Int,
    val amount: Double,
    val paidDate: LocalDate,
    val isPaid: Boolean = true,
    val receiptNumber: String? = null,
    val notes: String? = null
)
