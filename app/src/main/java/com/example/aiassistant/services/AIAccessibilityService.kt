package com.example.aiassistant.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.aiassistant.utils.ElementInfo
import com.example.aiassistant.utils.PhoneController
import com.example.aiassistant.utils.SystemButton
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Accessibility service that provides system-level control capabilities
 */
@AndroidEntryPoint
class AIAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var phoneController: PhoneController

    override fun onServiceConnected() {
        super.onServiceConnected()
        phoneController.registerAccessibilityService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
        // This can be used to monitor app changes, UI updates, etc.
    }

    override fun onInterrupt() {
        // Handle service interruption
    }

    /**
     * Take a screenshot and save it to internal storage
     */
    fun takeScreenshot(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val screenshotDir = File(filesDir, "screenshots")
                if (!screenshotDir.exists()) {
                    screenshotDir.mkdirs()
                }
                
                val screenshotFile = File(screenshotDir, "screenshot_${System.currentTimeMillis()}.png")
                
                takeScreenshot(
                    GLOBAL_ACTION_TAKE_SCREENSHOT,
                    { bitmap ->
                        try {
                            FileOutputStream(screenshotFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    null
                )
                
                screenshotFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null // Screenshots require API 30+
        }
    }

    /**
     * Perform a tap gesture at the specified coordinates
     */
    fun performTap(x: Float, y: Float): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            
            dispatchGesture(gesture, null, null)
        } else {
            false
        }
    }

    /**
     * Perform a swipe gesture
     */
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            dispatchGesture(gesture, null, null)
        } else {
            false
        }
    }

    /**
     * Type text into the currently focused input field
     */
    fun typeText(text: String): Boolean {
        val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focusedNode != null && focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            false
        }
    }

    /**
     * Press system buttons
     */
    fun pressButton(button: SystemButton): Boolean {
        return when (button) {
            SystemButton.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            SystemButton.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            SystemButton.RECENT_APPS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            SystemButton.POWER -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            else -> false // Volume buttons require different handling
        }
    }

    /**
     * Get a description of the current screen content
     */
    fun getScreenContent(): String {
        val rootNode = rootInActiveWindow ?: return "No active window"
        return buildScreenDescription(rootNode)
    }

    /**
     * Find UI elements by text content
     */
    fun findElementsByText(text: String): List<ElementInfo> {
        val rootNode = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ElementInfo>()
        findElementsByTextRecursive(rootNode, text, elements)
        return elements
    }

    private fun buildScreenDescription(node: AccessibilityNodeInfo, depth: Int = 0): String {
        val indent = "  ".repeat(depth)
        val description = StringBuilder()
        
        // Add node information
        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        val className = node.className?.toString()
        
        if (!text.isNullOrBlank() || !contentDesc.isNullOrBlank()) {
            description.append("$indent")
            
            if (!text.isNullOrBlank()) {
                description.append("Text: \"$text\"")
            }
            
            if (!contentDesc.isNullOrBlank()) {
                if (description.isNotEmpty() && !description.endsWith(indent)) {
                    description.append(", ")
                }
                description.append("Description: \"$contentDesc\"")
            }
            
            if (node.isClickable) {
                description.append(" [Clickable]")
            }
            
            if (node.isScrollable) {
                description.append(" [Scrollable]")
            }
            
            description.append(" ($className)\n")
        }
        
        // Recursively process child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                description.append(buildScreenDescription(child, depth + 1))
                child.recycle()
            }
        }
        
        return description.toString()
    }

    private fun findElementsByTextRecursive(
        node: AccessibilityNodeInfo,
        searchText: String,
        results: MutableList<ElementInfo>
    ) {
        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        
        if ((text != null && text.contains(searchText, ignoreCase = true)) ||
            (contentDesc != null && contentDesc.contains(searchText, ignoreCase = true))) {
            
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            results.add(
                ElementInfo(
                    text = text,
                    description = contentDesc,
                    bounds = bounds,
                    isClickable = node.isClickable,
                    isScrollable = node.isScrollable,
                    className = node.className?.toString()
                )
            )
        }
        
        // Recursively search child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findElementsByTextRecursive(child, searchText, results)
                child.recycle()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
    }
}

