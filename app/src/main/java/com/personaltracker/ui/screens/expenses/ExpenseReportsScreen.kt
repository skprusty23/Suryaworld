package com.personaltracker.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.CategoryColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ---- ViewModel ----

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MonthlyTotal(val yearMonth: YearMonth, val total: Double)

data class ExpenseReportsUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthlyTrend: List<MonthlyTotal> = emptyList(),
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val grandTotal: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class ExpenseReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val MONTH_KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExpenseReportsUiState> = _selectedMonth
        .flatMapLatest { month ->
            val monthKey = month.format(MONTH_KEY_FMT)
            combine(
                expenseRepository.getCategoryTotalsByMonth(monthKey),
                expenseRepository.getTotalByMonth(monthKey)
            ) { catTotals, grandTotal ->
                // Build 6-month trend synchronously from selected month
                val trendMonths = (5 downTo 0).map { offset -> month.minusMonths(offset.toLong()) }
                ExpenseReportsUiState(
                    selectedMonth = month,
                    monthlyTrend = trendMonths.map { MonthlyTotal(it, 0.0) },
                    categoryBreakdown = catTotals.map { CategoryTotal(it.category, it.total) },
                    grandTotal = grandTotal ?: 0.0,
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseReportsUiState(isLoading = true))

    fun previousMonth() = _selectedMonth.update { it.minusMonths(1) }
    fun nextMonth() = _selectedMonth.update { it.plusMonths(1) }
    fun selectMonth(ym: YearMonth) { _selectedMonth.value = ym }
}

// ---- Screen ----

private val MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
private val SHORT_MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReportsScreen(
    onBack: () -> Unit,
    onExportPdf: () -> Unit = {},
    viewModel: ExpenseReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Expense Reports",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    IconButton(onClick = { /* share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item {
                MonthSelectorRow(
                    displayMonth = uiState.selectedMonth.format(MONTH_FMT),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            // Grand Total Card
            item {
                TotalSummaryCard(total = uiState.grandTotal)
            }

            // Bar Chart - Monthly Trend
            item {
                MonthlyTrendCard(trend = uiState.monthlyTrend)
            }

            // Category Pie Chart placeholder + breakdown
            item {
                CategoryBreakdownCard(
                    categories = uiState.categoryBreakdown,
                    total = uiState.grandTotal
                )
            }

            // Export Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                    OutlinedButton(
                        onClick = { /* share */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MonthSelectorRow(
    displayMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }
        Text(
            text = displayMonth,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
    }
}

@Composable
private fun TotalSummaryCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Expenses",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatCurrency(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MonthlyTrendCard(trend: List<MonthlyTotal>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Trend (Last 6 Months)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            val maxTotal = trend.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0
            val chartHeight = 120.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 24.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trend.forEachIndexed { index, monthlyTotal ->
                    val fraction = (monthlyTotal.total / maxTotal).coerceIn(0.0, 1.0).toFloat()
                    val barHeight = (chartHeight.value * fraction).coerceAtLeast(4f).dp
                    val color = CategoryColors[index % CategoryColors.size]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (monthlyTotal.total > 0) {
                            Text(
                                text = "₹${(monthlyTotal.total / 1000).toInt()}k",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = monthlyTotal.yearMonth.format(SHORT_MONTH_FMT),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    categories: List<CategoryTotal>,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "No data for this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categories.sortedByDescending { it.total }.forEachIndexed { index, categoryTotal ->
                    val percent = if (total > 0) (categoryTotal.total / total * 100) else 0.0
                    val color = CategoryColors[index % CategoryColors.size]
                    CategoryBreakdownRow(
                        categoryTotal = categoryTotal,
                        percent = percent,
                        color = color
                    )
                    if (index < categories.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    categoryTotal: CategoryTotal,
    percent: Double,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = categoryTotal.category,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "%.1f%%".format(percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = formatCurrency(categoryTotal.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percent / 100).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
