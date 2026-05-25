package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.GoldInvestmentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GoldInvestmentDao {
    @Query("SELECT * FROM gold_investments ORDER BY date DESC")
    fun getAllGoldInvestments(): Flow<List<GoldInvestmentEntity>>

    @Query("SELECT * FROM gold_investments WHERE goldType = :type ORDER BY date DESC")
    fun getGoldByType(type: String): Flow<List<GoldInvestmentEntity>>

    @Query("SELECT SUM(quantityGrams) FROM gold_investments")
    fun getTotalGrams(): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM gold_investments")
    fun getTotalInvested(): Flow<Double?>

    @Query("SELECT * FROM gold_investments WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getGoldByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<GoldInvestmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoldInvestment(gold: GoldInvestmentEntity): Long

    @Update
    suspend fun updateGoldInvestment(gold: GoldInvestmentEntity)

    @Delete
    suspend fun deleteGoldInvestment(gold: GoldInvestmentEntity)
}
