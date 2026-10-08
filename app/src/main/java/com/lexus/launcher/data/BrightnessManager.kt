package com.lexus.launcher.data

import android.app.Activity
import android.view.WindowManager

object BrightnessManager {
    /**
     * Устанавливает яркость для конкретного Activity.
     * Значение от 0.0 (полностью тёмный) до 1.0 (максимальная яркость).
     */
    fun setActivityBrightness(activity: Activity, brightness: Float) {
        val lp = activity.window.attributes
        lp.screenBrightness = brightness.coerceIn(0.0f, 1.0f)
        activity.window.attributes = lp
    }

    /**
     * Возвращает текущую установленную яркость (если не задавали — вернёт WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
     * но для простоты будем хранить своё значение в памяти).
     */
    @Volatile
    var currentBrightness: Float = 1.0f
}