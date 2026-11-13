package com.prismpixelvertex.palatelog.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Universal composable map facade.
 * Isolates the underlying map vendor (Mappls vs OSMDroid vs future Google Maps)
 * from screen layouts so that map providers can be switched or fallen back to seamlessly.
 */
@Composable
fun PalateInteractiveMap(
    modifier: Modifier = Modifier,
    centerLatitude: Double = 12.9716,
    centerLongitude: Double = 77.5946,
    zoomLevel: Double = 16.0,
    animationTrigger: Long = 0L,
    state: PalateMapState? = null,
    markers: List<PalateMapMarker> = emptyList(),
    onSingleTap: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onLongPress: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onMarkerClick: ((markerId: String) -> Unit)? = null,
    onCameraIdle: ((latitude: Double, longitude: Double, zoom: Double) -> Unit)? = null,
    provider: MapProviderType = MapProviderConfig.getEffectiveProvider()
) {
    var fallbackToOsm by remember { mutableStateOf(false) }

    val effectiveProvider = if (fallbackToOsm) {
        MapProviderType.OSM_DROID
    } else {
        provider
    }

    val targetLat = state?.latitude ?: centerLatitude
    val targetLng = state?.longitude ?: centerLongitude
    val targetZoom = state?.zoom ?: zoomLevel
    val effectiveAnimationTrigger = state?.cameraAnimationTrigger ?: animationTrigger

    when (effectiveProvider) {
        MapProviderType.MAPPLS -> {
            MapplsMapRenderer(
                modifier = modifier,
                centerLatitude = targetLat,
                centerLongitude = targetLng,
                zoomLevel = targetZoom,
                animationTrigger = effectiveAnimationTrigger,
                markers = markers,
                onSingleTap = onSingleTap,
                onLongPress = onLongPress,
                onMarkerClick = onMarkerClick,
                onCameraIdle = onCameraIdle,
                onInitializationFailed = {
                    fallbackToOsm = true
                    MapProviderConfig.notifyFallbackEngaged()
                }
            )
        }
        MapProviderType.OSM_DROID -> {
            OsmDroidMapRenderer(
                modifier = modifier,
                centerLatitude = targetLat,
                centerLongitude = targetLng,
                zoomLevel = targetZoom,
                animationTrigger = effectiveAnimationTrigger,
                markers = markers,
                onSingleTap = onSingleTap,
                onLongPress = onLongPress,
                onMarkerClick = onMarkerClick
            )
        }
        MapProviderType.GOOGLE_MAPS, MapProviderType.OLA_MAPS -> {
            // Future plug-and-play integrations: fallback gracefully to OsmDroid until native renderers are plugged in
            OsmDroidMapRenderer(
                modifier = modifier,
                centerLatitude = targetLat,
                centerLongitude = targetLng,
                zoomLevel = targetZoom,
                animationTrigger = effectiveAnimationTrigger,
                markers = markers,
                onSingleTap = onSingleTap,
                onLongPress = onLongPress,
                onMarkerClick = onMarkerClick
            )
        }
    }
}
