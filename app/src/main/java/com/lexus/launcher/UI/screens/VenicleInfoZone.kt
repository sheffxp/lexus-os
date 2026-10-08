package com.lexus.launcher.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexus.launcher.data.VehicleRepository
import com.lexus.launcher.data.VehicleRepository.openRecentApps
import com.lexus.launcher.ui.theme.*

enum class RadarDistance { NONE, FAR, MEDIUM, DANGER }
enum class BsmStatus { CLEAR, WARNING }

data class LexusRadarState(
    val frontLeft: RadarDistance = RadarDistance.NONE,
    val frontCenter: RadarDistance = RadarDistance.NONE,
    val frontRight: RadarDistance = RadarDistance.NONE,
    val rearLeft: RadarDistance = RadarDistance.NONE,
    val rearCenter: RadarDistance = RadarDistance.NONE,
    val rearRight: RadarDistance = RadarDistance.NONE
)

data class LexusTirePressureState(
    val frontLeft: Float = 2.3f,
    val frontRight: Float = 2.3f,
    val rearLeft: Float = 2.2f,
    val rearRight: Float = 2.2f
)

data class LexusBsmState(
    val leftZone: BsmStatus = BsmStatus.WARNING,
    val rightZone: BsmStatus = BsmStatus.CLEAR
)

