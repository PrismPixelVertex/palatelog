package com.prismpixelvertex.palatelog.core.designsystem

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prismpixelvertex.palatelog.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported map rendering providers for plug-and-play architecture.
 */
enum class MapProviderType(
    @get:StringRes val nameResId: Int,
    @get:StringRes val attributionResId: Int,
    val isGovernmentCompliant: Boolean,
) {
    MAPPLS(
        nameResId = R.string.map_provider_mappls_name,
        attributionResId = R.string.map_provider_mappls_attr,
        isGovernmentCompliant = true,
    ),
    OSM_DROID(
        nameResId = R.string.map_provider_osm_name,
        attributionResId = R.string.map_provider_osm_attr,
        isGovernmentCompliant = false,
    ),
    GOOGLE_MAPS(
        nameResId = R.string.map_provider_google_name,
        attributionResId = R.string.map_provider_google_attr,
        isGovernmentCompliant = true,
    ),
    OLA_MAPS(
        nameResId = R.string.map_provider_ola_name,
        attributionResId = R.string.map_provider_ola_attr,
        isGovernmentCompliant = true,
    );
}

/**
 * Snapshot of the currently configured and effective map provider state.
 */
data class ActiveMapProviderState(
    val primaryProvider: MapProviderType,
    val effectiveProvider: MapProviderType,
    val isFallbackEngaged: Boolean,
)

private const val TAG = "PalateLog_Map"

/**
 * Runtime configuration and fallback state for map providers.
 */
object MapProviderConfig {
    @Volatile
    var isMapplsInitialized: Boolean = false
        private set

    @Volatile
    var activeProvider: MapProviderType = MapProviderType.MAPPLS

    private val _providerState = MutableStateFlow(resolveState())
    val providerState: StateFlow<ActiveMapProviderState> = _providerState.asStateFlow()

    /**
     * Resolves the active provider, falling back to OSM_DROID if Mappls is unavailable or uninitialized.
     */
    fun getEffectiveProvider(): MapProviderType {
        return resolveState().effectiveProvider
    }

    fun notifyMapplsInitialized(success: Boolean) {
        Log.d(TAG, "notifyMapplsInitialized: success=$success")
        isMapplsInitialized = success
        _providerState.value = resolveState()
    }

    fun notifyFallbackEngaged() {
        Log.w(TAG, "notifyFallbackEngaged: falling back to OSM_DROID")
        isMapplsInitialized = false
        _providerState.value = resolveState()
    }

    private fun resolveState(): ActiveMapProviderState {
        val effective = when {
            activeProvider == MapProviderType.MAPPLS && isMapplsInitialized -> MapProviderType.MAPPLS
            activeProvider == MapProviderType.MAPPLS && !isMapplsInitialized -> MapProviderType.OSM_DROID
            else -> activeProvider
        }
        val isFallback = activeProvider != effective
        return ActiveMapProviderState(
            primaryProvider = activeProvider,
            effectiveProvider = effective,
            isFallbackEngaged = isFallback
        )
    }
}

const val DEFAULT_MAP_CAMERA_ANIMATION_MS = 3000

/**
 * State holder allowing external composables to command the camera imperatively or declaratively.
 */
class PalateMapState(
    initialLatitude: Double = 12.9716,
    initialLongitude: Double = 77.5946,
    initialZoom: Double = 16.0
) {
    var latitude by mutableDoubleStateOf(initialLatitude)
        internal set
    var longitude by mutableDoubleStateOf(initialLongitude)
        internal set
    var zoom by mutableDoubleStateOf(initialZoom)
        internal set
    var cameraAnimationTrigger by mutableLongStateOf(0L)
        private set

    fun animateCamera(lat: Double, lng: Double, zoomLevel: Double = zoom) {
        latitude = lat
        longitude = lng
        zoom = zoomLevel
        cameraAnimationTrigger = System.currentTimeMillis()
    }
}

@Composable
fun rememberPalateMapState(
    latitude: Double = 12.9716,
    longitude: Double = 77.5946,
    zoom: Double = 16.0
): PalateMapState {
    return remember {
        PalateMapState(latitude, longitude, zoom)
    }
}

/**
 * Platform-agnostic marker data contract for PalateLog maps.
 */
data class PalateMapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String = "",
    val customIcon: Drawable? = null
)
