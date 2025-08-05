package com.example.aiassistant.di

import android.content.Context
import com.example.aiassistant.data.local.AppDatabase
import com.example.aiassistant.data.local.LLMConfigDao
import com.example.aiassistant.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideLLMConfigDao(database: AppDatabase): LLMConfigDao {
        return database.llmConfigDao()
    }
}

