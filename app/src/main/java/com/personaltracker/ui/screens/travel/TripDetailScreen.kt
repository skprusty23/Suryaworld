package com.personaltracker.ui.screens.travel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.dao.TravelCategoryTotal
import com.personaltracker.data.database.entity.TravelExpenseEntity
import com.personaltracker.data.database.entity.TripEntity
import com.personaltracker.domain.repository.TravelRepository
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---- ViewModel ----

data class TripDetailUiState(
    val trip: TripEntity? = null,
    val expenses: List<TravelExpenseEntity> = emptyList(),
    val categoryTotals: List<TravelCategoryTotal> = emptyList(),
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val travelRepository: TravelRepository
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    init {
        loadTrip()
        loadExpenses()
    }

    private fun loadTrip() {
        viewModelScope.launch {
            val trip = travelRepository.getTripById(tripId)
            _uiState.update { it.copy(trip = trip, isLoading = false) }
        }
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            combine(
                travelRepository.getExpensesForTrip(tripId),
                travelRepository.getTotalForTrip(tripId),
                travelRepository.getCategoryTotalsForTrip(tripId)
            ) { expenses, total, categoryTotals ->
                Triple(expenses, total, categoryTotals)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { (expenses, total, categoryTotals) ->
                _uiState.update {
                    it.copy(
                        expenses = expenses,
                        totalSpent = total ?: 0.0,
                        categoryTotals = categoryTotals,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteExpense(expense: TravelExpenseEntity) {
        viewModelScope.launch {
            try {
                travelRepository.deleteTravelExpense(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun markCompleted() {
        viewModelScope.launch {
            try {
                _uiState.value.trip?.let { trip ->
                    travelRepository.updateTrip(trip.copy(isCompleted = true))
                    _uiState.update { it.copy(trip = it.trip?.copy(isCompleted = true)) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

// ---- Category icon/color helpers ----

private val TravelExpenseIcons: Map<String, ImageVector> = mapOf(
    "Flight" to Icons.Default.Flight,
    "Train" to Icons.Default.Train,
    "Bus" to Icons.Default.DirectionsBus,
    "Cab" to Icons.Default.LocalTaxi,
    "Hotel" to Icons.Default.Hotel,
    "Food" to Icons.Default.Restaurant,
    "Miscellaneous" to Icons.Default.MoreHoriz
)

private val TravelExpenseColors: Map<String, Color> = mapOf(
    "Flight" to Color(0xFF1565C0),
    "Train" to Color(0xFF7B1FA2),
    "Bus" to Color(0xFF00897B),
    "Cab" to Color(0xFFE65100),
    "Hotel" to Color(0xFFAD1457),
    "Food" to Color(0xFF558B2F),
    "Miscellaneous" to Color(0xFF6D4C41)
)

private val TravelBlue = Color(0xFF0277BD)
private val TravelCyan = Color(0xFF00BCD4)

// ---- Screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onNavigateToAddExpense: (Long) -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trip = uiState.trip

    Scaffold(
        topBar = {
            PTTopBar(
                title = trip?.destination ?: "Trip Details",
                onBack = onBack,
                actions = {
                    if (trip != null && !trip.isCompleted) {
                        IconButton(onClick = { viewModel.markCompleted() }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Mark completed")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { trip?.let { onNavigateToAddExpense(it.id) } },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expense") },
                containerColor = TravelBlue,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading || trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Trip header
            item { TripHeaderSection(trip = trip, totalSpent = uiState.totalSpent) }

            // Category breakdown
            if (uiState.categoryTotals.isNotEmpty()) {
                item { CategoryBreakdownSection(categoryTotals = uiState.categoryTotals) }
            }

            // Expenses header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Expenses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.expenses.size} entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (uiState.expenses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        EmptyState(
                            icon = Icons.Default.Receipt,
                            message = "No expenses yet",
                            actionLabel = "Add Expense",
                            onAction = { onNavigateToAddExpense(trip.id) }
                        )
                    }
                }
            } else {
                items(uiState.expenses, key = { it.id }) { expense ->
                    TravelExpenseCard(
                        expense = expense,
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripHeaderSection(trip: TripEntity, totalSpent: Double) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val budget = trip.budget
    val progress = if (budget != null && budget > 0) (totalSpent / budget).coerceIn(0.0, 1.0).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(TravelBlue, TravelCyan)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Flight, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Column {
                        Text(trip.destination, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(trip.name, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(
                        text = "${trip.startDate.format(dateFormatter)}${trip.endDate?.let { " – ${it.format(dateFormatter)}" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Spent", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(formatCurrency(totalSpent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (budget != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Budget", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(formatCurrency(budget), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                if (budget != null) {
                    Spacer(Modifier.height(8.dp))
                    val overBudget = totalSpent > budget
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (overBudget) Color(0xFFEF5350) else Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    if (overBudget) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Over budget by ${formatCurrency(totalSpent - budget)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFCDD2)
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatCurrency(budget - totalSpent)} remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownSection(categoryTotals: List<TravelCategoryTotal>) {
    val grandTotal = categoryTotals.sumOf { it.total }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Category Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        categoryTotals.sortedByDescending { it.total }.forEach { ct ->
            val icon = TravelExpenseIcons[ct.category] ?: Icons.Default.MoreHoriz
            val color = TravelExpenseColors[ct.category] ?: Color(0xFF6D4C41)
            val fraction = if (grandTotal > 0) (ct.total / grandTotal).toFloat() else 0f

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                            Text(ct.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(formatCurrency(ct.total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TravelExpenseCard(
    expense: TravelExpenseEntity,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val icon = TravelExpenseIcons[expense.category] ?: Icons.Default.MoreHoriz
    val color = TravelExpenseColors[expense.category] ?: Color(0xFF6D4C41)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Remove this expense?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (!expense.description.isNullOrBlank()) {
                    Text(expense.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(expense.date.format(dateFormatter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• ${expense.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(expense.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
