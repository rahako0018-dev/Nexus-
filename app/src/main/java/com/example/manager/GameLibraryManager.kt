package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.DiagnosticLogger
import com.example.model.GamingProfileType
import com.example.model.InstalledGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GameLibraryManager(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private val prefs = context.getSharedPreferences("nexus_game_library", Context.MODE_PRIVATE)

    private val _games = MutableStateFlow<List<InstalledGame>>(emptyList())
    val games: StateFlow<List<InstalledGame>> = _games.asStateFlow()

    suspend fun loadGames() = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val loadedList = mutableListOf<InstalledGame>()

        for (resolve in resolveInfos) {
            val pkg = resolve.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip Nexus itself

            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                val isSystemGame = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                        (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                appInfo.category == ApplicationInfo.CATEGORY_GAME)

                val isSaved = prefs.contains("game_$pkg")
                val isFavorite = prefs.getBoolean("fav_$pkg", false)
                val profileName = prefs.getString("prof_$pkg", GamingProfileType.BALANCED.name)
                val profile = try {
                    GamingProfileType.valueOf(profileName ?: GamingProfileType.BALANCED.name)
                } catch (_: Exception) {
                    GamingProfileType.BALANCED
                }
                val launchCount = prefs.getInt("count_$pkg", 0)

                // Include if system recognized as game, or user explicitly saved/favorited, or popular social app
                if (isSystemGame || isSaved || isFavorite) {
                    loadedList.add(
                        InstalledGame(
                            packageName = pkg,
                            appName = label,
                            isFavorite = isFavorite,
                            profileType = profile,
                            launchCount = launchCount
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Sort: favorites first, then by launch count, then alphabetically
        _games.value = loadedList.sortedWith(
            compareByDescending<InstalledGame> { it.isFavorite }
                .thenByDescending { it.launchCount }
                .thenBy { it.appName }
        )
    }

    suspend fun getAllInstalledApps(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val apps = mutableListOf<Pair<String, String>>()
        for (resolve in resolveInfos) {
            val pkg = resolve.activityInfo.packageName
            if (pkg == context.packageName) continue
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                apps.add(pkg to label)
            } catch (_: Exception) {}
        }
        apps.sortedBy { it.second }
    }

    fun addGame(packageName: String, appName: String) {
        prefs.edit().putBoolean("game_$packageName", true).apply()
        DiagnosticLogger.log("GAME_LIB", "Added $appName to library")
    }

    fun removeGame(packageName: String) {
        prefs.edit()
            .remove("game_$packageName")
            .remove("fav_$packageName")
            .remove("prof_$packageName")
            .apply()
        DiagnosticLogger.log("GAME_LIB", "Removed $packageName from library")
    }

    fun toggleFavorite(game: InstalledGame) {
        val newFav = !game.isFavorite
        prefs.edit().putBoolean("fav_${game.packageName}", newFav).apply()
        _games.value = _games.value.map {
            if (it.packageName == game.packageName) it.copy(isFavorite = newFav) else it
        }.sortedWith(
            compareByDescending<InstalledGame> { it.isFavorite }
                .thenByDescending { it.launchCount }
                .thenBy { it.appName }
        )
    }

    fun setGameProfile(packageName: String, profile: GamingProfileType) {
        prefs.edit().putString("prof_$packageName", profile.name).apply()
        _games.value = _games.value.map {
            if (it.packageName == packageName) it.copy(profileType = profile) else it
        }
    }

    fun recordGameLaunch(packageName: String) {
        val current = prefs.getInt("count_$packageName", 0)
        prefs.edit()
            .putInt("count_$packageName", current + 1)
            .putLong("last_$packageName", System.currentTimeMillis())
            .apply()
    }
}
