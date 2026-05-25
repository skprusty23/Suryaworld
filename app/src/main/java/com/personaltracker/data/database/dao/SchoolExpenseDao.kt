package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.SchoolExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolExpenseDao {
    @Query("SELECT * FROM school_expenses ORDER BY date DESC")
    fun getAllSchoolExpenses(): Flow<List<SchoolExpenseEntity>>

    @Query("SELECT * FROM school_expenses WHERE childName = :childName ORDER BY date DESC")
    fun getExpensesByChild(childName: String): Flow<List<SchoolExpenseEntity>>

    @Query("SELECT * FROM school_expenses WHERE academicYear = :year ORDER BY date DESC")
    fun getExpensesByYear(year: String): Flow<List<SchoolExpenseEntity>>

    @Query("SELECT * FROM school_expenses WHERE childName = :childName AND academicYear = :year ORDER BY date DESC")
    fun getExpensesByChildAndYear(childName: String, year: String): Flow<List<SchoolExpenseEntity>>

    @Query("SELECT SUM(amount) FROM school_expenses WHERE childName = :childName AND academicYear = :year")
    fun getTotalByChildAndYear(childName: String, year: String): Flow<Double?>

    @Query("SELECT DISTINCT childName FROM school_expenses ORDER BY childName ASC")
    fun getAllChildren(): Flow<List<String>>

    @Query("SELECT DISTINCT academicYear FROM school_expenses ORDER BY academicYear DESC")
    fun getAllYears(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolExpense(expense: SchoolExpenseEntity): Long

    @Update
    suspend fun updateSchoolExpense(expense: SchoolExpenseEntity)

    @Delete
    suspend fun deleteSchoolExpense(expense: SchoolExpenseEntity)
}
