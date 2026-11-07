package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Hub", Icons.Default.Dashboard),
    LIBRARY("library", "Games", Icons.Default.Gamepad),
    SIDEBAR("sidebar", "Overlay", Icons.Default.Tune),
    PERFORMANCE("performance", "Perf", Icons.Default.Memory),
    SETTINGS("settings", "Stability", Icons.Default.Settings)
}
