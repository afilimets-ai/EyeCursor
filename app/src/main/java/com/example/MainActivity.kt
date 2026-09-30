package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.CalibrationWizardScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SamsungGuideScreen
import com.example.ui.screens.SandboxScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermissions()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    viewModel.refreshPermissions()
                }

                val currentTab by viewModel.currentTab.collectAsState()
                val isServiceRunning by viewModel.isServiceRunning.collectAsState()
                val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
                val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
                val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
                val diagnostics by viewModel.diagnosticsFlow.collectAsState()
                val settings by viewModel.settingsFlow.collectAsState()
                val sandboxScore by viewModel.sandboxScore.collectAsState()
                val sandboxTargetIndex by viewModel.sandboxTargetIndex.collectAsState()

                BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
                    viewModel.setTab(AppTab.DASHBOARD)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "EyeCursor • Samsung A24",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.DASHBOARD,
                                onClick = { viewModel.setTab(AppTab.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Головна"
                                    )
                                },
                                label = { Text("Головна", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.CALIBRATION,
                                onClick = { viewModel.setTab(AppTab.CALIBRATION) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Калібрування"
                                    )
                                },
                                label = { Text("Калібрування", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_calibration")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SANDBOX,
                                onClick = { viewModel.setTab(AppTab.SANDBOX) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CenterFocusStrong,
                                        contentDescription = "Тренування"
                                    )
                                },
                                label = { Text("Тренування", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_sandbox")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SETTINGS,
                                onClick = { viewModel.setTab(AppTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Налаштування"
                                    )
                                },
                                label = { Text("Налаштування", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_settings")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SAMSUNG_GUIDE,
                                onClick = { viewModel.setTab(AppTab.SAMSUNG_GUIDE) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Посібник"
                                    )
                                },
                                label = { Text("Samsung A24", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_guide")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_animation"
                        ) { tab ->
                            when (tab) {
                                AppTab.DASHBOARD -> DashboardScreen(
                                    isServiceRunning = isServiceRunning,
                                    isAccessibilityActive = isAccessibilityActive,
                                    hasCameraPermission = hasCameraPermission,
                                    hasOverlayPermission = hasOverlayPermission,
                                    diagnostics = diagnostics,
                                    settings = settings,
                                    onRequestCameraPermission = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    onOpenOverlaySettings = { ctx ->
                                        viewModel.openOverlaySettings(ctx)
                                    },
                                    onOpenAccessibilitySettings = { ctx ->
                                        viewModel.openAccessibilitySettings(ctx)
                                    },
                                    onToggleService = { viewModel.toggleService() },
                                    onRecalibrate = { viewModel.recalibrate() },
                                    onTestClick = { viewModel.triggerManualTestClick() },
                                    onOpenCalibration = { viewModel.setTab(AppTab.CALIBRATION) }
                                )

                                AppTab.CALIBRATION -> CalibrationWizardScreen(
                                    settings = settings,
                                    diagnostics = diagnostics,
                                    isServiceRunning = isServiceRunning,
                                    onStartService = { viewModel.toggleService() },
                                    onCaptureSample = { idx, nx, ny -> viewModel.captureCalibrationSample(idx, nx, ny) },
                                    onApplyProfile = { samples -> viewModel.applyCalibrationProfile(samples) },
                                    onGoToSandbox = { viewModel.setTab(AppTab.SANDBOX) }
                                )

                                AppTab.SANDBOX -> SandboxScreen(
                                    score = sandboxScore,
                                    targetIndex = sandboxTargetIndex,
                                    diagnostics = diagnostics,
                                    isServiceRunning = isServiceRunning,
                                    onHitTarget = { viewModel.hitSandboxTarget() },
                                    onResetSandbox = { viewModel.resetSandbox() },
                                    onRecalibrate = { viewModel.recalibrate() }
                                )

                                AppTab.SETTINGS -> SettingsScreen(
                                    settings = settings,
                                    onUpdateSettings = { update -> viewModel.updateSettings(update) },
                                    onResetDefaults = { viewModel.resetToDefaults() }
                                )

                                AppTab.SAMSUNG_GUIDE -> SamsungGuideScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
