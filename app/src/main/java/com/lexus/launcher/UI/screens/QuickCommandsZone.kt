package com.lexus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexus.launcher.ui.theme.*
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.draw.clip
import com.lexus.launcher.data.LexusUpdateManager
import com.lexus.launcher.data.VehicleRepository.openRecentApps


@Composable
fun QuickCommandsZoneSkeleton(
    onToggleZone: () -> Unit,
    onOpenAppsClick: () -> Unit,
    onOpenDialerClick: () -> Unit,
    onLaunchAppClick: (String) -> Unit,
    onNavigateToUpdate: () -> Unit
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(12.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. ЗАГОЛОВОК ПАНЕЛИ
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleZone() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("НАСТРОЙКИ АВТОМОБИЛЯ", color = LexusTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Мониторинг", color = LexusSportRed, fontSize = 12.sp)
                Icon(Icons.Default.ChevronRight, "Go", tint = LexusSportRed, modifier = Modifier.size(16.dp))
            }
        }

        // 2. СЕТКА ЖЕСТКИХ КНОПОК ПРИЛОЖЕНИЙ
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommandTile(
                    icon = Icons.Default.Phone,
                    label = "Вызов",
                    modifier = Modifier.weight(1f).clickable { onOpenDialerClick() }
                )





                CommandTile(
                    icon = Icons.Default.Home,
                    label = "Маршрут: Домой",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            try {
                                android.util.Log.d("LexusNavi", "Силовой сквозной запуск ведения по маршруту...")

                                // Укажите координаты вашего дома (Широта и Долгота)
                                // Замените 55.7558 и 37.6173 на ваши точные данные!
                                //46.353189, 47.998071
                                val latTo = "46.353189"
                                val lonTo = "47.998071"

                                // Формируем URI Яндекса с флагами build_route_on_map=1 и guidance=1
                                // Это переводит Яндекс Карты в режим мгновенного старта ведения стрелки
                                val uriString = "yandexmaps://maps.yandex.ru/?" +
                                        "rtext=~" + latTo + "," + lonTo +
                                        "&rtt=auto" +
                                        "&build_route_on_map=1" +
                                        "&guidance=1"

                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString)).apply {
                                    // Жестко блокируем браузеры и Дзен, открывая строго внутри Яндекс Карт
                                    `package` = "ru.yandex.yandexmaps"
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }

                                context.startActivity(intent)
                                android.util.Log.d("LexusNavi", "Интент прямого ведения отправлен в Яндекс Карты.")

                            } catch (e: Exception) {
                                android.util.Log.w("LexusNavi", "Яндекс Карты отклонили команду, пробуем через Навигатор...")
                                try {
                                    // Резервный вариант для классического Яндекс Навигатора
                                    val latTo = "55.7558"
                                    val lonTo = "37.6173"
                                    val uriString = "yandexnavi://build_route_on_map" +
                                            "?lat_to=" + latTo +
                                            "&lon_to=" + lonTo +
                                            "&auto_start=1" // Навигатор по этому флагу стартует сам без кнопок

                                    val naviIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString)).apply {
                                        `package` = "ru.yandex.yandexnavi"
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(naviIntent)
                                } catch (ex: Exception) {
                                    android.util.Log.e("LexusNavi", "Все автостарты заблокированы прошивкой ГУ: " + ex.localizedMessage)
                                    // Аварийный запуск приложения с нуля
                                    val backup = context.packageManager.getLaunchIntentForPackage("ru.yandex.yandexmaps")
                                    backup?.let { context.startActivity(it) }
                                }
                            }
                        }
                )
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommandTile(
                    icon = Icons.Default.Settings,
                    label = "Настройки",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLaunchAppClick("com.android.settings") }
                )

                CommandTile(
                    icon = Icons.Default.GridOn,
                    label = "Все приложения",
                    modifier = Modifier.weight(1f).clickable { onOpenAppsClick() }
                )
            }
        }

        // 3. ИСПРАВЛЕНО: НИЖНИЙ ГОРЫЗОНТАЛЬНЫЙ РЯД С ОПРЕДЕЛЕНИЕМ СТРОГОГО ВЕСА
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // КОМПОНЕНТ КНОПКИ ОТКРЫТЫХ ПРИЛОЖЕНИЙ (ЗАДАЧИ) — СЛЕВА
            RecentAppsButton(modifier = Modifier.weight(1f))

            // КНОПКА ОБНОВЛЕНИЯ С СЕРВЕРА (OTA) — СПРАВА ОТ НЕЕ
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LexusDeepBlack)
                    .border(1.dp, LexusCardStroke, RoundedCornerShape(8.dp))
                    .clickable {
                        LexusUpdateManager.checkServerForUpdates()
                        onNavigateToUpdate()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Обновление",
                        tint = LexusSportRed, // Спортивный красный акцент F-Sport
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ОБНОВЛЕНИЕ",
                        color = LexusTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CommandTile(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LexusDeepBlack, RoundedCornerShape(8.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = LexusSportRed, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = LexusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RecentAppsButton(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = modifier
            .height(44.dp) // Выровняли высоту в один шаг с кнопкой обновления
            .clip(RoundedCornerShape(8.dp))
            .background(LexusDeepBlack)
            .border(1.dp, LexusCardStroke, RoundedCornerShape(8.dp))
            .clickable {
                openRecentApps(context)
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Открытые приложения",
                tint = LexusTextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "ЗАДАЧИ",
                color = LexusTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}