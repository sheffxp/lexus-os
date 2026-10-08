package com.lexus.launcher.ui.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexus.launcher.data.LexusUpdateManager
import com.lexus.launcher.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun LexusStatusBar(modifier: Modifier = Modifier) {
    val s = StatusBarManager.state // Читаем состояние из менеджера

    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var isColonVisible by remember { mutableStateOf(true) }

    // Часы (только UI-логика)
    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = LocalTime.now()
            hours = currentTime.format(DateTimeFormatter.ofPattern("HH"))
            minutes = currentTime.format(DateTimeFormatter.ofPattern("mm"))
            isColonVisible = !isColonVisible
            delay(1000L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- ЛЕВАЯ: LEXUS + РЕЖИМ ---
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "LEXUS",
                color = LexusTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            val modeColor = when (s.driveMode) {
                "SPORT" -> LexusSportRed
                "ECO" -> LexusParkGreen
                else -> LexusTextSecondary
            }
            Box(
                modifier = Modifier
                    .background(modeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, modeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = s.driveMode,
                    color = modeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- ЦЕНТР: ЧАСЫ ---
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = hours, color = LexusTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = ":",
                color = if (isColonVisible) LexusTextPrimary else LexusTextSecondary.copy(alpha = 0.4f),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Text(text = minutes, color = LexusTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        // --- ПРАВАЯ: ИНДИКАТОРЫ ---
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // === ИНДИКАТОР ГРОМКОСТИ ===

            val s = StatusBarManager.state

            // Выбираем поток: если идёт звонок — громкость разговора, иначе музыка
            val volume = if (s.isBluetoothConnected && s.callVolume > 0) {
                s.callVolume
            } else {
                s.musicVolume
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = volume.toString(),
                    color = Color(0xFFFF0000),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }



            Icon(
                imageVector = if (s.isBluetoothConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = "BT",
                tint = if (s.isBluetoothConnected) Color(0xFF2979FF) else LexusTextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "WiFi",
                tint = if (s.wifiSignalLevel > 0) LexusTextPrimary else LexusCardStroke.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(14.dp)) {
                for (bar in 1..5) {
                    val isBarActive = s.cellularSignalBars >= bar
                    val barColor = if (isBarActive) LexusTextPrimary else LexusCardStroke.copy(alpha = 0.4f)

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight(bar * 0.2f)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (s.cellularSignalBars > 0) "4G" else "",
                    color = LexusTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
@Composable
fun SystemUpdateButton(
    onNavigateToUpdateZone: () -> Unit, // Лямбда-колбэк для переключения экрана
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(44.dp) // Комфортный автомобильный размер под тапы на ходу в Lexus RC
            .clip(RoundedCornerShape(8.dp))
            .background(LexusCardDark)
            .border(1.dp, LexusCardStroke, RoundedCornerShape(8.dp))
            .clickable {
                // 1. Сразу инициируем фоновую проверку новой версии на сервере
                LexusUpdateManager.checkServerForUpdates()
                // 2. Переключаем интерфейс на экран обновлений
                onNavigateToUpdateZone()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudSync, // Иконка синхронизации с облаком
            contentDescription = "Обновление системы",
            tint = LexusTextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
