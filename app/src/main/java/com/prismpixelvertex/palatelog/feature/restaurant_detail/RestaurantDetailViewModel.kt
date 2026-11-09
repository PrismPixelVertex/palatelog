package com.prismpixelvertex.palatelog.feature.restaurant_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prismpixelvertex.palatelog.core.database.DishFeedbackDao
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.database.VisitDao
import com.prismpixelvertex.palatelog.core.database.VisitEntryEntity
import com.prismpixelvertex.palatelog.core.location.LocationResult
import com.prismpixelvertex.palatelog.core.location.LocationService
import com.prismpixelvertex.palatelog.core.model.DishFeedback
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.Restaurant
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.core.model.VisitEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantDetailUiState(
    val restaurant: Restaurant? = null,
    val visits: List<VisitEntry> = emptyList(),
    val mustTryDishes: List<DishFeedback> = emptyList(),
    val avoidDishes: List<DishFeedback> = emptyList(),
    val neutralDishes: List<DishFeedback> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val locationService: LocationService
) : ViewModel() {

    val restaurantId: String = checkNotNull(savedStateHandle["restaurantId"])

    val uiState: StateFlow<RestaurantDetailUiState> = combine(
        restaurantDao.getRestaurantById(restaurantId),
        visitDao.getVisitsForRestaurant(restaurantId),
        dishDao.getDishesForRestaurant(restaurantId)
    ) { restaurantEntity, visitEntities, dishEntities ->
        val restaurant = restaurantEntity?.toDomain()
        val visits = visitEntities.map { it.toDomain() }
        val dishes = dishEntities.map { it.toDomain() }

        val mustTry = dishes.filter { it.verdict == DishVerdict.GO }.distinctBy { it.dishName.lowercase() }
        val avoid = dishes.filter { it.verdict == DishVerdict.NO_GO }.distinctBy { it.dishName.lowercase() }
        val neutral = dishes.filter { it.verdict == DishVerdict.MEH }.distinctBy { it.dishName.lowercase() }

        RestaurantDetailUiState(
            restaurant = restaurant,
            visits = visits,
            mustTryDishes = mustTry,
            avoidDishes = avoid,
            neutralDishes = neutral,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RestaurantDetailUiState(isLoading = true)
    )

    fun deleteRestaurant() {
        viewModelScope.launch {
            restaurantDao.softDelete(restaurantId)
        }
    }

    fun updateRestaurant(
        name: String,
        address: String,
        city: String,
        highwayOrRoute: String,
        latitude: Double,
        longitude: Double,
        cuisine: String,
        restaurantType: RestaurantType,
        overallVerdict: Verdict
    ) {
        val current = uiState.value.restaurant ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedEntity = RestaurantEntity(
                id = current.id,
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                address = address.trim(),
                city = city.trim(),
                highwayOrRoute = highwayOrRoute.trim(),
                cuisine = cuisine.trim(),
                restaurantType = restaurantType.name,
                overallVerdict = overallVerdict.name,
                tags = current.tags,
                isShared = current.isShared,
                createdAt = current.createdAt,
                updatedAt = now,
                isDeleted = false
            )
            restaurantDao.update(updatedEntity)
        }
    }

    fun updateVisit(
        visitId: String,
        channel: String,
        billAmount: Double,
        visitVerdict: Verdict,
        generalNotes: String
    ) {
        viewModelScope.launch {
            val currentVisits = uiState.value.visits
            val existing = currentVisits.firstOrNull { it.id == visitId } ?: return@launch
            val now = System.currentTimeMillis()
            val updatedEntity = VisitEntryEntity(
                id = existing.id,
                restaurantId = existing.restaurantId,
                visitDate = existing.visitDate,
                channel = channel,
                visitVerdict = visitVerdict.name,
                billAmount = billAmount,
                billReceiptPhotoUri = existing.billReceiptPhotoUri,
                generalNotes = generalNotes.trim(),
                createdByMemberId = existing.createdByMemberId,
                createdByMemberName = existing.createdByMemberName,
                isShared = existing.isShared,
                updatedAt = now,
                isDeleted = false
            )
            visitDao.insert(updatedEntity)
        }
    }

    fun deleteVisit(visitId: String) {
        viewModelScope.launch {
            visitDao.softDelete(visitId)
            dishDao.softDeleteForVisit(visitId)
        }
    }

    fun deleteDish(dishId: String) {
        viewModelScope.launch {
            dishDao.softDelete(dishId)
        }
    }

    fun getCurrentLocation(onResult: (LocationResult?) -> Unit) {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            onResult(location)
        }
    }

    fun reverseGeocode(latitude: Double, longitude: Double, onResult: (placeName: String, street: String, city: String) -> Unit) {
        viewModelScope.launch {
            val (place, street, city) = locationService.reverseGeocode(latitude, longitude)
            onResult(place, street, city)
        }
    }
}
