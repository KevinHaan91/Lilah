package com.example.aiassistant.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PhoneController provides system-level control capabilities for Android devices
 * Similar to Claude's computer use functionality but for mobile devices
 */
@Singleton
class PhoneController @Inject constructor(
    private val context: Context
) {

    companion object {
        const val ACCESSIBILITY_SERVICE_NAME = "com.example.aiassistant.services.AIAccessibilityService"
    }

    private var accessibilityService: AIAccessibilityService? = null

    /**
     * Register the accessibility service instance
     */
    fun registerAccessibilityService(service: AIAccessibilityService) {
        this.accessibilityService = service
    }

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(ACCESSIBILITY_SERVICE_NAME) == true
    }

    /**
     * Open accessibility settings to enable the service
     */
    fun openAccessibilitySettings(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Take a screenshot of the current screen
     */
    suspend fun takeScreenshot(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val screenshotPath = accessibilityService?.takeScreenshot()
            if (screenshotPath != null) {
                Result.success(screenshotPath)
            } else {
                Result.failure(Exception("Failed to take screenshot. Accessibility service may not be enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tap at specific coordinates on the screen
     */
    suspend fun tapAtCoordinates(x: Float, y: Float): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = accessibilityService?.performTap(x, y) ?: false
            if (success) {
                Result.success("Tapped at coordinates ($x, $y)")
            } else {
                Result.failure(Exception("Failed to tap at coordinates. Accessibility service may not be enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Swipe from one point to another
     */
    suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 500): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = accessibilityService?.performSwipe(startX, startY, endX, endY, duration) ?: false
            if (success) {
                Result.success("Swiped from ($startX, $startY) to ($endX, $endY)")
            } else {
                Result.failure(Exception("Failed to perform swipe. Accessibility service may not be enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Type text into the currently focused input field
     */
    suspend fun typeText(text: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = accessibilityService?.typeText(text) ?: false
            if (success) {
                Result.success("Typed text: $text")
            } else {
                Result.failure(Exception("Failed to type text. No input field focused or accessibility service not enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Press hardware buttons (back, home, recent apps)
     */
    suspend fun pressButton(button: SystemButton): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = accessibilityService?.pressButton(button) ?: false
            if (success) {
                Result.success("Pressed ${button.name} button")
            } else {
                Result.failure(Exception("Failed to press ${button.name} button. Accessibility service may not be enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Open an application by package name
     */
    suspend fun openApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Result.success("Opened app: $packageName")
            } else {
                Result.failure(Exception("App not found: $packageName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of installed applications
     */
    suspend fun getInstalledApps(): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            val apps = packages.mapNotNull { packageInfo ->
                try {
                    val appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
                    val packageName = packageInfo.packageName
                    val isSystemApp = (packageInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    
                    AppInfo(appName, packageName, isSystemApp)
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.name }
            
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current screen content description
     */
    suspend fun getScreenContent(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val content = accessibilityService?.getScreenContent()
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Failed to get screen content. Accessibility service may not be enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find UI elements by text or description
     */
    suspend fun findElementByText(text: String): Result<List<ElementInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val elements = accessibilityService?.findElementsByText(text) ?: emptyList()
            Result.success(elements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Make a phone call
     */
    suspend fun makeCall(phoneNumber: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Initiated call to: $phoneNumber")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send SMS message
     */
    suspend fun sendSMS(phoneNumber: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Opened SMS composer for: $phoneNumber")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Open camera
     */
    suspend fun openCamera(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Opened camera")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set device volume
     */
    suspend fun setVolume(volumeType: VolumeType, level: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val streamType = when (volumeType) {
                VolumeType.MEDIA -> android.media.AudioManager.STREAM_MUSIC
                VolumeType.RING -> android.media.AudioManager.STREAM_RING
                VolumeType.ALARM -> android.media.AudioManager.STREAM_ALARM
                VolumeType.NOTIFICATION -> android.media.AudioManager.STREAM_NOTIFICATION
            }
            
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val targetVolume = (level.coerceIn(0, 100) * maxVolume / 100)
            
            audioManager.setStreamVolume(streamType, targetVolume, 0)
            Result.success("Set ${volumeType.name} volume to $level%")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle WiFi (requires system permissions)
     */
    suspend fun toggleWiFi(enable: Boolean): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Opened WiFi settings (manual toggle required)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get device information
     */
    suspend fun getDeviceInfo(): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceInfo = DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                screenDensity = context.resources.displayMetrics.density,
                screenWidth = context.resources.displayMetrics.widthPixels,
                screenHeight = context.resources.displayMetrics.heightPixels
            )
            Result.success(deviceInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * System buttons that can be pressed
 */
enum class SystemButton {
    BACK, HOME, RECENT_APPS, POWER, VOLUME_UP, VOLUME_DOWN
}

/**
 * Volume types for audio control
 */
enum class VolumeType {
    MEDIA, RING, ALARM, NOTIFICATION
}

/**
 * Information about an installed app
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean
)

/**
 * Information about a UI element
 */
data class ElementInfo(
    val text: String?,
    val description: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val className: String?
)

/**
 * Device information
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val screenDensity: Float,
    val screenWidth: Int,
    val screenHeight: Int
)

