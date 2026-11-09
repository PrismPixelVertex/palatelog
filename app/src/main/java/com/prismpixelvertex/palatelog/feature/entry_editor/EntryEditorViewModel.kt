package com.prismpixelvertex.palatelog.feature.entry_editor

import android.content.Context
import androidx.core.content.edit
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
import com.prismpixelvertex.palatelog.core.location.LocationService
import com.prismpixelvertex.palatelog.core.model.Channel
import com.prismpixelvertex.palatelog.core.model.DishCategory
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.FamilyMember
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DishDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: DishCategory = DishCategory.MAIN,
    val verdict: DishVerdict = DishVerdict.GO,
    val tagsText: String = "",
    val priceText: String = "",
    val memberReaction: String = "",
    val selectedMemberName: String = "",
    val photoUris: List<String> = emptyList()
)

data class EntryEditorUiState(
    val restaurantId: String? = null,
    val restaurantName: String = "",
    val existingRestaurants: List<RestaurantEntity> = emptyList(),
    val previousDishes: List<DishFeedbackEntity> = emptyList(),
    val channel: String = Channel.WALK_IN.displayName,
    val availableChannels: List<String> = Channel.DEFAULT_CHANNELS,
    val showAddChannelDialog: Boolean = false,
    val overallVerdict: Verdict = Verdict.GO,
    val cuisine: String = "",
    val address: String = "",
    val city: String = "",
    val highwayOrRoute: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val billAmountText: String = "",
    val visitPhotoUris: List<String> = emptyList(),
    val generalNotes: String = "",
    val dishes: List<DishDraft> = emptyList(),
    val familyMembers: List<FamilyMember> = emptyList(),
    val isShared: Boolean = true,
    val isSaving: Boolean = false
)

sealed interface EntryEditorEvent {
    object SavedSuccessfully : EntryEditorEvent
    data class Error(val message: String) : EntryEditorEvent
}

