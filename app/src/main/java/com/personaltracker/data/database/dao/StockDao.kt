package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks WHERE isActive = 1 ORDER BY purchaseDate DESC")
    fun getAllActive(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<StockEntity>>

    @Query("SELECT COALESCE(SUM(totalPurchaseAmount), 0) FROM stocks WHERE isActive = 1")
    fun getTotalInvested(): Flow<Double>

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getById(id: Long): StockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StockEntity): Long

    @Update
    suspend fun update(entity: StockEntity)

    @Delete
    suspend fun delete(entity: StockEntity)
}
