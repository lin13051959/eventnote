package com.example.eventnote.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * 通知渠道管理器 - 创建和管理通知渠道
 * 
 * Android 8.0+ 需要创建通知渠道才能显示通知
 */
object NotificationChannelManager {
    
    // 事件提醒通知渠道ID
    const val EVENT_REMINDER_CHANNEL_ID = "event_reminder_channel"
    
    // 渠道名称和描述
    private const val CHANNEL_NAME = "事件提醒"
    private const val CHANNEL_DESCRIPTION = "事件到期提醒通知"
    
    /**
     * 创建通知渠道（仅Android 8.0+需要）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                EVENT_REMINDER_CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
                // 显示徽章（图标圆点）
                setShowBadge(true)
                // 启用震动
                enableVibration(true)
                // 震动模式
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                // 在锁屏上显示
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // 设置默认提示音
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            
            // 注册通知渠道
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 检查通知是否启用
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * 检查通知渠道是否启用
     */
    fun isNotificationChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }
}
