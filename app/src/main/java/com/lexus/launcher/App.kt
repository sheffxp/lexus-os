package com.lexus.launcher

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // ЖЕЛЕЗОБЕТОННО: Ключ задается в первую миллисекунду жизни процесса
        MapKitFactory.setApiKey("4b7c9cba-d142-4db5-8e87-40062115ac09")
        MapKitFactory.initialize(this)
    }
}