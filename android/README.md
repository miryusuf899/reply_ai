# ReplyAI — Android Client

Kotlin Android app for the ReplyAI backend — floating bubble assistant for Telegram, Instagram, and WhatsApp.

## Open in Android Studio

1. Open Android Studio → **File → Open** → select the `android/` folder
2. Let Gradle sync complete
3. Run on a device or emulator (API 26+)

## Features

- JWT auth with EncryptedSharedPreferences
- Chat sessions CRUD + AI reply generation
- Floating overlay bubble (Mobizen-style)
- Accessibility service for auto-insert into messengers
- Material Design 3 dark theme

## API Base URL

`http://31.25.238.184:8005/api/`

Configured in `app/build.gradle.kts` as `BuildConfig.BASE_URL`.

## Permissions Setup

1. **Overlay**: Settings → Grant Overlay Permission
2. **Accessibility**: System Settings → Accessibility → ReplyAI → Enable
3. Start floating bubble from Settings screen

## Project Structure

```
app/src/main/java/com/replyai/
├── data/api/          RetrofitClient, ApiService
├── data/models/       DTOs
├── data/repository/   AuthRepository, ChatRepository
├── ui/                Activities & adapters
├── service/           FloatingOverlay, Accessibility
├── utils/             TokenManager, Extensions
└── viewmodel/         MVVM ViewModels
```

## Backend Endpoints Used

| Endpoint | Purpose |
|----------|---------|
| `POST users/login/` | JWT login (email + password) |
| `POST users/register/` | Registration |
| `GET/POST chats/sessions/` | Session list & create |
| `GET/DELETE chats/sessions/{uuid}/` | Session detail & delete |
| `POST chats/sessions/{uuid}/ask/` | AI reply |
| `GET/PUT settings/my-settings/` | User preferences |
| `GET users/profile/` | Account info |
