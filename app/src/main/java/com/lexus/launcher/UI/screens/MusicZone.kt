package com.lexus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexus.launcher.ui.theme.*
import com.lexus.launcher.data.MediaRepository

@Composable
fun MusicZoneSkeleton() {
    val context = LocalContext.current // Берем контекст для интентов

    // Считываем живые стейты медиасессии
    val trackName = MediaRepository.trackName
    val artistName = MediaRepository.artistName
    val isPlaying = MediaRepository.isPlaying
    val progressPercent = MediaRepository.progressPercent
    val currentTime = MediaRepository.currentTime
    val totalTime = MediaRepository.totalTime
    val albumArtBitmap = MediaRepository.albumArt

    // --- АВТОМАТИЧЕСКИЙ ТРИГГЕР ПРОГРЕВА ---
    // Если музыка не запущена, корутина один раз принудительно будит Яндекс Музыку в фоне
    if (MediaRepository.activeController == null) {
        LaunchedEffect(Unit) {
            MediaRepository.warmUpYandexMusic(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(12.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Вывод шапки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ЯНДЕКС МУЗЫКА", color = LexusTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Box(modifier = Modifier.background(LexusSportRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(
                    text = if (MediaRepository.activeController != null) "Y+" else "CONNECT",
                    color = if (MediaRepository.activeController != null) LexusSportRed else LexusTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Обложка трека с Glow эффектом
        Box(modifier = Modifier.size(150.dp).padding(4.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(130.dp).blur(20.dp).background(Brush.radialGradient(colors = listOf(LexusSportRed.copy(alpha = 0.4f), Color.Transparent))))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LexusDeepBlack)
                    .border(1.dp, LexusCardStroke, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = albumArtBitmap.asImageBitmap(),
                        contentDescription = "Обложка",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // Анимационный или строгий текст ожидания запуска плеере
                    Text(
                        text = if (MediaRepository.activeController == null) "↻" else "♫",
                        color = LexusTextSecondary.copy(alpha = 0.3f),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin
                    )
                }
            }
        }

        // Название трека и артист
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.Start) {
            Text(
                text = if (MediaRepository.activeController == null) "Служба ожидания" else trackName,
                color = LexusTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (MediaRepository.activeController == null) "Запуск плеера в Lexus RC..." else artistName,
                color = LexusTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        // Живой таймлайн
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(LexusCardStroke)) {
                Box(modifier = Modifier.fillMaxWidth(progressPercent).fillMaxHeight().clip(CircleShape).background(LexusSportRed))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = currentTime, color = LexusTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(text = totalTime, color = LexusTextSecondary, fontSize = 11.sp)
            }
        }

        // Панель управления кнопками
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev", tint = LexusTextPrimary,
                modifier = Modifier.size(38.dp).clickable { MediaRepository.sendPrevious() }
            )

            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape).background(LexusTextPrimary).clickable {
                    if (MediaRepository.activeController == null) {
                        // Если контроллера нет, клик по центральной кнопке принудительно будит приложение повторенным методом
                        MediaRepository.hasAttemptedAutostart = false
                        MediaRepository.warmUpYandexMusic(context)
                    } else {
                        MediaRepository.sendPlayPause()
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "PlayPause", tint = LexusDeepBlack, modifier = Modifier.size(30.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = LexusTextPrimary,
                modifier = Modifier.size(38.dp).clickable { MediaRepository.sendNext() }
            )
        }
    }
}