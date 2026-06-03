package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.InsuranceEntity
import com.personaltracker.data.database.entity.InsuranceType
import kotlinx.coroutines.flow.Flow

@Dao
interface InsuranceDao {
    @Query("SELECT * FROM insurance_policies WHERE isActive = 1 ORDER BY maturityDate ASC")
    fun getAllActive(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurance_policies ORDER BY createdAt DESC")
    fun getAll(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurance_policies WHERE insuranceType = :type AND isActive = 1 ORDER BY maturityDate ASC")
    fun getByType(type: InsuranceType): Flow<List<InsuranceEntity>>

    @Query("SELECT COALESCE(SUM(sumAssured), 0) FROM insurance_policies WHERE isActive = 1")
    fun getTotalCoverage(): Flow<Double>

    @Query("SELECT * FROM insurance_policies WHERE id = :id")
    suspend fun getById(id: Long): InsuranceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InsuranceEntity): Long

    @Update
    suspend fun update(entity: InsuranceEntity)

    @Delete
    suspend fun delete(entity: InsuranceEntity)
}
