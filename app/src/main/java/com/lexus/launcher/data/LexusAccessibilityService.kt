package com.lexus.launcher.data

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class LexusAccessibilityService : AccessibilityService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == "com.lexus.launcher.ACTION_OPEN_RECENTS") {
            Log.d("LexusAccessibility", "Триггер принят. Отправка MCU-сигнала KEYCODE_APP_SWITCH (187)...")

            try {
                // МЕТОД А: Системный Broadcast эмуляции нажатия физической кнопки "Обзор/Квадрат"
                val keyIntent = Intent("android.intent.action.KEYCODE_APP_SWITCH").apply {
                    // Код клавиши 187 — это стандартный KEYCODE_APP_SWITCH в Android
                    putExtra("keycode", 187)
                    // Разрешаем отправку из фонового сервиса
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                sendBroadcast(keyIntent)
                Log.d("LexusAccessibility", "Интент KEYCODE_APP_SWITCH отправлен.")

                // МЕТОД Б: Резервный Broadcast для китайских платформ (TS10/Teyes),
                // имитирующий клик по кнопке "Недавние" из статусбара
                val carStatusIntent = Intent("com.ts.intent.action.SHOW_RECENTS")
                sendBroadcast(carStatusIntent)

            } catch (e: Exception) {
                Log.e("LexusAccessibility", "Ошибка отправки интента клавиши: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}