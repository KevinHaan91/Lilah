package com.example.aiassistant.data.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for Claude API
 */
data class ClaudeRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    @SerializedName("messages")
    val messages: List<ClaudeMessage>,
    @SerializedName("temperature")
    val temperature: Float? = null,
    @SerializedName("top_k")
    val topK: Int? = null,
    @SerializedName("system")
    val system: String? = null
)

/**
 * Message model for Claude API
 */
data class ClaudeMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

/**
 * Response model for Claude API
 */
data class ClaudeResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: List<ClaudeContent>,
    @SerializedName("model")
    val model: String,
    @SerializedName("stop_reason")
    val stopReason: String?,
    @SerializedName("stop_sequence")
    val stopSequence: String?,
    @SerializedName("usage")
    val usage: ClaudeUsage
)

/**
 * Content model for Claude API response
 */
data class ClaudeContent(
    @SerializedName("type")
    val type: String,
    @SerializedName("text")
    val text: String
)

/**
 * Usage statistics for Claude API response
 */
data class ClaudeUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int
)

