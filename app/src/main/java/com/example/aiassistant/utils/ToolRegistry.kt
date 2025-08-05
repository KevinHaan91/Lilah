package com.example.aiassistant.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ToolRegistry manages the available tools and their execution
 */
@Singleton
class ToolRegistry @Inject constructor(
    private val context: Context
) {

    @Inject
    lateinit var phoneController: PhoneController

    private val tools by lazy { mapOf(
        "web_search" to WebSearchTool(),
        "calculator" to CalculatorTool(),
        "weather" to WeatherTool(),
        "time" to TimeTool(),
        "phone_control" to PhoneControlTool(phoneController),
        "screenshot" to ScreenshotTool(phoneController),
        "app_control" to AppControlTool(phoneController)
    )}

    /**
     * Execute a tool with given parameters
     */
    suspend fun executeTool(toolName: String, parameters: Map<String, Any>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val tool = tools[toolName]
                    ?: return@withContext Result.failure(Exception("Tool '$toolName' not found"))

                tool.execute(parameters)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get list of available tools
     */
    fun getAvailableTools(): List<String> = tools.keys.toList()
}

/**
 * Base interface for all tools
 */
interface Tool {
    suspend fun execute(parameters: Map<String, Any>): Result<String>
}

/**
 * Web search tool (placeholder implementation)
 */
class WebSearchTool : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        val query = parameters["query"]?.toString()
            ?: return Result.failure(Exception("Missing 'query' parameter"))

        // Placeholder implementation - in a real app, this would use a search API
        return Result.success(
            "I would search for '$query' on the web, but web search functionality " +
            "is not implemented in this demo. In a production app, this would integrate " +
            "with search APIs like Google Search API or Bing Search API."
        )
    }
}

/**
 * Calculator tool for mathematical operations
 */
class CalculatorTool : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        val expression = parameters["expression"]?.toString()
            ?: return Result.failure(Exception("Missing 'expression' parameter"))

        return try {
            val result = evaluateExpression(expression)
            Result.success("The result of '$expression' is: $result")
        } catch (e: Exception) {
            Result.failure(Exception("Error evaluating expression: ${e.message}"))
        }
    }

    /**
     * Simple expression evaluator for basic math operations
     */
    private fun evaluateExpression(expression: String): Double {
        // Remove spaces
        val cleanExpression = expression.replace(" ", "")
        
        // Handle basic operations - this is a simplified implementation
        // In production, you'd use a proper expression parser
        return when {
            cleanExpression.contains("+") -> {
                val parts = cleanExpression.split("+")
                parts.sumOf { it.toDouble() }
            }
            cleanExpression.contains("-") && !cleanExpression.startsWith("-") -> {
                val parts = cleanExpression.split("-")
                parts.drop(1).fold(parts[0].toDouble()) { acc, part -> acc - part.toDouble() }
            }
            cleanExpression.contains("*") -> {
                val parts = cleanExpression.split("*")
                parts.fold(1.0) { acc, part -> acc * part.toDouble() }
            }
            cleanExpression.contains("/") -> {
                val parts = cleanExpression.split("/")
                parts.drop(1).fold(parts[0].toDouble()) { acc, part -> acc / part.toDouble() }
            }
            cleanExpression.startsWith("sqrt(") && cleanExpression.endsWith(")") -> {
                val number = cleanExpression.substring(5, cleanExpression.length - 1).toDouble()
                sqrt(number)
            }
            cleanExpression.startsWith("sin(") && cleanExpression.endsWith(")") -> {
                val number = cleanExpression.substring(4, cleanExpression.length - 1).toDouble()
                sin(Math.toRadians(number))
            }
            cleanExpression.startsWith("cos(") && cleanExpression.endsWith(")") -> {
                val number = cleanExpression.substring(4, cleanExpression.length - 1).toDouble()
                cos(Math.toRadians(number))
            }
            else -> cleanExpression.toDouble() // Simple number
        }
    }
}

/**
 * Weather tool (placeholder implementation)
 */
class WeatherTool : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        val location = parameters["location"]?.toString()
            ?: return Result.failure(Exception("Missing 'location' parameter"))

        // Placeholder implementation - in a real app, this would use a weather API
        return Result.success(
            "I would get weather information for '$location', but weather functionality " +
            "is not implemented in this demo. In a production app, this would integrate " +
            "with weather APIs like OpenWeatherMap or WeatherAPI."
        )
    }
}

/**
 * Time tool for getting current time
 */
class TimeTool : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        val timezone = parameters["timezone"]?.toString()

        return try {
            val timeZone = if (timezone != null) {
                TimeZone.getTimeZone(timezone)
            } else {
                TimeZone.getDefault()
            }

            val calendar = Calendar.getInstance(timeZone)
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            formatter.timeZone = timeZone

            val currentTime = formatter.format(calendar.time)
            
            Result.success(
                if (timezone != null) {
                    "The current time in $timezone is: $currentTime"
                } else {
                    "The current local time is: $currentTime"
                }
            )
        } catch (e: Exception) {
            Result.failure(Exception("Error getting time: ${e.message}"))
        }
    }
}



