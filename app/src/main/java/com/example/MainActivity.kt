package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiagnosticLogger
import com.example.data.SettingsRepository
import com.example.model.SidebarConfig
import com.example.monitor.PerformanceMonitor
import com.example.ui.navigation.NavScreen
import com.example.ui.screens.GameLibraryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SidebarConfigScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        DiagnosticLogger.init(applicationContext)
        performanceMonitor = PerformanceMonitor(this)
        settingsRepository = SettingsRepository(this)

        setContent {
            MyApplicationTheme {
                NexusMainApp(
                    performanceMonitor = performanceMonitor,
                    settingsRepository = settingsRepository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        performanceMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        performanceMonitor.stop()
    }
}

@Composable
fun NexusMainApp(
    performanceMonitor: PerformanceMonitor,
    settingsRepository: SettingsRepository
) {
    var currentScreen by remember { mutableStateOf(NavScreen.HOME) }
    val telemetry by performanceMonitor.telemetry.collectAsState()
    val config by settingsRepository.configFlow.collectAsState(initial = SidebarConfig())

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = currentScreen != NavScreen.HOME) {
        currentScreen = NavScreen.HOME
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusDarkBg),
        containerColor = NexusDarkBg,
        bottomBar = {
            if (!isLandscape) {
                NavigationBar(
                    containerColor = NexusSurface,
                    contentColor = TextPrimary
                ) {
                    NavScreen.values().forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLandscape) {
            // Adaptive wide layout: NavigationRail on left
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavigationRail(
                    containerColor = NexusSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavScreen.values().forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    RenderScreen(
                        screen = currentScreen,
                        telemetry = telemetry,
                        config = config,
                        onNavigate = { currentScreen = it }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                RenderScreen(
                    screen = currentScreen,
                    telemetry = telemetry,
                    config = config,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    }
}

@Composable
private fun RenderScreen(
    screen: NavScreen,
    telemetry: com.example.model.FullTelemetry,
    config: SidebarConfig,
    onNavigate: (NavScreen) -> Unit
) {
    when (screen) {
        NavScreen.HOME -> HomeScreen(
            telemetry = telemetry,
            config = config,
            onNavigateToLibrary = { onNavigate(NavScreen.LIBRARY) },
            onNavigateToSidebar = { onNavigate(NavScreen.SIDEBAR) },
            onNavigateToSettings = { onNavigate(NavScreen.SETTINGS) }
        )
        NavScreen.LIBRARY -> GameLibraryScreen()
        NavScreen.SIDEBAR -> SidebarConfigScreen(config = config)
        NavScreen.PERFORMANCE -> PerformanceScreen(telemetry = telemetry, config = config)
        NavScreen.SETTINGS -> SettingsScreen()
    }
}
