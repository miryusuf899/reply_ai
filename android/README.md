# Reply AI — Android

AI-ассистент для Telegram, WhatsApp и Instagram с плавающей кнопкой и Accessibility.

## Запуск

1. Откройте папку `android/` в Android Studio
2. Sync Gradle
3. Запустите на устройстве API 26+

**Base URL:** `http://31.25.238.184:8005/api/`

## Возможности

- Вход по Email/паролю и Google Sign-In
- JWT в EncryptedSharedPreferences + авто-refresh при 401
- Чаты, избранное, настройки, профиль (тариф, usage, планы)
- Overlay: меню → генерация → вставка в мессенджер
- Accessibility: чтение сообщений + вставка текста
- 👍👎 и избранное для ответов AI

## Настройка на устройстве

1. Войти в аккаунт
2. **Настройки** → Overlay permission → Start floating button
3. **Accessibility** → Reply AI → включить
4. Открыть Telegram/WhatsApp → нажать плавающую кнопку

## Google Sign-In

Web Client ID в `BuildConfig.GOOGLE_WEB_CLIENT_ID`.  
Добавьте SHA-1 debug/release в Google Cloud Console для package `com.replyai`.

## Стек

Kotlin, Hilt, Retrofit, Coroutines, Navigation, Material 3, Lottie, Shimmer
