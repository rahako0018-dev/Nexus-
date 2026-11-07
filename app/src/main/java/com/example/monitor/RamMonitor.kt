package com.example.monitor

import android.app.ActivityManager
import android.content.Context
import com.example.model.RamInfo

class RamMonitor(private val context: Context) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    fun getRamInfo(): RamInfo {
        val am = activityManager ?: return RamInfo()
        am.getMemoryInfo(memoryInfo)
        return RamInfo(
            totalBytes = memoryInfo.totalMem,
            availableBytes = memoryInfo.availMem,
            thresholdBytes = memoryInfo.threshold,
            isLowMemory = memoryInfo.lowMemory
        )
    }
}
