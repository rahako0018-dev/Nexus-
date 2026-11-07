package com.example.monitor

import android.content.Context
import android.os.Build
import android.view.Choreographer
import android.view.WindowManager
import com.example.model.DisplayInfo

class DisplayMonitor(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    // Frame measurement for overlay rendering
    private var lastFrameTimeNanos: Long = 0L
    private var frameCount: Int = 0
    private var lastFpsCalculationTime: Long = System.currentTimeMillis()
    private var currentOverlayFps: Int = 60
    private var isTrackingFps = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isTrackingFps) return

            frameCount++
            val now = System.currentTimeMillis()
            val delta = now - lastFpsCalculationTime
            if (delta >= 1000L) {
                currentOverlayFps = (frameCount * 1000L / delta).toInt().coerceIn(1, 120)
                frameCount = 0
                lastFpsCalculationTime = now
            }
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startFpsTracking() {
        if (!isTrackingFps) {
            isTrackingFps = true
            frameCount = 0
            lastFpsCalculationTime = System.currentTimeMillis()
            try {
                Choreographer.getInstance().postFrameCallback(frameCallback)
            } catch (_: Exception) {
                // If running on non-UI thread or headless
            }
        }
    }

    fun stopFpsTracking() {
        isTrackingFps = false
        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    fun getDisplayInfo(): DisplayInfo {
        val wm = windowManager ?: return DisplayInfo()
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: wm.defaultDisplay
        } else {
            wm.defaultDisplay
        }

        val refreshRate = display?.refreshRate ?: 60f
        val supportedRates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
            display.supportedModes.map { it.refreshRate }.distinct().sorted()
        } else {
            listOf(refreshRate)
        }

        val resolution = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            "${bounds.width()}x${bounds.height()}"
        } else {
            val metrics = android.util.DisplayMetrics()
            display?.getRealMetrics(metrics)
            "${metrics.widthPixels}x${metrics.heightPixels}"
        }

        return DisplayInfo(
            refreshRateHz = refreshRate,
            supportedRates = supportedRates,
            resolution = resolution,
            overlayFps = currentOverlayFps
        )
    }
}
