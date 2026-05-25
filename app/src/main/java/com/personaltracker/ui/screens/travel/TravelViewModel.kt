package com.personaltracker.ui.screens.travel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.TripEntity
import com.personaltracker.domain.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TravelUiState(
    val trips: List<TripEntity> = emptyList(),
    val activeTrips: List<TripEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TravelViewModel @Inject constructor(
    private val travelRepository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TravelUiState(isLoading = true))
    val uiState: StateFlow<TravelUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                travelRepository.getAllTrips(),
                travelRepository.getActiveTrips()
            ) { trips, activeTrips ->
                TravelUiState(
                    trips = trips,
                    activeTrips = activeTrips,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addTrip(trip: TripEntity) {
        viewModelScope.launch {
            try {
                travelRepository.insertTrip(trip)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            try {
                travelRepository.deleteTrip(trip)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun markTripCompleted(trip: TripEntity) {
        viewModelScope.launch {
            try {
                travelRepository.updateTrip(trip.copy(isCompleted = true))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
