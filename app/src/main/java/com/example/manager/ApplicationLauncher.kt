package com.example.manager

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.example.data.DiagnosticLogger

sealed class AppLaunchResult {
    data object Success : AppLaunchResult()
    data class FallbackToBrowser(val fallbackUrl: String) : AppLaunchResult()
    data class Error(val message: String) : AppLaunchResult()
}

sealed class SplitScreenResult {
    data object LaunchedAdjacent : SplitScreenResult()
    data class FallbackToFloating(val reason: String) : SplitScreenResult()
    data class NotSupported(val reason: String) : SplitScreenResult()
}

data class SocialAppStatus(
    val id: String,
    val name: String,
    val packageName: String,
    val isInstalled: Boolean,
    val icon: Drawable? = null,
    val webFallbackUrl: String
)

class ApplicationLauncher(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    // Target packages for Facebook, Messenger, and Instagram
    private val fbPackages = listOf("com.facebook.katana", "com.facebook.lite")
    private val msgPackages = listOf("com.facebook.orca", "com.facebook.mlite")
    private val instaPackages = listOf("com.instagram.android", "com.instagram.lite")

    fun getFacebookStatus(): SocialAppStatus {
        for (pkg in fbPackages) {
            val status = checkPackage(pkg, "Facebook", "https://m.facebook.com")
            if (status.isInstalled) return status
        }
        return SocialAppStatus(
            id = "facebook",
            name = "Facebook",
            packageName = "com.facebook.katana",
            isInstalled = false,
            icon = null,
            webFallbackUrl = "https://m.facebook.com"
        )
    }

    fun getMessengerStatus(): SocialAppStatus {
        for (pkg in msgPackages) {
            val status = checkPackage(pkg, "Messenger", "https://www.messenger.com")
            if (status.isInstalled) return status
        }
        return SocialAppStatus(
            id = "messenger",
            name = "Messenger",
            packageName = "com.facebook.orca",
            isInstalled = false,
            icon = null,
            webFallbackUrl = "https://www.messenger.com"
        )
    }

    fun getInstagramStatus(): SocialAppStatus {
        for (pkg in instaPackages) {
            val status = checkPackage(pkg, "Instagram", "https://www.instagram.com")
            if (status.isInstalled) return status
        }
        return SocialAppStatus(
            id = "instagram",
            name = "Instagram",
            packageName = "com.instagram.android",
            isInstalled = false,
            icon = null,
            webFallbackUrl = "https://www.instagram.com"
        )
    }

    private fun checkPackage(pkg: String, defaultName: String, fallbackUrl: String): SocialAppStatus {
        return try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            val name = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            SocialAppStatus(pkg, name, pkg, true, icon, fallbackUrl)
        } catch (_: PackageManager.NameNotFoundException) {
            SocialAppStatus(pkg, defaultName, pkg, false, null, fallbackUrl)
        }
    }

    fun launchPackage(packageName: String): AppLaunchResult {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                DiagnosticLogger.log("LAUNCHER", "Successfully launched $packageName")
                AppLaunchResult.Success
            } catch (e: Exception) {
                DiagnosticLogger.log("LAUNCHER", "Failed to launch $packageName: ${e.message}", "ERROR")
                AppLaunchResult.Error("Could not launch application: ${e.message}")
            }
        } else {
            DiagnosticLogger.log("LAUNCHER", "Package $packageName not installed on device", "WARN")
            AppLaunchResult.Error("Application is not installed on this device.")
        }
    }

    fun launchSplitScreenAdjacent(packageName: String): SplitScreenResult {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return SplitScreenResult.FallbackToFloating("Application is not installed.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                val options = ActivityOptions.makeBasic()
                context.startActivity(launchIntent, options.toBundle())
                DiagnosticLogger.log("SPLIT_SCREEN", "Dispatched FLAG_ACTIVITY_LAUNCH_ADJACENT for $packageName")
                return SplitScreenResult.LaunchedAdjacent
            } catch (e: Exception) {
                DiagnosticLogger.log("SPLIT_SCREEN", "OS rejected split-screen: ${e.message}", "WARN")
                return SplitScreenResult.FallbackToFloating("Android multi-window rejected launch. Switch to Floating Mini-Window.")
            }
        } else {
            return SplitScreenResult.NotSupported("Multi-window is not supported on this Android version.")
        }
    }

    fun openPlayStore(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
