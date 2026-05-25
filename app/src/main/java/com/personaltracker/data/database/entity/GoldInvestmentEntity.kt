package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "gold_investments")
data class GoldInvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val goldType: String,
    val quantityGrams: Double,
    val pricePerGram: Double,
    val totalAmount: Double,
    val purity: String = "24K",
    val storageLocation: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
