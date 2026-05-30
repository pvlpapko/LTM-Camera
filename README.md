# LowLatencyCamStreamer

Новый Android-проект вместо старого APK NDI Camera.
Цель: Android 10+ с минимальной задержкой, аппаратное кодирование H.264 и потоковая передача.

## Что уже заложено

- Android 10+ (`minSdk 29`), `targetSdk 35`.
- 64-bit/новые прошивки: нет старых 32-bit `.so` из NDI APK.
- Camera2/OpenGL preview через RootEncoder.
- RTSP server на телефоне: `rtsp://IP_ТЕЛЕФОНА:8554`.
- Подготовка под RTSP/SRT push URL.
- Подготовлена зависимость WebRTC SDK для следующего шага WHIP/signaling.
- Микрофон on/off.
- Переключение передней/задней камеры.
- Битрейт на лету.
- Основа под автофокус/цветокоррекцию через Camera2/OpenGL.

## Почему RootEncoder

RootEncoder умеет кодировать H.264/H.265/AAC/OPUS через Android MediaCodec и отправлять поток по RTSP/SRT/UDP/RTMP. RTSP-Server добавлен как плагин, чтобы телефон мог сам раздавать RTSP-поток без отдельного сервера.

## Как собрать

1. Открой папку в Android Studio.
2. Дождись Gradle Sync.
3. Build → Build APK / Run.
4. Установи на Android 10+.

## Быстрый тест RTSP

1. Запусти приложение.
2. Оставь режим `RTSP server`.
3. Нажми `Старт`.
4. В VLC/OBS открой:

```text
rtsp://IP_ТЕЛЕФОНА:8554
```

## SRT/RTSP push

В интерфейсе уже есть поле URL, например:

```text
srt://192.168.1.10:9000?mode=caller&latency=40
rtsp://192.168.1.10:8554/live
```

В текущей версии push-режим оставлен как точка подключения, потому что API RootEncoder 2.7.x лучше сверить при Gradle Sync в Android Studio и подключить конкретный класс `SrtCamera2` / `RtspCamera2` из версии, которая подтянется с JitPack.

## WebRTC

WebRTC без сервера не работает: нужен signaling/WHIP/WHEP или свой WebSocket-сервер. Зависимость добавлена:

```kotlin
implementation("io.github.webrtc-sdk:android:144.7559.05")
```

Следующий шаг — добавить WHIP publish endpoint, например Ant Media / MediaMTX / LiveKit / свой сервер.

## Настройки низкой задержки

Рекомендуемые стартовые параметры:

- 1280x720
- 30 fps
- H.264
- bitrate 4–8 Mbps для Wi-Fi
- SRT latency 40–80 ms в хорошей сети
- RTSP over UDP, если клиент/сеть позволяют

## Важно

Это исходный проект, а не готовый APK. В среде ChatGPT я не могу полноценно прогнать Android Gradle build с внешними JitPack-зависимостями, поэтому проект нужно открыть в Android Studio и собрать там.
