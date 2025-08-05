package com.example.aiassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aiassistant.data.models.LLMConfig
import com.example.aiassistant.data.models.LLMType
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeConfig.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Model")
                }
            }
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "LLM Configurations",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (configs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No LLM configurations found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add a configuration to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(configs) { config ->
                    LLMConfigCard(
                        config = config,
                        isActive = config.id == activeConfig?.id,
                        onActivate = { viewModel.setActiveConfig(config.id) },
                        onDelete = { viewModel.deleteConfig(config) }
                    )
                }
            }
        }
    }

    // Add Configuration Dialog
    if (showAddDialog) {
        AddConfigDialog(
            onDismiss = { showAddDialog = false },
            onSave = { config ->
                viewModel.saveConfig(config)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LLMConfigCard(
    config: LLMConfig,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = config.type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (!isActive) {
                        TextButton(onClick = onActivate) {
                            Text("Activate")
                        }
                    } else {
                        Text(
                            text = "Active",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Configuration details
            Spacer(modifier = Modifier.height(8.dp))
            
            when (config.type) {
                LLMType.LOCAL -> {
                    Text(
                        text = "Model Path: ${config.modelPath ?: "Not set"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = "API Endpoint: ${config.apiEndpoint ?: "Default"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "Max Tokens: ${config.maxTokens}, Temperature: ${config.temperature}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddConfigDialog(
    onDismiss: () -> Unit,
    onSave: (LLMConfig) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LLMType.REMOTE_OPENAI) }
    var modelPath by remember { mutableStateOf("") }
    var apiEndpoint by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var maxTokens by remember { mutableStateOf("512") }
    var temperature by remember { mutableStateOf("0.8") }
    var topK by remember { mutableStateOf("40") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add LLM Configuration") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Configuration Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Type selection
                    Text(
                        text = "Model Type",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    LLMType.values().forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(
                                text = type.name.replace("_", " "),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (selectedType == LLMType.LOCAL) {
                    item {
                        OutlinedTextField(
                            value = modelPath,
                            onValueChange = { modelPath = it },
                            label = { Text("Model Path") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("/data/local/tmp/llm/model.task") }
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = apiEndpoint,
                            onValueChange = { apiEndpoint = it },
                            label = { Text("API Endpoint (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://api.openai.com/v1/") }
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        label = { Text("Max Tokens") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temperature") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                item {
                    OutlinedTextField(
                        value = topK,
                        onValueChange = { topK = it },
                        label = { Text("Top K") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = LLMConfig(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = selectedType,
                        modelPath = if (selectedType == LLMType.LOCAL) modelPath.ifBlank { null } else null,
                        apiEndpoint = if (selectedType != LLMType.LOCAL) apiEndpoint.ifBlank { null } else null,
                        apiKey = if (selectedType != LLMType.LOCAL) apiKey.ifBlank { null } else null,
                        maxTokens = maxTokens.toIntOrNull() ?: 512,
                        temperature = temperature.toFloatOrNull() ?: 0.8f,
                        topK = topK.toIntOrNull() ?: 40
                    )
                    onSave(config)
                },
                enabled = name.isNotBlank() && 
                         (selectedType == LLMType.LOCAL && modelPath.isNotBlank() ||
                          selectedType != LLMType.LOCAL && apiKey.isNotBlank())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

