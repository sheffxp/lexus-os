package com.lexus.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.lexus.launcher.ui.theme.*
import com.lexus.launcher.data.TaskManager

@Composable
fun TaskOverlayZone(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apps = TaskManager.runningApps

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(12.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. ШАПКА ОКНА ЗАДАЧ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "АКТИВНЫЕ ЗАДАЧИ (${apps.size})",
                color = LexusTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Кнопка закрытия оверлея
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { TaskManager.isTaskOverlayVisible = false },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = LexusTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 2. ЦЕНТРАЛЬНЫЙ КОНТЕНТ: СЕРТИФИЦИРОВАННЫЙ СПИСОК ПЛИТОК
        if (apps.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Память Lexus RC оптимизирована",
                    color = LexusTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Вертикальный список широких автомобильных плиток (Удобно скроллить на экране)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LexusDeepBlack)
                            .border(0.5.dp, LexusCardStroke, RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // КЛИКАБЕЛЬНАЯ ЗОНА: Для разворачивания приложения (Слева)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    launchIntent?.let { context.startActivity(it) }
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Круглая премиальная подложка под иконку
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(LexusCardDark, CircleShape)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = app.appName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Название запущенной программы
                            Text(
                                text = app.appName.uppercase(),
                                color = LexusTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // ИЗОЛИРОВАННАЯ КНОПКА ЗАКРЫТИЯ ПРОЦЕССА (Справа)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(46.dp)
                                .clickable { TaskManager.closeTask(context, app.packageName) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Вертикальный разделитель перед крестиком
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.4f)
                                    .width(0.5.dp)
                                    .background(LexusCardStroke)
                                    .align(Alignment.CenterStart)
                            )

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Убить процесс",
                                tint = LexusSportRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. НИЖНЯЯ КНОПКА "ОЧИСТИТЬ ВСЁ" БЕЗ ИЗМЕНЕНИЙ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (apps.isEmpty()) LexusDeepBlack else LexusSportRed)
                .clickable(enabled = apps.isNotEmpty()) { TaskManager.clearAllTasks(context) },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, null, tint = LexusTextPrimary, modifier = Modifier.size(16.dp))
                Text("ОЧИСТИТЬ ПАМЯТЬ ГУ", color = LexusTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}