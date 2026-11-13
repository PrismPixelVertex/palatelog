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
import androidx.compose.runtime.rememberUpdatedState
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
import com.mappls.sdk.gestures.MoveGestureDetector
import com.mappls.sdk.gestures.StandardScaleGestureDetector
import com.mappls.sdk.maps.MapView
import com.mappls.sdk.maps.MapplsMap
import com.mappls.sdk.maps.MapplsMapOptions
import com.mappls.sdk.maps.OnMapReadyCallback
import com.mappls.sdk.maps.annotations.IconFactory
import com.mappls.sdk.maps.annotations.Marker
import com.mappls.sdk.maps.annotations.MarkerOptions
import com.mappls.sdk.maps.camera.CameraUpdateFactory
import com.mappls.sdk.maps.geometry.LatLng

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
    onSingleTap: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onLongPress: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onMarkerClick: ((markerId: String) -> Unit)? = null,
    onCameraIdle: ((latitude: Double, longitude: Double, zoom: Double) -> Unit)? = null,
    onInitializationFailed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val compassMarginTopPx = remember(density) { with(density) { 68.dp.roundToPx() } }
    val compassMarginEndPx = remember(density) { with(density) { 12.dp.roundToPx() } }

    val currentOnSingleTap by rememberUpdatedState(onSingleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)

    var mapplsMapInstance by remember { mutableStateOf<MapplsMap?>(null) }
    val markerIdMap = remember { mutableMapOf<Marker, String>() }

    val mapView = remember {
        val options = MapplsMapOptions.createFromAttributes(context, null)
            .compassGravity(Gravity.TOP or Gravity.END)
            .compassMargins(intArrayOf(0, compassMarginTopPx, compassMarginEndPx, 0))
        MapView(context, options).apply {
            onCreate(Bundle())
            post {
                compassView?.let { compass ->
                    val params = (compass.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.gravity = Gravity.TOP or Gravity.END
                    params.setMargins(0, compassMarginTopPx, compassMarginEndPx, 0)
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
                    mapplsMap.uiSettings?.compassGravity = Gravity.TOP or Gravity.END
                    mapplsMap.uiSettings?.setCompassMargins(0, compassMarginTopPx, compassMarginEndPx, 0)
                } catch (_: Exception) {}
                mapView.post {
                    mapView.compassView?.let { compass ->
                        val params = (compass.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.gravity = Gravity.TOP or Gravity.END
                        params.setMargins(0, compassMarginTopPx, compassMarginEndPx, 0)
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

                val notifyCameraChanged = {
                    val target = mapplsMap.cameraPosition.target
                    val currentZoom = mapplsMap.cameraPosition.zoom
                    if (target != null) {
                        Log.d(TAG, "Camera moved/idle: lat=${target.latitude}, lng=${target.longitude}, zoom=$currentZoom")
                        currentOnCameraIdle?.invoke(target.latitude, target.longitude, currentZoom)
                    }
                }

                mapplsMap.addOnCameraIdleListener {
                    notifyCameraChanged()
                }

                mapplsMap.addOnMoveListener(object : MapplsMap.OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {}
                    override fun onMove(detector: MoveGestureDetector) {}
                    override fun onMoveEnd(detector: MoveGestureDetector) {
                        notifyCameraChanged()
                    }
                })

                mapplsMap.addOnScaleListener(object : MapplsMap.OnScaleListener {
                    override fun onScaleBegin(detector: StandardScaleGestureDetector) {}
                    override fun onScale(detector: StandardScaleGestureDetector) {}
                    override fun onScaleEnd(detector: StandardScaleGestureDetector) {
                        notifyCameraChanged()
                    }
                })

                mapplsMap.addOnMapClickListener { latLng ->
                    currentOnSingleTap?.invoke(latLng.latitude, latLng.longitude)
                    true
                }

                mapplsMap.addOnMapLongClickListener { latLng ->
                    currentOnLongPress?.invoke(latLng.latitude, latLng.longitude)
                    true
                }

                mapplsMap.setOnMarkerClickListener { marker ->
                    val clickedId = markerIdMap[marker] ?: marker.title
                    if (clickedId != null) {
                        currentOnMarkerClick?.invoke(clickedId)
                    }
                    true
                }
            }

            override fun onMapError(code: Int, message: String?) {
                Log.e(TAG, "Mappls onMapError: code=$code, message=$message")
                MapProviderConfig.notifyFallbackEngaged()
                onInitializationFailed?.invoke()
            }
        })
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
                    val bitmap = (iconDrawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: iconDrawable.toBitmap()
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
