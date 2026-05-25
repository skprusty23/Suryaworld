package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.TravelExpenseEntity
import com.personaltracker.data.database.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE isCompleted = 0 ORDER BY startDate ASC")
    fun getActiveTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("SELECT * FROM travel_expenses WHERE tripId = :tripId ORDER BY date DESC")
    fun getExpensesForTrip(tripId: Long): Flow<List<TravelExpenseEntity>>

    @Query("SELECT SUM(amount) FROM travel_expenses WHERE tripId = :tripId")
    fun getTotalForTrip(tripId: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM travel_expenses WHERE tripId = :tripId GROUP BY category")
    fun getCategoryTotalsForTrip(tripId: Long): Flow<List<TravelCategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelExpense(expense: TravelExpenseEntity): Long

    @Update
    suspend fun updateTravelExpense(expense: TravelExpenseEntity)

    @Delete
    suspend fun deleteTravelExpense(expense: TravelExpenseEntity)
}

data class TravelCategoryTotal(val category: String, val total: Double)
