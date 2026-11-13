package com.prismpixelvertex.palatelog.core.location

import android.util.Log
import com.mappls.sdk.services.api.OnResponseCallback
import com.mappls.sdk.services.api.nearby.MapplsNearby
import com.mappls.sdk.services.api.nearby.MapplsNearbyManager
import com.mappls.sdk.services.api.nearby.model.NearbyAtlasResponse
import com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
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
 * Helper to discover nearby restaurants and dining POIs using Mappls Nearby Search API,
 * with automatic OpenStreetMap (Nominatim) fallback (Option A.2) when Mappls credentials
 * are not configured or when Mappls Nearby search returns no results.
 */
object MapplsPoiSearchHelper {
    private const val TAG = "PalateLog_PoiSearch"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun searchNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 1500
    ): List<RestaurantPoi> {
        if (latitude == 0.0 || longitude == 0.0) {
            return emptyList()
        }

        // 1. Try Mappls Nearby Search if Mappls map engine is initialized
        if (MapProviderConfig.isMapplsInitialized) {
            try {
                val mapplsResults = searchMapplsNearby(latitude, longitude, radiusMeters)
                if (mapplsResults.isNotEmpty()) {
                    Log.d(TAG, "Discovered ${mapplsResults.size} POIs via Mappls Nearby")
                    return mapplsResults
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mappls nearby search failed, falling back to OpenStreetMap", e)
            }
        }

        // 2. Fallback to OpenStreetMap food POI search (Option A.2)
        return try {
            val osmResults = searchOsmFoodPois(latitude, longitude, radiusMeters)
            Log.d(TAG, "Discovered ${osmResults.size} POIs via OpenStreetMap fallback")
            osmResults
        } catch (e: Exception) {
            Log.w(TAG, "OSM food POI fallback search failed", e)
            emptyList()
        }
    }

    private suspend fun searchMapplsNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<RestaurantPoi> = suspendCancellableCoroutine { continuation ->
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
                    Log.w(TAG, "Mappls nearby search returned error: code=$code, message=$message")
                    continuation.resume(emptyList())
                }
            })

            continuation.invokeOnCancellation {
                try {
                    manager.cancel()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mappls nearby exception", e)
            continuation.resume(emptyList())
        }
    }

    private suspend fun searchOsmFoodPois(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<RestaurantPoi> = withContext(Dispatchers.IO) {
        val radiusKm = (radiusMeters / 1000.0).coerceAtLeast(0.5)
        val box = GeoUtils.getBoundingBox(latitude, longitude, radiusKm)
        // Nominatim viewbox parameter: <minLon>,<maxLat>,<maxLon>,<minLat>
        val url = "https://nominatim.openstreetmap.org/search" +
                "?format=json" +
                "&amenity=restaurant" +
                "&viewbox=${box.minLng},${box.maxLat},${box.maxLng},${box.minLat}" +
                "&bounded=1" +
                "&limit=40"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PalateLog-Android/1.0 (Food POI Discovery)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "OSM search HTTP failed: code=${response.code}")
                return@withContext emptyList()
            }
            val body = response.body.string()
            if (body.isBlank()) return@withContext emptyList()

            val jsonArray = JSONArray(body)
            val results = mutableListOf<RestaurantPoi>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val name = item.optString("name").trim()
                val lat = item.optDouble("lat", 0.0)
                val lon = item.optDouble("lon", 0.0)
                val placeId = item.optString("place_id", "${lat}_${lon}")
                val displayName = item.optString("display_name").trim()

                if (name.isNotBlank() && lat != 0.0 && lon != 0.0) {
                    val distKm = GeoUtils.calculateDistanceKm(latitude, longitude, lat, lon)
                    results.add(
                        RestaurantPoi(
                            id = placeId,
                            name = name,
                            address = displayName,
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = (distKm * 1000).toLong()
                        )
                    )
                }
            }
            results.sortedBy { it.distanceMeters ?: Long.MAX_VALUE }
        }
    }
}
