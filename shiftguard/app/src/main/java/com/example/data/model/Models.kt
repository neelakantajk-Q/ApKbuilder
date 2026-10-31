package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String = "EMP-1042",
    val email: String = "officer@shiftguard.com",
    val name: String = "Vikram Sharma",
    val role: String = "Field Operations Officer",
    val token: String = "sg_tok_991823",
    val siteName: String = "Bangalore Central HQ Hub",
    val shiftStart: String = "08:00",
    val shiftEnd: String = "18:00",
    val geofenceLat: Double = 12.9716, // Default site center
    val geofenceLng: Double = 77.5946,
    val geofenceRadiusMeters: Double = 800.0,
    val loginTimestampMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    val id: String,
    val userEmail: String,
    val timestampUtc: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val photoUriOrBase64: String = "",
    val syncStatus: String = "SYNCED" // PENDING, SYNCED, FAILED
)

@JsonClass(generateAdapter = true)
data class JobRecord(
    val id: String,
    val userEmail: String,
    val licensePlate: String,
    val startTimeMillis: Long,
    val stopTimeMillis: Long,
    val durationSeconds: Long,
    val documentProofUri: String = "",
    val documentProofBase64: String = "",
    val syncStatus: String = "SYNCED"
)

@JsonClass(generateAdapter = true)
data class TelemetryLog(
    val id: String,
    val userEmail: String,
    val timestampUtc: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val batteryPct: Int,
    val triggerSource: String = "PERIODIC_1H", // PERIODIC_1H, FCM_PUSH, MANUAL
    val syncStatus: String = "SYNCED"
)

@JsonClass(generateAdapter = true)
data class TravelPermission(
    val id: String,
    val userEmail: String,
    val timestampUtc: String,
    val reason: String,
    val destinationLocation: String = "North Depot Cargo Hub",
    val transitMinutes: Int = 45,
    val status: String = "APPROVED", // REQUESTED, APPROVED, REJECTED
    val latitude: Double,
    val longitude: Double
)

data class GeofenceState(
    val isWithinShift: Boolean = true,
    val isWithinBoundary: Boolean = true,
    val currentDistanceMeters: Double = 120.0,
    val currentLatitude: Double = 12.9716,
    val currentLongitude: Double = 77.5946,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val breachActive: Boolean = false,
    val isTransitActive: Boolean = false,
    val transitDestination: String = ""
)

data class ActiveJobState(
    val isRunning: Boolean = false,
    val licensePlate: String = "",
    val startTimeMillis: Long = 0L,
    val elapsedSeconds: Long = 0L
)
