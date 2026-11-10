package com.prismpixelvertex.palatelog.feature.map

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.MapMarkerFactory
import com.prismpixelvertex.palatelog.core.designsystem.VerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.navigation.ExternalMapNavigator
import com.prismpixelvertex.palatelog.core.designsystem.PalateInteractiveMap
import com.prismpixelvertex.palatelog.core.designsystem.PalateMapMarker
import com.prismpixelvertex.palatelog.core.designsystem.rememberPalateMapState

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToRestaurantDetail: (String) -> Unit,
    onNavigateToQuickCapture: (lat: Double, lng: Double, address: String, city: String) -> Unit,
    onNavigateToEntryEditor: (restaurantId: String?, lat: Double?, lng: Double?, address: String?, city: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLocationBannerDismissed by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val mapState = rememberPalateMapState(
        latitude = uiState.userLatitude,
        longitude = uiState.userLongitude,
        zoom = 13.0
    )

    LaunchedEffect(uiState.userLatitude, uiState.userLongitude) {
        if (uiState.isLocationReady) {
            mapState.animateCamera(uiState.userLatitude, uiState.userLongitude, mapState.zoom)
        }
    }

    val markers = remember(uiState.isLocationReady, uiState.userLatitude, uiState.userLongitude, uiState.selectedCustomLocation, uiState.restaurants) {
        val list = mutableListOf<PalateMapMarker>()

        if (uiState.isLocationReady) {
            list.add(
                PalateMapMarker(
                    id = "USER_LOCATION",
                    latitude = uiState.userLatitude,
                    longitude = uiState.userLongitude,
                    title = context.getString(R.string.map_your_location),
                    customIcon = MapMarkerFactory.getUserLocationMarker(context)
                )
            )
        }

        uiState.selectedCustomLocation?.let { customLoc ->
            list.add(
                PalateMapMarker(
                    id = "CUSTOM_LOCATION",
                    latitude = customLoc.latitude,
                    longitude = customLoc.longitude,
                    title = if (customLoc.street.isNotBlank()) customLoc.street else context.getString(R.string.map_selected_spot_default),
                    snippet = context.getString(R.string.map_tap_actions_sub),
                    customIcon = MapMarkerFactory.getCustomLocationMarker(context)
                )
            )
        }

        uiState.restaurants.forEach { r ->
            list.add(
                PalateMapMarker(
                    id = r.id,
                    latitude = r.latitude,
                    longitude = r.longitude,
                    title = r.name,
                    snippet = "${r.overallVerdict.name} • ${r.cuisine}",
                    customIcon = MapMarkerFactory.getRestaurantMarker(context, r.overallVerdict, r.restaurantType)
                )
            )
        }

        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        PalateInteractiveMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            markers = markers,
            onSingleTap = { lat, lng ->
                viewModel.onMapTapped(lat, lng)
            },
            onLongPress = { lat, lng ->
                viewModel.onMapTapped(lat, lng)
            },
            onMarkerClick = { markerId ->
                val clickedRestaurant = uiState.restaurants.firstOrNull { it.id == markerId }
                if (clickedRestaurant != null) {
                    viewModel.onMarkerSelected(clickedRestaurant)
                }
            }
        )

        // Top Warning Pill when Location is Turned Off
        AnimatedVisibility(
            visible = uiState.isLocationDisabled && !isLocationBannerDismissed,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 72.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.map_location_disabled_banner),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.map_location_disabled_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { isLocationBannerDismissed = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Center on Me FAB
        FloatingActionButton(
            onClick = {
                viewModel.loadLocation()
                if (uiState.isLocationDisabled) {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.map_location_disabled_snackbar),
                            actionLabel = context.getString(R.string.map_location_disabled_action)
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                    }
                } else {
                    mapState.animateCamera(uiState.userLatitude, uiState.userLongitude, 13.0)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                if (uiState.isLocationDisabled) Icons.Default.LocationOff else Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.location_picker_my_location)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Bottom Card when a custom location on map is tapped
        AnimatedVisibility(
            visible = uiState.selectedCustomLocation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            uiState.selectedCustomLocation?.let { customLoc ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (customLoc.isLoadingAddress) stringResource(R.string.map_acquiring_address)
                                    else listOfNotNull(customLoc.street.ifBlank { null }, customLoc.city.ifBlank { null })
                                        .joinToString(", ").ifBlank { stringResource(R.string.map_selected_spot_default) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearCustomLocation() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.map_coords_fmt, String.format(java.util.Locale.US, "%.4f", customLoc.latitude), String.format(java.util.Locale.US, "%.4f", customLoc.longitude)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onNavigateToQuickCapture(
                                        customLoc.latitude,
                                        customLoc.longitude,
                                        customLoc.street,
                                        customLoc.city
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdictNoGoRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.dashboard_fab_quick_log))
                            }

                            Button(
                                onClick = {
                                    onNavigateToEntryEditor(
                                        null,
                                        customLoc.latitude,
                                        customLoc.longitude,
                                        customLoc.street,
                                        customLoc.city
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.dashboard_fab_full_visit))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Preview Card when a restaurant marker is tapped
        AnimatedVisibility(
            visible = uiState.selectedRestaurant != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            uiState.selectedRestaurant?.let { restaurant ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = restaurant.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onMarkerSelected(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerdictBadge(verdict = restaurant.overallVerdict)
                            if (uiState.selectedDistanceKm != null) {
                                Text(
                                    text = "• ${GeoUtils.formatDistance(uiState.selectedDistanceKm!!)} away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (restaurant.highwayOrRoute.isNotBlank() || restaurant.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (restaurant.highwayOrRoute.isNotBlank()) "🛣️ ${restaurant.highwayOrRoute}" else restaurant.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onNavigateToEntryEditor(
                                        restaurant.id,
                                        restaurant.latitude,
                                        restaurant.longitude,
                                        restaurant.address,
                                        restaurant.city
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.detail_action_new_visit),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    ExternalMapNavigator.navigateTo(
                                        context = context,
                                        latitude = restaurant.latitude,
                                        longitude = restaurant.longitude,
                                        label = restaurant.name
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.detail_action_navigate),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { onNavigateToRestaurantDetail(restaurant.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.map_preview_view_details),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
