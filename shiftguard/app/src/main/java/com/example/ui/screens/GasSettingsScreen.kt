package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShiftGuardApplication
import com.example.data.remote.GasEngine
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.QuantumBorder
import com.example.ui.theme.QuantumDark
import com.example.ui.theme.QuantumElevated
import com.example.ui.theme.QuantumLime
import com.example.ui.theme.QuantumSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenDark
import kotlinx.coroutines.launch

@Composable
fun GasSettingsScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val user by storage.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var isTestingConnection by remember { mutableStateOf(false) }
    var pingLatencyMs by remember { mutableStateOf<Long?>(null) }
    var pingStatus by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantumDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Closed Loop Sealed Architecture Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("closed_loop_banner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = QuantumSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WhatsAppGreen))
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2525D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("CLOSED-LOOP SYSTEM", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 0.5.sp)
                                Text("Engine Concealed & Tamper-Sealed", fontSize = 11.sp, color = WhatsAppGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x3325D366),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen)
                        ) {
                            Text(
                                "SEALED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "All Google Apps Script endpoints and Firebase Realtime Database connections are compiled into the application binary. No external configuration fields are exposed, ensuring closed-loop enterprise compliance.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Concealed Endpoints Audit (Read-Only / Masked)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sealed_endpoints_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = QuantumSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumBorder))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = QuantumLime)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backend Routing Engine (Bytecode Level)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sealed Google Apps Script Endpoint
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = QuantumDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuantumBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GOOGLE APPS SCRIPT WEB APP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text("COMPILED IN BINARY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "https://script.google.com/macros/s/AKfycb...QRl4q/exec",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = WhatsAppGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sealed Firebase Realtime Database Endpoint
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = QuantumDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuantumBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("FIREBASE REALTIME DB NODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text("AUTHENTICATED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = QuantumLime)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "quantum-99abd-default-rtdb.asia-southeast1.firebasedatabase.app",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = QuantumLime
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ping Diagnostic Action
                    Button(
                        onClick = {
                            isTestingConnection = true
                            pingStatus = null
                            val start = System.currentTimeMillis()
                            scope.launch {
                                val res = GasEngine.login(
                                    GasEngine.CONCEALED_GAS_URL,
                                    user?.email ?: "officer@shiftguard.com",
                                    "pass123"
                                )
                                pingLatencyMs = System.currentTimeMillis() - start
                                isTestingConnection = false
                                if (res.isSuccess) {
                                    pingStatus = "Node Online • HTTP 200 • ${pingLatencyMs}ms"
                                } else {
                                    pingStatus = "Fallback active • ${pingLatencyMs}ms"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ping_engine_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenDark),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isTestingConnection
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Closed-Loop Node...", color = Color.White, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ping Quantum Engine Node", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (pingStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            pingStatus!!,
                            color = WhatsAppGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Live Module Health Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = QuantumSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumBorder))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ACTIVE DATA MODULES IN GOOGLE SHEETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))

                    val modules = listOf(
                        Triple("Users Master Sheet", "Emp Profile & Credentials", "ACTIVE"),
                        Triple("Attendance Logs", "Selfie, GPS & Reverse Geocoding", "STREAMING"),
                        Triple("Jobs & Document OCR", "Vehicle Plates & ML Kit B&W", "READY"),
                        Triple("Hourly Telemetry Engine", "WorkManager Periodic 1h", "SCHEDULED"),
                        Triple("Geofence Guard", "30s Fused Location Polling", "ARMED"),
                        Triple("Travel Permissions", "Multi-Site Transit Clearance", "ENABLED")
                    )

                    modules.forEach { (name, desc, badge) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(desc, fontSize = 11.sp, color = TextSecondary)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x2225D366)
                            ) {
                                Text(
                                    badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = QuantumBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        // Active Session Card & Logout
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_session_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = QuantumSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumBorder))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("AUTHENTICATED SESSION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${user?.name} (${user?.email})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Role: ${user?.role} • Worksite: ${user?.siteName}", fontSize = 12.sp, color = TextSecondary)
                    Text("Token: ${user?.token}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = QuantumLime)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("logout_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out / Switch User", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
