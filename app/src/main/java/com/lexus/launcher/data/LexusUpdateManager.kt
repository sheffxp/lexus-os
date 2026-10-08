package com.lexus.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.lexus.launcher.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object LexusUpdateManager {
    // Реактивные стейты для вывода данных на экран обновлений
    var currentVersionName by mutableStateOf("v${BuildConfig.VERSION_NAME}")
    var serverVersionName by mutableStateOf("---")
    var changelogText by mutableStateOf("Описание изменений отсутствует.")
    var updateStatusMessage by mutableStateOf("Система проверена. Обновлений не найдено.")

    var isNewVersionAvailable by mutableStateOf(false)
    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableStateOf(0f)

    private var apkDownloadUrl = ""

    // 1. ПРОВЕРКА ОБНОВЛЕНИЙ НА ВАШЕМ СЕРВЕРЕ
    fun checkServerForUpdates() {
        updateStatusMessage = "Подключение к серверу обновлений Lexus..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Укажите URL вашего сервера, где лежит JSON-конфиг прошивки
                val url = URL("https://sheffxp.github.io/lexus-ota/lexus_update.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000

                if (connection.responseCode == 200) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonString)

                    val serverVersionCode = json.getInt("versionCode")
                    serverVersionName = json.getString("versionName")
                    changelogText = json.getString("changelog")
                    apkDownloadUrl = json.getString("downloadUrl")

                    // Сравниваем локальный versionCode лаунчера с серверным
                    if (serverVersionCode > BuildConfig.VERSION_CODE) {
                        isNewVersionAvailable = true
                        updateStatusMessage = "ДОСТУПНА НОВАЯ ВЕРСИЯ ЛАУНЧЕРА!"
                    } else {
                        isNewVersionAvailable = false
                        updateStatusMessage = "У вас установлена самая свежая версия."
                    }
                } else {
                    updateStatusMessage = "Ошибка сервера" //: ${connection.responseCode}"
                }
            } catch (e: Exception) {
                updateStatusMessage = "Не удалось связаться с сервером обновлений."
            }
        }
    }

    // 2. СКАЧИВАНИЕ И АВТОМАТИЧЕСКАЯ УСТАНОВКА НОВОГО APK
    fun downloadAndInstallApk(context: Context) {
        if (apkDownloadUrl.isEmpty() || isDownloading) return
        isDownloading = true
        updateStatusMessage = "Скачивание новой прошивки..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(apkDownloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val input = connection.inputStream

                // Сохраняем APK во внутренний кэш лаунчера
                val outputFile = File(context.externalCacheDir, "lexus_os_update.apk")
                if (outputFile.exists()) outputFile.delete()

                val output = FileOutputStream(outputFile)
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        downloadProgress = total.toFloat() / fileLength.toFloat()
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                updateStatusMessage = "Скачивание завершено. Запуск установки..."
                isDownloading = false

                // Запуск штатного установщика Android пакетов магнитолы
                launchAndroidInstaller(context, outputFile)

            } catch (e: Exception) {
                isDownloading = false
                updateStatusMessage = "Ошибка при скачивании файла" //: ${e.message}
            }
        }
    }

    private fun launchAndroidInstaller(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Разрешаем чтение нашего APK системному установщику пакетов
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            setDataAndType(apkUri, "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }
}