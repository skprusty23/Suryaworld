package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.DepositEntity
import com.personaltracker.data.database.entity.DepositType
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {
    @Query("SELECT * FROM deposits WHERE isActive = 1 ORDER BY maturityDate ASC")
    fun getAllActive(): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits WHERE depositType = :type AND isActive = 1 ORDER BY maturityDate ASC")
    fun getByType(type: DepositType): Flow<List<DepositEntity>>

    @Query("SELECT COALESCE(SUM(depositAmount), 0) FROM deposits WHERE isActive = 1")
    fun getTotalDeposited(): Flow<Double>

    @Query("SELECT * FROM deposits WHERE id = :id")
    suspend fun getById(id: Long): DepositEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DepositEntity): Long

    @Update
    suspend fun update(entity: DepositEntity)

    @Delete
    suspend fun delete(entity: DepositEntity)
}
