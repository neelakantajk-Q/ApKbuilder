package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.local.ShiftStorage
import com.example.service.FloatingBubbleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppRoot()
            }
        }
    }
}

/**
 * JavaScript Interface exposed to the Web Portal SPA
 * Allows the web app to call native Android functionality
 */
class PortalJavascriptInterface(
    private val activity: MainActivity,
    private val storage: ShiftStorage
) {

    @JavascriptInterface
    fun getUserProfile(): String {
        val profile = storage.userProfile.value
        return if (profile != null) {
            """{"email":"${profile.email}","name":"${profile.name}","siteName":"${profile.siteName}","geofenceLat":${profile.geofenceLat},"geofenceLng":${profile.geofenceLng}}"""
        } else "null"
    }

    @JavascriptInterface
    fun startShift() {
        activity.runOnUiThread {
            storage.startShift()
            Toast.makeText(activity, "Shift started", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun endShift() {
        activity.runOnUiThread {
            storage.endShift()
            Toast.makeText(activity, "Shift ended", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun checkIn(lat: Double, lng: Double, accuracy: Float) {
        activity.runOnUiThread {
            storage.recordAttendance(lat, lng, accuracy)
            Toast.makeText(activity, "Checked in", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun checkOut(lat: Double, lng: Double, accuracy: Float) {
        activity.runOnUiThread {
            storage.recordAttendance(lat, lng, accuracy, isCheckOut = true)
            Toast.makeText(activity, "Checked out", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun startJob(jobId: String, lat: Double, lng: Double) {
        activity.runOnUiThread {
            storage.startJob(jobId, lat, lng)
        }
    }

    @JavascriptInterface
    fun completeJob(jobId: String) {
        activity.runOnUiThread {
            storage.completeJob(jobId)
        }
    }

    @JavascriptInterface
    fun getActiveJob(): String {
        val job = storage.activeJob.value
        return if (job.isRunning) {
            """{"id":"${job.jobId}","startLat":${job.startLat},"startLng":${job.startLng}}"""
        } else "null"
    }

    @JavascriptInterface
    fun getGeofenceState(): String {
        val gf = storage.geofenceState.value
        return """{"inside":${gf.inside},"breachActive":${gf.breachActive},"siteLat":${gf.siteLat},"siteLng":${gf.siteLng},"radiusMeters":${gf.radiusMeters}}"""
    }

    @JavascriptInterface
    fun openNativeMaps(lat: Double, lng: Double, label: String) {
        activity.runOnUiThread {
            val uri = Uri.parse("google.navigation:q=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun openNativeDialer(phoneNumber: String) {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            activity.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun startFloatingBubble() {
        activity.runOnUiThread {
            if (Settings.canDrawOverlays(activity)) {
                FloatingBubbleService.startService(activity)
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
                activity.startActivity(intent)
            }
        }
    }

    @JavascriptInterface
    fun stopFloatingBubble() {
        activity.runOnUiThread {
            FloatingBubbleService.stopService(activity)
        }
    }

    @JavascriptInterface
    fun isBubbleRunning(): Boolean {
        return storage.isBubbleServiceRunning.value
    }

    @JavascriptInterface
    fun scanLicensePlate(base64Image: String): String {
        return "SCANNING..."
    }

    @JavascriptInterface
    fun logout() {
        activity.runOnUiThread {
            storage.logoutAndCloseShift()
            FloatingBubbleService.stopService(activity)
        }
    }

    @JavascriptInterface
    fun getShiftStatus(): String {
        val shift = storage.currentShift.value
        return if (shift != null) {
            """{"active":true,"startTime":${shift.startTime},"siteName":"${shift.siteName}"}"""
        } else """{"active":false}"""
    }
}

/**
 * Main App Root - Single WebView shell for the SPA portal.
 * No native login gate; the portal handles auth internally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as ShiftGuardApplication
    val storage = app.storage

    val isBubbleRunning by storage.isBubbleServiceRunning.collectAsState()

    var showSupportSheet by remember { mutableStateOf(false) }

    // Request permissions on startup
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    // Entire app is the Web Portal WebView
    Box(modifier = Modifier.fillMaxSize()) {
        WebPortalScreen(
            modifier = Modifier.fillMaxSize(),
            javascriptInterface = PortalJavascriptInterface(this@MainActivity, storage),
            onError = { error ->
                Toast.makeText(context, "Portal load error: $error", Toast.LENGTH_LONG).show()
            }
        )

        // Minimal floating bubble status indicator (top-right)
        if (isBubbleRunning) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = com.example.ui.theme.WhatsAppGreen,
                    elevation = 4.dp
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }

    // Support Modal Bottom Sheet (native UI for emergency contacts)
    if (showSupportSheet) {
        // Portal can trigger this via AndroidBridge if needed
    }

    // 12:00 AM Midnight Re-Authentication Modal (native security layer)
    // Portal can request this via AndroidBridge if needed
}

/**
 * WebPortalScreen - The ENTIRE app is the web portal SPA.
 * Login, routing, and all UI happen inside the WebView.
 */
@Composable
fun WebPortalScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    javascriptInterface: PortalJavascriptInterface,
    onError: ((String) -> Unit)? = null,
    onPageFinished: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewClient by remember { mutableStateOf<PortalWebViewClient?>(null) }
    var loading by remember { mutableStateOf(true) }

    if (webViewClient == null) {
        webViewClient = PortalWebViewClient(
            onPageStarted = { loading = true },
            onPageFinished = { url ->
                loading = false
                onPageFinished?.invoke(url)
            },
            onError = { error ->
                loading = false
                onError?.invoke(error)
            }
        )
    }

    Box(modifier = modifier.background(Color.White)) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        allowFileAccess = false
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        supportZoom = false
                        userAgentString = "${settings.userAgentString} ShiftGuard/1.0"
                    }
                    webViewClient = webViewClient!!
                    webChromeClient = PortalWebChromeClient()

                    // Inject JavaScript interface - exposes native Android to web portal
                    addJavascriptInterface(javascriptInterface, "AndroidBridge")

                    val url = BuildConfig.PORTAL_URL
                    if (url.isNotEmpty() && url.startsWith("http")) {
                        loadUrl(url)
                    }
                }
            },
            update = { /* no-op */ }
        )

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF25D366)
                )
            }
        }
    }
}

private class PortalWebViewClient(
    private val onPageStarted: () -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onError: (String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        // Keep navigation within portal or Google Apps Script
        return !url.startsWith("https://neelakantajk-q.github.io/fieldops-hrms-portal/") &&
               !url.startsWith("https://script.google.com/")
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it) }
        // Signal bridge readiness to the SPA
        view?.evaluateJavascript(
            "window.AndroidBridgeReady = true;" +
            "if (window.onAndroidBridgeReady) window.onAndroidBridgeReady();",
            null
        )
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError) {
        super.onReceivedError(view, request, error)
        onError("Error ${error.errorCode}: ${error.description}")
    }

    override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError) {
        if (BuildConfig.DEBUG) handler?.proceed() else { handler?.cancel(); onError("SSL Error: ${error.primaryError}") }
    }
}

private class PortalWebChromeClient : WebChromeClient() {
    override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
        request?.grant(request.resources)
    }
}