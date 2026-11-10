package com.prismpixelvertex.palatelog.feature.quick_capture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prismpixelvertex.palatelog.core.database.DishFeedbackDao
import com.prismpixelvertex.palatelog.core.database.DishFeedbackEntity
import com.prismpixelvertex.palatelog.core.database.FamilyMemberDao
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.database.VisitDao
import com.prismpixelvertex.palatelog.core.database.VisitEntryEntity
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.location.LocationService
import com.prismpixelvertex.palatelog.core.model.Channel
import com.prismpixelvertex.palatelog.core.model.DishCategory
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuickCaptureUiState(
    val placeName: String = "",
    val streetOrArea: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isLocationLoading: Boolean = true,
    val selectedVerdict: Verdict = Verdict.NO_GO,
    val quickReason: String = "",
    val dishName: String = "",
    val photoUris: List<String> = emptyList(),
    val isHighwayStop: Boolean = false,
    val highwayName: String = "",
    val nearbyExistingRestaurant: RestaurantEntity? = null,
    val isSaving: Boolean = false
)

sealed interface QuickCaptureEvent {
    object SavedSuccessfully : QuickCaptureEvent
    data class Error(val message: String) : QuickCaptureEvent
}

@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val locationService: LocationService,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val memberDao: FamilyMemberDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuickCaptureEvent>()
    val events: SharedFlow<QuickCaptureEvent> = _events.asSharedFlow()

    init {
        val navLat = savedStateHandle.get<String>("lat")?.toDoubleOrNull()
        val navLng = savedStateHandle.get<String>("lng")?.toDoubleOrNull()
        val navAddress = savedStateHandle.get<String>("address")
        val navCity = savedStateHandle.get<String>("city")

        if (navLat != null && navLng != null) {
            _uiState.value = _uiState.value.copy(
                latitude = navLat,
                longitude = navLng,
                streetOrArea = navAddress ?: "",
                city = navCity ?: "",
                isLocationLoading = false
            )
            checkNearbyRestaurant(navLat, navLng)
        } else {
            acquireLocation()
        }
    }

    private fun checkNearbyRestaurant(lat: Double, lng: Double) {
        viewModelScope.launch {
            val bbox = GeoUtils.getBoundingBox(lat, lng, 0.1) // 100m
            val nearby = restaurantDao.getRestaurantsInBoundingBox(bbox.minLat, bbox.maxLat, bbox.minLng, bbox.maxLng)
            val closest = nearby.minByOrNull {
                GeoUtils.calculateDistanceKm(lat, lng, it.latitude, it.longitude)
            }
            _uiState.value = _uiState.value.copy(nearbyExistingRestaurant = closest)
        }
    }

    fun acquireLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true)
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                val bbox = GeoUtils.getBoundingBox(loc.latitude, loc.longitude, 0.1) // 100m
                val nearby = restaurantDao.getRestaurantsInBoundingBox(bbox.minLat, bbox.maxLat, bbox.minLng, bbox.maxLng)
                val closest = nearby.minByOrNull {
                    GeoUtils.calculateDistanceKm(loc.latitude, loc.longitude, it.latitude, it.longitude)
                }

                _uiState.value = _uiState.value.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    placeName = if (_uiState.value.placeName.isBlank()) loc.placeName else _uiState.value.placeName,
                    streetOrArea = loc.street,
                    city = loc.city,
                    nearbyExistingRestaurant = closest,
                    isLocationLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLocationLoading = false)
            }
        }
    }

    fun onLocationPicked(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                latitude = lat,
                longitude = lng,
                isLocationLoading = false
            )
            val geocoded = locationService.reverseGeocode(lat, lng)
            _uiState.value = _uiState.value.copy(
                placeName = geocoded.first,
                streetOrArea = if (geocoded.second.isNotBlank()) geocoded.second else _uiState.value.streetOrArea,
                city = if (geocoded.third.isNotBlank()) geocoded.third else _uiState.value.city
            )
            checkNearbyRestaurant(lat, lng)
        }
    }

    fun onPlaceNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(placeName = name)
    }

    fun onVerdictSelected(verdict: Verdict) {
        _uiState.value = _uiState.value.copy(selectedVerdict = verdict)
    }

    fun onQuickReasonChanged(reason: String) {
        _uiState.value = _uiState.value.copy(quickReason = reason)
    }

    fun onDishNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(dishName = name)
    }

    fun onPhotoAdded(uriString: String) {
        val updated = (_uiState.value.photoUris + uriString).distinct()
        _uiState.value = _uiState.value.copy(photoUris = updated)
    }

    fun onPhotoRemoved(uriString: String) {
        val updated = _uiState.value.photoUris.filterNot { it == uriString }
        _uiState.value = _uiState.value.copy(photoUris = updated)
    }

    fun onHighwayToggle(isHighway: Boolean) {
        _uiState.value = _uiState.value.copy(isHighwayStop = isHighway)
    }

    fun onHighwayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(highwayName = name)
    }

    fun onUseNearbyRestaurant(restaurant: RestaurantEntity) {
        _uiState.value = _uiState.value.copy(
            placeName = restaurant.name,
            selectedVerdict = try { Verdict.valueOf(restaurant.overallVerdict) } catch (e: Exception) { Verdict.NO_GO },
            nearbyExistingRestaurant = null
        )
    }

    fun saveQuickLog() {
        val state = _uiState.value
        if (state.placeName.isBlank()) {
            viewModelScope.launch {
                _events.emit(QuickCaptureEvent.Error("Please enter the restaurant or stall name"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val now = System.currentTimeMillis()
                val restaurantId = UUID.randomUUID().toString()

                val restaurantType = if (state.isHighwayStop) RestaurantType.DHABA else RestaurantType.DINE_IN

                val restaurantEntity = RestaurantEntity(
                    id = restaurantId,
                    name = state.placeName.trim(),
                    latitude = state.latitude,
                    longitude = state.longitude,
                    address = state.streetOrArea,
                    city = state.city,
                    highwayOrRoute = if (state.isHighwayStop) state.highwayName.ifBlank { "Highway Route" } else "",
                    cuisine = "",
                    restaurantType = restaurantType.name,
                    overallVerdict = state.selectedVerdict.name,
                    tags = if (state.quickReason.isNotBlank()) listOf(state.quickReason) else emptyList(),
                    isShared = true,
                    createdAt = now,
                    updatedAt = now,
                    isDeleted = false
                )
                restaurantDao.insert(restaurantEntity)

                val primaryMember = memberDao.getPrimaryMember()
                val primaryName = primaryMember?.displayName?.ifBlank { "Me" } ?: "Me"

                val visitId = UUID.randomUUID().toString()
                val visitChannel = if (state.isHighwayStop) Channel.HIGHWAY_STOP.displayName else Channel.WALK_IN.displayName
                val visitPhotosString = if (state.photoUris.isEmpty()) null else state.photoUris.joinToString(",")

                val visitEntity = VisitEntryEntity(
                    id = visitId,
                    restaurantId = restaurantId,
                    visitDate = now,
                    channel = visitChannel,
                    visitVerdict = state.selectedVerdict.name,
                    billAmount = 0.0,
                    billReceiptPhotoUri = visitPhotosString,
                    generalNotes = state.quickReason,
                    createdByMemberId = primaryMember?.id ?: "",
                    createdByMemberName = primaryName,
                    isShared = true,
                    updatedAt = now,
                    isDeleted = false
                )
                visitDao.insert(visitEntity)

                if (state.dishName.isNotBlank()) {
                    val dishVerdict = when (state.selectedVerdict) {
                        Verdict.GO -> DishVerdict.GO
                        Verdict.NO_GO -> DishVerdict.NO_GO
                        Verdict.CAUTION -> DishVerdict.MEH
                    }
                    val memberPrefMap = if (state.quickReason.isNotBlank()) {
                        mapOf(primaryName to state.quickReason)
                    } else {
                        mapOf(primaryName to dishVerdict.displayName)
                    }

                    val dishEntity = DishFeedbackEntity(
                        id = UUID.randomUUID().toString(),
                        visitId = visitId,
                        restaurantId = restaurantId,
                        dishName = state.dishName.trim(),
                        category = DishCategory.MAIN.name,
                        verdict = dishVerdict.name,
                        tags = if (state.quickReason.isNotBlank()) listOf(state.quickReason) else emptyList(),
                        memberPreferences = memberPrefMap,
                        price = 0.0,
                        photoUris = state.photoUris,
                        notes = state.quickReason,
                        isShared = true,
                        updatedAt = now,
                        isDeleted = false
                    )
                    dishDao.insert(dishEntity)
                }

                _events.emit(QuickCaptureEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.emit(QuickCaptureEvent.Error(e.localizedMessage ?: "Failed to save"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
