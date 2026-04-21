package com.example.eventnote.data.local.dao

import androidx.room.*
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.Priority
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): Event?

    @Query("SELECT * FROM events WHERE status = :status ORDER BY createdAt DESC")
    fun getEventsByStatus(status: EventStatus): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE priority = :priority ORDER BY createdAt DESC")
    fun getEventsByPriority(priority: Priority): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchEvents(query: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE createdAt >= :startTime AND createdAt < :endTime ORDER BY createdAt DESC")
    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM events WHERE id IN (:ids)")
    suspend fun deleteEventsByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM events")
    fun getEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM events WHERE status = :status")
    fun getEventCountByStatus(status: EventStatus): Flow<Int>
    
    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
}
