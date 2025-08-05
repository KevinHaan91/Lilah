package com.example.aiassistant.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * HotwordDetector implements a simple keyword spotting system
 * for detecting activation phrases like "Hey Assistant" or "OK Assistant"
 */
@Singleton
class HotwordDetector @Inject constructor(
    private val context: Context
) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences("hotword_prefs", Context.MODE_PRIVATE)
    
    // Audio processing parameters
    private val sampleRate = 16000
    private val frameSize = 512
    private val hopSize = 256
    private val numMelFilters = 40
    private val numMfccCoeffs = 13
    
    // Detection parameters
    private val detectionThreshold = 0.7f
    private val activationPhrases = listOf("hey assistant", "ok assistant", "hello assistant")
    
    // Audio buffers and processing
    private val audioBuffer = mutableListOf<Short>()
    private val featureBuffer = mutableListOf<FloatArray>()
    private var isInitialized = false
    
    // Simple template matching for hotword detection
    private val hotwordTemplates = mutableMapOf<String, List<FloatArray>>()
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            loadHotwordTemplates()
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
        }
    }
    
    /**
     * Process incoming audio data and detect hotwords
     */
    fun processAudioData(audioData: ShortArray, length: Int): Boolean {
        if (!isInitialized) return false
        
        // Add new audio data to buffer
        for (i in 0 until length) {
            audioBuffer.add(audioData[i])
        }
        
        // Process audio in frames
        while (audioBuffer.size >= frameSize) {
            val frame = audioBuffer.take(frameSize).toShortArray()
            audioBuffer.removeAll(audioBuffer.take(hopSize))
            
            // Extract features from audio frame
            val features = extractMfccFeatures(frame)
            featureBuffer.add(features)
            
            // Keep only recent features for detection window
            if (featureBuffer.size > 100) { // ~2.5 seconds at 16kHz with 256 hop size
                featureBuffer.removeAt(0)
            }
            
            // Check for hotword detection
            if (featureBuffer.size >= 20) { // Minimum frames for detection
                val isDetected = detectHotword()
                if (isDetected) {
                    featureBuffer.clear() // Clear buffer after detection
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Extract MFCC features from audio frame
     */
    private fun extractMfccFeatures(audioFrame: ShortArray): FloatArray {
        // Convert to float and normalize
        val floatFrame = audioFrame.map { it.toFloat() / 32768.0f }.toFloatArray()
        
        // Apply window function (Hamming window)
        for (i in floatFrame.indices) {
            floatFrame[i] *= (0.54 - 0.46 * cos(2.0 * PI * i / (floatFrame.size - 1))).toFloat()
        }
        
        // Compute FFT (simplified - in production use proper FFT library)
        val fftResult = computeSimpleFFT(floatFrame)
        
        // Compute mel-scale filter bank
        val melFilters = computeMelFilters(fftResult)
        
        // Compute MFCC coefficients
        return computeMfccCoeffs(melFilters)
    }
    
    /**
     * Simplified FFT computation (for demonstration - use proper FFT in production)
     */
    private fun computeSimpleFFT(signal: FloatArray): FloatArray {
        val n = signal.size
        val magnitude = FloatArray(n / 2)
        
        for (k in 0 until n / 2) {
            var real = 0.0f
            var imag = 0.0f
            
            for (i in signal.indices) {
                val angle = -2.0 * PI * k * i / n
                real += signal[i] * cos(angle).toFloat()
                imag += signal[i] * sin(angle).toFloat()
            }
            
            magnitude[k] = sqrt(real * real + imag * imag)
        }
        
        return magnitude
    }
    
    /**
     * Compute mel-scale filter bank energies
     */
    private fun computeMelFilters(fftMagnitude: FloatArray): FloatArray {
        val melFilters = FloatArray(numMelFilters)
        val melMin = hzToMel(300.0f)
        val melMax = hzToMel(8000.0f)
        
        for (i in 0 until numMelFilters) {
            val melCenter = melMin + (melMax - melMin) * i / (numMelFilters - 1)
            val hzCenter = melToHz(melCenter)
            val binCenter = (hzCenter * fftMagnitude.size / (sampleRate / 2.0f)).toInt()
            
            var energy = 0.0f
            val windowSize = 10 // Simple rectangular window
            
            for (j in maxOf(0, binCenter - windowSize)..minOf(fftMagnitude.size - 1, binCenter + windowSize)) {
                energy += fftMagnitude[j]
            }
            
            melFilters[i] = ln(maxOf(energy, 1e-10f))
        }
        
        return melFilters
    }
    
    /**
     * Compute MFCC coefficients using DCT
     */
    private fun computeMfccCoeffs(melFilters: FloatArray): FloatArray {
        val mfcc = FloatArray(numMfccCoeffs)
        
        for (i in 0 until numMfccCoeffs) {
            var sum = 0.0f
            for (j in melFilters.indices) {
                sum += melFilters[j] * cos(PI * i * (j + 0.5) / melFilters.size).toFloat()
            }
            mfcc[i] = sum
        }
        
        return mfcc
    }
    
    /**
     * Convert Hz to Mel scale
     */
    private fun hzToMel(hz: Float): Float {
        return 2595.0f * log10(1.0f + hz / 700.0f)
    }
    
    /**
     * Convert Mel to Hz scale
     */
    private fun melToHz(mel: Float): Float {
        return 700.0f * (10.0f.pow(mel / 2595.0f) - 1.0f)
    }
    
    /**
     * Detect hotword using template matching
     */
    private fun detectHotword(): Boolean {
        if (featureBuffer.size < 20) return false
        
        val recentFeatures = featureBuffer.takeLast(40) // ~1 second of features
        
        for ((phrase, templates) in hotwordTemplates) {
            for (template in templates) {
                val similarity = computeSimilarity(recentFeatures, template)
                if (similarity > detectionThreshold) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Compute similarity between feature sequence and template
     */
    private fun computeSimilarity(features: List<FloatArray>, template: FloatArray): Float {
        if (features.isEmpty()) return 0.0f
        
        // Simple DTW-like alignment (simplified)
        var maxSimilarity = 0.0f
        
        for (startIdx in 0..maxOf(0, features.size - template.size / numMfccCoeffs)) {
            var similarity = 0.0f
            var count = 0
            
            for (i in 0 until minOf(features.size - startIdx, template.size / numMfccCoeffs)) {
                val featureFrame = features[startIdx + i]
                val templateStart = i * numMfccCoeffs
                val templateEnd = minOf(templateStart + numMfccCoeffs, template.size)
                
                if (templateEnd > templateStart) {
                    val templateFrame = template.sliceArray(templateStart until templateEnd)
                    similarity += computeCosineSimilarity(featureFrame, templateFrame)
                    count++
                }
            }
            
            if (count > 0) {
                similarity /= count
                maxSimilarity = maxOf(maxSimilarity, similarity)
            }
        }
        
        return maxSimilarity
    }
    
    /**
     * Compute cosine similarity between two feature vectors
     */
    private fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val minSize = minOf(a.size, b.size)
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        
        for (i in 0 until minSize) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA * normB)
        return if (denominator > 0) dotProduct / denominator else 0.0f
    }
    
    /**
     * Load or create hotword templates
     */
    private suspend fun loadHotwordTemplates() = withContext(Dispatchers.IO) {
        // In a production app, you would load pre-trained templates or train them
        // For this demo, we'll create simple synthetic templates
        
        for (phrase in activationPhrases) {
            val templates = mutableListOf<FloatArray>()
            
            // Create synthetic templates (in production, use real recordings)
            for (variation in 0 until 3) {
                val template = createSyntheticTemplate(phrase, variation)
                templates.add(template)
            }
            
            hotwordTemplates[phrase] = templates
        }
    }
    
    /**
     * Create a synthetic template for demonstration
     * In production, this would be replaced with real audio processing
     */
    private fun createSyntheticTemplate(phrase: String, variation: Int): FloatArray {
        val templateLength = phrase.length * 5 * numMfccCoeffs // Approximate duration
        val template = FloatArray(templateLength)
        
        // Create a simple pattern based on phrase characteristics
        for (i in template.indices) {
            val t = i.toFloat() / template.size
            val baseValue = sin(2.0 * PI * phrase.hashCode() * t).toFloat()
            val noise = (Math.random() - 0.5).toFloat() * 0.1f
            template[i] = baseValue + noise + variation * 0.05f
        }
        
        return template
    }
    
    /**
     * Train hotword detection with user recordings
     */
    suspend fun trainHotword(phrase: String, audioSamples: List<ShortArray>) = withContext(Dispatchers.IO) {
        val templates = mutableListOf<FloatArray>()
        
        for (sample in audioSamples) {
            val features = mutableListOf<FloatArray>()
            
            // Process audio sample in frames
            for (i in 0 until sample.size - frameSize step hopSize) {
                val frame = sample.sliceArray(i until i + frameSize)
                val mfcc = extractMfccFeatures(frame)
                features.add(mfcc)
            }
            
            // Flatten features into template
            val template = FloatArray(features.size * numMfccCoeffs)
            for (j in features.indices) {
                System.arraycopy(features[j], 0, template, j * numMfccCoeffs, numMfccCoeffs)
            }
            
            templates.add(template)
        }
        
        hotwordTemplates[phrase] = templates
        saveHotwordTemplates()
    }
    
    /**
     * Save trained templates to storage
     */
    private suspend fun saveHotwordTemplates() = withContext(Dispatchers.IO) {
        // Save templates to internal storage
        // In production, use proper serialization
        val templatesDir = File(context.filesDir, "hotword_templates")
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
        }
        
        for ((phrase, templates) in hotwordTemplates) {
            val file = File(templatesDir, "${phrase.replace(" ", "_")}.dat")
            // Save template data (implementation depends on chosen format)
        }
    }
    
    /**
     * Adjust detection sensitivity
     */
    fun setDetectionThreshold(threshold: Float) {
        preferences.edit().putFloat("detection_threshold", threshold).apply()
    }
    
    /**
     * Get current detection threshold
     */
    fun getDetectionThreshold(): Float {
        return preferences.getFloat("detection_threshold", detectionThreshold)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        audioBuffer.clear()
        featureBuffer.clear()
        hotwordTemplates.clear()
        isInitialized = false
    }
}

