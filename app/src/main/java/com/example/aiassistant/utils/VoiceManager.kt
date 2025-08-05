package com.example.aiassistant.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VoiceManager handles speech recognition and text-to-speech functionality
 */
@Singleton
class VoiceManager @Inject constructor(
    private val context: Context
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    private val _speechResults = Channel<String>(Channel.UNLIMITED)
    val speechResults: Flow<String> = _speechResults.receiveAsFlow()
    
    private val _speechErrors = Channel<String>(Channel.UNLIMITED)
    val speechErrors: Flow<String> = _speechErrors.receiveAsFlow()

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                setVoiceGender(TextToSpeech.GENDER_FEMALE)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = _voiceState.value.copy(isSpeaking = true)
                    }

                    override fun onDone(utteranceId: String?) {
                        _voiceState.value = _voiceState.value.copy(isSpeaking = false)
                    }

                    override fun onError(utteranceId: String?) {
                        _voiceState.value = _voiceState.value.copy(isSpeaking = false)
                    }
                })
                isInitialized = true
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceState.value = _voiceState.value.copy(isListening = true)
                }

                override fun onBeginningOfSpeech() {
                    _voiceState.value = _voiceState.value.copy(isProcessingSpeech = true)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _voiceState.value = _voiceState.value.copy(audioLevel = rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _voiceState.value = _voiceState.value.copy(
                        isListening = false,
                        isProcessingSpeech = false
                    )
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    
                    _voiceState.value = _voiceState.value.copy(
                        isListening = false,
                        isProcessingSpeech = false
                    )
                    
                    _speechErrors.trySend(errorMessage)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _speechResults.trySend(matches[0])
                    }
                    
                    _voiceState.value = _voiceState.value.copy(
                        isListening = false,
                        isProcessingSpeech = false
                    )
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _voiceState.value = _voiceState.value.copy(partialSpeechResult = matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Start listening for speech input
     */
    fun startListening() {
        if (!isInitialized) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
    }

    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceState.value = _voiceState.value.copy(
            isListening = false,
            isProcessingSpeech = false,
            partialSpeechResult = null
        )
    }

    /**
     * Speak the given text
     */
    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()) {
        if (!isInitialized) return
        
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Stop current speech
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
        _voiceState.value = _voiceState.value.copy(isSpeaking = false)
    }

    /**
     * Check if speech recognition is available
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Check if text-to-speech is available
     */
    fun isTextToSpeechAvailable(): Boolean {
        return isInitialized
    }

    /**
     * Set speech rate (0.1 to 3.0, 1.0 is normal)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    /**
     * Set speech pitch (0.5 to 2.0, 1.0 is normal)
     */
    fun setSpeechPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        speechRecognizer = null
        textToSpeech = null
        isInitialized = false
    }
}

/**
 * Represents the current state of voice operations
 */
data class VoiceState(
    val isListening: Boolean = false,
    val isProcessingSpeech: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioLevel: Float = 0f,
    val partialSpeechResult: String? = null
)



    /**
     * Set the gender of the voice for Text-to-Speech.
     * @param gender TextToSpeech.GENDER_FEMALE or TextToSpeech.GENDER_MALE
     */
    fun setVoiceGender(gender: Int) {
        val voices = textToSpeech?.voices
        if (voices != null) {
            for (voice in voices) {
                if (voice.features != null && voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) && voice.gender == gender) {
                    textToSpeech?.voice = voice
                    break
                }
            }
        }
    }


