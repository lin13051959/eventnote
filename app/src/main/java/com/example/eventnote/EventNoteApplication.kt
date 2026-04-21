package com.example.eventnote

import android.app.Application
import android.util.Log
import com.example.eventnote.data.local.EventDatabase
import com.example.eventnote.util.NotificationChannelManager
import com.example.eventnote.util.ReminderManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class EventNoteApplication : Application() {
    
    private val TAG = "EventNoteApplication"
    
    override fun onCreate() {
        super.onCreate()
        
        // 创建通知渠道
        NotificationChannelManager.createNotificationChannel(this)
        
        // 应用启动时恢复所有有效提醒
        restoreRemindersOnStartup()
    }
    
    private fun restoreRemindersOnStartup() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 使用正确的 Room 数据库获取方式
                val database = EventDatabase.getInstance(this@EventNoteApplication)
                val eventDao = database.eventDao()
                val allEvents = eventDao.getAllEvents().first()
                
                val currentTime = System.currentTimeMillis()
                // 明确指定参数类型以解决类型推断问题
                val validEvents = allEvents.filter { event: com.example.eventnote.data.local.entity.Event ->
                    event.reminderTime != null && event.reminderTime > currentTime
                }
                
                validEvents.forEach { event: com.example.eventnote.data.local.entity.Event ->
                    try {
                        ReminderManager.setReminder(this@EventNoteApplication, event)
                    } catch (e: Exception) {
                        Log.e(TAG, "恢复提醒失败: " + event.title + ", error: " + e.message)
                    }
                }
                
                Log.d(TAG, "已恢复 " + validEvents.size + " 个有效提醒")
                
            } catch (e: Exception) {
                Log.e(TAG, "恢复提醒失败: " + e.message, e)
            }
        }
    }
}
