package com.personaltracker.ui.screens.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.GoldInvestmentEntity
import com.personaltracker.domain.repository.GoldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoldUiState(
    val goldList: List<GoldInvestmentEntity> = emptyList(),
    val filteredList: List<GoldInvestmentEntity> = emptyList(),
    val totalGrams: Double = 0.0,
    val totalInvested: Double = 0.0,
    val selectedType: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GoldViewModel @Inject constructor(
    private val goldRepository: GoldRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow("All")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _uiState = MutableStateFlow(GoldUiState(isLoading = true))
    val uiState: StateFlow<GoldUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                goldRepository.getAllGoldInvestments(),
                goldRepository.getTotalGrams(),
                goldRepository.getTotalInvested(),
                _selectedType
            ) { list, grams, invested, type ->
                val filtered = if (type == "All") list
                else list.filter { it.goldType.equals(type, ignoreCase = true) }
                GoldUiState(
                    goldList = list,
                    filteredList = filtered,
                    totalGrams = grams ?: 0.0,
                    totalInvested = invested ?: 0.0,
                    selectedType = type,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setTypeFilter(type: String) {
        _selectedType.value = type
    }

    fun addGoldInvestment(gold: GoldInvestmentEntity) {
        viewModelScope.launch {
            try {
                goldRepository.insertGoldInvestment(gold)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteGoldInvestment(gold: GoldInvestmentEntity) {
        viewModelScope.launch {
            try {
                goldRepository.deleteGoldInvestment(gold)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
