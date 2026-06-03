package com.personaltracker.data.database.converter

import androidx.room.TypeConverter
import com.personaltracker.data.database.entity.*
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter fun fromLocalDate(date: LocalDate?): String? = date?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
    @TypeConverter fun fromLocalDateTime(dt: LocalDateTime?): String? = dt?.toString()
    @TypeConverter fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }

    // Insurance
    @TypeConverter fun fromInsuranceType(v: InsuranceType?): String? = v?.name
    @TypeConverter fun toInsuranceType(v: String?): InsuranceType? = v?.let { InsuranceType.valueOf(it) }
    @TypeConverter fun fromPremiumFrequency(v: PremiumFrequency?): String? = v?.name
    @TypeConverter fun toPremiumFrequency(v: String?): PremiumFrequency? = v?.let { PremiumFrequency.valueOf(it) }

    // Mutual Fund
    @TypeConverter fun fromMfInvestmentType(v: MfInvestmentType?): String? = v?.name
    @TypeConverter fun toMfInvestmentType(v: String?): MfInvestmentType? = v?.let { MfInvestmentType.valueOf(it) }
    @TypeConverter fun fromSipFrequency(v: SipFrequency?): String? = v?.name
    @TypeConverter fun toSipFrequency(v: String?): SipFrequency? = v?.let { SipFrequency.valueOf(it) }

    // Deposit
    @TypeConverter fun fromDepositType(v: DepositType?): String? = v?.name
    @TypeConverter fun toDepositType(v: String?): DepositType? = v?.let { DepositType.valueOf(it) }
    @TypeConverter fun fromContributionType(v: ContributionType?): String? = v?.name
    @TypeConverter fun toContributionType(v: String?): ContributionType? = v?.let { ContributionType.valueOf(it) }
}
