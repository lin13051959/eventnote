package com.example.eventnote.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.eventnote.MainActivity
import com.example.eventnote.R
import com.example.eventnote.util.NotificationChannelManager

/**
 * 事件提醒接收器 - 接收闹钟提醒并显示通知
 * 
 * 当事件提醒时间到达时，系统会触发此接收器显示通知
 */
class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("event_id", -1)
        val eventTitle = intent.getStringExtra("event_title") ?: "事件提醒"
        val eventContent = intent.getStringExtra("event_content") ?: "您有一个事件到期了"
        val playSound = intent.getBooleanExtra("play_sound", true)
        val vibrate = intent.getBooleanExtra("vibrate", true)
        
        if (eventId == -1L) return
        
        // 创建通知渠道
        NotificationChannelManager.createNotificationChannel(context)
        
        // 创建点击通知的意图 - 打开应用并显示事件详情
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("event_id", eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannelManager.EVENT_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 使用专门的24dp通知图标
            .setContentTitle(eventTitle)
            .setContentText(eventContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(eventContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
        
        // 设置声音和震动 - 根据用户设置
        if (playSound) {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }
        
        if (vibrate) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }
        
        // 最重要的：设置锁屏和通知灯
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        // 显示通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId.toInt(), notificationBuilder.build())
    }
}
