package com.lexus.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.lexus.launcher.ui.screens.MainLauncherScreen
import com.lexus.launcher.ui.screens.AppsListScreen
import com.lexus.launcher.ui.screens.DialerScreen
import com.lexus.launcher.ui.theme.LexusDeepBlack
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.lexus.launcher.data.BrightnessManager
import com.lexus.launcher.data.BrightnessManager.currentBrightness
import com.lexus.launcher.data.LexusNaviManager
import com.lexus.launcher.data.MapRepository
import com.lexus.launcher.data.WifiAutoConnector
import com.lexus.launcher.ui.components.StatusBarManager
import com.lexus.launcher.ui.screens.BrightnessControls
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color


enum class AppScreen { MAIN_LAUNCHER, ALL_APPS, DIALER }

class MainActivity : ComponentActivity() {

    var showDialer by mutableStateOf(false)
    var dialerNumber by mutableStateOf("")

    val canReceiver = com.lexus.launcher.data.CanBusReceiver()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDialIntent(intent)

        // Твои сети (SSID, пароль) — можно хранить в настройках лаунчера
        val networks = listOf(
            "spyX" to "11111111",
            "MyPhoneHotspot" to "hotspotpass"
        )
        WifiAutoConnector.suggestNetworks(this, networks)

        // Проверка/назначение диалером по умолчанию
        checkDefaultDialer()

        // Запускаем поиск флешки и создание файла
        com.lexus.launcher.data.LexusLogger.initLogger(this)
        // Запуск официального авто-менеджера
        com.lexus.launcher.data.LexusCarManager.connectToVehicle(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Логика подписки на спутники GPS
        setupLocationUpdates()

        hideSystemNavigation(window)

        val filter = android.content.IntentFilter().apply {
            addAction("android.intent.action.MCU_NOTIFICATION")
            addAction("com.tw.mcu.status")
            addAction("com.tw.mcu.data")
            addAction("com.syu.canbus")
            addAction("com.syu.ms.canbus")
            addAction("com.suding.speedplay.action.CAN_DATA")
            addAction("android.intent.action.CANBUS_DATA")
            addAction("com.android.ecar.canbus")
        }

// 2. Безопасная регистрация с явным указанием флага RECEIVER_EXPORTED для Android 14+ (API 34+)
        ContextCompat.registerReceiver(
            this,
            canReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Принудительно отправляем широковещательный пинг в систему магнитолы,
        // чтобы "разбудить" McuManager и CanBus-службу прошивки
        try {
            sendBroadcast(Intent("com.syu.canbus.status"))
            sendBroadcast(Intent("com.tw.mcu.status"))
            sendBroadcast(Intent("android.intent.action.CANBUS_WAKEUP"))
            Log.d("LexusCAN", "Отправлен стартовый пинг для активации CAN-шины")
        } catch (e: Exception) {
            Log.e("LexusCAN", "Не удалось отправить пинг пробуждения: ${e.message}")
        }

        currentBrightness = BrightnessManager.currentBrightness
        BrightnessManager.setActivityBrightness(this, currentBrightness)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.MAIN_LAUNCHER) }

