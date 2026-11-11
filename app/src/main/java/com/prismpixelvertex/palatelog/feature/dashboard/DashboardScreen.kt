package com.prismpixelvertex.palatelog.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.prismpixelvertex.palatelog.core.service.PalateRadarService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
import com.prismpixelvertex.palatelog.core.designsystem.DishVerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.navigation.ExternalMapNavigator
import com.prismpixelvertex.palatelog.feature.help.HelpTopicKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRestaurantDetail: (String) -> Unit,
    onNavigateToQuickCapture: () -> Unit,
    onNavigateToEntryEditor: (restaurantId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHelpGuide: (HelpTopicKey?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isFabExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isScrollIndicatorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(listState, uiState.restaurants.size) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collectLatest {
                if ((listState.canScrollForward) && (uiState.restaurants.size > 1)) {
                    isScrollIndicatorVisible = true
                    delay(2500.milliseconds)
                    isScrollIndicatorVisible = false
                } else {
                    isScrollIndicatorVisible = false
                }
            }
    }

    // Refresh location whenever entering/returning to the Explore screen
    LaunchedEffect(Unit) {
        viewModel.refreshLocation()
    }

    // Reset scroll position to top whenever search query or filter selection changes
    LaunchedEffect(uiState.selectedFilter, uiState.searchQuery) {
        listState.scrollToItem(0)
    }

    // Reset scroll position to top when location loading completes or distance sorting is applied
    LaunchedEffect(uiState.isLocationLoading, uiState.isSortedByDistance) {
        if (!uiState.isLocationLoading) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToHelpGuide(null) }) {
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Quick Regret Log (3s) FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                onNavigateToQuickCapture()
                            },
                            containerColor = VerdictNoGoRed,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                            text = { Text(stringResource(R.string.dashboard_fab_quick_log), fontWeight = FontWeight.Bold) }
                        )

                        // 2. Full Visit Log FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                onNavigateToEntryEditor(null)
                            },
                            containerColor = AmberPrimary,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text(stringResource(R.string.dashboard_fab_full_visit), fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Main Trigger FAB
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = if (isFabExpanded) MaterialTheme.colorScheme.surfaceVariant else AmberPrimary,
                    contentColor = if (isFabExpanded) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isFabExpanded) 45f else 0f,
                        label = "fab_rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isFabExpanded) stringResource(R.string.action_close) else stringResource(R.string.nav_quick_log),
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.showFirstLaunchHelpDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onDismissFirstLaunchHelp() },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(Dimens.IconSizeLarge)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.help_first_launch_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.help_first_launch_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onAcceptFirstLaunchHelp()
                            onNavigateToHelpGuide(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.help_first_launch_action_view),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDismissFirstLaunchHelp() }) {
                        Text(
                            text = stringResource(R.string.help_first_launch_action_dismiss),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                shape = RoundedCornerShape(Dimens.RadiusDialog)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.dashboard_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_close))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Palate Radar Highway Trip Card (Option A: Collapsible Banner)
            PalateRadarCard(
                onNavigateToHelpGuide = onNavigateToHelpGuide,
                isListScrolling = listState.isScrollInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.ALL,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.ALL) },
                    label = { Text(stringResource(R.string.dashboard_filter_all, uiState.totalCount)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.GO_ONLY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.GO_ONLY) },
                    label = { Text(stringResource(R.string.dashboard_filter_go, uiState.goCount)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.NO_GO_ONLY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.NO_GO_ONLY) },
                    label = { Text(stringResource(R.string.dashboard_filter_no_go, uiState.noGoCount)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.NEARBY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.NEARBY) },
                    label = { Text(stringResource(R.string.dashboard_filter_nearby)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.HIGHWAY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.HIGHWAY) },
                    label = { Text(stringResource(R.string.dashboard_filter_highway)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.DELIVERY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.DELIVERY) },
                    label = { Text(stringResource(R.string.dashboard_filter_delivery)) }
                )
            }

            // Location / Sorting Status Badge Indicator
            if (uiState.isLocationLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = AmberPrimary
                    )
                    Text(
                        text = stringResource(R.string.dashboard_location_fetching),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.selectedFilter == DashboardFilter.NEARBY && uiState.isNearbyFallbackActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val noteText = if (uiState.userLatitude == null) {
                        stringResource(R.string.dashboard_nearby_location_unavailable)
                    } else {
                        stringResource(R.string.dashboard_nearby_no_spots)
                    }
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.isSortedByDistance && uiState.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_sorted_by_distance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!uiState.isSortedByDistance && uiState.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_sorted_by_recent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Restaurant Cards List
            if (uiState.restaurants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.SpacingXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(Dimens.IconSizeHero)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingDefault))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) stringResource(R.string.dashboard_empty_title) else stringResource(R.string.dashboard_empty_search_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                        Text(
                            text = stringResource(R.string.dashboard_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.restaurants, key = { it.restaurant.id }) { item ->
                            RestaurantCard(
                                item = item,
                                onClick = { onNavigateToRestaurantDetail(item.restaurant.id) },
                                onNavigateClick = {
                                    ExternalMapNavigator.navigateTo(
                                        context = context,
                                        latitude = item.restaurant.latitude,
                                        longitude = item.restaurant.longitude,
                                        label = item.restaurant.name
                                    )
                                }
                            )
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
                                    val nextIndex = (listState.firstVisibleItemIndex + 2).coerceAtMost(uiState.restaurants.lastIndex)
                                    listState.animateScrollToItem(nextIndex)
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
    }
}

@Composable
private fun RestaurantCard(
    item: RestaurantCardUiState,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    val r = item.restaurant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Name + Verdict Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = r.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                VerdictBadge(verdict = r.overallVerdict)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Row: Cuisine & Type
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (r.cuisine.isNotBlank()) {
                    Text(
                        text = r.cuisine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    text = r.restaurantType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Location & Highway Row
            if (r.highwayOrRoute.isNotBlank() || r.address.isNotBlank() || r.city.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val locationText = buildString {
                    if (r.highwayOrRoute.isNotBlank()) append("🛣️ ${r.highwayOrRoute} • ")
                    if (r.city.isNotBlank()) append(r.city)
                    else if (r.address.isNotBlank()) append(r.address)
                }
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Matched Dishes or Top Dishes Row
            if (item.matchedDishes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.matchedDishes.take(4).forEach { dish ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DishVerdictBadge(verdict = dish.verdict)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dish.dishName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (item.topMustTryDishes.isNotEmpty() || item.topAvoidDishes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.topMustTryDishes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.dashboard_must_try_prefix, item.topMustTryDishes.joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = VerdictGoGreen,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (item.topAvoidDishes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.dashboard_avoid_prefix, item.topAvoidDishes.joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = VerdictNoGoRed,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Distance + Visits + Navigate Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.distanceKm != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = GeoUtils.formatDistance(item.distanceKm),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = stringResource(
                            if (item.totalVisits == 1) R.string.dashboard_card_visits else R.string.dashboard_card_visits_plural,
                            item.totalVisits
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = stringResource(R.string.map_preview_navigate),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PalateRadarCard(
    onNavigateToHelpGuide: (HelpTopicKey?) -> Unit,
    isListScrolling: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRadarActive by PalateRadarService.isRadarActive.collectAsState()
    val activeStopCount by PalateRadarService.activeStopCount.collectAsState()
    val prefs = remember { context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE) }
    var isExpanded by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    val locationRequiredMsg = stringResource(R.string.radar_permission_location_required)

    // Auto-collapse when user scrolls the restaurant list
    LaunchedEffect(isListScrolling) {
        if (isListScrolling && isExpanded) {
            isExpanded = false
        }
    }

    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(Dimens.IconSizeLarge)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.radar_disclosure_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.radar_disclosure_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit { putBoolean("radar_disclosure_accepted", true) }
                        showDisclosureDialog = false
                        PalateRadarService.startRadar(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.radar_disclosure_confirm),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosureDialog = false }) {
                    Text(
                        text = stringResource(R.string.radar_disclosure_dismiss),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(Dimens.RadiusDialog)
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRadarActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = if (isRadarActive) {
            BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.8f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Collapsed Header Row (40-44dp high)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = if (isRadarActive) AmberPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.radar_dashboard_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isRadarActive) {
                            VerdictGoGreen.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }
                    ) {
                        Text(
                            text = if (isRadarActive) {
                                "${stringResource(R.string.radar_tile_state_active).uppercase()} ($activeStopCount)"
                            } else {
                                stringResource(R.string.radar_tile_state_inactive).uppercase()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRadarActive) VerdictGoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isRadarActive) {
                        Button(
                            onClick = { PalateRadarService.stopRadar(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = VerdictNoGoRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.radar_dashboard_btn_stop_short),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val fineLocationGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!fineLocationGranted) {
                                    Toast.makeText(
                                        context,
                                        locationRequiredMsg,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                val isAccepted = prefs.getBoolean("radar_disclosure_accepted", false)
                                if (!isAccepted) {
                                    showDisclosureDialog = true
                                } else {
                                    PalateRadarService.startRadar(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.radar_dashboard_btn_start_short),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            label = "radar_chevron"
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(
                                if (isExpanded) R.string.radar_dashboard_collapse else R.string.radar_dashboard_expand
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }

            // Expanded Drawer Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isRadarActive) {
                            stringResource(R.string.radar_dashboard_card_active_desc, activeStopCount)
                        } else {
                            stringResource(R.string.radar_dashboard_card_inactive_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        TextButton(
                            onClick = { onNavigateToHelpGuide(HelpTopicKey.RADAR) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.help_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


