package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', date) = :yearMonth ORDER BY date DESC")
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', date) = :yearMonth")
    fun getTotalByMonth(yearMonth: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', date) = :yearMonth AND category = :category")
    fun getTotalByMonthAndCategory(yearMonth: String, category: String): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE strftime('%Y-%m', date) = :yearMonth GROUP BY category ORDER BY total DESC")
    fun getCategoryTotalsByMonth(yearMonth: String): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int = 10): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT DISTINCT category FROM expenses ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}

data class CategoryTotal(val category: String, val total: Double)
