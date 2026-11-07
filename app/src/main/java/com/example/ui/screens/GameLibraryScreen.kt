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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.manager.ApplicationLauncher
import com.example.manager.GameLibraryManager
import com.example.manager.PermissionManager
import com.example.model.GamingProfileType
import com.example.model.InstalledGame
import com.example.service.OverlayService
import com.example.ui.components.CyberButton
import com.example.ui.components.GamingCard
import com.example.ui.components.NeonBadge
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusSurfaceCard
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun GameLibraryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val libraryManager = remember { GameLibraryManager(context) }
    val appLauncher = remember { ApplicationLauncher(context) }
    val permissionManager = remember { PermissionManager(context) }

    val games by libraryManager.games.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var allInstalledApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        libraryManager.loadGames()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header + Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GAME LIBRARY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${games.size} Titles Configured",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(
                    onClick = {
                        scope.launch { libraryManager.loadGames() }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            allInstalledApps = libraryManager.getAllInstalledApps()
                            showAddDialog = true
                        }
                    },
                    modifier = Modifier
                        .testTag("add_game_button")
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, NeonCyan), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Game", tint = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Games in Library",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap + to add games or applications installed on your phone.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CyberButton(
                        text = "Scan & Add Installed Game",
                        onClick = {
                            scope.launch {
                                allInstalledApps = libraryManager.getAllInstalledApps()
                                showAddDialog = true
                            }
                        },
                        modifier = Modifier.width(240.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games, key = { it.packageName }) { game ->
                    GameItemCard(
                        game = game,
                        onToggleFavorite = {
                            libraryManager.toggleFavorite(game)
                        },
                        onSelectProfile = { profile ->
                            libraryManager.setGameProfile(game.packageName, profile)
                        },
                        onLaunch = {
                            // 1. Ensure overlay is running
                            if (permissionManager.hasOverlayPermission()) {
                                OverlayService.startService(context)
                            }
                            // 2. Launch game
                            libraryManager.recordGameLaunch(game.packageName)
                            appLauncher.launchPackage(game.packageName)
                        },
                        onRemove = {
                            libraryManager.removeGame(game.packageName)
                            scope.launch { libraryManager.loadGames() }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Add Game Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Select Application to Add",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allInstalledApps) { (pkg, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NexusSurfaceVariant)
                                .clickable {
                                    libraryManager.addGame(pkg, label)
                                    scope.launch { libraryManager.loadGames() }
                                    showAddDialog = false
                                    Toast.makeText(context, "Added $label to Game Library", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = pkg,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = NexusSurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun GameItemCard(
    game: InstalledGame,
    onToggleFavorite: () -> Unit,
    onSelectProfile: (GamingProfileType) -> Unit,
    onLaunch: () -> Unit,
    onRemove: () -> Unit
) {
    GamingCard(
        borderColor = if (game.isFavorite) NeonAmber.copy(alpha = 0.6f) else NexusCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusSurfaceVariant)
                        .border(BorderStroke(1.dp, NexusCardBorder), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = game.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Launched ${game.launchCount} times",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) NeonAmber else TextMuted
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GamingProfileType.values().forEach { profile ->
                val isSelected = game.profileType == profile
                val color = Color(profile.badgeColorHex)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) color.copy(alpha = 0.2f) else NexusDarkBg)
                        .border(
                            BorderStroke(1.dp, if (isSelected) color else NexusCardBorder),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectProfile(profile) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.displayName.split(" ").first(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) color else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CyberButton(
            text = "Launch with Nexus Sidebar",
            onClick = onLaunch,
            accentColor = NeonCyan,
            modifier = Modifier.height(42.dp),
            testTag = "launch_game_${game.packageName}"
        )
    }
}
