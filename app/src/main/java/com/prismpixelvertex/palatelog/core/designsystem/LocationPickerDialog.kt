package com.prismpixelvertex.palatelog.core.designsystem

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.location.MapplsPoiSearchHelper
import com.prismpixelvertex.palatelog.core.location.RestaurantPoi
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationPickerEntryPoint {
    fun restaurantDao(): RestaurantDao
}

enum class SelectedSpotType {
    CUSTOM,
    LOGGED_RESTAURANT,
    MAPPLS_POI
}

private const val TAG = "LocationPickerDialog"

@Composable
fun LocationPickerDialog(
    initialLatitude: Double,
    initialLongitude: Double,
    userGpsLatitude: Double? = null,
    userGpsLongitude: Double? = null,
    onDismiss: () -> Unit,
    onLocationConfirmed: (latitude: Double, longitude: Double) -> Unit,
    onLocationConfirmedWithPlace: ((latitude: Double, longitude: Double, placeName: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    val connectivityManager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val isNetworkAvailable = remember(context) {
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val restaurantDao = remember(context) {
        try {
            EntryPointAccessors.fromApplication(context.applicationContext, LocationPickerEntryPoint::class.java).restaurantDao()
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve RestaurantDao from application context", e)
            null
        }
    }

    // Default fallback coordinates if invalid (Bangalore default or given initial)
    val startLat = if (initialLatitude != 0.0) initialLatitude else 12.9716
    val startLng = if (initialLongitude != 0.0) initialLongitude else 77.5946

    var selectedLat by remember { mutableDoubleStateOf(startLat) }
    var selectedLng by remember { mutableDoubleStateOf(startLng) }
    var centerLat by remember { mutableDoubleStateOf(startLat) }
    var centerLng by remember { mutableDoubleStateOf(startLng) }
    var animationTrigger by remember { mutableLongStateOf(0L) }

    // POI & Saved Restaurant Layer Controls
    var showPois by remember { mutableStateOf(true) }
    var showLogged by remember { mutableStateOf(true) }
    var isSearchingPois by remember { mutableStateOf(false) }
    var mapplsPois by remember { mutableStateOf<List<RestaurantPoi>>(emptyList()) }
    var lastSearchedCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Selected spot details for display banner and confirmation
    var selectedSpotName by remember { mutableStateOf<String?>(null) }
    var selectedSpotSnippet by remember { mutableStateOf<String?>(null) }
    var selectedSpotType by remember { mutableStateOf(SelectedSpotType.CUSTOM) }

    // Fetch user-logged restaurants from local Room database
    val localRestaurants by produceState<List<RestaurantEntity>>(initialValue = emptyList(), restaurantDao) {
        try {
            restaurantDao?.getAllRestaurants()?.collect { entities ->
                value = entities.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error collecting local restaurants", e)
        }
    }

    // Initial search for Mappls restaurant POIs around initial coordinates
    LaunchedEffect(startLat, startLng, isNetworkAvailable) {
        if (isNetworkAvailable && MapProviderConfig.isMapplsInitialized) {
            isSearchingPois = true
            mapplsPois = MapplsPoiSearchHelper.searchNearbyRestaurants(startLat, startLng, radiusMeters = 1500)
            lastSearchedCoords = Pair(startLat, startLng)
            isSearchingPois = false
        }
    }

    val defaultSpotTitle = stringResource(R.string.restaurant_detail_selected_spot_default)
    val defaultSpotSnippet = stringResource(R.string.map_tap_actions_sub)

    // Build consolidated marker list (Saved + Mappls POIs + Selected Pin)
    val markers = remember(
        selectedLat, selectedLng, selectedSpotName, selectedSpotSnippet,
        showPois, mapplsPois, showLogged, localRestaurants, centerLat, centerLng,
        defaultSpotTitle, defaultSpotSnippet
    ) {
        buildList {
            // 1. User-logged restaurants from local DB (colored by verdict: Green, Red, Amber)
            if (showLogged) {
                val nearbySaved = localRestaurants.filter { rest ->
                    GeoUtils.calculateDistanceKm(centerLat, centerLng, rest.latitude, rest.longitude) <= 15.0
                }
                nearbySaved.forEach { rest ->
                    val verdict = try { Verdict.valueOf(rest.overallVerdict) } catch (_: Exception) { Verdict.GO }
                    val type = try { RestaurantType.valueOf(rest.restaurantType) } catch (_: Exception) { RestaurantType.DINE_IN }
                    add(
                        PalateMapMarker(
                            id = "USER_${rest.id}",
                            latitude = rest.latitude,
                            longitude = rest.longitude,
                            title = rest.name,
                            snippet = "${verdict.name} • ${rest.cuisine.ifBlank { rest.restaurantType }}",
                            customIcon = MapMarkerFactory.getRestaurantMarker(context, verdict, type)
                        )
                    )
                }
            }

            // 2. Mappls Food POIs (distinct Sky Blue pin)
            if (showPois) {
                val poiIcon = MapMarkerFactory.getPoiRestaurantMarker(context)
                mapplsPois.forEach { poi ->
                    add(
                        PalateMapMarker(
                            id = "POI_${poi.id}",
                            latitude = poi.latitude,
                            longitude = poi.longitude,
                            title = poi.name,
                            snippet = poi.address,
                            customIcon = poiIcon
                        )
                    )
                }
            }

            // 3. User's currently selected pin (Gold/Amber custom location marker)
            add(
                PalateMapMarker(
                    id = "SELECTED_PIN",
                    latitude = selectedLat,
                    longitude = selectedLng,
                    title = selectedSpotName ?: defaultSpotTitle,
                    snippet = selectedSpotSnippet ?: defaultSpotSnippet,
                    customIcon = MapMarkerFactory.getCustomLocationMarker(context)
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(Dimens.RadiusDialog),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationHigh),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(Dimens.LocationDialogHeight)
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.SpacingDefault)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(Dimens.IconSizeDefault)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                        Column {
                            Text(
                                text = stringResource(R.string.location_picker_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.location_picker_instructions),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Map View Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    PalateInteractiveMap(
                        modifier = Modifier.fillMaxSize(),
                        centerLatitude = centerLat,
                        centerLongitude = centerLng,
                        zoomLevel = 16.0,
                        animationTrigger = animationTrigger,
                        markers = markers,
                        hideGenericPois = true,
                        onSingleTap = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                            selectedSpotName = null
                            selectedSpotSnippet = null
                            selectedSpotType = SelectedSpotType.CUSTOM
                        },
                        onLongPress = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                            selectedSpotName = null
                            selectedSpotSnippet = null
                            selectedSpotType = SelectedSpotType.CUSTOM
                        },
                        onMarkerClick = { markerId ->
                            when {
                                markerId.startsWith("USER_") -> {
                                    val restId = markerId.removePrefix("USER_")
                                    val match = localRestaurants.find { it.id == restId }
                                    if (match != null) {
                                        selectedLat = match.latitude
                                        selectedLng = match.longitude
                                        selectedSpotName = match.name
                                        selectedSpotSnippet = "${match.overallVerdict} • ${match.cuisine.ifBlank { match.restaurantType }}"
                                        selectedSpotType = SelectedSpotType.LOGGED_RESTAURANT
                                    }
                                }
                                markerId.startsWith("POI_") -> {
                                    val poiId = markerId.removePrefix("POI_")
                                    val match = mapplsPois.find { it.id == poiId }
                                    if (match != null) {
                                        selectedLat = match.latitude
                                        selectedLng = match.longitude
                                        selectedSpotName = match.name
                                        selectedSpotSnippet = match.address
                                        selectedSpotType = SelectedSpotType.MAPPLS_POI
                                    }
                                }
                                markerId == "SELECTED_PIN" -> {
                                    // Already focused on the selected pin
                                }
                            }
                        }
                    )

                    // Floating Filter Chips at Top-Start (Food POIs toggle & Saved Restaurants toggle)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showPois,
                            onClick = {
                                val nextState = !showPois
                                showPois = nextState
                                if (nextState && mapplsPois.isEmpty() && isNetworkAvailable) {
                                    coroutineScope.launch {
                                        isSearchingPois = true
                                        mapplsPois = MapplsPoiSearchHelper.searchNearbyRestaurants(centerLat, centerLng, 1500)
                                        lastSearchedCoords = Pair(centerLat, centerLng)
                                        isSearchingPois = false
                                    }
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.location_picker_filter_food_pois),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    if (isSearchingPois) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        if (localRestaurants.isNotEmpty()) {
                            FilterChip(
                                selected = showLogged,
                                onClick = { showLogged = !showLogged },
                                label = {
                                    Text(
                                        text = stringResource(R.string.location_picker_filter_logged),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }

                    // Floating "Search this area" prompt when camera moves away from last search
                    val needsSearchHere = remember(centerLat, centerLng, lastSearchedCoords, showPois) {
                        showPois && lastSearchedCoords?.let {
                            GeoUtils.calculateDistanceKm(it.first, it.second, centerLat, centerLng) > 0.8
                        } ?: false
                    }

                    if (needsSearchHere && !isSearchingPois && isNetworkAvailable) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        isSearchingPois = true
                                        mapplsPois = MapplsPoiSearchHelper.searchNearbyRestaurants(centerLat, centerLng, 1500)
                                        lastSearchedCoords = Pair(centerLat, centerLng)
                                        isSearchingPois = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.location_picker_search_area),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Re-center on user's current GPS location button
                    FloatingActionButton(
                        onClick = {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        centerLat = loc.latitude
                                        centerLng = loc.longitude
                                        selectedLat = loc.latitude
                                        selectedLng = loc.longitude
                                        selectedSpotName = null
                                        selectedSpotSnippet = null
                                        selectedSpotType = SelectedSpotType.CUSTOM
                                        animationTrigger = System.currentTimeMillis()
                                    } else if (userGpsLatitude != null && userGpsLongitude != null && userGpsLatitude != 0.0) {
                                        centerLat = userGpsLatitude
                                        centerLng = userGpsLongitude
                                        selectedLat = userGpsLatitude
                                        selectedLng = userGpsLongitude
                                        selectedSpotName = null
                                        selectedSpotSnippet = null
                                        selectedSpotType = SelectedSpotType.CUSTOM
                                        animationTrigger = System.currentTimeMillis()
                                    } else {
                                        centerLat = selectedLat
                                        centerLng = selectedLng
                                        animationTrigger = System.currentTimeMillis()
                                    }
                                }
                            } catch (_: SecurityException) {
                                if (userGpsLatitude != null && userGpsLongitude != null && userGpsLatitude != 0.0) {
                                    centerLat = userGpsLatitude
                                    centerLng = userGpsLongitude
                                    selectedLat = userGpsLatitude
                                    selectedLng = userGpsLongitude
                                    selectedSpotName = null
                                    selectedSpotSnippet = null
                                    selectedSpotType = SelectedSpotType.CUSTOM
                                    animationTrigger = System.currentTimeMillis()
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = stringResource(R.string.location_picker_my_location),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selected spot info card (if a POI or Logged restaurant was tapped)
                if (selectedSpotName != null) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedSpotType == SelectedSpotType.MAPPLS_POI) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedSpotType == SelectedSpotType.MAPPLS_POI) {
                                    Icons.Default.Restaurant
                                } else {
                                    Icons.Default.Bookmark
                                },
                                contentDescription = null,
                                tint = if (selectedSpotType == SelectedSpotType.MAPPLS_POI) AmberPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSpotName ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!selectedSpotSnippet.isNullOrBlank()) {
                                    Text(
                                        text = selectedSpotSnippet ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedSpotType == SelectedSpotType.MAPPLS_POI) {
                                    stringResource(R.string.location_picker_selected_poi_badge)
                                } else {
                                    stringResource(R.string.location_picker_selected_logged_badge)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Coordinates readout & Offline Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.restaurant_detail_coords_fmt,
                            String.format(Locale.US, "%.5f", selectedLat),
                            String.format(Locale.US, "%.5f", selectedLng)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isNetworkAvailable) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.location_picker_offline_notice),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.RadiusMedium)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            onLocationConfirmed(selectedLat, selectedLng)
                            onLocationConfirmedWithPlace?.invoke(selectedLat, selectedLng, selectedSpotName)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.RadiusMedium),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Text(
                            stringResource(R.string.location_picker_confirm),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
