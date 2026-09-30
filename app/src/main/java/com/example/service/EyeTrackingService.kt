package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.EyeCursorApp
import com.example.MainActivity
import com.example.data.SettingsRepository
import com.example.model.AppSettings
import com.example.model.CursorMode
import com.example.model.TrackerDiagnostics
import com.example.tracking.GazeTrackerEngine
import com.example.ui.overlay.CursorOverlayView
import com.example.ui.overlay.FloatingQuickDockView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class EyeTrackingService : Service(), LifecycleOwner, EyeCursorAccessibilityService.KeyEventListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepository: SettingsRepository

    private var cursorOverlayView: CursorOverlayView? = null
    private var quickDockView: FloatingQuickDockView? = null
    private var trackerEngine: GazeTrackerEngine? = null

    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        settingsRepository = SettingsRepository.getInstance(this)

        EyeCursorAccessibilityService.keyEventListener = this
        currentInstance = this
        _isServiceRunning.value = true

        startForegroundServiceNotification()
        setupOverlayViews()
        setupCameraTracking()

        // Observe settings changes
        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                trackerEngine?.currentSettings = settings
                cursorOverlayView?.setCursorConfig(settings.cursorSizeDp, settings.cursorColorHex)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        when (intent?.action) {
            ACTION_RECALIBRATE -> trackerEngine?.calibrateCenter()
            ACTION_TOGGLE_PAUSE -> {
                trackerEngine?.let {
                    it.isPaused = !it.isPaused
                }
            }
            ACTION_CLICK_NOW -> trackerEngine?.triggerManualClick("MANUAL_COMMAND")
            ACTION_STOP -> stopSelf()
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calibIntent = Intent(this, EyeTrackingService::class.java).apply { action = ACTION_RECALIBRATE }
        val calibPending = PendingIntent.getService(this, 1, calibIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, EyeTrackingService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, EyeCursorApp.CHANNEL_ID)
            .setContentTitle("EyeCursor працює")
            .setContentText("Керування поглядом активно. Подивіться на центр для калібрування.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_compass, "Калібрувати", calibPending)
            .addAction(android.R.drawable.ic_delete, "Зупинити", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupOverlayViews() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w("EyeTrackingService", "Overlay permission not granted!")
            return
        }

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // 1. Fullscreen transparent click-through cursor overlay
        val cursorParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val cursor = CursorOverlayView(this)
        val initialSettings = settingsRepository.getSettings()
        cursor.setCursorConfig(initialSettings.cursorSizeDp, initialSettings.cursorColorHex)
        windowManager.addView(cursor, cursorParams)
        cursorOverlayView = cursor

        // 2. Floating Quick Actions Dock
        if (initialSettings.showQuickActionsDock) {
            val dockParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 80 // slight offset from status bar
            }

            val dock = FloatingQuickDockView(
                context = this,
                onModeChanged = { mode ->
                    trackerEngine?.currentMode = mode
                },
                onBackClicked = {
                    EyeCursorAccessibilityService.currentInstance?.triggerGlobalBack()
                },
                onHomeClicked = {
                    EyeCursorAccessibilityService.currentInstance?.triggerGlobalHome()
                },
                onRecentsClicked = {
                    EyeCursorAccessibilityService.currentInstance?.triggerGlobalRecents()
                },
                onCalibrateClicked = {
                    trackerEngine?.calibrateCenter()
                    triggerHaptic()
                },
                onPauseToggled = {
                    trackerEngine?.let { it.isPaused = !it.isPaused }
                    triggerHaptic()
                },
                onDismissRequest = {
                    quickDockView?.let { windowManager.removeView(it) }
                    quickDockView = null
                }
            )

            windowManager.addView(dock, dockParams)
            quickDockView = dock
        }
    }

    private fun setupCameraTracking() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        trackerEngine = GazeTrackerEngine(
            context = this,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            onCursorUpdate = { x, y, dwellProgress, blinkProgress, gestureLabel ->
                cursorOverlayView?.updatePosition(x, y, dwellProgress, blinkProgress, gestureLabel)
            },
            onTriggerAction = { x, y, mode, source ->
                handleTriggerAction(x, y, mode, source, screenHeight)
            },
            onDiagnosticsUpdate = { diagnostics ->
                _diagnosticsFlow.value = diagnostics
                quickDockView?.setTrackingActive(diagnostics.isFaceDetected && !diagnostics.isPaused)
            },
            onMorphologyUpdated = { updatedProfile ->
                settingsRepository.updateSettings { it.copy(morphologicalProfile = updatedProfile) }
            }
        ).apply {
            currentSettings = settingsRepository.getSettings()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                trackerEngine?.let { engine ->
                    imageAnalysis.setAnalyzer(cameraExecutor, engine)
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                Log.d("EyeTrackingService", "CameraX front camera successfully bound to service lifecycle")
            } catch (e: Exception) {
                Log.e("EyeTrackingService", "Failed to bind camera provider", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleTriggerAction(x: Float, y: Float, mode: CursorMode, source: String, screenHeight: Int) {
        cursorOverlayView?.triggerClickAnimation()
        triggerHaptic()

        val accService = EyeCursorAccessibilityService.currentInstance
        if (accService != null) {
            when (mode) {
                CursorMode.CLICK -> {
                    accService.dispatchClick(x, y)
                }
                CursorMode.LONG_PRESS -> {
                    accService.dispatchLongPress(x, y)
                }
                CursorMode.SCROLL_UP -> {
                    // Scroll up (swipe down)
                    accService.dispatchScroll(x, y, x, y + (screenHeight * 0.35f))
                }
                CursorMode.SCROLL_DOWN -> {
                    // Scroll down (swipe up)
                    accService.dispatchScroll(x, y, x, y - (screenHeight * 0.35f))
                }
            }
        } else {
            Log.w("EyeTrackingService", "Accessibility service not running, click not dispatched!")
        }

        _diagnosticsFlow.value = _diagnosticsFlow.value.copy(
            lastClickTimestamp = System.currentTimeMillis(),
            lastTriggerSource = source
        )
    }

    private fun triggerHaptic() {
        if (!settingsRepository.getSettings().vibrationFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (e: Exception) {
            Log.w("EyeTrackingService", "Vibration failed", e)
        }
    }

    override fun onVolumeUp(): Boolean {
        if (!settingsRepository.getSettings().volumeButtonTrigger) return false
        trackerEngine?.triggerManualClick("VOLUME_UP")
        return true
    }

    override fun onVolumeDown(): Boolean {
        if (!settingsRepository.getSettings().volumeButtonTrigger) return false
        trackerEngine?.calibrateCenter()
        triggerHaptic()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        EyeCursorAccessibilityService.keyEventListener = null
        if (currentInstance == this) {
            currentInstance = null
        }
        _isServiceRunning.value = false

        cursorOverlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        quickDockView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        cursorOverlayView = null
        quickDockView = null

        trackerEngine?.shutdown()
        cameraExecutor.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun captureCalibrationPoint(pointIndex: Int, normX: Float, normY: Float): com.example.model.CalibrationPointSample? {
        return trackerEngine?.captureCurrentCalibrationPoint(pointIndex, normX, normY)
    }

    fun completeCalibrationWithSamples(samples: List<com.example.model.CalibrationPointSample>): com.example.model.MorphologicalProfile? {
        val engine = trackerEngine ?: return null
        val profile = engine.calculateMorphologicalProfile(samples)
        settingsRepository.updateSettings { it.copy(morphologicalProfile = profile) }
        engine.currentSettings = settingsRepository.getSettings()
        return profile
    }

    companion object {
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.example.eyecursor.START"
        const val ACTION_STOP = "com.example.eyecursor.STOP"
        const val ACTION_RECALIBRATE = "com.example.eyecursor.RECALIBRATE"
        const val ACTION_TOGGLE_PAUSE = "com.example.eyecursor.TOGGLE_PAUSE"
        const val ACTION_CLICK_NOW = "com.example.eyecursor.CLICK_NOW"

        @Volatile
        var currentInstance: EyeTrackingService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _diagnosticsFlow = MutableStateFlow(TrackerDiagnostics())
        val diagnosticsFlow: StateFlow<TrackerDiagnostics> = _diagnosticsFlow.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, EyeTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EyeTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun recalibrate(context: Context) {
            val intent = Intent(context, EyeTrackingService::class.java).apply {
                action = ACTION_RECALIBRATE
            }
            context.startService(intent)
        }
    }
}
