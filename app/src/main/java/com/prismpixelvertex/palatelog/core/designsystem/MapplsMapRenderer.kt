package com.prismpixelvertex.palatelog.core.designsystem

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mappls.sdk.maps.MapView
import com.mappls.sdk.maps.MapplsMap
import com.mappls.sdk.maps.MapplsMapOptions
import com.mappls.sdk.maps.OnMapReadyCallback
import com.mappls.sdk.maps.annotations.IconFactory
import com.mappls.sdk.maps.annotations.Marker
import com.mappls.sdk.maps.annotations.MarkerOptions
import com.mappls.sdk.maps.camera.CameraUpdateFactory
import com.mappls.sdk.maps.geometry.LatLng
import com.mappls.sdk.maps.style.layers.Property
import com.mappls.sdk.maps.style.layers.PropertyFactory

private const val TAG = "PalateLog_Mappls"

/**
 * Mappls (MapmyIndia) vector map renderer.
 * 100% Survey of India compliant for Indian territory and boundary rendering.
 */
@Suppress("DEPRECATION")
@Composable
fun MapplsMapRenderer(
    modifier: Modifier = Modifier,
    centerLatitude: Double,
    centerLongitude: Double,
    zoomLevel: Double = 16.0,
    animationTrigger: Long = 0L,
    markers: List<PalateMapMarker> = emptyList(),
    hideGenericPois: Boolean = false,
    onSingleTap: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onLongPress: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onMarkerClick: ((markerId: String) -> Unit)? = null,
    onInitializationFailed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val marginPx = remember(density) { with(density) { 16.dp.roundToPx() } }

    var mapplsMapInstance by remember { mutableStateOf<MapplsMap?>(null) }
    val markerIdMap = remember { mutableMapOf<Marker, String>() }

    val mapView = remember {
        val options = MapplsMapOptions.createFromAttributes(context, null)
            .compassGravity(Gravity.TOP or Gravity.START)
            .compassMargins(intArrayOf(marginPx, marginPx, marginPx, marginPx))
        MapView(context, options).apply {
            onCreate(Bundle())
            post {
                compassView?.let { compass ->
                    val params = (compass.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.gravity = Gravity.TOP or Gravity.START
                    params.setMargins(marginPx, marginPx, marginPx, marginPx)
                    compass.layoutParams = params
                    compass.requestLayout()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(mapplsMap: MapplsMap) {
                mapplsMapInstance = mapplsMap
                try {
                    mapplsMap.uiSettings?.compassGravity = Gravity.TOP or Gravity.START
                    mapplsMap.uiSettings?.setCompassMargins(marginPx, marginPx, marginPx, marginPx)
                } catch (_: Exception) {}
                mapView.post {
                    mapView.compassView?.let { compass ->
                        val params = (compass.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.gravity = Gravity.TOP or Gravity.START
                        params.setMargins(marginPx, marginPx, marginPx, marginPx)
                        compass.layoutParams = params
                        compass.requestLayout()
                    }
                }
                mapplsMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(centerLatitude, centerLongitude),
                        zoomLevel
                    )
                )

                mapplsMap.addOnMapClickListener { latLng ->
                    onSingleTap?.invoke(latLng.latitude, latLng.longitude)
                    true
                }

                mapplsMap.addOnMapLongClickListener { latLng ->
                    onLongPress?.invoke(latLng.latitude, latLng.longitude)
                    true
                }

                mapplsMap.setOnMarkerClickListener { marker ->
                    val clickedId = markerIdMap[marker] ?: marker.title
                    if (clickedId != null) {
                        onMarkerClick?.invoke(clickedId)
                    }
                    true
                }

                applyPoiLayerVisibility(mapplsMap, hideGenericPois)
            }

            override fun onMapError(code: Int, message: String?) {
                Log.e(TAG, "Mappls onMapError: code=$code, message=$message")
                MapProviderConfig.notifyFallbackEngaged()
                onInitializationFailed?.invoke()
            }
        })
    }

    // Toggle generic POI visibility when hideGenericPois changes
    LaunchedEffect(mapplsMapInstance, hideGenericPois) {
        val map = mapplsMapInstance ?: return@LaunchedEffect
        applyPoiLayerVisibility(map, hideGenericPois)
    }

    // Animate camera smoothly when center coordinates, zoom level, or animationTrigger change
    LaunchedEffect(mapplsMapInstance, centerLatitude, centerLongitude, zoomLevel, animationTrigger) {
        mapplsMapInstance?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(centerLatitude, centerLongitude),
                zoomLevel
            ),
            DEFAULT_MAP_CAMERA_ANIMATION_MS
        )
    }

    // Update markers when list changes
    LaunchedEffect(mapplsMapInstance, markers) {
        val map = mapplsMapInstance ?: return@LaunchedEffect
        map.clear()
        markerIdMap.clear()
        val iconFactory = IconFactory.getInstance(context)

        markers.forEach { markerData ->
            val options = MarkerOptions()
                .position(LatLng(markerData.latitude, markerData.longitude))
                .title(markerData.title)
                .snippet(markerData.snippet)

            markerData.customIcon?.let { iconDrawable ->
                try {
                    val bitmap = iconDrawable.toBitmap()
                    options.icon(iconFactory.fromBitmap(bitmap))
                } catch (_: Exception) {
                    // Fall back to default marker if bitmap conversion fails
                }
            }

            val addedMarker = map.addMarker(options)
            if (addedMarker != null) {
                markerIdMap[addedMarker] = markerData.id
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

private fun applyPoiLayerVisibility(map: MapplsMap, hidePois: Boolean) {
    try {
        map.getStyle { style ->
            for (layer in style.layers) {
                val layerId = layer.id.lowercase()
                if (layerId.contains("poi")) {
                    layer.setProperties(
                        PropertyFactory.visibility(
                            if (hidePois) Property.NONE else Property.VISIBLE
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to apply POI layer visibility on Mappls style", e)
    }
}
