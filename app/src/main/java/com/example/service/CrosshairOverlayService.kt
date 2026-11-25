package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.data.analytics.AppEventTracker
import com.example.data.analytics.AppEventType
import com.example.model.CrosshairConfig
import com.example.ui.components.CrosshairView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class CrosshairOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, SensorEventListener {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: CrosshairRepository
    private lateinit var eventTracker: AppEventTracker

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    private var isProximityHidden = false
    private var isShakeHidden = false
    private var shakeRestoreJob: Job? = null
    private var dynamicOpacityFactor = MutableStateFlow(1f)
    private var currentActiveConfig: CrosshairConfig? = null

    private var crosshairView: ComposeView? = null
    private var crosshairParams: WindowManager.LayoutParams? = null

    private var calibrateView: View? = null
    private var calibrateParams: WindowManager.LayoutParams? = null

    private var quickToggleView: View? = null
    private var quickToggleParams: WindowManager.LayoutParams? = null

    private val isCrosshairVisibleState = mutableStateOf(true)
    private var isCalibrateMode = false
    private var lastNotifiedReticleTitle: String? = null
    private var lastNotifiedVisibility: Boolean? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val CHANNEL_ID = "crosshair_pro_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_OVERLAY"
        const val ACTION_STOP = "ACTION_STOP_OVERLAY"
        const val ACTION_TOGGLE_VISIBILITY = "ACTION_TOGGLE_VISIBILITY"
        const val ACTION_TOGGLE_CALIBRATE = "ACTION_TOGGLE_CALIBRATE"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
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
            context.stopService(Intent(context, CrosshairOverlayService::class.java))
        }

        fun toggleCalibrate(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                action = ACTION_TOGGLE_CALIBRATE
            }
            if (isServiceRunning) {
                context.startService(intent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = CrosshairRepository.getInstance(applicationContext)
        eventTracker = AppEventTracker.getInstance(applicationContext)

        createNotificationChannel()
        startForegroundCompat(buildNotification(repository.activeConfig.value))

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        initSensors()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        showCrosshairOverlay()
        showQuickToggleFloatingWidget()

        serviceScope.launch {
            repository.activeConfig.collectLatest { config ->
                currentActiveConfig = config
                updateCrosshairPosition(config)
                updateNotificationIfChanged(config)
            }
        }

        serviceScope.launch {
            repository.usageStats.collectLatest { stats ->
                if (!stats.proximityAutoHideEnabled && isProximityHidden) {
                    isProximityHidden = false
                    updateEffectiveVisibility()
                }
                if (!stats.shakeAutoToggleEnabled && isShakeHidden) {
                    isShakeHidden = false
                    updateEffectiveVisibility()
                }
                if (!stats.autoLightOpacityEnabled) {
                    dynamicOpacityFactor.value = 1f
                }
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.let { sm ->
            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
            proximitySensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            lightSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            proximitySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val stats = repository.usageStats.value

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                if (stats.proximityAutoHideEnabled) {
                    val distance = event.values[0]
                    val maxRange = event.sensor.maximumRange
                    val isNear = distance < maxRange && distance < 5f
                    if (isNear != isProximityHidden) {
                        isProximityHidden = isNear
                        updateEffectiveVisibility()
                    }
                }
            }

            Sensor.TYPE_LIGHT -> {
                if (stats.autoLightOpacityEnabled) {
                    val lux = event.values[0]
                    val targetFactor = when {
                        lux < 15f -> 0.55f
                        lux < 100f -> 0.80f
                        else -> 1.0f
                    }
                    if (dynamicOpacityFactor.value != targetFactor) {
                        dynamicOpacityFactor.value = targetFactor
                    }
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (stats.shakeAutoToggleEnabled) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gX = x / SensorManager.GRAVITY_EARTH
                    val gY = y / SensorManager.GRAVITY_EARTH
                    val gZ = z / SensorManager.GRAVITY_EARTH
                    val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble())

                    if (gForce > 2.8) {
                        triggerShakeHide()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun triggerShakeHide() {
        isShakeHidden = true
        updateEffectiveVisibility()

        shakeRestoreJob?.cancel()
        shakeRestoreJob = serviceScope.launch {
            delay(2500L)
            isShakeHidden = false
            updateEffectiveVisibility()
        }
    }

    private fun updateEffectiveVisibility() {
        val effectiveVisible = isCrosshairVisibleState.value && !isProximityHidden && !isShakeHidden
        crosshairView?.visibility = if (effectiveVisible) View.VISIBLE else View.GONE
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP -> {
                repository.setOverlayActive(false)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_VISIBILITY -> {
                isCrosshairVisibleState.value = !isCrosshairVisibleState.value
                updateEffectiveVisibility()
                updateNotification(repository.activeConfig.value)
            }

            ACTION_TOGGLE_CALIBRATE -> {
                isCalibrateMode = !isCalibrateMode
                eventTracker.logEvent(
                    AppEventType.OVERLAY_CALIBRATION_TOGGLED,
                    "overlay",
                    mapOf("enabled" to isCalibrateMode)
                )
                if (isCalibrateMode) {
                    showCalibrateWidget()
                } else {
                    hideCalibrateWidget()
                }
            }

            else -> {
                repository.setOverlayActive(true)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        sensorManager?.unregisterListener(this)
        shakeRestoreJob?.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        crosshairView?.let { runCatching { windowManager.removeView(it) } }
        calibrateView?.let { runCatching { windowManager.removeView(it) } }
        quickToggleView?.let { runCatching { windowManager.removeView(it) } }
        crosshairView = null
        calibrateView = null
        quickToggleView = null

        repository.setOverlayActive(false)
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(config: CrosshairConfig): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleVisIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_TOGGLE_VISIBILITY
        }
        val toggleVisPending = PendingIntent.getService(
            this,
            1,
            toggleVisIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calibrateIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_TOGGLE_CALIBRATE
        }
        val calibratePending = PendingIntent.getService(
            this,
            2,
            calibrateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val visibilityActionText = if (isCrosshairVisibleState.value) {
            getString(R.string.notification_hide_hud)
        } else {
            getString(R.string.notification_show_hud)
        }

        val reticleTitle = com.example.model.PredefinedCrosshairCatalog.resolveDisplayName(this, config)
        lastNotifiedReticleTitle = reticleTitle
        lastNotifiedVisibility = isCrosshairVisibleState.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_active_title, getString(R.string.app_name)))
            .setContentText(getString(R.string.notification_active_reticle, reticleTitle))
            .setSmallIcon(R.drawable.ic_stat_crosshair)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPending)
            .addAction(0, visibilityActionText, toggleVisPending)
            .addAction(0, getString(R.string.notification_calibrate), calibratePending)
            .addAction(0, getString(R.string.notification_exit), stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotificationIfChanged(config: CrosshairConfig) {
        val reticleTitle = com.example.model.PredefinedCrosshairCatalog.resolveDisplayName(this, config)
        val visible = isCrosshairVisibleState.value
        if (reticleTitle == lastNotifiedReticleTitle && visible == lastNotifiedVisibility) return
        updateNotification(config)
    }

    private fun updateNotification(config: CrosshairConfig) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(config))
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun showCrosshairOverlay() {
        val config = repository.activeConfig.value
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = config.offsetX
            y = config.offsetY
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val activeConfig by repository.activeConfig.collectAsState()
                val opacityFactor by dynamicOpacityFactor.collectAsState()
                val isDark by repository.isDarkMode.collectAsState()
                val effectiveConfig = activeConfig.copy(
                    opacity = (activeConfig.opacity * opacityFactor).coerceIn(0.1f, 1.0f)
                )
                MyApplicationTheme(darkTheme = isDark) {
                    CrosshairView(config = effectiveConfig)
                }
            }
        }

        crosshairView = view
        crosshairParams = params

        runCatching {
            windowManager.addView(view, params)
        }
    }

    private fun updateCrosshairPosition(config: CrosshairConfig) {
        val params = crosshairParams ?: return
        val view = crosshairView ?: return
        if (params.x != config.offsetX || params.y != config.offsetY) {
            params.x = config.offsetX
            params.y = config.offsetY
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun showCalibrateWidget() {
        if (calibrateView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 32
            y = 120
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val config by repository.activeConfig.collectAsState()
                val savedPresets by repository.savedPresets.collectAsState()
                val isDark by repository.isDarkMode.collectAsState()
                MyApplicationTheme(darkTheme = isDark) {
                    CalibrateFloatingWidget(
                        config = config,
                        savedPresets = savedPresets,
                        onSelectPreset = { preset ->
                            repository.updateActiveConfig(preset)
                            eventTracker.logEvent(
                                AppEventType.PRESET_EQUIPPED,
                                "overlay",
                                mapOf("preset_name" to preset.name, "style" to preset.style.name, "source" to "hud_panel")
                            )
                        },
                        onNudge = { dx, dy ->
                            val updated = config.copy(
                                offsetX = config.offsetX + dx,
                                offsetY = config.offsetY + dy
                            )
                            repository.updateActiveConfig(updated)
                        },
                        onReset = {
                            repository.updateActiveConfig(config.copy(offsetX = 0, offsetY = 0))
                        },
                        onScaleChange = { newSize ->
                            repository.updateActiveConfig(config.copy(sizeDp = newSize.coerceIn(14f, 90f)))
                        },
                        onOpacityChange = { newOpacity ->
                            repository.updateActiveConfig(config.copy(opacity = newOpacity.coerceIn(0.10f, 1.00f)))
                        },
                        onRotationChange = { newRotation ->
                            repository.updateActiveConfig(config.copy(rotationDeg = newRotation.coerceIn(0f, 360f)))
                        },
                        onClose = {
                            isCalibrateMode = false
                            hideCalibrateWidget()
                        }
                    )
                }
            }
        }

        calibrateView = view
        calibrateParams = params

        runCatching {
            windowManager.addView(view, params)
        }
    }

    private fun hideCalibrateWidget() {
        calibrateView?.let { runCatching { windowManager.removeView(it) } }
        calibrateView = null
        calibrateParams = null
    }

    private fun showQuickToggleFloatingWidget() {
        if (quickToggleView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
        }

        val frameLayout = object : FrameLayout(this) {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
                ev ?: return false
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = ev.rawX
                        initialTouchY = ev.rawY
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = Math.abs(ev.rawX - initialTouchX)
                        val dy = Math.abs(ev.rawY - initialTouchY)
                        if (dx > 12f || dy > 12f) {
                            isDragging = true
                            return true
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(event: MotionEvent?): Boolean {
                event ?: return false
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            runCatching { windowManager.updateViewLayout(this, params) }
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            isDragging = false
                            return true
                        }
                    }
                }
                return super.onTouchEvent(event)
            }
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isVisible = isCrosshairVisibleState.value
                val savedPresets by repository.savedPresets.collectAsState()
                val isDark by repository.isDarkMode.collectAsState()
                MyApplicationTheme(darkTheme = isDark) {
                    PersistentFloatingQuickToggle(
                        isCrosshairVisible = isVisible,
                        hasCustomPresets = savedPresets.isNotEmpty(),
                        onToggle = {
                            isCrosshairVisibleState.value = !isCrosshairVisibleState.value
                            updateEffectiveVisibility()
                            updateNotification(repository.activeConfig.value)
                        },
                        onCyclePreset = {
                            repository.switchToNextCustomPreset()?.let { switched ->
                                eventTracker.logEvent(
                                    AppEventType.PRESET_EQUIPPED,
                                    "overlay",
                                    mapOf("preset_name" to switched.name, "style" to switched.style.name, "source" to "hud_quick_cycle")
                                )
                            }
                        },
                        onToggleControls = {
                            isCalibrateMode = !isCalibrateMode
                            eventTracker.logEvent(
                                AppEventType.OVERLAY_CALIBRATION_TOGGLED,
                                "overlay",
                                mapOf("enabled" to isCalibrateMode)
                            )
                            if (isCalibrateMode) {
                                showCalibrateWidget()
                            } else {
                                hideCalibrateWidget()
                            }
                        }
                    )
                }
            }
        }
        frameLayout.addView(composeView)

        quickToggleView = frameLayout
        quickToggleParams = params

        runCatching {
            windowManager.addView(frameLayout, params)
        }
    }
}

@Composable
private fun PersistentFloatingQuickToggle(
    isCrosshairVisible: Boolean,
    hasCustomPresets: Boolean,
    onToggle: () -> Unit,
    onCyclePreset: () -> Unit,
    onToggleControls: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(
            1.5.dp,
            if (isCrosshairVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clickable { onToggle() }
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isCrosshairVisible) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isCrosshairVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = stringResource(R.string.cd_quick_toggle_hud),
                    tint = if (isCrosshairVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCrosshairVisible) stringResource(R.string.hud_on) else stringResource(R.string.hud_off),
                    color = if (isCrosshairVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            )

            Box(
                modifier = Modifier
                    .clickable { onToggleControls() }
                    .padding(horizontal = 9.dp, vertical = 8.dp)
                    .testTag("hud_quick_tune_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = stringResource(R.string.cd_calibrate_hud_toggle),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (hasCustomPresets) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                )

                Box(
                    modifier = Modifier
                        .clickable { onCyclePreset() }
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                        .testTag("hud_quick_cycle_preset_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.cd_quick_switch_preset),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrateFloatingWidget(
    config: CrosshairConfig,
    savedPresets: List<CrosshairConfig>,
    onSelectPreset: (CrosshairConfig) -> Unit,
    onNudge: (dx: Int, dy: Int) -> Unit,
    onReset: () -> Unit,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(188.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.hud_offset_compact, config.offsetX, config.offsetY),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_hud),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MiniNudgeButton(icon = Icons.Default.KeyboardArrowUp) { onNudge(0, -1) }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniNudgeButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft) { onNudge(-1, 0) }
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.cd_reset_center),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                MiniNudgeButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight) { onNudge(1, 0) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            MiniNudgeButton(icon = Icons.Default.KeyboardArrowDown) { onNudge(0, 1) }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(188.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOutMap,
                    contentDescription = stringResource(R.string.overlay_scale_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = config.sizeDp.coerceIn(14f, 90f),
                    onValueChange = onScaleChange,
                    valueRange = 14f..90f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hud_scale_slider")
                )
                Text(
                    text = stringResource(R.string.unit_dp_int, config.sizeDp.toInt()),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(188.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = stringResource(R.string.overlay_opacity_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = config.opacity.coerceIn(0.10f, 1.00f),
                    onValueChange = onOpacityChange,
                    valueRange = 0.10f..1.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hud_opacity_slider")
                )
                Text(
                    text = stringResource(
                        R.string.percent_format,
                        (config.opacity.coerceIn(0.10f, 1.00f) * 100).toInt()
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(188.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = stringResource(R.string.overlay_rotation_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = config.rotationDeg.coerceIn(0f, 360f),
                    onValueChange = onRotationChange,
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hud_rotation_slider")
                )
                Text(
                    text = stringResource(R.string.unit_deg_int, config.rotationDeg.toInt()),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (savedPresets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.width(188.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.hud_presets_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .width(188.dp)
                            .horizontalScroll(rememberScrollState())
                            .testTag("hud_presets_quick_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        savedPresets.forEach { preset ->
                            val isSelected = config.id == preset.id
                            val displayName = preset.name.ifBlank {
                                stringResource(R.string.default_custom_crosshair_name)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("hud_preset_chip_${preset.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.primaryColorHex or 0xFF000000L))
                                        .border(0.6.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                )
                                Text(
                                    text = displayName,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .width(164.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.cd_lock_position),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.btn_lock_position),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MiniNudgeButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.cd_nudge_aim),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}
