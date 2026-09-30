package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.TrackerDiagnostics
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
fun SandboxScreen(
    score: Int,
    targetIndex: Int,
    diagnostics: TrackerDiagnostics,
    isServiceRunning: Boolean,
    onHitTarget: () -> Unit,
    onResetSandbox: () -> Unit,
    onRecalibrate: () -> Unit
) {
    val targetPositions = listOf(
        Pair(0.5f, 0.35f),
        Pair(0.25f, 0.55f),
        Pair(0.75f, 0.55f),
        Pair(0.35f, 0.75f),
        Pair(0.65f, 0.75f)
    )

    val currentPos = targetPositions[targetIndex % targetPositions.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Тренувальний полігон",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Практикуйте рух очей, затримку та кліки",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$score",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarningAmber
                    )
                }
            }
        }

        // Live guidance strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isServiceRunning) CyberBlue.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    if (isServiceRunning) CyberCyan.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (isServiceRunning) CyberCyan else WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isServiceRunning) {
                        if (diagnostics.isSustainedBlinkActive) {
                            "⚡ ${diagnostics.activeEyeGesture}: ${(diagnostics.blinkProgress * 100).toInt()}%..."
                        } else {
                            "Наведіть курсор поглядом на мішень. Заплющіть очі на ~0.4 сек (тривале моргання), підморгніть або затримайте погляд для тапу!"
                        }
                    } else {
                        "Увімкніть трекінг погляду на вкладці «Головна» для початку тренування!"
                    },
                    fontSize = 12.sp,
                    fontWeight = if (diagnostics.isSustainedBlinkActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (diagnostics.isSustainedBlinkActive) WarningAmber else TextPrimary
                )
            }
        }

        // Interactive Target Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
        ) {
            val areaWidth = maxWidth
            val areaHeight = maxHeight

            val targetSize = 88.dp
            val offsetX = (areaWidth - targetSize) * currentPos.first
            val offsetY = (areaHeight - targetSize) * currentPos.second

            // Grid background lines
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Зона прицілювання",
                    fontSize = 12.sp,
                    color = TextMuted.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold
                )
            }

            // Target Bullseye (clickable directly or via gaze cursor dispatch)
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(targetSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyberCyan, CyberBlue, Color(0xFF0D1B2A))
                        )
                    )
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onHitTarget() }
                    .testTag("sandbox_target_button"),
                contentAlignment = Alignment.Center
            ) {
                // Inner bullseye rings
                Box(
                    modifier = Modifier
                        .size(targetSize * 0.6f)
                        .clip(CircleShape)
                        .background(Color(0xFF090D16))
                        .border(2.dp, CyberTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onResetSandbox,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("reset_sandbox_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Скинути рахунок", fontSize = 12.sp)
            }

            Button(
                onClick = onRecalibrate,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("sandbox_recalibrate_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Центрувати", fontSize = 12.sp, color = TextPrimary)
            }
        }
    }
}
