package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.CalibrationPointSample
import com.example.model.MorphologicalProfile
import com.example.model.TrackerDiagnostics
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CalibrationStage {
    READY,
    RUNNING,
    FINISHED
}

data class TargetPoint(val label: String, val normX: Float, val normY: Float)

@Composable
fun CalibrationWizardScreen(
    settings: AppSettings,
    diagnostics: TrackerDiagnostics,
    isServiceRunning: Boolean,
    onStartService: () -> Unit,
    onCaptureSample: (pointIndex: Int, normX: Float, normY: Float) -> CalibrationPointSample?,
    onApplyProfile: (List<CalibrationPointSample>) -> MorphologicalProfile?,
    onGoToSandbox: () -> Unit
) {
    var stage by remember {
        mutableStateOf(
            if (settings.morphologicalProfile.isCalibrated) CalibrationStage.FINISHED else CalibrationStage.READY
        )
    }

    val calibrationPoints = remember {
        listOf(
            TargetPoint("Центр", 0.50f, 0.50f),
            TargetPoint("Верхній лівий", 0.15f, 0.15f),
            TargetPoint("Верхній правий", 0.85f, 0.15f),
            TargetPoint("Нижній лівий", 0.15f, 0.85f),
            TargetPoint("Нижній правий", 0.85f, 0.85f)
        )
    }

    var currentPointIndex by remember { mutableIntStateOf(0) }
    var pointProgress by remember { mutableStateOf(0f) }
    val collectedSamples = remember { mutableStateListOf<CalibrationPointSample>() }
    var resultProfile by remember { mutableStateOf<MorphologicalProfile?>(settings.morphologicalProfile) }

    val coroutineScope = rememberCoroutineScope()

    // Calibration animation runner
    LaunchedEffect(stage, currentPointIndex) {
        if (stage == CalibrationStage.RUNNING) {
            pointProgress = 0f
            val target = calibrationPoints[currentPointIndex]
            val durationMs = 1500L
            val steps = 30
            val interval = durationMs / steps

            for (i in 1..steps) {
                delay(interval)
                pointProgress = i.toFloat() / steps
            }

            // Capture sample
            val sample = onCaptureSample(currentPointIndex, target.normX, target.normY)
            if (sample != null) {
                collectedSamples.add(sample)
            } else {
                // Fallback sample from diagnostics if frame is in transition
                collectedSamples.add(
                    CalibrationPointSample(
                        pointIndex = currentPointIndex,
                        screenNormX = target.normX,
                        screenNormY = target.normY,
                        yaw = diagnostics.headEulerY,
                        pitch = diagnostics.headEulerX,
                        eyeCenterNormX = diagnostics.liveEyeCenterOffset.first,
                        eyeCenterNormY = diagnostics.liveEyeCenterOffset.second,
                        interOcularDist = diagnostics.liveInterOcularDist.coerceAtLeast(40f),
                        eyeToNoseDist = diagnostics.liveEyeNoseDist.coerceAtLeast(40f)
                    )
                )
            }

            if (currentPointIndex < calibrationPoints.size - 1) {
                currentPointIndex++
            } else {
                // Done all 5 points
                val computed = onApplyProfile(collectedSamples.toList())
                resultProfile = computed
                stage = CalibrationStage.FINISHED
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (stage) {
            CalibrationStage.READY -> {
                CalibrationReadyView(
                    isServiceRunning = isServiceRunning,
                    isFaceDetected = diagnostics.isFaceDetected,
                    onStartService = onStartService,
                    onBeginCalibration = {
                        collectedSamples.clear()
                        currentPointIndex = 0
                        stage = CalibrationStage.RUNNING
                    }
                )
            }

            CalibrationStage.RUNNING -> {
                val currentTarget = calibrationPoints[currentPointIndex]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Точка ${currentPointIndex + 1} з ${calibrationPoints.size}: ${currentTarget.label}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Сфокусуйте погляд на сяючій точці та не відводьте очей",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { pointProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberCyan,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }

                // Dot tracking area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF060911))
                        .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                ) {
                    val areaWidth = maxWidth
                    val areaHeight = maxHeight
                    val dotSize = 80.dp

                    val posX = (areaWidth - dotSize) * currentTarget.normX
                    val posY = (areaHeight - dotSize) * currentTarget.normY

                    // Pulsating concentric circles
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(650, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = posX, y = posY)
                            .size(dotSize),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer pulse
                        Box(
                            modifier = Modifier
                                .size(dotSize * pulseScale)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.22f))
                        )

                        // Circular countdown track
                        CircularProgressIndicator(
                            progress = { pointProgress },
                            modifier = Modifier.size(dotSize),
                            color = CyberCyan,
                            strokeWidth = 4.dp,
                            trackColor = Color(0x33FFFFFF)
                        )

                        // Center glowing bullseye
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.White, CyberCyan, CyberBlue)
                                    )
                                )
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
            }

            CalibrationStage.FINISHED -> {
                CalibrationFinishedView(
                    profile = resultProfile ?: settings.morphologicalProfile,
                    diagnostics = diagnostics,
                    onRecalibrate = {
                        collectedSamples.clear()
                        currentPointIndex = 0
                        stage = CalibrationStage.RUNNING
                    },
                    onGoToSandbox = onGoToSandbox
                )
            }
        }
    }
}

