package com.prismpixelvertex.palatelog.core.location

sealed interface GeocodeResult {
    data class Success(val placeName: String, val street: String, val city: String) : GeocodeResult
    data class NetworkUnavailable(val message: String = "") : GeocodeResult
    data class Failed(val message: String = "") : GeocodeResult
}

/**
 * Domain interface for location discovery and reverse geocoding.
 * Platform-agnostic contract enabling clean multiplatform implementations
 * (e.g., FusedLocationProviderClient on Android, CoreLocation on iOS).
 */
interface LocationService {
    suspend fun getCurrentLocation(skipGeocoding: Boolean = false): LocationResult?
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Triple<String, String, String>
    suspend fun reverseGeocodeWithStatus(latitude: Double, longitude: Double): GeocodeResult
    fun isLocationEnabled(): Boolean
    fun isNetworkAvailable(): Boolean
}

