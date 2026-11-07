package com.example.data

import android.content.Context
import com.example.model.DiagnosticEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object DiagnosticLogger {
    private const val MAX_LOGS = 150
    private const val PREF_NAME = "nexus_diagnostics"
    private const val KEY_LAST_EXIT_REASON = "last_exit_reason"
    private const val KEY_SESSION_COUNT = "session_count"

    private val _logs = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val logs: StateFlow<List<DiagnosticEvent>> = _logs.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadPersistedEvents()
    }

    @Synchronized
    fun log(tag: String, message: String, level: String = "INFO") {
        val event = DiagnosticEvent(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            level = level
        )
        val current = _logs.value.toMutableList()
        current.add(0, event)
        if (current.size > MAX_LOGS) {
            current.removeAt(current.lastIndex)
        }
        _logs.value = current
    }

    fun recordSessionStart(trigger: String) {
        log("LIFECYCLE", "Overlay session started (Trigger: $trigger)")
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
            prefs.edit()
                .putInt(KEY_SESSION_COUNT, count)
                .putLong("last_start_time", System.currentTimeMillis())
                .putString(KEY_LAST_EXIT_REASON, "Running normal session")
                .apply()
        }
    }

    fun recordSessionStop(reason: String) {
        log("LIFECYCLE", "Overlay session stopped (Reason: $reason)")
        appContext?.let { ctx ->
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAST_EXIT_REASON, reason)
                .putLong("last_stop_time", System.currentTimeMillis())
                .apply()
        }
    }

    fun recordTrimMemory(level: Int) {
        val description = when (level) {
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL (System severely low on RAM)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW (System low on RAM)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE (Process near end of LRU list)"
            else -> "Level $level"
        }
        log("MEMORY", "TrimMemory signal received: $description", "WARN")
    }

    fun getLastExitReason(): String {
        val prefs = appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs?.getString(KEY_LAST_EXIT_REASON, "No prior session recorded or exited cleanly") ?: "Clean"
    }

    fun getSessionCount(): Int {
        val prefs = appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs?.getInt(KEY_SESSION_COUNT, 0) ?: 0
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log("DIAGNOSTICS", "Diagnostic logs cleared by user")
    }

    private fun loadPersistedEvents() {
        val lastReason = getLastExitReason()
        log("INIT", "Nexus diagnostic engine initialized. Last exit status: $lastReason")
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
