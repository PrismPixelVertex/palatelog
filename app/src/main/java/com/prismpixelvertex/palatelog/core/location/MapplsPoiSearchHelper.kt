package com.prismpixelvertex.palatelog.core.location

import android.util.Log
import com.mappls.sdk.services.api.OnResponseCallback
import com.mappls.sdk.services.api.nearby.MapplsNearby
import com.mappls.sdk.services.api.nearby.MapplsNearbyManager
import com.mappls.sdk.services.api.nearby.model.NearbyAtlasResponse
import com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class RestaurantPoi(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Long? = null
)

/**
 * Helper to discover nearby restaurants and dining POIs using Mappls Nearby Search API.
 * Uses official Mappls Category Code `DINFOD` (Restaurants & Dining).
 */
object MapplsPoiSearchHelper {
    private const val TAG = "PalateLog_MapplsPoi"

    suspend fun searchNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 1000
    ): List<RestaurantPoi> {
        if (!MapProviderConfig.isMapplsInitialized || latitude == 0.0 || longitude == 0.0) {
            return emptyList()
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val nearby = MapplsNearby.builder()
                    .setLocation(latitude, longitude)
                    .keyword("DINFOD")
                    .radius(radiusMeters)
                    .build()

                val manager = MapplsNearbyManager.newInstance(nearby)
                manager.call(object : OnResponseCallback<NearbyAtlasResponse> {
                    override fun onSuccess(response: NearbyAtlasResponse?) {
                        val suggested = response?.suggestedLocations.orEmpty()
                        val pois = suggested.mapNotNull { item ->
                            val lat = item.latitude
                            val lng = item.longitude
                            val name = item.placeName?.trim()
                            if (lat != null && lng != null && lat != 0.0 && lng != 0.0 && !name.isNullOrBlank()) {
                                RestaurantPoi(
                                    id = item.mapplsPin ?: "${lat}_${lng}",
                                    name = name,
                                    address = item.placeAddress?.trim().orEmpty(),
                                    latitude = lat,
                                    longitude = lng,
                                    distanceMeters = item.distance
                                )
                            } else {
                                null
                            }
                        }
                        continuation.resume(pois)
                    }

                    override fun onError(code: Int, message: String?) {
                        Log.w(TAG, "Mappls nearby search error: code=$code, message=$message")
                        continuation.resume(emptyList())
                    }
                })

                continuation.invokeOnCancellation {
                    try {
                        manager.cancel()
                    } catch (_: Exception) {
                        // Ignore cancellation cleanup errors
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mappls nearby search exception", e)
                continuation.resume(emptyList())
            }
        }
    }
}
