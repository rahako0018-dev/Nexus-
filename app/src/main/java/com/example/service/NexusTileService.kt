package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.manager.PermissionManager

@RequiresApi(Build.VERSION_CODES.N)
class NexusTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val permissionManager = PermissionManager(this)
        if (!permissionManager.hasOverlayPermission()) {
            val intent = permissionManager.createOverlaySettingsIntent()
            startActivityAndCollapse(intent)
            return
        }

        val isRunning = OverlayService.isRunning.value
        if (isRunning) {
            OverlayService.stopService(this)
        } else {
            OverlayService.startService(this)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = OverlayService.isRunning.value
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Nexus: Active" else "Nexus Sidebar"
        tile.updateTile()
    }
}
