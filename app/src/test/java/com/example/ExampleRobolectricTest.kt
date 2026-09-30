package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.model.BlinkTriggerType
import com.example.model.CalibrationPointSample
import com.example.model.MorphologicalProfile
import com.example.tracking.GazeTrackerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EyeCursor", appName)
  }

  @Test
  fun `settings repository loads default settings and saves updates`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = SettingsRepository.getInstance(context)
    val initialSettings = repo.getSettings()
    assertNotNull(initialSettings)
    assertTrue(initialSettings.dwellClickEnabled)
    assertTrue(initialSettings.blinkClickEnabled)
    assertTrue(initialSettings.continuousAdaptationEnabled)
    assertEquals(400L, initialSettings.sustainedBlinkDurationMs)
    assertEquals(320L, initialSettings.sustainedWinkDurationMs)
    assertEquals(BlinkTriggerType.BOTH_OR_WINK, initialSettings.blinkTriggerType)

    repo.updateSettings {
      it.copy(
        sensitivityX = 3.2f,
        sustainedBlinkDurationMs = 550L,
        blinkTriggerType = BlinkTriggerType.SUSTAINED_BLINK_ONLY,
        morphologicalProfile = MorphologicalProfile(
          isCalibrated = true,
          calibrationScorePercent = 98,
          baselineInterOcularDist = 64f,
          baselineEyeToNoseDist = 58f
        )
      )
    }

    val updated = repo.getSettings()
    assertEquals(3.2f, updated.sensitivityX, 0.001f)
    assertEquals(550L, updated.sustainedBlinkDurationMs)
    assertEquals(BlinkTriggerType.SUSTAINED_BLINK_ONLY, updated.blinkTriggerType)
    assertTrue(updated.morphologicalProfile.isCalibrated)
    assertEquals(98, updated.morphologicalProfile.calibrationScorePercent)
    assertEquals(64f, updated.morphologicalProfile.baselineInterOcularDist, 0.01f)
  }

  @Test
  fun `gaze tracker engine computes morphological calibration profile from 5 points`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = GazeTrackerEngine(
      context = context,
      screenWidth = 1080,
      screenHeight = 2340,
      onCursorUpdate = { _, _, _, _, _ -> },
      onTriggerAction = { _, _, _, _ -> },
      onDiagnosticsUpdate = {}
    )

    val samples = listOf(
      CalibrationPointSample(0, 0.50f, 0.50f, yaw = 0f, pitch = 8f, eyeCenterNormX = 0f, eyeCenterNormY = 0f, interOcularDist = 62f, eyeToNoseDist = 55f),
      CalibrationPointSample(1, 0.15f, 0.15f, yaw = 12f, pitch = 16f, eyeCenterNormX = -0.3f, eyeCenterNormY = 0.2f, interOcularDist = 61f, eyeToNoseDist = 54f),
      CalibrationPointSample(2, 0.85f, 0.15f, yaw = -12f, pitch = 16f, eyeCenterNormX = 0.3f, eyeCenterNormY = 0.2f, interOcularDist = 63f, eyeToNoseDist = 56f),
      CalibrationPointSample(3, 0.15f, 0.85f, yaw = 12f, pitch = -2f, eyeCenterNormX = -0.3f, eyeCenterNormY = -0.2f, interOcularDist = 60f, eyeToNoseDist = 53f),
      CalibrationPointSample(4, 0.85f, 0.85f, yaw = -12f, pitch = -2f, eyeCenterNormX = 0.3f, eyeCenterNormY = -0.2f, interOcularDist = 62f, eyeToNoseDist = 55f)
    )

    val profile = engine.calculateMorphologicalProfile(samples)
    assertTrue(profile.isCalibrated)
    assertTrue(profile.calibrationScorePercent >= 85)
    assertTrue(profile.baselineInterOcularDist > 50f)
    assertTrue(profile.scaleX > 0.5f)
    assertTrue(profile.scaleY > 0.5f)
  }
}
