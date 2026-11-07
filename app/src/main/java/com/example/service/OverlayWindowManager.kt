package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.data.DiagnosticLogger
import com.example.data.SettingsRepository
import com.example.manager.ApplicationLauncher
import com.example.manager.SplitScreenResult
import com.example.model.FullTelemetry
import com.example.model.GamingProfileType
import com.example.model.SidebarConfig
import com.example.monitor.PerformanceMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayWindowManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsRepo = SettingsRepository(context)
    private val appLauncher = ApplicationLauncher(context)
    private val performanceMonitor = PerformanceMonitor(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val scope = CoroutineScope(Dispatchers.Main)
    private var configJob: Job? = null
    private var telemetryJob: Job? = null

    private var currentConfig = SidebarConfig()
    private var isExpanded = false
    private var isBrowserOpen = false
    private var isNotesOpen = false
    private var isStopwatchRunning = false
    private var stopwatchSeconds = 0
    private var stopwatchHandler = Handler(Looper.getMainLooper())
    private val stopwatchRunnable = object : Runnable {
        override fun run() {
            if (isStopwatchRunning) {
                stopwatchSeconds++
                updateStopwatchText()
                stopwatchHandler.postDelayed(this, 1000)
            }
        }
    }

    // Views
    private var handleView: View? = null
    private var panelView: View? = null
    private var browserView: View? = null
    private var notesView: View? = null

    // Layout Params
    private var handleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var browserParams: WindowManager.LayoutParams? = null
    private var notesParams: WindowManager.LayoutParams? = null

    // UI references in panel for dynamic updates
    private var ramText: TextView? = null
    private var batteryText: TextView? = null
    private var displayRefreshText: TextView? = null
    private var thermalText: TextView? = null
    private var profileBadgeText: TextView? = null
    private var stopwatchTextView: TextView? = null
    private var notesEditText: EditText? = null

    fun initialize() {
        if (!Settings.canDrawOverlays(context)) {
            DiagnosticLogger.log("OVERLAY_WM", "Cannot initialize: Overlay permission not granted", "WARN")
            return
        }

        performanceMonitor.start()
        createHandleView()

        configJob = scope.launch {
            settingsRepo.configFlow.collectLatest { config ->
                currentConfig = config
                applyConfig(config)
            }
        }

        telemetryJob = scope.launch {
            performanceMonitor.telemetry.collectLatest { telemetry ->
                updateTelemetryUI(telemetry)
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun vibrate() {
        if (!currentConfig.enableSoundOrHaptic) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    // ==========================================
    // 1. FLOATING HANDLE VIEW (Collapsed State)
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    private fun createHandleView() {
        if (handleView != null) return

        val metrics = context.resources.displayMetrics
        val screenHeight = metrics.heightPixels

        val baseWidth = dp((24 * currentConfig.handleSizeMultiplier).toInt())
        val baseHeight = dp((76 * currentConfig.handleSizeMultiplier).toInt())

        val p = WindowManager.LayoutParams(
            baseWidth,
            baseHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (currentConfig.isPositionLeft) Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP
            x = 0
            y = (screenHeight * currentConfig.verticalPercent).toInt().coerceIn(100, screenHeight - baseHeight - 100)
        }
        handleParams = p

        val container = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.argb((currentConfig.handleOpacity * 230).toInt(), 12, 18, 30))
                setStroke(dp(2), currentConfig.accentColorHex.toInt())
            }
            background = bg

            // Glowing indicator bar inside
            val glowBar = View(context).apply {
                val barBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(4).toFloat()
                    setColor(currentConfig.accentColorHex.toInt())
                }
                background = barBg
                val barParams = FrameLayout.LayoutParams(dp(4), dp(28)).apply {
                    gravity = Gravity.CENTER
                }
                layoutParams = barParams
            }
            addView(glowBar)
        }

        // Touch Listener for dragging and expanding
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            val lp = handleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (abs(deltaX) > 12 || abs(deltaY) > 12) {
                        isDragging = true
                    }
                    if (isDragging) {
                        lp.y = (initialY + deltaY.toInt()).coerceIn(60, metrics.heightPixels - lp.height - 60)
                        windowManager.updateViewLayout(container, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        vibrate()
                        // Save vertical position percent
                        val vertPercent = lp.y.toFloat() / metrics.heightPixels.toFloat()
                        scope.launch {
                            settingsRepo.updatePosition(currentConfig.isPositionLeft, vertPercent)
                        }
                    } else {
                        // It was a tap -> expand HUD panel
                        vibrate()
                        togglePanel(true)
                    }
                    true
                }
                else -> false
            }
        }

        handleView = container
        windowManager.addView(container, p)
        DiagnosticLogger.log("OVERLAY_WM", "Collapsed handle created and attached")
    }

    private fun applyConfig(config: SidebarConfig) {
        val handle = handleView ?: return
        val lp = handleParams ?: return

        lp.gravity = if (config.isPositionLeft) Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP
        val metrics = context.resources.displayMetrics
        lp.width = dp((24 * config.handleSizeMultiplier).toInt())
        lp.height = dp((76 * config.handleSizeMultiplier).toInt())
        lp.y = (metrics.heightPixels * config.verticalPercent).toInt().coerceIn(60, metrics.heightPixels - lp.height - 60)

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(Color.argb((config.handleOpacity * 230).toInt(), 12, 18, 30))
            setStroke(dp(2), config.accentColorHex.toInt())
        }
        handle.background = bg

        try {
            windowManager.updateViewLayout(handle, lp)
        } catch (_: Exception) {}

        performanceMonitor.updateProfile(config.activeProfileType)
    }

    // ==========================================
    // 2. EXPANDED GAMING SIDEBAR PANEL
    // ==========================================
    private fun togglePanel(expand: Boolean) {
        if (expand == isExpanded) return
        isExpanded = expand
        performanceMonitor.setSidebarExpanded(expand)

        if (expand) {
            handleView?.visibility = View.GONE
            createExpandedPanelView()
        } else {
            removeExpandedPanelView()
            handleView?.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createExpandedPanelView() {
        if (panelView != null) return

        val metrics = context.resources.displayMetrics
        val isLandscape = metrics.widthPixels > metrics.heightPixels
        val panelWidth = if (isLandscape) dp(380) else dp(320)
        val panelHeight = ViewGroup.LayoutParams.WRAP_CONTENT

        val p = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (currentConfig.isPositionLeft) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(8)
            y = 0
        }
        panelParams = p

        val rootCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#E60B101B")) // High opacity dark cyber
                setStroke(dp(2), currentConfig.accentColorHex.toInt())
            }
            background = bg
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Header: Nexus Title + Close Button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            weightSum = 1f
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(context).apply {
            text = "NEXUS SIDEBAR"
            textSize = 15f
            setTextColor(currentConfig.accentColorHex.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        profileBadgeText = TextView(context).apply {
            text = currentConfig.activeProfileType.displayName.uppercase()
            textSize = 10f
            setTextColor(Color.parseColor("#8E9EB8"))
        }

        titleCol.addView(title)
        titleCol.addView(profileBadgeText)
        header.addView(titleCol)

        // Close Button
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            val btnSize = dp(32)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
            }
            background = bg
            setOnClickListener {
                vibrate()
                togglePanel(false)
            }
        }
        header.addView(closeBtn)
        rootCard.addView(header)

        // Divider
        rootCard.addView(createDivider())

        // 1. Performance Telemetry HUD
        val telemetryCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#33182235"))
                setStroke(dp(1), Color.parseColor("#3300E5FF"))
            }
            background = bg
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val teleRow1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        val (ramBox, ramVal) = createMetricView("RAM AVAIL", "-- / -- GB", 1f)
        ramText = ramVal
        teleRow1.addView(ramBox)

        val (batBox, batVal) = createMetricView("BATTERY", "--%", 1f)
        batteryText = batVal
        teleRow1.addView(batBox)
        telemetryCard.addView(teleRow1)

        val teleRow2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            setPadding(0, dp(6), 0, 0)
        }

        val (dispBox, dispVal) = createMetricView("REFRESH / FPS", "90Hz (60 FPS)", 1f)
        displayRefreshText = dispVal
        teleRow2.addView(dispBox)

        val (thermBox, thermVal) = createMetricView("THERMAL", "Normal", 1f)
        thermalText = thermVal
        teleRow2.addView(thermBox)
        telemetryCard.addView(teleRow2)

        rootCard.addView(telemetryCard)
        rootCard.addView(createDivider())

        // 2. Facebook & Messenger Dedicated Shortcuts
        val socialTitle = TextView(context).apply {
            text = "SOCIAL QUICK ACCESS"
            textSize = 11f
            setTextColor(Color.parseColor("#8E9EB8"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        }
        rootCard.addView(socialTitle)

        val socialRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 4f
        }

        val fbStatus = appLauncher.getFacebookStatus()
        val msgStatus = appLauncher.getMessengerStatus()
        val instaStatus = appLauncher.getInstagramStatus()

        // Facebook Button
        val fbBtn = createActionCard(
            title = if (fbStatus.isInstalled) "Facebook" else "FB Web",
            subtitle = if (fbStatus.isInstalled) "Launch App" else "Mini Window",
            color = Color.parseColor("#1877F2"),
            weight = 1f
        ) {
            vibrate()
            if (fbStatus.isInstalled) {
                appLauncher.launchPackage(fbStatus.packageName)
                togglePanel(false)
            } else {
                openFloatingBrowser("https://m.facebook.com")
            }
        }
        socialRow.addView(fbBtn)

        // Messenger Button
        val msgBtn = createActionCard(
            title = if (msgStatus.isInstalled) "Messenger" else "Msg Web",
            subtitle = if (msgStatus.isInstalled) "Launch App" else "Mini Window",
            color = Color.parseColor("#0084FF"),
            weight = 1f
        ) {
            vibrate()
            if (msgStatus.isInstalled) {
                appLauncher.launchPackage(msgStatus.packageName)
                togglePanel(false)
            } else {
                openFloatingBrowser("https://www.messenger.com")
            }
        }
        socialRow.addView(msgBtn)

        // Instagram Button
        val instaBtn = createActionCard(
            title = if (instaStatus.isInstalled) "Instagram" else "Insta Web",
            subtitle = if (instaStatus.isInstalled) "Launch App" else "Mini Window",
            color = Color.parseColor("#E1306C"),
            weight = 1f
        ) {
            vibrate()
            if (instaStatus.isInstalled) {
                appLauncher.launchPackage(instaStatus.packageName)
                togglePanel(false)
            } else {
                openFloatingBrowser("https://www.instagram.com")
            }
        }
        socialRow.addView(instaBtn)

        // Split-Screen Button
        val splitBtn = createActionCard(
            title = "Split",
            subtitle = "Adjacent",
            color = Color.parseColor("#B388FF"),
            weight = 1f
        ) {
            vibrate()
            val targetPkg = when {
                msgStatus.isInstalled -> msgStatus.packageName
                fbStatus.isInstalled -> fbStatus.packageName
                instaStatus.isInstalled -> instaStatus.packageName
                else -> fbStatus.packageName
            }
            val result = appLauncher.launchSplitScreenAdjacent(targetPkg)
            when (result) {
                is SplitScreenResult.LaunchedAdjacent -> {
                    Toast.makeText(context, "Launched in adjacent window", Toast.LENGTH_SHORT).show()
                    togglePanel(false)
                }
                is SplitScreenResult.FallbackToFloating -> {
                    Toast.makeText(context, "Split rejected by OS. Opening Floating Mini-Browser...", Toast.LENGTH_SHORT).show()
                    openFloatingBrowser("https://m.facebook.com")
                }
                is SplitScreenResult.NotSupported -> {
                    Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }
        socialRow.addView(splitBtn)
        rootCard.addView(socialRow)

        rootCard.addView(createDivider())

        // 3. Gaming Tools Row
        val toolsTitle = TextView(context).apply {
            text = "GAMING COMPANION TOOLS"
            textSize = 11f
            setTextColor(Color.parseColor("#8E9EB8"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        }
        rootCard.addView(toolsTitle)

        val toolsScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        val toolsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Floating Mini-Browser Tool
        toolsRow.addView(createToolChip("Mini-Browser", android.R.drawable.ic_menu_view) {
            vibrate()
            openFloatingBrowser("https://m.facebook.com")
        })

        // Floating Notes Tool
        toolsRow.addView(createToolChip("Game Notes", android.R.drawable.ic_menu_edit) {
            vibrate()
            openFloatingNotes()
        })

        // Session Timer / Stopwatch
        stopwatchTextView = TextView(context).apply {
            text = "Timer: 00:00"
            textSize = 11f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val timerChip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#22304A"))
            }
            background = bg
            setPadding(dp(10), dp(6), dp(10), dp(6))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6)
            }
            layoutParams = lp
            addView(stopwatchTextView)
            setOnClickListener {
                vibrate()
                toggleStopwatch()
            }
        }
        toolsRow.addView(timerChip)

        // Open Dashboard
        toolsRow.addView(createToolChip("Nexus Hub", android.R.drawable.ic_menu_preferences) {
            vibrate()
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
            togglePanel(false)
        })

        // Display Brightness Settings Shortcut
        toolsRow.addView(createToolChip("Brightness", android.R.drawable.ic_menu_compass) {
            vibrate()
            try {
                context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {}
            togglePanel(false)
        })

        // Sound / Volume Settings Shortcut
        toolsRow.addView(createToolChip("Audio / DND", android.R.drawable.ic_lock_silent_mode) {
            vibrate()
            try {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {}
            togglePanel(false)
        })

        toolsScroll.addView(toolsRow)
        rootCard.addView(toolsScroll)

        // Dismiss when tapping outside the card
        rootCard.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                togglePanel(false)
                true
            } else {
                false
            }
        }

        panelView = rootCard
        windowManager.addView(rootCard, p)
        DiagnosticLogger.log("OVERLAY_WM", "Expanded gaming panel opened")
    }

    private fun removeExpandedPanelView() {
        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            panelView = null
            ramText = null
            batteryText = null
            displayRefreshText = null
            thermalText = null
            profileBadgeText = null
            stopwatchTextView = null
        }
    }

    private fun createMetricView(label: String, initialValue: String, weight: Float): Pair<LinearLayout, TextView> {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)

            val lbl = TextView(context).apply {
                text = label
                textSize = 9f
                setTextColor(Color.parseColor("#8E9EB8"))
            }
            addView(lbl)
        }
        val valView = TextView(context).apply {
            text = initialValue
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(valView)
        return Pair(root, valView)
    }

    private fun createActionCard(title: String, subtitle: String, color: Int, weight: Float, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#22304A"))
                setStroke(dp(1), color)
            }
            background = bg
            setPadding(dp(6), dp(8), dp(6), dp(8))

            val t = TextView(context).apply {
                text = title
                textSize = 11f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            val st = TextView(context).apply {
                text = subtitle
                textSize = 8f
                setTextColor(Color.parseColor("#8E9EB8"))
                gravity = Gravity.CENTER
            }
            addView(t)
            addView(st)
            setOnClickListener { onClick() }
        }
    }

    private fun createToolChip(label: String, iconRes: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#22304A"))
            }
            background = bg
            setPadding(dp(10), dp(6), dp(10), dp(6))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6)
            }
            layoutParams = lp

            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply {
                    marginEnd = dp(4)
                }
            }
            val text = TextView(context).apply {
                text = label
                textSize = 11f
                setTextColor(Color.WHITE)
            }
            addView(icon)
            addView(text)
            setOnClickListener { onClick() }
        }
    }

    private fun createDivider(): View {
        return View(context).apply {
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#3322304A"))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTelemetryUI(telemetry: FullTelemetry) {
        if (!isExpanded) return

        mainHandler.post {
            ramText?.text =
                "${String.format("%.1f", telemetry.ram.availableGb)} / ${String.format("%.1f", telemetry.ram.totalGb)} GB"

            batteryText?.text =
                "${telemetry.battery.levelPercent}% (${String.format("%.0f", telemetry.battery.temperatureCelsius)}°C)"

            displayRefreshText?.text =
                "${telemetry.display.refreshRateHz.toInt()}Hz (${telemetry.display.overlayFps} FPS)"

            thermalText?.apply {
                text = telemetry.thermal.statusLabel
                setTextColor(if (telemetry.thermal.isWarning) Color.parseColor("#FF5252") else Color.parseColor("#00E676"))
            }
        }
    }

    private fun toggleStopwatch() {
        if (isStopwatchRunning) {
            isStopwatchRunning = false
            stopwatchHandler.removeCallbacks(stopwatchRunnable)
            Toast.makeText(context, "Timer paused at ${formatStopwatch(stopwatchSeconds)}", Toast.LENGTH_SHORT).show()
        } else {
            isStopwatchRunning = true
            stopwatchHandler.post(stopwatchRunnable)
            Toast.makeText(context, "Timer started", Toast.LENGTH_SHORT).show()
        }
        updateStopwatchText()
    }

    @SuppressLint("SetTextI18n")
    private fun updateStopwatchText() {
        stopwatchTextView?.text = "Timer: ${formatStopwatch(stopwatchSeconds)}"
    }

    private fun formatStopwatch(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    // ==========================================
    // 3. FLOATING MINI-BROWSER (Facebook / Web)
    // ==========================================
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    fun openFloatingBrowser(initialUrl: String) {
        if (browserView != null) {
            // Already open, reload url
            (browserView?.findViewWithTag<WebView>("nexus_webview"))?.loadUrl(initialUrl)
            return
        }

        val metrics = context.resources.displayMetrics
        val isLandscape = metrics.widthPixels > metrics.heightPixels
        val width = if (isLandscape) dp(420) else dp(340)
        val height = if (isLandscape) dp(320) else dp(460)

        val p = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
        browserParams = p

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#F2090D14"))
                setStroke(dp(2), currentConfig.accentColorHex.toInt())
            }
            background = bg
            elevation = dp(8).toFloat()
        }

        var currentActiveUrl = initialUrl
        var webViewRef: WebView? = null

        // Title bar (Draggable header)
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), 0f, 0f, 0f, 0f)
                setColor(Color.parseColor("#182235"))
            }
            background = bg
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val title = TextView(context).apply {
            text = "NEXUS META BROWSER"
            textSize = 12f
            setTextColor(currentConfig.accentColorHex.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(title)

        // Native App Launch / Sync button
        val syncAppBtn = TextView(context).apply {
            text = "Open App"
            textSize = 10f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(6).toFloat()
                setColor(Color.parseColor("#3300E5FF"))
                setStroke(dp(1), Color.parseColor("#00E5FF"))
            }
            background = btnBg
            setPadding(dp(6), dp(4), dp(6), dp(4))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6)
            }
            layoutParams = lp
            setOnClickListener {
                vibrate()
                when {
                    currentActiveUrl.contains("instagram") -> {
                        val status = appLauncher.getInstagramStatus()
                        if (status.isInstalled) appLauncher.launchPackage(status.packageName)
                        else Toast.makeText(context, "Instagram app is not installed", Toast.LENGTH_SHORT).show()
                    }
                    currentActiveUrl.contains("messenger") -> {
                        val status = appLauncher.getMessengerStatus()
                        if (status.isInstalled) appLauncher.launchPackage(status.packageName)
                        else Toast.makeText(context, "Messenger app is not installed", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val status = appLauncher.getFacebookStatus()
                        if (status.isInstalled) appLauncher.launchPackage(status.packageName)
                        else Toast.makeText(context, "Facebook app is not installed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        titleBar.addView(syncAppBtn)

        // Back button
        val backBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(6) }
            setOnClickListener {
                if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
            }
        }
        titleBar.addView(backBtn)

        // Reload button
        val reloadBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_rotate)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(6) }
            setOnClickListener { webViewRef?.reload() }
        }
        titleBar.addView(reloadBtn)

        // Close button
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            setOnClickListener {
                closeFloatingBrowser()
            }
        }
        titleBar.addView(closeBtn)
        container.addView(titleBar)

        // Meta Quick-Switch Tabs Bar (Facebook, Messenger, Instagram)
        val tabsBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#121927"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            gravity = Gravity.CENTER_VERTICAL
        }

        fun createMetaTab(label: String, colorHex: String, targetUrl: String): TextView {
            return TextView(context).apply {
                text = label
                textSize = 10f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(6).toFloat()
                    setColor(Color.parseColor("#22304A"))
                    setStroke(dp(1), Color.parseColor(colorHex))
                }
                background = bg
                setPadding(dp(8), dp(4), dp(8), dp(4))
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                layoutParams = lp
                gravity = Gravity.CENTER
                setOnClickListener {
                    vibrate()
                    currentActiveUrl = targetUrl
                    CookieManager.getInstance().flush()
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        }

        tabsBar.addView(createMetaTab("Facebook", "#1877F2", "https://m.facebook.com"))
        tabsBar.addView(createMetaTab("Messenger", "#0084FF", "https://www.messenger.com"))
        tabsBar.addView(createMetaTab("Instagram", "#E1306C", "https://www.instagram.com"))
        container.addView(tabsBar)

        // Drag listener on Title Bar
        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        titleBar.setOnTouchListener { _, event ->
            val lp = browserParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = lp.x
                    initY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = initX + (event.rawX - touchX).toInt()
                    lp.y = initY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(container, lp)
                    true
                }
                else -> false
            }
        }

        // Initialize persistent CookieManager
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // WebView with modern secure configuration & Meta SSO support
        val webView = WebView(context).apply {
            tag = "nexus_webview"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            
            cookieManager.setAcceptThirdPartyCookies(this, true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = false
                allowContentAccess = false
                saveFormData = true
                cacheMode = WebSettings.LOAD_DEFAULT

                // Clean '; wv' from user-agent string so Meta services allow login & Accounts Center sync
                val defaultUa = userAgentString
                userAgentString = defaultUa.replace("; wv", "")
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    val url = uri.toString()
                    val scheme = uri.scheme ?: ""
                    // Handle native Meta app deep links and intents (e.g. fb://, instagram://, intent://)
                    if (scheme != "http" && scheme != "https") {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            DiagnosticLogger.log("BROWSER", "Dispatched Meta SSO / app intent for $scheme")
                            return true
                        } catch (e: Exception) {
                            DiagnosticLogger.log("BROWSER", "App link dispatch notice: ${e.message}", "WARN")
                        }
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null) currentActiveUrl = url
                    CookieManager.getInstance().flush()
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(initialUrl)
        }
        webViewRef = webView
        container.addView(webView)

        browserView = container
        windowManager.addView(container, p)
        isBrowserOpen = true
        DiagnosticLogger.log("OVERLAY_WM", "Floating mini-browser opened with persistent Meta session sync at $initialUrl")
    }

    fun closeFloatingBrowser() {
        browserView?.let {
            CookieManager.getInstance().flush()
            val wv = it.findViewWithTag<WebView>("nexus_webview")
            wv?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            browserView = null
            isBrowserOpen = false
            DiagnosticLogger.log("OVERLAY_WM", "Floating mini-browser closed (cookies permanently flushed)")
        }
    }

    // ==========================================
    // 4. FLOATING QUICK NOTES PAD
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    fun openFloatingNotes() {
        if (notesView != null) return

        val p = WindowManager.LayoutParams(
            dp(280),
            dp(240),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
        notesParams = p

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#E6101726"))
                setStroke(dp(2), Color.parseColor("#FFB388FF"))
            }
            background = bg
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }

        // Title bar
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(context).apply {
            text = "GAME NOTES"
            textSize = 12f
            setTextColor(Color.parseColor("#FFB388FF"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        bar.addView(title)

        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            setOnClickListener {
                closeFloatingNotes()
            }
        }
        bar.addView(closeBtn)
        container.addView(bar)

        // Dragging notes pad
        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        bar.setOnTouchListener { _, event ->
            val lp = notesParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = lp.x
                    initY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = initX + (event.rawX - touchX).toInt()
                    lp.y = initY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(container, lp)
                    true
                }
                else -> false
            }
        }

        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        notesEditText = EditText(context).apply {
            hint = "Jot down game codes, map locations, or tactics..."
            setText(currentConfig.quickNotes)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#668E9EB8"))
            textSize = 12f
            background = null
            isVerticalScrollBarEnabled = true
        }
        scroll.addView(notesEditText)
        container.addView(scroll)

        notesView = container
        windowManager.addView(container, p)
        isNotesOpen = true
        DiagnosticLogger.log("OVERLAY_WM", "Floating notes pad opened")
    }

    fun closeFloatingNotes() {
        notesView?.let {
            val text = notesEditText?.text?.toString() ?: ""
            scope.launch {
                settingsRepo.updateQuickNotes(text)
            }
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            notesView = null
            notesEditText = null
            isNotesOpen = false
            DiagnosticLogger.log("OVERLAY_WM", "Floating notes pad saved and closed")
        }
    }

    fun onConfigurationChanged() {
        val metrics = context.resources.displayMetrics
        handleParams?.let { lp ->
            lp.y = (metrics.heightPixels * currentConfig.verticalPercent).toInt().coerceIn(60, metrics.heightPixels - lp.height - 60)
            handleView?.let {
                try {
                    windowManager.updateViewLayout(it, lp)
                } catch (_: Exception) {}
            }
        }
    }

    fun destroy() {
        configJob?.cancel()
        telemetryJob?.cancel()
        stopwatchHandler.removeCallbacks(stopwatchRunnable)
        performanceMonitor.stop()

        removeExpandedPanelView()
        closeFloatingBrowser()
        closeFloatingNotes()

        handleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            handleView = null
        }

        DiagnosticLogger.log("OVERLAY_WM", "OverlayWindowManager destroyed and all views released")
    }
}
