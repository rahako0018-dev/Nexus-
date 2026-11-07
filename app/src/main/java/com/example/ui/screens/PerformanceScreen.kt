package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsRepository
import com.example.manager.PermissionManager
import com.example.manager.StabilityManager
import com.example.model.FullTelemetry
import com.example.model.GamingProfileType
import com.example.model.ProfileConfig
import com.example.model.SidebarConfig
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
fun PerformanceScreen(
    telemetry: FullTelemetry,
    config: SidebarConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val stabilityManager = remember { StabilityManager(context) }
    val permissionManager = remember { PermissionManager(context) }

    var cleanupResultText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "PERFORMANCE DASHBOARD",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
            Text(
                text = "Hardware telemetry, profile optimization, and memory management",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // 1. Physical RAM Details Card
        item {
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM MEMORY (RAM)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    NeonBadge(
                        text = if (telemetry.ram.isLowMemory) "LOW MEMORY WARNING" else "NORMAL STATUS",
                        color = if (telemetry.ram.isLowMemory) NeonCrimson else NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(
                        label = "Total Physical RAM",
                        value = "${String.format("%.2f", telemetry.ram.totalGb)} GB",
                        subValue = "Unisoc Memory",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Available RAM",
                        value = "${String.format("%.2f", telemetry.ram.availableGb)} GB",
                        subValue = "${100 - telemetry.ram.usedPercent}% Free",
                        color = if (telemetry.ram.availableGb < 0.6f) NeonCrimson else NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Used RAM",
                        value = "${String.format("%.2f", telemetry.ram.usedGb)} GB",
                        subValue = "${telemetry.ram.usedPercent}% utilized",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                CyberProgressBar(
                    progress = telemetry.ram.usedPercent / 100f,
                    color = if (telemetry.ram.isLowMemory) NeonCrimson else NeonCyan,
                    height = 10.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Threshold: ${String.format("%.2f", telemetry.ram.thresholdBytes / (1024f * 1024f * 1024f))} GB (OS begins trimming background apps below this point)",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // 2. Display Refresh Rate & FPS Transparency Card
        item {
            GamingCard {
                Text(
                    text = "DISPLAY & FRAME MONITOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(
                        label = "Active Display Refresh",
                        value = "${telemetry.display.refreshRateHz.toInt()} Hz",
                        subValue = "Display Mode",
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Overlay Rendering Rate",
                        value = "${telemetry.display.overlayFps} FPS",
                        subValue = "Choreographer",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Screen Resolution",
                        value = telemetry.display.resolution,
                        subValue = "Native Canvas",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Important Technical Notice (Requirement 7)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexusSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Row {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Note: Screen refresh rate (${telemetry.display.refreshRateHz.toInt()}Hz) is the hardware panel scan rate. Real game FPS depends on the individual game's engine and cannot be accessed by ordinary apps without root/system tracing. Nexus never shows fabricated game FPS.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 3. Battery & Thermal Status Card
        item {
            GamingCard {
                Text(
                    text = "BATTERY & THERMAL TELEMETRY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(
                        label = "Battery Charge",
                        value = "${telemetry.battery.levelPercent}%",
                        subValue = telemetry.battery.powerSource,
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Battery Temp",
                        value = "${String.format("%.1f", telemetry.battery.temperatureCelsius)}°C",
                        subValue = telemetry.battery.health,
                        color = if (telemetry.battery.temperatureCelsius > 42f) NeonCrimson else NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = "Thermal Status",
                        value = telemetry.thermal.statusLabel,
                        subValue = if (telemetry.thermal.isWarning) "Throttling" else "Safe",
                        color = if (telemetry.thermal.isWarning) NeonCrimson else NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Thermal Engine: ${telemetry.thermal.recommendation}",
                    fontSize = 11.sp,
                    color = if (telemetry.thermal.isWarning) NeonCrimson else TextSecondary
                )
            }
        }

        // 4. Three Gaming Profiles Card
        item {
            GamingCard {
                Text(
                    text = "THREE GAMING PERFORMANCE PROFILES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "These profiles optimize Nexus's own resource footprint to avoid competing with your game for RAM & CPU.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                GamingProfileType.values().forEach { profile ->
                    val pConfig = ProfileConfig.forType(profile)
                    val isSelected = config.activeProfileType == profile
                    val color = Color(profile.badgeColorHex)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) color.copy(alpha = 0.15f) else NexusDarkBg)
                            .border(
                                BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else NexusCardBorder),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                scope.launch { settingsRepo.updateProfile(profile) }
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) color else TextPrimary
                                )
                                if (isSelected) {
                                    NeonBadge(text = "ACTIVE", color = color)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pConfig.description,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 5. RAM Clean-up & Nexus Self-Optimization
        item {
            GamingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEXUS RESOURCE CLEANUP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Frees Nexus's temporary caches and optimizes memory allocations before launching a game. Does NOT forcefully kill other apps or harm system stability.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                CyberButton(
                    text = "Execute Nexus Cache Cleanup",
                    onClick = {
                        val freed = stabilityManager.performNexusSelfCleanup()
                        val freedMb = freed / (1024 * 1024)
                        cleanupResultText = if (freedMb > 0) "Freed approximately $freedMb MB of memory allocations." else "Nexus caches optimized and memory refreshed."
                        Toast.makeText(context, cleanupResultText, Toast.LENGTH_SHORT).show()
                    },
                    accentColor = NeonCyan,
                    testTag = "clean_nexus_cache_button"
                )

                if (cleanupResultText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cleanupResultText ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
