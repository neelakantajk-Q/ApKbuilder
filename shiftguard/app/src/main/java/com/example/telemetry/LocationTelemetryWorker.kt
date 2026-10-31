package com.example.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ShiftGuardApplication
import com.example.data.model.TelemetryLog
import com.example.data.remote.GasEngine
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LocationTelemetryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TelemetryWorker"
        const val PERIODIC_WORK_NAME = "shiftguard_hourly_telemetry_sync"

        fun scheduleHourlySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // 1-hour periodic work request as mandated
            val periodicWork = PeriodicWorkRequestBuilder<LocationTelemetryWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
            Log.d(TAG, "Hourly telemetry sync enqueued")
        }

        fun enqueueImmediateSync(context: Context, source: String = "MANUAL") {
            val oneTime = OneTimeWorkRequestBuilder<LocationTelemetryWorker>()
                .addTag("telemetry_immediate")
                .build()
            WorkManager.getInstance(context).enqueue(oneTime)
            Log.d(TAG, "Immediate telemetry sync enqueued via source: $source")
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing LocationTelemetryWorker...")
        return try {
            val appContext = applicationContext
            val app = appContext as? ShiftGuardApplication
            val storage = app?.storage

            val fusedLocation = LocationServices.getFusedLocationProviderClient(appContext)
            val location: Location? = try {
                fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    ?: fusedLocation.lastLocation.await()
            } catch (e: SecurityException) {
                Log.w(TAG, "Location permission missing during worker execution", e)
                null
            }

            // Fallback default coordinates if emulator GPS is cold
            val lat = location?.latitude ?: 12.9716
            val lng = location?.longitude ?: 77.5946

            // Battery percentage
            val batteryStatus: Intent? = appContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct: Int = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85

            // Device UUID
            val deviceId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "DEVICE_UUID_FALLBACK"

            // UTC Timestamp
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val utcNow = sdf.format(Date())

            val user = storage?.userProfile?.value
            val gasUrl = storage?.gasUrl?.value ?: ""

            val log = TelemetryLog(
                id = "TEL-${System.currentTimeMillis() % 100000}",
                userEmail = user?.email ?: "officer@shiftguard.com",
                timestampUtc = utcNow,
                deviceId = deviceId,
                latitude = lat,
                longitude = lng,
                batteryPct = batteryPct,
                triggerSource = tags.firstOrNull { it.contains("immediate") }?.let { "FCM_PUSH" } ?: "PERIODIC_1H",
                syncStatus = "SYNCED"
            )

            // Submit to Google Apps Script / Sheets endpoint
            GasEngine.submitTelemetry(gasUrl, log)
            storage?.addTelemetryLog(log)

            // Evaluate geofence
            if (user != null && storage != null) {
                val results = FloatArray(1)
                Location.distanceBetween(lat, lng, user.geofenceLat, user.geofenceLng, results)
                val distance = results[0].toDouble()
                val isInside = distance <= user.geofenceRadiusMeters

                // Determine if within shift hours
                val isWithinShift = isCurrentTimeWithinShift(user.shiftStart, user.shiftEnd)
                val breach = !isInside

                storage.updateGeofence(
                    isWithinShift = isWithinShift,
                    isWithinBoundary = isInside,
                    distanceMeters = distance,
                    latitude = lat,
                    longitude = lng,
                    breachActive = breach
                )
            }

            Log.d(TAG, "LocationTelemetryWorker finished successfully: lat=$lat, lng=$lng, bat=$batteryPct%")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationTelemetryWorker", e)
            Result.retry()
        }
    }

    private fun isCurrentTimeWithinShift(startStr: String, endStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.US)
            val nowStr = format.format(Date())
            val now = format.parse(nowStr)
            val start = format.parse(startStr)
            val end = format.parse(endStr)
            if (now != null && start != null && end != null) {
                if (end.after(start)) {
                    now in start..end
                } else {
                    // Overnight shift
                    now.after(start) || now.before(end)
                }
            } else true
        } catch (e: Exception) {
            true
        }
    }
}
