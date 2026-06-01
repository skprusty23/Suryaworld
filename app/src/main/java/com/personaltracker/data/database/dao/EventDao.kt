package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE isActive = 1 ORDER BY eventDate ASC")
    fun getAllActiveEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY eventDate ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventDate = :date AND isActive = 1 ORDER BY title ASC")
    fun getEventsByDate(date: LocalDate): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE eventDate >= :today
          AND eventDate <= :future
          AND isActive = 1
        ORDER BY eventDate ASC
    """)
    fun getUpcomingEvents(today: LocalDate, future: LocalDate): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventDate = :today AND isActive = 1")
    suspend fun getEventsForToday(today: LocalDate): List<EventEntity>

    @Query("SELECT * FROM events WHERE category = :category AND isActive = 1 ORDER BY eventDate ASC")
    fun getEventsByCategory(category: String): Flow<List<EventEntity>>

    @Query("SELECT DISTINCT category FROM events ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)
}
