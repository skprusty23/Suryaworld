package com.personaltracker.ui.screens.expenses

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.CategoryColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ── ViewModel ─────────────────────────────────────────────────────────────────

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.repository.ExpenseRepository
import com.personaltracker.reports.ExpenseReportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MonthlyTotal(val yearMonth: YearMonth, val total: Double)

data class ExpenseReportsUiState(
    val selectedMonth: YearMonth      = YearMonth.now(),
    val monthlyTrend: List<MonthlyTotal> = emptyList(),
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val grandTotal: Double            = 0.0,
    val isLoading: Boolean            = false,
    val exportFile: File?             = null,
    val exportError: String?          = null,
    val isExporting: Boolean          = false
)

@HiltViewModel
class ExpenseReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val reportManager: ExpenseReportManager
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val MONTH_KEY_FMT  = DateTimeFormatter.ofPattern("yyyy-MM")

    private val _exportFile   = MutableStateFlow<File?>(null)
    private val _exportError  = MutableStateFlow<String?>(null)
    private val _isExporting  = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExpenseReportsUiState> = _selectedMonth
        .flatMapLatest { month ->
            val key = month.format(MONTH_KEY_FMT)
            combine(
                expenseRepository.getCategoryTotalsByMonth(key),
                expenseRepository.getTotalByMonth(key),
                _exportFile,
                _exportError,
                _isExporting
            ) { catTotals, grandTotal, exportFile, exportError, isExporting ->
                val trendMonths = (5 downTo 0).map { offset -> month.minusMonths(offset.toLong()) }
                ExpenseReportsUiState(
                    selectedMonth     = month,
                    monthlyTrend      = trendMonths.map { MonthlyTotal(it, 0.0) },
                    categoryBreakdown = catTotals.map { CategoryTotal(it.category, it.total) },
                    grandTotal        = grandTotal ?: 0.0,
                    isLoading         = false,
                    exportFile        = exportFile,
                    exportError       = exportError,
                    isExporting       = isExporting
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExpenseReportsUiState(isLoading = true)
        )

    fun previousMonth() = _selectedMonth.update { it.minusMonths(1) }
    fun nextMonth()     = _selectedMonth.update { it.plusMonths(1) }
    fun selectMonth(ym: YearMonth) { _selectedMonth.value = ym }

    fun exportAsCsv() {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            _exportFile.value  = null
            _exportError.value = null
            try {
                val month      = _selectedMonth.value
                val monthKey   = month.format(MONTH_KEY_FMT)
                val expenses   = expenseRepository.getExpensesByMonthOnce(monthKey)
                val catTotals  = expenseRepository.getCategoryTotalsByMonthOnce(monthKey)
                val file = reportManager.exportCsv(
                    expenses      = expenses,
                    categoryTotals = catTotals,
                    reportTitle   = "Monthly Report",
                    yearMonth     = monthKey
                )
                _exportFile.value = file
            } catch (e: Exception) {
                _exportError.value = "CSV export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAsPdf() {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            _exportFile.value  = null
            _exportError.value = null
            try {
                val month      = _selectedMonth.value
                val monthKey   = month.format(MONTH_KEY_FMT)
                val expenses   = expenseRepository.getExpensesByMonthOnce(monthKey)
                val catTotals  = expenseRepository.getCategoryTotalsByMonthOnce(monthKey)
                val file = reportManager.exportPdf(
                    expenses       = expenses,
                    categoryTotals = catTotals,
                    reportTitle    = "Monthly Report",
                    yearMonth      = monthKey
                )
                _exportFile.value = file
            } catch (e: Exception) {
                _exportError.value = "PDF export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportState() {
        _exportFile.value  = null
        _exportError.value = null
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val MONTH_FMT: DateTimeFormatter       = DateTimeFormatter.ofPattern("MMM yyyy")
private val SHORT_MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReportsScreen(
    onBack: () -> Unit,
    viewModel: ExpenseReportsViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val context        = LocalContext.current
    val snackbarHost   = remember { SnackbarHostState() }

    // Share the exported file when it becomes available
    LaunchedEffect(uiState.exportFile) {
        val file = uiState.exportFile ?: return@LaunchedEffect
        try {
            val uri   = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime  = if (file.extension == "pdf") "application/pdf" else "text/csv"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SuryaWorld Expense Report — ${file.nameWithoutExtension}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            snackbarHost.showSnackbar("Export saved: ${file.name}")
        }
        viewModel.clearExportState()
    }

    LaunchedEffect(uiState.exportError) {
        val err = uiState.exportError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(err)
        viewModel.clearExportState()
    }

    Scaffold(
        topBar = {
            PTTopBar(title = "Expense Reports", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Month selector
            item {
                MonthSelectorRow(
                    displayMonth = uiState.selectedMonth.format(MONTH_FMT),
                    onPrevious   = viewModel::previousMonth,
                    onNext       = viewModel::nextMonth
                )
            }

            // Grand total card
            item { TotalSummaryCard(total = uiState.grandTotal) }

            // Bar chart — monthly trend
            item { MonthlyTrendCard(trend = uiState.monthlyTrend) }

            // Category breakdown
            item {
                CategoryBreakdownCard(
                    categories = uiState.categoryBreakdown,
                    total      = uiState.grandTotal
                )
            }

            // Export buttons
            item {
                ExportButtonsRow(
                    isExporting = uiState.isExporting,
                    onExportPdf = viewModel::exportAsPdf,
                    onExportCsv = viewModel::exportAsCsv
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun ExportButtonsRow(
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Download Report",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick  = onExportPdf,
                enabled  = !isExporting,
                modifier = Modifier.weight(1f)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("PDF Report")
            }
            OutlinedButton(
                onClick  = onExportCsv,
                enabled  = !isExporting,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.TableChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("CSV / Excel")
            }
        }
        Text(
            "Reports are saved locally. Use the Share sheet to open in any app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthSelectorRow(
    displayMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }
        Text(
            text       = displayMonth,
            style      = MaterialTheme.typography.titleMedium,
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
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Expenses",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = formatCurrency(total),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MonthlyTrendCard(trend: List<MonthlyTotal>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Monthly Trend (Last 6 Months)",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            val maxTotal  = trend.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0
            val chartH    = 120.dp

            Row(
                modifier              = Modifier.fillMaxWidth().height(chartH + 24.dp),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trend.forEachIndexed { index, mt ->
                    val fraction  = (mt.total / maxTotal).coerceIn(0.0, 1.0).toFloat()
                    val barHeight = (chartH.value * fraction).coerceAtLeast(4f).dp
                    val color     = CategoryColors[index % CategoryColors.size]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier            = Modifier.weight(1f)
                    ) {
                        if (mt.total > 0) {
                            Text(
                                "₹${(mt.total / 1000).toInt()}k",
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
                            mt.yearMonth.format(SHORT_MONTH_FMT),
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
private fun CategoryBreakdownCard(categories: List<CategoryTotal>, total: Double) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Category Breakdown",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (categories.isEmpty()) {
                Text(
                    "No expenses recorded for this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categories.sortedByDescending { it.total }.forEachIndexed { index, ct ->
                    val percent = if (total > 0) ct.total / total * 100 else 0.0
                    val color   = CategoryColors[index % CategoryColors.size]
                    CategoryBreakdownRow(
                        categoryTotal = ct,
                        percent       = percent,
                        color         = color
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
            modifier          = Modifier.fillMaxWidth(),
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
                categoryTotal.category,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "%.1f%%".format(percent),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                formatCurrency(categoryTotal.total),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress    = { (percent / 100).toFloat().coerceIn(0f, 1f) },
            modifier    = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color       = color,
            trackColor  = color.copy(alpha = 0.15f)
        )
    }
}
