package com.example.eventnote.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.eventnote.EventNoteRepository
import com.example.eventnote.data.local.EventDatabase
import com.example.eventnote.data.local.dao.CategoryDao
import com.example.eventnote.data.local.dao.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // 数据库迁移: 版本 1 -> 2 (添加分类表)
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 创建分类表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """)
            
            // 在 events 表中添加 categoryId 列
            database.execSQL("ALTER TABLE events ADD COLUMN categoryId INTEGER")
            
            // 创建索引以优化查询
            database.execSQL("CREATE INDEX IF NOT EXISTS index_events_categoryId ON events(categoryId)")
        }
    }
    
    @Provides
    @Singleton
    fun provideEventDatabase(@ApplicationContext context: Context): EventDatabase {
        return Room.databaseBuilder(
            context,
            EventDatabase::class.java,
            EventDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }
    
    @Provides
    @Singleton
    fun provideEventDao(database: EventDatabase): EventDao {
        return database.eventDao()
    }
    
    @Provides
    @Singleton
    fun provideCategoryDao(database: EventDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    @Singleton
    fun provideEventNoteRepository(
        eventDao: EventDao, 
        categoryDao: CategoryDao,
        @ApplicationContext context: Context
    ): EventNoteRepository {
        return EventNoteRepository(eventDao, categoryDao, context)
    }
}