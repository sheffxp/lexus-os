package com.lexus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.lexus.launcher.ui.theme.*
import com.lexus.launcher.data.LexusUpdateManager

@Composable
fun UpdateZoneSkeleton(
    onNavigateBack: () -> Unit, // ИСПРАВЛЕНО: Добавлен колбэк для возврата обратно в меню
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentVersion = LexusUpdateManager.currentVersionName
    val serverVersion = LexusUpdateManager.serverVersionName
    val changelogText = LexusUpdateManager.changelogText
    val statusMessage = LexusUpdateManager.updateStatusMessage
    val isNewAvailable = LexusUpdateManager.isNewVersionAvailable
    val isDownloading = LexusUpdateManager.isDownloading
    val progress = LexusUpdateManager.downloadProgress

    LaunchedEffect(Unit) {
        LexusUpdateManager.checkServerForUpdates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(12.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // 1. ШАПКА С КНОПКОЙ НАЗАД
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Кнопка возврата в меню
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(LexusDeepBlack)
                    .border(0.5.dp, LexusCardStroke, CircleShape)
                    .clickable { onNavigateBack() }, // Клик возвращает в предыдущую зону
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = LexusTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "СЕРВЕР ОБНОВЛЕНИЙ LEXUS OS (OTA)",
                color = LexusTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 2. ДВУХКАНАЛЬНЫЙ МОНИТОР ВЕРСИЙ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(LexusDeepBlack, RoundedCornerShape(8.dp))
                    .border(0.5.dp, LexusCardStroke, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("ТЕКУЩАЯ ПРОШИВКА", color = LexusTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(currentVersion, color = LexusTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(LexusDeepBlack, RoundedCornerShape(8.dp))
                    .border(0.5.dp, if (isNewAvailable) LexusSportRed else LexusCardStroke, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("АКТУАЛЬНАЯ НА СЕРВЕРЕ", color = LexusTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(serverVersion, color = if (isNewAvailable) LexusSportRed else LexusTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        // 3. СПИСОК ИЗМЕНЕНИЙ (CHANGELOG)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp)
                .background(LexusDeepBlack.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .border(0.5.dp, LexusCardStroke, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text("СПИСОК ИЗМЕНЕНИЙ СБОРКИ:", color = LexusTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = changelogText, color = LexusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
        }

        // 4. ИНДИКАТОР И КНОПКА СКАЧИВАНИЯ
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = statusMessage, color = if (isNewAvailable) LexusSportRed else LexusTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = LexusSportRed,
                    trackColor = LexusCardStroke
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNewAvailable) LexusSportRed else LexusDeepBlack)
                        .border(0.5.dp, if (isNewAvailable) Color.Transparent else LexusCardStroke, RoundedCornerShape(6.dp))
                        .clickable {
                            if (isNewAvailable) {
                                LexusUpdateManager.downloadAndInstallApk(context)
                            } else {
                                LexusUpdateManager.checkServerForUpdates()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isNewAvailable) "УСТАНОВИТЬ С СЕРВЕРА (OTA)" else "ПРОВЕРИТЬ ОБНОВЛЕНИЯ",
                        color = LexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
