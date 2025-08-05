package com.example.aiassistant.utils

import android.content.Context
import com.example.aiassistant.data.models.Message
import com.example.aiassistant.data.repository.LLMRepository
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AgentOrchestrator handles the coordination of agentic capabilities,
 * including tool selection, execution, and response generation.
 */
@Singleton
class AgentOrchestrator @Inject constructor(
    private val context: Context,
    private val llmRepository: LLMRepository,
    private val toolRegistry: ToolRegistry,
    private val gson: Gson = Gson()
) {



    /**
     * Process a user message with potential agentic capabilities
     */
    suspend fun processMessage(
        userMessage: String,
        conversationHistory: List<Message>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create enhanced conversation history with system prompt
            val enhancedHistory = mutableListOf<Message>().apply {
                add(Message(content = ClaudeSystemPrompt.PROMPT, isFromUser = false))
                addAll(conversationHistory.takeLast(10)) // Keep recent context
            }

            // Get initial LLM response
            val llmResult = llmRepository.generateResponse(userMessage, enhancedHistory)
            
            llmResult.fold(
                onSuccess = { response ->
                    // Check if response contains tool usage request
                    val toolRequest = parseToolRequest(response)
                    
                    if (toolRequest != null) {
                        // Execute the requested tool
                        executeToolAndGenerateResponse(toolRequest, userMessage, enhancedHistory)
                    } else {
                        // Return normal text response
                        Result.success(response)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse LLM response to extract tool usage requests
     */
    private fun parseToolRequest(response: String): ToolRequest? {
        return try {
            // Look for JSON in the response
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonString = response.substring(jsonStart, jsonEnd)
                gson.fromJson(jsonString, ToolRequest::class.java)
            } else {
                null
            }
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Execute the requested tool and generate a final response
     */
    private suspend fun executeToolAndGenerateResponse(
        toolRequest: ToolRequest,
        originalMessage: String,
        conversationHistory: List<Message>
    ): Result<String> {
        return try {
            // Execute the tool
            val toolResult = toolRegistry.executeTool(
                toolName = toolRequest.tool_name,
                parameters = toolRequest.parameters
            )

            toolResult.fold(
                onSuccess = { result ->
                    // Generate final response incorporating tool result
                    val followUpPrompt = """
                    The user asked: "$originalMessage"
                    
                    I used the ${toolRequest.tool_name} tool and got this result:
                    $result
                    
                    Please provide a helpful response to the user incorporating this information.
                    Do not mention the tool usage explicitly - just provide a natural response.
                    """.trimIndent()

                    llmRepository.generateResponse(followUpPrompt, conversationHistory)
                },
                onFailure = { error ->
                    // Handle tool execution error
                    val errorPrompt = """
                    I tried to help with: "$originalMessage"
                    
                    However, I encountered an error: ${error.message}
                    
                    Please provide a helpful response explaining that I couldn't complete the requested action.
                    """.trimIndent()

                    llmRepository.generateResponse(errorPrompt, conversationHistory)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class representing a tool usage request from the LLM
 */
data class ToolRequest(
    val reasoning: String,
    val tool_name: String,
    val parameters: Map<String, Any>
)

