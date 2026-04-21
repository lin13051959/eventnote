package com.example.eventnote.di

import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(): EventDatabase { }
    
    @Provides
    @Singleton 
    fun provideEventDao(database: EventDatabase): EventDao {}
}
