package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
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

@Composable
fun DashboardScreen(
    isServiceRunning: Boolean,
    isAccessibilityActive: Boolean,
    hasCameraPermission: Boolean,
    hasOverlayPermission: Boolean,
    diagnostics: TrackerDiagnostics,
    settings: AppSettings,
    onRequestCameraPermission: () -> Unit,
    onOpenOverlaySettings: (Context) -> Unit,
    onOpenAccessibilitySettings: (Context) -> Unit,
    onToggleService: () -> Unit,
    onRecalibrate: () -> Unit,
    onTestClick: () -> Unit,
    onOpenCalibration: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val canStartService = hasCameraPermission && hasOverlayPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mandatory Calibration Alert if not calibrated
        if (!settings.morphologicalProfile.isCalibrated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Потрібне 5-точкове калібрування",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    Text(
                        text = "Для точного керування поглядом без зсувів пройдіть швидке калібрування по точках. Модель комп'ютерного зору адаптується під анатомічну відстань між вашими очима та форму обличчя!",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )

                    Button(
                        onClick = onOpenCalibration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("open_calibration_banner_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black)
                    ) {
                        Text("Пройти 5-точкове калібрування", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        // Hero Card with Service Status & Master Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isServiceRunning) CyberCyan else DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Pill
                val statusBgColor by animateColorAsState(
                    targetValue = if (isServiceRunning) NeonGreen.copy(alpha = 0.18f) else DangerCoral.copy(alpha = 0.18f)
                )
                val statusTextColor by animateColorAsState(
                    targetValue = if (isServiceRunning) NeonGreen else DangerCoral
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusTextColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "ТРЕКІНГ ПОГЛЯДУ АКТИВНИЙ" else "СЕРВІС ЗУПИНЕНО",
                        color = statusTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "EyeCursor для Samsung A24",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Керування курсором та кліками рухами очей і голови через фронтальну камеру",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Big Master Toggle Button
                Button(
                    onClick = onToggleService,
                    enabled = canStartService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("toggle_service_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) DangerCoral else CyberCyan,
                        contentColor = if (isServiceRunning) Color.White else Color(0xFF00363D)
                    )
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isServiceRunning) "Зупинити трекінг" else "Запустити трекінг погляду",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!canStartService) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Для запуску потрібні дозволи камери та показу поверх додатків",
                        color = WarningAmber,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Permissions Checklist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Системні дозволи для роботи без дотиків",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 1. Camera permission
                PermissionItem(
                    title = "Фронтальна камера",
                    description = "Для захоплення рухів очей та нахилу голови",
                    icon = Icons.Default.CameraAlt,
                    isGranted = hasCameraPermission,
                    actionText = "Надати дозвіл",
                    onAction = onRequestCameraPermission,
                    testTag = "grant_camera_permission_button"
                )

                // 2. Overlay permission
                PermissionItem(
                    title = "Відображення поверх додатків",
                    description = "Для відображення курсора поверх будь-яких програм",
                    icon = Icons.Default.Layers,
                    isGranted = hasOverlayPermission,
                    actionText = "Надати оверлей",
                    onAction = { onOpenOverlaySettings(context) },
                    testTag = "grant_overlay_permission_button"
                )

                // 3. Accessibility service
                PermissionItem(
                    title = "Служба доступності (Кліки та жести)",
                    description = "Необхідно для автоматичного виконання кліків та скролу при зламаному сенсорі",
                    icon = Icons.Default.TouchApp,
                    isGranted = isAccessibilityActive,
                    actionText = "Увімкнути службу",
                    onAction = { onOpenAccessibilitySettings(context) },
                    testTag = "enable_accessibility_button"
                )
            }
        }

        // Live Diagnostic Telemetry Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Телеметрія обличчя та очей",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (diagnostics.isFaceDetected) NeonGreen else DangerCoral)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (diagnostics.isFaceDetected) "Обличчя знайдено" else "Пошук обличчя...",
                            fontSize = 12.sp,
                            color = if (diagnostics.isFaceDetected) NeonGreen else WarningAmber
                        )
                    }
                }

                // Angles display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryValue(
                        label = "Поворот (Yaw)",
                        value = "%.1f°".format(diagnostics.headEulerY),
                        subLabel = "Вліво/Вправо"
                    )
                    TelemetryValue(
                        label = "Нахил (Pitch)",
                        value = "%.1f°".format(diagnostics.headEulerX),
                        subLabel = "Вгору/Вниз"
                    )
                    TelemetryValue(
                        label = "Курсор X, Y",
                        value = "${diagnostics.cursorX.toInt()}, ${diagnostics.cursorY.toInt()}",
                        subLabel = "Пікселі"
                    )
                }

                // Morphological Face Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryValue(
                        label = "База між очима",
                        value = "${diagnostics.liveInterOcularDist.toInt()} px",
                        subLabel = "Морфологія"
                    )
                    TelemetryValue(
                        label = "Вектор очі-ніс",
                        value = "${diagnostics.liveEyeNoseDist.toInt()} px",
                        subLabel = "Кут екрана"
                    )
                    TelemetryValue(
                        label = "Адаптація моделі",
                        value = if (settings.morphologicalProfile.isCalibrated) "${settings.morphologicalProfile.calibrationScorePercent}%" else "Немає",
                        subLabel = "Вивчено: ${settings.morphologicalProfile.learnedInteractionsCount}"
                    )
                }

                // Eye openness
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ліве око (Підморгування)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(diagnostics.leftEyeOpenProb * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diagnostics.leftEyeOpenProb < 0.25f) CyberCyan else TextPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { diagnostics.leftEyeOpenProb.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (diagnostics.leftEyeOpenProb < 0.25f) CyberCyan else NeonGreen,
                        trackColor = DarkSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Праве око",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(diagnostics.rightEyeOpenProb * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { diagnostics.rightEyeOpenProb.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonGreen,
                        trackColor = DarkSurfaceVariant
                    )
                }

                // Sustained Blink / Wink Active Progress Indicator
                if (diagnostics.isSustainedBlinkActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningAmber.copy(alpha = 0.15f))
                            .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ ${diagnostics.activeEyeGesture.ifEmpty { "Фіксація моргання..." }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                Text(
                                    text = "${(diagnostics.blinkProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WarningAmber
                                )
                            }
                            LinearProgressIndicator(
                                progress = { diagnostics.blinkProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = WarningAmber,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }

                // Last click source info
                if (diagnostics.lastClickTimestamp > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Останнє спрацювання: ${diagnostics.lastTriggerSource}",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Quick Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRecalibrate,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("recalibrate_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Центрувати", fontSize = 13.sp, color = TextPrimary)
            }

            Button(
                onClick = onTestClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("test_click_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = CyberTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Тест кліку", fontSize = 13.sp, color = TextPrimary)
            }
        }

        // Hardware Volume Button Tip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberBlue.copy(alpha = 0.12f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBlue.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Фізичні кнопки для Samsung A24",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Text(
                        text = "Гучність (+) = миттєвий клік у позиції курсора\nГучність (-) = центрування погляду",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) NeonGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) NeonGreen else WarningAmber,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(text = actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TelemetryValue(label: String, value: String, subLabel: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberCyan)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subLabel, fontSize = 9.sp, color = TextMuted)
    }
}
