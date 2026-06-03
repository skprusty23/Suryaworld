package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class MfInvestmentType { SIP, LUMPSUM }
enum class SipFrequency { MONTHLY, QUARTERLY, YEARLY }

@Entity(tableName = "mutual_funds")
data class MutualFundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val portfolioName: String,
    val amcName: String = "",
    val fundName: String = "",
    val folioNumber: String = "",
    val investmentType: MfInvestmentType = MfInvestmentType.SIP,
    // SIP fields
    val sipStartDate: LocalDate? = null,
    val sipAmount: Double? = null,
    val sipFrequency: SipFrequency = SipFrequency.MONTHLY,
    // Lumpsum fields
    val investmentDate: LocalDate? = null,
    val investmentAmount: Double? = null,
    // Common
    val purpose: String = "",
    val nominee: String = "",
    val autoPay: Boolean = false,
    val linkedBankName: String = "",
    val linkedAccountNumber: String = "",
    // Optional tracking
    val currentValue: Double? = null,
    val returnsPercent: Double? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
