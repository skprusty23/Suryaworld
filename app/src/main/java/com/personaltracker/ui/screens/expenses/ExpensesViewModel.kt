package com.personaltracker.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.ExpenseEntity
import com.personaltracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CategoryTotal(val category: String, val total: Double)

data class ExpensesUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthTotal: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val selectedCategory: String = "All",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "All",
            "House Rent",
            "Grocery",
            "Vegetables",
            "Food",
            "Fuel",
            "Shopping",
            "Utilities",
            "Medical",
            "Miscellaneous"
        )
        private val MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
    }

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _selectedCategory = MutableStateFlow("All")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _monthExpenses: Flow<List<ExpenseEntity>> = _selectedMonth
        .flatMapLatest { ym ->
            val key = ym.format(MONTH_KEY_FORMATTER)
            expenseRepository.getExpensesByMonth(key)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _monthTotal: Flow<Double> = _selectedMonth
        .flatMapLatest { ym ->
            val key = ym.format(MONTH_KEY_FORMATTER)
            expenseRepository.getTotalByMonth(key).map { it ?: 0.0 }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _categoryTotals: Flow<List<CategoryTotal>> = _selectedMonth
        .flatMapLatest { ym ->
            val key = ym.format(MONTH_KEY_FORMATTER)
            expenseRepository.getCategoryTotalsByMonth(key).map { list ->
                list.map { ct -> CategoryTotal(ct.category, ct.total) }
            }
        }

    val uiState: StateFlow<ExpensesUiState> = combine(
        _monthExpenses,
        _monthTotal,
        _categoryTotals,
        _selectedMonth,
        _selectedCategory
    ) { expenses, total, categoryTotals, month, category ->
        val filtered = if (category == "All") {
            expenses
        } else {
            expenses.filter { it.category == category }
        }
        ExpensesUiState(
            expenses = filtered,
            selectedMonth = month,
            monthTotal = total,
            categoryTotals = categoryTotals,
            selectedCategory = category,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState(isLoading = true)
    )

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        _selectedMonth.update { it.plusMonths(1) }
    }

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    fun getUpcomingMaturities(): List<CategoryTotal> = uiState.value.categoryTotals
}
