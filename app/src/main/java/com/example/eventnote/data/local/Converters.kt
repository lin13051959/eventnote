package com.example.eventnote.data.local

import androidx.room.TypeConverter
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.MediaItem
import com.example.eventnote.data.local.entity.MediaType
import com.example.eventnote.data.local.entity.Priority

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(value: String): Priority {
        return Priority.valueOf(value)
    }

    @TypeConverter
    fun fromEventStatus(status: EventStatus): String {
        return status.name
    }

    @TypeConverter
    fun toEventStatus(value: String): EventStatus {
        return EventStatus.valueOf(value)
    }

    @TypeConverter
    fun fromTagsList(tags: List<String>): String {
        return tags.joinToString(separator = ",")
    }

    @TypeConverter
    fun toTagsList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    // MediaItem List 转换器
    @TypeConverter
    fun fromMediaItemList(mediaItems: List<MediaItem>): String {
        if (mediaItems.isEmpty()) return ""
        
        return mediaItems.joinToString(";;") { item ->
            listOf(
                item.id,
                item.uri,
                item.type.name,
                item.name,
                item.mimeType,
                item.size.toString(),
                item.duration?.toString() ?: "",
                item.createdAt.toString()
            ).joinToString("|")
        }
    }

    @TypeConverter
    fun toMediaItemList(data: String): List<MediaItem> {
        if (data.isBlank()) return emptyList()
        
        return try {
            data.split(";;").mapNotNull { itemStr ->
                val parts = itemStr.split("|")
                if (parts.size < 5) return@mapNotNull null
                
                MediaItem(
                    id = parts.getOrElse(0) { "" },
                    uri = parts.getOrElse(1) { "" },
                    type = try { MediaType.valueOf(parts.getOrElse(2) { "IMAGE" }) } catch (e: Exception) { MediaType.IMAGE },
                    name = parts.getOrElse(3) { "" },
                    mimeType = parts.getOrElse(4) { "" },
                    size = parts.getOrElse(5) { "0" }.toLongOrNull() ?: 0L,
                    duration = parts.getOrElse(6) { "" }.toLongOrNull(),
                    createdAt = parts.getOrElse(7) { "" }.toLongOrNull() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
