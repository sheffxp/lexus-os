package com.lexus.launcher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lexus.launcher.data.LexusLogger
import com.lexus.launcher.ui.theme.*
import com.lexus.launcher.data.LexusNaviManager
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PointOfView
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.navigation.guidance_camera.CameraListener
import com.yandex.mapkit.navigation.guidance_camera.CameraMode
import com.yandex.mapkit.navigation.automotive.layer.NavigationLayerFactory
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.road_events.EventTag
import com.yandex.mapkit.styling.automotivenavigation.AutomotiveNavigationStyleProvider
import com.yandex.mapkit.styling.roadevents.RoadEventsLayerDefaultStyleProvider
import com.yandex.runtime.image.ImageProvider
import java.lang.ref.WeakReference



@Composable
fun MapZoneSkeleton() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapDebugStatus = LexusNaviManager.mapDebugStatus
    val currentSpeed = LexusNaviManager.currentSpeedText
    val nextManeuverDistance = LexusNaviManager.maneuverDistanceText
    val nextStreetName = LexusNaviManager.nextManeuverText
    val trafficScore = LexusNaviManager.trafficScoreValue

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var isCameraTrackingByNavi by remember { mutableStateOf(true) }
    var zoomOffset by remember { mutableStateOf(0f) }
    var needsReinit by remember { mutableStateOf(false) }
    var userLocationLayerRef by remember { mutableStateOf<com.yandex.mapkit.user_location.UserLocationLayer?>(null) }
    var cameraModeSet by remember { mutableStateOf(false) }

    // Сильная ссылка на слушатель
    val locationListener = remember {
        object : UserLocationObjectListener {
            override fun onObjectAdded(view: UserLocationView) {
                LexusLogger.writeLog("LexusMap", "=== onObjectAdded ВЫЗВАН! ===")

                // 1. Сдвигаем точку привязки камеры вниз экрана
                userLocationLayerRef?.let { layer ->
                    val w = mapViewRef?.width ?: 0
                    val h = mapViewRef?.height ?: 0

                    if (w > 0 && h > 0) {
                        layer.setAnchor(
                            PointF(w * 0.5f, h * 0.5f),
                            PointF(w * 0.5f, h * 0.83f)
                        )
                        LexusLogger.writeLog("LexusMap", "Anchor установлен: (${w * 0.5f}, ${h * 0.83f})")
                    }
                }

                // 2. Стиль для своей иконки (на arrow)
                val arrowStyle = IconStyle().apply {
                    flat = true
                    rotationType = RotationType.ROTATE
                    scale = 1.0f
                    anchor = PointF(0.5f, 0.9f)
                }

                //view.arrow.setIcon(imageProvider, arrowStyle)

                // 3. Скрываем стандартный pin
                val pinStyle = IconStyle().apply {
                    flat = true
                    rotationType = RotationType.NO_ROTATION
                    scale = 0.0f
                    anchor = PointF(0.5f, 0.5f)
                }


                // 4. Убираем круг точности
                view.accuracyCircle.fillColor = android.graphics.Color.TRANSPARENT
                view.accuracyCircle.strokeColor = android.graphics.Color.TRANSPARENT

                LexusLogger.writeLog("LexusMap", "arrow scale=${arrowStyle.scale}, pin scale=${pinStyle.scale}")
            }

            override fun onObjectRemoved(view: UserLocationView) {
                LexusLogger.writeLog("LexusMap", "onObjectRemoved")
            }

            override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}
        }
    }

    // CameraListener
    val cameraListener = remember {
        object : CameraListener {
            override fun onCameraModeChanged() {
                // В 4.42.0 cameraMode() — метод со скобками
                val mode = LexusNaviManager.navigationLayer?.camera?.cameraMode()
                LexusLogger.writeLog("LexusMap", "Camera mode changed: $mode")
                isCameraTrackingByNavi = (mode == CameraMode.FOLLOWING)
            }
        }
    }

    // Запрос разрешения
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LexusLogger.writeLog("LexusMap", "Разрешение ACCESS_FINE_LOCATION получено")
            LexusNaviManager.mapDebugStatus = "NaviKit: Поиск GPS..."
        } else {
            Log.e("LexusMap", "Разрешение геолокации отклонено")
            LexusNaviManager.mapDebugStatus = "Нет разрешения GPS"
        }
    }



    // Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    LexusLogger.writeLog("LexusMap", "Lifecycle ON_START")
                    MapKitFactory.getInstance().onStart()
                    mapViewRef?.onStart()
                    needsReinit = true
                    cameraModeSet = false
                    LexusNaviManager.resume()
                }
                Lifecycle.Event.ON_STOP -> {
                    LexusLogger.writeLog("LexusMap", "Lifecycle ON_STOP")
                    mapViewRef?.onStop()
                    MapKitFactory.getInstance().onStop()
                    // Сбрасываем нативную ссылку — объект уничтожён
                    LexusNaviManager.navigationLayer = null
                    LexusNaviManager.navigation = null          // <-- ДОБАВИТЬ
//                    LexusNaviManager.roadEventsLayer = null
                    LexusNaviManager.suspend()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(12.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                LexusLogger.writeLog("LexusMap", "AndroidView factory запущена")

                val mapView = MapView(ctx)

                // Проверка разрешения
                val hasPermission = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    LexusLogger.writeLog("LexusMap", "Запрос разрешения ACCESS_FINE_LOCATION...")
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                // Инициализация NaviKit
                if (LexusNaviManager.navigation == null) {
                    LexusNaviManager.init(ctx, mapView.mapWindow)
                }



                mapView.apply {
                    try {
                        val map = this.mapWindow.map
                        map.isNightModeEnabled = true
                        LexusLogger.writeLog("LexusMap", "Ночная тема применена")

                        // ============================================================
                        // NavigationLayer: 4 параметра для 4.42.0
                        // ============================================================
                        val navi = LexusNaviManager.navigation
                        if (navi != null) {
                            val existingLayer = LexusNaviManager.navigationLayer
                            if (existingLayer == null || !existingLayer.isValid) {
                                LexusLogger.writeLog("LexusMap", "Создаём новый NavigationLayer")

                                // 1. StyleProvider для дорожных событий (из roadevents:4.42.0)
                                val roadEventsStyleProvider =
                                    RoadEventsLayerDefaultStyleProvider(ctx)

                                // 2. NavigationStyleProvider (из automotivenavigation:4.42.0)
                                val navigationStyleProvider = AutomotiveNavigationStyleProvider(ctx)

                                // 3. Создаём слой: 4 параметра
                                LexusNaviManager.navigationLayer = NavigationLayerFactory.createNavigationLayer(
                                    this.mapWindow,                    // 1. MapWindow
                                    roadEventsStyleProvider,            // 2. StyleProvider (road events)
                                    navigationStyleProvider,            // 3. NavigationStyleProvider
                                    navi                                // 4. Navigation
                                )
                                LexusLogger.writeLog("LexusMap", "NavigationLayer создан")
                            } else {
                                LexusLogger.writeLog("LexusMap", "NavigationLayer уже существует и валиден")
                            }
                        }

                        // Настройка NavigationLayer
                        LexusNaviManager.navigationLayer?.let { layer ->
                            layer.isIsVisible = true
                            EventTag.values().forEach { tag ->
                                layer.setRoadEventVisibleOnRoute(tag, true)
                            }
                            // Камера
                            layer.camera.setCameraMode(CameraMode.FOLLOWING, Animation(Animation.Type.SMOOTH, 1f))
                            layer.camera.addListener(cameraListener)

//                            if (!cameraModeSet) {
//                                layer.camera.setCameraMode(
//                                    CameraMode.FOLLOWING,
//                                    Animation(Animation.Type.SMOOTH, 1f)
//                                )
//                                layer.camera.addListener(cameraListener)
//                                cameraModeSet = true
//                            }
                        }

//                        LexusNaviManager.navigationLayer?.let { layer ->
//                            layer.isIsVisible = true
//
//                            // === ВКЛЮЧАЕМ ВСЕ ДОРОЖНЫЕ СОБЫТИЯ ===
//                            // По умолчанию NONE visible — нужно явно включить каждый тег
//                            com.yandex.mapkit.road_events.EventTag.values().forEach { tag ->
//                                layer.setRoadEventVisibleOnRoute(tag, true)
//                            }
//                            LexusLogger.writeLog("LexusMap", "Включены все дорожные события: ${com.yandex.mapkit.road_events.EventTag.values().size} тегов")
//
//                            if (!cameraModeSet) {
//                                layer.camera.setCameraMode(
//                                    CameraMode.FOLLOWING,
//                                    Animation(Animation.Type.SMOOTH, 1f)
//                                )
//                                layer.camera.addListener(cameraListener)
//                                cameraModeSet = true
//                            }
//                        }

                        // Точка фокуса — нижняя треть экрана (стрелка будет тут)
                        val w = this.width.toFloat()
                        val h = this.height.toFloat()

                        if (w > 0 && h > 0) {
                            this.mapWindow.focusPoint = ScreenPoint(w / 2f, h * 0.75f)
                            LexusLogger.writeLog("LexusMap", "focusPoint установлен: (${w / 2f}, ${h * 0.75f})")
                        }

                        // Только HORIZONTALLY — другого значения нет
                        this.mapWindow.setPointOfView(PointOfView.ADAPT_TO_FOCUS_POINT_HORIZONTALLY)

                        // ============================================================
                        // UserLocationLayer
                        // ============================================================

                        // PointOfView: адаптация по обеим осям
                        this.mapWindow.setPointOfView(
                            PointOfView.ADAPT_TO_FOCUS_POINT_HORIZONTALLY
                        )


                        this.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                val w = this@apply.width.toFloat()
                                val h = this@apply.height.toFloat()
                                if (w > 0 && h > 0) {
                                    this@apply.mapWindow.focusPoint = ScreenPoint(w / 2f, h * 0.85f)
                                    this@apply.mapWindow.setPointOfView(PointOfView.ADAPT_TO_FOCUS_POINT_HORIZONTALLY)
                                    LexusLogger.writeLog("LexusMap", "focusPoint после layout: (${w / 2f}, ${h * 0.85f})")

                                    // Убираем слушатель, чтобы не вызывался повторно
                                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                }
                            }
                        })


                        mapViewRef = this
                        LexusNaviManager.mapDebugStatus = "NaviKit: Активен (ожидание GPS)"
                    } catch (e: Exception) {
                        Log.e("LexusMap", "Ошибка инициализации: ${e.message}", e)
                        LexusNaviManager.mapDebugStatus = "Ошибка: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (needsReinit) {
                    needsReinit = false
                    LexusLogger.writeLog("LexusMap", "update: пересоздание NavigationLayer")

                    try {
                        val navi = LexusNaviManager.navigation
                        if (navi != null) {
                            val existingLayer = LexusNaviManager.navigationLayer
                            if (existingLayer == null || !existingLayer.isValid) {
                                val roadEventsStyleProvider = RoadEventsLayerDefaultStyleProvider(view.context)
                                val navigationStyleProvider = AutomotiveNavigationStyleProvider(view.context)

                                LexusNaviManager.navigationLayer = NavigationLayerFactory
                                    .createNavigationLayer(
                                        view.mapWindow,
                                        roadEventsStyleProvider,
                                        navigationStyleProvider,
                                        navi
                                    )
                                LexusLogger.writeLog("LexusMap", "NavigationLayer пересоздан (update)")
                            }
                        }

                        LexusNaviManager.navigationLayer?.let { layer ->
                            layer.isIsVisible = true
                            layer.camera.setCameraMode(
                                CameraMode.FOLLOWING,
                                Animation(Animation.Type.SMOOTH, 1f)
                            )
                            layer.camera.addListener(cameraListener)
                        }

                        LexusNaviManager.navigationLayer?.let { layer ->
                            layer.isIsVisible = true

                            // Включаем события заново (слой пересоздан — настройки сбросились)
                            EventTag.values().forEach { tag ->
                                layer.setRoadEventVisibleOnRoute(tag, true)
                            }
                            LexusLogger.writeLog("LexusMap", "Дорожные события включены (update)")

                            layer.camera.setCameraMode(
                                CameraMode.FOLLOWING,
                                Animation(Animation.Type.SMOOTH, 1f)
                            )
                            layer.camera.addListener(cameraListener)
                        }

                        val w = view.width.toFloat()
                        val h = view.height.toFloat()
                        if (w > 0 && h > 0) {
                            view.mapWindow.focusPoint = ScreenPoint(w / 2f, h * 0.75f)
                            view.mapWindow.setPointOfView(
                                PointOfView.ADAPT_TO_FOCUS_POINT_HORIZONTALLY
                            )
                        }

                        LexusNaviManager.mapDebugStatus = "NaviKit: Восстановлен"
                    } catch (e: Exception) {
                        Log.e("LexusMap", "Ошибка восстановления: ${e.message}", e)
                    }
                }


            }
        )

        LaunchedEffect(mapViewRef) {
            if (mapViewRef != null) {
                LexusLogger.writeLog("LexusMap", "Ждём 3 сек перед авто-FOLLOWING...")
                kotlinx.coroutines.delay(1000)

                val layer = LexusNaviManager.navigationLayer
                if (layer != null && layer.isValid) {
                    layer.camera.setCameraMode(
                        CameraMode.FOLLOWING,
                        Animation(Animation.Type.SMOOTH, 1f)
                    )
                    LexusLogger.writeLog("LexusMap", "Авто-переход в FOLLOWING выполнен")
                } else {
                    Log.w("LexusMap", "NavigationLayer не готов для авто-FOLLOWING")
                }
            }
        }

        // --- HUD ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("HUD: $mapDebugStatus", color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        // --- Оверлей 1: Маневр ---
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(LexusCardDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(0.5.dp, LexusCardStroke, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Navigation, "Turn", tint = LexusSportRed, modifier = Modifier.size(24.dp).rotate(90f))
                Column {
                    Text(nextManeuverDistance, color = LexusTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(nextStreetName, color = LexusTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // --- Оверлей 2: Пробки ---
        val trafficColor = when {
            trafficScore <= 3 -> LexusParkGreen
            trafficScore <= 6 -> LexusParkYellow
            else -> LexusSportRed
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(LexusCardDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(0.5.dp, LexusCardStroke, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(14.dp).background(trafficColor, CircleShape))
            Text(
                if (trafficScore > 0) "$trafficScore балла" else "Пробки: Ок",
                color = LexusTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }

        // --- Оверлей 3: Спидометр / возврат слежения ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(
                    if (isCameraTrackingByNavi) LexusDeepBlack.copy(alpha = 0.8f)
                    else LexusSportRed.copy(alpha = 0.9f),
                    RoundedCornerShape(6.dp)
                )
                .border(
                    0.5.dp,
                    if (isCameraTrackingByNavi) Color.Transparent else LexusTextPrimary,
                    RoundedCornerShape(6.dp)
                )
                .clickable {
                    // setCameraMode принимает (CameraMode, Animation) в 4.42.0
                    LexusNaviManager.navigationLayer?.camera?.setCameraMode(
                        CameraMode.FOLLOWING,
                        Animation(Animation.Type.SMOOTH, 0.5f)
                    )
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isCameraTrackingByNavi) {
                    Icon(Icons.Default.Navigation, "Recenter", tint = LexusTextPrimary, modifier = Modifier.size(12.dp))
                }
                Text(
                    if (isCameraTrackingByNavi) currentSpeed else "ЦЕНТР",
                    color = LexusTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black
                )
            }
        }

        // --- Оверлей 4: Зум ---
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(LexusCardDark.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                    .clickable {
                        Log.w("LexusMap", "+++++++++++++++++++++++++++")
                        zoomOffset += 1f
                        LexusNaviManager.navigationLayer?.camera?.setFollowingModeZoomOffset(
                            zoomOffset,
                            Animation(Animation.Type.SMOOTH, 0.5f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, "+", tint = LexusTextPrimary, modifier = Modifier.size(20.dp)) }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(LexusCardDark.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                    .clickable {
                        Log.w("LexusMap", "----------------------------")
                        zoomOffset -= 1f
                        LexusNaviManager.navigationLayer?.camera?.setFollowingModeZoomOffset(
                            zoomOffset,
                            Animation(Animation.Type.SMOOTH, 0.3f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Remove, "-", tint = LexusTextPrimary, modifier = Modifier.size(20.dp)) }
        }
    }
}