package com.lexus.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

object LexusCarManager {
    private var isRegistered = false

    // 1. ПРИЕМНИК ЖИВЫХ ДАННЫХ ИЗ ШТАТНОГО ДРАЙВЕРА CAN
    private val nativeCanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            try {
                // Извлекаем сырой массив данных CAN-декодера
                val canData = intent.getByteArrayExtra("DATA") ?: intent.getByteArrayExtra("value")

                if (canData != null) {
                    // Пишем сырые байты на флешку для анализа протокола Lexus RC
                    val hexString = canData.joinToString(" ") { String.format("%02X", it) }
                    LexusLogger.writeLog("LexusCAN_Raw", "Драйвер [$action] прислал: $hexString")

                    // Парсим стандартный для SMCU/LinkOS макет распределения байт
                    parseNativeBytes(canData)
                } else {
                    // Резервный вариант: некоторые прошивки передают данные в виде отдельных Extras
                    val speed = intent.getIntExtra("speed", intent.getIntExtra("Speed", -1))
                    val rpm = intent.getIntExtra("rpm", intent.getIntExtra("Rpm", -1))
                    val temp = intent.getIntExtra("temp", intent.getIntExtra("Temperature", -1))
                    val mileage = intent.getIntExtra("mileage", intent.getIntExtra("Odo", -1))

                    if (speed != -1 || rpm != -1) {
                        if (speed != -1) MapRepository.currentSpeed = "$speed км/ч"
                        if (rpm != -1) MapRepository.engineRpm = rpm
                        LexusLogger.writeLog("LexusCAN_Direct", "Скорость: $speed, Обороты: $rpm, Темп: $temp")
                    }
                }
            } catch (e: Exception) {
                Log.e("LexusCar", "Ошибка при обработке штатного CAN-пакета: ${e.message}")
            }
        }
    }

    // 2. ПОДКЛЮЧЕНИЕ К СИСТЕМНЫМ ПОТОКАМ МАГНИТОЛЫ
    fun connectToVehicle(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext

        val filter = IntentFilter().apply {
            // Подключаемся ко всем известным каналам трансляции LinkOS / SMCU2 / Экар
            addAction("com.android.ecar.canbus")
            addAction("android.intent.action.CANBUS_DATA")
            addAction("com.syu.canbus")
            addAction("com.fyt.canbus")
            addAction("action.canbus.data")
            addAction("com.lsw.canbus.data")
        }

        try {
            ContextCompat.registerReceiver(
                appContext,
                nativeCanReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED // Разрешено для системных процессов ГУ
            )
            isRegistered = true
            Log.d("LexusCar", "Успешно подключились к штатному системному потоку CAN прошивки.")
        } catch (e: Exception) {
            Log.e("LexusCar", "Ошибка подключения к драйверу: ${e.message}")
        }
    }

    // 3. ПАРСЕР СЫРЫХ БАЙТ (Типичный протокол для AC8227L/JingAn платформ)
    private fun parseNativeBytes(data: ByteArray) {
        if (data.size < 6) return

        // Пример базового разбора (зависит от конкретного типа коробочки CAN):
        // Обычно байт 2-3 — это Тахометр, байт 4 — Спидометр, байт 5 — Температура
        try {
            val rawSpeed = data[4].toInt() and 0xFF
            if (rawSpeed in 0..260) {
                MapRepository.currentSpeed = "$rawSpeed км/ч"
            }

            // Тахометр часто передается двумя байтами (высокий и низкий)
            val rpmHigh = data[2].toInt() and 0xFF
            val rpmLow = data[3].toInt() and 0xFF
            val rawRpm = (rpmHigh shl 8) or rpmLow
            if (rawRpm in 0..8000) {
                MapRepository.engineRpm = rawRpm
            }
        } catch (e: Exception) {
            // Игнорируем ошибки несовпадения индексов
        }
    }

    fun disconnect() {
        // Оставляем пустой метод для совместимости с MainActivity
    }
}