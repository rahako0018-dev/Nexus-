package com.example.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.DiagnosticLogger
import com.example.model.FullTelemetry
import com.example.model.GamingProfileType
import com.example.model.NetworkInfo
import com.example.model.ProfileConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PerformanceMonitor(private val context: Context) {
    private val ramMonitor = RamMonitor(context)
    private val displayMonitor = DisplayMonitor(context)
    private val batteryMonitor = BatteryMonitor(context)
    private val thermalMonitor = ThermalMonitor(context)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _telemetry = MutableStateFlow(FullTelemetry())
    val telemetry: StateFlow<FullTelemetry> = _telemetry.asStateFlow()

    private var monitorJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.Default)

    private var activeProfile: GamingProfileType = GamingProfileType.BALANCED
    private var isSidebarExpanded: Boolean = false

    fun start() {
        thermalMonitor.startListening()
        displayMonitor.startFpsTracking()
        restartPolling()
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        thermalMonitor.stopListening()
        displayMonitor.stopFpsTracking()
    }

    fun updateProfile(profileType: GamingProfileType) {
        if (activeProfile != profileType) {
            activeProfile = profileType
            restartPolling()
        }
    }

    fun setSidebarExpanded(expanded: Boolean) {
        if (isSidebarExpanded != expanded) {
            isSidebarExpanded = expanded
            restartPolling()
        }
    }

    private fun restartPolling() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                pollNow()

                // Interval based on profile & expanded state
                val baseConfig = ProfileConfig.forType(activeProfile)
                val interval = if (isSidebarExpanded) {
                    baseConfig.pollingIntervalMs.coerceAtMost(3000L)
                } else {
                    // When collapsed, reduce polling frequency to save budget device CPU/RAM
                    (baseConfig.pollingIntervalMs * 1.5).toLong().coerceIn(3000L, 12000L)
                }
                delay(interval)
            }
        }
    }

    fun pollNow() {
        try {
            val ram = ramMonitor.getRamInfo()
            val battery = batteryMonitor.getBatteryInfo()
            val thermal = thermalMonitor.getThermalInfo()
            val display = displayMonitor.getDisplayInfo()
            val network = getNetworkInfo()

            val snapshot = FullTelemetry(
                ram = ram,
                battery = battery,
                thermal = thermal,
                display = display,
                network = network,
                timestamp = System.currentTimeMillis()
            )

            _telemetry.value = snapshot

            if (ram.isLowMemory) {
                DiagnosticLogger.log("RAM", "System memory threshold reached: ${(ram.availableGb)}GB free", "WARN")
            }
        } catch (e: Exception) {
            DiagnosticLogger.log("MONITOR", "Error during telemetry update: ${e.message}", "ERROR")
        }
    }

    private fun getNetworkInfo(): NetworkInfo {
        val cm = connectivityManager ?: return NetworkInfo("Unknown", false, "No Service")
        val activeNetwork = cm.activeNetwork ?: return NetworkInfo("None", false, "Disconnected")
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkInfo("Unknown", false, "No Link")

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val linkDown = caps.linkDownstreamBandwidthKbps
                val speed = if (linkDown > 0) "${linkDown / 1000} Mbps" else "Active"
                NetworkInfo("Wi-Fi", true, speed)
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                NetworkInfo("Cellular 4G/5G", true, "Mobile Data")
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                NetworkInfo("Ethernet", true, "Cable")
            }
            else -> NetworkInfo("Connected", true, "Online")
        }
    }
}
