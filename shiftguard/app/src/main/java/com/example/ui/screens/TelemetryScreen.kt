package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShiftGuardApplication
import com.example.service.FloatingBubbleService
import com.example.telemetry.LocationTelemetryWorker
import com.example.ui.overlay.BubbleActionActivity
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
import java.util.Locale

@Composable
fun TelemetryScreen() {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val geofence by storage.geofenceState.collectAsState()
    val user by storage.userProfile.collectAsState()
    val telemetryLogs by storage.telemetryLogs.collectAsState()
    val travelPermissions by storage.travelPermissions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Geofence Guard Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("geofence_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (geofence.breachActive) AlertRed else SuccessGreen)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (geofence.breachActive) Color(0x33EF4444) else Color(0x3310B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (geofence.breachActive) Icons.Default.Warning else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (geofence.breachActive) AlertRed else SuccessGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Shift & Geofence Guard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Continuous FusedLocation tracking", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (geofence.breachActive) Color(0x33EF4444) else Color(0x3310B981)
                        ) {
                            Text(
                                if (geofence.breachActive) "BREACH ACTIVE" else "INSIDE ZONE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (geofence.breachActive) AlertRed else SuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NavyDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Distance:", fontSize = 12.sp, color = TextMuted)
                                Text("${geofence.currentDistanceMeters.toInt()}m from center", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (geofence.breachActive) AlertRed else TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Allowed Radius:", fontSize = 12.sp, color = TextMuted)
                                Text("${user?.geofenceRadiusMeters?.toInt() ?: 800}m boundary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Shift Time Status:", fontSize = 12.sp, color = TextMuted)
                                Text(if (geofence.isWithinShift) "Within Shift Hours" else "Outside Shift Hours", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (geofence.isWithinShift) AmberWarning else TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breach simulation test controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (geofence.breachActive) {
                                    // Reset to inside
                                    storage.updateGeofence(
                                        isWithinShift = geofence.isWithinShift,
                                        isWithinBoundary = true,
                                        distanceMeters = 240.0,
                                        latitude = user?.geofenceLat ?: 12.9716,
                                        longitude = user?.geofenceLng ?: 77.5946,
                                        breachActive = false
                                    )
                                    Toast.makeText(context, "Perimeter status reset to Inside Zone", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Trigger breach
                                    val intent = Intent(context, FloatingBubbleService::class.java).apply {
                                        action = FloatingBubbleService.ACTION_SIMULATE_BREACH
                                    }
                                    context.startService(intent)
                                    Toast.makeText(context, "Simulating Geofence Breach! Bubble 2 is now flashing alert.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("simulate_breach_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (geofence.breachActive) SuccessGreen else AlertRed)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (geofence.breachActive) "Reset Inside" else "Simulate Breach", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(context, BubbleActionActivity::class.java).apply {
                                    action = BubbleActionActivity.ACTION_GEOFENCE_BREACH
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f).testTag("open_breach_dialog_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                        ) {
                            Text("Open Breach Alert", color = NavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Telemetry Engine & FCM Server Trigger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("telemetry_engine_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TechCyan))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TechCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Background Telemetry Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3300E5FF)
                        ) {
                            Text("1-HR POLLING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TechCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Text(
                        "Periodic WorkManager task polls every 1 hour. In addition, incoming high-priority Firebase Cloud Messaging (FCM) push messages force an immediate background location and telemetry synchronization push.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                LocationTelemetryWorker.enqueueImmediateSync(context, source = "FCM_PUSH")
                                Toast.makeText(context, "Sync Signal Received: Telemetry Updated", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("simulate_fcm_push_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyElevated),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Push", color = AmberWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                LocationTelemetryWorker.enqueueImmediateSync(context, source = "MANUAL")
                                Toast.makeText(context, "Immediate Telemetry Sync Queued", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("manual_sync_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", color = NavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Travel Permissions Granted History
        if (travelPermissions.isNotEmpty()) {
            item {
                Text("APPROVED TRAVELLING PERMISSIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            }
            items(travelPermissions) { perm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(perm.id, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0x33FFB703)) {
                                Text(perm.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberWarning, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(perm.reason, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                        Text(perm.timestampUtc, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }
                }
            }
        }

        // Telemetry Logs Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TELEMETRY SYNC LOGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text("${telemetryLogs.size} logs", fontSize = 11.sp, color = TechCyan)
            }
        }

        items(telemetryLogs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = TechCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${String.format(Locale.US, "%.4f", log.latitude)}, ${String.format(Locale.US, "%.4f", log.longitude)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(log.timestampUtc, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        Text("Trigger: ${log.triggerSource}", fontSize = 10.sp, color = TechCyan)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${log.batteryPct}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        Text(log.syncStatus, fontSize = 10.sp, color = SuccessGreen)
                    }
                }
            }
        }
    }
}