            Surface(modifier = Modifier.fillMaxSize(), color = LexusDeepBlack) {
                when (currentScreen) {
                    AppScreen.MAIN_LAUNCHER -> {
                        MainLauncherScreen(
                            onOpenAllApps = { currentScreen = AppScreen.ALL_APPS },
                            onOpenDialer = { currentScreen = AppScreen.DIALER },
                            onLaunchApp = { packageName -> launchApp(packageName) } // Передаем запуск
                        )
                    }
                    AppScreen.ALL_APPS -> {
                        AppsListScreen(
                            onBackToMain = { currentScreen = AppScreen.MAIN_LAUNCHER },
                            onLaunchApp = { packageName -> launchApp(packageName) } // Передаем запуск
                        )
                    }
                    AppScreen.DIALER -> {
                        DialerScreen(
                            initialContactName = "Жена",
                            onBackToMain = { currentScreen = AppScreen.MAIN_LAUNCHER },
                            onPerformCall = { number -> makePhoneCall(number) }
                        )
                    }

                }

                // Панель яркости (внизу по центру, поверх всего)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    BrightnessControls(
                        onDecrease = {
                            currentBrightness = (currentBrightness - 0.1f).coerceIn(0.0f, 1.0f)
                            BrightnessManager.currentBrightness = currentBrightness
                            BrightnessManager.setActivityBrightness(this@MainActivity, currentBrightness)
                        },
                        onIncrease = {
                            currentBrightness = (currentBrightness + 0.1f).coerceIn(0.0f, 1.0f)
                            BrightnessManager.currentBrightness = currentBrightness
                            BrightnessManager.setActivityBrightness(this@MainActivity, currentBrightness)
                        },
                        current = currentBrightness
                    )
                }
            }
        }
        StatusBarManager.init(this)
        com.lexus.launcher.data.MediaRepository.initAudio(this)


    }

    // --- НОВАЯ ФУНКЦИЯ: скрываем навигацию без deprecated ---
    private fun hideSystemNavigation(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(
                    WindowInsets.Type.navigationBars() or
                WindowInsets.Type.statusBars()
                )
                // Без свайпа обратно (для ГУ это обычно лучше)
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_DEFAULT
            }
        } else {
            // Для старых Android (8–10) — тут оставляем @Suppress, потому что альтернативы нет
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun checkDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = getSystemService(TELECOM_SERVICE)
                    as android.telecom.TelecomManager

            // Узнаём текущий диалер по умолчанию
            val currentDefault = telecomManager.defaultDialerPackage
            val myPackage = packageName

            Log.d("LexusCall", "Текущий диалер: $currentDefault, наш пакет: $myPackage")

            if (currentDefault != myPackage) {
                // Открываем настройки выбора приложения для звонков
                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    Log.d("LexusCall", "Открыты настройки default apps")
                } catch (e: Exception) {
                    // Если ACTION_MANAGE_DEFAULT_APPS не поддерживается —
                    // пробуем через ROLE_CALLER (Android 9+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tryRoleCaller()
                    }
                    Log.e("LexusCall", "Не удалось открыть настройки", e)
                }
            }
        }
    }

    private fun tryRoleCaller() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE)
                    as android.app.role.RoleManager

            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(
                    android.app.role.RoleManager.ROLE_DIALER
                )
                try {
                    startActivity(intent)
                    Log.d("LexusCall", "Открыт запрос ROLE_DIALER")
                } catch (e: Exception) {
                    Log.e("LexusCall", "Не удалось открыть ROLE_DIALER", e)
                }
            }
        }
    }


    // Универсальный метод запуска приложения по его Package Name
    private fun launchApp(packageName: String) {
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            // Если приложение не установлено на магнитоле, выводим аккуратное уведомление
            Toast.makeText(this, "Приложение $packageName не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    //
    private fun makePhoneCall(number: String) {
        if (number.isEmpty()) return

        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
        var callStarted = false

        // СПИСОК СИСТЕМНЫХ ИМЕН ПАКЕТОВ BLUETOOTH-ЗВОНИЛОК ДЛЯ МАГНИТОЛ
        val bluetoothPackages = listOf(
            "com.syu.bt",            // Платформы FYT / Joying / Teyes (часть прошивок)
            "com.tw.bt",             // Платформы TopWay / TS10 / TS18
            "com.android.ecar.bt",   // Универсальные платформы Allwinner / MediaTek
            "com.android.bluetooth", // Стандартный Bluetooth Android
            "com.fyt.bluetooth"      // Расширенный FYT сервис
        )

        // МЕТОД 1: Попытка запустить Bluetooth-приложение напрямую через PackageManager
        for (pkg in bluetoothPackages) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    // Добавляем номер телефона в стандартные автомобильные Extras
                    launchIntent.putExtra("phone_number", cleanNumber)
                    launchIntent.putExtra("number", cleanNumber)
                    launchIntent.putExtra("dial_number", cleanNumber)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                    callStarted = true
                    break
                }
            } catch (e: Exception) {
                // Игнорируем и пробуем следующий пакет
            }
        }

        // МЕТОД 2: Отправка скрытой Broadcast-команды в MCU (если приложение не открылось напрямую)
        if (!callStarted) {
            val broadcastIntents = listOf(
                Intent("com.syu.bt.dial").apply { putExtra("number", cleanNumber) },
                Intent("com.tw.bt.dial").apply { putExtra("number", cleanNumber) },
                Intent("android.intent.action.BT_DIAL").apply { putExtra("number", cleanNumber) }
            )

            for (bIntent in broadcastIntents) {
                try {
                    sendBroadcast(bIntent)
                    callStarted = true
                } catch (e: Exception) {
                    // Игнорируем
                }
            }
        }

        // МЕТОД 3 (ФИНАЛЬНЫЙ СПАСАТЕЛЬНЫЙ КРУГ): Открываем системный интерфейс Dialer без ACTION_CALL
        // Этот метод встроен в ядро Android и сработает ДАЖЕ на пустой магнитоле без SIM-карт
        if (!callStarted) {
            try {
                val systemDialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:$cleanNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(systemDialIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Bluetooth-модуль автомобиля заблокирован", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
            setMinUpdateIntervalMillis(1000L) // Обновляем строго раз в секунду
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateVehicleGpsData(location)
                }
            }
        }

        // Запрашиваем доступ, если он еще не выдан
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 202)
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun updateVehicleGpsData(location: android.location.Location) {
        val speedKmH = if (location.hasSpeed()) {
            (location.speed * 3.6f).toInt()
        } else 0

        MapRepository.currentSpeed = "$speedKmH км/ч"
        MapRepository.lastKnownLocation = Point(location.latitude, location.longitude)

        // ФИКС ВРАЩЕНИЯ: Чтобы капот всегда смотрел ВВЕРХ,
        // камера должна повернуться на угол, противоположный курсу автомобиля
        if (location.hasBearing()) {
            MapRepository.vehicleAzimuth = (360f - location.bearing) % 360f
        }
    }




    private fun handleDialIntent(intent: Intent?) {
        if (intent == null) return

        var number = ""

        // 1. Пробуем взять из EXTRA (стандартный способ для кнопок вызова)
        val extraNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        if (!extraNumber.isNullOrBlank()) {
            number = extraNumber
            Log.d("LexusCall", "Номер из EXTRA_PHONE_NUMBER: $number")
        }

        // 2. Если нет — пробуем из data (ACTION_DIAL / tel:)
        if (number.isBlank()) {
            val uri = intent.data
            if (uri != null && uri.scheme == "tel") {
                //number = uri.schemeSpecificPart()
                Log.d("LexusCall", "Номер из data (tel): $number")
            }
        }

        // 3. Если всё ещё пусто — открываем диалер пустым (пользователь сам наберёт)
        dialerNumber = number
        showDialer = true

        Log.d("LexusCall", "Итоговый номер для диалера: '${number.ifBlank { "(пусто)" }}'")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDialIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        LexusNaviManager.resume()  // возобновить отслеживание

    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        LexusNaviManager.suspend()  // приостановить отслеживание
        super.onStop()
    }

    override fun onDestroy() {
        try {
            // Освобождаем память и убираем утечку IntentReceiverLeaked
            unregisterReceiver(canReceiver)
            Log.d("LexusCAN", "Ресивер CAN-шины успешно отвязан (unregisterReceiver)")
        } catch (e: Exception) {
            // На случай, если ресивер не был зарегистрирован ранее, предотвращаем краш
            Log.e("LexusCAN", "Ошибка при отвязке ресивера: ${e.message}")
        }
        com.lexus.launcher.data.LexusCarManager.disconnect()

        StatusBarManager.destroy()
        super.onDestroy()
    }
}