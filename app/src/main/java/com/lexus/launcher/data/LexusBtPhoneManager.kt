package com.lexus.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object LexusBtPhoneManager {

    // Метод вызова абонента через штатный Bluetooth-модуль магнитолы
    fun makeBluetoothCall(context: Context, phoneNumber: String) {
        val appContext = context.applicationContext
        Log.d("LexusPhone", "Инициализация Bluetooth-вызова на номер:  $phoneNumber")

        try {
            // 1. НАДЁЖНЫЙ ВАРИАНТ ДЛЯ LinkOS / SMCU2: Заводской интент Bluetooth-микшера
            // Передаем команду "Позвонить" (action=6 или dial) напрямую в Bluetooth-сервер
            val btIntent = Intent("com.android.ecar.bt.action.CALL").apply {
                putExtra("number", phoneNumber)
                putExtra("cmd", 6) // Команда старта вызова в прошивках L_OS
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            appContext.sendBroadcast(btIntent)
            Log.d("LexusPhone", "Автомобильный Broadcast com.android.ecar.bt успешно отправлен.")

        } catch (e: Exception) {
            Log.w("LexusPhone", "Заводской Broadcast не принят, пробуем альтернативный экшен...")
        }

        try {
            // 2. РЕЗЕРВНЫЙ ВАРИАНТ ДЛЯ КИТАЙСКИХ ГУ (Пакетный запуск звонилки в фоне)
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:\$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Проверяем, какой Bluetooth-диалер зашит в вашей системе, чтобы направить вызов туда
            val packageManager = appContext.packageManager
            val btDialerPackages = listOf("com.android.ecar.bt", "com.fyt.bluetooth", "com.syu.bt")
            var launched = false

            for (pkg in btDialerPackages) {
                try {
                    packageManager.getPackageInfo(pkg, 0)
                    intent.`package` = pkg // Жестко привязываем к автомобильному BT приложению
                    appContext.startActivity(intent)
                    launched = true
                    Log.d("LexusPhone", "Вызов успешно перенаправлен в пакет: \$pkg")
                    break
                } catch (e: Exception) {}
            }

            // 3. АВАРИЙНЫЙ ВАРИАНТ: Если все автомобильные надстройки вырезаны, вызываем стандартный системный Dialer
            if (!launched) {
                intent.`package` = null
                appContext.startActivity(intent)
            }

        } catch (e: Exception) {
            Log.e("LexusPhone", "Критическая ошибка при попытке совершить звонок: \${e.message}")
        }
    }
}