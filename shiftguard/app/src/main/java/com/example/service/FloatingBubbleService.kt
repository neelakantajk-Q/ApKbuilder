package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ShiftGuardApplication
import com.example.data.local.ShiftStorage
import com.example.data.model.ActiveJobState
import com.example.data.model.GeofenceState
import com.example.ui.overlay.BubbleActionActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FloatingBubbleService : Service() {

    companion object {
        private const val TAG = "FloatingBubbleService"
        private const val NOTIFICATION_ID = 4001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SIMULATE_BREACH = "ACTION_SIMULATE_BREACH"

        fun startService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var storage: ShiftStorage
    private lateinit var fusedLocation: FusedLocationProviderClient

    // Bubble 1: Job Timer & OCR
    private var bubble1View: View? = null
    private var bubble1Params: WindowManager.LayoutParams? = null

    // Bubble 2: Geofence Guard & Telemetry Sync
    private var bubble2View: View? = null
    private var bubble2Params: WindowManager.LayoutParams? = null

    // Timer handler for active job
    private val timerHandler = Handler(Looper.getMainLooper())
    private var hasAlertedOvertime = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            val active = storage.activeJob.value
            if (active.isRunning) {
                val elapsed = (System.currentTimeMillis() - active.startTimeMillis) / 1000L
                val currentElapsed = if (elapsed > 0) elapsed else 0
                storage.updateJobElapsed(currentElapsed)
                val updated = active.copy(elapsedSeconds = currentElapsed)
                checkOvertimeWarning(updated)
                updateBubble1Display(updated)
                timerHandler.postDelayed(this, 1000L)
            } else {
                hasAlertedOvertime = false
            }
        }
    }

    private fun checkOvertimeWarning(active: ActiveJobState) {
        if (active.elapsedSeconds >= 3600 && !hasAlertedOvertime) {
            hasAlertedOvertime = true
            triggerOvertimeNotification(active)
        } else if (active.elapsedSeconds < 3600) {
            hasAlertedOvertime = false
        }
    }

    private fun triggerOvertimeNotification(active: ActiveJobState) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration warning error", e)
        }

        try {
            val notification = NotificationCompat.Builder(this, ShiftGuardApplication.CHANNEL_BUBBLE_SERVICE)
                .setContentTitle("Overtime Alert: Active > 1 hr")
                .setContentText("Job timer for ${active.licensePlate} has exceeded 60 minutes.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NOTIFICATION_ID + 10, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Notification alert error", e)
        }
    }

    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        storage = (application as ShiftGuardApplication).storage
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        storage.setBubbleServiceRunning(true)

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        initFloatingBubbles()
        observeStateChanges()
        startLocationGuard()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SIMULATE_BREACH -> {
                triggerSimulatedBreach()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        timerHandler.removeCallbacks(timerRunnable)
        stopLocationGuard()
        storage.setBubbleServiceRunning(false)
        removeBubbleViews()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ShiftGuardApplication.CHANNEL_BUBBLE_SERVICE)
            .setContentTitle("Quantum Field Operations Active")
            .setContentText("Persistent WhatsApp-Green Floating Bubbles running globally on screen")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // -------------------------------------------------------------
    // Bubble Initialization & WindowManager Attachment
    // -------------------------------------------------------------
    private fun initFloatingBubbles() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot draw overlays! Permission not granted.")
            return
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // BUBBLE 1: Job Timer & OCR (Starts at right edge)
        bubble1Params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - dpToPx(100)
            y = dpToPx(130)
        }
        bubble1View = createBubble1View()
        windowManager.addView(bubble1View, bubble1Params)
        attachDragListener(bubble1View!!, bubble1Params!!) {
            onBubble1Click()
        }

        // BUBBLE 2: Geofence Guard (Starts directly below Bubble 1)
        bubble2Params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - dpToPx(100)
            y = dpToPx(245)
        }
        bubble2View = createBubble2View()
        windowManager.addView(bubble2View, bubble2Params)
        attachDragListener(bubble2View!!, bubble2Params!!) {
            onBubble2Click()
        }
    }

    // -------------------------------------------------------------
    // Bubble 1 View Construction & Rapido Style Animation
    // -------------------------------------------------------------
    private fun createBubble1View(): View {
        val root = FrameLayout(this)
        root.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
        updateBubble1Layout(root, storage.activeJob.value)
        return root
    }

    private fun updateBubble1Display(active: ActiveJobState) {
        val root = bubble1View as? FrameLayout ?: return
        updateBubble1Layout(root, active)
    }

    private fun updateBubble1Layout(root: FrameLayout, active: ActiveJobState) {
        root.removeAllViews()

        if (!active.isRunning) {
            // RAPIDO CAPTAIN STYLE IDLE BUBBLE (WhatsApp Green)
            // Container with concentric breathing wave ring
            val container = FrameLayout(this)
            val outerSize = dpToPx(76)
            container.layoutParams = FrameLayout.LayoutParams(outerSize, outerSize)

            // Rapido breathing wave ripple ring
            val rippleRing = View(this).apply {
                val rSize = dpToPx(72)
                layoutParams = FrameLayout.LayoutParams(rSize, rSize).apply {
                    gravity = Gravity.CENTER
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dpToPx(2), Color.parseColor("#25D366"))
                    setColor(Color.parseColor("#1525D366"))
                }

                // Smooth double-ring breathing wave animation
                val pulseAnim = AnimationSet(true).apply {
                    addAnimation(ScaleAnimation(0.92f, 1.18f, 0.92f, 1.18f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                        duration = 1400
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    })
                    addAnimation(AlphaAnimation(0.35f, 0.85f).apply {
                        duration = 1400
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    })
                }
                startAnimation(pulseAnim)
            }

            // Central WhatsApp Green Captain Circle
            val circle = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val size = dpToPx(60)
                layoutParams = FrameLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER
                }
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.parseColor("#25D366"), Color.parseColor("#0E9F56"))
                ).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dpToPx(2), Color.WHITE)
                }
                elevation = 18f
            }

            val icon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_agenda)
                setColorFilter(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
            }

            val label = TextView(this).apply {
                text = "JOB"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
            }

            circle.addView(icon)
            circle.addView(label)

            container.addView(rippleRing)
            container.addView(circle)
            root.addView(container)
        } else {
            // ACTIVE STATE: Job Timer On Pill with Overtime Support (> 1 hr)
            val isOvertime = active.elapsedSeconds >= 3600
            val accentColor = if (isOvertime) Color.parseColor("#F59E0B") else Color.parseColor("#25D366")
            val darkSlateBg = if (isOvertime) Color.parseColor("#261705") else Color.parseColor("#0B1220")

            val pill = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
                layoutParams = FrameLayout.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(22).toFloat()
                    setColor(darkSlateBg)
                    setStroke(dpToPx(2), accentColor)
                }
                elevation = 22f

                // Continuous breathing glow
                val pulseAnim = AlphaAnimation(0.85f, 1.0f).apply {
                    duration = if (isOvertime) 450 else 800
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
                startAnimation(pulseAnim)
            }

            // Top Status Row: Glowing Beacon + "JOB TIMER ON" or "OVERTIME (> 1h)"
            val statusRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val beaconDot = View(this).apply {
                val dSize = dpToPx(8)
                layoutParams = LinearLayout.LayoutParams(dSize, dSize).apply {
                    marginEnd = dpToPx(6)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(accentColor)
                }
                val dotBlink = AlphaAnimation(0.2f, 1.0f).apply {
                    duration = if (isOvertime) 350 else 500
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
                startAnimation(dotBlink)
            }

            val timerStatusLabel = TextView(this).apply {
                text = if (isOvertime) "OVERTIME (> 1h)" else "JOB TIMER ON"
                setTextColor(accentColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
            }
            statusRow.addView(beaconDot)
            statusRow.addView(timerStatusLabel)

            // Vehicle Plate Text
            val plateText = TextView(this).apply {
                text = active.licensePlate.ifEmpty { "ACTIVE JOB" }
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }

            // Running digital timer clock
            val timerText = TextView(this).apply {
                val hours = active.elapsedSeconds / 3600
                val mins = (active.elapsedSeconds % 3600) / 60
                val secs = active.elapsedSeconds % 60
                text = String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
                setTextColor(if (isOvertime) Color.parseColor("#F59E0B") else Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                typeface = Typeface.MONOSPACE
            }

            val stopHint = TextView(this).apply {
                text = "Tap to Complete"
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                typeface = Typeface.DEFAULT_BOLD
            }

            pill.addView(statusRow)
            pill.addView(plateText)
            pill.addView(timerText)
            pill.addView(stopHint)
            root.addView(pill)
        }
    }

    private fun onBubble1Click() {
        val active = storage.activeJob.value
        if (!active.isRunning) {
            // Trigger Job Start: ML Kit License Plate Recognition
            val intent = Intent(this, BubbleActionActivity::class.java).apply {
                action = BubbleActionActivity.ACTION_START_JOB_OCR
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } else {
            // Stop Job: Document Proof scanner & B&W processing
            val intent = Intent(this, BubbleActionActivity::class.java).apply {
                action = BubbleActionActivity.ACTION_STOP_JOB_DOCUMENT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
    }

    // -------------------------------------------------------------
    // Bubble 2 View Construction & Geofence Alert States (WhatsApp Green)
    // -------------------------------------------------------------
    private fun createBubble2View(): View {
        val root = FrameLayout(this)
        root.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
        updateBubble2Layout(root, storage.geofenceState.value)
        return root
    }

    private fun updateBubble2Layout(root: FrameLayout, geofence: GeofenceState) {
        root.removeAllViews()

        val isBreach = geofence.breachActive
        val mainColor = if (isBreach) Color.parseColor("#EF4444") else Color.parseColor("#25D366")
        val darkBgColor = if (isBreach) Color.parseColor("#3B1212") else Color.parseColor("#0B1220")

        val container = FrameLayout(this)
        val outerSize = dpToPx(76)
        container.layoutParams = FrameLayout.LayoutParams(outerSize, outerSize)

        // Rapido style pulse ring
        val rippleRing = View(this).apply {
            val rSize = dpToPx(72)
            layoutParams = FrameLayout.LayoutParams(rSize, rSize).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dpToPx(2), mainColor)
                setColor(if (isBreach) Color.parseColor("#33EF4444") else Color.parseColor("#1525D366"))
            }

            val pulseAnim = AnimationSet(true).apply {
                addAnimation(ScaleAnimation(0.92f, 1.18f, 0.92f, 1.18f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                    duration = if (isBreach) 500 else 1400
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                })
                addAnimation(AlphaAnimation(0.35f, 0.9f).apply {
                    duration = if (isBreach) 500 else 1400
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                })
            }
            startAnimation(pulseAnim)
        }

        val circle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val size = dpToPx(60)
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
            background = if (!isBreach) {
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.parseColor("#25D366"), Color.parseColor("#0E9F56"))
                ).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dpToPx(2), Color.WHITE)
                }
            } else {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(darkBgColor)
                    setStroke(dpToPx(2), mainColor)
                }
            }
            elevation = 18f
        }

        val icon = ImageView(this).apply {
            val iconRes = if (isBreach) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_menu_compass
            setImageResource(iconRes)
            setColorFilter(if (isBreach) mainColor else Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
        }

        val label = TextView(this).apply {
            text = if (isBreach) "ALERT!" else "GUARD"
            setTextColor(if (isBreach) mainColor else Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
        }

        circle.addView(icon)
        circle.addView(label)

        container.addView(rippleRing)
        container.addView(circle)
        root.addView(container)
    }

    private fun onBubble2Click() {
        val geofence = storage.geofenceState.value
        val intent = Intent(this, BubbleActionActivity::class.java).apply {
            action = if (geofence.breachActive) {
                BubbleActionActivity.ACTION_GEOFENCE_BREACH
            } else {
                BubbleActionActivity.ACTION_TELEMETRY_SYNC
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    // -------------------------------------------------------------
    // Touch Dragging & Smooth Movement
    // -------------------------------------------------------------
    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener(
        view: View,
        params: WindowManager.LayoutParams,
        onClick: () -> Unit
    ) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > dpToPx(6) || abs(dy) > dpToPx(6)) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (e: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            onClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    // -------------------------------------------------------------
    // Geofence & Location Telemetry Guard
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private fun startLocationGuard() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000L)
                .setMinUpdateDistanceMeters(20f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    evaluateGeofencePosition(loc.latitude, loc.longitude)
                }
            }
            fusedLocation.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing in foreground service", e)
        }
    }

    private fun stopLocationGuard() {
        locationCallback?.let {
            fusedLocation.removeLocationUpdates(it)
        }
    }

    private fun evaluateGeofencePosition(lat: Double, lng: Double) {
        val user = storage.userProfile.value ?: return
        val results = FloatArray(1)
        Location.distanceBetween(lat, lng, user.geofenceLat, user.geofenceLng, results)
        val distance = results[0].toDouble()
        val isInside = distance <= user.geofenceRadiusMeters
        val isWithinShift = isCurrentTimeWithinShift(user.shiftStart, user.shiftEnd)
        val isBreach = !isInside

        storage.updateGeofence(
            isWithinShift = isWithinShift,
            isWithinBoundary = isInside,
            distanceMeters = distance,
            latitude = lat,
            longitude = lng,
            breachActive = isBreach
        )
    }

    private fun isCurrentTimeWithinShift(startStr: String, endStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.US)
            val nowStr = format.format(Date())
            val now = format.parse(nowStr)
            val start = format.parse(startStr)
            val end = format.parse(endStr)
            if (now != null && start != null && end != null) {
                if (end.after(start)) now in start..end else (now.after(start) || now.before(end))
            } else true
        } catch (e: Exception) {
            true
        }
    }

    private fun triggerSimulatedBreach() {
        val current = storage.geofenceState.value
        storage.updateGeofence(
            isWithinShift = current.isWithinShift,
            isWithinBoundary = false,
            distanceMeters = 1850.0,
            latitude = current.currentLatitude + 0.015,
            longitude = current.currentLongitude + 0.015,
            breachActive = true
        )
    }

    // -------------------------------------------------------------
    // Observability & Clean-up
    // -------------------------------------------------------------
    private fun observeStateChanges() {
        serviceScope.launch {
            storage.activeJob.collectLatest { active ->
                if (active.isRunning) {
                    timerHandler.removeCallbacks(timerRunnable)
                    timerHandler.post(timerRunnable)
                } else {
                    timerHandler.removeCallbacks(timerRunnable)
                    updateBubble1Display(active)
                }
            }
        }

        serviceScope.launch {
            storage.geofenceState.collectLatest { geofence ->
                bubble2View?.let { root ->
                    updateBubble2Layout(root as FrameLayout, geofence)
                }
            }
        }
    }

    private fun removeBubbleViews() {
        bubble1View?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            bubble1View = null
        }
        bubble2View?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            bubble2View = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
