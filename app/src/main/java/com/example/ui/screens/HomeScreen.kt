package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SettingsRepository
import com.example.manager.ApplicationLauncher
import com.example.manager.PermissionManager
import com.example.manager.SplitScreenResult
import com.example.model.FullTelemetry
import com.example.model.GamingProfileType
import com.example.model.SidebarConfig
import com.example.service.OverlayService
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberProgressBar
import com.example.ui.components.GamingCard
import com.example.ui.components.NeonBadge
import com.example.ui.components.StatMetric
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusSurfaceCard
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    telemetry: FullTelemetry,
    config: SidebarConfig,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSidebar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = SettingsRepository(context)
    val permissionManager = PermissionManager(context)
    val appLauncher = ApplicationLauncher(context)

    val isServiceRunning by OverlayService.isRunning.collectAsState()
    val hasOverlayPerm = permissionManager.hasOverlayPermission()

    val deviceName = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. Header Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.nexus_icon_fg),
                        contentDescription = "Nexus Emblem",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.5.dp, NeonCyan), RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "NEXUS GAME SIDEBAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target: $deviceName",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                NeonBadge(
                    text = if (isServiceRunning) "OVERLAY ACTIVE" else "STANDBY",
                    color = if (isServiceRunning) NeonGreen else TextSecondary
                )
            }
        }

        // 2. Permission Warning (If Needed)
        if (!hasOverlayPerm) {
            item {
                GamingCard(
                    borderColor = NeonCrimson,
                    modifier = Modifier.testTag("overlay_permission_banner")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Permission Alert",
                            tint = NeonCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overlay Permission Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Allow 'Display over other apps' to float the sidebar over games.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CyberButton(
                        text = "Grant Overlay Permission",
                        onClick = {
                            context.startActivity(permissionManager.createOverlaySettingsIntent())
                        },
                        accentColor = NeonCrimson,
                        testTag = "grant_overlay_button"
                    )
                }
            }
        }

        // 3. Master Session Control Card
        item {
            GamingCard(
                borderColor = if (isServiceRunning) NeonCyan else NexusCardBorder,
                modifier = Modifier.testTag("session_control_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isServiceRunning) "Gaming Session in Progress" else "Gaming Session Idle",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isServiceRunning) "Floating handle active at screen edge" else "Tap start to activate the gaming handle",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) NeonCyan.copy(alpha = 0.2f) else NexusSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Bolt else Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isServiceRunning) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (isServiceRunning) {
                    CyberButton(
                        text = "Stop Gaming Session",
                        onClick = {
                            OverlayService.stopService(context)
                            Toast.makeText(context, "Gaming sidebar stopped", Toast.LENGTH_SHORT).show()
                        },
                        accentColor = NeonCrimson,
                        isPrimary = false,
                        testTag = "stop_session_button"
                    )
                } else {
                    CyberButton(
                        text = "Start Gaming Session",
                        onClick = {
                            if (!permissionManager.hasOverlayPermission()) {
                                context.startActivity(permissionManager.createOverlaySettingsIntent())
                            } else {
                                OverlayService.startService(context)
                                Toast.makeText(context, "Gaming sidebar activated! Check screen edge.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        accentColor = NeonCyan,
                        isPrimary = true,
                        testTag = "start_session_button"
                    )
                }
            }
        }

        // 4. Real-time Hardware Telemetry Overview
        item {
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HARDWARE TELEMETRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Live Nubia A56 Monitor",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                // RAM row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Physical RAM: ${String.format("%.1f", telemetry.ram.totalGb)} GB",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${String.format("%.1f", telemetry.ram.availableGb)} GB Available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (telemetry.ram.isLowMemory) NeonCrimson else NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                CyberProgressBar(
                    progress = telemetry.ram.usedPercent / 100f,
                    color = if (telemetry.ram.isLowMemory) NeonCrimson else NeonCyan
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Stats in a Row: Refresh Rate, Battery, Thermal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(
                        label = "Display Refresh",
                        value = "${telemetry.display.refreshRateHz.toInt()} Hz",
                        subValue = "Modes: ${telemetry.display.supportedRates.joinToString("/") { it.toInt().toString() }}",
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Battery Temp",
                        value = "${telemetry.battery.levelPercent}%",
                        subValue = "${String.format("%.1f", telemetry.battery.temperatureCelsius)}°C (${telemetry.battery.powerSource})",
                        color = if (telemetry.battery.temperatureCelsius > 42f) NeonCrimson else NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Thermal Status",
                        value = telemetry.thermal.statusLabel,
                        subValue = if (telemetry.thermal.isWarning) "Throttling" else "Normal",
                        color = if (telemetry.thermal.isWarning) NeonCrimson else NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Gaming Profiles Quick Selector
        item {
            GamingCard {
                Text(
                    text = "ACTIVE GAMING PROFILE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GamingProfileType.values().forEach { profile ->
                        val isSelected = config.activeProfileType == profile
                        val color = Color(profile.badgeColorHex)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else NexusDarkBg)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) color else NexusCardBorder
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        settingsRepo.updateProfile(profile)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = profile.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) color else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Facebook & Messenger Dedicated Shortcuts Card
        item {
            GamingCard {
                Text(
                    text = "SOCIAL MULTITASKING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val fbStatus = appLauncher.getFacebookStatus()
                val msgStatus = appLauncher.getMessengerStatus()
                val instaStatus = appLauncher.getInstagramStatus()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Facebook Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusDarkBg)
                            .border(BorderStroke(1.dp, Color(0xFF1877F2)), RoundedCornerShape(12.dp))
                            .clickable {
                                if (fbStatus.isInstalled) {
                                    appLauncher.launchPackage(fbStatus.packageName)
                                } else {
                                    Toast.makeText(context, "Opening Facebook in Floating Browser", Toast.LENGTH_SHORT).show()
                                    if (permissionManager.hasOverlayPermission()) {
                                        OverlayService.startService(context)
                                    }
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Facebook",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (fbStatus.isInstalled) "Installed App" else "Web Version",
                                fontSize = 10.sp,
                                color = Color(0xFF1877F2)
                            )
                        }
                    }

                    // Messenger Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusDarkBg)
                            .border(BorderStroke(1.dp, Color(0xFF0084FF)), RoundedCornerShape(12.dp))
                            .clickable {
                                if (msgStatus.isInstalled) {
                                    appLauncher.launchPackage(msgStatus.packageName)
                                } else {
                                    Toast.makeText(context, "Opening Messenger in Floating Browser", Toast.LENGTH_SHORT).show()
                                    if (permissionManager.hasOverlayPermission()) {
                                        OverlayService.startService(context)
                                    }
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Messenger",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (msgStatus.isInstalled) "Installed App" else "Web Version",
                                fontSize = 10.sp,
                                color = Color(0xFF0084FF)
                            )
                        }
                    }

                    // Instagram Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusDarkBg)
                            .border(BorderStroke(1.dp, Color(0xFFE1306C)), RoundedCornerShape(12.dp))
                            .clickable {
                                if (instaStatus.isInstalled) {
                                    appLauncher.launchPackage(instaStatus.packageName)
                                } else {
                                    Toast.makeText(context, "Opening Instagram in Floating Browser", Toast.LENGTH_SHORT).show()
                                    if (permissionManager.hasOverlayPermission()) {
                                        OverlayService.startService(context)
                                    }
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Instagram",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (instaStatus.isInstalled) "Installed App" else "Web Version",
                                fontSize = 10.sp,
                                color = Color(0xFFE1306C)
                            )
                        }
                    }
                }
            }
        }

        // 7. Automatic Meta Login Sync Information & Action
        item {
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "META SESSION & LOGIN SYNC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    NeonBadge(text = "UNIFIED COOKIES", color = NeonCyan)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Cross-Service Synchronization: Facebook, Messenger, and Instagram web panels share a single persistent Meta cookie store. Logging in to any one account automatically connects the others via Meta Accounts Center.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Native App SSO: Tapping 'Continue as [Account]' or 'Log in with App' dispatches directly to your installed Meta applications to authorize the session with one tap.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Persistent Storage: Cookies are flushed and saved to disk so you stay logged in permanently even after closing games or restarting your phone.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                CyberButton(
                    text = "Open Floating Meta Browser",
                    onClick = {
                        if (permissionManager.hasOverlayPermission()) {
                            OverlayService.startService(context)
                            Toast.makeText(context, "Tap the edge handle or tools to open the Meta mini-browser", Toast.LENGTH_SHORT).show()
                        } else {
                            context.startActivity(permissionManager.createOverlaySettingsIntent())
                        }
                    },
                    accentColor = NeonCyan,
                    testTag = "open_meta_browser_button"
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
