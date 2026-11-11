package com.prismpixelvertex.palatelog.core.designsystem

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.prismpixelvertex.palatelog.R
import java.util.Locale

@Composable
fun LocationPickerDialog(
    initialLatitude: Double,
    initialLongitude: Double,
    userGpsLatitude: Double? = null,
    userGpsLongitude: Double? = null,
    onDismiss: () -> Unit,
    onLocationConfirmed: (latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    val connectivityManager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val isNetworkAvailable = remember(context) {
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // Default fallback coordinates if invalid (Bangalore default or given initial)
    val startLat = if (initialLatitude != 0.0) initialLatitude else 12.9716
    val startLng = if (initialLongitude != 0.0) initialLongitude else 77.5946

    var selectedLat by remember { mutableDoubleStateOf(startLat) }
    var selectedLng by remember { mutableDoubleStateOf(startLng) }
    var centerLat by remember { mutableDoubleStateOf(startLat) }
    var centerLng by remember { mutableDoubleStateOf(startLng) }
    var animationTrigger by remember { mutableLongStateOf(0L) }

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

                Spacer(modifier = Modifier.height(12.dp))

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
                        markers = listOf(
                            PalateMapMarker(
                                id = "SELECTED_PIN",
                                latitude = selectedLat,
                                longitude = selectedLng,
                                title = stringResource(R.string.restaurant_detail_selected_spot_default),
                                snippet = stringResource(R.string.map_tap_actions_sub),
                                customIcon = MapMarkerFactory.getCustomLocationMarker(context)
                            )
                        ),
                        onSingleTap = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                        },
                        onLongPress = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                        }
                    )

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
                                        animationTrigger = System.currentTimeMillis()
                                    } else if (userGpsLatitude != null && userGpsLongitude != null && userGpsLatitude != 0.0) {
                                        centerLat = userGpsLatitude
                                        centerLng = userGpsLongitude
                                        selectedLat = userGpsLatitude
                                        selectedLng = userGpsLongitude
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
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.location_picker_my_location), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Coordinates readout & Offline Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.restaurant_detail_coords_fmt, String.format(Locale.US, "%.5f", selectedLat), String.format(Locale.US, "%.5f", selectedLng)),
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

                Spacer(modifier = Modifier.height(12.dp))

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
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.RadiusMedium),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Text(stringResource(R.string.location_picker_confirm), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
