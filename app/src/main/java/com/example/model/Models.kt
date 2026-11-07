package com.example.model

enum class GamingProfileType(val displayName: String, val badgeColorHex: Long) {
    PERFORMANCE_FOCUS("Performance Focus", 0xFFFF2A55),
    BALANCED("Balanced Mode", 0xFF00E5FF),
    BATTERY_SAVER("Battery Saver", 0xFF00E676)
}

data class ProfileConfig(
    val type: GamingProfileType,
    val pollingIntervalMs: Long,
    val animationsEnabled: Boolean,
    val particleEffectsEnabled: Boolean,
    val compactHandle: Boolean,
    val description: String
) {
    companion object {
        fun forType(type: GamingProfileType): ProfileConfig = when (type) {
            GamingProfileType.PERFORMANCE_FOCUS -> ProfileConfig(
                type = type,
                pollingIntervalMs = 5000L,
                animationsEnabled = false,
                particleEffectsEnabled = false,
                compactHandle = true,
                description = "Throttles Nexus overlay polling and disables effects to minimize RAM/CPU usage during gameplay."
            )
            GamingProfileType.BALANCED -> ProfileConfig(
                type = type,
                pollingIntervalMs = 2500L,
                animationsEnabled = true,
                particleEffectsEnabled = true,
                compactHandle = false,
                description = "Standard refresh rate, smooth HUD animations, and responsive quick-access tools."
            )
            GamingProfileType.BATTERY_SAVER -> ProfileConfig(
                type = type,
                pollingIntervalMs = 8000L,
                animationsEnabled = false,
                particleEffectsEnabled = false,
                compactHandle = true,
                description = "Extended telemetry intervals and ultra-low background processing to preserve battery."
            )
        }
    }
}

data class RamInfo(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val thresholdBytes: Long = 0L,
    val isLowMemory: Boolean = false
) {
    val totalGb: Float get() = totalBytes / (1024f * 1024f * 1024f)
    val availableGb: Float get() = availableBytes / (1024f * 1024f * 1024f)
    val usedGb: Float get() = (totalBytes - availableBytes).coerceAtLeast(0L) / (1024f * 1024f * 1024f)
    val usedPercent: Int get() = if (totalBytes > 0) (((totalBytes - availableBytes) * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

data class BatteryInfo(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 0.0f,
    val powerSource: String = "Battery",
    val health: String = "Good"
)

data class ThermalInfo(
    val statusLevel: Int = 0,
    val statusLabel: String = "Normal",
    val isWarning: Boolean = false,
    val recommendation: String = "Thermal conditions optimal."
)

data class DisplayInfo(
    val refreshRateHz: Float = 60f,
    val supportedRates: List<Float> = listOf(60f),
    val resolution: String = "1600x720",
    val overlayFps: Int = 60
)

data class NetworkInfo(
    val type: String = "Wi-Fi",
    val isConnected: Boolean = true,
    val details: String = "Connected"
)

data class FullTelemetry(
    val ram: RamInfo = RamInfo(),
    val battery: BatteryInfo = BatteryInfo(),
    val thermal: ThermalInfo = ThermalInfo(),
    val display: DisplayInfo = DisplayInfo(),
    val network: NetworkInfo = NetworkInfo(),
    val timestamp: Long = System.currentTimeMillis()
)

data class InstalledGame(
    val packageName: String,
    val appName: String,
    val isFavorite: Boolean = false,
    val profileType: GamingProfileType = GamingProfileType.BALANCED,
    val launchCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L
)

data class SidebarConfig(
    val isPositionLeft: Boolean = false,
    val verticalPercent: Float = 0.45f,
    val handleOpacity: Float = 0.85f,
    val handleSizeMultiplier: Float = 1.0f,
    val accentColorHex: Long = 0xFF00E5FF,
    val isCompactMode: Boolean = false,
    val enableSoundOrHaptic: Boolean = true,
    val activeProfileType: GamingProfileType = GamingProfileType.BALANCED,
    val quickNotes: String = ""
)

data class DiagnosticEvent(
    val id: String,
    val timestamp: Long,
    val tag: String,
    val message: String,
    val level: String = "INFO"
)
