package com.personaltracker.ui.screens.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.EmiEntity
import com.personaltracker.data.database.entity.EmiPaymentEntity
import com.personaltracker.domain.repository.EmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class EmiWithStatus(
    val emi: EmiEntity,
    val paidMonths: Int,
    val isOverdue: Boolean,
    val nextDueDate: LocalDate?
)

data class EmiUiState(
    val emis: List<EmiWithStatus> = emptyList(),
    val totalMonthlyEmi: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Detail view state
    val selectedEmi: EmiEntity? = null,
    val payments: List<EmiPaymentEntity> = emptyList(),
    val saveSuccess: Boolean = false
)

@HiltViewModel
class EmiViewModel @Inject constructor(
    private val emiRepository: EmiRepository
) : ViewModel() {

    private val _rawEmis: Flow<List<EmiEntity>> = emiRepository.getAllActiveEmis()
    private val _totalMonthly: Flow<Double> = emiRepository.getTotalMonthlyEmi().map { it ?: 0.0 }

    val uiState: StateFlow<EmiUiState> = combine(
        _rawEmis,
        _totalMonthly
    ) { emis, totalMonthly ->
        val today = LocalDate.now()
        val currentYm = YearMonth.now()
        val emisWithStatus = emis.map { emi ->
            val startYm = YearMonth.of(emi.startDate.year, emi.startDate.month)
            val monthsElapsed = startYm.until(currentYm, java.time.temporal.ChronoUnit.MONTHS).toInt()
            val expectedPaid = (monthsElapsed + 1).coerceAtMost(emi.tenureMonths)
            val nextDueDate = today.withDayOfMonth(emi.dueDay.coerceAtMost(today.lengthOfMonth()))
            val adjustedDue = if (nextDueDate.isBefore(today)) nextDueDate.plusMonths(1) else nextDueDate
            EmiWithStatus(
                emi = emi,
                paidMonths = 0, // Will be loaded separately per EMI in detail screen
                isOverdue = today.dayOfMonth > emi.dueDay && monthsElapsed >= 0,
                nextDueDate = adjustedDue
            )
        }
        EmiUiState(
            emis = emisWithStatus,
            totalMonthlyEmi = totalMonthly,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EmiUiState(isLoading = true)
    )

    fun deleteEmi(emi: EmiEntity) {
        viewModelScope.launch {
            emiRepository.deleteEmi(emi)
        }
    }

    fun markPayment(emiId: Long, month: Int, year: Int, amount: Double) {
        viewModelScope.launch {
            val existing = emiRepository.getPayment(emiId, month, year)
            if (existing == null) {
                emiRepository.insertPayment(
                    EmiPaymentEntity(
                        emiId = emiId,
                        month = month,
                        year = year,
                        amount = amount,
                        paidDate = LocalDate.now(),
                        isPaid = true
                    )
                )
            }
            loadPayments(emiId)
        }
    }

    fun loadEmiDetail(emiId: Long) {
        viewModelScope.launch {
            val emi = emiRepository.getEmiById(emiId)
            _detailState.update { it.copy(selectedEmi = emi) }
            loadPayments(emiId)
        }
    }

    private val _detailState = MutableStateFlow(EmiUiState())

    private fun loadPayments(emiId: Long) {
        viewModelScope.launch {
            emiRepository.getPaymentsForEmi(emiId).collect { payments ->
                _detailState.update { it.copy(payments = payments) }
            }
        }
    }

    val detailState: StateFlow<EmiUiState> get() = _detailState.asStateFlow()

    fun addEmi(
        name: String, lenderName: String, loanAmount: Double, emiAmount: Double,
        interestRate: Double, tenureMonths: Int, startDate: String,
        dueDay: Int, accountNumber: String?, notes: String?
    ) {
        if (name.isBlank() || emiAmount <= 0 || tenureMonths <= 0) return
        viewModelScope.launch {
            try {
                val parsedDate = runCatching { java.time.LocalDate.parse(startDate) }.getOrElse { LocalDate.now() }
                emiRepository.insertEmi(
                    EmiEntity(
                        name = name, lenderName = lenderName, loanAmount = loanAmount,
                        emiAmount = emiAmount, interestRate = interestRate,
                        tenureMonths = tenureMonths, startDate = parsedDate,
                        dueDay = dueDay, loanAccountNumber = accountNumber, notes = notes
                    )
                )
                _detailState.update { it.copy(saveSuccess = true) }
            } catch (e: Exception) {
                _detailState.update { it.copy(error = e.message) }
            }
        }
    }

    fun resetSaveState() {
        _detailState.update { it.copy(saveSuccess = false, error = null) }
    }
}
