package com.prismpixelvertex.palatelog.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.location.LocationService
import com.prismpixelvertex.palatelog.core.model.Restaurant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectedCustomLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String = "",
    val street: String = "",
    val city: String = "",
    val isLoadingAddress: Boolean = true
)

data class MapUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val userLatitude: Double = 12.9716, // Default Bangalore center fallback
    val userLongitude: Double = 77.5946,
    val selectedRestaurant: Restaurant? = null,
    val selectedDistanceKm: Double? = null,
    val selectedCustomLocation: SelectedCustomLocation? = null,
    val isLocationReady: Boolean = false,
    val isLocationDisabled: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val restaurantDao: RestaurantDao,
    private val locationService: LocationService
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)
    private val _selectedCustomLocation = MutableStateFlow<SelectedCustomLocation?>(null)
    private val _isLocationDisabled = MutableStateFlow(false)

    val uiState: StateFlow<MapUiState> = combine(
        restaurantDao.getAllRestaurants(),
        _userLocation,
        _selectedRestaurant,
        _selectedCustomLocation,
        _isLocationDisabled
    ) { restaurantEntities, userLoc, selectedRest, selectedCustom, isLocDisabled ->
        val domainRestaurants = restaurantEntities.map { it.toDomain() }
        val dist = if (selectedRest != null && userLoc != null) {
            GeoUtils.calculateDistanceKm(userLoc.first, userLoc.second, selectedRest.latitude, selectedRest.longitude)
        } else null

        MapUiState(
            restaurants = domainRestaurants,
            userLatitude = userLoc?.first ?: 12.9716,
            userLongitude = userLoc?.second ?: 77.5946,
            selectedRestaurant = selectedRest,
            selectedDistanceKm = dist,
            selectedCustomLocation = selectedCustom,
            isLocationReady = userLoc != null,
            isLocationDisabled = isLocDisabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        loadLocation()
    }

    fun loadLocation() {
        val enabled = locationService.isLocationEnabled()
        _isLocationDisabled.value = !enabled
        if (!enabled) {
            return
        }
        viewModelScope.launch {
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                _userLocation.value = Pair(loc.latitude, loc.longitude)
                _isLocationDisabled.value = false
            }
        }
    }

    fun onMarkerSelected(restaurant: Restaurant?) {
        _selectedRestaurant.value = restaurant
        if (restaurant != null) {
            _selectedCustomLocation.value = null
        }
    }

    fun onMapTapped(latitude: Double, longitude: Double) {
        _selectedRestaurant.value = null
        _selectedCustomLocation.value = SelectedCustomLocation(
            latitude = latitude,
            longitude = longitude,
            isLoadingAddress = true
        )

        viewModelScope.launch {
            val geocoded = locationService.reverseGeocode(latitude, longitude)
            if (_selectedCustomLocation.value?.latitude == latitude &&
                _selectedCustomLocation.value?.longitude == longitude) {
                _selectedCustomLocation.value = SelectedCustomLocation(
                    latitude = latitude,
                    longitude = longitude,
                    placeName = geocoded.first,
                    street = geocoded.second,
                    city = geocoded.third,
                    isLoadingAddress = false
                )
            }
        }
    }

    fun clearCustomLocation() {
        _selectedCustomLocation.value = null
    }
}
