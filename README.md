# AI Assistant for Android

An AI agentic assistant for Android that supports various Large Language Models (LLMs), including both local on-device models and remote cloud-based models.

## Features

- **Multiple LLM Support**: Supports both local on-device models (via MediaPipe) and remote models (OpenAI, Gemini, custom APIs)
- **Modern UI**: Built with Jetpack Compose for a modern, responsive user interface
- **Conversation History**: Persistent conversation storage using Room database
- **Configurable Models**: Easy switching between different LLM configurations
- **Agentic Capabilities**: Framework for extending with tool usage and external integrations

## Architecture

The application follows Clean Architecture principles with:

- **UI Layer**: Jetpack Compose screens and ViewModels
- **Domain Layer**: Repository interfaces and business logic
- **Data Layer**: Local database (Room) and remote API services

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **Networking**: Retrofit + OkHttp
- **On-device LLM**: MediaPipe LLM Inference API
- **State Management**: StateFlow and Compose State

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 or higher
- Kotlin 1.9.10 or later

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run on a device or emulator

### Configuring LLM Models

#### Local Models (MediaPipe)

1. Download a compatible quantized model (e.g., Gemma-3n from Hugging Face)
2. Convert the model to MediaPipe format if necessary
3. Push the model to the device:
   ```bash
   adb push model_file.task /data/local/tmp/llm/
   ```
4. Configure the model path in the app settings

#### Remote Models

1. Obtain API keys from your preferred LLM provider (OpenAI, etc.)
2. Configure the API endpoint and key in the app settings
3. Select the remote model configuration

## Project Structure

```
app/src/main/java/com/example/aiassistant/
├── data/
│   ├── local/          # Room database, DAOs
│   ├── remote/         # API services, DTOs
│   ├── repository/     # Repository implementations
│   └── models/         # Data models
├── di/                 # Dependency injection modules
├── ui/
│   ├── screens/        # Compose screens and ViewModels
│   └── theme/          # UI theme and styling
└── utils/              # Utility classes
```

## Dependencies

Key dependencies include:

- **MediaPipe**: `com.google.mediapipe:tasks-genai:0.10.24`
- **Compose BOM**: `androidx.compose:compose-bom:2023.10.01`
- **Hilt**: `com.google.dagger:hilt-android:2.48`
- **Room**: `androidx.room:room-runtime:2.6.1`
- **Retrofit**: `com.squareup.retrofit2:retrofit:2.9.0`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Troubleshooting

### Common Issues

1. **MediaPipe model loading fails**: Ensure the model file is in the correct format and location
2. **API requests fail**: Check network connectivity and API key configuration
3. **Build errors**: Ensure all dependencies are properly synced

### Performance Considerations

- Local models work best on high-end devices (Pixel 8, Samsung S23+)
- Consider model size vs. performance trade-offs
- Monitor memory usage when running large models

## Future Enhancements

- [ ] Support for more LLM providers (Anthropic, Cohere, etc.)
- [ ] Advanced agentic capabilities (tool usage, web search)
- [ ] Voice input/output integration
- [ ] Multi-modal support (image, audio)
- [ ] Custom model fine-tuning interface
- [ ] Conversation export/import
- [ ] Dark/light theme toggle
- [ ] Accessibility improvements

