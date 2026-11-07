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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiagnosticLogger
import com.example.manager.PermissionManager
import com.example.manager.StabilityManager
import com.example.ui.components.CyberButton
import com.example.ui.components.GamingCard
import com.example.ui.components.NeonBadge
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusSurfaceCard
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stabilityManager = remember { StabilityManager(context) }
    val permissionManager = remember { PermissionManager(context) }

    val report = remember { stabilityManager.getHealthReport() }
    val logs by DiagnosticLogger.logs.collectAsState()

    val testingChecklist = listOf(
        "1. First app launch & initialization",
        "2. SYSTEM_ALERT_WINDOW permission request",
        "3. Starting floating sidebar handle",
        "4. Opening a game with sidebar active",
        "5. Opening and collapsing sidebar repeatedly",
        "6. Launching official Facebook app",
        "7. Launching official Messenger app",
        "8. Using Floating Mini-Browser (Facebook web)",
        "9. Rotating screen (Portrait <-> Landscape)",
        "10. Dragging handle away from game controls",
        "11. Running 15-minute gaming session",
        "12. Closing sidebar cleanly from notification",
        "13. Testing Quick Settings Tile toggle",
        "14. Testing under low-memory conditions",
        "15. Checking PowerManager thermal status",
        "16. Testing missing application handling",
        "17. Testing split-screen fallback mode",
        "18. Battery saver whitelist configuration",
        "19. Verifying diagnostic log entries"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "BACKGROUND STABILITY ENGINE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
            Text(
                text = "Device persistence, nubia MyOS tuning, and diagnostic event logs",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // 1. Stability Health Score Card
        item {
            GamingCard(
                borderColor = if (report.stabilityScorePercent > 80) NeonGreen else NeonAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "STABILITY HEALTH SCORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "${report.stabilityScorePercent}% Optimal",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (report.stabilityScorePercent > 80) NeonGreen else NeonAmber
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (report.stabilityScorePercent > 80) NeonGreen.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (report.stabilityScorePercent > 80) NeonGreen else NeonAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Hardware: ${report.deviceModel} | ${report.androidVersion}",
                    fontSize = 11.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Last Exit Status: ${DiagnosticLogger.getLastExitReason()}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // 2. ZTE nubia A56 / Unisoc Setup Survival Guide
        item {
            GamingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NUBIA A56 STABILITY CHECKLIST",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                report.recommendations.forEachIndexed { idx, rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(NexusSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = rec,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberButton(
                        text = "Battery Optimization",
                        onClick = {
                            context.startActivity(permissionManager.createBatteryOptimizationIntent())
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = NeonCyan,
                        isPrimary = false,
                        testTag = "open_battery_settings_button"
                    )
                    CyberButton(
                        text = "App Details",
                        onClick = {
                            context.startActivity(permissionManager.createAppDetailsIntent())
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = NeonCyan,
                        isPrimary = false,
                        testTag = "open_app_details_button"
                    )
                }
            }
        }

        // 3. Real-time Diagnostic Log Viewer
        item {
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DIAGNOSTIC EVENT LOGS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "${logs.size} Events Recorded Locally",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(
                        onClick = {
                            DiagnosticLogger.clearLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusDarkBg)
                        .border(BorderStroke(1.dp, NexusCardBorder), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "No diagnostic events logged yet.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(logs, key = { it.id }) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DiagnosticLogger.formatTime(log.timestamp),
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    Text(
                                        text = "[${log.tag}]",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (log.level) {
                                            "WARN" -> NeonAmber
                                            "ERROR" -> NeonCrimson
                                            else -> NeonCyan
                                        },
                                        modifier = Modifier.width(90.dp)
                                    )
                                    Text(
                                        text = log.message,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Testing Checklist Card
        item {
            GamingCard {
                Text(
                    text = "DEVICE VERIFICATION CHECKLIST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                testingChecklist.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
