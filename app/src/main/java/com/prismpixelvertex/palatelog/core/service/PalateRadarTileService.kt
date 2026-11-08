package com.prismpixelvertex.palatelog.core.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.prismpixelvertex.palatelog.R

class PalateRadarTileService : TileService() {

    companion object {
        private const val TAG = "PalateRadar_Tile"

        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, PalateRadarTileService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not request tile update", e)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isCurrentlyActive = PalateRadarService.isRadarActive.value
        if (isCurrentlyActive) {
            Log.i(TAG, "Tile clicked: stopping Palate Radar")
            PalateRadarService.stopRadar(this)
        } else {
            Log.i(TAG, "Tile clicked: starting Palate Radar")
            PalateRadarService.startRadar(this)
        }
        updateTileState(!isCurrentlyActive)
    }

    private fun updateTileState(overrideActive: Boolean? = null) {
        val tile = qsTile ?: return
        val isActive = overrideActive ?: PalateRadarService.isRadarActive.value

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.radar_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) {
                getString(R.string.radar_tile_state_active)
            } else {
                getString(R.string.radar_tile_state_inactive)
            }
        }
        tile.updateTile()
    }
}
