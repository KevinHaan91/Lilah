package com.example.aiassistant.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.models.Message
import com.example.aiassistant.data.repository.LLMRepository
import com.example.aiassistant.data.repository.MessageRepository
import com.example.aiassistant.utils.AgentOrchestrator
import com.example.aiassistant.utils.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val llmRepository: LLMRepository,
    private val agentOrchestrator: AgentOrchestrator,
    private val voiceManager: VoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val conversationId = "default"

    val messages: StateFlow<List<Message>> = messageRepository
        .getMessagesForConversation(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Save user message
                val userMessage = Message(
                    content = content,
                    isFromUser = true,
                    conversationId = conversationId
                )
                messageRepository.insertMessage(userMessage)

                // Generate AI response using agent orchestrator
                val conversationHistory = messages.value
                val result = agentOrchestrator.processMessage(content, conversationHistory)

                result.fold(
                    onSuccess = { response ->
                        // Save AI response
                        val aiMessage = Message(
                            content = response,
                            isFromUser = false,
                            conversationId = conversationId
                        )
                        messageRepository.insertMessage(aiMessage)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Unknown error occurred"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unknown error occurred"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearConversation() {
        viewModelScope.launch {
            messageRepository.deleteConversation(conversationId)
        }
    }

    init {
        // Observe voice state changes
        viewModelScope.launch {
            voiceManager.voiceState.collect { voiceState ->
                _uiState.value = _uiState.value.copy(
                    isListening = voiceState.isListening,
                    isSpeaking = voiceState.isSpeaking
                )
            }
        }
        
        // Handle speech recognition results
        viewModelScope.launch {
            voiceManager.speechResults.collect { speechText ->
                sendMessage(speechText)
            }
        }
        
        // Handle speech recognition errors
        viewModelScope.launch {
            voiceManager.speechErrors.collect { error ->
                _uiState.value = _uiState.value.copy(error = "Speech recognition error: $error")
            }
        }
    }

    fun toggleVoiceInput() {
        if (_uiState.value.isListening) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun speakResponse(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
    }
}

