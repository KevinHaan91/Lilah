package com.example.aiassistant.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.aiassistant.MainActivity
import com.example.aiassistant.R
import com.example.aiassistant.utils.HotwordDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Background service for voice activation using hotword detection
 * Listens for activation phrases like "Hey Assistant" or "OK Assistant"
 */
@AndroidEntryPoint
class VoiceActivationService : Service() {

    @Inject
    lateinit var hotwordDetector: HotwordDetector

    private var serviceJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioRecord: AudioRecord? = null
    private var isListening = false

    companion object {
        const val CHANNEL_ID = "voice_activation_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_LISTENING = "start_listening"
        const val ACTION_STOP_LISTENING = "stop_listening"
        const val ACTION_TOGGLE_LISTENING = "toggle_listening"
        
        // Audio configuration
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            hotwordDetector.initialize()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_LISTENING -> stopListening()
            ACTION_TOGGLE_LISTENING -> toggleListening()
            else -> startListening()
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Activation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background voice activation for AI Assistant"
                setShowBadge(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceActivationService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Assistant Voice Activation")
            .setContentText(if (isListening) "Listening for activation phrase..." else "Voice activation paused")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop,
                if (isListening) "Stop" else "Start",
                stopPendingIntent
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AIAssistant::VoiceActivationWakeLock"
        )
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }

    private fun startListening() {
        if (isListening) return
        
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_FACTOR

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isListening = true
                
                serviceJob?.cancel()
                serviceJob = CoroutineScope(Dispatchers.Default).launch {
                    processAudioStream()
                }
                
                updateNotification()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopListening()
        }
    }

    private fun stopListening() {
        if (!isListening) return
        
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        serviceJob?.cancel()
        updateNotification()
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private suspend fun processAudioStream() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        
        val audioBuffer = ShortArray(bufferSize)
        
        while (isListening && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                val bytesRead = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                
                if (bytesRead > 0) {
                    // Process audio data for hotword detection
                    val isHotwordDetected = hotwordDetector.processAudioData(audioBuffer, bytesRead)
                    
                    if (isHotwordDetected) {
                        onHotwordDetected()
                    }
                }
                
                // Small delay to prevent excessive CPU usage
                delay(10)
                
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
    }

    private fun onHotwordDetected() {
        // Hotword detected - activate the main app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("voice_activated", true)
        }
        startActivity(intent)
        
        // Send broadcast for other components
        val broadcastIntent = Intent("com.example.aiassistant.HOTWORD_DETECTED")
        sendBroadcast(broadcastIntent)
        
        // Temporarily stop listening to avoid repeated activations
        stopListening()
        
        // Resume listening after a short delay
        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            delay(3000) // 3 second cooldown
            if (!isDestroyed()) {
                startListening()
            }
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun isDestroyed(): Boolean {
        return serviceJob?.isCancelled == true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceJob?.cancel()
        wakeLock?.release()
        hotwordDetector.cleanup()
    }
}

