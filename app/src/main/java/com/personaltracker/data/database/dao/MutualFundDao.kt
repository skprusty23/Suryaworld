package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.MutualFundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MutualFundDao {
    @Query("SELECT * FROM mutual_funds WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<MutualFundEntity>>

    @Query("SELECT * FROM mutual_funds ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MutualFundEntity>>

    @Query("SELECT COALESCE(SUM(sipAmount), 0) FROM mutual_funds WHERE isActive = 1 AND investmentType = 'SIP'")
    fun getTotalSipAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(investmentAmount), 0) FROM mutual_funds WHERE isActive = 1 AND investmentType = 'LUMPSUM'")
    fun getTotalLumpsumAmount(): Flow<Double>

    @Query("SELECT * FROM mutual_funds WHERE id = :id")
    suspend fun getById(id: Long): MutualFundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MutualFundEntity): Long

    @Update
    suspend fun update(entity: MutualFundEntity)

    @Delete
    suspend fun delete(entity: MutualFundEntity)
}
