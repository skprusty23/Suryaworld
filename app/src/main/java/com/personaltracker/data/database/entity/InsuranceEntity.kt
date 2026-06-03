package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class InsuranceType { LIFE, HEALTH, TERM }
enum class PremiumFrequency { MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY }

@Entity(tableName = "insurance_policies")
data class InsuranceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val policyName: String,
    val insuranceType: InsuranceType,
    val policyNumber: String = "",
    val providerCompany: String = "",
    val startDate: LocalDate,
    val maturityDate: LocalDate,
    val sumAssured: Double,
    val premiumAmount: Double,
    val premiumFrequency: PremiumFrequency = PremiumFrequency.YEARLY,
    val policyTermYears: Int = 0,
    val nomineeName: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
