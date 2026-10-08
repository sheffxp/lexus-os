package com.lexus.launcher.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.navigation.automotive.Navigation
import com.yandex.mapkit.navigation.automotive.NavigationFactory
import com.yandex.mapkit.navigation.automotive.layer.NavigationLayer
import com.yandex.mapkit.navigation.automotive.layer.NavigationLayerFactory
import com.yandex.mapkit.road_events.EventTag
import com.yandex.mapkit.styling.automotivenavigation.AutomotiveNavigationStyleProvider
import com.yandex.mapkit.styling.roadevents.RoadEventsLayerDefaultStyleProvider


object LexusNaviManager {

    var navigation: Navigation? = null
    var navigationLayer: NavigationLayer? = null

    var mapDebugStatus by mutableStateOf("NaviKit: Запуск...")
    var currentSpeedText by mutableStateOf("0 км/ч")
    var nextManeuverText by mutableStateOf("---")
    var maneuverDistanceText by mutableStateOf("---")
    var trafficScoreValue by mutableStateOf(0)

    /**
     * Создаёт ТОЛЬКО Navigation. Без слоя, без startGuidance.
     */
    fun init(context: Context, mapWindow: MapWindow) {
        if (navigation == null) {
            LexusLogger.writeLog("LexusMap", "LexusNaviManager: создание Navigation")
            navigation = NavigationFactory.createNavigation(DrivingRouterType.COMBINED)
        }

        try {
            val navi = navigation!!
            navi.resume()
            LexusLogger.writeLog("LexusMap", "NaviKit: resume() вызван")

            // StyleProvider'ы — используем ГОТОВЫЙ от Яндекса
            val navigationStyleProvider = AutomotiveNavigationStyleProvider(context)

            val roadEventsStyleProvider = RoadEventsLayerDefaultStyleProvider(context)

            // NavigationLayer
            navigationLayer = NavigationLayerFactory.createNavigationLayer(
                mapWindow,
                roadEventsStyleProvider,
                navigationStyleProvider,
                navi
            )
            LexusLogger.writeLog("LexusMap", "NavigationLayer создан")

            // Все типы камер и дорожных событий
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.SPEED_CONTROL, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.MOBILE_CONTROL, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.LANE_CONTROL, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.CROSS_ROAD_DANGER, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.OVERTAKING_DANGER, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.NO_STOPPING_CONTROL, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.ROAD_MARKING_CONTROL, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.ACCIDENT, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.RECONSTRUCTION, true)
            navigationLayer!!.setRoadEventVisibleOnRoute(EventTag.DANGER, true)
            LexusLogger.writeLog("LexusMap", "Дорожные события включены (камеры + ДТП + ремонт)")

            // Камера
            val camera = navigationLayer!!.camera
            camera.setAutoZoom(true, null)
            camera.setAutoRotation(true, null)

            mapDebugStatus = "NaviKit: Активен (ожидание GPS)"
        } catch (e: Exception) {
            Log.e("LexusMap", "Ошибка NaviKit: ${e.message}", e)
            mapDebugStatus = "Ошибка: ${e.message}"
        }
    }

    fun suspend() {
        navigation?.suspend()
    }

    fun resume() {
        navigation?.resume()
    }
}