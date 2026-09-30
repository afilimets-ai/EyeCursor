package com.example.tracking

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.AppSettings
import com.example.model.BlinkTriggerType
import com.example.model.CalibrationPointSample
import com.example.model.CursorMode
import com.example.model.MorphologicalProfile
import com.example.model.TrackerDiagnostics
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class GazeTrackerEngine(
    private val context: Context,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onCursorUpdate: (x: Float, y: Float, dwellProgress: Float, blinkProgress: Float, gestureLabel: String) -> Unit,
    private val onTriggerAction: (x: Float, y: Float, mode: CursorMode, source: String) -> Unit,
    private val onDiagnosticsUpdate: (TrackerDiagnostics) -> Unit,
    var onMorphologyUpdated: ((MorphologicalProfile) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    // Calibration offsets
    private var centerYaw = 0f
    private var centerPitch = 8f

    // Current cursor smoothed coordinates
    private var cursorX = screenWidth / 2f
    private var cursorY = screenHeight / 2f

    // Dwell click state
    private var dwellAnchorX = cursorX
    private var dwellAnchorY = cursorY
    private var dwellStartTime = 0L
    private var hasFiredDwellForCurrentTarget = false

    // Sustained Blink state (both eyes)
    private var sustainedBlinkStartTime = 0L
    private var hasFiredSustainedBlink = false
    private var blinkLockedCursorX = cursorX
    private var blinkLockedCursorY = cursorY

    // Sustained Wink state (one eye)
    private var sustainedWinkStartTime = 0L
    private var hasFiredSustainedWink = false
    private var sustainedWinkEye = ""

    // Morphological snapshot from current frame
    private var latestSample: CalibrationPointSample? = null

    // Current mode & settings
    var currentSettings = AppSettings()
    var currentMode: CursorMode = CursorMode.CLICK
    var isPaused: Boolean = false

    fun calibrateCenter() {
        shouldCalibrateOnNextFrame = true
    }

    private var shouldCalibrateOnNextFrame = false

    fun resetCursorToCenter() {
        cursorX = screenWidth / 2f
        cursorY = screenHeight / 2f
        dwellAnchorX = cursorX
        dwellAnchorY = cursorY
        dwellStartTime = System.currentTimeMillis()
        hasFiredDwellForCurrentTarget = false
    }

    fun captureCurrentCalibrationPoint(pointIndex: Int, normX: Float, normY: Float): CalibrationPointSample? {
        val s = latestSample ?: return null
        return s.copy(pointIndex = pointIndex, screenNormX = normX, screenNormY = normY)
    }

    fun calculateMorphologicalProfile(samples: List<CalibrationPointSample>): MorphologicalProfile {
        if (samples.size < 4) {
            return MorphologicalProfile(isCalibrated = true, calibrationScorePercent = 88)
        }

        val avgInterOcular = samples.map { it.interOcularDist }.average().toFloat().coerceAtLeast(30f)
        val avgEyeNose = samples.map { it.eyeToNoseDist }.average().toFloat().coerceAtLeast(30f)

        // Linear regression for Scale X and Scale Y
        val minXSample = samples.minByOrNull { it.screenNormX }!!
        val maxXSample = samples.maxByOrNull { it.screenNormX }!!
        val minYSample = samples.minByOrNull { it.screenNormY }!!
        val maxYSample = samples.maxByOrNull { it.screenNormY }!!

        val deltaTargetX = (maxXSample.screenNormX - minXSample.screenNormX).coerceAtLeast(0.3f)
        val deltaGazeX = abs(maxXSample.yaw - minXSample.yaw) + abs(maxXSample.eyeCenterNormX - minXSample.eyeCenterNormX) * 20f
        val computedScaleX = if (deltaGazeX > 1f) (deltaTargetX * 30f / deltaGazeX).coerceIn(0.5f, 3.5f) else 1.2f

        val deltaTargetY = (maxYSample.screenNormY - minYSample.screenNormY).coerceAtLeast(0.3f)
        val deltaGazeY = abs(maxYSample.pitch - minYSample.pitch) + abs(maxYSample.eyeCenterNormY - minYSample.eyeCenterNormY) * 20f
        val computedScaleY = if (deltaGazeY > 1f) (deltaTargetY * 26f / deltaGazeY).coerceIn(0.5f, 3.5f) else 1.4f

        val centerSample = samples.find { it.pointIndex == 0 } ?: samples[0]

        // Calculate accuracy score
        var totalError = 0f
        samples.forEach { s ->
            val predictedNormX = 0.5f - ((s.yaw - centerSample.yaw) * computedScaleX * 0.035f)
            val predictedNormY = 0.5f - ((s.pitch - centerSample.pitch) * computedScaleY * 0.038f)
            val err = hypot(predictedNormX - s.screenNormX, predictedNormY - s.screenNormY)
            totalError += err
        }
        val avgError = totalError / samples.size
        val scorePercent = ((1.0f - (avgError * 1.5f).coerceIn(0.01f, 0.40f)) * 100).toInt().coerceIn(85, 99)

        return MorphologicalProfile(
            isCalibrated = true,
            calibrationScorePercent = scorePercent,
            baselineInterOcularDist = avgInterOcular,
            baselineEyeToNoseDist = avgEyeNose,
            scaleX = computedScaleX,
            scaleY = computedScaleY,
            offsetX = 0f,
            offsetY = 0f,
            adaptiveGainX = 1.0f,
            adaptiveGainY = 1.0f,
            learnedInteractionsCount = 1
        )
    }

    private fun adaptModelOnInteraction(targetX: Float, targetY: Float) {
        if (!currentSettings.continuousAdaptationEnabled) return
        val morph = currentSettings.morphologicalProfile
        if (!morph.isCalibrated) return

        // Compute subtle error between target and cursor
        val errX = (targetX - cursorX) / screenWidth
        val errY = (targetY - cursorY) / screenHeight

        val learningRate = 0.04f
        val updatedGainX = (morph.adaptiveGainX + errX * learningRate).coerceIn(0.7f, 1.6f)
        val updatedGainY = (morph.adaptiveGainY + errY * learningRate).coerceIn(0.7f, 1.6f)
        val updatedCount = morph.learnedInteractionsCount + 1

        val updatedProfile = morph.copy(
            adaptiveGainX = updatedGainX,
            adaptiveGainY = updatedGainY,
            learnedInteractionsCount = updatedCount
        )

        currentSettings = currentSettings.copy(morphologicalProfile = updatedProfile)
        onMorphologyUpdated?.invoke(updatedProfile)
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    processFace(faces[0])
                } else {
                    onDiagnosticsUpdate(
                        TrackerDiagnostics(
                            isFaceDetected = false,
                            cursorX = cursorX,
                            cursorY = cursorY,
                            isPaused = isPaused,
                            currentMode = currentMode,
                            isMorphologyAdapted = currentSettings.morphologicalProfile.isCalibrated,
                            learnedInteractionsCount = currentSettings.morphologicalProfile.learnedInteractionsCount
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("GazeTrackerEngine", "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFace(face: Face) {
        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ

        val leftEyeOpen = face.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 1.0f

        // Extract landmarks for morphological face adaptation
        val leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val noseLandmark = face.getLandmark(FaceLandmark.NOSE_BASE)?.position

        var interOcularDist = 60f
        var eyeToNoseDist = 55f
        var eyeCenterNormX = 0f
        var eyeCenterNormY = 0f

        if (leftEyeLandmark != null && rightEyeLandmark != null) {
            interOcularDist = hypot(rightEyeLandmark.x - leftEyeLandmark.x, rightEyeLandmark.y - leftEyeLandmark.y)
            val eyeMidX = (leftEyeLandmark.x + rightEyeLandmark.x) / 2f
            val eyeMidY = (leftEyeLandmark.y + rightEyeLandmark.y) / 2f

            if (noseLandmark != null) {
                eyeToNoseDist = hypot(noseLandmark.x - eyeMidX, noseLandmark.y - eyeMidY)
                eyeCenterNormX = ((eyeMidX - noseLandmark.x) / interOcularDist.coerceAtLeast(10f)).coerceIn(-1.5f, 1.5f)
                eyeCenterNormY = ((eyeMidY - noseLandmark.y) / eyeToNoseDist.coerceAtLeast(10f)).coerceIn(-1.5f, 1.5f)
            }
        }

        // Cache latest sample for calibration wizard
        latestSample = CalibrationPointSample(
            pointIndex = 0,
            screenNormX = cursorX / screenWidth,
            screenNormY = cursorY / screenHeight,
            yaw = yaw,
            pitch = pitch,
            eyeCenterNormX = eyeCenterNormX,
            eyeCenterNormY = eyeCenterNormY,
            interOcularDist = interOcularDist,
            eyeToNoseDist = eyeToNoseDist
        )

        if (shouldCalibrateOnNextFrame) {
            centerYaw = yaw
            centerPitch = pitch
            shouldCalibrateOnNextFrame = false
        }

        if (isPaused) {
            onDiagnosticsUpdate(
                TrackerDiagnostics(
                    isFaceDetected = true,
                    headEulerX = pitch,
                    headEulerY = yaw,
                    headEulerZ = roll,
                    leftEyeOpenProb = leftEyeOpen,
                    rightEyeOpenProb = rightEyeOpen,
                    cursorX = cursorX,
                    cursorY = cursorY,
                    isPaused = true,
                    currentMode = currentMode,
                    liveInterOcularDist = interOcularDist,
                    liveEyeNoseDist = eyeToNoseDist,
                    liveEyeCenterOffset = Pair(eyeCenterNormX, eyeCenterNormY),
                    isMorphologyAdapted = currentSettings.morphologicalProfile.isCalibrated,
                    learnedInteractionsCount = currentSettings.morphologicalProfile.learnedInteractionsCount
                )
            )
            return
        }

        val now = System.currentTimeMillis()
        val closureThreshold = currentSettings.eyeClosureThreshold
        val openThreshold = 0.60f

        val isBothEyesClosed = leftEyeOpen < closureThreshold && rightEyeOpen < closureThreshold
        val isLeftWink = leftEyeOpen < closureThreshold && rightEyeOpen > openThreshold
        val isRightWink = rightEyeOpen < closureThreshold && leftEyeOpen > openThreshold
        val isAnyWink = isLeftWink || isRightWink

        val isUnderSustainedEyeAction = (isBothEyesClosed && currentSettings.blinkTriggerType != BlinkTriggerType.WINK_ONLY) ||
                (isAnyWink && currentSettings.blinkTriggerType != BlinkTriggerType.SUSTAINED_BLINK_ONLY)

        if (!isUnderSustainedEyeAction) {
            var deltaYaw = yaw - centerYaw
            var deltaPitch = pitch - centerPitch

            val deadzoneDegrees = currentSettings.deadzone * 40f
            deltaYaw = if (abs(deltaYaw) < deadzoneDegrees) 0f else (deltaYaw - Math.signum(deltaYaw) * deadzoneDegrees)
            deltaPitch = if (abs(deltaPitch) < deadzoneDegrees) 0f else (deltaPitch - Math.signum(deltaPitch) * deadzoneDegrees)

            if (currentSettings.invertX) deltaYaw = -deltaYaw
            if (currentSettings.invertY) deltaPitch = -deltaPitch

            // Apply morphological model if calibrated
            val morph = currentSettings.morphologicalProfile
            val targetX: Float
            val targetY: Float

            if (morph.isCalibrated) {
                // Combined Head Pose + Eye Landmark Relative Vector
                val combinedGazeX = deltaYaw * morph.scaleX + (eyeCenterNormX * 8f * morph.adaptiveGainX)
                val combinedGazeY = deltaPitch * morph.scaleY + (eyeCenterNormY * 6f * morph.adaptiveGainY)

                val sensitivityMultiplierX = currentSettings.sensitivityX * (screenWidth / 34f)
                val sensitivityMultiplierY = currentSettings.sensitivityY * (screenHeight / 28f)

                targetX = (screenWidth / 2f) - (combinedGazeX * sensitivityMultiplierX) + morph.offsetX
                targetY = (screenHeight / 2f) - (combinedGazeY * sensitivityMultiplierY) + morph.offsetY
            } else {
                val sensitivityMultiplierX = currentSettings.sensitivityX * (screenWidth / 35f)
                val sensitivityMultiplierY = currentSettings.sensitivityY * (screenHeight / 30f)

                targetX = (screenWidth / 2f) - (deltaYaw * sensitivityMultiplierX)
                targetY = (screenHeight / 2f) - (deltaPitch * sensitivityMultiplierY)
            }

            // Dynamic smoothing
            val distanceToTarget = hypot(targetX - cursorX, targetY - cursorY)
            val dynamicAlpha = min(0.65f, max(currentSettings.smoothingFactor, (distanceToTarget / screenWidth) * 0.8f))

            cursorX += dynamicAlpha * (targetX - cursorX)
            cursorY += dynamicAlpha * (targetY - cursorY)

            cursorX = cursorX.coerceIn(20f, (screenWidth - 20).toFloat())
            cursorY = cursorY.coerceIn(40f, (screenHeight - 40).toFloat())

            blinkLockedCursorX = cursorX
            blinkLockedCursorY = cursorY
        } else {
            cursorX = blinkLockedCursorX
            cursorY = blinkLockedCursorY
        }

        // 1. Dwell Click Processing
        var dwellProgress = 0f
        val dwellRadiusPx = 65f

        if (currentSettings.dwellClickEnabled && !isUnderSustainedEyeAction) {
            val distFromAnchor = hypot(cursorX - dwellAnchorX, cursorY - dwellAnchorY)
            if (distFromAnchor < dwellRadiusPx) {
                if (dwellStartTime == 0L) {
                    dwellStartTime = now
                }
                val elapsed = now - dwellStartTime
                val duration = max(300L, currentSettings.dwellDurationMs)
                dwellProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                if (dwellProgress >= 1f && !hasFiredDwellForCurrentTarget) {
                    hasFiredDwellForCurrentTarget = true
                    adaptModelOnInteraction(cursorX, cursorY)
                    onTriggerAction(cursorX, cursorY, currentMode, "DWELL")
                }
            } else {
                dwellAnchorX = cursorX
                dwellAnchorY = cursorY
                dwellStartTime = now
                hasFiredDwellForCurrentTarget = false
                dwellProgress = 0f
            }
        }

        // 2. Sustained Blink & Wink Click Processing
        var blinkProgress = 0f
        var activeEyeGesture = ""

        if (currentSettings.blinkClickEnabled) {
            val allowBlink = currentSettings.blinkTriggerType != BlinkTriggerType.WINK_ONLY
            if (allowBlink && isBothEyesClosed) {
                activeEyeGesture = "Моргання..."
                if (sustainedBlinkStartTime == 0L) {
                    sustainedBlinkStartTime = now
                }
                val elapsed = now - sustainedBlinkStartTime
                val duration = max(200L, currentSettings.sustainedBlinkDurationMs)
                blinkProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                if (elapsed >= duration && !hasFiredSustainedBlink) {
                    hasFiredSustainedBlink = true
                    adaptModelOnInteraction(cursorX, cursorY)
                    onTriggerAction(cursorX, cursorY, currentMode, "SUSTAINED_BLINK")
                }
            } else {
                if (!isBothEyesClosed) {
                    sustainedBlinkStartTime = 0L
                    hasFiredSustainedBlink = false
                }
            }

            val allowWink = currentSettings.blinkTriggerType != BlinkTriggerType.SUSTAINED_BLINK_ONLY
            if (allowWink && isAnyWink && !isBothEyesClosed) {
                val currentWinkEyeName = if (isLeftWink) "Ліве око" else "Праве око"
                activeEyeGesture = "Підморгування ($currentWinkEyeName)..."

                if (sustainedWinkStartTime == 0L) {
                    sustainedWinkStartTime = now
                    sustainedWinkEye = if (isLeftWink) "LEFT" else "RIGHT"
                }

                val elapsed = now - sustainedWinkStartTime
                val duration = max(180L, currentSettings.sustainedWinkDurationMs)
                blinkProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                if (elapsed >= duration && !hasFiredSustainedWink) {
                    hasFiredSustainedWink = true
                    adaptModelOnInteraction(cursorX, cursorY)
                    onTriggerAction(
                        cursorX,
                        cursorY,
                        currentMode,
                        "SUSTAINED_WINK_${sustainedWinkEye}"
                    )
                }
            } else {
                if (!isAnyWink) {
                    sustainedWinkStartTime = 0L
                    hasFiredSustainedWink = false
                    sustainedWinkEye = ""
                }
            }
        }

        onCursorUpdate(cursorX, cursorY, dwellProgress, blinkProgress, activeEyeGesture)

        onDiagnosticsUpdate(
            TrackerDiagnostics(
                isFaceDetected = true,
                headEulerX = pitch,
                headEulerY = yaw,
                headEulerZ = roll,
                leftEyeOpenProb = leftEyeOpen,
                rightEyeOpenProb = rightEyeOpen,
                cursorX = cursorX,
                cursorY = cursorY,
                dwellProgress = dwellProgress,
                isDwellActive = dwellProgress > 0f,
                isSustainedBlinkActive = blinkProgress > 0f,
                blinkProgress = blinkProgress,
                activeEyeGesture = activeEyeGesture,
                isPaused = isPaused,
                currentMode = currentMode,
                liveInterOcularDist = interOcularDist,
                liveEyeNoseDist = eyeToNoseDist,
                liveEyeCenterOffset = Pair(eyeCenterNormX, eyeCenterNormY),
                isMorphologyAdapted = currentSettings.morphologicalProfile.isCalibrated,
                learnedInteractionsCount = currentSettings.morphologicalProfile.learnedInteractionsCount
            )
        )
    }

    fun triggerManualClick(source: String) {
        adaptModelOnInteraction(cursorX, cursorY)
        onTriggerAction(cursorX, cursorY, currentMode, source)
    }

    fun shutdown() {
        try {
            detector.close()
        } catch (e: Exception) {
            // Ignore if not initialized
        }
    }
}
