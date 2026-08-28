package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.CrosshairRepository
import com.example.model.CrosshairConfig
import com.example.ui.components.CrosshairView
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalCanvas
import com.example.ui.theme.MinimalGreen
import com.example.ui.theme.MinimalPurple
import com.example.ui.theme.MinimalPurpleContainer
import com.example.ui.theme.MinimalPurpleDark
import com.example.ui.theme.MinimalPurpleLight
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CrosshairOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, SensorEventListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var calibrateContainer: FrameLayout? = null
    private var quickToggleContainer: FrameLayout? = null

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    private var isShakeHidden = false
    private var shakeRestoreJob: Job? = null

    private lateinit var repository: CrosshairRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var isCrosshairVisible = true
    private var isCalibrateHudVisible = false

    companion object {
        const val CHANNEL_ID = "crosshair_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY"
        const val ACTION_TOGGLE_VISIBILITY = "com.example.action.TOGGLE_VISIBILITY"
        const val ACTION_TOGGLE_CALIBRATE = "com.example.action.TOGGLE_CALIBRATE"

        fun start(context: Context) {
            val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleCalibrate(context: Context) {
            val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                action = ACTION_TOGGLE_CALIBRATE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        repository = CrosshairRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.let { sm ->
            proximitySensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
            accelerometerSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            proximitySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            lightSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            accelerometerSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Settings.canDrawOverlays(this)) {
            setupCrosshairOverlay()
            setupQuickToggleWidget()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Maintain screen density scaling and recalculate dynamic center point across orientation changes
        overlayContainer?.let { container ->
            val layoutParams = container.layoutParams as? WindowManager.LayoutParams
            if (layoutParams != null) {
                val config = repository.activeConfig.value
                layoutParams.gravity = Gravity.CENTER
                layoutParams.x = config.offsetX
                layoutParams.y = config.offsetY
                try {
                    windowManager?.updateViewLayout(container, layoutParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        quickToggleContainer?.let { container ->
            val layoutParams = container.layoutParams as? WindowManager.LayoutParams
            if (layoutParams != null) {
                layoutParams.gravity = Gravity.TOP or Gravity.START
                try {
                    windowManager?.updateViewLayout(container, layoutParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                repository.setOverlayActive(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_VISIBILITY -> {
                isCrosshairVisible = !isCrosshairVisible
                overlayContainer?.visibility = if (isCrosshairVisible) View.VISIBLE else View.GONE
                updateNotification()
            }
            ACTION_TOGGLE_CALIBRATE -> {
                toggleCalibrateHud()
            }
            else -> {
                repository.setOverlayActive(true)
            }
        }
        return START_STICKY
    }

    private fun setupCrosshairOverlay() {
        if (overlayContainer != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = repository.activeConfig.value.offsetX
            y = repository.activeConfig.value.offsetY
        }

        val frameLayout = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairOverlayService)
        }

        val composeView = ComposeView(this).apply {
            setContent {
                MyApplicationTheme {
                    val config by repository.activeConfig.collectAsState()
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CrosshairView(
                            config = config,
                            overrideSize = (config.sizeDp * 1.5f).dp
                        )
                    }
                }
            }
        }

        frameLayout.addView(composeView)
        overlayContainer = frameLayout

        try {
            windowManager?.addView(overlayContainer, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Observe position & config updates
        serviceScope.launch {
            repository.activeConfig.collectLatest { config ->
                overlayContainer?.let { container ->
                    val layoutParams = container.layoutParams as? WindowManager.LayoutParams
                    if (layoutParams != null) {
                        if (layoutParams.x != config.offsetX || layoutParams.y != config.offsetY) {
                            layoutParams.x = config.offsetX
                            layoutParams.y = config.offsetY
                            windowManager?.updateViewLayout(container, layoutParams)
                        }
                    }
                }
            }
        }
    }

    private fun toggleCalibrateHud() {
        if (!Settings.canDrawOverlays(this)) return

        if (isCalibrateHudVisible) {
            calibrateContainer?.let {
                try {
                    windowManager?.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            calibrateContainer = null
            isCalibrateHudVisible = false
        } else {
            showCalibrateHud()
            isCalibrateHudVisible = true
        }
    }

    private fun showCalibrateHud() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        val frameLayout = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairOverlayService)
        }

        val composeView = ComposeView(this).apply {
            setContent {
                MyApplicationTheme {
                    val config by repository.activeConfig.collectAsState()
                    CalibrateFloatingWidget(
                        config = config,
                        onNudge = { dx, dy ->
                            repository.updateActiveConfig(
                                config.copy(
                                    offsetX = config.offsetX + dx,
                                    offsetY = config.offsetY + dy
                                )
                            )
                        },
                        onReset = {
                            repository.updateActiveConfig(
                                config.copy(offsetX = 0, offsetY = 0)
                            )
                        },
                        onClose = {
                            toggleCalibrateHud()
                        }
                    )
                }
            }
        }

        frameLayout.addView(composeView)
        calibrateContainer = frameLayout

        try {
            windowManager?.addView(calibrateContainer, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Crosshair Pro Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for floating game crosshair overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_TOGGLE_VISIBILITY
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calibrateIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_TOGGLE_CALIBRATE
        }
        val calibratePendingIntent = PendingIntent.getService(
            this, 2, calibrateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeName = repository.activeConfig.value.name

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Crosshair Pro: Active")
            .setContentText("Current Reticle: $activeName (100% Free & No Ads)")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                if (isCrosshairVisible) "Hide HUD" else "Show HUD",
                togglePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_crop,
                "Calibrate",
                calibratePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Exit",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        shakeRestoreJob?.cancel()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        overlayContainer?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayContainer = null

        calibrateContainer?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        calibrateContainer = null

        quickToggleContainer?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        quickToggleContainer = null

        repository.setOverlayActive(false)
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val stats = repository.usageStats.value

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                if (stats.proximityAutoHideEnabled) {
                    val distance = event.values.getOrNull(0) ?: 10f
                    val maxRange = event.sensor.maximumRange
                    val isNear = distance < maxRange && distance < 5.0f
                    if (isNear) {
                        overlayContainer?.visibility = View.GONE
                    } else if (isCrosshairVisible && !isShakeHidden) {
                        overlayContainer?.visibility = View.VISIBLE
                    }
                }
            }
            Sensor.TYPE_LIGHT -> {
                if (stats.autoLightOpacityEnabled) {
                    val lux = event.values.getOrNull(0) ?: 300f
                    val baseOpacity = repository.activeConfig.value.opacity
                    val adjustedAlpha = when {
                        lux < 50f -> (baseOpacity * 0.65f).coerceIn(0.25f, 1.0f)
                        lux > 500f -> 1.0f
                        else -> baseOpacity
                    }
                    overlayContainer?.alpha = adjustedAlpha
                } else {
                    overlayContainer?.alpha = 1.0f
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (stats.shakeAutoToggleEnabled) {
                    val x = event.values.getOrNull(0) ?: 0f
                    val y = event.values.getOrNull(1) ?: 0f
                    val z = event.values.getOrNull(2) ?: 0f
                    val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH
                    if (gForce > 13.5f) {
                        if (!isShakeHidden) {
                            isShakeHidden = true
                            overlayContainer?.visibility = View.GONE
                            shakeRestoreJob?.cancel()
                            shakeRestoreJob = serviceScope.launch {
                                delay(2500)
                                isShakeHidden = false
                                if (isCrosshairVisible) {
                                    overlayContainer?.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun setupQuickToggleWidget() {
        if (quickToggleContainer != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 240
        }

        val frameLayout = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairOverlayService)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        frameLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(frameLayout, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        isCrosshairVisible = !isCrosshairVisible
                        overlayContainer?.visibility = if (isCrosshairVisible) View.VISIBLE else View.GONE
                        updateNotification()
                        // Force refresh UI in quick toggle
                        setupQuickToggleContent(frameLayout)
                    }
                    true
                }
                else -> false
            }
        }

        setupQuickToggleContent(frameLayout)
        quickToggleContainer = frameLayout

        try {
            windowManager?.addView(quickToggleContainer, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupQuickToggleContent(frameLayout: FrameLayout) {
        frameLayout.removeAllViews()
        val composeView = ComposeView(this).apply {
            setContent {
                MyApplicationTheme {
                    PersistentFloatingQuickToggle(
                        isCrosshairVisible = isCrosshairVisible,
                        onOpenApp = {
                            val intent = Intent(this@CrosshairOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
        frameLayout.addView(composeView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun PersistentFloatingQuickToggle(
    isCrosshairVisible: Boolean,
    onOpenApp: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MinimalSurface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalPurpleLight),
        shadowElevation = 8.dp,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // LED Indicator Circle
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isCrosshairVisible) MinimalGreen else Color.Gray)
            )

            // Crosshair Icon
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Quick Toggle HUD",
                tint = if (isCrosshairVisible) MinimalPurpleDark else TextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = if (isCrosshairVisible) "HUD ON" else "HUD OFF",
                color = if (isCrosshairVisible) TextPrimary else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CalibrateFloatingWidget(
    config: CrosshairConfig,
    onNudge: (dx: Int, dy: Int) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MinimalSurface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        shadowElevation = 10.dp,
        modifier = Modifier.padding(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(220.dp)
            ) {
                Text(
                    text = "CALIBRATION HUD",
                    color = MinimalPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "X:${config.offsetX} Y:${config.offsetY}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close calibration HUD",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // D-Pad Controller
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // UP
                NudgeButton(icon = Icons.Default.KeyboardArrowUp, onClick = { onNudge(0, -2) })

                // LEFT, RESET/LOCK, RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NudgeButton(icon = Icons.Default.KeyboardArrowLeft, onClick = { onNudge(-2, 0) })

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MinimalPurpleContainer)
                            .border(1.dp, MinimalPurpleLight, CircleShape)
                            .clickable { onReset() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset offset to center",
                            tint = MinimalPurpleDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    NudgeButton(icon = Icons.Default.KeyboardArrowRight, onClick = { onNudge(2, 0) })
                }

                // DOWN
                NudgeButton(icon = Icons.Default.KeyboardArrowDown, onClick = { onNudge(0, 2) })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MinimalPurple)
                    .clickable { onClose() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Lock aim position",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "LOCK POSITION",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MinimalSurfaceContainer)
            .border(1.dp, MinimalBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Nudge aim",
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
