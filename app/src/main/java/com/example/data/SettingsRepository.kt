package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppSettings
import com.example.model.BlinkTriggerType
import com.example.model.MorphologicalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("eye_cursor_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): AppSettings = _settingsFlow.value

    private fun loadSettings(): AppSettings {
        val triggerTypeName = prefs.getString(KEY_BLINK_TRIGGER_TYPE, BlinkTriggerType.BOTH_OR_WINK.name)
        val triggerType = try {
            BlinkTriggerType.valueOf(triggerTypeName ?: BlinkTriggerType.BOTH_OR_WINK.name)
        } catch (e: Exception) {
            BlinkTriggerType.BOTH_OR_WINK
        }

        val morphology = MorphologicalProfile(
            isCalibrated = prefs.getBoolean(KEY_MORPH_CALIBRATED, false),
            calibrationScorePercent = prefs.getInt(KEY_MORPH_SCORE, 0),
            baselineInterOcularDist = prefs.getFloat(KEY_MORPH_INTER_OCULAR, 60f),
            baselineEyeToNoseDist = prefs.getFloat(KEY_MORPH_EYE_NOSE, 55f),
            faceAspectRatio = prefs.getFloat(KEY_MORPH_ASPECT, 1.35f),
            eyeAsymmetryOffset = prefs.getFloat(KEY_MORPH_ASYMMETRY, 0f),
            scaleX = prefs.getFloat(KEY_MORPH_SCALE_X, 1.0f),
            scaleY = prefs.getFloat(KEY_MORPH_SCALE_Y, 1.0f),
            offsetX = prefs.getFloat(KEY_MORPH_OFFSET_X, 0f),
            offsetY = prefs.getFloat(KEY_MORPH_OFFSET_Y, 0f),
            adaptiveGainX = prefs.getFloat(KEY_MORPH_GAIN_X, 1.0f),
            adaptiveGainY = prefs.getFloat(KEY_MORPH_GAIN_Y, 1.0f),
            learnedInteractionsCount = prefs.getInt(KEY_MORPH_LEARNED_COUNT, 0)
        )

        return AppSettings(
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false),
            sensitivityX = prefs.getFloat(KEY_SENSITIVITY_X, 1.9f),
            sensitivityY = prefs.getFloat(KEY_SENSITIVITY_Y, 2.4f),
            deadzone = prefs.getFloat(KEY_DEADZONE, 0.04f),
            smoothingFactor = prefs.getFloat(KEY_SMOOTHING, 0.22f),
            dwellClickEnabled = prefs.getBoolean(KEY_DWELL_ENABLED, true),
            dwellDurationMs = prefs.getLong(KEY_DWELL_DURATION, 1000L),
            blinkClickEnabled = prefs.getBoolean(KEY_BLINK_ENABLED, true),
            sustainedBlinkDurationMs = prefs.getLong(KEY_SUSTAINED_BLINK_DURATION, 400L),
            sustainedWinkDurationMs = prefs.getLong(KEY_SUSTAINED_WINK_DURATION, 320L),
            eyeClosureThreshold = prefs.getFloat(KEY_EYE_CLOSURE_THRESHOLD, 0.22f),
            blinkTriggerType = triggerType,
            continuousAdaptationEnabled = prefs.getBoolean(KEY_CONTINUOUS_ADAPTATION, true),
            cursorSizeDp = prefs.getInt(KEY_CURSOR_SIZE, 36),
            cursorColorHex = prefs.getLong(KEY_CURSOR_COLOR, 0xFF00E5FF),
            showCameraBubble = prefs.getBoolean(KEY_SHOW_BUBBLE, true),
            showQuickActionsDock = prefs.getBoolean(KEY_SHOW_DOCK, true),
            volumeButtonTrigger = prefs.getBoolean(KEY_VOLUME_TRIGGER, true),
            invertX = prefs.getBoolean(KEY_INVERT_X, false),
            invertY = prefs.getBoolean(KEY_INVERT_Y, false),
            vibrationFeedback = prefs.getBoolean(KEY_VIBRATION, true),
            autoStartOnBoot = prefs.getBoolean(KEY_BOOT_START, true),
            morphologicalProfile = morphology
        )
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val current = _settingsFlow.value
        val updated = update(current)
        prefs.edit().apply {
            putBoolean(KEY_IS_ENABLED, updated.isEnabled)
            putFloat(KEY_SENSITIVITY_X, updated.sensitivityX)
            putFloat(KEY_SENSITIVITY_Y, updated.sensitivityY)
            putFloat(KEY_DEADZONE, updated.deadzone)
            putFloat(KEY_SMOOTHING, updated.smoothingFactor)
            putBoolean(KEY_DWELL_ENABLED, updated.dwellClickEnabled)
            putLong(KEY_DWELL_DURATION, updated.dwellDurationMs)
            putBoolean(KEY_BLINK_ENABLED, updated.blinkClickEnabled)
            putLong(KEY_SUSTAINED_BLINK_DURATION, updated.sustainedBlinkDurationMs)
            putLong(KEY_SUSTAINED_WINK_DURATION, updated.sustainedWinkDurationMs)
            putFloat(KEY_EYE_CLOSURE_THRESHOLD, updated.eyeClosureThreshold)
            putString(KEY_BLINK_TRIGGER_TYPE, updated.blinkTriggerType.name)
            putBoolean(KEY_CONTINUOUS_ADAPTATION, updated.continuousAdaptationEnabled)
            putInt(KEY_CURSOR_SIZE, updated.cursorSizeDp)
            putLong(KEY_CURSOR_COLOR, updated.cursorColorHex)
            putBoolean(KEY_SHOW_BUBBLE, updated.showCameraBubble)
            putBoolean(KEY_SHOW_DOCK, updated.showQuickActionsDock)
            putBoolean(KEY_VOLUME_TRIGGER, updated.volumeButtonTrigger)
            putBoolean(KEY_INVERT_X, updated.invertX)
            putBoolean(KEY_INVERT_Y, updated.invertY)
            putBoolean(KEY_VIBRATION, updated.vibrationFeedback)
            putBoolean(KEY_BOOT_START, updated.autoStartOnBoot)

            // Morphological profile
            val morph = updated.morphologicalProfile
            putBoolean(KEY_MORPH_CALIBRATED, morph.isCalibrated)
            putInt(KEY_MORPH_SCORE, morph.calibrationScorePercent)
            putFloat(KEY_MORPH_INTER_OCULAR, morph.baselineInterOcularDist)
            putFloat(KEY_MORPH_EYE_NOSE, morph.baselineEyeToNoseDist)
            putFloat(KEY_MORPH_ASPECT, morph.faceAspectRatio)
            putFloat(KEY_MORPH_ASYMMETRY, morph.eyeAsymmetryOffset)
            putFloat(KEY_MORPH_SCALE_X, morph.scaleX)
            putFloat(KEY_MORPH_SCALE_Y, morph.scaleY)
            putFloat(KEY_MORPH_OFFSET_X, morph.offsetX)
            putFloat(KEY_MORPH_OFFSET_Y, morph.offsetY)
            putFloat(KEY_MORPH_GAIN_X, morph.adaptiveGainX)
            putFloat(KEY_MORPH_GAIN_Y, morph.adaptiveGainY)
            putInt(KEY_MORPH_LEARNED_COUNT, morph.learnedInteractionsCount)

            apply()
        }
        _settingsFlow.value = updated
    }

    companion object {
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_SENSITIVITY_X = "sensitivity_x"
        private const val KEY_SENSITIVITY_Y = "sensitivity_y"
        private const val KEY_DEADZONE = "deadzone"
        private const val KEY_SMOOTHING = "smoothing"
        private const val KEY_DWELL_ENABLED = "dwell_enabled"
        private const val KEY_DWELL_DURATION = "dwell_duration"
        private const val KEY_BLINK_ENABLED = "blink_enabled"
        private const val KEY_SUSTAINED_BLINK_DURATION = "sustained_blink_duration"
        private const val KEY_SUSTAINED_WINK_DURATION = "sustained_wink_duration"
        private const val KEY_EYE_CLOSURE_THRESHOLD = "eye_closure_threshold"
        private const val KEY_BLINK_TRIGGER_TYPE = "blink_trigger_type"
        private const val KEY_CONTINUOUS_ADAPTATION = "continuous_adaptation"
        private const val KEY_CURSOR_SIZE = "cursor_size"
        private const val KEY_CURSOR_COLOR = "cursor_color"
        private const val KEY_SHOW_BUBBLE = "show_bubble"
        private const val KEY_SHOW_DOCK = "show_dock"
        private const val KEY_VOLUME_TRIGGER = "volume_trigger"
        private const val KEY_INVERT_X = "invert_x"
        private const val KEY_INVERT_Y = "invert_y"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_BOOT_START = "boot_start"

        private const val KEY_MORPH_CALIBRATED = "morph_calibrated"
        private const val KEY_MORPH_SCORE = "morph_score"
        private const val KEY_MORPH_INTER_OCULAR = "morph_inter_ocular"
        private const val KEY_MORPH_EYE_NOSE = "morph_eye_nose"
        private const val KEY_MORPH_ASPECT = "morph_aspect"
        private const val KEY_MORPH_ASYMMETRY = "morph_asymmetry"
        private const val KEY_MORPH_SCALE_X = "morph_scale_x"
        private const val KEY_MORPH_SCALE_Y = "morph_scale_y"
        private const val KEY_MORPH_OFFSET_X = "morph_offset_x"
        private const val KEY_MORPH_OFFSET_Y = "morph_offset_y"
        private const val KEY_MORPH_GAIN_X = "morph_gain_x"
        private const val KEY_MORPH_GAIN_Y = "morph_gain_y"
        private const val KEY_MORPH_LEARNED_COUNT = "morph_learned_count"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
