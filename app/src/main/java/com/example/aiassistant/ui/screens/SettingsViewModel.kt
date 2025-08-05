package com.example.aiassistant.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.models.LLMConfig
import com.example.aiassistant.data.repository.LLMRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val llmRepository: LLMRepository
) : ViewModel() {

    val configs: StateFlow<List<LLMConfig>> = llmRepository.getAllConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeConfig = MutableStateFlow<LLMConfig?>(null)
    val activeConfig: StateFlow<LLMConfig?> = _activeConfig.asStateFlow()

    init {
        // Load active config
        viewModelScope.launch {
            _activeConfig.value = llmRepository.getActiveConfig()
        }
    }

    fun saveConfig(config: LLMConfig) {
        viewModelScope.launch {
            llmRepository.saveConfig(config)
        }
    }

    fun deleteConfig(config: LLMConfig) {
        viewModelScope.launch {
            llmRepository.deleteConfig(config)
            
            // If deleted config was active, clear active config
            if (_activeConfig.value?.id == config.id) {
                _activeConfig.value = null
            }
        }
    }

    fun setActiveConfig(configId: String) {
        viewModelScope.launch {
            llmRepository.setActiveConfig(configId)
            _activeConfig.value = llmRepository.getActiveConfig()
        }
    }
}

