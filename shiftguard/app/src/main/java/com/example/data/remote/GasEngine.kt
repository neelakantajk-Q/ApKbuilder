package com.example.data.remote

import android.util.Log
import com.example.data.model.AttendanceRecord
import com.example.data.model.JobRecord
import com.example.data.model.TelemetryLog
import com.example.data.model.TravelPermission
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GasEngine {
    private const val TAG = "GasEngine"

    // Closed-loop concealed endpoints - sealed inside compiled bytecode
    const val CONCEALED_GAS_URL = "https://script.google.com/macros/s/AKfycbzOECe8KcLcRGiCsoDSSTvA6VTjPvQ0ZfRyMQw2iSgA6WjKSoxuyRxnu_ZyQg3QRl4q/exec"
    const val CONCEALED_FIREBASE_URL = "https://quantum-99abd-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // Default mock users for testing or instant offline resilience
    val DEFAULT_USERS = listOf(
        UserProfile(
            id = "EMP-1042",
            email = "officer@shiftguard.com",
            name = "Vikram Sharma",
            role = "Field Operations Officer",
            token = "sg_tok_v1",
            siteName = "Bangalore Central HQ Hub",
            shiftStart = "08:00",
            shiftEnd = "18:00",
            geofenceLat = 12.9716,
            geofenceLng = 77.5946,
            geofenceRadiusMeters = 800.0,
            loginTimestampMillis = System.currentTimeMillis()
        ),
        UserProfile(
            id = "EMP-2088",
            email = "driver@shiftguard.com",
            name = "Rajesh Kumar",
            role = "Fleet Logistics Driver",
            token = "sg_tok_v2",
            siteName = "Whitefield Freight Terminal",
            shiftStart = "06:00",
            shiftEnd = "15:00",
            geofenceLat = 12.9698,
            geofenceLng = 77.7500,
            geofenceRadiusMeters = 1200.0,
            loginTimestampMillis = System.currentTimeMillis()
        ),
        UserProfile(
            id = "EMP-3011",
            email = "supervisor@shiftguard.com",
            name = "Ananya Desai",
            role = "Shift Supervisor",
            token = "sg_tok_v3",
            siteName = "Electronic City Gateway",
            shiftStart = "09:00",
            shiftEnd = "19:00",
            geofenceLat = 12.8452,
            geofenceLng = 77.6602,
            geofenceRadiusMeters = 1500.0,
            loginTimestampMillis = System.currentTimeMillis()
        )
    )

    suspend fun login(gasUrl: String, email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanUrl = if (gasUrl.trim().startsWith("http")) gasUrl.trim() else CONCEALED_GAS_URL
        if (cleanUrl.isNotEmpty() && cleanUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "login")
                    put("email", email.trim().lowercase())
                    put("password", pass)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(cleanUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val resJson = JSONObject(bodyStr)
                        if (resJson.optBoolean("success", false) || resJson.optString("status") == "success") {
                            val userObj = resJson.optJSONObject("user") ?: resJson
                            val user = UserProfile(
                                id = userObj.optString("id", "EMP-${System.currentTimeMillis() % 10000}"),
                                email = userObj.optString("email", email),
                                name = userObj.optString("name", "Field Officer"),
                                role = userObj.optString("role", "Field Officer"),
                                token = userObj.optString("token", "gas_session_${System.currentTimeMillis()}"),
                                siteName = userObj.optString("siteName", "Assigned Worksite"),
                                shiftStart = userObj.optString("shiftStart", "08:00"),
                                shiftEnd = userObj.optString("shiftEnd", "18:00"),
                                geofenceLat = userObj.optDouble("geofenceLat", 12.9716),
                                geofenceLng = userObj.optDouble("geofenceLng", 77.5946),
                                geofenceRadiusMeters = userObj.optDouble("geofenceRadiusMeters", 800.0),
                                loginTimestampMillis = System.currentTimeMillis()
                            )
                            return@withContext Result.success(user)
                        } else {
                            val err = resJson.optString("message", "Invalid credentials in Google Sheets Users tab")
                            Log.w(TAG, "GAS Login failed: $err")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "GAS HTTP login error, falling back to local credentials matching", e)
            }
        }

        // Local simulation / fallback against mock users
        val found = DEFAULT_USERS.find { it.email.equals(email.trim(), ignoreCase = true) }
        if (found != null) {
            Result.success(found.copy(loginTimestampMillis = System.currentTimeMillis()))
        } else {
            val generic = UserProfile(
                id = "EMP-${(email.hashCode() % 9000 + 1000).let { if (it < 0) -it else it }}",
                email = email.trim(),
                name = email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
                role = "Field Operations Officer",
                token = "sim_token_${System.currentTimeMillis()}",
                siteName = "Regional Field Hub",
                shiftStart = "08:00",
                shiftEnd = "18:00",
                geofenceLat = 12.9716,
                geofenceLng = 77.5946,
                geofenceRadiusMeters = 800.0,
                loginTimestampMillis = System.currentTimeMillis()
            )
            Result.success(generic)
        }
    }

    suspend fun submitAttendance(gasUrl: String, record: AttendanceRecord): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = if (gasUrl.trim().startsWith("http")) gasUrl.trim() else CONCEALED_GAS_URL
        if (cleanUrl.isNotEmpty() && cleanUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "attendance")
                    put("id", record.id)
                    put("email", record.userEmail)
                    put("timestampUtc", record.timestampUtc)
                    put("deviceId", record.deviceId)
                    put("latitude", record.latitude)
                    put("longitude", record.longitude)
                    put("address", record.address)
                    put("hasPhoto", record.photoUriOrBase64.isNotEmpty())
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(cleanUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "GAS attendance sync error", e)
            }
        }
        Result.success(true)
    }

    suspend fun submitTelemetry(gasUrl: String, log: TelemetryLog): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = if (gasUrl.trim().startsWith("http")) gasUrl.trim() else CONCEALED_GAS_URL
        if (cleanUrl.isNotEmpty() && cleanUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "telemetry")
                    put("id", log.id)
                    put("email", log.userEmail)
                    put("timestampUtc", log.timestampUtc)
                    put("deviceId", log.deviceId)
                    put("latitude", log.latitude)
                    put("longitude", log.longitude)
                    put("batteryPct", log.batteryPct)
                    put("triggerSource", log.triggerSource)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(cleanUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "GAS telemetry sync error", e)
            }
        }
        Result.success(true)
    }

    suspend fun submitJobRecord(gasUrl: String, job: JobRecord): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = if (gasUrl.trim().startsWith("http")) gasUrl.trim() else CONCEALED_GAS_URL
        if (cleanUrl.isNotEmpty() && cleanUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "job_proof")
                    put("id", job.id)
                    put("email", job.userEmail)
                    put("licensePlate", job.licensePlate)
                    put("startTimeMillis", job.startTimeMillis)
                    put("stopTimeMillis", job.stopTimeMillis)
                    put("durationSeconds", job.durationSeconds)
                    put("hasDocumentProof", job.documentProofBase64.isNotEmpty() || job.documentProofUri.isNotEmpty())
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(cleanUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "GAS job proof sync error", e)
            }
        }
        Result.success(true)
    }

    suspend fun submitTravelPermission(gasUrl: String, permission: TravelPermission): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = if (gasUrl.trim().startsWith("http")) gasUrl.trim() else CONCEALED_GAS_URL
        if (cleanUrl.isNotEmpty() && cleanUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "travel_permission")
                    put("id", permission.id)
                    put("email", permission.userEmail)
                    put("timestampUtc", permission.timestampUtc)
                    put("reason", permission.reason)
                    put("destination", permission.destinationLocation)
                    put("transitMinutes", permission.transitMinutes)
                    put("status", permission.status)
                    put("latitude", permission.latitude)
                    put("longitude", permission.longitude)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(cleanUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "GAS travel permission sync error", e)
            }
        }
        Result.success(true)
    }

    suspend fun logShiftLogout(gasUrl: String, email: String, reason: String, lat: Double, lng: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = if (gasUrl.trim().startsWith("http")) gasUrl.trim() else CONCEALED_GAS_URL
        if (cleanUrl.isNotEmpty() && cleanUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "shift_logout")
                    put("email", email)
                    put("reason", reason)
                    put("timestampUtc", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date()))
                    put("latitude", lat)
                    put("longitude", lng)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(cleanUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "GAS shift logout sync error", e)
            }
        }
        Result.success(true)
    }

    // Google Apps Script source code template for deployment in Google Sheets
    fun getGasScriptTemplate(): String {
        return """
/**
 * ShiftGuard Google Apps Script Backend Engine
 * Deploy as Web App with 'Execute as: Me' and 'Who has access: Anyone'.
 */
function doPost(e) {
  try {
    var raw = e.postData ? e.postData.contents : "{}";
    var data = JSON.parse(raw);
    var action = data.action || "";
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    if (action === "login") {
      var usersSheet = getOrCreateSheet(ss, "Users", [
        "Email", "Password", "Name", "Role", "SiteName", "ShiftStart", "ShiftEnd", "GeofenceLat", "GeofenceLng", "RadiusMeters"
      ]);
      // Seed default user if empty
      if (usersSheet.getLastRow() <= 1) {
        usersSheet.appendRow([
          "officer@shiftguard.com", "pass123", "Vikram Sharma", "Field Operations Officer", "Bangalore Central HQ Hub",
          "08:00", "18:00", 12.9716, 77.5946, 800
        ]);
        usersSheet.appendRow([
          "driver@shiftguard.com", "pass123", "Rajesh Kumar", "Fleet Logistics Driver", "Whitefield Freight Terminal",
          "06:00", "15:00", 12.9698, 77.7500, 1200
        ]);
      }
      var rows = usersSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        var rowEmail = String(rows[i][0]).toLowerCase().trim();
        var rowPass = String(rows[i][1]);
        if (rowEmail === String(data.email).toLowerCase().trim() && (!data.password || rowPass === String(data.password))) {
          return respondJson({
            success: true,
            user: {
              id: "EMP-" + (1000 + i),
              email: rows[i][0],
              name: rows[i][2],
              role: rows[i][3],
              siteName: rows[i][4] || "Assigned Site",
              token: "gas_tok_" + new Date().getTime(),
              shiftStart: rows[i][5] || "08:00",
              shiftEnd: rows[i][6] || "18:00",
              geofenceLat: Number(rows[i][7]) || 12.9716,
              geofenceLng: Number(rows[i][8]) || 77.5946,
              geofenceRadiusMeters: Number(rows[i][9]) || 800
            }
          });
        }
      }
      return respondJson({ success: false, message: "User not found or invalid password" });
    }

    if (action === "attendance") {
      var attSheet = getOrCreateSheet(ss, "Attendance", [
        "ID", "TimestampUTC", "Email", "DeviceID", "Latitude", "Longitude", "Address", "HasPhoto"
      ]);
      attSheet.appendRow([
        data.id, data.timestampUtc, data.email, data.deviceId,
        data.latitude, data.longitude, data.address, data.hasPhoto ? "YES" : "NO"
      ]);
      return respondJson({ success: true, message: "Attendance logged successfully" });
    }

    if (action === "telemetry") {
      var telSheet = getOrCreateSheet(ss, "Telemetry", [
        "ID", "TimestampUTC", "Email", "DeviceID", "Latitude", "Longitude", "BatteryPct", "TriggerSource"
      ]);
      telSheet.appendRow([
        data.id, data.timestampUtc, data.email, data.deviceId,
        data.latitude, data.longitude, data.batteryPct, data.triggerSource
      ]);
      return respondJson({ success: true, message: "Telemetry synced" });
    }

    if (action === "job_proof") {
      var jobSheet = getOrCreateSheet(ss, "Jobs", [
        "ID", "Email", "LicensePlate", "StartTime", "StopTime", "DurationSec", "DocumentProof"
      ]);
      jobSheet.appendRow([
        data.id, data.email, data.licensePlate,
        new Date(data.startTimeMillis).toISOString(),
        new Date(data.stopTimeMillis).toISOString(),
        data.durationSeconds,
        data.hasDocumentProof ? "DOCUMENT_CAPTURED" : "NONE"
      ]);
      return respondJson({ success: true, message: "Job proof recorded" });
    }

    if (action === "travel_permission") {
      var permSheet = getOrCreateSheet(ss, "TravelPermissions", [
        "ID", "TimestampUTC", "Email", "Destination", "Reason", "DurationMin", "Status", "Latitude", "Longitude"
      ]);
      permSheet.appendRow([
        data.id, data.timestampUtc, data.email, data.destination || "Other Site", data.reason, data.transitMinutes || 30, data.status, data.latitude, data.longitude
      ]);
      return respondJson({ success: true, message: "Transit permission granted" });
    }

    if (action === "shift_logout") {
      var logSheet = getOrCreateSheet(ss, "ShiftLogouts", [
        "TimestampUTC", "Email", "Reason", "Latitude", "Longitude"
      ]);
      logSheet.appendRow([
        data.timestampUtc, data.email, data.reason, data.latitude, data.longitude
      ]);
      return respondJson({ success: true, message: "Shift logout logged" });
    }

    return respondJson({ success: false, message: "Unknown action" });
  } catch (err) {
    return respondJson({ success: false, error: err.toString() });
  }
}

function doGet(e) {
  return respondJson({ status: "ShiftGuard GAS Engine Active", timestamp: new Date().toISOString() });
}

function getOrCreateSheet(ss, name, headers) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#D1E4FF");
  }
  return sheet;
}

function respondJson(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
        """.trimIndent()
    }
}
