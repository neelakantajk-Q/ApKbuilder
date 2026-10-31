package com.example.ui.screens

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WebPortalScreen - Embeds the FieldOps HRMS Portal web app in a WebView.
 *
 * The portal URL is injected at build time via BuildConfig.PORTAL_URL (from .env).
 * This keeps the URL out of plaintext in the APK.
 *
 * Features:
 * - JavaScript enabled for full SPA functionality
 * - DOM storage for local data persistence
 * - Mixed content handling for dev/prod
 * - Cookie persistence across sessions
 * - Loading indicator
 * - Pull-to-refresh (handled by web app itself)
 * - Back navigation handled by WebView history
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WebPortalScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    onError: ((String) -> Unit)? = null,
    onPageFinished: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewClient by remember { mutableStateOf<PortalWebViewClient?>(null) }
    var loading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(BuildConfig.PORTAL_URL) }
    var canGoBack by remember { mutableStateOf(false) }

    // Initialize WebViewClient once
    if (webViewClient == null) {
        webViewClient = PortalWebViewClient(
            onPageStarted = { loading = true },
            onPageFinished = { url ->
                loading = false
                currentUrl = url
                canGoBack = false // Will be updated by WebView
                onPageFinished?.invoke(url)
            },
            onError = { error ->
                loading = false
                onError?.invoke(error)
            }
        )
    }

    Box(modifier = modifier.background(Color.White)) {
        // WebView wrapped in AndroidView
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

                    // Load initial URL from BuildConfig (injected from .env at build time)
                    val url = BuildConfig.PORTAL_URL
                    if (url.isNotEmpty() && url.startsWith("http")) {
                        loadUrl(url)
                    }
                }
            },
            update = { webView ->
                // Sync canGoBack state
                canGoBack = webView.canGoBack()
            }
        )

        // Loading overlay
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF25D366) // WhatsApp Green
                )
            }
        }
    }
}

/**
 * Custom WebViewClient to handle navigation, errors, and SSL
 */
private class PortalWebViewClient(
    private val onPageStarted: () -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onError: (String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        // Allow navigation within the portal domain
        if (url.startsWith("https://neelakantajk-q.github.io/fieldops-hrms-portal/") ||
            url.startsWith("https://script.google.com/")) {
            return false // Let WebView handle it
        }
        // External links - could open in browser if needed
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: android.webkit.WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        onError("Error ${error.errorCode}: ${error.description}")
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: android.webkit.SslErrorHandler?,
        error: android.net.http.SslError
    ) {
        // For production: handler?.cancel()
        // For dev: handler?.proceed() - allows self-signed certs on dev URLs
        if (BuildConfig.DEBUG) {
            handler?.proceed()
        } else {
            handler?.cancel()
            onError("SSL Error: ${error.primaryError}")
        }
    }
}

/**
 * WebChromeClient for progress, console logs, and permissions
 */
private class PortalWebChromeClient : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        // Progress handled via loading state in Composable
    }

    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("PortalWebView", "[Console] ${consoleMessage.messageLevel().name}: ${consoleMessage.message()}")
        }
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
        // Grant geolocation/camera permissions if web app requests them
        request?.let {
            it.grant(it.resources)
        }
    }
}

/**
 * Composable to handle hardware back button for WebView navigation
 */
@Composable
fun WebPortalBackHandler(
    webViewRef: org.jetbrains.android.AndroidView<WebView>?,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val webView = webViewRef?.view
                if (webView != null && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    onBackPressed()
                }
            }
        }
        context.findViewTreeLifecycleOwner()?.lifecycle?.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                    context.activity?.onBackPressedDispatcher?.addCallback(callback)
                } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    callback.remove()
                }
            }
        )
        onDispose {
            callback.remove()
        }
    }
}