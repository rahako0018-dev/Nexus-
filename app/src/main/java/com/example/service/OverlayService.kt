package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.DiagnosticLogger
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE = "com.example.service.ACTION_TOGGLE"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "nexus_game_overlay_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var overlayWindowManager: OverlayWindowManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        DiagnosticLogger.recordSessionStart("OverlayService.onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                DiagnosticLogger.recordSessionStop("User requested stop via notification / app")
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                if (overlayWindowManager == null) {
                    overlayWindowManager = OverlayWindowManager(applicationContext).apply {
                        initialize()
                    }
                    _isRunning.value = true
                    serviceScope.launch {
                        settingsRepo.setLastSessionActive(true)
                    }
                }
            }
            ACTION_TOGGLE -> {
                if (_isRunning.value) {
                    stopForegroundService()
                    return START_NOT_STICKY
                } else {
                    startForegroundWithNotification()
                    if (overlayWindowManager == null) {
                        overlayWindowManager = OverlayWindowManager(applicationContext).apply {
                            initialize()
                        }
                        _isRunning.value = true
                        serviceScope.launch {
                            settingsRepo.setLastSessionActive(true)
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 14/15 requires matching foregroundServiceType in startForeground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nexus Gaming Sidebar Active")
            .setContentText("Tap the floating screen edge handle while playing")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sidebar", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_preferences, "Open Nexus", openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Nexus Gaming Overlay"
            val descriptionText = "Keeps the floating gaming sidebar active above games"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        _isRunning.value = false
        serviceScope.launch {
            settingsRepo.setLastSessionActive(false)
        }
        overlayWindowManager?.destroy()
        overlayWindowManager = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        DiagnosticLogger.log("LIFECYCLE", "Configuration changed (Orientation: ${newConfig.orientation})")
        overlayWindowManager?.onConfigurationChanged()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        DiagnosticLogger.recordTrimMemory(level)
    }

    override fun onDestroy() {
        super.onDestroy()
        DiagnosticLogger.recordSessionStop("OverlayService.onDestroy")
        _isRunning.value = false
        serviceScope.launch {
            settingsRepo.setLastSessionActive(false)
        }
        overlayWindowManager?.destroy()
        overlayWindowManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
