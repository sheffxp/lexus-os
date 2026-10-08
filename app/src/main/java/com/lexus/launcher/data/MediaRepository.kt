package com.lexus.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MediaRepository {
    var trackName by mutableStateOf("Нет трека")
    var artistName by mutableStateOf("Яндекс Музыка закрыта")
    var isPlaying by mutableStateOf(false)
    var progressPercent by mutableStateOf(0f)
    var currentTime by mutableStateOf("00:00")
    var totalTime by mutableStateOf("00:00")
    var hasAttemptedAutostart = false

    // Новое состояние: хранит текущую обложку альбома в виде Bitmap
    var albumArt by mutableStateOf<Bitmap?>(null)

    var activeController: MediaController? = null

    fun sendPlayPause() {
        val state = activeController?.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            activeController?.transportControls?.pause()
        } else {
            activeController?.transportControls?.play()
        }
    }

    fun sendNext() { activeController?.transportControls?.skipToNext() }
    fun sendPrevious() { activeController?.transportControls?.skipToPrevious() }

    // --- УМНЫЙ ФОНОВЫЙ ЗАПУСК ЯНДЕКС МУЗЫКИ ---
    fun warmUpYandexMusic(context: Context) {
        if (hasAttemptedAutostart || activeController != null) return
        hasAttemptedAutostart = true

        try {
            // МЕТОД 1: Посылаем системный интент кнопки "PLAY" конкретно для Яндекс Музыки
            // Это заставляет Android запустить фоновый медиа-сервис Яндекса без открытия окна
            val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                `package` = "ru.yandex.music"
                putExtra(Intent.EXTRA_KEY_EVENT,
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
                )
            }
            context.sendBroadcast(mediaIntent)

            val mediaIntentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                `package` = "ru.yandex.music"
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            }
            context.sendBroadcast(mediaIntentUp)

        } catch (e: Exception) {
            // МЕТОД 2 (РЕЗЕРВНЫЙ): Если магнитола заблокировала Broadcast, открываем приложение на 100мс и закрываем
            val launchIntent = context.packageManager.getLaunchIntentForPackage("ru.yandex.music")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)

                // Возвращаем лаунчер на передний план через микро-задержку
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(homeIntent)
                }, 250) // 250 миллисекунд достаточно, чтобы сервис Яндекса проснулся
            }
        }
    } // Живые стейты громкости для вывода в статусбар или виджет
    var currentVolume by mutableStateOf(15)
    var maxVolume by mutableStateOf(30)

    private var audioManager: AudioManager? = null

    // Специализированный автомобильный ресивер громкости
    private val volumeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            LexusLogger.writeLog("LexusVolume", "Пойман интент изменения звука: $action")

            // 1. Метод для стандартного Android
            if (action == "android.media.VOLUME_CHANGED_ACTION") {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == AudioManager.STREAM_MUSIC) {
                    updateVolumeData()
                }
            }
            // 2. Метод для китайских платформ (TopWay/TS10/Syu/Teyes)
            else {
                // Большинство магнитол отдают текущую громкость в Extras по ключу "volume" или "value"
                val vol = intent.getIntExtra("volume", intent.getIntExtra("value", -1))
                if (vol != -1) {
                    currentVolume = vol
                    LexusLogger.writeLog("LexusVolume", "Автомобильная громкость перехвачена из MCU: $currentVolume")
                } else {
                    // Резервный опрос
                    updateVolumeData()
                }
            }
        }
    }

    fun initAudio(context: Context) {
        if (audioManager != null) return

        val appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        updateVolumeData()

        // Настраиваем фильтр под точную спецификацию прошивки L_OS_P / LinkOS / SMCU2
        val filter = IntentFilter().apply {
            addAction("com.android.ecar.action.VOLUME_CHANGED") // Основной автомобильный интент LinkOS
            addAction("android.intent.action.AV_VOLUME_CHANGED") // Дополнительный AV-канал прошивки
            addAction("android.media.VOLUME_CHANGED_ACTION")     // Стандартный Android поток
            addAction("com.ts.intent.volume")                    // Совместимость
            addAction("com.syu.ms.volume")
        }

        try {
            // Регистрируем ресивер с официальным флагом видимости для внешних MCU систем ГУ
            androidx.core.content.ContextCompat.registerReceiver(
                appContext,
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        val action = intent?.action ?: return
                        android.util.Log.d("LexusVolume", "LinkOS: Получен интент громкости: $action")

                        // Извлекаем значение по специфичным ключам вашей прошивки
                        val vol = intent.getIntExtra("volume_value",
                            intent.getIntExtra("volume",
                                intent.getIntExtra("value", -1)))

                        if (vol != -1) {
                            currentVolume = vol
                            android.util.Log.d("LexusVolume", "Громкость успешно перехвачена из SMCU2: $currentVolume")
                        } else {
                            // Если extras пусты, делаем прямой опрос аудиоменеджера Android
                            updateVolumeData()
                        }
                    }
                },
                filter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
            Log.d("LexusVolume", "Автомобильный ресивер звука LinkOS успешно запущен.")
        } catch (e: Exception) {
            Log.e("LexusVolume", "Ошибка запуска ресивера звука: ${e.message}")
        }
    }

    private fun updateVolumeData() {
        audioManager?.let { am ->
            currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }
    }
}
