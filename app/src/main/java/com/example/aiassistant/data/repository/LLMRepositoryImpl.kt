package com.example.aiassistant.data.repository

import android.content.Context
import com.example.aiassistant.data.local.LLMConfigDao
import com.example.aiassistant.data.models.LLMConfig
import com.example.aiassistant.data.models.LLMType
import com.example.aiassistant.data.models.Message
import com.example.aiassistant.data.remote.ChatCompletionRequest
import com.example.aiassistant.data.remote.ChatMessage
import com.example.aiassistant.data.remote.OpenAIService
import com.example.aiassistant.data.models.ClaudeRequest
import com.example.aiassistant.data.models.ClaudeMessage
import com.example.aiassistant.data.remote.ClaudeService
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMRepositoryImpl @Inject constructor(
    private val context: Context,
    private val llmConfigDao: LLMConfigDao,
    private val openAIService: OpenAIService,
    private val claudeService: ClaudeService
) : LLMRepository {

    private var localLlmInference: LlmInference? = null

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<Message>
    ): Result<String> {
        return try {
            val activeConfig = getActiveConfig()
                ?: return Result.failure(Exception("No active LLM configuration found"))

            when (activeConfig.type) {
                LLMType.LOCAL -> generateLocalResponse(prompt, activeConfig)
                LLMType.REMOTE_OPENAI -> generateOpenAIResponse(prompt, conversationHistory, activeConfig)
                LLMType.REMOTE_GEMINI -> generateGeminiResponse(prompt, conversationHistory, activeConfig)
                LLMType.REMOTE_CLAUDE -> Result.success(getClaudeCompletion(prompt, activeConfig))
                LLMType.REMOTE_CUSTOM -> generateCustomResponse(prompt, conversationHistory, activeConfig)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateLocalResponse(prompt: String, config: LLMConfig): Result<String> {
        return try {
            if (localLlmInference == null) {
                initializeLocalModel(config)
            }
            
            val response = localLlmInference?.generateResponse(prompt)
                ?: return Result.failure(Exception("Failed to generate response from local model"))
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun initializeLocalModel(config: LLMConfig) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(config.modelPath ?: "")
                .setMaxTokens(config.maxTokens)
                .setTemperature(config.temperature)
                .setTopK(config.topK)
                .build()

            localLlmInference = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            throw Exception("Failed to initialize local model: ${e.message}")
        }
    }

    private suspend fun generateOpenAIResponse(
        prompt: String,
        conversationHistory: List<Message>,
        config: LLMConfig
    ): Result<String> {
        return try {
            val messages = mutableListOf<ChatMessage>()
            
            // Add conversation history
            conversationHistory.takeLast(10).forEach { message ->
                messages.add(
                    ChatMessage(
                        role = if (message.isFromUser) "user" else "assistant",
                        content = message.content
                    )
                )
            }
            
            // Add current prompt
            messages.add(ChatMessage(role = "user", content = prompt))

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = messages,
                max_tokens = config.maxTokens,
                temperature = config.temperature
            )

            val response = openAIService.createChatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                val content = responseBody?.choices?.firstOrNull()?.message?.content
                    ?: return Result.failure(Exception("Empty response from OpenAI"))
                Result.success(content)
            } else {
                Result.failure(Exception("OpenAI API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateGeminiResponse(
        prompt: String,
        conversationHistory: List<Message>,
        config: LLMConfig
    ): Result<String> {
        // TODO: Implement Gemini API integration
        return Result.failure(Exception("Gemini integration not yet implemented"))
    }

    private suspend fun generateCustomResponse(
        prompt: String,
        conversationHistory: List<Message>,
        config: LLMConfig
    ): Result<String> {
        // TODO: Implement custom API integration
        return Result.failure(Exception("Custom API integration not yet implemented"))
    }

    override suspend fun getActiveConfig(): LLMConfig? {
        return llmConfigDao.getActiveConfig()
    }

    override suspend fun setActiveConfig(configId: String) {
        llmConfigDao.deactivateAllConfigs()
        llmConfigDao.activateConfig(configId)
        
        // Reset local model if switching away from local
        localLlmInference?.close()
        localLlmInference = null
    }

    override fun getAllConfigs(): Flow<List<LLMConfig>> {
        return llmConfigDao.getAllConfigs()
    }

    override suspend fun saveConfig(config: LLMConfig) {
        llmConfigDao.insertConfig(config)
    }

    override suspend fun deleteConfig(config: LLMConfig) {
        llmConfigDao.deleteConfig(config)
    }
}



    override suspend fun getClaudeCompletion(prompt: String, config: LLMConfig): String {
        return try {
            val messages = mutableListOf<ClaudeMessage>()
            messages.add(ClaudeMessage(role = "user", content = prompt))

            val request = ClaudeRequest(
                model = config.modelName ?: "claude-sonnet-3.5-20240620", // Default to Sonnet 3.5
                maxTokens = config.maxTokens,
                messages = messages,
                temperature = config.temperature,
                topK = config.topK
            )

            val response = claudeService.createMessage(
                apiKey = config.apiKey ?: "",
                request = request
            )

            response.content.firstOrNull()?.text
                ?: throw Exception("Empty response from Claude")
        } catch (e: Exception) {
            throw Exception("Claude API error: ${e.message}")
        }
    }


