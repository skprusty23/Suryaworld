package com.personaltracker.ui.screens.school

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.SchoolExpenseEntity
import com.personaltracker.domain.repository.SchoolExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SchoolUiState(
    val expenses: List<SchoolExpenseEntity> = emptyList(),
    val filteredExpenses: List<SchoolExpenseEntity> = emptyList(),
    val children: List<String> = emptyList(),
    val availableYears: List<String> = emptyList(),
    val selectedChild: String = "",
    val selectedYear: String = "",
    val totalAmount: Double = 0.0,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SchoolViewModel @Inject constructor(
    private val schoolExpenseRepository: SchoolExpenseRepository
) : ViewModel() {

    private val _selectedChild = MutableStateFlow("")
    private val _selectedYear = MutableStateFlow(currentAcademicYear())

    private val _uiState = MutableStateFlow(SchoolUiState(isLoading = true))
    val uiState: StateFlow<SchoolUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Observe children list
            schoolExpenseRepository.getAllChildren().collect { children ->
                val initialChild = if (_selectedChild.value.isBlank() && children.isNotEmpty()) children.first() else _selectedChild.value
                _selectedChild.value = initialChild
            }
        }

        viewModelScope.launch {
            combine(
                schoolExpenseRepository.getAllChildren(),
                schoolExpenseRepository.getAllYears(),
                schoolExpenseRepository.getAllSchoolExpenses(),
                _selectedChild,
                _selectedYear
            ) { children, years, allExpenses, selectedChild, selectedYear ->
                val allYears = buildAcademicYears()
                val mergedYears = (years + allYears).distinct().sortedDescending()
                val childToUse = selectedChild.ifBlank { children.firstOrNull() ?: "" }
                val yearToUse = selectedYear.ifBlank { mergedYears.firstOrNull() ?: currentAcademicYear() }

                val filtered = allExpenses.filter { expense ->
                    (childToUse.isBlank() || expense.childName == childToUse) &&
                            (yearToUse.isBlank() || expense.academicYear == yearToUse)
                }

                val total = filtered.sumOf { it.amount }
                val categoryTotals = filtered.groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                SchoolUiState(
                    expenses = allExpenses,
                    filteredExpenses = filtered,
                    children = children,
                    availableYears = mergedYears,
                    selectedChild = childToUse,
                    selectedYear = yearToUse,
                    totalAmount = total,
                    categoryTotals = categoryTotals,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectChild(child: String) {
        _selectedChild.value = child
    }

    fun selectYear(year: String) {
        _selectedYear.value = year
    }

    fun addSchoolExpense(expense: SchoolExpenseEntity) {
        viewModelScope.launch {
            try {
                schoolExpenseRepository.insertSchoolExpense(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteSchoolExpense(expense: SchoolExpenseEntity) {
        viewModelScope.launch {
            try {
                schoolExpenseRepository.deleteSchoolExpense(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun currentAcademicYear(): String {
            val now = LocalDate.now()
            return if (now.monthValue >= 4) {
                "${now.year}-${(now.year + 1).toString().takeLast(2)}"
            } else {
                "${now.year - 1}-${now.year.toString().takeLast(2)}"
            }
        }

        fun buildAcademicYears(): List<String> {
            val currentYear = LocalDate.now().year
            return (0..4).map { offset ->
                val start = currentYear - offset
                val end = (start + 1).toString().takeLast(2)
                "$start-$end"
            }
        }
    }
}
