package com.example.eventnote.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.receiver.AlarmReceiver
import java.util.Calendar

/**
 * 提醒管理器 - 管理事件提醒的创建和取消
 * 
 * 使用AlarmManager设置定时提醒
 */
object ReminderManager {
    
    private const val TAG = "ReminderManager"
    
    /**
     * 检查是否可以设置精确闹钟
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * 获取精确闹钟设置意图（引导用户开启权限）
     */
    fun getExactAlarmSettingsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            null
        }
    }
    
    /**
     * 设置事件提醒
     */
    fun setReminder(context: Context, event: Event) {
        // 检查是否需要设置提醒
        if (event.reminderTime == null || event.reminderTime!! <= System.currentTimeMillis()) {
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 创建提醒Intent
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_content", event.content ?: "您有一个事件到期了")
            putExtra("play_sound", event.reminderSound)
            putExtra("vibrate", event.reminderVibration)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 设置闹钟
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要检查闹钟权限
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, 
                        event.reminderTime!!, 
                        pendingIntent
                    )
                } else {
                    // 如果没有权限，使用非精确闹钟作为后备
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, 
                        event.reminderTime!!, 
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, 
                    event.reminderTime!!, 
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // 如果setExact失败，使用setAndAllowWhileIdle
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, 
                event.reminderTime!!, 
                pendingIntent
            )
        }
    }
    
    /**
     * 取消事件提醒
     */
    fun cancelReminder(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * 恢复所有有效提醒（用于开机后）
     */
    suspend fun restoreAllReminders(context: Context, events: List<Event>) {
        events.forEach { event ->
            if (event.reminderTime != null && event.reminderTime!! > System.currentTimeMillis()) {
                setReminder(context, event)
            }
        }
    }
}
