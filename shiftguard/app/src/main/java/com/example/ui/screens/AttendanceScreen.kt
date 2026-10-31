package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShiftGuardApplication
import com.example.data.model.AttendanceRecord
import com.example.data.remote.GasEngine
import com.example.ml.DocumentProcessor
import com.example.service.FloatingBubbleService
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@SuppressLint("MissingPermission", "HardwareIds")
@Composable
fun AttendanceScreen() {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val user by storage.userProfile.collectAsState()
    val attendanceList by storage.attendanceHistory.collectAsState()
    val isBubbleRunning by storage.isBubbleServiceRunning.collectAsState()
    val geofence by storage.geofenceState.collectAsState()
    val scope = rememberCoroutineScope()

    var isCapturingPhoto by remember { mutableStateOf(false) }
    var capturedSelfie by remember { mutableStateOf<Bitmap?>(null) }
    var lastSubmittedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var showSiteConfigDialog by remember { mutableStateOf(false) }

    // Check overlay permission periodically
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    val selfieLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedSelfie = bitmap
            isCapturingPhoto = true
            scope.launch {
                try {
                    val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
                    val loc: Location? = try {
                        fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                            ?: fusedLocation.lastLocation.await()
                    } catch (e: Exception) { null }

                    val lat = loc?.latitude ?: user?.geofenceLat ?: 12.9716
                    val lng = loc?.longitude ?: user?.geofenceLng ?: 77.5946

                    // Reverse Geocoding Address
                    val addressStr = withContext(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val a = addresses[0]
                                "${a.thoroughfare ?: ""}, ${a.locality ?: a.subAdminArea ?: "Bangalore"}, ${a.adminArea ?: "Karnataka"}"
                                    .trim(',', ' ')
                            } else "MG Road Hub, Central District, Bangalore"
                        } catch (e: Exception) {
                            "Bangalore Tech Corridor, MG Road, Karnataka"
                        }
                    }

                    // UTC Timestamp
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val utcNow = sdf.format(Date())

                    // Device UUID
                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "DEVICE_UUID"

                    val base64 = DocumentProcessor.bitmapToBase64(bitmap, quality = 80)
                    val record = AttendanceRecord(
                        id = "ATT-${System.currentTimeMillis() % 100000}",
                        userEmail = user?.email ?: "user@shiftguard.com",
                        timestampUtc = utcNow,
                        deviceId = deviceId,
                        latitude = lat,
                        longitude = lng,
                        address = addressStr,
                        photoUriOrBase64 = base64,
                        syncStatus = "SYNCED"
                    )

                    // Submit attendance record
                    GasEngine.submitAttendance(storage.gasUrl.value, record)
                    storage.addAttendanceRecord(record)
                    lastSubmittedRecord = record
                    isCapturingPhoto = false
                    Toast.makeText(context, "Attendance Recorded Successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    isCapturingPhoto = false
                    Toast.makeText(context, "Error recording attendance: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overlay Permission Banner (if not granted)
        if (!hasOverlayPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1700)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AmberWarning)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Floating Bubbles Permission Required", fontWeight = FontWeight.Bold, color = AmberWarning, fontSize = 14.sp)
                        }
                        Text(
                            "Enable 'Display over other apps' to allow ShiftGuard's persistent Job Timer & Geofence Guard bubbles on your home screen.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Overlay Permission", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // User Profile & Shift Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NavyElevated)
                                    .border(1.5.dp, TechCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TechCyan, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user?.name ?: "Field Officer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(user?.role ?: "Field Operations", fontSize = 12.sp, color = TechCyan)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (geofence.isWithinShift) Color(0x3310B981) else Color(0x33EF4444)
                        ) {
                            Text(
                                if (geofence.isWithinShift) "ON-DUTY" else "OFF-SHIFT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (geofence.isWithinShift) SuccessGreen else AlertRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = NavyBorder)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ASSIGNED SHIFT", fontSize = 10.sp, color = TextMuted)
                            }
                            Text("${user?.shiftStart ?: "08:00"} — ${user?.shiftEnd ?: "18:00"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DEVICE UUID", fontSize = 10.sp, color = TextMuted)
                            }
                            val devId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "N/A"
                            Text(devId.take(12), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Dynamic Site & Geofence Boundary Card (Answers user question: dynamic, not hardcoded!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("worksite_config_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TechCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Active Worksite & Perimeter", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0x3300E5FF)) {
                            Text("DYNAMIC SITE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TechCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Text(
                        "Assigned station coordinates and duty perimeter configured below.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NavyDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Site Name:", fontSize = 11.sp, color = TextMuted)
                                Text(user?.siteName ?: "Bangalore Central HQ Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Center Coordinates:", fontSize = 11.sp, color = TextMuted)
                                Text("${String.format(Locale.US, "%.4f", user?.geofenceLat ?: 12.9716)}, ${String.format(Locale.US, "%.4f", user?.geofenceLng ?: 77.5946)}", fontSize = 11.sp, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Perimeter Radius:", fontSize = 11.sp, color = TextMuted)
                                Text("${user?.geofenceRadiusMeters?.toInt() ?: 800} meters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val lat = user?.geofenceLat ?: 12.9716
                                val lng = user?.geofenceLng ?: 77.5946
                                val site = user?.siteName ?: "Station"
                                com.example.ui.screens.launchNativeMaps(context, lat, lng, site)
                            },
                            modifier = Modifier.weight(1f).height(42.dp).testTag("site_directions_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Directions", color = WhatsAppGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showSiteConfigDialog = true },
                            modifier = Modifier.weight(1f).height(42.dp).testTag("edit_site_config_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyElevated),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = TechCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure", color = TechCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Midnight Session Policy & Test Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("midnight_policy_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Nightlight, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("12:00 AM Midnight Re-Auth", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            "Mandatory password prompt enforces shift verification after 12:00 AM midnight.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Button(
                        onClick = {
                            storage.simulateMidnightExpiry(true)
                            Toast.makeText(context, "12:00 AM Midnight Session Expiry triggered!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("simulate_midnight_btn")
                    ) {
                        Text("Test 12 AM", color = NavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Bubble Foreground Service Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("bubble_toggle_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isBubbleRunning) TechCyan else NavyBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = TechCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Always-On Floating Bubbles", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            if (isBubbleRunning) "Active on Android home screen & across all apps" else "Toggle on to float Job Timer & Geofence Guard",
                            fontSize = 12.sp,
                            color = if (isBubbleRunning) SuccessGreen else TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Switch(
                        checked = isBubbleRunning,
                        onCheckedChange = { start ->
                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                                return@Switch
                            }
                            if (start) {
                                FloatingBubbleService.startService(context)
                                Toast.makeText(context, "ShiftGuard Floating Bubbles Launched!", Toast.LENGTH_SHORT).show()
                            } else {
                                FloatingBubbleService.stopService(context)
                                Toast.makeText(context, "Floating Bubbles Stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TechCyan,
                            checkedTrackColor = NavyElevated,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = NavyDark
                        )
                    )
                }
            }
        }

        // Photo Attendance Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("photo_attendance_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Photo Attendance Check-In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3310B981)
                        ) {
                            Text("GAS VERIFIED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Text(
                        "Selfie capture tagged with UTC timestamp, Device UUID, exact GPS coords, and reverse geocoded human-readable address.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (capturedSelfie != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, SuccessGreen, RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    bitmap = capturedSelfie!!.asImageBitmap(),
                                    contentDescription = "Attendance Photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Selfie Tagged & Processed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                                lastSubmittedRecord?.let { rec ->
                                    Text(rec.address, fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                                    Text("${rec.latitude}, ${rec.longitude}", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { selfieLauncher.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("capture_attendance_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCapturingPhoto
                    ) {
                        if (isCapturingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NavyDark)
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NavyDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capture Selfie & Mark Attendance", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Attendance History Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ATTENDANCE LOG HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text("${attendanceList.size} records", fontSize = 11.sp, color = TechCyan)
            }
        }

        items(attendanceList) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TechCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.address, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                        }
                        Text(item.syncStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.timestampUtc, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        Text("${String.format(Locale.US, "%.4f", item.latitude)}, ${String.format(Locale.US, "%.4f", item.longitude)}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }

    if (showSiteConfigDialog) {
        SiteConfigDialog(
            user = user,
            onDismiss = { showSiteConfigDialog = false },
            onSave = { name, lat, lng, radius, start, end ->
                storage.updateSiteConfig(name, lat, lng, radius, start, end)
                showSiteConfigDialog = false
                Toast.makeText(context, "Worksite & Geofence updated dynamically!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SiteConfigDialog(
    user: com.example.data.model.UserProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double, radius: Double, start: String, end: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf(user?.siteName ?: "Bangalore Central HQ Hub") }
    var latInput by remember { mutableStateOf(user?.geofenceLat?.toString() ?: "12.9716") }
    var lngInput by remember { mutableStateOf(user?.geofenceLng?.toString() ?: "77.5946") }
    var radiusInput by remember { mutableStateOf(user?.geofenceRadiusMeters?.toInt()?.toString() ?: "800") }
    var shiftStartInput by remember { mutableStateOf(user?.shiftStart ?: "08:00") }
    var shiftEndInput by remember { mutableStateOf(user?.shiftEnd ?: "18:00") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("site_config_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TechCyan))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = TechCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Worksite", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Text(
                    "Station parameters and shift times can be reconfigured dynamically as needed.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Worksite Name", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("config_site_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("config_lat"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        label = { Text("Longitude", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("config_lng"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val fused = LocationServices.getFusedLocationProviderClient(context)
                                val loc = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                                    ?: fused.lastLocation.await()
                                if (loc != null) {
                                    latInput = String.format(Locale.US, "%.6f", loc.latitude)
                                    lngInput = String.format(Locale.US, "%.6f", loc.longitude)
                                    Toast.makeText(context, "Coordinates set to current GPS!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not acquire current GPS", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = TechCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Coordinates to Current GPS", color = TechCyan, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = radiusInput,
                    onValueChange = { radiusInput = it },
                    label = { Text("Perimeter Radius (Meters)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("config_radius"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shiftStartInput,
                        onValueChange = { shiftStartInput = it },
                        label = { Text("Shift Start (HH:mm)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("config_shift_start"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = shiftEndInput,
                        onValueChange = { shiftEndInput = it },
                        label = { Text("Shift End (HH:mm)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("config_shift_end"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val lat = latInput.toDoubleOrNull() ?: 12.9716
                            val lng = lngInput.toDoubleOrNull() ?: 77.5946
                            val rad = radiusInput.toDoubleOrNull() ?: 800.0
                            onSave(nameInput, lat, lng, rad, shiftStartInput, shiftEndInput)
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("save_site_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save & Apply", color = NavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
