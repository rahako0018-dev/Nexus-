package com.example.manager

import android.content.Context
import android.os.Build
import com.example.data.DiagnosticLogger
import com.example.monitor.RamMonitor

data class StabilityHealthReport(
    val deviceModel: String,
    val androidVersion: String,
    val isBatteryOptimized: Boolean,
    val physicalRamGb: Float,
    val availableRamGb: Float,
    val stabilityScorePercent: Int,
    val recommendations: List<String>
)

class StabilityManager(private val context: Context) {
    private val ramMonitor = RamMonitor(context)
    private val permissionManager = PermissionManager(context)

    fun getHealthReport(): StabilityHealthReport {
        val ram = ramMonitor.getRamInfo()
        val isIgnoringBattery = permissionManager.isIgnoringBatteryOptimizations()
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        var score = 100
        val recs = mutableListOf<String>()

        if (!isIgnoringBattery) {
            score -= 30
            recs.add("Battery optimization is active. Set Nexus to 'Unrestricted' in Battery Settings to prevent OS from closing it.")
        }

        if (ram.totalGb in 0.1f..4.2f) {
            // Nubia A56 Unisoc typically 3GB to 4GB RAM
            if (ram.availableGb < 0.6f) {
                score -= 25
                recs.add("Low available RAM (<600MB free). Use 'Clean Nexus Cache' before launching heavy games.")
            } else {
                recs.add("Device has ${String.format("%.1f", ram.totalGb)}GB RAM. Balanced Mode is recommended.")
            }
        }

        // OEM specific advice for ZTE/Nubia/Unisoc
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer.contains("zte") || manufacturer.contains("nubia") || manufacturer.contains("unisoc")) {
            recs.add("nubia MyOS Tip: Lock Nexus in the Recent Apps screen (swipe down on the app card or tap the lock icon).")
            recs.add("Unisoc Power Manager: Enable 'Auto-start' and 'Allow background activity' in Settings.")
        } else {
            recs.add("Keep the ongoing status bar notification visible so Android keeps the foreground service at top priority.")
        }

        return StabilityHealthReport(
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            isBatteryOptimized = !isIgnoringBattery,
            physicalRamGb = ram.totalGb,
            availableRamGb = ram.availableGb,
            stabilityScorePercent = score.coerceIn(10, 100),
            recommendations = recs
        )
    }

    /**
     * Performs lightweight, responsible cleanup of Nexus's own memory footprint.
     * Does NOT kill other apps or fabricate numbers.
     */
    fun performNexusSelfCleanup(): Long {
        val ramBefore = ramMonitor.getRamInfo().availableBytes
        try {
            // Clear context cache
            context.cacheDir.deleteRecursively()
            // Suggest garbage collection to clean up Nexus temporary allocations
            System.gc()
            System.runFinalization()
        } catch (_: Exception) {}

        val ramAfter = ramMonitor.getRamInfo().availableBytes
        val freedBytes = (ramAfter - ramBefore).coerceAtLeast(0L)
        DiagnosticLogger.log("STABILITY", "Performed Nexus self-cleanup. Headroom refreshed.")
        return freedBytes
    }
}
