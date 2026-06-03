package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockName: String,
    val stockSymbol: String = "",
    val brokerName: String = "",
    val dematAccountNumber: String = "",
    val purchaseDate: LocalDate,
    val unitsPurchased: Double,
    val purchasePricePerUnit: Double,
    val totalPurchaseAmount: Double,
    val nominee: String = "",
    val notes: String = "",
    // Optional tracking
    val currentMarketPrice: Double? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