@HiltViewModel
class EntryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val memberDao: FamilyMemberDao,
    private val locationService: LocationService
) : ViewModel() {

    private val initialRestaurantId: String? = savedStateHandle["restaurantId"]
    private val navLat: Double? = savedStateHandle.get<String>("lat")?.toDoubleOrNull()
    private val navLng: Double? = savedStateHandle.get<String>("lng")?.toDoubleOrNull()
    private val navAddress: String? = savedStateHandle.get<String>("address")
    private val navCity: String? = savedStateHandle.get<String>("city")

    private val _uiState = MutableStateFlow(EntryEditorUiState(restaurantId = initialRestaurantId))
    val uiState: StateFlow<EntryEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EntryEditorEvent>()
    val events: SharedFlow<EntryEditorEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun getSavedCustomChannels(): Set<String> {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("custom_channels", emptySet()) ?: emptySet()
    }

    private fun saveCustomChannel(channelName: String) {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("custom_channels", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(channelName)
        prefs.edit { putStringSet("custom_channels", current) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val allRestaurants = restaurantDao.getAllRestaurants().firstOrNull() ?: emptyList()
            val members = memberDao.getAllMembers().firstOrNull()?.map { it.toDomain() } ?: emptyList()
            val dbChannels = visitDao.getDistinctChannels().map { Channel.getChannelDisplayName(it) }
            val customChannels = getSavedCustomChannels()
            val allChannels = (Channel.DEFAULT_CHANNELS + dbChannels + customChannels)
                .distinctBy { it.lowercase() }

            _uiState.value = _uiState.value.copy(
                existingRestaurants = allRestaurants,
                familyMembers = members,
                availableChannels = allChannels
            )

            if (!initialRestaurantId.isNullOrBlank()) {
                val existing = restaurantDao.getRestaurantByIdSync(initialRestaurantId)
                if (existing != null) {
                    selectExistingRestaurant(existing)
                }
            } else if (navLat != null && navLng != null) {
                _uiState.value = _uiState.value.copy(
                    latitude = navLat,
                    longitude = navLng,
                    address = navAddress ?: "",
                    city = navCity ?: ""
                )
            } else {
                val loc = locationService.getCurrentLocation()
                if (loc != null) {
                    _uiState.value = _uiState.value.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = if (_uiState.value.address.isBlank()) loc.street else _uiState.value.address,
                        city = if (_uiState.value.city.isBlank()) loc.city else _uiState.value.city
                    )
                }
            }
        }
    }

    fun selectExistingRestaurant(restaurant: RestaurantEntity) {
        viewModelScope.launch {
            val pastDishes = dishDao.getDishesForRestaurant(restaurant.id).firstOrNull() ?: emptyList()

            _uiState.value = _uiState.value.copy(
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                cuisine = restaurant.cuisine,
                address = restaurant.address,
                city = restaurant.city,
                highwayOrRoute = restaurant.highwayOrRoute,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                overallVerdict = try { Verdict.valueOf(restaurant.overallVerdict) } catch (_: Exception) { Verdict.GO },
                previousDishes = pastDishes
            )
        }
    }

    fun onRestaurantNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(restaurantName = name)
    }

    fun onChannelSelected(channel: String) {
        _uiState.value = _uiState.value.copy(channel = channel)
    }

    fun showAddChannelDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddChannelDialog = show)
    }

    fun onAddCustomChannel(newChannelName: String) {
        val trimmed = newChannelName.trim()
        if (trimmed.isNotBlank()) {
            saveCustomChannel(trimmed)
            val updatedAvailable = (_uiState.value.availableChannels + trimmed).distinctBy { it.lowercase() }
            _uiState.value = _uiState.value.copy(
                availableChannels = updatedAvailable,
                channel = trimmed,
                showAddChannelDialog = false
            )
        }
    }

    fun onOverallVerdictSelected(verdict: Verdict) {
        _uiState.value = _uiState.value.copy(overallVerdict = verdict)
    }

    @Suppress("unused")
    fun onCuisineChanged(cuisine: String) {
        _uiState.value = _uiState.value.copy(cuisine = cuisine)
    }

    fun onAddressChanged(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    fun onCityChanged(city: String) {
        _uiState.value = _uiState.value.copy(city = city)
    }

    @Suppress("unused")
    fun onHighwayChanged(highway: String) {
        _uiState.value = _uiState.value.copy(highwayOrRoute = highway)
    }

    fun onLocationPicked(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(latitude = lat, longitude = lng)
            val geocoded = locationService.reverseGeocode(lat, lng)
            val resolvedAddress = when {
                geocoded.second.isNotBlank() -> geocoded.second
                geocoded.first.isNotBlank() -> geocoded.first
                else -> _uiState.value.address
            }
            _uiState.value = _uiState.value.copy(
                address = resolvedAddress,
                city = if (geocoded.third.isNotBlank()) geocoded.third else _uiState.value.city
            )
        }
    }

    fun onBillAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(billAmountText = amount)
    }

    fun onVisitPhotoAdded(uriString: String) {
        val updated = (_uiState.value.visitPhotoUris + uriString).distinct()
        _uiState.value = _uiState.value.copy(visitPhotoUris = updated)
    }

    fun onVisitPhotoRemoved(uriString: String) {
        val updated = _uiState.value.visitPhotoUris.filterNot { it == uriString }
        _uiState.value = _uiState.value.copy(visitPhotoUris = updated)
    }

    fun onGeneralNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(generalNotes = notes)
    }

    fun onSharedToggle(isShared: Boolean) {
        _uiState.value = _uiState.value.copy(isShared = isShared)
    }

    // Dish Management
    fun addDishDraft(dishName: String = "", verdict: DishVerdict = DishVerdict.GO, tags: String = "") {
        val currentDishes = _uiState.value.dishes.toMutableList()
        currentDishes.add(
            DishDraft(
                name = dishName,
                verdict = verdict,
                tagsText = tags
            )
        )
        _uiState.value = _uiState.value.copy(dishes = currentDishes)
    }

    fun updateDishDraft(index: Int, updated: DishDraft) {
        val current = _uiState.value.dishes.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _uiState.value = _uiState.value.copy(dishes = current)
        }
    }

    fun removeDishDraft(index: Int) {
        val current = _uiState.value.dishes.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(dishes = current)
        }
    }

    fun onDishPhotoAdded(index: Int, uriString: String) {
        val current = _uiState.value.dishes.toMutableList()
        if (index in current.indices) {
            val updatedPhotos = (current[index].photoUris + uriString).distinct()
            current[index] = current[index].copy(photoUris = updatedPhotos)
            _uiState.value = _uiState.value.copy(dishes = current)
        }
    }

    fun onDishPhotoRemoved(index: Int, uriString: String) {
        val current = _uiState.value.dishes.toMutableList()
        if (index in current.indices) {
            val updatedPhotos = current[index].photoUris.filterNot { it == uriString }
            current[index] = current[index].copy(photoUris = updatedPhotos)
            _uiState.value = _uiState.value.copy(dishes = current)
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.restaurantName.isBlank()) {
            viewModelScope.launch {
                _events.emit(EntryEditorEvent.Error("Restaurant name cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val now = System.currentTimeMillis()
                val restaurantId = state.restaurantId ?: UUID.randomUUID().toString()

                val channelLower = state.channel.lowercase()
                val restaurantType = when {
                    channelLower.contains("highway") -> RestaurantType.DHABA
                    channelLower.contains("zomato") || channelLower.contains("swiggy") ||
                    channelLower.contains("ownly") || channelLower.contains("eatclub") ||
                    channelLower.contains("cloud") || channelLower.contains("delivery") -> RestaurantType.CLOUD_KITCHEN
                    channelLower.contains("drive") -> RestaurantType.DRIVE_THRU
                    else -> RestaurantType.DINE_IN
                }

                // 1. Insert or Update Restaurant
                val existing = if (!state.restaurantId.isNullOrBlank()) restaurantDao.getRestaurantByIdSync(state.restaurantId) else null
                if (existing != null) {
                    val restaurantEntity = RestaurantEntity(
                        id = existing.id,
                        name = state.restaurantName.trim(),
                        latitude = if (state.latitude != 0.0) state.latitude else existing.latitude,
                        longitude = if (state.longitude != 0.0) state.longitude else existing.longitude,
                        address = if (state.address.isNotBlank()) state.address else existing.address,
                        city = if (state.city.isNotBlank()) state.city else existing.city,
                        highwayOrRoute = if (state.highwayOrRoute.isNotBlank()) state.highwayOrRoute else existing.highwayOrRoute,
                        cuisine = if (state.cuisine.isNotBlank()) state.cuisine else existing.cuisine,
                        restaurantType = restaurantType.name,
                        overallVerdict = state.overallVerdict.name,
                        tags = existing.tags,
                        isShared = state.isShared,
                        createdAt = existing.createdAt,
                        updatedAt = now,
                        isDeleted = false
                    )
                    restaurantDao.update(restaurantEntity)
                } else {
                    val restaurantEntity = RestaurantEntity(
                        id = restaurantId,
                        name = state.restaurantName.trim(),
                        latitude = state.latitude,
                        longitude = state.longitude,
                        address = state.address,
                        city = state.city,
                        highwayOrRoute = state.highwayOrRoute,
                        cuisine = state.cuisine,
                        restaurantType = restaurantType.name,
                        overallVerdict = state.overallVerdict.name,
                        tags = emptyList(),
                        isShared = state.isShared,
                        createdAt = now,
                        updatedAt = now,
                        isDeleted = false
                    )
                    restaurantDao.insert(restaurantEntity)
                }

                // 2. Insert Visit Entry
                val primaryMember = memberDao.getPrimaryMember()
                val primaryName = primaryMember?.displayName?.ifBlank { "Me" } ?: "Me"

                val visitId = UUID.randomUUID().toString()
                val billAmount = state.billAmountText.toDoubleOrNull() ?: 0.0
                val visitPhotosString = if (state.visitPhotoUris.isEmpty()) null else state.visitPhotoUris.joinToString(",")

                val visitEntity = VisitEntryEntity(
                    id = visitId,
                    restaurantId = restaurantId,
                    visitDate = now,
                    channel = state.channel,
                    visitVerdict = state.overallVerdict.name,
                    billAmount = billAmount,
                    billReceiptPhotoUri = visitPhotosString,
                    generalNotes = state.generalNotes,
                    createdByMemberId = primaryMember?.id ?: "",
                    createdByMemberName = primaryName,
                    isShared = state.isShared,
                    updatedAt = now,
                    isDeleted = false
                )
                visitDao.insert(visitEntity)

                // 3. Insert Dishes
                val dishEntities = state.dishes.filter { it.name.isNotBlank() }.map { draft ->
                    val memberPrefMap = if (draft.selectedMemberName.isNotBlank() && draft.memberReaction.isNotBlank()) {
                        mapOf(draft.selectedMemberName to draft.memberReaction)
                    } else if (draft.memberReaction.isNotBlank()) {
                        mapOf("" to draft.memberReaction)
                    } else if (draft.tagsText.isNotBlank()) {
                        mapOf(primaryName to draft.tagsText)
                    } else {
                        mapOf(primaryName to draft.verdict.displayName)
                    }

                    DishFeedbackEntity(
                        id = draft.id,
                        visitId = visitId,
                        restaurantId = restaurantId,
                        dishName = draft.name.trim(),
                        category = draft.category.name,
                        verdict = draft.verdict.name,
                        tags = if (draft.tagsText.isNotBlank()) draft.tagsText.split(",").map { it.trim() } else emptyList(),
                        memberPreferences = memberPrefMap,
                        price = draft.priceText.toDoubleOrNull() ?: 0.0,
                        photoUris = draft.photoUris,
                        notes = draft.tagsText,
                        isShared = state.isShared,
                        updatedAt = now,
                        isDeleted = false
                    )
                }
                if (dishEntities.isNotEmpty()) {
                    dishDao.insertAll(dishEntities)
                }

                _events.emit(EntryEditorEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.emit(EntryEditorEvent.Error(e.localizedMessage ?: "Failed to save entry"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
