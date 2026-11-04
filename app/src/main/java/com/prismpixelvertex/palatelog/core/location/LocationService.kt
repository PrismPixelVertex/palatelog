package com.prismpixelvertex.palatelog.core.location

/**
 * Domain interface for location discovery and reverse geocoding.
 * Platform-agnostic contract enabling clean multiplatform implementations
 * (e.g., FusedLocationProviderClient on Android, CoreLocation on iOS).
 */
interface LocationService {
    suspend fun getCurrentLocation(skipGeocoding: Boolean = false): LocationResult?
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Triple<String, String, String>
}
