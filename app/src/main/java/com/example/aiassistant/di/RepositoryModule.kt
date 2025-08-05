package com.example.aiassistant.di

import com.example.aiassistant.data.repository.LLMRepository
import com.example.aiassistant.data.repository.LLMRepositoryImpl
import com.example.aiassistant.data.repository.MessageRepository
import com.example.aiassistant.data.repository.MessageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLLMRepository(
        llmRepositoryImpl: LLMRepositoryImpl
    ): LLMRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository
}

