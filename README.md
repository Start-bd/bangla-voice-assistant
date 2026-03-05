# 🇧🇩 Bangla Voice Assistant

A complete voice assistant system that understands Bangla speech and performs actions on your Android device.

![Architecture](https://img.shields.io/badge/Architecture-Client%2FServer-blue)
![Backend](https://img.shields.io/badge/Backend-Node.js-green)
![Frontend](https://img.shields.io/badge/Android-Kotlin-purple)

## 🎯 What It Does

Speak in Bangla → Get English translation → Execute actions automatically

**Example:**
- You say: *"আমি রিয়াজকে কল করতে চাই"* (I want to call Riaz)
- App translates: "I want to call Riaz"
- App opens dialer with Riaz's number

## 🏗️ Architecture

```
┌─────────────────┐     HTTP/JSON      ┌──────────────────┐
│   Android App   │ ◄────────────────► │  Node.js Backend │
│  (Kotlin)       │                    │  (Express)       │
│                 │                    │                  │
│ • Floating      │                    │ • Receive audio  │
│   bubble UI     │                    │ • Bangla STT     │
│ • Voice record  │                    │ • LLM translate  │
│ • Action exec   │                    │ • Intent classify│
└─────────────────┘                    └──────────────────┘
                                               │
                                               ▼
                                        ┌──────────────┐
                                        │ Soniox STT   │
                                        │ OpenAI GPT   │
                                        └──────────────┘
```

## 📁 Project Structure

```
bangla-voice-assistant/
├── backend/              # Node.js backend
│   ├── server.js         # Main Express server
│   ├── package.json      # Dependencies
│   └── .env.example      # Environment template
│
└── android-app/          # Kotlin Android app
    ├── app/src/main/
    │   ├── java/...      # Kotlin source files
    │   └── res/          # Layouts, drawables, values
    └── build.gradle.kts  # Build configuration
```

## 🚀 Quick Start

### Prerequisites

- Node.js 18+
- Android Studio Hedgehog (2023.1.1) or newer
- Android device or emulator (API 26+)

### 1. Backend Setup

```bash
cd backend
npm install

# Create .env file
cp .env.example .env
# Edit .env with your API keys:
# - SONIOX_API_KEY (from soniox.com)
# - OPENAI_API_KEY (from platform.openai.com)

npm run dev
```

Backend runs at `http://localhost:3000`

### 2. Android App Setup

1. Open `android-app` folder in Android Studio
2. Update `BASE_URL` in `RetrofitClient.kt`:
   - For emulator: `http://10.0.2.2:3000/`
   - For device: `http://YOUR_COMPUTER_IP:3000/`
3. Build and run on device/emulator

### 3. Deploy Backend (Production)

**Render (Recommended):**
1. Push backend code to GitHub
2. Create new Web Service on [Render](https://render.com)
3. Connect GitHub repo
4. Set environment variables
5. Deploy!

Update Android app `BASE_URL` to your Render URL.

## 📱 Features

### Supported Actions

| Intent | Bangla Example | Action |
|--------|---------------|--------|
| `call_contact` | "আমি রিয়াজকে কল করতে চাই" | Opens dialer for contact |
| `open_camera` | "ক্যামেরা খুলো" | Opens camera app |
| `open_youtube` | "ইউটিউবে AI সার্চ করো" | Opens YouTube search |
| `lovable_build` | "ওয়েবসাইট বানাও" | Opens Lovable.dev builder |
| `translate_only` | "এটা অনুবাদ করো" | Shows translation only |

### App Interface

- **Main Screen**: Start/stop assistant, test backend
- **Floating Bubble**: Always-accessible trigger
- **Result Panel**: Shows translation + action buttons

## 🔧 API Endpoints

### Health Check
```
GET /health
```

### Voice Command
```
POST /voice/command
Content-Type: application/json

{
  "audioBase64": "<base64-audio>",
  "metadata": {
    "language": "bn",
    "mode": "agent"
  }
}
```

**Response:**
```json
{
  "transcriptBn": "আমি রিয়াজকে কল করতে চাই",
  "englishText": "I want to call Riaz.",
  "intent": "call_contact",
  "contactName": "Riaz",
  "searchQuery": null,
  "promptForBuilder": null
}
```

## 🔐 Environment Variables

### Backend (.env)

| Variable | Description | Get From |
|----------|-------------|----------|
| `SONIOX_API_KEY` | Bengali STT API | [soniox.com](https://soniox.com/speech-to-text/bengali) |
| `OPENAI_API_KEY` | Translation & Intent | [platform.openai.com](https://platform.openai.com) |
| `PORT` | Server port | Optional (default: 3000) |

## 🧪 Testing

### Backend Test
```bash
curl http://localhost:3000/health

curl -X POST http://localhost:3000/voice/command/test \
  -H "Content-Type: application/json" \
  -d '{"testIntent": "call_contact"}'
```

### Android Test
1. Tap "Test Backend Connection" in main screen
2. Check Logcat for detailed logs

## 📦 Building Release

### Backend
```bash
git push origin main
# Auto-deploys on Render
```

### Android APK
1. Android Studio → Build → Generate Signed Bundle/APK
2. Create signing key
3. Build APK
4. Transfer to phone and install

## 🛠️ Tech Stack

**Backend:**
- Node.js + Express
- Soniox API (Bengali STT)
- OpenAI GPT (Translation + Intent)

**Android:**
- Kotlin
- Retrofit (HTTP client)
- Coroutines (Async)
- Material Design 3

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Bubble not showing | Grant "Display over other apps" permission |
| Recording fails | Check microphone permission, close other apps |
| Backend connection error | Verify BASE_URL, check backend is running |
| STT not working | Check SONIOX_API_KEY in .env |
| Translation fails | Check OPENAI_API_KEY in .env |

## 🗺️ Roadmap

- [ ] On-device STT (faster, offline)
- [ ] More actions (WhatsApp, SMS, etc.)
- [ ] AccessibilityService for deeper integration
- [ ] Voice feedback (TTS)
- [ ] Custom wake word

## 📄 License

MIT License - Feel free to use and modify!

## 🙏 Credits

- [Soniox](https://soniox.com) for Bengali STT
- [OpenAI](https://openai.com) for GPT
- [Lovable](https://lovable.dev) for builder integration

---

**Made with ❤️ for Bangla speakers worldwide**