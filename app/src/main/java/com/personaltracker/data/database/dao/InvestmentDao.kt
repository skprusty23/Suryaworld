package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments ORDER BY name ASC")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE investmentType = :type ORDER BY name ASC")
    fun getInvestmentsByType(type: String): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Long): InvestmentEntity?

    @Query("SELECT * FROM investments WHERE maturityDate IS NOT NULL AND maturityDate BETWEEN :today AND :futureDate ORDER BY maturityDate ASC")
    fun getUpcomingMaturities(today: LocalDate, futureDate: LocalDate): Flow<List<InvestmentEntity>>

    @Query("SELECT SUM(principalAmount) FROM investments WHERE isActive = 1")
    fun getTotalInvestmentAmount(): Flow<Double?>

    @Query("SELECT SUM(currentValue) FROM investments WHERE isActive = 1 AND currentValue IS NOT NULL")
    fun getTotalCurrentValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    @Delete
    suspend fun deleteInvestment(investment: InvestmentEntity)

    @Query("SELECT DISTINCT investmentType FROM investments ORDER BY investmentType ASC")
    fun getAllTypes(): Flow<List<String>>
}
