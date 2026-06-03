package com.personaltracker.ui.screens.mutualfund

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.personaltracker.data.database.entity.MfInvestmentType
import com.personaltracker.data.database.entity.MutualFundEntity
import com.personaltracker.data.database.entity.SipFrequency
import com.personaltracker.domain.repository.MutualFundRepository
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ─────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

private val MF_GREEN = Color(0xFF1B8A5A)
private val MF_BLUE = Color(0xFF1565C0)
private val MF_ORANGE = Color(0xFFE65100)

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localDateToMillis(initialDate)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onDateSelected(millisToLocalDate(it)) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) { DatePicker(state = state) }
}

// ─────────────────────────────────────────────────────────
// MutualFundsViewModel
// ─────────────────────────────────────────────────────────

data class MutualFundsState(
    val funds: List<MutualFundEntity> = emptyList(),
    val filteredFunds: List<MutualFundEntity> = emptyList(),
    val selectedFilter: String = "All",   // "All" | "SIP" | "LUMPSUM"
    val totalSip: Double = 0.0,
    val totalLumpsum: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class MutualFundsViewModel @Inject constructor(
    private val repository: MutualFundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MutualFundsState())
    val state: StateFlow<MutualFundsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAll(),
                repository.getTotalSipAmount(),
                repository.getTotalLumpsumAmount()
            ) { funds, sip, lumpsum ->
                val filter = _state.value.selectedFilter
                Triple(funds, sip, lumpsum) to filter
            }.collect { (triple, filter) ->
                val (funds, sip, lumpsum) = triple
                _state.value = _state.value.copy(
                    funds = funds,
                    filteredFunds = applyFilter(funds, filter),
                    totalSip = sip,
                    totalLumpsum = lumpsum,
                    isLoading = false
                )
            }
        }
    }

    private fun applyFilter(funds: List<MutualFundEntity>, filter: String): List<MutualFundEntity> =
        when (filter) {
            "SIP" -> funds.filter { it.investmentType == MfInvestmentType.SIP }
            "LUMPSUM" -> funds.filter { it.investmentType == MfInvestmentType.LUMPSUM }
            else -> funds
        }

    fun setFilter(filter: String) {
        _state.value = _state.value.copy(
            selectedFilter = filter,
            filteredFunds = applyFilter(_state.value.funds, filter)
        )
    }

    fun delete(fund: MutualFundEntity) {
        viewModelScope.launch { repository.delete(fund) }
    }
}

