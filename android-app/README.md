# Bangla Voice Assistant - Android App

Kotlin Android app with a floating bubble interface for the Bangla Voice Assistant.

## Features

- 🫧 **Floating Bubble**: Always-accessible voice assistant bubble
- 🎤 **Voice Recording**: Tap to record Bangla speech
- 🌐 **Backend Integration**: Sends audio to backend for STT + LLM processing
- 📱 **Action Dispatcher**: Executes actions based on intent
  - Call contacts by name
  - Open camera
  - Search YouTube
  - Build with Lovable.dev
- 📋 **Copy & Share**: Easy sharing of translations

## Project Structure

```
app/src/main/java/com/banglavoiceassistant/
├── data/
│   ├── AgentResponse.kt      # Data models for API responses
│   ├── ApiService.kt         # Retrofit API interface
│   └── RetrofitClient.kt     # Retrofit configuration
├── service/
│   └── FloatingBubbleService.kt  # Main floating bubble service
├── ui/
│   └── MainActivity.kt       # Main activity
└── util/
    ├── ActionDispatcher.kt   # Handles action execution
    └── VoiceRecorder.kt      # Audio recording utility
```

## Setup

### 1. Open in Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to `android-app` folder
4. Wait for Gradle sync to complete

### 2. Configure Backend URL

Edit `data/RetrofitClient.kt`:

```kotlin
// For local testing with emulator
private const val BASE_URL = "http://10.0.2.2:3000/"

// For production
private const val BASE_URL = "https://your-backend.onrender.com/"
```

### 3. Build and Run

1. Connect your Android device or start an emulator
2. Click "Run" in Android Studio
3. Grant required permissions (Microphone, Contacts, Overlay)

## Permissions Required

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Capture voice input |
| `READ_CONTACTS` | Find contacts for calling |
| `SYSTEM_ALERT_WINDOW` | Show floating bubble overlay |
| `CALL_PHONE` | Open dialer for calls |

## Usage

1. Launch the app
2. Tap "Start Voice Assistant"
3. Grant all permissions
4. The floating bubble appears
5. Tap the bubble to open the result panel
6. Tap the mic button and speak in Bangla
7. The app will translate and execute the action

## Example Commands

| Bangla | English | Action |
|--------|---------|--------|
| "আমি রিয়াজকে কল করতে চাই" | "I want to call Riaz" | Opens dialer for Riaz |
| "ক্যামেরা খুলো" | "Open the camera" | Opens camera app |
| "ইউটিউবে AI সার্চ করো" | "Search AI on YouTube" | Opens YouTube search |
| "ওয়েবসাইট বানাও" | "Build a website" | Opens Lovable.dev |

## Building Release APK

1. In Android Studio: Build → Generate Signed Bundle/APK
2. Create or select a signing key
3. Choose APK format
4. The APK will be generated in `app/release/`

## Troubleshooting

### Bubble not showing
- Check overlay permission in Settings → Apps → Special access → Draw over apps

### Recording not working
- Check microphone permission
- Ensure no other app is using the microphone

### Backend connection failed
- Verify backend URL in `RetrofitClient.kt`
- Check if backend is running
- For emulator, use `10.0.2.2` instead of `localhost`

## Dependencies

- Retrofit 2.9.0 - HTTP client
- OkHttp 4.12.0 - Networking
- Gson 2.10.1 - JSON parsing
- Kotlin Coroutines 1.7.3 - Async operations
- Material Design 3 - UI components
