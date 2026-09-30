package com.example.model

enum class BlinkTriggerType {
    BOTH_OR_WINK,
    SUSTAINED_BLINK_ONLY,
    WINK_ONLY
}

data class MorphologicalProfile(
    val isCalibrated: Boolean = false,
    val calibrationScorePercent: Int = 0,
    val baselineInterOcularDist: Float = 60f,
    val baselineEyeToNoseDist: Float = 55f,
    val faceAspectRatio: Float = 1.35f,
    val eyeAsymmetryOffset: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val adaptiveGainX: Float = 1.0f,
    val adaptiveGainY: Float = 1.0f,
    val learnedInteractionsCount: Int = 0
)

data class CalibrationPointSample(
    val pointIndex: Int,
    val screenNormX: Float, // 0.0 .. 1.0
    val screenNormY: Float, // 0.0 .. 1.0
    val yaw: Float,
    val pitch: Float,
    val eyeCenterNormX: Float,
    val eyeCenterNormY: Float,
    val interOcularDist: Float,
    val eyeToNoseDist: Float
)

data class AppSettings(
    val isEnabled: Boolean = false,
    val sensitivityX: Float = 1.9f,
    val sensitivityY: Float = 2.4f,
    val deadzone: Float = 0.04f,
    val smoothingFactor: Float = 0.22f,
    val dwellClickEnabled: Boolean = true,
    val dwellDurationMs: Long = 1000L,
    val blinkClickEnabled: Boolean = true,
    val sustainedBlinkDurationMs: Long = 400L,
    val sustainedWinkDurationMs: Long = 320L,
    val eyeClosureThreshold: Float = 0.22f,
    val blinkTriggerType: BlinkTriggerType = BlinkTriggerType.BOTH_OR_WINK,
    val continuousAdaptationEnabled: Boolean = true,
    val cursorSizeDp: Int = 36,
    val cursorColorHex: Long = 0xFF00E5FF,
    val showCameraBubble: Boolean = true,
    val showQuickActionsDock: Boolean = true,
    val volumeButtonTrigger: Boolean = true,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val vibrationFeedback: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val morphologicalProfile: MorphologicalProfile = MorphologicalProfile()
)

enum class CursorMode {
    CLICK,
    SCROLL_UP,
    SCROLL_DOWN,
    LONG_PRESS
}

data class TrackerDiagnostics(
    val isFaceDetected: Boolean = false,
    val headEulerX: Float = 0f,
    val headEulerY: Float = 0f,
    val headEulerZ: Float = 0f,
    val leftEyeOpenProb: Float = 1f,
    val rightEyeOpenProb: Float = 1f,
    val cursorX: Float = 0f,
    val cursorY: Float = 0f,
    val dwellProgress: Float = 0f,
    val isDwellActive: Boolean = false,
    val isSustainedBlinkActive: Boolean = false,
    val blinkProgress: Float = 0f,
    val activeEyeGesture: String = "",
    val isPaused: Boolean = false,
    val currentMode: CursorMode = CursorMode.CLICK,
    val lastClickTimestamp: Long = 0L,
    val lastTriggerSource: String = "",
    val liveInterOcularDist: Float = 0f,
    val liveEyeNoseDist: Float = 0f,
    val liveEyeCenterOffset: Pair<Float, Float> = Pair(0f, 0f),
    val isMorphologyAdapted: Boolean = false,
    val learnedInteractionsCount: Int = 0
)
