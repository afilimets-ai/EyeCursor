package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.model.BlinkTriggerType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onResetDefaults: () -> Unit
) {
    val scrollState = rememberScrollState()

    val cursorColors = listOf(
        Pair(0xFF00E5FF, "Бірюза"),
        Pair(0xFF00E676, "Лайм"),
        Pair(0xFFFFD600, "Жовтий"),
        Pair(0xFFFF5252, "Корал"),
        Pair(0xFFE040FB, "Фіолет")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Movement Sensitivity
        SettingsCard(title = "Чутливість та швидкість курсора", icon = Icons.Default.Speed) {
            // Sensitivity X
            SliderSetting(
                label = "Чутливість по горизонталі (X)",
                valueText = "%.1fx".format(settings.sensitivityX),
                value = settings.sensitivityX,
                range = 0.5f..4.0f,
                onValueChange = { v -> onUpdateSettings { it.copy(sensitivityX = v) } },
                testTag = "sensitivity_x_slider"
            )

            // Sensitivity Y
            SliderSetting(
                label = "Чутливість по вертикалі (Y)",
                valueText = "%.1fx".format(settings.sensitivityY),
                value = settings.sensitivityY,
                range = 0.5f..4.0f,
                onValueChange = { v -> onUpdateSettings { it.copy(sensitivityY = v) } },
                testTag = "sensitivity_y_slider"
            )

            // Deadzone
            SliderSetting(
                label = "Мертва зона (стабілізація тремтіння)",
                valueText = "${(settings.deadzone * 100).toInt()}%",
                value = settings.deadzone,
                range = 0.01f..0.12f,
                onValueChange = { v -> onUpdateSettings { it.copy(deadzone = v) } },
                testTag = "deadzone_slider"
            )

            // Smoothing Factor
            SliderSetting(
                label = "Плавність руху курсора (Фільтрація)",
                valueText = "${(settings.smoothingFactor * 100).toInt()}%",
                value = settings.smoothingFactor,
                range = 0.05f..0.50f,
                onValueChange = { v -> onUpdateSettings { it.copy(smoothingFactor = v) } },
                testTag = "smoothing_slider"
            )

            // Invert axes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToggleSettingItem(
                    label = "Інвертувати X",
                    checked = settings.invertX,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(invertX = v) } },
                    modifier = Modifier.weight(1f),
                    testTag = "invert_x_switch"
                )
                Spacer(modifier = Modifier.width(12.dp))
                ToggleSettingItem(
                    label = "Інвертувати Y",
                    checked = settings.invertY,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(invertY = v) } },
                    modifier = Modifier.weight(1f),
                    testTag = "invert_y_switch"
                )
            }
        }

        // Section: Click & Triggers
        SettingsCard(title = "Тригери кліку та взаємодії", icon = Icons.Default.TouchApp) {
            // Dwell click toggle
            ToggleRow(
                label = "Авто-клік затримкою погляду (Dwell)",
                description = "Автоматичний клік при фіксації погляду на кнопці чи посиланні",
                checked = settings.dwellClickEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(dwellClickEnabled = v) } },
                testTag = "dwell_click_switch"
            )

            if (settings.dwellClickEnabled) {
                SliderSetting(
                    label = "Час фіксації для авто-кліку",
                    valueText = "%.1f сек".format(settings.dwellDurationMs / 1000f),
                    value = settings.dwellDurationMs.toFloat(),
                    range = 400f..2500f,
                    onValueChange = { v -> onUpdateSettings { it.copy(dwellDurationMs = v.toLong()) } },
                    testTag = "dwell_duration_slider"
                )
            }

            // Sustained Blink & Wink Click
            ToggleRow(
                label = "Клік тривалим морганням або підморгуванням",
                description = "Фіксація заплющених очей (або одного ока) викликає тап на координатах курсора",
                checked = settings.blinkClickEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(blinkClickEnabled = v) } },
                testTag = "blink_click_switch"
            )

            if (settings.blinkClickEnabled) {
                // Type selector: Both, Blink only, Wink only
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Режим жестів очей", fontSize = 13.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            Triple(BlinkTriggerType.BOTH_OR_WINK, "Моргання + Підморг.", "both_wink"),
                            Triple(BlinkTriggerType.SUSTAINED_BLINK_ONLY, "Лише моргання", "blink_only"),
                            Triple(BlinkTriggerType.WINK_ONLY, "Лише підморг.", "wink_only")
                        )

                        types.forEach { (type, label, tag) ->
                            val isSelected = settings.blinkTriggerType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberCyan else DarkBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onUpdateSettings { it.copy(blinkTriggerType = type) }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .testTag("trigger_type_$tag"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyberCyan else TextPrimary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (settings.blinkTriggerType != BlinkTriggerType.WINK_ONLY) {
                    SliderSetting(
                        label = "Час утримання моргання (обома очима)",
                        valueText = "${settings.sustainedBlinkDurationMs} мс",
                        value = settings.sustainedBlinkDurationMs.toFloat(),
                        range = 250f..850f,
                        onValueChange = { v -> onUpdateSettings { it.copy(sustainedBlinkDurationMs = v.toLong()) } },
                        testTag = "sustained_blink_duration_slider"
                    )
                }

                if (settings.blinkTriggerType != BlinkTriggerType.SUSTAINED_BLINK_ONLY) {
                    SliderSetting(
                        label = "Час утримання підморгування (одним оком)",
                        valueText = "${settings.sustainedWinkDurationMs} мс",
                        value = settings.sustainedWinkDurationMs.toFloat(),
                        range = 200f..650f,
                        onValueChange = { v -> onUpdateSettings { it.copy(sustainedWinkDurationMs = v.toLong()) } },
                        testTag = "sustained_wink_duration_slider"
                    )
                }

                SliderSetting(
                    label = "Поріг закриття ока (чутливість MLKit)",
                    valueText = "${(settings.eyeClosureThreshold * 100).toInt()}%",
                    value = settings.eyeClosureThreshold,
                    range = 0.12f..0.35f,
                    onValueChange = { v -> onUpdateSettings { it.copy(eyeClosureThreshold = v) } },
                    testTag = "eye_closure_threshold_slider"
                )
            }

            // Hardware volume buttons
            ToggleRow(
                label = "Фізичні кнопки гучності як клікер",
                description = "Гучність (+) = Клік у позиції курсора, Гучність (-) = Центрування",
                checked = settings.volumeButtonTrigger,
                onCheckedChange = { v -> onUpdateSettings { it.copy(volumeButtonTrigger = v) } },
                testTag = "volume_trigger_switch"
            )

            // Vibration feedback
            ToggleRow(
                label = "Тактильна вібрація при кліку",
                description = "Легка вібрація смартфона при успішному натисканні",
                checked = settings.vibrationFeedback,
                onCheckedChange = { v -> onUpdateSettings { it.copy(vibrationFeedback = v) } },
                testTag = "vibration_switch"
            )
        }

        // Section: Appearance & Floating Dock
        SettingsCard(title = "Вигляд курсора та панель дій", icon = Icons.Default.Palette) {
            // Show quick dock
            ToggleRow(
                label = "Плаваюча панель швидких дій",
                description = "Кнопки Назад, Додому, Меню та режим скролу на екрані",
                checked = settings.showQuickActionsDock,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showQuickActionsDock = v) } },
                testTag = "quick_dock_switch"
            )

            // Cursor Size
            SliderSetting(
                label = "Розмір прицілу курсора",
                valueText = "${settings.cursorSizeDp} dp",
                value = settings.cursorSizeDp.toFloat(),
                range = 24f..56f,
                onValueChange = { v -> onUpdateSettings { it.copy(cursorSizeDp = v.toInt()) } },
                testTag = "cursor_size_slider"
            )

            // Color Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Колір підсвічування курсора", fontSize = 13.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    cursorColors.forEach { (colorHex, name) ->
                        val isSelected = settings.cursorColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else DarkBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateSettings { it.copy(cursorColorHex = colorHex) }
                                }
                                .testTag("color_picker_${name.lowercase()}")
                        )
                    }
                }
            }

            // Auto-start on boot
            ToggleRow(
                label = "Автозапуск при перезавантаженні смартфона",
                description = "Критично при неробочому сенсорі, щоб не втратити доступ після розрядки",
                checked = settings.autoStartOnBoot,
                onCheckedChange = { v -> onUpdateSettings { it.copy(autoStartOnBoot = v) } },
                testTag = "boot_start_switch"
            )
        }

        // Section: Machine Vision Model & Morphology Adaptation
        SettingsCard(title = "Машинний зір та морфологічна адаптація", icon = Icons.Default.Tune) {
            ToggleRow(
                label = "Онлайн-адаптація до форми обличчя",
                description = "Модель машинного зору автоматично підлаштовує коефіцієнти при кожному успішному кліку (вивчено взаємодій: ${settings.morphologicalProfile.learnedInteractionsCount})",
                checked = settings.continuousAdaptationEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(continuousAdaptationEnabled = v) } },
                testTag = "continuous_adaptation_switch"
            )

            // Current Morphological State Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Статус калібрування:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = if (settings.morphologicalProfile.isCalibrated) "Відкалібровано (${settings.morphologicalProfile.calibrationScorePercent}%)" else "Не відкалібровано",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.morphologicalProfile.isCalibrated) NeonGreen else WarningAmber
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Анатомічна база між очима:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = "%.1f px".format(settings.morphologicalProfile.baselineInterOcularDist),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Базовий кут очей-носа:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = "%.1f px".format(settings.morphologicalProfile.baselineEyeToNoseDist),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Адаптивні коефіцієнти X / Y:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = "%.2f / %.2f".format(settings.morphologicalProfile.adaptiveGainX, settings.morphologicalProfile.adaptiveGainY),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }
            }
        }

        // Reset to Defaults Button
        OutlinedButton(
            onClick = onResetDefaults,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("reset_defaults_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Скинути всі налаштування до стандартних", fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = TextPrimary)
            Text(text = valueText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun ToggleSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF101726)
            )
        )
    }
}