// ─────────────────────────────────────────────────────────
// MutualFundsScreen
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutualFundsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: MutualFundsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var fundToDelete by remember { mutableStateOf<MutualFundEntity?>(null) }

    Scaffold(
        topBar = { PTTopBar(title = "Mutual Funds", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MF_GREEN
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Fund", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MfSummaryCard(
                    label = "Monthly SIP",
                    value = formatCurrency(state.totalSip),
                    color = MF_GREEN,
                    icon = Icons.Default.Repeat,
                    modifier = Modifier.weight(1f)
                )
                MfSummaryCard(
                    label = "Lumpsum",
                    value = formatCurrency(state.totalLumpsum),
                    color = MF_BLUE,
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
                MfSummaryCard(
                    label = "Total Funds",
                    value = state.funds.size.toString(),
                    color = MF_ORANGE,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("All", "SIP", "LUMPSUM")) { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter) },
                        leadingIcon = when (filter) {
                            "SIP" -> { { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                            "LUMPSUM" -> { { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                            else -> null
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.filteredFunds.isEmpty() -> MfEmptyState(onAdd = onNavigateToAdd)

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredFunds, key = { it.id }) { fund ->
                        MutualFundCard(
                            fund = fund,
                            onClick = { onNavigateToDetail(fund.id) },
                            onDelete = { fundToDelete = fund }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    fundToDelete?.let { fund ->
        ConfirmDeleteDialog(
            title = "Delete Fund",
            message = "Delete \"${fund.portfolioName}\"? This cannot be undone.",
            onConfirm = {
                viewModel.delete(fund)
                fundToDelete = null
            },
            onDismiss = { fundToDelete = null }
        )
    }
}

@Composable
private fun MfSummaryCard(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MutualFundCard(
    fund: MutualFundEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val typeColor = if (fund.investmentType == MfInvestmentType.SIP) MF_GREEN else MF_BLUE
    val amount = if (fund.investmentType == MfInvestmentType.SIP)
        fund.sipAmount ?: 0.0
    else
        fund.investmentAmount ?: 0.0

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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (fund.investmentType == MfInvestmentType.SIP)
                        Icons.Default.Repeat else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fund.portfolioName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (fund.fundName.isNotBlank()) {
                    Text(
                        text = fund.fundName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (fund.amcName.isNotBlank()) {
                    Text(
                        text = fund.amcName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = typeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = fund.investmentType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (fund.investmentType == MfInvestmentType.SIP) {
                        Text(
                            text = fund.sipFrequency.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                if (fund.investmentType == MfInvestmentType.SIP) {
                    Text(
                        text = "/instalment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                fund.currentValue?.let { cv ->
                    Text(
                        text = "CV: ${formatCurrency(cv)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00897B)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MfEmptyState(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No mutual funds yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Fund")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// AddMutualFundViewModel
// ─────────────────────────────────────────────────────────

data class AddMutualFundState(
    val saveResult: Boolean? = null
)

@HiltViewModel
class AddMutualFundViewModel @Inject constructor(
    private val repository: MutualFundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddMutualFundState())
    val state: StateFlow<AddMutualFundState> = _state.asStateFlow()

    fun save(entity: MutualFundEntity) {
        viewModelScope.launch {
            repository.insert(entity)
            _state.value = _state.value.copy(saveResult = true)
        }
    }
}

// ─────────────────────────────────────────────────────────
// AddMutualFundScreen
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMutualFundScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddMutualFundViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saveResult) {
        if (state.saveResult == true) onSaved()
    }

    // Basic fields
    var portfolioName by remember { mutableStateOf("") }
    var amcName by remember { mutableStateOf("") }
    var fundName by remember { mutableStateOf("") }
    var folioNumber by remember { mutableStateOf("") }
    var investmentType by remember { mutableStateOf(MfInvestmentType.SIP) }

    // SIP fields
    var sipStartDate by remember { mutableStateOf(LocalDate.now()) }
    var sipAmount by remember { mutableStateOf("") }
    var sipFrequency by remember { mutableStateOf(SipFrequency.MONTHLY) }

    // Lumpsum fields
    var investmentDate by remember { mutableStateOf(LocalDate.now()) }
    var investmentAmount by remember { mutableStateOf("") }

    // Common
    var purpose by remember { mutableStateOf("") }
    var nominee by remember { mutableStateOf("") }
    var autoPay by remember { mutableStateOf(false) }
    var linkedBankName by remember { mutableStateOf("") }
    var linkedAccountNumber by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var returnsPercent by remember { mutableStateOf("") }

    // Validation
    var portfolioError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    // Date picker visibility
    var showSipStartDatePicker by remember { mutableStateOf(false) }
    var showInvestmentDatePicker by remember { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        portfolioError = portfolioName.isBlank()
        amountError = when (investmentType) {
            MfInvestmentType.SIP -> sipAmount.isBlank() || sipAmount.toDoubleOrNull() == null || sipAmount.toDouble() <= 0
            MfInvestmentType.LUMPSUM -> investmentAmount.isBlank() || investmentAmount.toDoubleOrNull() == null || investmentAmount.toDouble() <= 0
        }
        return !portfolioError && !amountError
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Mutual Fund", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Basic Info ──────────────────────────────
            MfSectionLabel("Fund Information")

            OutlinedTextField(
                value = portfolioName,
                onValueChange = { portfolioName = it; portfolioError = false },
                label = { Text("Portfolio Name *") },
                isError = portfolioError,
                supportingText = if (portfolioError) { { Text("Portfolio name is required") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
            )

            OutlinedTextField(
                value = amcName,
                onValueChange = { amcName = it },
                label = { Text("AMC Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            OutlinedTextField(
                value = fundName,
                onValueChange = { fundName = it },
                label = { Text("Fund Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
            )

            OutlinedTextField(
                value = folioNumber,
                onValueChange = { folioNumber = it },
                label = { Text("Folio Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) }
            )

            // ── Investment Type ─────────────────────────
            MfSectionLabel("Investment Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MfInvestmentType.values().forEach { type ->
                    FilterChip(
                        selected = investmentType == type,
                        onClick = { investmentType = type; amountError = false },
                        label = { Text(type.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (type == MfInvestmentType.SIP) Icons.Default.Repeat else Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // ── SIP Fields ──────────────────────────────
            if (investmentType == MfInvestmentType.SIP) {
                MfSectionLabel("SIP Details")

                // SIP Start Date
                OutlinedTextField(
                    value = sipStartDate.format(DATE_FMT),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("SIP Start Date") },
                    trailingIcon = {
                        IconButton(onClick = { showSipStartDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick SIP Start Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSipStartDatePicker = true }
                )

                // SIP Amount
                OutlinedTextField(
                    value = sipAmount,
                    onValueChange = { sipAmount = it; amountError = false },
                    label = { Text("SIP Amount *") },
                    isError = amountError,
                    supportingText = if (amountError) { { Text("Enter a valid SIP amount") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // SIP Frequency
                ExposedDropdownMenuBox(
                    expanded = showFrequencyDropdown,
                    onExpandedChange = { showFrequencyDropdown = it }
                ) {
                    OutlinedTextField(
                        value = sipFrequency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SIP Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFrequencyDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showFrequencyDropdown,
                        onDismissRequest = { showFrequencyDropdown = false }
                    ) {
                        SipFrequency.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.name) },
                                onClick = {
                                    sipFrequency = freq
                                    showFrequencyDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Lumpsum Fields ──────────────────────────
            if (investmentType == MfInvestmentType.LUMPSUM) {
                MfSectionLabel("Lumpsum Details")

                // Investment Date
                OutlinedTextField(
                    value = investmentDate.format(DATE_FMT),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Investment Date") },
                    trailingIcon = {
                        IconButton(onClick = { showInvestmentDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Investment Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInvestmentDatePicker = true }
                )

                // Lumpsum Amount
                OutlinedTextField(
                    value = investmentAmount,
                    onValueChange = { investmentAmount = it; amountError = false },
                    label = { Text("Investment Amount *") },
                    isError = amountError,
                    supportingText = if (amountError) { { Text("Enter a valid investment amount") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // ── Additional Info ─────────────────────────
            MfSectionLabel("Additional Info")

            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Purpose / Goal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
            )

            OutlinedTextField(
                value = nominee,
                onValueChange = { nominee = it },
                label = { Text("Nominee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            // ── Auto Pay ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Auto Pay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Linked to bank account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoPay, onCheckedChange = { autoPay = it })
            }

            if (autoPay) {
                OutlinedTextField(
                    value = linkedBankName,
                    onValueChange = { linkedBankName = it },
                    label = { Text("Linked Bank Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                )
                OutlinedTextField(
                    value = linkedAccountNumber,
                    onValueChange = { linkedAccountNumber = it },
                    label = { Text("Linked Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                )
            }

            // ── Optional Tracking ───────────────────────
            MfSectionLabel("Tracking (Optional)")

            OutlinedTextField(
                value = currentValue,
                onValueChange = { currentValue = it },
                label = { Text("Current Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = returnsPercent,
                onValueChange = { returnsPercent = it },
                label = { Text("Returns %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text("%") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (validate()) {
                        viewModel.save(
                            MutualFundEntity(
                                portfolioName = portfolioName.trim(),
                                amcName = amcName.trim(),
                                fundName = fundName.trim(),
                                folioNumber = folioNumber.trim(),
                                investmentType = investmentType,
                                sipStartDate = if (investmentType == MfInvestmentType.SIP) sipStartDate else null,
                                sipAmount = if (investmentType == MfInvestmentType.SIP) sipAmount.toDoubleOrNull() else null,
                                sipFrequency = sipFrequency,
                                investmentDate = if (investmentType == MfInvestmentType.LUMPSUM) investmentDate else null,
                                investmentAmount = if (investmentType == MfInvestmentType.LUMPSUM) investmentAmount.toDoubleOrNull() else null,
                                purpose = purpose.trim(),
                                nominee = nominee.trim(),
                                autoPay = autoPay,
                                linkedBankName = if (autoPay) linkedBankName.trim() else "",
                                linkedAccountNumber = if (autoPay) linkedAccountNumber.trim() else "",
                                currentValue = currentValue.toDoubleOrNull(),
                                returnsPercent = returnsPercent.toDoubleOrNull()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Fund", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSipStartDatePicker) {
        SimpleDatePickerDialog(
            initialDate = sipStartDate,
            onDateSelected = { sipStartDate = it },
            onDismiss = { showSipStartDatePicker = false }
        )
    }

    if (showInvestmentDatePicker) {
        SimpleDatePickerDialog(
            initialDate = investmentDate,
            onDateSelected = { investmentDate = it },
            onDismiss = { showInvestmentDatePicker = false }
        )
    }
}

@Composable
private fun MfSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

// ─────────────────────────────────────────────────────────
// MutualFundDetailViewModel
// ─────────────────────────────────────────────────────────

data class MutualFundDetailState(
    val fund: MutualFundEntity? = null,
    val deleted: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class MutualFundDetailViewModel @Inject constructor(
    private val repository: MutualFundRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val fundId: Long = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(MutualFundDetailState())
    val state: StateFlow<MutualFundDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val fund = repository.getById(fundId)
            _state.value = _state.value.copy(fund = fund, isLoading = false)
        }
    }

    fun update(entity: MutualFundEntity) {
        viewModelScope.launch {
            repository.update(entity)
            _state.value = _state.value.copy(fund = entity)
        }
    }

    fun delete() {
        viewModelScope.launch {
            _state.value.fund?.let {
                repository.delete(it)
                _state.value = _state.value.copy(deleted = true)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// SIP Calculation helpers
// ─────────────────────────────────────────────────────────

private fun installmentsPaid(startDate: LocalDate, frequency: SipFrequency): Int {
    val today = LocalDate.now()
    if (!today.isAfter(startDate)) return 0
    val monthsBetween = ChronoUnit.MONTHS.between(startDate, today).toInt().coerceAtLeast(0)
    return when (frequency) {
        SipFrequency.MONTHLY -> monthsBetween + 1
        SipFrequency.QUARTERLY -> (monthsBetween / 3) + 1
        SipFrequency.YEARLY -> (monthsBetween / 12) + 1
    }
}

private fun investmentAgeYears(startDate: LocalDate): Double {
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(0)
    return days / 365.25
}

// ─────────────────────────────────────────────────────────
// MutualFundDetailScreen
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutualFundDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: MutualFundDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Fund Details",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.fund == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Fund not found") }

            else -> MutualFundDetailContent(
                fund = state.fund!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Fund",
            message = "Delete \"${state.fund?.portfolioName}\"? This cannot be undone.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showEditDialog && state.fund != null) {
        MutualFundEditDialog(
            fund = state.fund!!,
            onSave = { updated ->
                viewModel.update(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun MutualFundDetailContent(
    fund: MutualFundEntity,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val typeColor = if (fund.investmentType == MfInvestmentType.SIP) MF_GREEN else MF_BLUE

    // SIP calculations
    val instalmentsPaid = if (fund.investmentType == MfInvestmentType.SIP && fund.sipStartDate != null)
        installmentsPaid(fund.sipStartDate, fund.sipFrequency) else 0
    val totalInvested = when (fund.investmentType) {
        MfInvestmentType.SIP -> (fund.sipAmount ?: 0.0) * instalmentsPaid
        MfInvestmentType.LUMPSUM -> fund.investmentAmount ?: 0.0
    }
    val ageYears = if (fund.investmentType == MfInvestmentType.SIP && fund.sipStartDate != null)
        investmentAgeYears(fund.sipStartDate)
    else if (fund.investmentType == MfInvestmentType.LUMPSUM && fund.investmentDate != null)
        investmentAgeYears(fund.investmentDate)
    else 0.0

    val averagePerYear = if (ageYears > 0) totalInvested / ageYears else totalInvested

    val gainLoss = fund.currentValue?.let { it - totalInvested }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f))
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
                        .background(typeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (fund.investmentType == MfInvestmentType.SIP)
                            Icons.Default.Repeat else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = fund.portfolioName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (fund.fundName.isNotBlank()) {
                    Text(
                        text = fund.fundName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = typeColor
                    )
                }
                if (fund.amcName.isNotBlank()) {
                    Text(
                        text = fund.amcName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = typeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = fund.investmentType.name +
                            if (fund.investmentType == MfInvestmentType.SIP) " · ${fund.sipFrequency.name}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Investment Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MfDetailCard(
                label = "Total Invested",
                value = formatCurrency(totalInvested),
                color = typeColor,
                modifier = Modifier.weight(1f)
            )
            fund.currentValue?.let { cv ->
                MfDetailCard(
                    label = "Current Value",
                    value = formatCurrency(cv),
                    color = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // SIP-specific stats
        if (fund.investmentType == MfInvestmentType.SIP) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MfDetailCard(
                    label = "Instalments Paid",
                    value = instalmentsPaid.toString(),
                    color = MF_ORANGE,
                    modifier = Modifier.weight(1f)
                )
                MfDetailCard(
                    label = "Avg / Year",
                    value = formatCurrency(averagePerYear),
                    color = MF_BLUE,
                    modifier = Modifier.weight(1f)
                )
                MfDetailCard(
                    label = "Age (Yrs)",
                    value = "%.1f".format(ageYears),
                    color = Color(0xFF6D4C41),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Lumpsum age
        if (fund.investmentType == MfInvestmentType.LUMPSUM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MfDetailCard(
                    label = "Investment Age",
                    value = "%.1f yrs".format(ageYears),
                    color = Color(0xFF6D4C41),
                    modifier = Modifier.weight(1f)
                )
                MfDetailCard(
                    label = "Avg / Year",
                    value = formatCurrency(averagePerYear),
                    color = MF_BLUE,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Gain/Loss card
        gainLoss?.let { gl ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (gl >= 0) Color(0xFF43A047).copy(alpha = 0.08f)
                    else Color(0xFFE53935).copy(alpha = 0.08f)
                )
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
                            text = "Absolute Gain / Loss",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = (if (gl >= 0) "+" else "") + formatCurrency(gl),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (gl >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                        )
                    }
                    Icon(
                        imageVector = if (gl >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (gl >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Details card
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (fund.folioNumber.isNotBlank()) MfDetailRow("Folio Number", fund.folioNumber)

                if (fund.investmentType == MfInvestmentType.SIP) {
                    fund.sipStartDate?.let { MfDetailRow("SIP Start Date", it.format(DATE_FMT)) }
                    fund.sipAmount?.let { MfDetailRow("SIP Amount", formatCurrency(it)) }
                    MfDetailRow("Frequency", fund.sipFrequency.name)
                } else {
                    fund.investmentDate?.let { MfDetailRow("Investment Date", it.format(DATE_FMT)) }
                    fund.investmentAmount?.let { MfDetailRow("Amount", formatCurrency(it)) }
                }

                if (fund.purpose.isNotBlank()) MfDetailRow("Purpose", fund.purpose)
                if (fund.nominee.isNotBlank()) MfDetailRow("Nominee", fund.nominee)
                MfDetailRow("Auto Pay", if (fund.autoPay) "Yes" else "No")
                if (fund.autoPay) {
                    if (fund.linkedBankName.isNotBlank()) MfDetailRow("Bank", fund.linkedBankName)
                    if (fund.linkedAccountNumber.isNotBlank()) MfDetailRow("Account No.", fund.linkedAccountNumber)
                }
                fund.returnsPercent?.let { MfDetailRow("Returns %", "%.2f%%".format(it)) }
                MfDetailRow("Status", if (fund.isActive) "Active" else "Inactive")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MfDetailCard(
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
            modifier = Modifier.padding(10.dp),
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
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MfDetailRow(label: String, value: String) {
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

// ─────────────────────────────────────────────────────────
// Inline Edit Dialog
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MutualFundEditDialog(
    fund: MutualFundEntity,
    onSave: (MutualFundEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var portfolioName by remember { mutableStateOf(fund.portfolioName) }
    var amcName by remember { mutableStateOf(fund.amcName) }
    var fundName by remember { mutableStateOf(fund.fundName) }
    var folioNumber by remember { mutableStateOf(fund.folioNumber) }
    var purpose by remember { mutableStateOf(fund.purpose) }
    var nominee by remember { mutableStateOf(fund.nominee) }
    var autoPay by remember { mutableStateOf(fund.autoPay) }
    var linkedBankName by remember { mutableStateOf(fund.linkedBankName) }
    var linkedAccountNumber by remember { mutableStateOf(fund.linkedAccountNumber) }
    var currentValue by remember { mutableStateOf(fund.currentValue?.toString() ?: "") }
    var returnsPercent by remember { mutableStateOf(fund.returnsPercent?.toString() ?: "") }
    var sipAmount by remember { mutableStateOf(fund.sipAmount?.toString() ?: "") }
    var investmentAmount by remember { mutableStateOf(fund.investmentAmount?.toString() ?: "") }
    var sipStartDate by remember { mutableStateOf(fund.sipStartDate ?: LocalDate.now()) }
    var investmentDate by remember { mutableStateOf(fund.investmentDate ?: LocalDate.now()) }
    var sipFrequency by remember { mutableStateOf(fund.sipFrequency) }
    var showSipDatePicker by remember { mutableStateOf(false) }
    var showInvestmentDatePicker by remember { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Fund") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = portfolioName,
                    onValueChange = { portfolioName = it },
                    label = { Text("Portfolio Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amcName,
                    onValueChange = { amcName = it },
                    label = { Text("AMC Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fundName,
                    onValueChange = { fundName = it },
                    label = { Text("Fund Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = folioNumber,
                    onValueChange = { folioNumber = it },
                    label = { Text("Folio Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (fund.investmentType == MfInvestmentType.SIP) {
                    OutlinedTextField(
                        value = sipStartDate.format(DATE_FMT),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SIP Start Date") },
                        trailingIcon = {
                            IconButton(onClick = { showSipDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSipDatePicker = true }
                    )
                    OutlinedTextField(
                        value = sipAmount,
                        onValueChange = { sipAmount = it },
                        label = { Text("SIP Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = showFrequencyDropdown,
                        onExpandedChange = { showFrequencyDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = sipFrequency.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFrequencyDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showFrequencyDropdown,
                            onDismissRequest = { showFrequencyDropdown = false }
                        ) {
                            SipFrequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.name) },
                                    onClick = { sipFrequency = freq; showFrequencyDropdown = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = investmentDate.format(DATE_FMT),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Investment Date") },
                        trailingIcon = {
                            IconButton(onClick = { showInvestmentDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInvestmentDatePicker = true }
                    )
                    OutlinedTextField(
                        value = investmentAmount,
                        onValueChange = { investmentAmount = it },
                        label = { Text("Investment Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nominee,
                    onValueChange = { nominee = it },
                    label = { Text("Nominee") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto Pay", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoPay, onCheckedChange = { autoPay = it })
                }
                if (autoPay) {
                    OutlinedTextField(
                        value = linkedBankName,
                        onValueChange = { linkedBankName = it },
                        label = { Text("Bank Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = linkedAccountNumber,
                        onValueChange = { linkedAccountNumber = it },
                        label = { Text("Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    label = { Text("Current Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = returnsPercent,
                    onValueChange = { returnsPercent = it },
                    label = { Text("Returns %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = { Text("%") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    fund.copy(
                        portfolioName = portfolioName.trim(),
                        amcName = amcName.trim(),
                        fundName = fundName.trim(),
                        folioNumber = folioNumber.trim(),
                        sipStartDate = if (fund.investmentType == MfInvestmentType.SIP) sipStartDate else fund.sipStartDate,
                        sipAmount = if (fund.investmentType == MfInvestmentType.SIP) sipAmount.toDoubleOrNull() else fund.sipAmount,
                        sipFrequency = sipFrequency,
                        investmentDate = if (fund.investmentType == MfInvestmentType.LUMPSUM) investmentDate else fund.investmentDate,
                        investmentAmount = if (fund.investmentType == MfInvestmentType.LUMPSUM) investmentAmount.toDoubleOrNull() else fund.investmentAmount,
                        purpose = purpose.trim(),
                        nominee = nominee.trim(),
                        autoPay = autoPay,
                        linkedBankName = if (autoPay) linkedBankName.trim() else "",
                        linkedAccountNumber = if (autoPay) linkedAccountNumber.trim() else "",
                        currentValue = currentValue.toDoubleOrNull(),
                        returnsPercent = returnsPercent.toDoubleOrNull()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showSipDatePicker) {
        SimpleDatePickerDialog(
            initialDate = sipStartDate,
            onDateSelected = { sipStartDate = it },
            onDismiss = { showSipDatePicker = false }
        )
    }

    if (showInvestmentDatePicker) {
        SimpleDatePickerDialog(
            initialDate = investmentDate,
            onDateSelected = { investmentDate = it },
            onDismiss = { showInvestmentDatePicker = false }
        )
    }
}
