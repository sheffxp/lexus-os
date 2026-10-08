package com.lexus.launcher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexus.launcher.ui.theme.*

// Модель данных для заглушки приложения
data class AppInfoPlaceholder(
    val name: String,
    val icon: ImageVector,
    val iconColor: Color = LexusTextPrimary,
    val packageName: String // Поле для системного имени пакета
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsListScreen(
    onBackToMain: () -> Unit,
    onLaunchApp: (String) -> Unit) {
    // Список приложений (Имитируем установленный софт на магнитоле)
    val appList = listOf(
        AppInfoPlaceholder("Яндекс Навигатор", Icons.Default.Map, Color(0xFFFFD600),"ru.yandex.yandexmaps"),
        AppInfoPlaceholder("Яндекс Музыка", Icons.Default.MusicNote, LexusSportRed, "ru.yandex.music"),
        AppInfoPlaceholder("Настройки CAN", Icons.Default.DirectionsCar, LexusTextSecondary, "com.android.settings"),
        AppInfoPlaceholder("YouTube", Icons.Default.VideoLibrary, Color(0xFFFF0000), "com.google.android.youtube"),
        AppInfoPlaceholder("Google Chrome", Icons.Default.Web, Color(0xFF2979FF), "com.android.chrome"),
        AppInfoPlaceholder("Настройки звука", Icons.Default.Settings, LexusTextPrimary, "com.android.settings"),
        AppInfoPlaceholder("Радио AM/FM", Icons.Default.MusicNote, LexusTextSecondary, "com.android.settings"),
        AppInfoPlaceholder("Файлы", Icons.Default.Web, LexusTextPrimary, "com.android.settings"),
        AppInfoPlaceholder("Видео", Icons.Default.VideoLibrary, LexusTextPrimary, "com.android.settings"),
        AppInfoPlaceholder("Bluetooth Аудио", Icons.Default.SettingsBluetooth, Color(0xFF2979FF), "com.android.settings"),
        // Вторая страница для демонстрации свайпа
        AppInfoPlaceholder("Калькулятор", Icons.Default.Settings, LexusTextSecondary, "com.android.settings"),
        AppInfoPlaceholder("Галерея", Icons.Default.Web, LexusTextSecondary, "com.android.settings")
    )

    // Делим список на страницы по 10 приложений (2 ряда по 5 штук)
    val itemsPerPage = 10
    val chunkedApps = appList.chunked(itemsPerPage)
    val pagerState = rememberPagerState(pageCount = { chunkedApps.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusDeepBlack)
            .padding(16.dp)
    ) {
        // 1. ВЕРХНИЙ БАР ЭКРАНА ПРИЛОЖЕНИЙ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBackToMain() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = LexusTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ГЛАВНЫЙ ЭКРАН",
                    color = LexusTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "ВСЕ ПРИЛОЖЕНИЯ",
                color = LexusTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. СЕТКА ПРИЛОЖЕНИЙ С ГОРИЗОНТАЛЬНЫМ ПЕЙДЖЕРОМ
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            val pageApps = chunkedApps[pageIndex]

            // Заполняем сетку 2 ряда на 5 колонок вручную для идеального контроля размеров на 1920x720
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rows = pageApps.chunked(5) // Максимум 5 элементов в строке

                for (rowApps in rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Равное распределение высоты между двумя рядами
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (app in rowApps) {
                            AppGridTile(
                                app = app,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onLaunchApp(app.packageName) }
                            )
                        }

                        // Если в последнем ряду меньше 5 приложений, заполняем пустоту инвиз-блоками
                        if (rowApps.size < 5) {
                            val emptySpaces = 5 - rowApps.size
                            for (i in 0 until emptySpaces) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Если в странице всего 1 ряд, заполняем второй ряд пустым пространством
                if (rows.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 3. ИНДИКАТОР СТРАНИЦ (Точки снизу)
        if (chunkedApps.size > 1) {
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(chunkedApps.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) LexusSportRed else LexusCardStroke
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }
}

// Плитка одного приложения в сетке
@Composable
fun AppGridTile(app: AppInfoPlaceholder, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(10.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Подложка для иконки
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(LexusDeepBlack, CircleShape)
                .border(0.5.dp, LexusCardStroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = app.icon,
                contentDescription = app.name,
                tint = app.iconColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = app.name,
            color = LexusTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}