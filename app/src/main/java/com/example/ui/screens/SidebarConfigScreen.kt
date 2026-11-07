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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.SidebarConfig
import com.example.service.OverlayService
import com.example.ui.components.CyberButton
import com.example.ui.components.GamingCard
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
fun SidebarConfigScreen(
    config: SidebarConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val permissionManager = remember { PermissionManager(context) }

    var isLeft by remember(config.isPositionLeft) { mutableStateOf(config.isPositionLeft) }
    var vertPercent by remember(config.verticalPercent) { mutableFloatStateOf(config.verticalPercent) }
    var opacity by remember(config.handleOpacity) { mutableFloatStateOf(config.handleOpacity) }
    var sizeMult by remember(config.handleSizeMultiplier) { mutableFloatStateOf(config.handleSizeMultiplier) }
    var accentHex by remember(config.accentColorHex) { mutableStateOf(config.accentColorHex) }
    var compact by remember(config.isCompactMode) { mutableStateOf(config.isCompactMode) }
    var haptic by remember(config.enableSoundOrHaptic) { mutableStateOf(config.enableSoundOrHaptic) }

    val accentColors = listOf(
        0xFF00E5FF to "Cyan",
        0xFFB388FF to "Purple",
        0xFFFF2A55 to "Crimson",
        0xFF00E676 to "Emerald"
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
                text = "SIDEBAR CUSTOMIZATION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
            Text(
                text = "Adjust handle position, opacity, scale, and theme color",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Live Interactive Bezel Preview
        item {
            GamingCard {
                Text(
                    text = "SCREEN EDGE PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexusDarkBg)
                        .border(BorderStroke(1.dp, NexusCardBorder), RoundedCornerShape(12.dp))
                ) {
                    // Simulated game content behind
                    Text(
                        text = "Game Area (Full Touch Passthrough)",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Render handle
                    val handleAlignment = if (isLeft) Alignment.TopStart else Alignment.TopEnd
                    val handleColor = Color(accentHex)

                    Box(
                        modifier = Modifier
                            .align(handleAlignment)
                            .offset(y = (180 * vertPercent - 25).coerceIn(10f, 130f).dp)
                            .width((14 * sizeMult).dp)
                            .height((48 * sizeMult).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = opacity))
                            .border(BorderStroke(1.5.dp, handleColor), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(handleColor)
                        )
                    }
                }
            }
        }

        // Edge Position Selector
        item {
            GamingCard {
                Text(
                    text = "SCREEN EDGE ANCHOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLeft) Color(accentHex).copy(alpha = 0.2f) else NexusDarkBg)
                            .border(
                                BorderStroke(1.dp, if (isLeft) Color(accentHex) else NexusCardBorder),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                isLeft = true
                                scope.launch { settingsRepo.updatePosition(true, vertPercent) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Left Screen Edge",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLeft) Color(accentHex) else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isLeft) Color(accentHex).copy(alpha = 0.2f) else NexusDarkBg)
                            .border(
                                BorderStroke(1.dp, if (!isLeft) Color(accentHex) else NexusCardBorder),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                isLeft = false
                                scope.launch { settingsRepo.updatePosition(false, vertPercent) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Right Screen Edge",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isLeft) Color(accentHex) else TextSecondary
                        )
                    }
                }
            }
        }

        // Sliders: Vertical Position, Opacity, Size
        item {
            GamingCard {
                // Vertical Position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vertical Position", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${(vertPercent * 100).toInt()}%", fontSize = 13.sp, color = Color(accentHex))
                }
                Slider(
                    value = vertPercent,
                    onValueChange = {
                        vertPercent = it
                        scope.launch { settingsRepo.updatePosition(isLeft, it) }
                    },
                    valueRange = 0.1f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(accentHex),
                        activeTrackColor = Color(accentHex)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Opacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Handle Opacity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${(opacity * 100).toInt()}%", fontSize = 13.sp, color = Color(accentHex))
                }
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        scope.launch { settingsRepo.updateAppearance(it, sizeMult, compact, accentHex) }
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(accentHex),
                        activeTrackColor = Color(accentHex)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Handle Scale", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${String.format("%.1f", sizeMult)}x", fontSize = 13.sp, color = Color(accentHex))
                }
                Slider(
                    value = sizeMult,
                    onValueChange = {
                        sizeMult = it
                        scope.launch { settingsRepo.updateAppearance(opacity, it, compact, accentHex) }
                    },
                    valueRange = 0.8f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(accentHex),
                        activeTrackColor = Color(accentHex)
                    )
                )
            }
        }

        // Accent Color Selection
        item {
            GamingCard {
                Text(
                    text = "NEON ACCENT COLOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accentColors.forEach { (hex, name) ->
                        val isSelected = accentHex == hex
                        val c = Color(hex)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) c.copy(alpha = 0.25f) else NexusDarkBg)
                                .border(
                                    BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) c else NexusCardBorder),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    accentHex = hex
                                    scope.launch {
                                        settingsRepo.updateAppearance(opacity, sizeMult, compact, hex)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) c else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggles: Compact Mode & Haptics
        item {
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Compact Gaming Handle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Reduces handle profile to avoid blocking touch gestures", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = compact,
                        onCheckedChange = {
                            compact = it
                            scope.launch { settingsRepo.updateAppearance(opacity, sizeMult, it, accentHex) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(accentHex),
                            checkedTrackColor = Color(accentHex).copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Test Handle Button
        item {
            CyberButton(
                text = "Apply & Open Floating Sidebar",
                onClick = {
                    if (!permissionManager.hasOverlayPermission()) {
                        context.startActivity(permissionManager.createOverlaySettingsIntent())
                    } else {
                        OverlayService.startService(context)
                        Toast.makeText(context, "Sidebar settings applied! Check screen edge.", Toast.LENGTH_SHORT).show()
                    }
                },
                accentColor = Color(accentHex),
                testTag = "apply_sidebar_config_button"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
