package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.ShiftStorage
import com.example.telemetry.LocationTelemetryWorker

class ShiftGuardApplication : Application() {

    lateinit var storage: ShiftStorage
        private set

    companion object {
        const val CHANNEL_BUBBLE_SERVICE = "channel_bubble_service"
        const val CHANNEL_GEOFENCE_ALERTS = "channel_geofence_alerts"
        const val CHANNEL_TELEMETRY = "channel_telemetry"
    }

    override fun onCreate() {
        super.onCreate()
        storage = ShiftStorage(this)
        createNotificationChannels()
        // Schedule periodic 1-hour telemetry polling
        try {
            LocationTelemetryWorker.scheduleHourlySync(this)
        } catch (e: Exception) {
            android.util.Log.w("ShiftGuardApp", "WorkManager schedule skipped in test/restricted environment", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_BUBBLE_SERVICE,
                "ShiftGuard Active Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent indicator for ShiftGuard Floating Bubbles and Job Timer"
            }

            val geofenceChannel = NotificationChannel(
                CHANNEL_GEOFENCE_ALERTS,
                "Shift & Geofence Guard",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for shift boundary and geofence breaches"
                enableVibration(true)
            }

            val telemetryChannel = NotificationChannel(
                CHANNEL_TELEMETRY,
                "Background Telemetry Sync",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Periodic and FCM-triggered location sync notifications"
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(geofenceChannel)
            notificationManager.createNotificationChannel(telemetryChannel)
        }
    }
}
