# Как довести StreamPanel до полной готовности

Эта инструкция разделяет работу на две части:

- что уже подготовлено в проекте;
- что нужно сделать на твоем ПК, потому что это требует установки SDK, устройств, токенов и реальных аккаунтов.

## 1. Установи инструменты

Открой PowerShell от обычного пользователя в папке проекта:

```powershell
cd C:\Users\admin\Desktop\streamdeck
```

Сначала попробуй автоматическую установку:

```powershell
.\tools\install-prereqs-windows.ps1 -IncludeAndroidStudio
```

Если Windows спросит разрешение, подтверди. После установки закрой PowerShell и открой его заново.

Если автоматическая установка не сработала, установи вручную:

- Android Studio: https://developer.android.com/studio
- .NET 8 SDK: https://dotnet.microsoft.com/download/dotnet/8.0
- JDK 17: https://adoptium.net/temurin/releases/?version=17
- Gradle не обязателен: в проекте уже есть `android\gradlew.bat`

## 2. Настрой Android SDK

1. Открой Android Studio.
2. Открой `Settings` -> `Languages & Frameworks` -> `Android SDK`.
3. Установи:
   - Android SDK Platform 35;
   - Android SDK Build-Tools;
   - Android SDK Platform-Tools;
   - Android Emulator, если нужен эмулятор.
4. Проверь переменную окружения:
   - `ANDROID_HOME` или `ANDROID_SDK_ROOT` должна указывать на Android SDK.

Обычно путь такой:

```text
C:\Users\admin\AppData\Local\Android\Sdk
```

## 3. Проверь окружение

В новой PowerShell-сессии выполни:

```powershell
cd C:\Users\admin\Desktop\streamdeck
.\tools\check-prereqs.ps1
```

Все пункты должны быть `[ok]`. Для Gradle достаточно строки `Gradle Wrapper`, отдельно ставить Gradle не нужно.

## 4. Собери проект

Сборка всего сразу:

```powershell
.\tools\build-all.ps1
```

Отдельно Android:

```powershell
.\tools\build-android.ps1
```

Отдельно Windows server:

```powershell
.\tools\build-server.ps1
```

Если появятся ошибки сборки, пришли текст ошибки целиком. Их нужно исправлять уже по фактическому выводу Gradle или dotnet.

## 5. Запусти Windows companion server

На ПК выполни:

```powershell
.\tools\run-server.ps1
```

Проверь статус в браузере:

```text
http://localhost:17820/status
```

Если Android-планшет в той же Wi-Fi сети, узнай IP ПК:

```powershell
ipconfig
```

Ищи IPv4 адрес Wi-Fi адаптера, например:

```text
192.168.1.34
```

В Android-приложении укажи:

```text
Host: 192.168.1.34
Port: 17820
```

## 6. Подключи реальные интеграции

Для живого теста нужны реальные ключи:

- OBS: включи `Tools` -> `WebSocket Server Settings`, запомни порт `4455` и пароль.
- Discord: создай webhook URL в настройках канала.
- Spotify: получи OAuth token с правами `user-modify-playback-state`.
- Home Assistant: создай long-lived access token.
- Philips Hue: создай bridge app key.
- MQTT: нужен адрес брокера, порт, topic и message.
- Streamlabs: нужен API token.

Без этих ключей код есть, но проверить живой сервис невозможно.

## 7. Протестируй сценарии

Минимальный тест перед тем как считать проект готовым:

- Android запускается на планшете.
- Dashboard показывает стартовые кнопки.
- Сетка меняется в настройках.
- Long press открывает editor.
- Drag меняет позицию кнопки.
- PC connection показывает Connected.
- Кнопка URL открывает сайт на ПК.
- Hotkey отправляется в активное окно Windows.
- Send text печатает текст на ПК.
- OBS screen переключает сцену или получает статус.
- `/status` сервера показывает последние команды.

## 8. Release-подготовка

Перед публикацией:

- создать release signing key для Android;
- собрать release APK/AAB;
- добавить privacy policy;
- проверить permissions;
- добавить crash reporting;
- сделать экспорт/импорт профилей;
- ограничить опасные server-команды allowlist-ом;
- проверить приложение на реальном планшете минимум 1-2 часа.
