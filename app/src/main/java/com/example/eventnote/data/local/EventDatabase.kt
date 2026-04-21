package com.example.eventnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.eventnote.data.local.dao.CategoryDao
import com.example.eventnote.data.local.dao.EventDao
import com.example.eventnote.data.local.entity.Category
import com.example.eventnote.data.local.entity.Event

@Database(
    entities = [Event::class, Category::class],
    version = 2,  // 升级版本
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null
        
        fun getInstance(context: Context): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
        const val DATABASE_NAME = "event_database"
    }
}