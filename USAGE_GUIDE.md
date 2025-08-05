# AI Assistant Android App - Usage Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Installation and Setup](#installation-and-setup)
3. [First-Time Configuration](#first-time-configuration)
4. [Using the Chat Interface](#using-the-chat-interface)
5. [Configuring LLM Models](#configuring-llm-models)
6. [Agentic Capabilities](#agentic-capabilities)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Configuration](#advanced-configuration)
9. [Privacy and Security](#privacy-and-security)
10. [Frequently Asked Questions](#frequently-asked-questions)

## Introduction

The AI Assistant Android app is a powerful, flexible AI companion that supports both local on-device Large Language Models (LLMs) and remote cloud-based models. This application is designed to provide intelligent assistance while giving you full control over which AI models to use and how your data is processed.

### Key Features

- **Multi-Model Support**: Switch between local MediaPipe models and remote APIs (OpenAI, Gemini, custom endpoints)
- **Agentic Capabilities**: The assistant can use tools like calculators, time queries, and more
- **Privacy-Focused**: Option to use completely local models for maximum privacy
- **Conversation History**: Persistent chat history with conversation management
- **Modern UI**: Built with Material Design 3 and Jetpack Compose for a smooth user experience

### System Requirements

- **Android Version**: Android 7.0 (API level 24) or higher
- **RAM**: Minimum 4GB (8GB+ recommended for local models)
- **Storage**: At least 2GB free space (more for local model files)
- **Network**: Internet connection required for remote models and initial setup

## Installation and Setup

### Installing the Application

1. **From APK File** (Development/Beta):
   - Download the APK file to your Android device
   - Enable "Install from Unknown Sources" in your device settings
   - Tap the APK file and follow the installation prompts

2. **Building from Source**:
   - Clone the repository: `git clone [repository-url]`
   - Open the project in Android Studio
   - Connect your Android device or start an emulator
   - Click "Run" or use `./gradlew installDebug`

### Initial App Launch

When you first launch the app, you'll see an empty chat interface with no configured models. Before you can start chatting, you'll need to configure at least one LLM model.

## First-Time Configuration

### Quick Start with Remote Models

The fastest way to get started is with a remote model like OpenAI's GPT:

1. **Open Settings**: Tap the settings icon (⚙️) in the top-right corner of the chat screen
2. **Add Configuration**: Tap the plus (+) button to add a new model configuration
3. **Configure OpenAI Model**:
   - **Configuration Name**: Enter a descriptive name (e.g., "GPT-3.5 Turbo")
   - **Model Type**: Select "REMOTE OPENAI"
   - **API Key**: Enter your OpenAI API key
   - **Leave API Endpoint blank** (uses default OpenAI endpoint)
   - **Adjust parameters** as needed:
     - Max Tokens: 512 (default) - 4000 (maximum for most models)
     - Temperature: 0.8 (default) - controls randomness (0.0 = deterministic, 1.0 = very random)
     - Top K: 40 (default) - limits token selection to top K most probable tokens
4. **Save Configuration**: Tap "Save"
5. **Activate Model**: Tap "Activate" on your new configuration

### Setting Up Local Models (Advanced)

For maximum privacy and offline capability, you can use local models:

1. **Download a Compatible Model**:
   - Visit [Hugging Face LiteRT Community](https://huggingface.co/models?library=litert)
   - Download a quantized model file (e.g., Gemma-3n models)
   - Models should have `.task` extension for MediaPipe compatibility

2. **Transfer Model to Device**:
   ```bash
   # Using ADB (Android Debug Bridge)
   adb shell mkdir -p /data/local/tmp/llm/
   adb push your-model-file.task /data/local/tmp/llm/
   ```

3. **Configure Local Model**:
   - **Configuration Name**: Enter a descriptive name (e.g., "Gemma-3n Local")
   - **Model Type**: Select "LOCAL"
   - **Model Path**: Enter the full path (e.g., `/data/local/tmp/llm/your-model-file.task`)
   - **Adjust parameters** based on your model's capabilities

4. **Activate and Test**: Save and activate the configuration, then test with a simple message

## Using the Chat Interface

### Basic Chat Functionality

The chat interface is designed to be intuitive and familiar:

1. **Sending Messages**: Type your message in the text field at the bottom and tap the send button (➤)
2. **Message History**: Scroll up to view previous messages in the conversation
3. **Loading Indicators**: When the AI is thinking, you'll see a "Thinking..." indicator
4. **Error Handling**: If something goes wrong, you'll see an error message with details

### Message Types and Formatting

The app supports various types of interactions:

- **Simple Questions**: "What is the capital of France?"
- **Complex Reasoning**: "Explain the pros and cons of renewable energy"
- **Tool Usage**: "What is 15 * 23?" (automatically uses calculator tool)
- **Time Queries**: "What time is it?" or "What time is it in Tokyo?"

### Conversation Management

- **Persistent History**: Your conversations are automatically saved and restored when you reopen the app
- **Context Awareness**: The AI maintains context from previous messages in the conversation
- **Long Conversations**: The app handles long conversations efficiently, keeping recent context while managing memory usage

## Configuring LLM Models

### Understanding Model Types

The app supports four types of LLM configurations:

1. **LOCAL**: On-device models using MediaPipe
   - **Pros**: Complete privacy, no internet required, no API costs
   - **Cons**: Requires powerful device, limited model selection, larger app size

2. **REMOTE_OPENAI**: OpenAI's GPT models
   - **Pros**: State-of-the-art performance, regularly updated
   - **Cons**: Requires API key, usage costs, internet dependency

3. **REMOTE_GEMINI**: Google's Gemini models
   - **Pros**: Strong performance, integration with Google services
   - **Cons**: Requires API key, usage costs, internet dependency

4. **REMOTE_CUSTOM**: Custom API endpoints
   - **Pros**: Flexibility to use any compatible API
   - **Cons**: Requires technical setup, varies by provider

### Model Configuration Parameters

Understanding these parameters helps optimize your AI experience:

#### Max Tokens
- **Definition**: Maximum number of tokens (words/word pieces) in input + output
- **Typical Values**: 512 (short responses) to 4000 (long responses)
- **Impact**: Higher values allow longer conversations but may increase costs/latency

#### Temperature
- **Definition**: Controls randomness in AI responses
- **Range**: 0.0 to 1.0
- **Values**:
  - 0.0-0.3: Very focused, deterministic responses
  - 0.4-0.7: Balanced creativity and consistency
  - 0.8-1.0: More creative and varied responses

#### Top K
- **Definition**: Limits token selection to the K most probable options
- **Typical Values**: 20-50
- **Impact**: Lower values = more focused, higher values = more diverse responses

### Managing Multiple Configurations

You can maintain multiple model configurations for different use cases:

- **Work Configuration**: Conservative temperature, higher token limit for detailed responses
- **Creative Configuration**: Higher temperature for brainstorming and creative tasks
- **Quick Configuration**: Lower token limit for fast, concise responses
- **Local Configuration**: For private or offline use

To switch between configurations:
1. Go to Settings
2. Tap "Activate" on the desired configuration
3. Return to chat - new messages will use the activated model

## Agentic Capabilities

The AI Assistant includes agentic capabilities, meaning it can use tools to perform actions beyond simple text generation.

### Available Tools

#### Calculator Tool
The assistant can perform mathematical calculations:

**Examples**:
- "What is 15 + 27?" → Uses calculator tool automatically
- "Calculate the square root of 144" → Returns 12
- "What is sin(30)?" → Returns trigonometric result

**Supported Operations**:
- Basic arithmetic: +, -, *, /
- Square root: sqrt(number)
- Trigonometry: sin(degrees), cos(degrees)

#### Time Tool
Get current time information:

**Examples**:
- "What time is it?" → Returns local time
- "What time is it in Tokyo?" → Returns time in specified timezone
- "Current time in UTC" → Returns UTC time

#### Web Search Tool (Placeholder)
Currently shows a placeholder message explaining that web search would be implemented in a production version.

#### Weather Tool (Placeholder)
Currently shows a placeholder message explaining that weather information would be implemented in a production version.

### How Agentic Capabilities Work

1. **Intent Recognition**: The AI analyzes your message to determine if tools are needed
2. **Tool Selection**: If tools are required, the AI selects the appropriate tool
3. **Tool Execution**: The selected tool runs with the provided parameters
4. **Response Integration**: The tool result is incorporated into a natural response

### Extending Agentic Capabilities

The app is designed to be extensible. Developers can add new tools by:

1. Implementing the `Tool` interface
2. Adding the tool to the `ToolRegistry`
3. Updating the system prompt to include the new tool

## Troubleshooting

### Common Issues and Solutions

#### "No active LLM configuration found"
**Problem**: You haven't configured or activated any LLM model.
**Solution**: 
1. Go to Settings (⚙️ icon)
2. Add a new configuration using the + button
3. Save and activate the configuration

#### "Error loading model" (Local Models)
**Problem**: The local model file cannot be loaded.
**Solutions**:
1. Verify the model file path is correct
2. Ensure the model file is in MediaPipe-compatible format (.task extension)
3. Check that your device has sufficient RAM
4. Try restarting the app

#### "OpenAI API error: 401" (Remote Models)
**Problem**: Invalid or expired API key.
**Solutions**:
1. Verify your API key is correct
2. Check that your OpenAI account has available credits
3. Ensure the API key has the necessary permissions

#### "Error generating response"
**Problem**: General error during AI response generation.
**Solutions**:
1. Check your internet connection (for remote models)
2. Try a shorter or simpler message
3. Switch to a different model configuration
4. Restart the app if the problem persists

#### App Crashes or Freezes
**Problem**: App becomes unresponsive or crashes.
**Solutions**:
1. Ensure your device meets minimum requirements
2. Close other apps to free up memory
3. Clear the app's cache in Android settings
4. Restart your device

### Performance Optimization

#### For Local Models
- **Use High-End Devices**: Local models work best on devices with 8GB+ RAM
- **Close Background Apps**: Free up memory before using local models
- **Choose Smaller Models**: Smaller quantized models run faster but may have reduced capabilities

#### For Remote Models
- **Stable Internet**: Ensure good internet connectivity for best performance
- **Optimize Parameters**: Lower max_tokens for faster responses
- **Monitor Usage**: Be aware of API rate limits and costs

### Debugging Steps

If you encounter persistent issues:

1. **Check Device Compatibility**: Ensure your device meets minimum requirements
2. **Update the App**: Make sure you're using the latest version
3. **Clear App Data**: Go to Android Settings > Apps > AI Assistant > Storage > Clear Data
4. **Check Logs**: If you're comfortable with technical details, use `adb logcat` to view detailed error logs
5. **Report Issues**: Contact support with detailed information about your device, Android version, and the specific issue

## Advanced Configuration

### Custom API Endpoints

For advanced users wanting to use custom LLM APIs:

1. **Select REMOTE_CUSTOM** as the model type
2. **API Endpoint**: Enter your custom API URL
3. **API Key**: Enter your authentication key
4. **Ensure Compatibility**: Your API should be compatible with OpenAI's chat completions format

### Model Performance Tuning

#### Optimizing for Speed
- Lower max_tokens (256-512)
- Lower temperature (0.1-0.3)
- Lower top_k (10-20)

#### Optimizing for Quality
- Higher max_tokens (1000-4000)
- Moderate temperature (0.7-0.9)
- Higher top_k (40-60)

#### Optimizing for Consistency
- Lower temperature (0.0-0.3)
- Moderate top_k (20-40)
- Consistent max_tokens based on use case

### Network Configuration

For enterprise or restricted network environments:

1. **Proxy Support**: The app respects system proxy settings
2. **Certificate Pinning**: Can be configured for additional security
3. **Custom Endpoints**: Support for internal API endpoints

## Privacy and Security

### Data Handling

#### Local Models
- **Complete Privacy**: All processing happens on-device
- **No Network Access**: No data leaves your device
- **Local Storage**: Conversations stored locally in encrypted database

#### Remote Models
- **API Communication**: Messages sent to configured API endpoints
- **No Persistent Storage**: Remote providers may not store conversations permanently
- **Encryption**: All API communications use HTTPS encryption

### Security Best Practices

1. **API Key Management**:
   - Store API keys securely within the app
   - Don't share API keys with others
   - Regularly rotate API keys
   - Monitor API usage for unusual activity

2. **Device Security**:
   - Use device lock screen protection
   - Keep your Android device updated
   - Only install the app from trusted sources
   - Be cautious with sensitive information in conversations

3. **Network Security**:
   - Use trusted Wi-Fi networks
   - Consider using VPN for additional privacy
   - Be aware of network monitoring in corporate environments

### Privacy Controls

- **Conversation Deletion**: Clear conversation history anytime in settings
- **Model Selection**: Choose local models for maximum privacy
- **Data Export**: Export conversations for backup or migration
- **Selective Sharing**: Control what information you share with the AI

## Frequently Asked Questions

### General Questions

**Q: Is the app free to use?**
A: The app itself is free, but remote AI models may require paid API keys. Local models are completely free once set up.

**Q: Can I use the app offline?**
A: Yes, if you configure local models. Remote models require internet connectivity.

**Q: How much storage does the app use?**
A: The base app is small (~50MB), but local model files can be 1-4GB each.

### Technical Questions

**Q: Which devices are supported?**
A: Android 7.0+ devices. Local models work best on high-end devices with 8GB+ RAM.

**Q: Can I use multiple models simultaneously?**
A: No, only one model configuration can be active at a time, but you can quickly switch between configurations.

**Q: Are conversations synced across devices?**
A: No, conversations are stored locally on each device. You can export/import conversations manually.

### Model-Specific Questions

**Q: Which local models are recommended?**
A: Gemma-3n models (2B or 4B) from Hugging Face are well-optimized for mobile devices.

**Q: How do I get an OpenAI API key?**
A: Visit platform.openai.com, create an account, and generate an API key in the API section.

**Q: Can I fine-tune local models?**
A: The current version doesn't support fine-tuning, but this may be added in future updates.

### Troubleshooting Questions

**Q: Why are responses slow?**
A: Local models depend on device performance. Remote models depend on internet speed and API response times.

**Q: The app crashes when loading local models. What should I do?**
A: Ensure your device has sufficient RAM and the model file is compatible. Try smaller models first.

**Q: Can I recover deleted conversations?**
A: No, deleted conversations cannot be recovered unless you've exported them previously.

---

For additional support or to report issues, please refer to the project's GitHub repository or contact the development team. This usage guide will be updated as new features are added to the application.

