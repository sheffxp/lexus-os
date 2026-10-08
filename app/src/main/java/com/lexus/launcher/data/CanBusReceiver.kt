package com.lexus.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CanBusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        val shortAction = action.substringAfterLast(".") // Укорачиваем для экрана

        // Если интент дошел — это уже 90% успеха! Сразу пишем об этом в сканер
        VehicleRepository.rawCanLog = "Пойман интент: $shortAction"
        Log.d("LexusCAN", "Захвачен широковещательный сигнал от авто: $action")

        val bundle = intent.extras
        if (bundle == null || bundle.isEmpty) {
            // Если extras пуст, возможно данные лежат в Uri-строке
            val dataUri = intent.dataString
            if (dataUri != null) {
                VehicleRepository.rawCanLog = "Act: $shortAction\nURI Data: $dataUri"
            } else {
                VehicleRepository.rawCanLog = "Act: $shortAction\n[Пакет пустой, без Extras]"
            }
            return
        }

        // Перебираем ключи (код парсинга остается прежним, как в прошлом шаге)
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value != null) {
                val dataString = when (value) {
                    is ByteArray -> value.joinToString(" ") { String.format("%02X", it) }
                    is IntArray -> value.joinToString(" ") { it.toString() }
                    is Int -> "Int: $value"
                    is String -> "Str: $value"
                    else -> value.toString()
                }

                VehicleRepository.rawCanLog = "Act: $shortAction\nKey: $key -> $dataString"

                // Проверка дверей
                if (value is ByteArray && value.isNotEmpty()) {
                    val dataTypeByte = value[0].toInt() and 0xFF
                    if (dataTypeByte == 0x01 && value.size > 1) {
                        VehicleRepository.isLeftDoorOpen = (value[1].toInt() and 0x01) != 0
                    }
                }
                return
            }
        }
    }
}