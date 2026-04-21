package com.example.eventnote.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.eventnote.data.local.Converters

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@TypeConverters(Converters::class)
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val mediaItems: List<MediaItem> = emptyList(),  // 多媒体附件
    val priority: Priority = Priority.MEDIUM,
    val status: EventStatus = EventStatus.IN_PROGRESS,
    val eventDate: Long = System.currentTimeMillis(),  // 事件日期（默认当天）
    val reminderTime: Long? = null,
    val reminderType: ReminderType = ReminderType.NOTIFICATION,
    val reminderSound: Boolean = true,
    val reminderVibration: Boolean = true,
    val categoryId: Long? = null,  // 分类ID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class Priority {
    HIGH, MEDIUM, LOW
}

enum class EventStatus {
    IN_PROGRESS, COMPLETED, ARCHIVED
}

enum class ReminderType {
    NOTIFICATION,   // 普通通知
    ALARM,          // 闹钟
    SILENT          // 静默提醒
}
