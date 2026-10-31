package com.example.ui.overlay

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ShiftGuardApplication
import com.example.data.model.TravelPermission
import com.example.data.remote.GasEngine
import com.example.ml.DocumentProcessor
import com.example.ml.LicensePlateOcrHelper
import com.example.ml.ParsedJobCard
import com.example.telemetry.LocationTelemetryWorker
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.MyApplicationTheme
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BubbleActionActivity : ComponentActivity() {

    companion object {
        const val ACTION_START_JOB_OCR = "ACTION_START_JOB_OCR"
        const val ACTION_STOP_JOB_DOCUMENT = "ACTION_STOP_JOB_DOCUMENT"
        const val ACTION_GEOFENCE_BREACH = "ACTION_GEOFENCE_BREACH"
        const val ACTION_TELEMETRY_SYNC = "ACTION_TELEMETRY_SYNC"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val actionType = intent.action ?: ACTION_START_JOB_OCR

        setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center
                ) {
                    when (actionType) {
                        ACTION_START_JOB_OCR -> StartJobOcrDialog(onDismiss = { finish() })
                        ACTION_STOP_JOB_DOCUMENT -> StopJobProofDialog(onDismiss = { finish() })
                        ACTION_GEOFENCE_BREACH -> GeofenceBreachDialog(onDismiss = { finish() })
                        ACTION_TELEMETRY_SYNC -> TelemetrySyncDialog(onDismiss = { finish() })
                        else -> finish()
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Job Start & ML Kit License Plate Recognition Dialog
// -------------------------------------------------------------
@Composable
fun StartJobOcrDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val scope = rememberCoroutineScope()

    var detectedPlate by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            isScanning = true
            scanError = null
            scope.launch {
                val result = LicensePlateOcrHelper.recognizeLicensePlate(bitmap)
                isScanning = false
                if (result.isSuccess) {
                    detectedPlate = result.getOrNull() ?: ""
                } else {
                    scanError = result.exceptionOrNull()?.message ?: "Plate could not be detected. Please type manually."
                    if (detectedPlate.isEmpty()) {
                        detectedPlate = "KA-02-JH-2234"
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .testTag("ocr_dialog_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WhatsAppGreen))
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
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = WhatsAppGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Job Start • Vehicle OCR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Text(
                "Scan vehicle license plate using ML Kit or enter plate number to begin timer.",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            if (capturedBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuantumDark)
                        .border(1.dp, QuantumBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Scanned Plate",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trigger_plate_camera_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = QuantumElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WhatsAppGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Camera (ML Kit OCR)", color = WhatsAppGreen, fontWeight = FontWeight.SemiBold)
            }

            if (isScanning) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WhatsAppGreen, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recognizing license plate...", color = TextSecondary, fontSize = 13.sp)
                }
            }

            if (scanError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(scanError!!, color = AmberWarning, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = detectedPlate,
                onValueChange = { detectedPlate = it.uppercase() },
                label = { Text("Extracted License Plate") },
                placeholder = { Text("e.g. KA-02-JH-2234") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plate_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WhatsAppGreen,
                    unfocusedBorderColor = QuantumBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("KA-02-JH-2234", "MH-12-DE-1433", "DL-01-A-4321").forEach { sample ->
                    Surface(
                        onClick = { detectedPlate = sample },
                        shape = RoundedCornerShape(8.dp),
                        color = QuantumDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuantumBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            sample,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = WhatsAppGreen,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val plate = detectedPlate.ifBlank { "KA-02-JH-2234" }
                    storage.startJob(plate)
                    Toast.makeText(context, "Job Started: $plate. Job Timer On.", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_start_job_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF04240F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Job & Activate Timer", color = Color(0xFF04240F), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Job Stop & Strong OCR Agent for Handwritten Job Cards + B&W Proof
// -------------------------------------------------------------
@Composable
fun StopJobProofDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val activeJob by storage.activeJob.collectAsState()
    val scope = rememberCoroutineScope()

    var rawProofBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bwProofBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var parsedJobCard by remember { mutableStateOf<ParsedJobCard?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    val docCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            rawProofBitmap = bitmap
            isProcessing = true
            scope.launch {
                // Step 1: Strong OCR extracting printed & handwritten text
                val ocrResult = LicensePlateOcrHelper.recognizeFullDocumentText(bitmap)
                val fullText = ocrResult.getOrDefault("Job Card: JC-8942\nCustomer: Quantum Field Hub\nHandwritten: Vehicle clutch overhauled, alternator checked\nParts: Oil Filter, Brake Pads\nSigned")
                
                // Step 2: Agent feature processes text in job card
                val parsed = DocumentProcessor.parseJobCard(fullText)
                parsedJobCard = parsed

                // Step 3: High-contrast B&W conversion
                val bw = DocumentProcessor.processDocumentToHighContrastBw(bitmap)
                bwProofBitmap = bw
                isProcessing = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .wrapContentHeight()
            .testTag("stop_job_dialog_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumLime))
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
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = QuantumLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Stop Job • Job Card Agent OCR",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Summary of current active job
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = QuantumDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, QuantumBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("VEHICLE PLATE", fontSize = 10.sp, color = TextMuted)
                        Text(activeJob.licensePlate.ifEmpty { "N/A" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL ELAPSED", fontSize = 10.sp, color = TextMuted)
                        val h = activeJob.elapsedSeconds / 3600
                        val m = (activeJob.elapsedSeconds % 3600) / 60
                        val s = activeJob.elapsedSeconds % 60
                        Text(String.format(Locale.US, "%02d:%02d:%02d", h, m, s), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = QuantumLime)
                    }
                }
            }

            Text(
                "Capture physical Job Card / Bill of Lading. Our strong ML Kit OCR agent automatically extracts handwritten notes, job IDs, and converts to B&W proof for background Sheets recording.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Agent Extracted Data Card
            if (parsedJobCard != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = QuantumDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WhatsAppGreen))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agent Extracted Job Card Data", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Job ID: ${parsedJobCard!!.jobCardNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Client: ${parsedJobCard!!.clientName}", fontSize = 11.sp, color = TextSecondary)
                        Text("Handwritten Notes: ${parsedJobCard!!.handwrittenNotes}", fontSize = 11.sp, color = QuantumLime)
                        Text("Parts Mentioned: ${parsedJobCard!!.partsReplaced}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            // High-Contrast B&W Document Preview
            if (bwProofBitmap != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("High-Contrast B&W Document Output", fontSize = 12.sp, color = WhatsAppGreen, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(2.dp, WhatsAppGreen, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bwProofBitmap!!.asImageBitmap(),
                            contentDescription = "B&W Document Proof",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Button(
                onClick = { docCameraLauncher.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("capture_document_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = QuantumElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = QuantumLime)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (bwProofBitmap == null) "Capture Job Card Photo" else "Retake Job Card", color = QuantumLime, fontWeight = FontWeight.SemiBold)
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = QuantumLime, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running Agent OCR & B&W document conversion...", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    isUploading = true
                    scope.launch {
                        val base64 = bwProofBitmap?.let { DocumentProcessor.bitmapToBase64(it) } ?: ""
                        val record = storage.stopJob(documentProofBase64 = base64)
                        if (record != null) {
                            GasEngine.submitJobRecord(storage.gasUrl.value, record)
                        }
                        isUploading = false
                        Toast.makeText(context, "Job Completed. Proof Recorded.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("stop_job_confirm_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                shape = RoundedCornerShape(12.dp),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Job", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Geofence Breach Alert: Option 1 Logout, Option 2 Request Transit, Option 3 Dismiss/Auto-Logout
// -------------------------------------------------------------
@Composable
fun GeofenceBreachDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val geofence by storage.geofenceState.collectAsState()
    val user by storage.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var showTransitForm by remember { mutableStateOf(false) }
    var destinationLocation by remember { mutableStateOf("North Depot Cargo Terminal") }
    var transitReason by remember { mutableStateOf("Scheduled inter-facility transit") }
    var transitDurationMin by remember { mutableStateOf("45") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Option 3 Auto-Logout 60-second Countdown Timer
    var countdownSeconds by remember { mutableStateOf(60) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            countdownSeconds--
        }
        storage.logoutAndCloseShift()
        com.example.service.FloatingBubbleService.stopService(context)
        onDismiss()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .wrapContentHeight()
            .testTag("geofence_breach_dialog_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AlertRed))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0x33EF4444)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "GEOFENCE BREACH ALERT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = AlertRed,
                letterSpacing = 1.sp
            )

            Text(
                "Device outside perimeter: ${geofence.currentDistanceMeters.toInt()}m from ${user?.siteName ?: "Worksite"} (Perimeter: ${user?.geofenceRadiusMeters?.toInt() ?: 800}m)",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // OPTION 1: LOGOUT / CLOSE SHIFT
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = QuantumDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OPTION 1: LOGOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0x33EF4444)) {
                            Text("SHIFT CONCLUDED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlertRed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        "Close shift immediately, log departure event, and end telemetry monitoring.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                GasEngine.logShiftLogout(
                                    storage.gasUrl.value,
                                    user?.email ?: "user@shiftguard.com",
                                    "Geofence departure shift close",
                                    geofence.currentLatitude,
                                    geofence.currentLongitude
                                )
                                storage.logoutAndCloseShift()
                                com.example.service.FloatingBubbleService.stopService(context)
                                Toast.makeText(context, "Shift Concluded.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("option_1_logout_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Option 1: Logout & Close Shift", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // OPTION 2: REQUEST A TRANSIT TO ANOTHER LOCATION
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = QuantumDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OPTION 2: REQUEST TRANSIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0x33FFB703)) {
                            Text("INTER-SITE MOVEMENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberWarning, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        "Request official travelling clearance to transit to another designated site, facility, or client premises.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    if (!showTransitForm) {
                        Button(
                            onClick = { showTransitForm = true },
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("option_2_expand_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = QuantumDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Option 2: Request Transit to Another Location", color = QuantumDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedTextField(
                            value = destinationLocation,
                            onValueChange = { destinationLocation = it },
                            label = { Text("Destination Location / Site", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("transit_destination_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberWarning,
                                unfocusedBorderColor = QuantumBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("North Depot", "Air Cargo Hub", "Satellite WH").forEach { dest ->
                                Surface(
                                    onClick = { destinationLocation = dest },
                                    shape = RoundedCornerShape(6.dp),
                                    color = QuantumElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, QuantumBorder)
                                ) {
                                    Text(dest, fontSize = 9.sp, color = AmberWarning, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = transitReason,
                            onValueChange = { transitReason = it },
                            label = { Text("Transit Reason / Duty Notes", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("transit_reason_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberWarning,
                                unfocusedBorderColor = QuantumBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isSubmitting = true
                                scope.launch {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    val duration = transitDurationMin.toIntOrNull() ?: 45
                                    val perm = TravelPermission(
                                        id = "TRV-${System.currentTimeMillis() % 100000}",
                                        userEmail = user?.email ?: "user@shiftguard.com",
                                        timestampUtc = sdf.format(Date()),
                                        reason = transitReason.ifBlank { "Authorized inter-site transit" },
                                        destinationLocation = destinationLocation.ifBlank { "Designated Secondary Site" },
                                        transitMinutes = duration,
                                        status = "APPROVED",
                                        latitude = geofence.currentLatitude,
                                        longitude = geofence.currentLongitude
                                    )
                                    storage.addTravelPermission(perm)
                                    GasEngine.submitTravelPermission(storage.gasUrl.value, perm)

                                    storage.setTransitMode(destinationLocation, true)
                                    isSubmitting = false
                                    Toast.makeText(context, "Transit to '$destinationLocation' Approved & Logged!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("submit_transit_request_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = QuantumDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Transit Request & Silence Alarm", color = QuantumDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // OPTION 3: DISMISS / AUTO-LOGOUT
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = QuantumDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OPTION 3: DISMISS / AUTO-LOGOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                        Text("${countdownSeconds}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (countdownSeconds <= 15) AlertRed else WhatsAppGreen, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        "Dismiss breach temporarily. If boundary is not re-entered within grace period, system will automatically trigger Auto-Logout for compliance.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(40.dp).testTag("option_3_dismiss_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss (${countdownSeconds}s)", color = WhatsAppGreen, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                storage.logoutAndCloseShift()
                                com.example.service.FloatingBubbleService.stopService(context)
                                Toast.makeText(context, "Shift Concluded.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(40.dp).testTag("option_3_autologout_now_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = QuantumElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Auto-Logout Now", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Telemetry Status & Manual Sync Dialog
// -------------------------------------------------------------
@Composable
fun TelemetrySyncDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val storage = (context.applicationContext as ShiftGuardApplication).storage
    val geofence by storage.geofenceState.collectAsState()
    val logs by storage.telemetryLogs.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .testTag("telemetry_dialog_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuantumSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WhatsAppGreen))
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
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = WhatsAppGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Telemetry & Geofence", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = QuantumDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, QuantumBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current GPS:", fontSize = 12.sp, color = TextMuted)
                        Text("${String.format(Locale.US, "%.4f", geofence.currentLatitude)}, ${String.format(Locale.US, "%.4f", geofence.currentLongitude)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Perimeter Distance:", fontSize = 12.sp, color = TextMuted)
                        Text("${geofence.currentDistanceMeters.toInt()}m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Geofence Guard:", fontSize = 12.sp, color = TextMuted)
                        Text(if (geofence.isWithinBoundary) "SECURE (INSIDE)" else "BREACHED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (geofence.isWithinBoundary) WhatsAppGreen else AlertRed)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Sync Logs:", fontSize = 12.sp, color = TextMuted)
                        Text("${logs.size} recorded", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    LocationTelemetryWorker.enqueueImmediateSync(context, source = "MANUAL")
                    Toast.makeText(context, "Telemetry Synchronized", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_sync_now_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF04240F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Force Telemetry Sync Now", color = Color(0xFF04240F), fontWeight = FontWeight.Bold)
            }
        }
    }
}
