package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShiftGuardApplication
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
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun JobsScreen() {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val activeJob by storage.activeJob.collectAsState()
    val jobHistory by storage.jobHistory.collectAsState()

    // Running timer pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Local ticker for live UI display if active
    var liveSeconds by remember { mutableStateOf(activeJob.elapsedSeconds) }
    LaunchedEffect(activeJob) {
        while (activeJob.isRunning) {
            val elapsed = (System.currentTimeMillis() - activeJob.startTimeMillis) / 1000L
            liveSeconds = if (elapsed > 0) elapsed else 0L
            delay(1000L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Job Banner or Start Job CTA
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_job_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (activeJob.isRunning) TechCyan else NavyBorder)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeJob.isRunning) {
                        // ACTIVE JOB WITH RUNNING CLOCK AND PULSE RING
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3300E5FF),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                "JOB IN PROGRESS • BUBBLE 1 ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Pulsing Wave Ring around timer
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF))
                                .border(2.dp, TechCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val h = liveSeconds / 3600
                                val m = (liveSeconds % 3600) / 60
                                val s = liveSeconds % 60
                                Text(
                                    String.format(Locale.US, "%02d:%02d:%02d", h, m, s),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text("RUNNING", fontSize = 10.sp, color = TechCyan, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyDark)
                                .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TechCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vehicle License Plate:", fontSize = 12.sp, color = TextMuted)
                            }
                            Text(activeJob.licensePlate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = Intent(context, BubbleActionActivity::class.java).apply {
                                    action = BubbleActionActivity.ACTION_STOP_JOB_DOCUMENT
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("stop_job_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Job & Capture Document Proof", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // IDLE STATE: PROMPT TO START JOB
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NavyElevated)
                                .border(1.5.dp, TechCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TechCyan, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Job Timer & Vehicle OCR", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                        Text(
                            "Start a field job with Google ML Kit License Plate Recognition. Floating Bubble 1 will track running job time globally over other apps.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                val intent = Intent(context, BubbleActionActivity::class.java).apply {
                                    action = BubbleActionActivity.ACTION_START_JOB_OCR
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_job_ocr_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NavyDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Initiate Job (ML Kit Plate Scan)", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Job History List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("COMPLETED JOBS & DOCUMENT PROOFS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text("${jobHistory.size} completed", fontSize = 11.sp, color = TechCyan)
            }
        }

        items(jobHistory) { job ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TechCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(job.licensePlate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3310B981)
                        ) {
                            Text(job.syncStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("JOB DURATION", fontSize = 10.sp, color = TextMuted)
                            val m = job.durationSeconds / 60
                            val s = job.durationSeconds % 60
                            Text("${m}m ${s}s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DOCUMENT PROOF", fontSize = 10.sp, color = TextMuted)
                            Text(
                                if (job.documentProofBase64.isNotEmpty() || job.documentProofUri.isNotEmpty()) "B&W ATTACHED" else "NO PROOF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (job.documentProofBase64.isNotEmpty() || job.documentProofUri.isNotEmpty()) AmberWarning else TextMuted
                            )
                        }
                    }

                    // Document Proof preview if base64 available
                    if (job.documentProofBase64.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val proofBitmap = remember(job.documentProofBase64) {
                            try {
                                val bytes = Base64.decode(job.documentProofBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (proofBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, NavyBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = proofBitmap.asImageBitmap(),
                                    contentDescription = "Document Proof Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
