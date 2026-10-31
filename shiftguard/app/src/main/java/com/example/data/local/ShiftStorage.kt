package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ActiveJobState
import com.example.data.model.AttendanceRecord
import com.example.data.model.GeofenceState
import com.example.data.model.JobRecord
import com.example.data.model.TelemetryLog
import com.example.data.model.TravelPermission
import com.example.data.model.UserProfile
import com.example.data.remote.GasEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ShiftStorage(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shiftguard_secure_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _gasUrl = MutableStateFlow(GasEngine.CONCEALED_GAS_URL)
    val gasUrl: StateFlow<String> = _gasUrl.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory.asStateFlow()

    private val _jobHistory = MutableStateFlow<List<JobRecord>>(emptyList())
    val jobHistory: StateFlow<List<JobRecord>> = _jobHistory.asStateFlow()

    private val _telemetryLogs = MutableStateFlow<List<TelemetryLog>>(emptyList())
    val telemetryLogs: StateFlow<List<TelemetryLog>> = _telemetryLogs.asStateFlow()

    private val _travelPermissions = MutableStateFlow<List<TravelPermission>>(emptyList())
    val travelPermissions: StateFlow<List<TravelPermission>> = _travelPermissions.asStateFlow()

    private val _activeJob = MutableStateFlow(ActiveJobState())
    val activeJob: StateFlow<ActiveJobState> = _activeJob.asStateFlow()

    private val _geofenceState = MutableStateFlow(GeofenceState())
    val geofenceState: StateFlow<GeofenceState> = _geofenceState.asStateFlow()

    private val _isBubbleServiceRunning = MutableStateFlow(false)
    val isBubbleServiceRunning: StateFlow<Boolean> = _isBubbleServiceRunning.asStateFlow()

    // Midnight session re-authentication required state
    private val _isMidnightAuthRequired = MutableStateFlow(false)
    val isMidnightAuthRequired: StateFlow<Boolean> = _isMidnightAuthRequired.asStateFlow()

    init {
        loadFromPrefs()
        checkMidnightExpiry()
    }

    private fun loadFromPrefs() {
        val gas = prefs.getString("gas_url", "") ?: ""
        _gasUrl.value = if (gas.isNotBlank()) gas else GasEngine.CONCEALED_GAS_URL

        val userJson = prefs.getString("user_profile", null)
        if (userJson != null) {
            try {
                val j = JSONObject(userJson)
                _userProfile.value = UserProfile(
                    id = j.optString("id", "EMP-1042"),
                    email = j.optString("email", "officer@shiftguard.com"),
                    name = j.optString("name", "Vikram Sharma"),
                    role = j.optString("role", "Field Operations Officer"),
                    token = j.optString("token", "tok_demo"),
                    siteName = j.optString("siteName", "Bangalore Central HQ Hub"),
                    shiftStart = j.optString("shiftStart", "08:00"),
                    shiftEnd = j.optString("shiftEnd", "18:00"),
                    geofenceLat = j.optDouble("geofenceLat", 12.9716),
                    geofenceLng = j.optDouble("geofenceLng", 77.5946),
                    geofenceRadiusMeters = j.optDouble("geofenceRadiusMeters", 800.0),
                    loginTimestampMillis = j.optLong("loginTimestampMillis", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                _userProfile.value = GasEngine.DEFAULT_USERS.first()
            }
        } else {
            // Default initial profile for immediate user experience
            _userProfile.value = GasEngine.DEFAULT_USERS.first()
            saveUserProfile(GasEngine.DEFAULT_USERS.first())
        }

        // Load active job if saved
        val jobPlate = prefs.getString("active_job_plate", "") ?: ""
        val jobStart = prefs.getLong("active_job_start", 0L)
        if (jobPlate.isNotEmpty() && jobStart > 0L) {
            val elapsed = (System.currentTimeMillis() - jobStart) / 1000L
            _activeJob.value = ActiveJobState(
                isRunning = true,
                licensePlate = jobPlate,
                startTimeMillis = jobStart,
                elapsedSeconds = if (elapsed > 0) elapsed else 0
            )
        }

        loadAttendanceHistory()
        loadJobHistory()
        loadTelemetryLogs()
    }

    // Checks if the user session has crossed 12:00 AM (midnight)
    fun checkMidnightExpiry(): Boolean {
        val user = _userProfile.value ?: return false
        val loginTime = user.loginTimestampMillis
        if (loginTime <= 0L) return false

        val calNow = Calendar.getInstance()
        val calLogin = Calendar.getInstance().apply { timeInMillis = loginTime }

        // Expire if current calendar day is after login calendar day (e.g., crossed 12:00 AM)
        val crossedMidnight = (calNow.get(Calendar.YEAR) > calLogin.get(Calendar.YEAR)) ||
                (calNow.get(Calendar.YEAR) == calLogin.get(Calendar.YEAR) &&
                        calNow.get(Calendar.DAY_OF_YEAR) > calLogin.get(Calendar.DAY_OF_YEAR))

        if (crossedMidnight) {
            _isMidnightAuthRequired.value = true
        }
        return crossedMidnight
    }

    fun simulateMidnightExpiry(force: Boolean) {
        _isMidnightAuthRequired.value = force
    }

    fun completeMidnightReauth() {
        val current = _userProfile.value ?: return
        val updated = current.copy(loginTimestampMillis = System.currentTimeMillis())
        saveUserProfile(updated)
        _isMidnightAuthRequired.value = false
    }

    fun saveGasUrl(url: String) {
        _gasUrl.value = url.trim()
        prefs.edit().putString("gas_url", url.trim()).apply()
    }

    fun saveUserProfile(profile: UserProfile?) {
        _userProfile.value = profile
        if (profile == null) {
            prefs.edit().remove("user_profile").apply()
        } else {
            val j = JSONObject().apply {
                put("id", profile.id)
                put("email", profile.email)
                put("name", profile.name)
                put("role", profile.role)
                put("token", profile.token)
                put("siteName", profile.siteName)
                put("shiftStart", profile.shiftStart)
                put("shiftEnd", profile.shiftEnd)
                put("geofenceLat", profile.geofenceLat)
                put("geofenceLng", profile.geofenceLng)
                put("geofenceRadiusMeters", profile.geofenceRadiusMeters)
                put("loginTimestampMillis", profile.loginTimestampMillis)
            }
            prefs.edit().putString("user_profile", j.toString()).apply()
        }
    }

    // Dynamic site and boundary configuration without hardcoding
    fun updateSiteConfig(
        siteName: String,
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        shiftStart: String,
        shiftEnd: String
    ) {
        val current = _userProfile.value ?: GasEngine.DEFAULT_USERS.first()
        val updated = current.copy(
            siteName = siteName,
            geofenceLat = lat,
            geofenceLng = lng,
            geofenceRadiusMeters = radiusMeters,
            shiftStart = shiftStart,
            shiftEnd = shiftEnd
        )
        saveUserProfile(updated)
    }

    fun setBubbleServiceRunning(running: Boolean) {
        _isBubbleServiceRunning.value = running
    }

    fun startJob(licensePlate: String, startTimeMillis: Long = System.currentTimeMillis()) {
        _activeJob.value = ActiveJobState(
            isRunning = true,
            licensePlate = licensePlate,
            startTimeMillis = startTimeMillis,
            elapsedSeconds = 0L
        )
        prefs.edit()
            .putString("active_job_plate", licensePlate)
            .putLong("active_job_start", startTimeMillis)
            .apply()
    }

    fun updateJobElapsed(seconds: Long) {
        if (_activeJob.value.isRunning) {
            _activeJob.value = _activeJob.value.copy(elapsedSeconds = seconds)
        }
    }

    fun stopJob(documentProofUri: String = "", documentProofBase64: String = ""): JobRecord? {
        val current = _activeJob.value
        if (!current.isRunning) return null

        val stopTime = System.currentTimeMillis()
        val duration = (stopTime - current.startTimeMillis) / 1000L
        val record = JobRecord(
            id = "JOB-${System.currentTimeMillis() % 100000}",
            userEmail = _userProfile.value?.email ?: "user@shiftguard.com",
            licensePlate = current.licensePlate,
            startTimeMillis = current.startTimeMillis,
            stopTimeMillis = stopTime,
            durationSeconds = duration,
            documentProofUri = documentProofUri,
            documentProofBase64 = documentProofBase64,
            syncStatus = "SYNCED"
        )

        // Clear active
        _activeJob.value = ActiveJobState()
        prefs.edit().remove("active_job_plate").remove("active_job_start").apply()

        // Append to history
        val list = listOf(record) + _jobHistory.value
        _jobHistory.value = list
        saveJobHistory(list)
        return record
    }

    fun addAttendanceRecord(record: AttendanceRecord) {
        val list = listOf(record) + _attendanceHistory.value
        _attendanceHistory.value = list
        saveAttendanceHistory(list)
    }

    fun addTelemetryLog(log: TelemetryLog) {
        val list = (listOf(log) + _telemetryLogs.value).take(50) // keep last 50
        _telemetryLogs.value = list
        saveTelemetryLogs(list)
    }

    fun addTravelPermission(permission: TravelPermission) {
        val list = listOf(permission) + _travelPermissions.value
        _travelPermissions.value = list
    }

    fun setTransitMode(destination: String, active: Boolean) {
        val cur = _geofenceState.value
        _geofenceState.value = cur.copy(
            isTransitActive = active,
            transitDestination = if (active) destination else "",
            breachActive = if (active) false else cur.breachActive
        )
    }

    fun logoutAndCloseShift() {
        _isBubbleServiceRunning.value = false
        _geofenceState.value = GeofenceState(
            isWithinShift = false,
            isWithinBoundary = false,
            breachActive = false
        )
        _activeJob.value = ActiveJobState()
        prefs.edit().remove("active_job_plate").remove("active_job_start").apply()
        saveUserProfile(null)
    }

    fun updateGeofence(
        isWithinShift: Boolean,
        isWithinBoundary: Boolean,
        distanceMeters: Double,
        latitude: Double,
        longitude: Double,
        breachActive: Boolean
    ) {
        // If user is in authorized transit, suppress breach alert
        val isTransit = _geofenceState.value.isTransitActive
        val effectiveBreach = if (isTransit) false else breachActive

        _geofenceState.value = _geofenceState.value.copy(
            isWithinShift = isWithinShift,
            isWithinBoundary = isWithinBoundary,
            currentDistanceMeters = distanceMeters,
            currentLatitude = latitude,
            currentLongitude = longitude,
            lastCheckedTimestamp = System.currentTimeMillis(),
            breachActive = effectiveBreach
        )
    }

    private fun saveAttendanceHistory(list: List<AttendanceRecord>) {
        val array = JSONArray()
        list.take(20).forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("userEmail", it.userEmail)
                put("timestampUtc", it.timestampUtc)
                put("deviceId", it.deviceId)
                put("latitude", it.latitude)
                put("longitude", it.longitude)
                put("address", it.address)
                put("photoUriOrBase64", it.photoUriOrBase64)
                put("syncStatus", it.syncStatus)
            })
        }
        prefs.edit().putString("attendance_list", array.toString()).apply()
    }

    private fun loadAttendanceHistory() {
        val str = prefs.getString("attendance_list", null)
        if (str != null) {
            try {
                val array = JSONArray(str)
                val list = mutableListOf<AttendanceRecord>()
                for (i in 0 until array.length()) {
                    val j = array.getJSONObject(i)
                    list.add(
                        AttendanceRecord(
                            id = j.optString("id"),
                            userEmail = j.optString("userEmail"),
                            timestampUtc = j.optString("timestampUtc"),
                            deviceId = j.optString("deviceId"),
                            latitude = j.optDouble("latitude"),
                            longitude = j.optDouble("longitude"),
                            address = j.optString("address"),
                            photoUriOrBase64 = j.optString("photoUriOrBase64"),
                            syncStatus = j.optString("syncStatus")
                        )
                    )
                }
                _attendanceHistory.value = list
                return
            } catch (e: Exception) {}
        }
        // Initial sample
        _attendanceHistory.value = listOf(
            AttendanceRecord(
                id = "ATT-101",
                userEmail = "officer@shiftguard.com",
                timestampUtc = "2026-10-30T08:05:00Z",
                deviceId = "ANDROID_ID_8831",
                latitude = 12.9716,
                longitude = 77.5946,
                address = "Bangalore Central Hub, MG Road, Karnataka",
                photoUriOrBase64 = "",
                syncStatus = "SYNCED"
            )
        )
    }

    private fun saveJobHistory(list: List<JobRecord>) {
        val array = JSONArray()
        list.take(20).forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("userEmail", it.userEmail)
                put("licensePlate", it.licensePlate)
                put("startTimeMillis", it.startTimeMillis)
                put("stopTimeMillis", it.stopTimeMillis)
                put("durationSeconds", it.durationSeconds)
                put("documentProofUri", it.documentProofUri)
                put("documentProofBase64", it.documentProofBase64)
                put("syncStatus", it.syncStatus)
            })
        }
        prefs.edit().putString("jobs_list", array.toString()).apply()
    }

    private fun loadJobHistory() {
        val str = prefs.getString("jobs_list", null)
        if (str != null) {
            try {
                val array = JSONArray(str)
                val list = mutableListOf<JobRecord>()
                for (i in 0 until array.length()) {
                    val j = array.getJSONObject(i)
                    list.add(
                        JobRecord(
                            id = j.optString("id"),
                            userEmail = j.optString("userEmail"),
                            licensePlate = j.optString("licensePlate"),
                            startTimeMillis = j.optLong("startTimeMillis"),
                            stopTimeMillis = j.optLong("stopTimeMillis"),
                            durationSeconds = j.optLong("durationSeconds"),
                            documentProofUri = j.optString("documentProofUri"),
                            documentProofBase64 = j.optString("documentProofBase64"),
                            syncStatus = j.optString("syncStatus")
                        )
                    )
                }
                _jobHistory.value = list
                return
            } catch (e: Exception) {}
        }
        _jobHistory.value = listOf(
            JobRecord(
                id = "JOB-8921",
                userEmail = "officer@shiftguard.com",
                licensePlate = "KA-02-JH-2234",
                startTimeMillis = System.currentTimeMillis() - 7200000,
                stopTimeMillis = System.currentTimeMillis() - 3600000,
                durationSeconds = 3600,
                documentProofUri = "",
                syncStatus = "SYNCED"
            )
        )
    }

    private fun saveTelemetryLogs(list: List<TelemetryLog>) {
        val array = JSONArray()
        list.take(20).forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("userEmail", it.userEmail)
                put("timestampUtc", it.timestampUtc)
                put("deviceId", it.deviceId)
                put("latitude", it.latitude)
                put("longitude", it.longitude)
                put("batteryPct", it.batteryPct)
                put("triggerSource", it.triggerSource)
                put("syncStatus", it.syncStatus)
            })
        }
        prefs.edit().putString("telemetry_list", array.toString()).apply()
    }

    private fun loadTelemetryLogs() {
        val str = prefs.getString("telemetry_list", null)
        if (str != null) {
            try {
                val array = JSONArray(str)
                val list = mutableListOf<TelemetryLog>()
                for (i in 0 until array.length()) {
                    val j = array.getJSONObject(i)
                    list.add(
                        TelemetryLog(
                            id = j.optString("id"),
                            userEmail = j.optString("userEmail"),
                            timestampUtc = j.optString("timestampUtc"),
                            deviceId = j.optString("deviceId"),
                            latitude = j.optDouble("latitude"),
                            longitude = j.optDouble("longitude"),
                            batteryPct = j.optInt("batteryPct"),
                            triggerSource = j.optString("triggerSource"),
                            syncStatus = j.optString("syncStatus")
                        )
                    )
                }
                _telemetryLogs.value = list
                return
            } catch (e: Exception) {}
        }
        _telemetryLogs.value = listOf(
            TelemetryLog(
                id = "TEL-901",
                userEmail = "officer@shiftguard.com",
                timestampUtc = "2026-10-30T09:00:00Z",
                deviceId = "ANDROID_ID_8831",
                latitude = 12.9716,
                longitude = 77.5946,
                batteryPct = 94,
                triggerSource = "PERIODIC_1H",
                syncStatus = "SYNCED"
            )
        )
    }
}
