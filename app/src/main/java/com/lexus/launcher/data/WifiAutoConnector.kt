package com.lexus.launcher.data

import android.content.Context
import android.net.wifi.WifiNetworkSuggestion
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

object WifiAutoConnector {

    private const val TAG = "LexusWifi"

    /**
     * "Предлагает" сети системе. Android сам подключится,
     * как только увидит их в эфире.
     * Сети сохраняются на уровне системы — переживают перезагрузку.
     */
    fun suggestNetworks(context: Context, networks: List<Pair<String, String>>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "WifiNetworkSuggestion доступен только с Android 10")
            return
        }

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val suggestions = networks.map { (ssid, password) ->
            val builder = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
            builder.build()
        }

        // Удаляем старые предложения, добавляем новые
        wifiManager.removeNetworkSuggestions(suggestions)

        val status = wifiManager.addNetworkSuggestions(suggestions)
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Log.d(TAG, "Сети добавлены: ${networks.map { it.first }}")
        } else {
            Log.e(TAG, "Ошибка добавления сетей: status=$status")
        }

        // Включаем Wi-Fi (если выключен)
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
            Log.d(TAG, "Wi-Fi включён")
        }
    }

    /**
     * Проверяет текущее состояние подключения.
     */
    fun getConnectionInfo(context: Context): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return if (wifiManager.isWifiEnabled && info?.ssid != null && info.ssid != "<unknown ssid>") {
            info.ssid.removeSurrounding("\"")
        } else {
            ""
        }
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }
}