/**
 * Phone control tool for system-level device control
 */
class PhoneControlTool(private val phoneController: PhoneController) : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        val action = parameters["action"]?.toString()
            ?: return Result.failure(Exception("Missing 'action' parameter"))

        return when (action.lowercase()) {
            "tap" -> {
                val x = parameters["x"]?.toString()?.toFloatOrNull()
                val y = parameters["y"]?.toString()?.toFloatOrNull()
                if (x != null && y != null) {
                    phoneController.tapAtCoordinates(x, y)
                } else {
                    Result.failure(Exception("Missing or invalid x, y coordinates"))
                }
            }
            "swipe" -> {
                val startX = parameters["start_x"]?.toString()?.toFloatOrNull()
                val startY = parameters["start_y"]?.toString()?.toFloatOrNull()
                val endX = parameters["end_x"]?.toString()?.toFloatOrNull()
                val endY = parameters["end_y"]?.toString()?.toFloatOrNull()
                if (startX != null && startY != null && endX != null && endY != null) {
                    phoneController.swipe(startX, startY, endX, endY)
                } else {
                    Result.failure(Exception("Missing or invalid swipe coordinates"))
                }
            }
            "type" -> {
                val text = parameters["text"]?.toString()
                if (text != null) {
                    phoneController.typeText(text)
                } else {
                    Result.failure(Exception("Missing 'text' parameter"))
                }
            }
            "back" -> phoneController.pressButton(SystemButton.BACK)
            "home" -> phoneController.pressButton(SystemButton.HOME)
            "recent" -> phoneController.pressButton(SystemButton.RECENT_APPS)
            "call" -> {
                val number = parameters["number"]?.toString()
                if (number != null) {
                    phoneController.makeCall(number)
                } else {
                    Result.failure(Exception("Missing 'number' parameter"))
                }
            }
            "sms" -> {
                val number = parameters["number"]?.toString()
                val message = parameters["message"]?.toString()
                if (number != null && message != null) {
                    phoneController.sendSMS(number, message)
                } else {
                    Result.failure(Exception("Missing 'number' or 'message' parameter"))
                }
            }
            "volume" -> {
                val type = parameters["type"]?.toString()?.let { 
                    try { VolumeType.valueOf(it.uppercase()) } catch (e: Exception) { null }
                }
                val level = parameters["level"]?.toString()?.toIntOrNull()
                if (type != null && level != null) {
                    phoneController.setVolume(type, level)
                } else {
                    Result.failure(Exception("Missing or invalid 'type' or 'level' parameter"))
                }
            }
            else -> Result.failure(Exception("Unknown action: $action"))
        }
    }
}

/**
 * Screenshot tool for capturing screen content
 */
class ScreenshotTool(private val phoneController: PhoneController) : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        return phoneController.takeScreenshot()
    }
}

/**
 * App control tool for managing applications
 */
class AppControlTool(private val phoneController: PhoneController) : Tool {
    override suspend fun execute(parameters: Map<String, Any>): Result<String> {
        val action = parameters["action"]?.toString()
            ?: return Result.failure(Exception("Missing 'action' parameter"))

        return when (action.lowercase()) {
            "open" -> {
                val packageName = parameters["package"]?.toString()
                if (packageName != null) {
                    phoneController.openApp(packageName)
                } else {
                    Result.failure(Exception("Missing 'package' parameter"))
                }
            }
            "list" -> {
                phoneController.getInstalledApps().fold(
                    onSuccess = { apps ->
                        val appList = apps.take(20).joinToString("\n") { "${it.name} (${it.packageName})" }
                        Result.success("Installed apps:\n$appList")
                    },
                    onFailure = { Result.failure(it) }
                )
            }
            "screen_content" -> phoneController.getScreenContent()
            "find" -> {
                val text = parameters["text"]?.toString()
                if (text != null) {
                    phoneController.findElementByText(text).fold(
                        onSuccess = { elements ->
                            if (elements.isNotEmpty()) {
                                val elementList = elements.joinToString("\n") { 
                                    "Text: ${it.text}, Description: ${it.description}, Bounds: ${it.bounds}"
                                }
                                Result.success("Found elements:\n$elementList")
                            } else {
                                Result.success("No elements found with text: $text")
                            }
                        },
                        onFailure = { Result.failure(it) }
                    )
                } else {
                    Result.failure(Exception("Missing 'text' parameter"))
                }
            }
            else -> Result.failure(Exception("Unknown action: $action"))
        }
    }
}

