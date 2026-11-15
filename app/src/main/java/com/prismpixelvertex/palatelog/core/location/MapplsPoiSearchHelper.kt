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
    val distanceMeters: Long? = null,
    val source: String = "Mappls"
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

        Log.d(TAG, "[POI Search] Initiating dining search at ($latitude, $longitude), radius=${radiusMeters}m")

        // Level 1: Try Mappls Nearby Search if Mappls map engine is initialized
        if (MapProviderConfig.isMapplsInitialized) {
            try {
                val mapplsResults = searchMapplsNearby(latitude, longitude, radiusMeters)
                if (mapplsResults.isNotEmpty()) {
                    return mapplsResults
                }
                Log.i(TAG, "[Level 1 - Mappls] Mappls search returned 0 results. Falling back to Level 2 (OpenStreetMap)...")
            } catch (e: Exception) {
                Log.w(TAG, "[Level 1 - Mappls] Mappls search threw exception, falling back to Level 2 (OpenStreetMap)", e)
            }
        } else {
            Log.i(TAG, "[Level 1 - Mappls] Mappls map engine not initialized. Skipping to Level 2 (OpenStreetMap)...")
        }

        // Level 2: Fallback to OpenStreetMap food POI search (Option A.2)
        return try {
            val osmResults = searchOsmFoodPois(latitude, longitude, radiusMeters)
            if (osmResults.isNotEmpty()) {
                Log.d(TAG, "[Level 2 - OSM Fallback] Discovered ${osmResults.size} POIs via OpenStreetMap")
            } else {
                Log.w(TAG, "[Level 2 - OSM Fallback] 0 POIs discovered via OpenStreetMap fallback")
            }
            osmResults
        } catch (e: Exception) {
            Log.w(TAG, "[Level 2 - OSM Fallback] OSM food POI fallback search failed", e)
            emptyList()
        }
    }

    private suspend fun searchMapplsNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<RestaurantPoi> {
        // Multi-keyword dining query using Mappls ';' (OR) operator to search across categories in a single call:
        // Prioritizes official Mappls category code FODCOF (Food & Coffee) for higher-quality verified listings,
        // with natural keywords (restaurant, cafe, food) as fallbacks.
        val diningKeywords = "FODCOF;restaurant;cafe;food"
        Log.d(TAG, "[Level 1 - Mappls] Querying Mappls Nearby with multi-keyword '$diningKeywords'...")
        val results = executeMapplsNearbyCall(latitude, longitude, radiusMeters, diningKeywords, "Level 1 - Mappls")
        if (results.isNotEmpty()) {
            Log.d(TAG, "[Level 1 - Mappls] SUCCESS: Discovered ${results.size} POIs via multi-keyword '$diningKeywords'")
            return results
        }
        Log.i(TAG, "[Level 1 - Mappls] 0 POIs returned for '$diningKeywords'")
        return emptyList()
    }

    private suspend fun executeMapplsNearbyCall(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        keyword: String,
        levelTag: String
    ): List<RestaurantPoi> = suspendCancellableCoroutine { continuation ->
        try {
            val nearby = MapplsNearby.builder()
                .setLocation(latitude, longitude)
                .keyword(keyword)
                .radius(radiusMeters)
                .itemCount(30)
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
                                distanceMeters = item.distance,
                                source = "Mappls"
                            )
                        } else {
                            null
                        }
                    }
                    continuation.resume(pois)
                }

                override fun onError(code: Int, message: String?) {
                    Log.w(TAG, "[$levelTag] Mappls search ('$keyword') returned: code=$code, message=$message")
                    continuation.resume(emptyList())
                }
            })

            continuation.invokeOnCancellation {
                try {
                    manager.cancel()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "[$levelTag] Mappls search ('$keyword') exception", e)
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

        // Level 2a: Primary query using q=restaurant to cover all dining categories (mess, dhabas, fast food, cafes)
        Log.d(TAG, "[Level 2 - OSM Fallback] Querying Nominatim with q=restaurant in bounding box...")
        val primaryUrl = "https://nominatim.openstreetmap.org/search" +
                "?format=json" +
                "&q=restaurant" +
                "&viewbox=${box.minLng},${box.maxLat},${box.maxLng},${box.minLat}" +
                "&bounded=1" +
                "&limit=40"

        val primaryRequest = Request.Builder()
            .url(primaryUrl)
            .header("User-Agent", "PalateLog-Android/1.0 (Food POI Discovery)")
            .build()

        val results = fetchOsmResults(primaryRequest, latitude, longitude, "q=restaurant")
        if (results.isNotEmpty()) {
            Log.d(TAG, "[Level 2 - OSM Fallback] SUCCESS: Discovered ${results.size} POIs via q=restaurant")
            return@withContext results
        }

        // Level 2b: Secondary fallback using amenity=restaurant
        Log.i(TAG, "[Level 2 - OSM Fallback] 0 results for q=restaurant. Trying secondary fallback (amenity=restaurant)...")
        val fallbackUrl = "https://nominatim.openstreetmap.org/search" +
                "?format=json" +
                "&amenity=restaurant" +
                "&viewbox=${box.minLng},${box.maxLat},${box.maxLng},${box.minLat}" +
                "&bounded=1" +
                "&limit=40"

        val fallbackRequest = Request.Builder()
            .url(fallbackUrl)
            .header("User-Agent", "PalateLog-Android/1.0 (Food POI Discovery)")
            .build()

        val fallbackResults = fetchOsmResults(fallbackRequest, latitude, longitude, "amenity=restaurant")
        if (fallbackResults.isNotEmpty()) {
            Log.d(TAG, "[Level 2 - OSM Fallback] SUCCESS: Discovered ${fallbackResults.size} POIs via amenity=restaurant")
        }
        fallbackResults
    }

    private fun fetchOsmResults(
        request: Request,
        centerLat: Double,
        centerLng: Double,
        queryType: String
    ): List<RestaurantPoi> {
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "[Level 2 - OSM Fallback] HTTP error for $queryType: code=${response.code}")
                    return emptyList()
                }
                val body = response.body.string()
                if (body.isBlank()) return emptyList()

                val jsonArray = JSONArray(body)
                val results = mutableListOf<RestaurantPoi>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val rawName = item.optString("name").trim()
                    val displayName = item.optString("display_name").trim()
                    val name = when {
                        rawName.isNotBlank() -> rawName
                        displayName.isNotBlank() -> displayName.split(",").firstOrNull()?.trim().orEmpty()
                        else -> ""
                    }
                    val lat = item.optDouble("lat", 0.0)
                    val lon = item.optDouble("lon", 0.0)
                    val placeId = item.optString("place_id", "${lat}_${lon}")

                    if (name.isNotBlank() && lat != 0.0 && lon != 0.0) {
                        val distKm = GeoUtils.calculateDistanceKm(centerLat, centerLng, lat, lon)
                        results.add(
                            RestaurantPoi(
                                id = placeId,
                                name = name,
                                address = displayName,
                                latitude = lat,
                                longitude = lon,
                                distanceMeters = (distKm * 1000).toLong(),
                                source = "OSM"
                            )
                        )
                    }
                }
                results.sortedBy { it.distanceMeters ?: Long.MAX_VALUE }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[Level 2 - OSM Fallback] Exception while querying $queryType", e)
            emptyList()
        }
    }
}
