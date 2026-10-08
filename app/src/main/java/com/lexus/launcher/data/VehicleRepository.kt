package com.lexus.launcher.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lexus.launcher.ui.screens.LexusRadarState
import com.lexus.launcher.ui.screens.LexusTirePressureState
import com.lexus.launcher.ui.screens.LexusBsmState
import com.lexus.launcher.ui.screens.RadarDistance
import com.lexus.launcher.ui.screens.BsmStatus

import android.content.Context
import android.content.Intent

object VehicleRepository {

    // Наша отладочная строка, которую будет читать Compose
    var rawCanLog by mutableStateOf("Ожидание данных CAN...")
    // Живые стейты для Compose (Зона 3)
    var radarState by mutableStateOf(LexusRadarState())
    var tireState by mutableStateOf(LexusTirePressureState())
    var bsmState by mutableStateOf(LexusBsmState())

    var isLeftDoorOpen by androidx.compose.runtime.mutableStateOf(false)
    var isRightDoorOpen by mutableStateOf(false)

    var engineOilTemp by mutableStateOf("0 °C")
    var coolantTemp by mutableStateOf("0 °C")
    var turboOilTemp by mutableStateOf("0 °C")
    var voltageCAN by mutableStateOf("12.0 V")
    var fuelRangeCAN by mutableStateOf("0 км")



    // Метод парсинга байтов парктроников (Пример для декодеров Raise/Hiworld)
    fun parseRadarData(frontLeftByte: Int, frontCenterByte: Int, frontRightByte: Int, rearLeftByte: Int, rearCenterByte: Int, rearRightByte: Int) {
        radarState = LexusRadarState(
            frontLeft = convertByteToRadarDistance(frontLeftByte),
            frontCenter = convertByteToRadarDistance(frontCenterByte),
            frontRight = convertByteToRadarDistance(frontRightByte),
            rearLeft = convertByteToRadarDistance(rearLeftByte),
            rearCenter = convertByteToRadarDistance(rearCenterByte), // ИСПРАВЛЕНО: добавили суффикс Byte
            rearRight = convertByteToRadarDistance(rearRightByte)
        )
    }

    private fun convertByteToRadarDistance(byteValue: Int): RadarDistance {
        // Большинство CAN-декодеров возвращают расстояние от 0 (нет препятствия) до 3-5 (опасно)
        return when (byteValue) {
            0 -> RadarDistance.NONE
            1 -> RadarDistance.FAR
            2 -> RadarDistance.MEDIUM
            3, 4, 5 -> RadarDistance.DANGER
            else -> RadarDistance.NONE
        }
    }

    fun openRecentApps(context: Context) {
        val appMode = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appMode.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        // Если доступ РАЗРЕШЕН — мгновенно открываем наш список задач
        if (mode == android.app.AppOpsManager.MODE_ALLOWED) {
            TaskManager.refreshRunningTasks(context)
            TaskManager.isTaskOverlayVisible = true
        } else {
            android.util.Log.w("LexusUI", "Нет прав на UsageStats. Открытие системного окна согласия...")
            // Если прав нет — открываем водителю меню "Доступ к истории использования"
            try {
                val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Резервный переход в общие настройки, если китайское ГУ урезало экшен
                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}