package com.personaltracker.ui.screens.deposits

import androidx.compose.foundation.background
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.ContributionType
import com.personaltracker.data.database.entity.DepositEntity
import com.personaltracker.data.database.entity.DepositType
import com.personaltracker.domain.repository.DepositRepository
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

private fun depositTypeColor(type: DepositType): Color = when (type) {
    DepositType.RD  -> Color(0xFF558B2F)
    DepositType.PPF -> Color(0xFF1565C0)
    DepositType.FD  -> Color(0xFFE65100)
    DepositType.NPS -> Color(0xFF7B1FA2)
}

/** Returns how many installments have been paid from [startDate] up to today given [contributionType]. */
private fun installmentsPaid(startDate: LocalDate, contributionType: ContributionType): Long {
    val today = LocalDate.now()
    if (today.isBefore(startDate)) return 0L
    return when (contributionType) {
        ContributionType.ONE_TIME   -> 1L
        ContributionType.MONTHLY    -> ChronoUnit.MONTHS.between(startDate, today) + 1
        ContributionType.QUARTERLY  -> ChronoUnit.MONTHS.between(startDate, today) / 3 + 1
        ContributionType.YEARLY     -> ChronoUnit.YEARS.between(startDate, today) + 1
    }
}

/** Returns remaining installments from today until [maturityDate]. Returns null if no maturity date. */
private fun remainingInstallments(
    today: LocalDate,
    maturityDate: LocalDate?,
    contributionType: ContributionType
): Long? {
    if (maturityDate == null) return null
    if (today.isAfter(maturityDate)) return 0L
    return when (contributionType) {
        ContributionType.ONE_TIME   -> 0L
        ContributionType.MONTHLY    -> ChronoUnit.MONTHS.between(today, maturityDate)
        ContributionType.QUARTERLY  -> ChronoUnit.MONTHS.between(today, maturityDate) / 3
        ContributionType.YEARLY     -> ChronoUnit.YEARS.between(today, maturityDate)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Deposits list — State & ViewModel
// ─────────────────────────────────────────────────────────────────────────────

data class DepositsState(
    val deposits: List<DepositEntity> = emptyList(),
    val totalDeposited: Double = 0.0,
    val selectedType: DepositType? = null,   // null = All
    val isLoading: Boolean = true
)

@HiltViewModel
class DepositsViewModel @Inject constructor(
    private val repository: DepositRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DepositsState())
    val state: StateFlow<DepositsState> = _state.asStateFlow()

    private var allDeposits: List<DepositEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAll().collect { list ->
                allDeposits = list
                applyFilter(_state.value.selectedType)
            }
        }
        viewModelScope.launch {
            repository.getTotalDeposited().collect { total ->
                _state.value = _state.value.copy(totalDeposited = total)
            }
        }
    }

    fun filterByType(type: DepositType?) {
        _state.value = _state.value.copy(selectedType = type)
        applyFilter(type)
    }

    private fun applyFilter(type: DepositType?) {
        val filtered = if (type == null) allDeposits else allDeposits.filter { it.depositType == type }
        _state.value = _state.value.copy(deposits = filtered, isLoading = false)
    }

    fun delete(entity: DepositEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DepositsScreen composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DepositsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var depositToDelete by remember { mutableStateOf<DepositEntity?>(null) }

    Scaffold(
        topBar = { PTTopBar(title = "Deposits", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = Color(0xFF1565C0)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Deposit", tint = Color.White)
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
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Total Deposited",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatCurrency(state.totalDeposited),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${state.deposits.size} active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Type filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedType == null,
                        onClick = { viewModel.filterByType(null) },
                        label = { Text("All") }
                    )
                }
                items(DepositType.values()) { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.filterByType(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.deposits.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.AccountBalance,
                        message = "No deposits found",
                        actionLabel = "Add Deposit",
                        onAction = onNavigateToAdd
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.deposits, key = { it.id }) { deposit ->
                            DepositCard(
                                deposit = deposit,
                                onClick = { onNavigateToDetail(deposit.id) },
                                onDelete = { depositToDelete = deposit }
                            )
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }

    depositToDelete?.let { dep ->
        ConfirmDeleteDialog(
            title = "Delete Deposit",
            message = "Delete this ${dep.depositType.name} deposit at ${dep.institutionName}? This cannot be undone.",
            onConfirm = {
                viewModel.delete(dep)
                depositToDelete = null
            },
            onDismiss = { depositToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositCard(
    deposit: DepositEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = depositTypeColor(deposit.depositType)
    val today = LocalDate.now()
    val isMaturing = deposit.maturityDate?.let {
        !it.isBefore(today) && ChronoUnit.DAYS.between(today, it) <= 90
    } ?: false

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
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = deposit.depositType.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = deposit.institutionName.ifBlank { deposit.depositType.name },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    DepositTypeBadge(type = deposit.depositType, color = color)
                }
                if (deposit.accountNumber.isNotBlank()) {
                    Text(
                        text = "A/C: ${deposit.accountNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                deposit.maturityDate?.let { maturity ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isMaturing) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Matures: ${maturity.format(DATE_FMT)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMaturing) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(deposit.depositAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = deposit.contributionType.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun DepositTypeBadge(type: DepositType, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Deposit — State & ViewModel
// ─────────────────────────────────────────────────────────────────────────────

data class AddDepositState(
    val depositType: DepositType = DepositType.FD,
    val accountNumber: String = "",
    val institutionName: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val maturityDate: LocalDate? = null,
    val depositAmount: String = "",
    val interestRate: String = "",
    val purpose: String = "",
    val nominee: String = "",
    val contributionType: ContributionType = ContributionType.MONTHLY,
    val autoDebit: Boolean = false,
    val linkedBankName: String = "",
    val linkedAccountNumber: String = "",
    val currentValue: String = "",
    val maturityValue: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddDepositViewModel @Inject constructor(
    private val repository: DepositRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddDepositState())
    val state: StateFlow<AddDepositState> = _state.asStateFlow()

    fun update(transform: AddDepositState.() -> AddDepositState) {
        _state.value = _state.value.transform()
    }

    fun save() {
        val s = _state.value
        val amount = s.depositAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.value = s.copy(error = "Enter a valid deposit amount")
            return
        }
        _state.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                repository.insert(
                    DepositEntity(
                        depositType = s.depositType,
                        accountNumber = s.accountNumber.trim(),
                        institutionName = s.institutionName.trim(),
                        startDate = s.startDate,
                        maturityDate = s.maturityDate,
                        depositAmount = amount,
                        interestRate = s.interestRate.toDoubleOrNull(),
                        purpose = s.purpose.trim(),
                        nominee = s.nominee.trim(),
                        contributionType = s.contributionType,
                        autoDebit = s.autoDebit,
                        linkedBankName = s.linkedBankName.trim(),
                        linkedAccountNumber = s.linkedAccountNumber.trim(),
                        currentValue = s.currentValue.toDoubleOrNull(),
                        maturityValue = s.maturityValue.toDoubleOrNull()
                    )
                )
                _state.value = _state.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddDepositScreen composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDepositScreen(
    onBack: () -> Unit,
    viewModel: AddDepositViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showMaturityDatePicker by remember { mutableStateOf(false) }
    var showDepositTypeMenu by remember { mutableStateOf(false) }
    var showContributionTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { PTTopBar(title = "Add Deposit", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Deposit Type dropdown ─────────────────────────────────────
            SectionHeader("Deposit Details")

            ExposedDropdownMenuBox(
                expanded = showDepositTypeMenu,
                onExpandedChange = { showDepositTypeMenu = it }
            ) {
                OutlinedTextField(
                    value = state.depositType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deposit Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDepositTypeMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showDepositTypeMenu,
                    onDismissRequest = { showDepositTypeMenu = false }
                ) {
                    DepositType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                viewModel.update { copy(depositType = type) }
                                showDepositTypeMenu = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.institutionName,
                onValueChange = { viewModel.update { copy(institutionName = it) } },
                label = { Text("Institution Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.accountNumber,
                onValueChange = { viewModel.update { copy(accountNumber = it) } },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Dates ─────────────────────────────────────────────────────
            SectionHeader("Dates")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.startDate.format(DATE_FMT),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date *") },
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick start date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.maturityDate?.format(DATE_FMT) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Maturity Date") },
                    placeholder = { Text("Optional") },
                    trailingIcon = {
                        if (state.maturityDate != null) {
                            IconButton(onClick = { viewModel.update { copy(maturityDate = null) } }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        } else {
                            IconButton(onClick = { showMaturityDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick maturity date")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Amount & Rate ─────────────────────────────────────────────
            SectionHeader("Amount & Interest")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.depositAmount,
                    onValueChange = { viewModel.update { copy(depositAmount = it) } },
                    label = { Text("Deposit Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.interestRate,
                    onValueChange = { viewModel.update { copy(interestRate = it) } },
                    label = { Text("Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // ── Contribution ──────────────────────────────────────────────
            SectionHeader("Contribution")

            ExposedDropdownMenuBox(
                expanded = showContributionTypeMenu,
                onExpandedChange = { showContributionTypeMenu = it }
            ) {
                OutlinedTextField(
                    value = state.contributionType.name.replace('_', ' '),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Contribution Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showContributionTypeMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showContributionTypeMenu,
                    onDismissRequest = { showContributionTypeMenu = false }
                ) {
                    ContributionType.values().forEach { ct ->
                        DropdownMenuItem(
                            text = { Text(ct.name.replace('_', ' ')) },
                            onClick = {
                                viewModel.update { copy(contributionType = ct) }
                                showContributionTypeMenu = false
                            }
                        )
                    }
                }
            }

            // ── Auto Debit ────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Auto Debit", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.autoDebit,
                        onCheckedChange = { viewModel.update { copy(autoDebit = it) } }
                    )
                }
                if (state.autoDebit) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                        OutlinedTextField(
                            value = state.linkedBankName,
                            onValueChange = { viewModel.update { copy(linkedBankName = it) } },
                            label = { Text("Linked Bank Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.linkedAccountNumber,
                            onValueChange = { viewModel.update { copy(linkedAccountNumber = it) } },
                            label = { Text("Linked Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // ── Purpose & Nominee ─────────────────────────────────────────
            SectionHeader("Other Details")

            OutlinedTextField(
                value = state.purpose,
                onValueChange = { viewModel.update { copy(purpose = it) } },
                label = { Text("Purpose") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.nominee,
                onValueChange = { viewModel.update { copy(nominee = it) } },
                label = { Text("Nominee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Optional Tracking Values ──────────────────────────────────
            SectionHeader("Optional Tracking")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.currentValue,
                    onValueChange = { viewModel.update { copy(currentValue = it) } },
                    label = { Text("Current Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.maturityValue,
                    onValueChange = { viewModel.update { copy(maturityValue = it) } },
                    label = { Text("Maturity Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            state.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save Deposit")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Date Pickers ──────────────────────────────────────────────────────────
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.startDate,
            title = "Select Start Date",
            onDateSelected = { date ->
                viewModel.update { copy(startDate = date) }
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showMaturityDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.maturityDate ?: LocalDate.now().plusYears(1),
            title = "Select Maturity Date",
            onDateSelected = { date ->
                viewModel.update { copy(maturityDate = date) }
                showMaturityDatePicker = false
            },
            onDismiss = { showMaturityDatePicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Deposit Detail — State & ViewModel
// ─────────────────────────────────────────────────────────────────────────────

data class DepositDetailState(
    val deposit: DepositEntity? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class DepositDetailViewModel @Inject constructor(
    private val repository: DepositRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DepositDetailState())
    val state: StateFlow<DepositDetailState> = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val deposit = repository.getById(id)
            _state.value = DepositDetailState(deposit = deposit, isLoading = false)
        }
    }

    fun delete() {
        val dep = _state.value.deposit ?: return
        viewModelScope.launch {
            repository.delete(dep)
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DepositDetailScreen composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositDetailScreen(
    depositId: Long,
    onBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: DepositDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(depositId) { viewModel.load(depositId) }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Deposit Detail",
                onBack = onBack,
                actions = {
                    if (state.deposit != null) {
                        IconButton(onClick = { onNavigateToEdit(depositId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.deposit == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Deposit not found.")
                }
            }
            else -> {
                val dep = state.deposit!!
                val today = LocalDate.now()
                val paid = installmentsPaid(dep.startDate, dep.contributionType)
                val totalDepositedTillDate = dep.depositAmount * paid
                val remaining = remainingInstallments(today, dep.maturityDate, dep.contributionType)
                val timeToMaturity = dep.maturityDate?.let {
                    if (today.isAfter(it)) "Matured"
                    else {
                        val months = ChronoUnit.MONTHS.between(today, it)
                        if (months >= 12) "${months / 12}y ${months % 12}m" else "${months}m"
                    }
                }
                val investmentAgeYears = ChronoUnit.MONTHS.between(dep.startDate, today).toDouble() / 12.0

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header card
                    item {
                        val color = depositTypeColor(dep.depositType)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            dep.depositType.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = color
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            dep.institutionName.ifBlank { dep.depositType.name },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (dep.accountNumber.isNotBlank()) {
                                            Text(
                                                "A/C: ${dep.accountNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Computed summary
                    item {
                        SectionHeader("Computed Summary")
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailRow("Total Deposited Till Date", formatCurrency(totalDepositedTillDate))
                                DetailRow("Installments Paid", "$paid")
                                DetailRow("Investment Age", "%.2f years".format(investmentAgeYears))
                                if (remaining != null) {
                                    DetailRow("Remaining Contributions", "$remaining")
                                }
                                if (timeToMaturity != null) {
                                    DetailRow("Time to Maturity", timeToMaturity)
                                }
                            }
                        }
                    }

                    // Basic deposit info
                    item {
                        SectionHeader("Deposit Information")
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailRow("Type", dep.depositType.name)
                                DetailRow("Contribution", dep.contributionType.name.replace('_', ' '))
                                DetailRow("Amount per Contribution", formatCurrency(dep.depositAmount))
                                dep.interestRate?.let { DetailRow("Interest Rate", "$it%") }
                                DetailRow("Start Date", dep.startDate.format(DATE_FMT))
                                dep.maturityDate?.let { DetailRow("Maturity Date", it.format(DATE_FMT)) }
                            }
                        }
                    }

                    // Optional tracking
                    if (dep.currentValue != null || dep.maturityValue != null) {
                        item {
                            SectionHeader("Tracking Values")
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    dep.currentValue?.let { DetailRow("Current Value", formatCurrency(it)) }
                                    dep.maturityValue?.let { DetailRow("Maturity Value", formatCurrency(it)) }
                                }
                            }
                        }
                    }

                    // Other details
                    item {
                        SectionHeader("Other Details")
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dep.purpose.isNotBlank()) DetailRow("Purpose", dep.purpose)
                                if (dep.nominee.isNotBlank()) DetailRow("Nominee", dep.nominee)
                                DetailRow("Auto Debit", if (dep.autoDebit) "Yes" else "No")
                                if (dep.autoDebit) {
                                    if (dep.linkedBankName.isNotBlank()) DetailRow("Linked Bank", dep.linkedBankName)
                                    if (dep.linkedAccountNumber.isNotBlank()) DetailRow("Linked A/C", dep.linkedAccountNumber)
                                }
                                DetailRow("Status", if (dep.isActive) "Active" else "Closed")
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Deposit",
            message = "Delete this ${state.deposit?.depositType?.name} deposit? This cannot be undone.",
            onConfirm = { viewModel.delete(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SimpleDatePickerDialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    title: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate
        .atStartOfDay(java.time.ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(selected)
                    } ?: onDismiss()
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
        )
    }
}
