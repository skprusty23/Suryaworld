package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class DepositType { RD, PPF, FD, NPS }
enum class ContributionType { ONE_TIME, MONTHLY, QUARTERLY, YEARLY }

@Entity(tableName = "deposits")
data class DepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val depositType: DepositType,
    val accountNumber: String = "",
    val institutionName: String = "",
    val startDate: LocalDate,
    val maturityDate: LocalDate? = null,
    val depositAmount: Double,
    val purpose: String = "",
    val nominee: String = "",
    val contributionType: ContributionType = ContributionType.MONTHLY,
    val autoDebit: Boolean = false,
    val linkedBankName: String = "",
    val linkedAccountNumber: String = "",
    // Optional tracking
    val currentValue: Double? = null,
    val maturityValue: Double? = null,
    val interestRate: Double? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
