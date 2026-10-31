package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShiftGuardApplication
import com.example.data.model.UserProfile
import com.example.ui.theme.AlertRed
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

data class TeamMember(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
    val station: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val isLeadership: Boolean = false
)

val TEAM_MEMBERS = listOf(
    TeamMember(
        id = "MGR-01",
        name = "Marcus Vance",
        role = "Operations Supervisor",
        phone = "+918045678900",
        email = "marcus.vance@shiftguard-ops.com",
        station = "Command Center",
        latitude = 12.9716,
        longitude = 77.5946,
        status = "Active",
        isLeadership = true
    ),
    TeamMember(
        id = "DIR-02",
        name = "Sarah Jenkins",
        role = "Director of Logistics",
        phone = "+918045678911",
        email = "sarah.jenkins@shiftguard-ops.com",
        station = "Regional HQ",
        latitude = 12.9800,
        longitude = 77.6000,
        status = "Available",
        isLeadership = true
    ),
    TeamMember(
        id = "FLD-101",
        name = "Rahul Sharma",
        role = "Field Specialist",
        phone = "+918045678922",
        email = "rahul.sharma@shiftguard-ops.com",
        station = "North Terminal Depot",
        latitude = 12.9780,
        longitude = 77.6400,
        status = "On Duty"
    ),
    TeamMember(
        id = "FLD-102",
        name = "Vikram Patel",
        role = "Fleet Engineer",
        phone = "+918045678933",
        email = "vikram.patel@shiftguard-ops.com",
        station = "South Air Cargo Yard",
        latitude = 12.9250,
        longitude = 77.5938,
        status = "In Transit"
    ),
    TeamMember(
        id = "FLD-103",
        name = "Priya Nair",
        role = "Security Lead",
        phone = "+918045678944",
        email = "priya.nair@shiftguard-ops.com",
        station = "West Logistics Hub",
        latitude = 12.9600,
        longitude = 77.5300,
        status = "On Duty"
    )
)

fun launchNativeDialer(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
    }
}

fun launchNativeMaps(context: Context, lat: Double, lng: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

@Composable
fun PeopleScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val userProfile by storage.userProfile.collectAsState()
    val geofence by storage.geofenceState.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) }
    val sectionTitles = listOf("Me", "Team")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantumDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("people_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Tab Row
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = QuantumSurface,
            contentColor = WhatsAppGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                    color = WhatsAppGreen,
                    height = 3.dp
                )
            },
            divider = { HorizontalDivider(color = QuantumBorder) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, QuantumBorder, RoundedCornerShape(12.dp))
        ) {
            sectionTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedSection == index) WhatsAppGreen else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("people_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSection == 0) {
            // SECTION: ME
            ProfileMeSection(
                user = userProfile,
                geofenceDistance = geofence.currentDistanceMeters,
                isBreach = geofence.breachActive,
                onDirectionsClick = {
                    userProfile?.let {
                        launchNativeMaps(context, it.geofenceLat, it.geofenceLng, it.siteName)
                    }
                },
                onLogoutClick = onLogout
            )
        } else {
            // SECTION: OTHERS / TEAM
            TeamOthersSection(
                onCall = { phone -> launchNativeDialer(context, phone) },
                onNavigate = { lat, lng, label -> launchNativeMaps(context, lat, lng, label) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ProfileMeSection(
    user: UserProfile?,
    geofenceDistance: Double,
    isBreach: Boolean,
    onDirectionsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // Identity Profile Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_me_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumBorder))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(WhatsAppGreen, WhatsAppGreenDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.name?.firstOrNull()?.toString() ?: "U",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF04240F)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.name ?: "Field Officer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user?.email ?: "officer@shiftguard-ops.com",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x2225D366)
                ) {
                    Text(
                        text = user?.role ?: "Specialist",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Station & Duty Hours Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("station_me_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumBorder))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Station", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                }

                Button(
                    onClick = onDirectionsClick,
                    modifier = Modifier.height(34.dp).testTag("me_directions_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumElevated),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WhatsAppGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = user?.siteName ?: "Main Operations Hub",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "GPS: ${user?.geofenceLat ?: 12.9716}, ${user?.geofenceLng ?: 77.5946} • ${user?.geofenceRadiusMeters?.toInt() ?: 800}m radius",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = QuantumBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = QuantumLime, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shift", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${user?.shiftStart ?: "08:00"} - ${user?.shiftEnd ?: "18:00"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Compliance", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBreach) Color(0x33EF4444) else Color(0x2225D366)
                    ) {
                        Text(
                            text = if (isBreach) "Breach: ${geofenceDistance.toInt()}m" else "In Perimeter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBreach) AlertRed else WhatsAppGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Sign Out
    OutlinedButton(
        onClick = onLogoutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("me_sign_out_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
    ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun TeamOthersSection(
    onCall: (String) -> Unit,
    onNavigate: (Double, Double, String) -> Unit
) {
    val leadership = TEAM_MEMBERS.filter { it.isLeadership }
    val peers = TEAM_MEMBERS.filter { !it.isLeadership }

    // Hierarchy / Leadership
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Management Hierarchy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
    }

    Spacer(modifier = Modifier.height(10.dp))

    leadership.forEach { member ->
        TeamMemberCard(
            member = member,
            onCall = { onCall(member.phone) },
            onNavigate = { onNavigate(member.latitude, member.longitude, member.station) }
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Team Roster
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = QuantumLime, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Field Team", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
    }

    Spacer(modifier = Modifier.height(10.dp))

    peers.forEach { member ->
        TeamMemberCard(
            member = member,
            onCall = { onCall(member.phone) },
            onNavigate = { onNavigate(member.latitude, member.longitude, member.station) }
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun TeamMemberCard(
    member: TeamMember,
    onCall: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (member.isLeadership) QuantumElevated else QuantumDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.firstOrNull()?.toString() ?: "T",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (member.isLeadership) WhatsAppGreen else QuantumLime
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (member.status) {
                            "Active", "On Duty" -> Color(0x2225D366)
                            "In Transit" -> Color(0x33FFB703)
                            else -> Color(0x2294A3B8)
                        }
                    ) {
                        Text(
                            text = member.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (member.status) {
                                "Active", "On Duty" -> WhatsAppGreen
                                "In Transit" -> Color(0xFFFFB703)
                                else -> TextMuted
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${member.role} • ${member.station}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(
                onClick = onNavigate,
                modifier = Modifier.size(36.dp).testTag("navigate_btn_${member.id}")
            ) {
                Icon(Icons.Default.Navigation, contentDescription = "Directions", tint = TextMuted, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = onCall,
                modifier = Modifier.size(36.dp).testTag("call_btn_${member.id}")
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = WhatsAppGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}
