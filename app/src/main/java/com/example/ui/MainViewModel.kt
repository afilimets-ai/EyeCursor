package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.model.AppSettings
import com.example.model.CalibrationPointSample
import com.example.model.CursorMode
import com.example.model.MorphologicalProfile
import com.example.model.TrackerDiagnostics
import com.example.service.EyeCursorAccessibilityService
import com.example.service.EyeTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Головна"),
    CALIBRATION("Калібрування"),
    SANDBOX("Тренування"),
    SETTINGS("Налаштування"),
    SAMSUNG_GUIDE("Samsung A24")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository.getInstance(application)
    val settingsFlow: StateFlow<AppSettings> = repository.settingsFlow

    val isServiceRunning: StateFlow<Boolean> = EyeTrackingService.isServiceRunning
    val isAccessibilityActive: StateFlow<Boolean> = EyeCursorAccessibilityService.isServiceActive
    val diagnosticsFlow: StateFlow<TrackerDiagnostics> = EyeTrackingService.diagnosticsFlow

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(checkCameraPermission())
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(checkOverlayPermission())
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    // Sandbox training state
    private val _sandboxScore = MutableStateFlow(0)
    val sandboxScore: StateFlow<Int> = _sandboxScore.asStateFlow()

    private val _sandboxTargetIndex = MutableStateFlow(0)
    val sandboxTargetIndex: StateFlow<Int> = _sandboxTargetIndex.asStateFlow()

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun refreshPermissions() {
        _hasCameraPermission.value = checkCameraPermission()
        _hasOverlayPermission.value = checkOverlayPermission()
    }

    fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(getApplication())
    }

    fun toggleService() {
        val context = getApplication<Application>()
        if (isServiceRunning.value) {
            EyeTrackingService.stopService(context)
            repository.updateSettings { it.copy(isEnabled = false) }
        } else {
            if (!checkOverlayPermission() || !checkCameraPermission()) {
                return
            }
            EyeTrackingService.startService(context)
            repository.updateSettings { it.copy(isEnabled = true) }
        }
    }

    fun recalibrate() {
        val context = getApplication<Application>()
        EyeTrackingService.recalibrate(context)
    }

    fun captureCalibrationSample(pointIndex: Int, normX: Float, normY: Float): CalibrationPointSample? {
        return EyeTrackingService.currentInstance?.captureCalibrationPoint(pointIndex, normX, normY)
    }

    fun applyCalibrationProfile(samples: List<CalibrationPointSample>): MorphologicalProfile? {
        val service = EyeTrackingService.currentInstance
        return if (service != null) {
            service.completeCalibrationWithSamples(samples)
        } else {
            val dummyEngine = com.example.tracking.GazeTrackerEngine(
                context = getApplication(),
                screenWidth = 1080,
                screenHeight = 2340,
                onCursorUpdate = { _, _, _, _, _ -> },
                onTriggerAction = { _, _, _, _ -> },
                onDiagnosticsUpdate = {}
            )
            val profile = dummyEngine.calculateMorphologicalProfile(samples)
            repository.updateSettings { it.copy(morphologicalProfile = profile) }
            profile
        }
    }

    fun triggerManualTestClick() {
        val context = getApplication<Application>()
        val intent = Intent(context, EyeTrackingService::class.java).apply {
            action = EyeTrackingService.ACTION_CLICK_NOW
        }
        context.startService(intent)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        repository.updateSettings(update)
    }

    fun resetToDefaults() {
        repository.updateSettings {
            AppSettings(isEnabled = it.isEnabled)
        }
    }

    fun hitSandboxTarget() {
        _sandboxScore.value += 1
        _sandboxTargetIndex.value = (_sandboxTargetIndex.value + 1) % 5
    }

    fun resetSandbox() {
        _sandboxScore.value = 0
        _sandboxTargetIndex.value = 0
    }
}
