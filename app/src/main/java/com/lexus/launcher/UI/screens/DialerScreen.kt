package com.lexus.launcher.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexus.launcher.ui.theme.*





// Модели данных
data class QuickContactInfo(val name: String, val number: String)
data class RecentCallInfo(val name: String, val number: String, val time: String, val isMissed: Boolean, val isIncoming: Boolean)

@Composable
fun DialerScreen(
    initialContactName: String = "Жена",
    onBackToMain: () -> Unit,
    onPerformCall: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var currentContactName by remember { mutableStateOf(initialContactName) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    // 6 Избранных контактов
    val quickContacts = listOf(
        QuickContactInfo("Жена", "+79991112233"),
        QuickContactInfo("Дом", "+79992223344"),
        QuickContactInfo("Работа", "+79993334455"),
        QuickContactInfo("Мама", "+79994445566"),
        QuickContactInfo("Папа", "+79995556677"),
        QuickContactInfo("Сервис", "+79997778899")
    )

    // Список последних вызовов (3 штуки для идеальной гармонии)
    val recentCalls = listOf(
        RecentCallInfo("Брат", "+79996667788", "14:20", isMissed = false, isIncoming = true),
        RecentCallInfo("Жена", "+79991112233", "12:05", isMissed = false, isIncoming = false),
        RecentCallInfo("Неизвестный", "+79110001122", "Вчера", isMissed = true, isIncoming = true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusDeepBlack)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. ВЕРХНИЙ БАР
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBackToMain() }
            ) {
                Icon(Icons.Default.ArrowBack, "Назад", tint = LexusTextPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("НАЗАД В ЛАУНЧЕР", color = LexusTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. РАБОЧАЯ ЗОНА 1920x720
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- КОЛОНКА 1: ИЗБРАННОЕ (6 КНОПОК) + ПОСЛЕДНИЕ ВЫЗОВЫ ---
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Сетка быстрых контактов (3 ряда по 2 кнопки)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pairs = quickContacts.chunked(2)
                    for (rowContacts in pairs) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (contact in rowContacts) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(LexusCardDark, RoundedCornerShape(6.dp))
                                        .border(1.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                                        .clickable {
                                            phoneNumber = contact.number
                                            currentContactName = contact.name
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(contact.name, color = LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Заголовок списка последних звонков
                Text(
                    text = "ПОСЛЕДНИЕ ВЫЗОВЫ",
                    color = LexusTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Список последних вызовов (Ровно 3 строки)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (call in recentCalls) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(LexusDeepBlack, RoundedCornerShape(6.dp))
                                .border(1.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                                .clickable {
                                    phoneNumber = call.number
                                    currentContactName = call.name
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Иконка направления звонка (Зеленая / Синяя / Красная если пропущен)
                                val iconColor = when {
                                    call.isMissed -> LexusSportRed
                                    call.isIncoming -> LexusParkGreen
                                    else -> Color(0xFF2979FF)
                                }
                                Icon(
                                    imageVector = if (call.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                                    contentDescription = "Тип вызова",
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Имя контакта
                                Text(
                                    text = call.name,
                                    color = if (call.isMissed) LexusSportRed else LexusTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Время звонка
                            Text(text = call.time, color = LexusTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // --- КОЛОНКА 2: ЦЕНТРАЛЬНЫЙ БЛОК СТАТУСА ТЕКУЩЕГО ВЫЗОВА ---
            Column(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .background(LexusCardDark, RoundedCornerShape(8.dp))
                    .border(1.dp, LexusCardStroke, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(LexusDeepBlack, CircleShape)
                        .border(1.dp, LexusCardStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Аватар", tint = LexusTextSecondary, modifier = Modifier.size(36.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (phoneNumber.isEmpty()) currentContactName else phoneNumber,
                        color = LexusTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = if (phoneNumber.isEmpty()) "Быстрый набор" else "Ввод номера", color = LexusTextSecondary, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSpeakerOn) LexusTextPrimary else LexusDeepBlack)
                            .border(1.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                            .clickable { isSpeakerOn = !isSpeakerOn },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VolumeUp, "Speaker", tint = if (isSpeakerOn) LexusDeepBlack else LexusTextPrimary, modifier = Modifier.size(22.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LexusSportRed)
                            .clickable {
                                phoneNumber = ""
                                currentContactName = initialContactName
                                onBackToMain()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CallEnd, "End", tint = LexusTextPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // --- КОЛОНКА 3: СТРОГАЯ КВАДРАТНАЯ NUMPAD КЛАВИАТУРА ---
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val numpadButtons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )

                for (row in numpadButtons) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (digit in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(LexusCardDark, RoundedCornerShape(6.dp))
                                    .border(1.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                                    .clickable { phoneNumber += digit },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(digit, color = LexusTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(LexusCardDark, RoundedCornerShape(6.dp))
                            .border(1.dp, LexusCardStroke, RoundedCornerShape(6.dp))
                            .clickable { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Backspace, "Clear", tint = LexusTextSecondary, modifier = Modifier.size(22.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .background(LexusParkGreen, RoundedCornerShape(6.dp))
                            .clickable {
                                // Если строка ввода пустая — набираем дефолтный номер (например, выбранный контакт)
                                val numberToCall = if (phoneNumber.isEmpty()) "+79991112233" else phoneNumber
                                //onPerformCall(numberToCall)
                                // Вставьте реальный номер телефона вместо тестового!
                                //val phoneNumber = "+79991234567"
                                Log.d("LexusPhone", "Инициализация Bluetooth-вызова на номер:  $numberToCall")
                                // Вызываем наш автомобильный менеджер звонков
                                com.lexus.launcher.data.LexusBtPhoneManager.makeBluetoothCall(context, numberToCall)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Call, "Call", tint = LexusDeepBlack, modifier = Modifier.size(20.dp))
                            Text("ВЫЗОВ", color = LexusDeepBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}