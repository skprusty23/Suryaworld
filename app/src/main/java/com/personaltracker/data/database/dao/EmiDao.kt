package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.EmiEntity
import com.personaltracker.data.database.entity.EmiPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmiDao {
    @Query("SELECT * FROM emis WHERE isActive = 1 ORDER BY dueDay ASC")
    fun getAllActiveEmis(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emis ORDER BY name ASC")
    fun getAllEmis(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emis WHERE id = :id")
    suspend fun getEmiById(id: Long): EmiEntity?

    @Query("SELECT SUM(emiAmount) FROM emis WHERE isActive = 1")
    fun getTotalMonthlyEmi(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmi(emi: EmiEntity): Long

    @Update
    suspend fun updateEmi(emi: EmiEntity)

    @Delete
    suspend fun deleteEmi(emi: EmiEntity)

    // Payments
    @Query("SELECT * FROM emi_payments WHERE emiId = :emiId ORDER BY year DESC, month DESC")
    fun getPaymentsForEmi(emiId: Long): Flow<List<EmiPaymentEntity>>

    @Query("SELECT * FROM emi_payments WHERE emiId = :emiId AND month = :month AND year = :year")
    suspend fun getPayment(emiId: Long, month: Int, year: Int): EmiPaymentEntity?

    @Query("SELECT COUNT(*) FROM emi_payments WHERE emiId = :emiId AND isPaid = 1")
    fun getPaidCount(emiId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: EmiPaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: EmiPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: EmiPaymentEntity)
}
