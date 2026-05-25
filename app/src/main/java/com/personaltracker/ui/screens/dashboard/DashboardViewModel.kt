package com.personaltracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.EmiEntity
import com.personaltracker.data.database.entity.ExpenseEntity
import com.personaltracker.domain.repository.EmiRepository
import com.personaltracker.domain.repository.ExpenseRepository
import com.personaltracker.domain.repository.GoldRepository
import com.personaltracker.domain.repository.InvestmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class DashboardState(
    val monthlyExpenses: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val activeEmiTotal: Double = 0.0,
    val totalGoldGrams: Double = 0.0,
    val recentExpenses: List<ExpenseEntity> = emptyList(),
    val upcomingEmis: List<EmiEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val investmentRepository: InvestmentRepository,
    private val emiRepository: EmiRepository,
    private val goldRepository: GoldRepository
) : ViewModel() {

    private val currentYearMonth: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    // ------- individual flows -------

    private val monthlyExpensesFlow = expenseRepository
        .getTotalByMonth(currentYearMonth)
        .map { it ?: 0.0 }

    private val totalInvestmentsFlow = investmentRepository
        .getTotalCurrentValue()
        .map { it ?: 0.0 }

    private val activeEmiTotalFlow = emiRepository
        .getTotalMonthlyEmi()
        .map { it ?: 0.0 }

    private val totalGoldGramsFlow = goldRepository
        .getTotalGrams()
        .map { it ?: 0.0 }

    private val recentExpensesFlow = expenseRepository.getRecentExpenses(limit = 5)

    private val activeEmisFlow = emiRepository.getAllActiveEmis()

    // ------- combined state -------

    /** Partial state from financial summary flows (first four). */
    private val summaryFlow = combine(
        monthlyExpensesFlow,
        totalInvestmentsFlow,
        activeEmiTotalFlow,
        totalGoldGramsFlow
    ) { monthly, investments, emiTotal, goldGrams ->
        DashboardSummary(
            monthlyExpenses = monthly,
            totalInvestments = investments,
            activeEmiTotal = emiTotal,
            totalGoldGrams = goldGrams
        )
    }

    /** Partial state from list flows (recent expenses + active EMIs). */
    private val listsFlow = combine(
        recentExpensesFlow,
        activeEmisFlow
    ) { recent, activeEmis ->
        val today = LocalDate.now()
        val upcoming = activeEmis.filter { emi ->
            val safeDay = minOf(emi.dueDay, today.lengthOfMonth())
            val dueDate = today.withDayOfMonth(safeDay)
            val daysDiff = ChronoUnit.DAYS.between(today, dueDate)
            daysDiff in 0..7
        }
        DashboardLists(recentExpenses = recent, upcomingEmis = upcoming)
    }

    val dashboardState: StateFlow<DashboardState> = combine(summaryFlow, listsFlow) { summary, lists ->
        DashboardState(
            monthlyExpenses = summary.monthlyExpenses,
            totalInvestments = summary.totalInvestments,
            activeEmiTotal = summary.activeEmiTotal,
            totalGoldGrams = summary.totalGoldGrams,
            recentExpenses = lists.recentExpenses,
            upcomingEmis = lists.upcomingEmis,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = DashboardState(isLoading = true)
    )

    // ------- private helper data classes -------

    private data class DashboardSummary(
        val monthlyExpenses: Double,
        val totalInvestments: Double,
        val activeEmiTotal: Double,
        val totalGoldGrams: Double
    )

    private data class DashboardLists(
        val recentExpenses: List<ExpenseEntity>,
        val upcomingEmis: List<EmiEntity>
    )
}