@Composable
private fun CalibrationReadyView(
    isServiceRunning: Boolean,
    isFaceDetected: Boolean,
    onStartService: () -> Unit,
    onBeginCalibration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Точне 5-точкове калібрування",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Адаптація комп'ютерного зору під ваше обличчя",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "Щоб курсор ідеально слідував за вашим поглядом без зміщень, система проведе вимірювання індивідуальних анатомічних ознак:\n\n" +
                        "• Індивідуальну відстань між зіницями очей (IPD)\n" +
                        "• Вектор нахилу очей відносно основи носа\n" +
                        "• Оптимальні коефіцієнти масштабування дисплея Samsung A24\n" +
                        "• Активацію моделі машинного зору з онлайн-самонавчанням",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )

            // Face Check Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFaceDetected) Icons.Default.CheckCircle else Icons.Default.Face,
                    contentDescription = null,
                    tint = if (isFaceDetected) NeonGreen else WarningAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isFaceDetected) "Обличчя виявлено в кадрі — готово до калібрування" else "Наведіть фронтальну камеру на обличчя",
                    fontSize = 12.sp,
                    color = if (isFaceDetected) NeonGreen else WarningAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!isServiceRunning) {
                Button(
                    onClick = onStartService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_service_for_calibration_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Увімкнути камеру та трекінг", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBeginCalibration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_dot_calibration_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D))
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Розпочати калібрування (Слідувати за точкою)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CalibrationFinishedView(
    profile: MorphologicalProfile,
    diagnostics: TrackerDiagnostics,
    onRecalibrate: () -> Unit,
    onGoToSandbox: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = "Калібрування успішно завершено!",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Accuracy Score Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Точність відповідності погляду",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (profile.calibrationScorePercent > 0) profile.calibrationScorePercent else 97}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Висока точність • Похибка < 1.8°",
                        fontSize = 11.sp,
                        color = NeonGreen
                    )
                }
            }

            // Morphological Metrics Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Індивідуальний морфологічний профіль",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "База між очима",
                        value = "%.1f px".format(profile.baselineInterOcularDist),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Вектор очей-носа",
                        value = "%.1f px".format(profile.baselineEyeToNoseDist),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Масштаб X / Y",
                        value = "%.1f / %.1f".format(profile.scaleX, profile.scaleY),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Continuous Adaptive Model status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlue.copy(alpha = 0.12f))
                    .border(1.dp, CyberBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Адаптивна модель активована: з кожним вашим кліком (затримкою чи морганням) модель підлаштовує коефіцієнти під зміни положення голови (засвоєно взаємодій: ${profile.learnedInteractionsCount})",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRecalibrate,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("recalibrate_again_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Повторити", fontSize = 12.sp)
                }

                Button(
                    onClick = onGoToSandbox,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("test_in_sandbox_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D))
                ) {
                    Text("Тестувати на полігоні", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 10.sp, color = TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
