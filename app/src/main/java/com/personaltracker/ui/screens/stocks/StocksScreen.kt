package com.personaltracker.ui.screens.stocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.StockEntity
import com.personaltracker.domain.repository.StockRepository
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val STOCK_COLOR = Color(0xFF1E88E5)
private val GAIN_COLOR = Color(0xFF43A047)
private val LOSS_COLOR = Color(0xFFE53935)

// ===========================================================================
// STOCKS LIST — State + ViewModel
// ===========================================================================

data class StocksState(
    val stocks: List<StockEntity> = emptyList(),
    val totalInvested: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StocksViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StocksState())
    val state: StateFlow<StocksState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllActive(),
                repository.getTotalInvested()
            ) { stocks, total ->
                StocksState(stocks = stocks, totalInvested = total, isLoading = false)
            }.collect { _state.value = it }
        }
    }

    fun delete(stock: StockEntity) {
        viewModelScope.launch { repository.delete(stock) }
    }
}

// ===========================================================================
// STOCKS LIST — Composable
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocksScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: StocksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var stockToDelete by remember { mutableStateOf<StockEntity?>(null) }

    Scaffold(
        topBar = { PTTopBar(title = "Stocks", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = STOCK_COLOR
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Stock", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = STOCK_COLOR.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Invested",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(state.totalInvested),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = STOCK_COLOR
                        )
                        Text(
                            text = "${state.stocks.size} holding${if (state.stocks.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(STOCK_COLOR.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = STOCK_COLOR,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.stocks.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ShowChart,
                        message = "No stocks yet",
                        actionLabel = "Add Stock",
                        onAction = onNavigateToAdd
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.stocks, key = { it.id }) { stock ->
                            StockCard(
                                stock = stock,
                                onClick = { onNavigateToDetail(stock.id) },
                                onDelete = { stockToDelete = stock }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    stockToDelete?.let { s ->
        ConfirmDeleteDialog(
            title = "Delete Stock",
            message = "Delete \"${s.stockName}\"? This cannot be undone.",
            onConfirm = {
                viewModel.delete(s)
                stockToDelete = null
            },
            onDismiss = { stockToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockCard(
    stock: StockEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(STOCK_COLOR.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = STOCK_COLOR,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name / Symbol / Broker
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stock.stockName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (stock.stockSymbol.isNotBlank()) {
                        Surface(
                            color = STOCK_COLOR.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stock.stockSymbol,
                                style = MaterialTheme.typography.labelSmall,
                                color = STOCK_COLOR,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (stock.brokerName.isNotBlank()) {
                    Text(
                        text = stock.brokerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${stock.unitsPurchased} units @ ${formatCurrency(stock.purchasePricePerUnit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount + menu
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(stock.totalPurchaseAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = STOCK_COLOR
                )
                stock.currentMarketPrice?.let { cmp ->
                    val currentValue = stock.unitsPurchased * cmp
                    val gain = currentValue - stock.totalPurchaseAmount
                    Text(
                        text = (if (gain >= 0) "+" else "") + formatCurrency(gain),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gain >= 0) GAIN_COLOR else LOSS_COLOR
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

// ===========================================================================
// ADD STOCK — State + ViewModel
// ===========================================================================

data class AddStockState(
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class AddStockViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddStockState())
    val state: StateFlow<AddStockState> = _state.asStateFlow()

    fun save(entity: StockEntity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val id = repository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, savedId = id)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}

// ===========================================================================
// ADD STOCK — Composable
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddStockViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.savedId) {
        if (state.savedId != null) onSaved()
    }

    var stockName by remember { mutableStateOf("") }
    var stockSymbol by remember { mutableStateOf("") }
    var brokerName by remember { mutableStateOf("") }
    var dematAccountNumber by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var unitsPurchased by remember { mutableStateOf("") }
    var purchasePricePerUnit by remember { mutableStateOf("") }
    var nominee by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var currentMarketPrice by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var unitsError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val totalPurchaseAmount = remember(unitsPurchased, purchasePricePerUnit) {
        val u = unitsPurchased.toDoubleOrNull() ?: 0.0
        val p = purchasePricePerUnit.toDoubleOrNull() ?: 0.0
        u * p
    }

    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        nameError = stockName.isBlank()
        unitsError = unitsPurchased.isBlank() || unitsPurchased.toDoubleOrNull() == null || unitsPurchased.toDouble() <= 0.0
        priceError = purchasePricePerUnit.isBlank() || purchasePricePerUnit.toDoubleOrNull() == null || purchasePricePerUnit.toDouble() <= 0.0
        return !nameError && !unitsError && !priceError
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Stock", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stock Name
            OutlinedTextField(
                value = stockName,
                onValueChange = { stockName = it; nameError = false },
                label = { Text("Stock Name *") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Stock name is required") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
            )

            // Stock Symbol
            OutlinedTextField(
                value = stockSymbol,
                onValueChange = { stockSymbol = it },
                label = { Text("Stock Symbol (e.g. RELIANCE)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) }
            )

            // Broker Name
            OutlinedTextField(
                value = brokerName,
                onValueChange = { brokerName = it },
                label = { Text("Broker Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            // Demat Account Number
            OutlinedTextField(
                value = dematAccountNumber,
                onValueChange = { dematAccountNumber = it },
                label = { Text("Demat Account Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
            )

            // Purchase Date
            OutlinedTextField(
                value = purchaseDate.format(DATE_FMT),
                onValueChange = {},
                readOnly = true,
                label = { Text("Purchase Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Purchase Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // Units Purchased
            OutlinedTextField(
                value = unitsPurchased,
                onValueChange = { unitsPurchased = it; unitsError = false },
                label = { Text("Units Purchased *") },
                isError = unitsError,
                supportingText = if (unitsError) { { Text("Enter valid units") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) }
            )

            // Purchase Price Per Unit
            OutlinedTextField(
                value = purchasePricePerUnit,
                onValueChange = { purchasePricePerUnit = it; priceError = false },
                label = { Text("Purchase Price Per Unit *") },
                isError = priceError,
                supportingText = if (priceError) { { Text("Enter valid price") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Total Purchase Amount (read-only, auto-calculated)
            OutlinedTextField(
                value = if (totalPurchaseAmount > 0.0) formatCurrency(totalPurchaseAmount) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Total Purchase Amount (auto)") },
                leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = false
            )

            // Current Market Price (optional)
            OutlinedTextField(
                value = currentMarketPrice,
                onValueChange = { currentMarketPrice = it },
                label = { Text("Current Market Price (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Nominee
            OutlinedTextField(
                value = nominee,
                onValueChange = { nominee = it },
                label = { Text("Nominee (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (validate()) {
                        viewModel.save(
                            StockEntity(
                                stockName = stockName.trim(),
                                stockSymbol = stockSymbol.trim(),
                                brokerName = brokerName.trim(),
                                dematAccountNumber = dematAccountNumber.trim(),
                                purchaseDate = purchaseDate,
                                unitsPurchased = unitsPurchased.toDouble(),
                                purchasePricePerUnit = purchasePricePerUnit.toDouble(),
                                totalPurchaseAmount = totalPurchaseAmount,
                                nominee = nominee.trim(),
                                notes = notes.trim(),
                                currentMarketPrice = currentMarketPrice.toDoubleOrNull()
                            )
                        )
                    }
                },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Stock", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Purchase Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = purchaseDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        purchaseDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ===========================================================================
// STOCK DETAIL — State + ViewModel
// ===========================================================================

data class StockDetailState(
    val stock: StockEntity? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val repository: StockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stockId: Long = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(StockDetailState())
    val state: StateFlow<StockDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stock = repository.getById(stockId)
            _state.value = StockDetailState(stock = stock, isLoading = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            _state.value.stock?.let {
                repository.delete(it)
                _state.value = _state.value.copy(deleted = true)
            }
        }
    }
}

// ===========================================================================
// STOCK DETAIL — Composable
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Stock Details",
                onBack = onBack,
                actions = {
                    state.stock?.let { s ->
                        IconButton(onClick = { onEdit(s.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            state.stock == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Stock not found.") }
            }
            else -> {
                StockDetailContent(
                    stock = state.stock!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Stock",
            message = "Delete \"${state.stock?.stockName}\"? This cannot be undone.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun StockDetailContent(
    stock: StockEntity,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val today = LocalDate.now()

    // Calculated fields
    val currentPortfolioValue: Double? = stock.currentMarketPrice?.let { cmp ->
        stock.unitsPurchased * cmp
    }
    val gainLossAmount: Double? = currentPortfolioValue?.let { it - stock.totalPurchaseAmount }
    val gainLossPercent: Double? = gainLossAmount?.let { gain ->
        if (stock.totalPurchaseAmount > 0.0) (gain / stock.totalPurchaseAmount) * 100.0 else 0.0
    }

    val totalDays = ChronoUnit.DAYS.between(stock.purchaseDate, today)
    val holdingYears = totalDays / 365
    val holdingMonths = (totalDays % 365) / 30
    val holdingPeriod = buildString {
        if (holdingYears > 0) append("${holdingYears} year${if (holdingYears != 1L) "s" else ""} ")
        append("${holdingMonths} month${if (holdingMonths != 1L) "s" else ""}")
    }.trim()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = STOCK_COLOR.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(STOCK_COLOR.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = STOCK_COLOR,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stock.stockName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (stock.stockSymbol.isNotBlank()) {
                    Surface(
                        color = STOCK_COLOR.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stock.stockSymbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = STOCK_COLOR,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (stock.brokerName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stock.brokerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Investment summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StockFinancialCard(
                label = "Invested",
                value = formatCurrency(stock.totalPurchaseAmount),
                color = STOCK_COLOR,
                modifier = Modifier.weight(1f)
            )
            if (currentPortfolioValue != null) {
                StockFinancialCard(
                    label = "Current Value",
                    value = formatCurrency(currentPortfolioValue),
                    color = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Gain / Loss card
        if (gainLossAmount != null && gainLossPercent != null) {
            val isGain = gainLossAmount >= 0
            val gainColor = if (isGain) GAIN_COLOR else LOSS_COLOR
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = gainColor.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Gain / Loss",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = (if (isGain) "+" else "") + formatCurrency(gainLossAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = gainColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Return",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = (if (isGain) "+" else "") + "%.2f%%".format(gainLossPercent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = gainColor
                        )
                    }
                    Icon(
                        imageVector = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = gainColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Holding period card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Holding Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = holdingPeriod,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Details card
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                StockDetailRow("Purchase Date", stock.purchaseDate.format(DATE_FMT))
                StockDetailRow("Units Purchased", stock.unitsPurchased.toString())
                StockDetailRow("Purchase Price / Unit", formatCurrency(stock.purchasePricePerUnit))
                StockDetailRow("Total Purchase Amount", formatCurrency(stock.totalPurchaseAmount))

                stock.currentMarketPrice?.let {
                    StockDetailRow("Current Market Price", formatCurrency(it))
                }
                if (currentPortfolioValue != null) {
                    StockDetailRow("Portfolio Value", formatCurrency(currentPortfolioValue))
                }
                if (stock.dematAccountNumber.isNotBlank()) {
                    StockDetailRow("Demat Account", stock.dematAccountNumber)
                }
                if (stock.nominee.isNotBlank()) {
                    StockDetailRow("Nominee", stock.nominee)
                }
                if (stock.notes.isNotBlank()) {
                    StockDetailRow("Notes", stock.notes)
                }
                StockDetailRow("Status", if (stock.isActive) "Active" else "Inactive")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StockFinancialCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun StockDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