@Composable
fun VehicleInfoZoneSkeleton(onToggleZone: () -> Unit) {
    val radarState = VehicleRepository.radarState
    val tireState = VehicleRepository.tireState
    val bsmState = VehicleRepository.bsmState

    val isLeftDoorOpen = VehicleRepository.isLeftDoorOpen
    val isRightDoorOpen = VehicleRepository.isRightDoorOpen

    val engineOilTemp = VehicleRepository.engineOilTemp
    val coolantTemp = VehicleRepository.coolantTemp
    val turboOilTemp = VehicleRepository.turboOilTemp
    val voltageCAN = VehicleRepository.voltageCAN
    val fuelRangeCAN = VehicleRepository.fuelRangeCAN

    val lexusBsmOrange = Color(0xFFFF8F00)

    val infiniteTransition = rememberInfiniteTransition(label = "LexusVehicleAnim")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blink"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexusCardDark, RoundedCornerShape(12.dp))
            .border(1.dp, LexusCardStroke, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Шапка панели
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleZone() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("МОНИТОРИНГ БЕЗОПАСНОСТИ", color = LexusTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Команды", color = LexusSportRed, fontSize = 12.sp)
                Icon(Icons.Default.ChevronRight, "Go", tint = LexusSportRed, modifier = Modifier.size(16.dp))
            }
        }

        // БЛОК РАДАРA: Вытянут на всю оставшуюся высоту благодаря .weight(1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Растягивает радар, сдвигая прямоугольники вниз
                .background(LexusDeepBlack, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Силуэт кузова (Вытянули по высоте со 110dp до 140dp)
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 140.dp)
                    .background(LexusCardDark, RoundedCornerShape(12.dp))
                    .border(1.5.dp, LexusCardStroke, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEXUS", color = LexusTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("RC", color = LexusTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                // Двери (Скорректированы под вытянутый кузов)
                if (isLeftDoorOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.35f)
                            .width(3.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (-3).dp, y = (-18).dp)
                            .rotate(15f)
                            .alpha(blinkAlpha)
                            .background(LexusSportRed, CircleShape)
                    )
                }
                if (isRightDoorOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.35f)
                            .width(3.dp)
                            .align(Alignment.CenterEnd)
                            .offset(x = 3.dp, y = (-18).dp)
                            .rotate(-15f)
                            .alpha(blinkAlpha)
                            .background(LexusSportRed, CircleShape)
                    )
                }
            }

            // Модули BSM (смещены ниже в зону задних колес)
            Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 46.dp, top = 85.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.DirectionsCar, "ThreatL", tint = if (bsmState.leftZone == BsmStatus.WARNING) lexusBsmOrange else Color.Transparent, modifier = Modifier.size(20.dp).alpha(if (bsmState.leftZone == BsmStatus.WARNING) blinkAlpha else 0f))
                    LexusAdvancedBsmSignal(status = bsmState.leftZone, color = lexusBsmOrange, blinkAlpha = blinkAlpha)
                }
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 46.dp, top = 85.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LexusAdvancedBsmSignal(status = bsmState.rightZone, color = lexusBsmOrange, blinkAlpha = blinkAlpha)
                    Icon(Icons.Default.DirectionsCar, "ThreatR", tint = if (bsmState.rightZone == BsmStatus.WARNING) lexusBsmOrange else Color.Transparent, modifier = Modifier.size(20.dp).alpha(if (bsmState.rightZone == BsmStatus.WARNING) blinkAlpha else 0f))
                }
            }

            // Давление в шинах (разнесено по углам вытянутого кузова)
            TirePressureIndicator(pressure = tireState.frontLeft, blinkAlpha = blinkAlpha, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp, bottom = 65.dp))
            TirePressureIndicator(pressure = tireState.frontRight, blinkAlpha = blinkAlpha, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp, bottom = 65.dp))
            TirePressureIndicator(pressure = tireState.rearLeft, blinkAlpha = blinkAlpha, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp, top = 75.dp))
            TirePressureIndicator(pressure = tireState.rearRight, blinkAlpha = blinkAlpha, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp, top = 75.dp))

            // Передний радар (сверху вытянутого поля)
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    LexusRadarArc(distance = radarState.frontLeft, blinkAlpha = blinkAlpha, modifier = Modifier.rotate(-25f))
                    LexusRadarArc(distance = radarState.frontCenter, blinkAlpha = blinkAlpha)
                    LexusRadarArc(distance = radarState.frontRight, blinkAlpha = blinkAlpha, modifier = Modifier.rotate(25f))
                }
            }

            // Задний радар (снизу вытянутого поля)
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    LexusRadarArc(distance = radarState.rearLeft, blinkAlpha = blinkAlpha, modifier = Modifier.rotate(-155f))
                    LexusRadarArc(distance = radarState.rearCenter, blinkAlpha = blinkAlpha, modifier = Modifier.rotate(180f))
                    LexusRadarArc(distance = radarState.rearRight, blinkAlpha = blinkAlpha, modifier = Modifier.rotate(155f))
                }
            }
        }

        // --- ПРЯМОУГОЛЬНИКИ CAN ШИНЫ (СТРОГО В НИЖНЕЙ ЧАСТИ ЭКРАНА) ---

        // Ряд 1: ТЕМПЕРАТУРЫ ДВС И АНТИФРИЗА
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(modifier = Modifier.weight(1f).background(LexusDeepBlack, RoundedCornerShape(6.dp)).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("МАСЛО ДВС", color = LexusTextSecondary, fontSize = 9.sp)
                Text(engineOilTemp, color = LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).background(LexusDeepBlack, RoundedCornerShape(6.dp)).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("АНТИФРИЗ", color = LexusTextSecondary, fontSize = 9.sp)
                Text(coolantTemp, color = LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Ряд 2: ТУРБИНА И ДВЕРИ
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(modifier = Modifier.weight(1f).background(LexusDeepBlack, RoundedCornerShape(6.dp)).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("МАСЛО ТУРБИНЫ", color = LexusTextSecondary, fontSize = 9.sp)
                Text(turboOilTemp, color = LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            val anyDoorOpen = isLeftDoorOpen || isRightDoorOpen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(LexusDeepBlack, RoundedCornerShape(6.dp))
                    .border(width = if (anyDoorOpen) 1.dp else 0.dp, color = if (anyDoorOpen) LexusSportRed.copy(alpha = blinkAlpha) else Color.Transparent, shape = RoundedCornerShape(6.dp))
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ДВЕРИ", color = if (anyDoorOpen) LexusSportRed else LexusTextSecondary, fontSize = 9.sp)
                Text(if (anyDoorOpen) "ОТКРЫТА" else "ЗАКРЫТЫ", color = if (anyDoorOpen) LexusSportRed else LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Ряд 3: АКБ И ЗАПАС ХОДА
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(modifier = Modifier.weight(1f).background(LexusDeepBlack, RoundedCornerShape(6.dp)).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("АКБ", color = LexusTextSecondary, fontSize = 9.sp)
                Text(voltageCAN, color = LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).background(LexusDeepBlack, RoundedCornerShape(6.dp)).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ЗАПАС ХОДА", color = LexusTextSecondary, fontSize = 9.sp)
                Text(fuelRangeCAN, color = LexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }


    }

//    // --- ОТЛАДОЧНЫЙ СКАНЕР СЫРЫХ БАЙТ CAN-ШИНЫ ---
//    val rawCanLog = com.lexus.launcher.data.VehicleRepository.rawCanLog
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(38.dp) // Компактная высота, чтобы не ломать верстку
//            .background(Color.Black, RoundedCornerShape(4.dp))
//            .border(0.5.dp, LexusCardStroke, RoundedCornerShape(4.dp))
//            .padding(horizontal = 8.dp, vertical = 4.dp),
//        contentAlignment = Alignment.CenterStart
//    ) {
//        Text(
//            text = rawCanLog,
//            color = Color(0xFF00E676), // Зеленый "хакерский" цвет терминала для легкого чтения
//            fontSize = 10.sp,
//            fontWeight = FontWeight.Bold,
//            maxLines = 2,
//            lineHeight = 12.sp,
//            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
//        )
//    }
} // Конец основной функции VehicleInfoZoneSkeleton

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ГРАФИКИ НИЖЕ
// ==========================================

@Composable
fun LexusAdvancedBsmSignal(status: BsmStatus, color: Color, blinkAlpha: Float) {
    val isActive = status == BsmStatus.WARNING
    val finalAlpha = if (isActive) blinkAlpha else 1.0f
    Column(
        modifier = Modifier.height(32.dp).alpha(finalAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val lineColor = if (isActive) color else LexusCardStroke.copy(alpha = 0.15f)
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(0.4f).clip(CircleShape).background(lineColor))
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(0.6f).clip(CircleShape).background(lineColor))
    }
}

@Composable
fun TirePressureIndicator(pressure: Float, blinkAlpha: Float, modifier: Modifier = Modifier) {
    val isLow = pressure < 1.9f
    val textColor = if (isLow) LexusSportRed else LexusTextPrimary
    val labelColor = if (isLow) LexusSportRed else LexusTextSecondary
    val currentAlpha = if (isLow) blinkAlpha else 1.0f
    Column(modifier = modifier.alpha(currentAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "%.1f".format(pressure), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = "bar", color = labelColor, fontSize = 8.sp)
    }
}

@Composable
fun LexusRadarArc(distance: RadarDistance, blinkAlpha: Float, modifier: Modifier = Modifier) {
    val activeLines = when (distance) {
        RadarDistance.NONE -> 0
        RadarDistance.FAR -> 1
        RadarDistance.MEDIUM -> 2
        RadarDistance.DANGER -> 3
    }
    Column(modifier = modifier.width(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (level in 3 downTo 1) {
            val isActive = activeLines >= level
            val baseColor = when (distance) {
                RadarDistance.FAR -> LexusParkGreen
                RadarDistance.MEDIUM -> LexusParkYellow
                RadarDistance.DANGER -> LexusSportRed
                else -> Color.Transparent
            }
            val finalColor = if (isActive) baseColor else LexusCardStroke.copy(alpha = 0.15f)
            val currentAlpha = if (isActive && distance == RadarDistance.DANGER) blinkAlpha else 1.0f
            val lineThickness = if (isActive && distance == RadarDistance.DANGER) 2.0.dp else 1.2.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f + (level * 0.15f))
                    .height(lineThickness)
                    .alpha(currentAlpha)
                    .clip(CircleShape)
                    .background(finalColor)
            )
        }
    }
}

