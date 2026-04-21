package com.example.eventnote

import android.content.Context
import com.example.eventnote.data.local.dao.CategoryDao
import com.example.eventnote.data.local.dao.EventDao
import com.example.eventnote.data.local.entity.Category
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.Priority
import com.example.eventnote.util.ReminderManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventNoteRepository @Inject constructor(
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val context: Context
) {
    
    // ===== Event Methods =====
    
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()
    
    suspend fun getEventById(id: Long): Event? = eventDao.getEventById(id)
    
    fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = 
        eventDao.getEventsByStatus(status)
    
    fun getEventsByPriority(priority: Priority): Flow<List<Event>> = 
        eventDao.getEventsByPriority(priority)
    
    fun searchEvents(query: String): Flow<List<Event>> = 
        eventDao.searchEvents(query)
    
    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<Event>> = 
        eventDao.getEventsByDateRange(startTime, endTime)
    
    suspend fun insertEvent(event: Event): Long {
        val id = eventDao.insertEvent(event)
        // 如果是新插入的事件且有提醒时间，设置提醒
        if (event.reminderTime != null && event.reminderTime!! > System.currentTimeMillis()) {
            ReminderManager.setReminder(context, event.copy(id = id))
        }
        return id
    }
    
    suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
        // 更新事件后，重新设置提醒（如果有提醒时间）
        if (event.reminderTime != null && event.reminderTime!! > System.currentTimeMillis()) {
            ReminderManager.setReminder(context, event)
        } else {
            // 如果提醒时间被删除，取消提醒
            ReminderManager.cancelReminder(context, event.id)
        }
    }
    
    suspend fun deleteEvent(event: Event) {
        ReminderManager.cancelReminder(context, event.id)
        eventDao.deleteEvent(event)
    }
    
    suspend fun deleteEvent(id: Long) {
        ReminderManager.cancelReminder(context, id)
        eventDao.deleteEventById(id)
    }
    
    suspend fun deleteEventsByIds(ids: List<Long>) {
        ids.forEach { ReminderManager.cancelReminder(context, it) }
        eventDao.deleteEventsByIds(ids)
    }
    
    fun getEventCount(): Flow<Int> = eventDao.getEventCount()
    
    fun getEventCountByStatus(status: EventStatus): Flow<Int> = 
        eventDao.getEventCountByStatus(status)
    
    suspend fun deleteAllEvents() = eventDao.deleteAllEvents()
    
    suspend fun insertEvents(events: List<Event>) {
        events.forEach { eventDao.insertEvent(it) }
    }
    
    // ===== Category Methods =====
    
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
    
    suspend fun deleteCategoryById(id: Long) = categoryDao.deleteCategoryById(id)
    
    fun getCategoryCount(): Flow<Int> = categoryDao.getCategoryCount()
}
