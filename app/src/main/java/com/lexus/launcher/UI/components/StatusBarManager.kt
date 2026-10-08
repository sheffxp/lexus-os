package com.lexus.launcher.ui.components

import android.media.AudioManager
import android.provider.Settings
import android.database.ContentObserver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lexus.launcher.data.LexusLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object StatusBarManager {
    var state by mutableStateOf(StatusBarState(
        driveMode = "SPORT",
        isBluetoothConnected = false,          // true, если есть активное ACL-соединение
        isBluetoothEnabled = false,           // true, если BT вообще включён
        wifiSignalLevel = 0,
        cellularSignalBars = 0,
        musicVolume = 0,
        callVolume = 0
    ))
        private set

    data class StatusBarState(
        val driveMode: String,
        val isBluetoothConnected: Boolean,     // подключён к устройству (ACL)
        val isBluetoothEnabled: Boolean,       // BT включён
        val wifiSignalLevel: Int,
        val cellularSignalBars: Int,
        val musicVolume: Int = 0,          // 0–15 (или 0–max) для STREAM_MUSIC
        val callVolume: Int = 0            // 0–7 для STREAM_VOICE_CALL (обычно меньше макс.)
    )

    private var connMgr: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Для Bluetooth
    private var btReceiver: BroadcastReceiver? = null
    private var appContext: Context? = null

    // Audio
    private var audioManager: AudioManager? = null
    private var volumeObserver: ContentObserver? = null

    fun init(context: Context) {
        LexusLogger.writeLog("LexusStatusBar", "=== StatusBarManager.init() ===")
        appContext = context.applicationContext // держим слабый/безопасный контекст

        audioManager = appContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        if (audioManager != null) {
            // Читаем текущие уровни
            val musicMax = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val musicCur = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            val callMax = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val callCur = audioManager!!.getStreamVolume(AudioManager.STREAM_VOICE_CALL)

            state = state.copy(
                musicVolume = musicCur,
                callVolume = callCur
            )
            Log.d("LexusStatusBar", "[Audio] music=$musicCur/$musicMax, call=$callCur/$callMax")

            // Подписываемся на изменения громкости через ContentObserver
            val handler = android.os.Handler(context.mainLooper)
            volumeObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d("LexusStatusBar", "[Audio] onChange detected")
                    val newMusic = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    val newCall = audioManager?.getStreamVolume(AudioManager.STREAM_VOICE_CALL) ?: 0

                    // Обновляем только если значение реально изменилось
                    if (newMusic != state.musicVolume || newCall != state.callVolume) {
                        state = state.copy(
                            musicVolume = newMusic,
                            callVolume = newCall
                        )
                    }
                }
            }

            // Наблюдаем за настройками громкости
            val settingsUri = android.provider.Settings.System.CONTENT_URI
            appContext?.contentResolver?.registerContentObserver(
                settingsUri,
                true,
                volumeObserver!!
            )
            LexusLogger.writeLog("LexusStatusBar", "[Audio] ContentObserver зарегистрирован")
        }

        // --- Сеть ---
        connMgr = appContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                Log.d("LexusStatusBar", "[Network] onAvailable")
                updateStateFromNetwork()
            }
            override fun onLost(network: android.net.Network) {
                Log.d("LexusStatusBar", "[Network] onLost")
                updateStateFromNetwork()
            }
            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: NetworkCapabilities) {
                LexusLogger.writeLog("LexusStatusBar", "[Network] onCapabilitiesChanged")
                updateStateFromNetwork()
            }
        }

        try {
            connMgr?.registerDefaultNetworkCallback(networkCallback!!)
            LexusLogger.writeLog("LexusStatusBar", "[Network] registerDefaultNetworkCallback выполнен")
        } catch (e: Exception) {
            LexusLogger.writeLog("LexusStatusBar", "[Network] Ошибка подписки на сеть e")
        }

        updateStateFromNetwork()

        // --- Bluetooth: сначала проверяем, включён ли адаптер ---
        updateBluetoothEnabledState()

        // --- Bluetooth: слушаем подключения ---
        setupBluetoothReceiver()

        LexusLogger.writeLog("LexusStatusBar", "=== StatusBarManager.init() завершено ===")
    }

    private fun updateStateFromNetwork() {
        val caps = connMgr?.getNetworkCapabilities(connMgr?.activeNetwork)
        var newWifiLevel = 0
        var newCellBars = 0

        if (caps != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            newWifiLevel = if (hasWifi) 4 else 0
            newCellBars = if (hasCellular) 4 else 0
            LexusLogger.writeLog("LexusStatusBar", "[Network] hasWifi=$hasWifi, hasCellular=$hasCellular, wifiLevel=$newWifiLevel, cellBars=$newCellBars")
        } else {
            LexusLogger.writeLog("LexusStatusBar", "[Network] Нет activeNetwork или версия < M")
        }

        state = state.copy(wifiSignalLevel = newWifiLevel, cellularSignalBars = newCellBars)
    }

    /**
     * Проверяет, включён ли Bluetooth (без прослушивания подключений)
     */
    private fun updateBluetoothEnabledState() {
        try {
            val btAdapterClass = Class.forName("android.bluetooth.BluetoothAdapter")
            val getDefaultMethod = btAdapterClass.getDeclaredMethod("getDefaultAdapter")
            val isEnabledMethod = btAdapterClass.getMethod("isEnabled")

            val adapter = getDefaultMethod.invoke(null)
            if (adapter != null) {
                val isEnabled = isEnabledMethod.invoke(adapter) as Boolean
                state = state.copy(isBluetoothEnabled = isEnabled)
                LexusLogger.writeLog("LexusStatusBar", "[BT] isEnabled=$isEnabled")
            } else {
                LexusLogger.writeLog("LexusStatusBar", "[BT] BluetoothAdapter == null")
                state = state.copy(isBluetoothEnabled = false)
            }
        } catch (e: Exception) {
            LexusLogger.writeLog("LexusStatusBar", "[BT] Ошибка проверки isEnabled")
            state = state.copy(isBluetoothEnabled = false)
        }
    }

    /**
     * Настраивает BroadcastReceiver на события подключения BT
     */
    private fun setupBluetoothReceiver() {
        btReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val action = intent.action
                // ИСПРАВЛЕНИЕ: явно указываем тип <BluetoothDevice>
                val device = intent.getParcelableExtra<android.bluetooth.BluetoothDevice>(
                    android.bluetooth.BluetoothDevice.EXTRA_DEVICE
                )

                when (action) {
                    android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        LexusLogger.writeLog("LexusStatusBar", "[BT] ACTION_ACL_CONNECTED: device=$device")
                        state = state.copy(isBluetoothConnected = true)
                    }
                    android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        LexusLogger.writeLog("LexusStatusBar", "[BT] ACTION_ACL_DISCONNECTED: device=$device")
                        state = state.copy(isBluetoothConnected = false)
                    }
                    android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val stateExtra = intent.getIntExtra(
                            android.bluetooth.BluetoothAdapter.EXTRA_STATE,
                            android.bluetooth.BluetoothAdapter.STATE_OFF
                        )
                        val isEnabled = (stateExtra == android.bluetooth.BluetoothAdapter.STATE_ON)
                        LexusLogger.writeLog("LexusStatusBar", "[BT] ACTION_STATE_CHANGED: state=$stateExtra, isEnabled=$isEnabled")
                        state = state.copy(isBluetoothEnabled = isEnabled)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        }

        try {
            appContext?.registerReceiver(btReceiver, filter)
            LexusLogger.writeLog("LexusStatusBar", "[BT] Receiver зарегистрирован")
        } catch (e: Exception) {
            LexusLogger.writeLog("LexusStatusBar", "[BT] Не удалось зарегистрировать Receiver (возможно, нет разрешений) e")
        }
    }

    fun setDriveMode(mode: String) {
        LexusLogger.writeLog("LexusStatusBar", "[DriveMode] set to: $mode")
        state = state.copy(driveMode = mode)
    }

    fun destroy() {
        LexusLogger.writeLog("LexusStatusBar", "=== StatusBarManager.destroy() ===")

        // Отписка от громкости (универсальная)
        volumeObserver?.let {
            appContext?.contentResolver?.unregisterContentObserver(it)
            LexusLogger.writeLog("LexusStatusBar", "[Audio] ContentObserver отписан")
        }
        volumeObserver = null
        audioManager = null

        // Отписка от сети
        networkCallback?.let { callback ->
            connMgr?.unregisterNetworkCallback(callback)
            LexusLogger.writeLog("LexusStatusBar", "[Network] unregisterNetworkCallback выполнен")
        }
        networkCallback = null
        connMgr = null

        // Отписка от BT
        btReceiver?.let { receiver ->
            try {
                appContext?.unregisterReceiver(receiver)
                LexusLogger.writeLog("LexusStatusBar", "[BT] unregisterReceiver выполнен")
            } catch (e: Exception) {
                LexusLogger.writeLog("LexusStatusBar", "[BT] Ошибка unregisterReceiver")
            }
        }
        btReceiver = null
        appContext = null

        LexusLogger.writeLog("LexusStatusBar", "=== StatusBarManager.destroy() завершено ===")
    }
}

