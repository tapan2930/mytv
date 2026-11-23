package com.localcast.phone.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.localcast.core.network.NetworkUtils
import com.localcast.phone.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ServerTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = scope.launch {
            ServerStateHolder.isServerRunning.collect { isRunning ->
                updateTileState(isRunning)
            }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = ServerStateHolder.isServerRunning.value
        if (isRunning) {
            LocalServerForegroundService.stopService(this)
            updateTileState(false)
        } else {
            if (!NetworkUtils.isWifiConnected(this)) {
                Toast.makeText(this, "Connect to Wi-Fi first to start My TV Server", Toast.LENGTH_SHORT).show()
                updateTileState(false)
                return
            }
            LocalServerForegroundService.startService(this)
            updateTileState(true)
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_mytv_server)
        tile.label = "My TV Server"
        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val ip = ServerStateHolder.serverIp.value
                val port = ServerStateHolder.serverPort.value
                tile.subtitle = if (ip != null) "$ip:$port" else "Online"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Off"
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        super.onDestroy()
    }
}
