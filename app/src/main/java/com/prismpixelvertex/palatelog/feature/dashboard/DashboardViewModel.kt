package com.prismpixelvertex.palatelog.feature.dashboard

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prismpixelvertex.palatelog.core.database.DishFeedbackDao
import com.prismpixelvertex.palatelog.core.database.DishFeedbackEntity
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.database.VisitDao
import com.prismpixelvertex.palatelog.core.database.VisitEntryEntity
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.location.LocationService
import com.prismpixelvertex.palatelog.core.model.DishFeedback
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.Restaurant
import com.prismpixelvertex.palatelog.core.model.Verdict
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

enum class DashboardFilter {
    ALL,
    GO_ONLY,
    NO_GO_ONLY,
    NEARBY,
    HIGHWAY,
    DELIVERY
}

data class RestaurantCardUiState(
    val restaurant: Restaurant,
    val distanceKm: Double?,
    val totalVisits: Int,
    val topMustTryDishes: List<String>,
    val topAvoidDishes: List<String>,
    val matchedDishes: List<DishFeedback> = emptyList(),
    val searchMatchScore: Int = 0
)

data class DashboardUiState(
    val searchQuery: String = "",
    val selectedFilter: DashboardFilter = DashboardFilter.ALL,
    val restaurants: List<RestaurantCardUiState> = emptyList(),
    val totalCount: Int = 0,
    val goCount: Int = 0,
    val noGoCount: Int = 0,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val isLoading: Boolean = false,
    val isLocationLoading: Boolean = true,
    val isSortedByDistance: Boolean = false,
    val isNearbyFallbackActive: Boolean = false,
    val showFirstLaunchHelpDialog: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val locationService: LocationService
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "palatelog_prefs"
        private const val KEY_FIRST_LAUNCH_HELP = "has_seen_first_launch_help"
        private const val LOCATION_CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes location cache expiry
        private const val SIGNIFICANT_DISTANCE_SHIFT_KM = 0.05 // 50 meters shift threshold for list re-sorting
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private var lastLocationFetchTime = 0L
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(DashboardFilter.ALL)
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isLocationLoading = MutableStateFlow(true)
    private val _showFirstLaunchHelpDialog = MutableStateFlow(
        !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_LAUNCH_HELP, false)
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        restaurantDao.getAllRestaurants(),
        visitDao.getAllVisits(),
        dishDao.getAllDishes(),
        _searchQuery,
        _selectedFilter,
        _userLocation,
        _isLocationLoading,
        _showFirstLaunchHelpDialog
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val restaurantEntities = flows[0] as List<RestaurantEntity>
        @Suppress("UNCHECKED_CAST")
        val visitEntities = flows[1] as List<VisitEntryEntity>
        @Suppress("UNCHECKED_CAST")
        val dishEntities = flows[2] as List<DishFeedbackEntity>
        val query = flows[3] as String
        val filter = flows[4] as DashboardFilter
        @Suppress("UNCHECKED_CAST")
        val location = flows[5] as Pair<Double, Double>?
        val isLocLoading = flows[6] as Boolean
        val showHelpDialog = flows[7] as Boolean

        val domainRestaurants = restaurantEntities.map { it.toDomain() }
        val domainDishes = dishEntities.map { it.toDomain() }
        val dishesByRestaurant = domainDishes.groupBy { it.restaurantId }

        val trimmedQuery = query.trim().lowercase()
        val queryTokens = trimmedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val evaluatedCards = domainRestaurants.mapNotNull { r ->
            val restaurantDishes = dishesByRestaurant[r.id] ?: emptyList()

            val topMustTry = restaurantDishes
                .filter { it.verdict == DishVerdict.GO }
                .map { it.dishName }
                .distinct()
                .take(3)

            val topAvoid = restaurantDishes
                .filter { it.verdict == DishVerdict.NO_GO }
                .map { it.dishName }
                .distinct()
                .take(2)

            var matchScore = 0
            val matchedDishesForRestaurant = mutableListOf<DishFeedback>()

            if (trimmedQuery.isNotBlank()) {
                val nameMatch = r.name.lowercase().contains(trimmedQuery)
                val cityMatch = r.city.lowercase().contains(trimmedQuery)
                val cuisineMatch = r.cuisine.lowercase().contains(trimmedQuery)
                val highwayMatch = r.highwayOrRoute.lowercase().contains(trimmedQuery)
                val tagMatch = r.tags.any { it.lowercase().contains(trimmedQuery) }

                if (nameMatch || cityMatch || cuisineMatch || highwayMatch || tagMatch) {
                    matchScore += 100
                } else if (queryTokens.isNotEmpty()) {
                    val tokenHits = queryTokens.count { token ->
                        r.name.lowercase().contains(token) ||
                        r.city.lowercase().contains(token) ||
                        r.cuisine.lowercase().contains(token) ||
                        r.highwayOrRoute.lowercase().contains(token) ||
                        r.tags.any { it.lowercase().contains(token) }
                    }
                    matchScore += tokenHits * 10
                }

                for (d in restaurantDishes) {
                    val dishNameLower = d.dishName.lowercase()
                    val dishTagsLower = d.tags.map { it.lowercase() }

                    if (dishNameLower.contains(trimmedQuery) || dishTagsLower.any { it.contains(trimmedQuery) }) {
                        matchScore += 50
                        matchedDishesForRestaurant.add(d)
                    } else if (queryTokens.size > 1 && queryTokens.all { token -> dishNameLower.contains(token) || dishTagsLower.any { it.contains(token) } }) {
                        matchScore += 30
                        matchedDishesForRestaurant.add(d)
                    } else if (queryTokens.isNotEmpty() && queryTokens.any { token -> dishNameLower.contains(token) || dishTagsLower.any { it.contains(token) } }) {
                        matchScore += 10
                        matchedDishesForRestaurant.add(d)
                    }
                }

                if (matchScore == 0) {
                    return@mapNotNull null
                }
            }

            val dist = location?.let {
                GeoUtils.calculateDistanceKm(it.first, it.second, r.latitude, r.longitude)
            }
            val visits = visitEntities.count { it.restaurantId == r.id && !it.isDeleted }

            RestaurantCardUiState(
                restaurant = r,
                distanceKm = dist,
                totalVisits = visits,
                topMustTryDishes = topMustTry,
                topAvoidDishes = topAvoid,
                matchedDishes = matchedDishesForRestaurant.distinctBy { it.dishName.lowercase() },
                searchMatchScore = matchScore
            )
        }

        var nearbyFallbackActive = false

        val categoryFiltered = when (filter) {
            DashboardFilter.ALL -> evaluatedCards
            DashboardFilter.GO_ONLY -> evaluatedCards.filter { it.restaurant.overallVerdict == Verdict.GO }
            DashboardFilter.NO_GO_ONLY -> evaluatedCards.filter { it.restaurant.overallVerdict == Verdict.NO_GO }
            DashboardFilter.NEARBY -> {
                if (location != null) {
                    val nearbyList = evaluatedCards.filter { card ->
                        card.distanceKm != null && card.distanceKm <= 15.0
                    }
                    nearbyList.ifEmpty {
                        nearbyFallbackActive = true
                        evaluatedCards
                    }
                } else {
                    nearbyFallbackActive = true
                    evaluatedCards
                }
            }
            DashboardFilter.HIGHWAY -> evaluatedCards.filter {
                it.restaurant.highwayOrRoute.isNotBlank() || it.restaurant.restaurantType.name == "DHABA"
            }
            DashboardFilter.DELIVERY -> {
                val deliveryKeywords = setOf("zomato", "swiggy", "ownly", "eatclub", "magicpin", "ubereats", "door", "dunzo", "delivery", "cloud")
                val deliveryRestaurantIds = visitEntities
                    .filter { visit ->
                        val lower = visit.channel.lowercase()
                        deliveryKeywords.any { lower.contains(it) }
                    }
                    .map { it.restaurantId }
                    .toSet()
                evaluatedCards.filter { it.restaurant.id in deliveryRestaurantIds || it.restaurant.restaurantType.name == "CLOUD_KITCHEN" }
            }
        }

        val sortedCards = if (trimmedQuery.isNotBlank()) {
            categoryFiltered.sortedWith(
                compareByDescending<RestaurantCardUiState> { it.searchMatchScore }
                    .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                    .thenByDescending { it.restaurant.updatedAt }
            )
        } else {
            categoryFiltered.sortedWith(
                compareBy<RestaurantCardUiState> { it.distanceKm ?: Double.MAX_VALUE }
                    .thenByDescending { it.restaurant.updatedAt }
            )
        }

        DashboardUiState(
            searchQuery = query,
            selectedFilter = filter,
            restaurants = sortedCards,
            totalCount = domainRestaurants.size,
            goCount = domainRestaurants.count { it.overallVerdict == Verdict.GO },
            noGoCount = domainRestaurants.count { it.overallVerdict == Verdict.NO_GO },
            userLatitude = location?.first,
            userLongitude = location?.second,
            isLoading = false,
            isLocationLoading = isLocLoading,
            isSortedByDistance = location != null,
            isNearbyFallbackActive = nearbyFallbackActive,
            showFirstLaunchHelpDialog = showHelpDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true, isLocationLoading = true)
    )

    init {
        refreshLocation()
    }

    fun onDismissFirstLaunchHelp() {
        prefs.edit { putBoolean(KEY_FIRST_LAUNCH_HELP, true) }
        _showFirstLaunchHelpDialog.value = false
    }

    fun onAcceptFirstLaunchHelp() {
        prefs.edit { putBoolean(KEY_FIRST_LAUNCH_HELP, true) }
        _showFirstLaunchHelpDialog.value = false
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: DashboardFilter) {
        _selectedFilter.value = filter
    }

    fun refreshLocation(force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!force && _userLocation.value != null && (currentTime - lastLocationFetchTime) < LOCATION_CACHE_EXPIRY_MS) {
            _isLocationLoading.value = false
            return
        }

        viewModelScope.launch {
            if (_userLocation.value == null) {
                _isLocationLoading.value = true
            }
            try {
                val loc = withTimeoutOrNull(3000.milliseconds) {
                    locationService.getCurrentLocation(skipGeocoding = true)
                }
                if (loc != null) {
                    val prevLoc = _userLocation.value
                    if (prevLoc == null) {
                        _userLocation.value = Pair(loc.latitude, loc.longitude)
                        lastLocationFetchTime = System.currentTimeMillis()
                    } else {
                        val movedKm = GeoUtils.calculateDistanceKm(
                            prevLoc.first, prevLoc.second,
                            loc.latitude, loc.longitude
                        )
                        if (movedKm >= SIGNIFICANT_DISTANCE_SHIFT_KM) {
                            _userLocation.value = Pair(loc.latitude, loc.longitude)
                            lastLocationFetchTime = System.currentTimeMillis()
                        } else {
                            // Location has not shifted significantly (under 50m); extend cache timestamp
                            lastLocationFetchTime = System.currentTimeMillis()
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore location timeout or error
            } finally {
                _isLocationLoading.value = false
            }
        }
    }
}
