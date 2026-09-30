package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.data.SettingsRepository
import com.example.service.EyeTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val repo = SettingsRepository.getInstance(context)
            val settings = repo.getSettings()

            if (settings.autoStartOnBoot && settings.isEnabled && Settings.canDrawOverlays(context)) {
                EyeTrackingService.startService(context)
            }
        }
    }
}
