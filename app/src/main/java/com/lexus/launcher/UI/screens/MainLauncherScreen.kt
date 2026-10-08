package com.lexus.launcher.ui.screens


import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.unit.dp
import com.lexus.launcher.ui.components.LexusStatusBar
import com.lexus.launcher.ui.components.TaskOverlayZone
import com.lexus.launcher.ui.theme.LexusDeepBlack
import com.lexus.launcher.ui.theme.LexusCardDark

enum class ActiveRightZone { VEHICLE_INFO, QUICK_COMMANDS, UPDATE_CENTER}

@Composable
fun MainLauncherScreen(
    onOpenAllApps: () -> Unit,
    onOpenDialer: () -> Unit,
    onLaunchApp: (String) -> Unit
) {



    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    backDispatcher?.addCallback(remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Вместо выхода — сворачиваем в Dock или показываем подсказку
                // Либо вообще ничего не делаем, чтобы Back не работал
            }
        }
    })


    var activeZone by remember { mutableStateOf(ActiveRightZone.VEHICLE_INFO) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusDeepBlack)
    ) {
        LexusStatusBar(modifier = Modifier.fillMaxWidth().height(60.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ЗОНА 1: Карта
            Box(modifier = Modifier.fillMaxHeight().weight(0.45f)) {
                MapZoneSkeleton()
            }

            // ЗОНА 2: Яндекс Музыка
            Box(modifier = Modifier.fillMaxHeight().weight(0.30f)) {
                MusicZoneSkeleton()
            }

            // ЗОНА 3 / 4: Инфо / Команды
            Surface(
                modifier = Modifier.fillMaxHeight().weight(0.25f),
                shape = RoundedCornerShape(12.dp),
                color = LexusCardDark
            ) {

                // Если водитель нажал "ЗАДАЧИ", правая панель перекрывается нашим кастомным диспетчером задач
                if (com.lexus.launcher.data.TaskManager.isTaskOverlayVisible) {
                    TaskOverlayZone()
                } else {
                    // Ваша стандартная навигация
                    when (activeZone) {
                        ActiveRightZone.VEHICLE_INFO -> VehicleInfoZoneSkeleton(
                            onToggleZone = { activeZone = ActiveRightZone.QUICK_COMMANDS }
                        )

                        ActiveRightZone.QUICK_COMMANDS -> QuickCommandsZoneSkeleton(
                            onToggleZone = { activeZone = ActiveRightZone.VEHICLE_INFO },
                            onOpenAppsClick = onOpenAllApps,
                            onOpenDialerClick = onOpenDialer,
                            onLaunchAppClick = onLaunchApp,
                            onNavigateToUpdate = { activeZone = ActiveRightZone.UPDATE_CENTER }
                        )

                        ActiveRightZone.UPDATE_CENTER -> UpdateZoneSkeleton(
                            // ДОБАВЛЕНО: При нажатии кнопки «Назад» возвращаем водителя в быстрые команды
                            onNavigateBack = { activeZone = ActiveRightZone.QUICK_COMMANDS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrightnessControls(
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    current: Float
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDecrease) {
            Icon(Icons.Default.Remove, contentDescription = "Уменьшить яркость")
        }
        Text(
            text = "${(current * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        IconButton(onClick = onIncrease) {
            Icon(Icons.Default.Add, contentDescription = "Увеличить яркость")
        }
    }
}