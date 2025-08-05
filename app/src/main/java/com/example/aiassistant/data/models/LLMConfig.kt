package com.example.aiassistant.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_configs")
data class LLMConfig(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: LLMType,
    val modelPath: String? = null, // For local models
    val apiEndpoint: String? = null, // For remote models
    val apiKey: String? = null, // For remote models
    val isActive: Boolean = false,
    val maxTokens: Int = 512,
    val temperature: Float = 0.8f,
    val topK: Int = 40
)

enum class LLMType {
    LOCAL,
    REMOTE_OPENAI,
    REMOTE_GEMINI,
    REMOTE_CLAUDE,
    REMOTE_CUSTOM
}

