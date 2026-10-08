package com.lexus.launcher.data

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaNotificationListener : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager

        // Регистрируем слушатель активных аудио-сессий в системе
        val componentName = ComponentName(this, MediaNotificationListener::class.java)
        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)

        // Первичная проверка при запуске
        try {
            val activeControllers = mediaSessionManager.getActiveSessions(componentName)
            updateActiveSession(activeControllers)
        } catch (e: SecurityException) {
            Log.e("LexusMedia", "Нет разрешения на чтение уведомлений/сессий")
        }
    }

    private fun updateActiveSession(controllers: List<android.media.session.MediaController>?) {
        // Ищем сессию Яндекс Музыки
        val yandexController = controllers?.firstOrNull { it.packageName == "ru.yandex.music" }
            ?: controllers?.firstOrNull() // Если Яндекса нет, берем любой активный плеер

        if (yandexController != null) {
            MediaRepository.activeController = yandexController

            // Регистрируем обратный вызов для отслеживания изменений внутри трека
            yandexController.registerCallback(object : android.media.session.MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    metadata?.let {
                        MediaRepository.trackName = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Неизвестный трек"
                        MediaRepository.artistName = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Неизвестный артист"

                        // ВЫТАСКИВАЕМ ОБЛОЖКУ (Проверяем два возможных ключа системы)
                        val bitmap = it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                            ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)
                        MediaRepository.albumArt = bitmap

                        val duration = it.getLong(MediaMetadata.METADATA_KEY_DURATION)
                        MediaRepository.totalTime = formatTime(duration)
                    }
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    state?.let {
                        MediaRepository.isPlaying = it.state == PlaybackState.STATE_PLAYING

                        // Вычисляем прогресс
                        val currentPos = it.position
                        val duration = yandexController.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 1L
                        MediaRepository.progressPercent = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        MediaRepository.currentTime = formatTime(currentPos)
                    }
                }
            })

            // Считываем первичные данные, если плеер уже играет
            yandexController.metadata?.let {
                MediaRepository.trackName = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Нет названия"
                MediaRepository.artistName = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Нет исполнителя"

                // Первичный сбор обложки при старте сессии
                MediaRepository.albumArt = it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)

                MediaRepository.totalTime = formatTime(it.getLong(MediaMetadata.METADATA_KEY_DURATION))
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
    }
}