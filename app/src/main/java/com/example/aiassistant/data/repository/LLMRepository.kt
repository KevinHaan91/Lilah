package com.example.aiassistant.data.repository

import com.example.aiassistant.data.models.LLMConfig
import com.example.aiassistant.data.models.Message
import kotlinx.coroutines.flow.Flow

interface LLMRepository {
    suspend fun generateResponse(prompt: String, conversationHistory: List<Message>): Result<String>
    suspend fun getActiveConfig(): LLMConfig?
    suspend fun setActiveConfig(configId: String)
    fun getAllConfigs(): Flow<List<LLMConfig>>
    suspend fun saveConfig(config: LLMConfig)
    suspend fun deleteConfig(config: LLMConfig)
    suspend fun getClaudeCompletion(prompt: String, config: LLMConfig): String
}

