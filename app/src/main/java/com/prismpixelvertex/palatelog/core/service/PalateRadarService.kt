package com.prismpixelvertex.palatelog.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.prismpixelvertex.palatelog.core.location.LocationClient
import com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager
import com.prismpixelvertex.palatelog.core.radar.PalateRadarManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PalateRadarService : Service() {

    companion object {
        private const val TAG = "PalateRadar_Service"

        const val ACTION_START_RADAR = "com.prismpixelvertex.palatelog.ACTION_START_RADAR"
        const val ACTION_STOP_RADAR = "com.prismpixelvertex.palatelog.ACTION_STOP_RADAR"
        const val EXTRA_INCLUDE_GO = "extra_include_go"

        private val _isRadarActive = MutableStateFlow(false)
        val isRadarActive: StateFlow<Boolean> = _isRadarActive.asStateFlow()

        private val _activeStopCount = MutableStateFlow(0)
        val activeStopCount: StateFlow<Int> = _activeStopCount.asStateFlow()

        fun startRadar(context: Context, includeGo: Boolean = false) {
            val intent = Intent(context, PalateRadarService::class.java).apply {
                action = ACTION_START_RADAR
                putExtra(EXTRA_INCLUDE_GO, includeGo)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopRadar(context: Context) {
            val intent = Intent(context, PalateRadarService::class.java).apply {
                action = ACTION_STOP_RADAR
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var notificationManager: PalateNotificationManager

    @Inject
    lateinit var radarManager: PalateRadarManager

    @Inject
    lateinit var locationClient: LocationClient

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_RADAR) {
            Log.i(TAG, "Received ACTION_STOP_RADAR: stopping trip monitor")
            teardownAndStop()
            return START_NOT_STICKY
        }

        Log.i(TAG, "Starting Palate Radar location foreground service")
        val initialNotification = notificationManager.buildRadarServiceNotification(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                PalateNotificationManager.NOTIFICATION_ID_RADAR_SERVICE,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(PalateNotificationManager.NOTIFICATION_ID_RADAR_SERVICE, initialNotification)
        }

        _isRadarActive.value = true

        val prefs = getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        val includeGo = intent?.getBooleanExtra(
            EXTRA_INCLUDE_GO,
            prefs.getBoolean("radar_include_go", false)
        ) ?: false

        serviceScope.launch(Dispatchers.IO) {
            try {
                val loc = locationClient.getCurrentLocation(skipGeocoding = true)
                val count = if (loc != null) {
                    radarManager.registerRadarGeofences(
                        centerLat = loc.latitude,
                        centerLng = loc.longitude,
                        includeGoRecommendations = includeGo
                    )
                } else {
                    Log.w(TAG, "Could not acquire current GPS fix; 0 geofences registered")
                    0
                }

                _activeStopCount.value = count
                val updatedNotification = notificationManager.buildRadarServiceNotification(count)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(PalateNotificationManager.NOTIFICATION_ID_RADAR_SERVICE, updatedNotification)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing radar geofences in foreground service", e)
            } finally {
                PalateRadarTileService.requestTileUpdate(this@PalateRadarService)
            }
        }

        return START_STICKY
    }

    private fun teardownAndStop() {
        _isRadarActive.value = false
        _activeStopCount.value = 0
        serviceScope.launch(Dispatchers.IO) {
            radarManager.clearAllGeofences()
        }
        notificationManager.clearAllRadarNotifications()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        PalateRadarTileService.requestTileUpdate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "PalateRadarService onDestroy: tearing down geofences")
        _isRadarActive.value = false
        _activeStopCount.value = 0
        serviceScope.launch(Dispatchers.IO) {
            radarManager.clearAllGeofences()
        }
        notificationManager.clearAllRadarNotifications()
        serviceJob.cancel()
        PalateRadarTileService.requestTileUpdate(this)
    }
}
