package com.lexus.launcher.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VoiceHelper {
    var isListening by mutableStateOf(false)
    var lastText by mutableStateOf("")
    var status by mutableStateOf("IDLE") // IDLE | LISTENING | PROCESSING | ERROR

    private val _response = MutableStateFlow("Я пока тестовый помощник.")
    val response: StateFlow<String> = _response.asStateFlow()

    // Запрос разрешения на запись
    fun requestRecordAudio(activity: ComponentActivity, onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
                return
            }
        }

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) onGranted() else {
                status = "ERROR"
                lastText = "Нет доступа к микрофону."
            }
        }
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * ТЕСТОВАЯ функция: имитирует распознавание речи.
     * В будущем тут будет вызов SpeechRecognizer или внешнего API.
     */
    fun simulateSpeechRecognition(input: String) {
        status = "PROCESSING"
        lastText = input

        // Имитация задержки сети/распознавания
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            delay(1500L)
            status = "IDLE"
            _response.value = when {
                input.contains("навигатор", ignoreCase = true) -> "Навигатор уже открыт на главном экране."
                input.contains("камера", ignoreCase = true) -> "Камеры контроля скорости отображаются на карте."
                input.contains("скорость", ignoreCase = true) -> "Текущая скорость берётся из CAN-шины."
                else -> "Я понял: «$input». Пока это тестовая фича."
            }
        }
    }
}