package com.lexus.launcher.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yandex.mapkit.geometry.Point

object MapRepository {
    // Базовые стейты навигатора и спидометра
    var currentSpeed by mutableStateOf("0 км/ч")
    var lastKnownLocation by mutableStateOf<Point?>(null)
    var vehicleAzimuth by mutableStateOf(0.0f)

    // ЖИВЫЕ СТЕНТЫ БОРТОВОГО КОМПЬЮТЕРА (Извлекаются штатным CAN-драйвером магнитолы)
    var engineRpm by mutableStateOf(0)               // Обороты двигателя (Тахометр)
    var coolantTemp by mutableStateOf("--- °C")      // Температура антифриза
    var totalMileage by mutableStateOf("--- км")      // Общий пробег (Одометр)

    // Маршрутная информация Яндекса
    val nextManeuverDistance by mutableStateOf("350 м")
    val nextStreetName by mutableStateOf("ул. Ново-Садовая")
    val trafficScore by mutableStateOf(4)
}