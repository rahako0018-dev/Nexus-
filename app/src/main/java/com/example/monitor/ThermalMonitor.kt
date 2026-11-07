package com.example.monitor

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.example.data.DiagnosticLogger
import com.example.model.ThermalInfo

class ThermalMonitor(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var currentStatusLevel: Int = PowerManager.THERMAL_STATUS_NONE
    private var isListenerRegistered = false

    private val thermalListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            currentStatusLevel = status
            val info = mapStatus(status)
            if (info.isWarning) {
                DiagnosticLogger.log("THERMAL", "Elevated thermal status detected: ${info.statusLabel}", "WARN")
            }
        }
    } else null

    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isListenerRegistered) {
            try {
                thermalListener?.let {
                    powerManager?.addThermalStatusListener(it)
                    isListenerRegistered = true
                }
            } catch (e: Exception) {
                DiagnosticLogger.log("THERMAL", "Could not register thermal listener: ${e.message}", "WARN")
            }
        }
    }

    fun stopListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isListenerRegistered) {
            try {
                thermalListener?.let {
                    powerManager?.removeThermalStatusListener(it)
                    isListenerRegistered = false
                }
            } catch (_: Exception) {}
        }
    }

    fun getThermalInfo(): ThermalInfo {
        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                powerManager.currentThermalStatus
            } catch (_: Exception) {
                currentStatusLevel
            }
        } else {
            currentStatusLevel
        }
        return mapStatus(level)
    }

    private fun mapStatus(level: Int): ThermalInfo {
        return when (level) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalInfo(
                statusLevel = 0,
                statusLabel = "Cool / Normal",
                isWarning = false,
                recommendation = "Optimal gaming conditions."
            )
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalInfo(
                statusLevel = 1,
                statusLabel = "Light Warmth",
                isWarning = false,
                recommendation = "Device slightly warm; full frame rate maintained."
            )
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalInfo(
                statusLevel = 2,
                statusLabel = "Moderate Thermal",
                isWarning = true,
                recommendation = "Device warm; consider lowering game graphic settings."
            )
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalInfo(
                statusLevel = 3,
                statusLabel = "High Thermal Pressure",
                isWarning = true,
                recommendation = "Severe thermal load; CPU/GPU throttling may occur."
            )
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalInfo(
                statusLevel = 4,
                statusLabel = "Critical Overheating",
                isWarning = true,
                recommendation = "Critical heat! Take a break to allow cooling."
            )
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalInfo(
                statusLevel = 5,
                statusLabel = "Thermal Emergency",
                isWarning = true,
                recommendation = "Hardware danger; device may shut down automatically."
            )
            else -> ThermalInfo(
                statusLevel = 0,
                statusLabel = "Normal",
                isWarning = false,
                recommendation = "Operating within acceptable limits."
            )
        }
    }
}
