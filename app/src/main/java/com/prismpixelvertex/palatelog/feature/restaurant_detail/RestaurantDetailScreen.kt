package com.prismpixelvertex.palatelog.feature.restaurant_detail

import com.prismpixelvertex.palatelog.core.model.photoUris
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.ChannelBadge
import com.prismpixelvertex.palatelog.core.designsystem.DishVerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.FamilyReactionTag
import com.prismpixelvertex.palatelog.core.designsystem.VerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmber
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmberContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictChoiceChip
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreenContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRedContainer
import com.prismpixelvertex.palatelog.core.model.Channel
import com.prismpixelvertex.palatelog.core.model.DishFeedback
import com.prismpixelvertex.palatelog.core.model.Restaurant
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.core.designsystem.MapMarkerFactory
import com.prismpixelvertex.palatelog.core.designsystem.PalateInteractiveMap
import com.prismpixelvertex.palatelog.core.designsystem.PalateMapMarker
import com.prismpixelvertex.palatelog.core.model.VisitEntry
import com.prismpixelvertex.palatelog.core.navigation.ExternalMapNavigator
import com.prismpixelvertex.palatelog.feature.help.HelpTopicKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogVisit: (restaurantId: String) -> Unit,
    onNavigateToHelpGuide: (HelpTopicKey?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val restaurant = uiState.restaurant

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isScrollIndicatorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState, uiState.visits.size, uiState.isLoading) {
        snapshotFlow { scrollState.value }
            .collectLatest {
                if ((scrollState.canScrollForward) && (scrollState.maxValue > 0)) {
                    isScrollIndicatorVisible = true
                    delay(2500.milliseconds)
                    isScrollIndicatorVisible = false
                } else {
                    isScrollIndicatorVisible = false
                }
            }
    }

    var showEditRestaurantDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var visitToEdit by remember { mutableStateOf<VisitEntry?>(null) }
    var visitToDelete by remember { mutableStateOf<VisitEntry?>(null) }
    var dishToDelete by remember { mutableStateOf<DishFeedback?>(null) }
    var selectedPhotoForPreview by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurant?.name ?: stringResource(R.string.dashboard_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    if (restaurant != null) {
                        IconButton(onClick = { showEditRestaurantDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { onNavigateToHelpGuide(HelpTopicKey.DISH_MATRIX) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.help_title),
                            tint = AmberPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading || restaurant == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmberPrimary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            VerdictBadge(verdict = restaurant.overallVerdict)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val defaultGeneralDining = stringResource(R.string.restaurant_detail_general_dining)
                        Text(
                            text = "${restaurant.restaurantType.displayName} • ${restaurant.cuisine.ifBlank { defaultGeneralDining }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (restaurant.highwayOrRoute.isNotBlank() || restaurant.address.isNotBlank() || restaurant.city.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val fullLoc = listOfNotNull(
                                if (restaurant.highwayOrRoute.isNotBlank()) "🛣️ ${restaurant.highwayOrRoute}" else null,
                                restaurant.address.ifBlank { null },
                                restaurant.city.ifBlank { null }
                            ).joinToString(" • ")
                            Text(
                                text = fullLoc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (restaurant.latitude != 0.0 || restaurant.longitude != 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📍 ${String.format(Locale.US, "%.4f", restaurant.latitude)}, ${String.format(Locale.US, "%.4f", restaurant.longitude)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    ExternalMapNavigator.navigateTo(
                                        context = context,
                                        latitude = restaurant.latitude,
                                        longitude = restaurant.longitude,
                                        label = restaurant.name
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.detail_action_navigate))
                            }

                            OutlinedButton(
                                onClick = { onNavigateToLogVisit(restaurant.id) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.detail_action_new_visit))
                            }
                        }
                    }
                }

                // Section: Dish Insights Matrix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.restaurant_detail_dish_matrix_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onNavigateToHelpGuide(HelpTopicKey.DISH_MATRIX) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.restaurant_detail_dish_matrix_help_desc),
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (uiState.mustTryDishes.isEmpty() && uiState.avoidDishes.isEmpty() && uiState.neutralDishes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.restaurant_detail_no_dishes_logged),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 🔴 AVOID DISHES (Shown first to prevent repeats!)
                    if (uiState.avoidDishes.isNotEmpty()) {
                        DishVerdictGroup(
                            title = stringResource(R.string.restaurant_detail_dishes_avoid),
                            dishes = uiState.avoidDishes,
                            accentColor = VerdictNoGoRed,
                            onPhotoClick = { selectedPhotoForPreview = it },
                            onDeleteDish = { dishToDelete = it }
                        )
                    }

                    // 🟢 MUST TRY DISHES
                    if (uiState.mustTryDishes.isNotEmpty()) {
                        DishVerdictGroup(
                            title = stringResource(R.string.restaurant_detail_dishes_must_try),
                            dishes = uiState.mustTryDishes,
                            accentColor = VerdictGoGreen,
                            onPhotoClick = { selectedPhotoForPreview = it },
                            onDeleteDish = { dishToDelete = it }
                        )
                    }

                    // 🟡 MEH / OK DISHES
                    if (uiState.neutralDishes.isNotEmpty()) {
                        DishVerdictGroup(
                            title = stringResource(R.string.restaurant_detail_dishes_neutral),
                            dishes = uiState.neutralDishes,
                            accentColor = VerdictCautionAmber,
                            onPhotoClick = { selectedPhotoForPreview = it },
                            onDeleteDish = { dishToDelete = it }
                        )
                    }
                }

                // Section: Past Visits History
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.restaurant_detail_visit_history_title, uiState.visits.size),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.visits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.restaurant_detail_no_visits_logged),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.visits.forEach { visit ->
                        VisitHistoryCard(
                            visit = visit,
                            onEditVisit = { visitToEdit = visit },
                            onDeleteVisit = { visitToDelete = visit },
                            onPhotoClick = { selectedPhotoForPreview = it }
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isScrollIndicatorVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollBy(600f)
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    tonalElevation = Dimens.ElevationMedium,
                    shadowElevation = Dimens.ElevationMedium,
                    border = BorderStroke(Dimens.BorderThin, AmberPrimary.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier.padding(Dimens.SpacingSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.help_scroll_more_cards),
                            tint = AmberPrimary,
                            modifier = Modifier.size(Dimens.IconSizeDefault)
                        )
                    }
                }
            }
        }
        }
    }

    selectedPhotoForPreview?.let { photoUri ->
        FullscreenPhotoDialog(
            photoUri = photoUri,
            onDismiss = { selectedPhotoForPreview = null }
        )
    }

    // Dialog: Edit Restaurant Details
    if (showEditRestaurantDialog && restaurant != null) {
        EditRestaurantDialog(
            restaurant = restaurant,
            viewModel = viewModel,
            onDismiss = { showEditRestaurantDialog = false },
            onFetchCurrentLocation = { onResult ->
                viewModel.getCurrentLocation(onResult)
            },
            onSave = { name, address, city, route, lat, lng, cuisine, type, verdict ->
                viewModel.updateRestaurant(
                    name = name,
                    address = address,
                    city = city,
                    highwayOrRoute = route,
                    latitude = lat,
                    longitude = lng,
                    cuisine = cuisine,
                    restaurantType = type,
                    overallVerdict = verdict
                )
                Toast.makeText(context, context.getString(R.string.restaurant_detail_updated_toast), Toast.LENGTH_SHORT).show()
                showEditRestaurantDialog = false
            }
        )
    }

    // Dialog: Delete Restaurant Confirmation
    if (showDeleteConfirmDialog && restaurant != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.detail_delete_dialog_title)) },
            text = { Text(stringResource(R.string.detail_delete_dialog_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRestaurant()
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialog: Edit Visit
    visitToEdit?.let { visit ->
        EditVisitDialog(
            visit = visit,
            onDismiss = { visitToEdit = null },
            onSave = { channel, billAmount, verdict, notes ->
                viewModel.updateVisit(
                    visitId = visit.id,
                    channel = channel,
                    billAmount = billAmount,
                    visitVerdict = verdict,
                    generalNotes = notes
                )
                Toast.makeText(context, context.getString(R.string.restaurant_detail_visit_updated_toast), Toast.LENGTH_SHORT).show()
                visitToEdit = null
            }
        )
    }

    // Dialog: Delete Visit Confirmation
    visitToDelete?.let { visit ->
        val linkedDishCount = uiState.allDishes.count { it.visitId == visit.id }
        AlertDialog(
            onDismissRequest = { visitToDelete = null },
            title = { Text(stringResource(R.string.restaurant_detail_delete_visit_dialog_title)) },
            text = {
                Text(
                    if (linkedDishCount > 0) {
                        stringResource(R.string.restaurant_detail_delete_visit_dialog_msg_with_dishes, linkedDishCount)
                    } else {
                        stringResource(R.string.restaurant_detail_delete_visit_dialog_msg)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVisit(visit.id)
                        Toast.makeText(context, context.getString(R.string.restaurant_detail_visit_deleted_toast), Toast.LENGTH_SHORT).show()
                        visitToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { visitToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialog: Delete Dish Confirmation
    dishToDelete?.let { dish ->
        AlertDialog(
            onDismissRequest = { dishToDelete = null },
            title = { Text(stringResource(R.string.restaurant_detail_delete_dish_dialog_title)) },
            text = { Text(stringResource(R.string.restaurant_detail_delete_dish_dialog_msg, dish.dishName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDish(dish.id)
                        Toast.makeText(context, context.getString(R.string.restaurant_detail_dish_deleted_toast), Toast.LENGTH_SHORT).show()
                        dishToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { dishToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun EditRestaurantDialog(
    restaurant: Restaurant,
    viewModel: RestaurantDetailViewModel,
    onDismiss: () -> Unit,
    onFetchCurrentLocation: (onResult: (com.prismpixelvertex.palatelog.core.location.LocationResult?) -> Unit) -> Unit,
    onSave: (
        name: String,
        address: String,
        city: String,
        highwayOrRoute: String,
        latitude: Double,
        longitude: Double,
        cuisine: String,
        restaurantType: RestaurantType,
        overallVerdict: Verdict
    ) -> Unit
) {
    var name by remember { mutableStateOf(restaurant.name) }
    var address by remember { mutableStateOf(restaurant.address) }
    var city by remember { mutableStateOf(restaurant.city) }
    var highwayOrRoute by remember { mutableStateOf(restaurant.highwayOrRoute) }
    var latText by remember { mutableStateOf(restaurant.latitude.toString()) }
    var lngText by remember { mutableStateOf(restaurant.longitude.toString()) }
    var cuisine by remember { mutableStateOf(restaurant.cuisine) }
    var selectedType by remember { mutableStateOf(restaurant.restaurantType) }
    var selectedVerdict by remember { mutableStateOf(restaurant.overallVerdict) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restaurant_detail_edit_restaurant_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.entry_editor_field_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(stringResource(R.string.restaurant_detail_area_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(stringResource(R.string.restaurant_detail_city_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = highwayOrRoute,
                    onValueChange = { highwayOrRoute = it },
                    label = { Text(stringResource(R.string.restaurant_detail_highway_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.restaurant_detail_geolocation_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text(stringResource(R.string.restaurant_detail_latitude_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text(stringResource(R.string.restaurant_detail_longitude_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isFetchingGps = true
                            onFetchCurrentLocation { loc ->
                                isFetchingGps = false
                                if (loc != null) {
                                    latText = loc.latitude.toString()
                                    lngText = loc.longitude.toString()
                                    if (address.isBlank()) address = loc.street
                                    if (city.isBlank()) city = loc.city
                                }
                            }
                        },
                        enabled = !isFetchingGps,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.restaurant_detail_use_gps), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = { showMapPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.restaurant_detail_pick_on_map), style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = cuisine,
                    onValueChange = { cuisine = it },
                    label = { Text(stringResource(R.string.restaurant_detail_cuisine_label)) },
                    placeholder = { Text(stringResource(R.string.restaurant_detail_cuisine_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.restaurant_detail_restaurant_type_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RestaurantType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.restaurant_detail_overall_verdict_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerdictChoiceChip(
                        label = stringResource(R.string.restaurant_detail_verdict_go_badge),
                        isSelected = selectedVerdict == Verdict.GO,
                        color = VerdictGoGreen,
                        containerColor = VerdictGoGreenContainer,
                        onClick = { selectedVerdict = Verdict.GO },
                        modifier = Modifier.weight(1f)
                    )
                    VerdictChoiceChip(
                        label = stringResource(R.string.restaurant_detail_verdict_no_go_badge),
                        isSelected = selectedVerdict == Verdict.NO_GO,
                        color = VerdictNoGoRed,
                        containerColor = VerdictNoGoRedContainer,
                        onClick = { selectedVerdict = Verdict.NO_GO },
                        modifier = Modifier.weight(1f)
                    )
                    VerdictChoiceChip(
                        label = stringResource(R.string.restaurant_detail_verdict_caution_badge),
                        isSelected = selectedVerdict == Verdict.CAUTION,
                        color = VerdictCautionAmber,
                        containerColor = VerdictCautionAmberContainer,
                        onClick = { selectedVerdict = Verdict.CAUTION },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: restaurant.latitude
                    val lng = lngText.toDoubleOrNull() ?: restaurant.longitude
                    onSave(name, address, city, highwayOrRoute, lat, lng, cuisine, selectedType, selectedVerdict)
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.restaurant_detail_save_changes_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showMapPicker) {
        val initialLat = latText.toDoubleOrNull() ?: restaurant.latitude
        val initialLng = lngText.toDoubleOrNull() ?: restaurant.longitude
        LocationPickerDialog(
            initialLat = if (initialLat != 0.0) initialLat else 12.9716,
            initialLng = if (initialLng != 0.0) initialLng else 77.5946,
            onReverseGeocode = { lat, lng, onResult ->
                viewModel.reverseGeocode(lat, lng, onResult)
            },
            onDismiss = { showMapPicker = false },
            onConfirm = { selectedLat, selectedLng, selectedStreet, selectedCity ->
                latText = selectedLat.toString()
                lngText = selectedLng.toString()
                if (selectedStreet.isNotBlank()) address = selectedStreet
                if (selectedCity.isNotBlank()) city = selectedCity
                showMapPicker = false
            }
        )
    }
}

@Composable
private fun LocationPickerDialog(
    initialLat: Double,
    initialLng: Double,
    onReverseGeocode: (lat: Double, lng: Double, onResult: (placeName: String, street: String, city: String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (lat: Double, lng: Double, street: String, city: String) -> Unit
) {
    val context = LocalContext.current
    var selectedLat by remember { mutableDoubleStateOf(initialLat) }
    var selectedLng by remember { mutableDoubleStateOf(initialLng) }
    var selectedStreet by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }

    // Initial reverse geocode
    LaunchedEffect(initialLat, initialLng) {
        if (initialLat != 0.0 && initialLng != 0.0) {
            onReverseGeocode(initialLat, initialLng) { _, street, city ->
                selectedStreet = street
                selectedCity = city
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.restaurant_detail_tap_map_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.restaurant_detail_tap_map_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                // Map View Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val defaultSpotTitle = stringResource(R.string.restaurant_detail_selected_spot_default)
                    val customMarkerIcon = remember { MapMarkerFactory.getCustomLocationMarker(context) }

                    PalateInteractiveMap(
                        modifier = Modifier.fillMaxSize(),
                        centerLatitude = selectedLat,
                        centerLongitude = selectedLng,
                        zoomLevel = 16.0,
                        markers = listOf(
                            PalateMapMarker(
                                id = "SELECTED_SPOT",
                                latitude = selectedLat,
                                longitude = selectedLng,
                                title = selectedStreet.ifBlank { defaultSpotTitle },
                                snippet = selectedCity,
                                customIcon = customMarkerIcon
                            )
                        ),
                        onSingleTap = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                            onReverseGeocode(lat, lng) { _, street, city ->
                                selectedStreet = street
                                selectedCity = city
                            }
                        },
                        onLongPress = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                            onReverseGeocode(lat, lng) { _, street, city ->
                                selectedStreet = street
                                selectedCity = city
                            }
                        }
                    )
                }

                // Bottom Confirmation Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val locationLabel = listOfNotNull(
                        selectedStreet.ifBlank { null },
                        selectedCity.ifBlank { null }
                    ).joinToString(", ")

                    if (locationLabel.isNotBlank()) {
                        Text(
                            text = "📍 $locationLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = stringResource(R.string.restaurant_detail_coords_fmt, String.format(Locale.US, "%.5f", selectedLat), String.format(Locale.US, "%.5f", selectedLng)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }

                        Button(
                            onClick = { onConfirm(selectedLat, selectedLng, selectedStreet, selectedCity) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.location_picker_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditVisitDialog(
    visit: VisitEntry,
    onDismiss: () -> Unit,
    onSave: (channel: String, billAmount: Double, verdict: Verdict, notes: String) -> Unit
) {
    var channel by remember { mutableStateOf(visit.channel) }
    var billText by remember { mutableStateOf(if (visit.billAmount > 0) visit.billAmount.toString() else "") }
    var selectedVerdict by remember { mutableStateOf(visit.visitVerdict) }
    var notes by remember { mutableStateOf(visit.generalNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restaurant_detail_edit_visit_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.restaurant_detail_channel_header),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Channel.DEFAULT_CHANNELS.forEach { ch ->
                        FilterChip(
                            selected = channel.equals(ch, ignoreCase = true),
                            onClick = { channel = ch },
                            label = { Text(Channel.getChannelDisplayName(ch)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = billText,
                    onValueChange = { billText = it },
                    label = { Text(stringResource(R.string.restaurant_detail_bill_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.restaurant_detail_visit_verdict_header),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerdictChoiceChip(
                        label = stringResource(R.string.restaurant_detail_verdict_go_badge),
                        isSelected = selectedVerdict == Verdict.GO,
                        color = VerdictGoGreen,
                        containerColor = VerdictGoGreenContainer,
                        onClick = { selectedVerdict = Verdict.GO },
                        modifier = Modifier.weight(1f)
                    )
                    VerdictChoiceChip(
                        label = stringResource(R.string.restaurant_detail_verdict_no_go_badge),
                        isSelected = selectedVerdict == Verdict.NO_GO,
                        color = VerdictNoGoRed,
                        containerColor = VerdictNoGoRedContainer,
                        onClick = { selectedVerdict = Verdict.NO_GO },
                        modifier = Modifier.weight(1f)
                    )
                    VerdictChoiceChip(
                        label = stringResource(R.string.restaurant_detail_verdict_caution_badge),
                        isSelected = selectedVerdict == Verdict.CAUTION,
                        color = VerdictCautionAmber,
                        containerColor = VerdictCautionAmberContainer,
                        onClick = { selectedVerdict = Verdict.CAUTION },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.restaurant_detail_notes_label)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bill = billText.toDoubleOrNull() ?: 0.0
                    onSave(channel, bill, selectedVerdict, notes)
                }
            ) {
                Text(stringResource(R.string.restaurant_detail_save_visit_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun FullscreenPhotoDialog(
    photoUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Full-screen photo view",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun DishVerdictGroup(
    title: String,
    dishes: List<DishFeedback>,
    accentColor: Color,
    onPhotoClick: (String) -> Unit,
    onDeleteDish: ((DishFeedback) -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            dishes.forEach { dish ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dish.dishName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DishVerdictBadge(verdict = dish.verdict)
                            if (onDeleteDish != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteDish(dish) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete dish",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (dish.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.restaurant_detail_learning_prefix, dish.tags.joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (dish.memberPreferences.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dish.memberPreferences.forEach { (member, reaction) ->
                                FamilyReactionTag(memberName = member, reaction = reaction)
                            }
                        }
                    }

                    if (dish.photoUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dish.photoUris.forEach { photoUri ->
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Dish photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onPhotoClick(photoUri) }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun VisitHistoryCard(
    visit: VisitEntry,
    onEditVisit: () -> Unit,
    onDeleteVisit: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = dateFormatter.format(Date(visit.visitDate))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelBadge(channel = visit.channel)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    VerdictBadge(verdict = visit.visitVerdict)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEditVisit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDeleteVisit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (visit.createdByMemberName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.restaurant_detail_logged_by, visit.createdByMemberName),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberPrimary
                    )
                }
            }

            if (visit.billAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.restaurant_detail_total_bill_fmt, String.format(Locale.getDefault(), "%.0f", visit.billAmount)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (visit.photoUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visit.photoUris.forEach { photoUri ->
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Visit photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPhotoClick(photoUri) }
                        )
                    }
                }
            }

            if (visit.generalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.restaurant_detail_note_prefix, visit.generalNotes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
