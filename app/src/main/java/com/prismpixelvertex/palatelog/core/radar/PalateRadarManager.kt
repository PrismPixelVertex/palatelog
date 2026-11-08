package com.prismpixelvertex.palatelog.core.radar

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.model.Verdict
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalateRadarManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val restaurantDao: RestaurantDao,
) {

    companion object {
        private const val TAG = "PalateRadar_Manager"

        /**
         * Trigger radius for high-priority road warnings (500 meters).
         * Provides ample stopping/exit notice at highway speeds (80-100 km/h).
         */
        const val GEOFENCE_RADIUS_METERS = 500f

        /**
         * Hard limit enforced by Google Play Services Geofencing API per application.
         */
        const val MAX_GEOFENCES = 100

        /**
         * Maximum search radius along the travel corridor from the current GPS location.
         */
        const val CORRIDOR_RADIUS_KM = 500.0
    }

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, PalateRadarReceiver::class.java).apply {
            action = PalateRadarReceiver.ACTION_RADAR_GEOFENCE
        }
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Queries saved highway stops within [CORRIDOR_RADIUS_KM] of ([centerLat], [centerLng]),
     * filters and prioritizes up to [MAX_GEOFENCES] spots, and registers geofences with Play Services.
     *
     * @return Number of registered geofences.
     */
    suspend fun registerRadarGeofences(
        centerLat: Double,
        centerLng: Double,
        includeGoRecommendations: Boolean = false,
    ): Int {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "ACCESS_FINE_LOCATION not granted; cannot register radar geofences")
            return 0
        }

        val box = GeoUtils.getBoundingBox(centerLat, centerLng, CORRIDOR_RADIUS_KM)
        val rawCandidates = restaurantDao.getRestaurantsInBoundingBox(
            minLat = box.minLat,
            maxLat = box.maxLat,
            minLng = box.minLng,
            maxLng = box.maxLng
        )

        // Filter valid coordinates within exact Haversine radius
        val eligibleSpots = rawCandidates.filter { spot ->
            val lat = spot.latitude
            val lng = spot.longitude
            if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                val distanceKm = GeoUtils.calculateDistanceKm(centerLat, centerLng, lat, lng)
                distanceKm <= CORRIDOR_RADIUS_KM
            } else {
                false
            }
        }

        // Filter based on user preferences and prioritize regrets (NO_GO) first
        val prioritizedCandidates = eligibleSpots
            .filter { spot ->
                val verdict = spot.overallVerdict
                when (verdict) {
                    Verdict.NO_GO.name -> true
                    Verdict.CAUTION.name -> true
                    Verdict.GO.name -> includeGoRecommendations
                    else -> true
                }
            }
            .sortedWith(
                compareBy<RestaurantEntity> { spot ->
                    // 0 = NO_GO (Top priority), 1 = CAUTION, 2 = Other / GO
                    when (spot.overallVerdict) {
                        Verdict.NO_GO.name -> 0
                        Verdict.CAUTION.name -> 1
                        else -> 2
                    }
                }.thenBy { spot ->
                    GeoUtils.calculateDistanceKm(centerLat, centerLng, spot.latitude!!, spot.longitude!!)
                }
            )
            .take(MAX_GEOFENCES)

        // Always tear down previously registered geofences before registering a fresh set
        clearAllGeofences()

        if (prioritizedCandidates.isEmpty()) {
            Log.i(TAG, "No candidate highway stops found within $CORRIDOR_RADIUS_KM km")
            return 0
        }

        val geofenceList = prioritizedCandidates.map { spot ->
            Geofence.Builder()
                .setRequestId(spot.id)
                .setCircularRegion(
                    spot.latitude!!,
                    spot.longitude!!,
                    GEOFENCE_RADIUS_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        return try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.i(TAG, "Successfully registered ${geofenceList.size} radar geofences with Play Services")
            geofenceList.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register radar geofences with Play Services", e)
            0
        }
    }

    /**
     * Immediately de-registers all Palate Radar geofences from Google Play Services.
     * Guaranteed clean teardown to prevent any background location checks when Radar is stopped.
     */
    suspend fun clearAllGeofences() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            Log.d(TAG, "Cleared all active radar geofences")
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove radar geofences (may already be empty)", e)
        }
    }
}
