package com.prismpixelvertex.palatelog.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val placeName: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = ""
)

@Singleton
class LocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationService {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(skipGeocoding: Boolean): LocationResult? = withContext(Dispatchers.IO) {
        val cancellationTokenSource = CancellationTokenSource()

        val location: Location? = suspendCancellableCoroutine { continuation ->
            try {
                // 1. Try cached lastLocation first for instant response
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        continuation.resume(lastLoc)
                    } else {
                        // 2. Fall back to requesting fresh location fix
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.token
                        ).addOnSuccessListener { freshLoc ->
                            continuation.resume(freshLoc)
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener {
                    // Fall back to getCurrentLocation if lastLocation task failed
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { freshLoc ->
                        continuation.resume(freshLoc)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            } catch (_: Exception) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }

        if (location != null) {
            // Reverse geocode with a short 500ms timeout unless skipGeocoding is requested (for fast distance sorting)
            val geocoded = if (!skipGeocoding) {
                try {
                    withTimeoutOrNull(500.milliseconds) {
                        reverseGeocode(location.latitude, location.longitude)
                    } ?: Triple("", "", "")
                } catch (_: Exception) {
                    Triple("", "", "")
                }
            } else {
                Triple("", "", "")
            }

            LocationResult(
                latitude = location.latitude,
                longitude = location.longitude,
                placeName = geocoded.first,
                street = geocoded.second,
                city = geocoded.third
            )
        } else {
            null
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): Triple<String, String, String> =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()

                if (address != null) {
                    val placeName = address.featureName ?: address.subLocality ?: address.locality ?: ""
                    val street = address.thoroughfare
                        ?: address.subLocality
                        ?: address.subAdminArea
                        ?: address.getAddressLine(0)?.split(",")?.firstOrNull()?.trim()
                        ?: address.featureName
                        ?: ""
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                    Triple(placeName, street, city)
                } else {
                    Triple("", "", "")
                }
            } catch (e: Exception) {
                Triple("", "", "")
            }
        }
}
