package com.lexus.launcher.data

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class RunningApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

object TaskManager {
    val runningApps = mutableStateListOf<RunningApp>()
    var isTaskOverlayVisible by mutableStateOf(false)

    // Наш собственный, независимый черный список закрытых водителем приложений
    private val closedAppsBlacklist = mutableSetOf<String>()

    fun refreshRunningTasks(context: Context) {
        val appContext = context.applicationContext
        val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val packageManager = appContext.packageManager

        if (usageStatsManager == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val localList = mutableListOf<RunningApp>()
            val endTime = System.currentTimeMillis()
            // Берем историю за последние 12 часов, чтобы точно поймать всё запущенное в поездке
            val startTime = endTime - (1000 * 60 * 60 * 12)

            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

            if (stats != null && stats.isNotEmpty()) {
                // Сортируем по времени последнего открытия
                val sortedStats = stats.sortedByDescending { it.lastTimeUsed }

                for (usageStat in sortedStats) {
                    val pkgName = usageStat.packageName

                    // 1. ИСКЛЮЧАЕМ системный мусор и сам лаунчер
                    if (pkgName == context.packageName || pkgName.contains("com.android.systemui")) continue

                    // 2. АВТОНОМНЫЙ ФИЛЬТР: Если водитель нажал Х, приложение в черном списке — жестко игнорируем его!
                    if (closedAppsBlacklist.contains(pkgName)) continue

                    // 3. Отсекаем программы, которые вообще не запускались (время предпросмотра нулевое)
                    if (usageStat.totalTimeInForeground <= 0) continue

                    try {
                        // Проверяем, есть ли у пакета иконка запуска в авто
                        packageManager.getLaunchIntentForPackage(pkgName) ?: continue

                        val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(appInfo)

                        if (localList.none { it.packageName == pkgName }) {
                            localList.add(RunningApp(pkgName, appName, icon))
                        }

                        // Выводим оптимальное количество вкладок на экране Lexus RC
                        if (localList.size >= 6) break

                    } catch (e: Exception) {
                        // Игнорируем ошибки чтения поврежденных пакетов
                    }
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                runningApps.clear()
                runningApps.addAll(localList)
            }
        }
    }

    // Нажатие на крестик Х
    fun closeTask(context: Context, packageName: String) {
        try {
            android.util.Log.d("LexusTask", "Ручное закрытие пакета: $packageName")

            // 1. Мгновенно добавляем пакет в наш черный список, чтобы он сразу исчез из UI
            closedAppsBlacklist.add(packageName)

            // 2. Удаляем из текущего живого Compose списка для моментального визуального отклика
            runningApps.removeAll { it.packageName == packageName }

            // 3. Вызываем легальное окно Android 14 для физической остановки приложения водителем
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // 4. Параллельно сбрасываем фоновый кэш ОЗУ магнитолы
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)

        } catch (e: Exception) {
            android.util.Log.e("LexusTask", "Ошибка при выполнении закрытия: " + e.localizedMessage)
        }
    }

    // Кнопка "ОЧИСТИТЬ ПАМЯТЬ ГУ"
    fun clearAllTasks(context: Context) {
        if (runningApps.isEmpty()) return

        // Вносим все текущие запущенные программы в черный список
        for (app in runningApps) {
            closedAppsBlacklist.add(app.packageName)
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(app.packageName)
            } catch (e: Exception) {}
        }

        runningApps.clear()
        isTaskOverlayVisible = false
    }

    // КРИТИЧЕСКИ ВАЖНО: Метод очистки черного списка при повторном запуске программ.
    // Если водитель кликнет по иконке Навигатора на главном экране — мы обязаны вычеркнуть его из черного списка!
    fun notifyAppLaunched(packageName: String) {
        closedAppsBlacklist.remove(packageName)
    }
}