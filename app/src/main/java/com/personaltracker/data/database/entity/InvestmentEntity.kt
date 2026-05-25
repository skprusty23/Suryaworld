package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val investmentType: String,
    val principalAmount: Double,
    val currentValue: Double? = null,
    val interestRate: Double? = null,
    val startDate: LocalDate,
    val maturityDate: LocalDate? = null,
    val policyNumber: String? = null,
    val providerName: String? = null,
    val nominee: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
