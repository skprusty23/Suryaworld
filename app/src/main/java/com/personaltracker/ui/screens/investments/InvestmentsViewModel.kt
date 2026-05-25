package com.personaltracker.ui.screens.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.InvestmentEntity
import com.personaltracker.domain.repository.InvestmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InvestmentsUiState(
    val investments: List<InvestmentEntity> = emptyList(),
    val selectedType: String = "All",
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val roiPercent: Double = 0.0,
    val upcomingMaturities: List<InvestmentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    companion object {
        val INVESTMENT_TYPES = listOf(
            "All", "LIC", "Mutual Fund", "SIP", "FD", "RD", "Stocks", "PPF", "Other"
        )
    }

    private val _selectedType = MutableStateFlow("All")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _filteredInvestments: Flow<List<InvestmentEntity>> = _selectedType
        .flatMapLatest { type ->
            if (type == "All") {
                investmentRepository.getAllActiveInvestments()
            } else {
                investmentRepository.getInvestmentsByType(type)
            }
        }

    private val _totalInvested = investmentRepository.getTotalInvestmentAmount().map { it ?: 0.0 }
    private val _totalCurrentValue = investmentRepository.getTotalCurrentValue().map { it ?: 0.0 }
    private val _upcomingMaturities = investmentRepository.getUpcomingMaturities(
        LocalDate.now(),
        LocalDate.now().plusMonths(3)
    )

    val uiState: StateFlow<InvestmentsUiState> = combine(
        _filteredInvestments,
        _selectedType,
        _totalInvested,
        _totalCurrentValue,
        _upcomingMaturities
    ) { investments, type, invested, currentVal, upcoming ->
        val roi = if (invested > 0) ((currentVal - invested) / invested * 100) else 0.0
        InvestmentsUiState(
            investments = investments,
            selectedType = type,
            totalInvested = invested,
            totalCurrentValue = currentVal,
            roiPercent = roi,
            upcomingMaturities = upcoming,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvestmentsUiState(isLoading = true)
    )

    fun filterByType(type: String) {
        _selectedType.value = type
    }

    fun deleteInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            investmentRepository.deleteInvestment(investment)
        }
    }

    fun getUpcomingMaturities(): List<InvestmentEntity> = uiState.value.upcomingMaturities
}
