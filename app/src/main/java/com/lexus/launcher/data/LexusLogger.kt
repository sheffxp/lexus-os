package com.lexus.launcher.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LexusLogger {
    private const val TAG = "LexusLogger"
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null

    // Находим путь к смонтированной USB-флешке в Android-магнитоле
    fun initLogger(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ищем внешние накопители в системной папке /storage/
                val storageDir = File("/storage")
                val files = storageDir.listFiles()

                var usbPath: File? = null

                if (files != null) {
                    for (file in files) {
                        // Исключаем внутреннюю память (emulated, self, enc_emulated)
                        if (file.isDirectory && file.canWrite() &&
                            !file.name.contains("emulated") &&
                            !file.name.contains("self")) {
                            usbPath = file
                            break
                        }
                    }
                }

                // Резервный метод через внешние медиа-каталоги Android
                if (usbPath == null) {
                    val externalDirs = context.getExternalFilesDirs(null)
                    if (externalDirs.size > 1 && externalDirs[1] != null) {
                        // Второй элемент массива — это почти всегда подключенная флешка
                        usbPath = externalDirs[1]
                    }
                }

                if (usbPath != null) {
                    // Создаем файл логов в корне флешки
                    logFile = File(usbPath, "lexus_os_debug.txt")
                    if (!logFile!!.exists()) {
                        logFile!!.createNewFile()
                    }
                    writeLog("SYSTEM", "==================================================")
                    writeLog("SYSTEM", "Логгер Lexus OS успешно запущен. Путь: ${logFile!!.absolutePath}")
                } else {
                    Log.w(TAG, "USB-флешка не найдена. Логи пишутся только в Logcat.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка инициализации файла логов: ${e.message}")
            }
        }
    }

    // Метод записи лога в файл (вызывать вместо или вместе с Log.d)
    fun writeLog(tag: String, message: String) {
        // Выводим в стандартный Logcat, чтобы видеть в Android Studio
        Log.d(tag, message)

        val file = logFile ?: return

        // Пишем в файл в асинхронном фоновом режиме, чтобы не тормозить UI лаунчера
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timeStamp = timeFormat.format(Date())
                val logLine = "$timeStamp [$tag]: $message\n"

                FileWriter(file, true).use { writer ->
                    writer.append(logLine)
                }
            } catch (e: Exception) {
                // Игнорируем ошибки записи, чтобы не уронить приложение
            }
        }
    }
}