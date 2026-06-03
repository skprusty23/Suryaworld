package com.personaltracker.ui.screens.insurance

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
import com.personaltracker.data.database.entity.InsuranceEntity
import com.personaltracker.data.database.entity.InsuranceType
import com.personaltracker.data.database.entity.PremiumFrequency
import com.personaltracker.domain.repository.InsuranceRepository
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

private fun premiumsPerYear(freq: PremiumFrequency): Int = when (freq) {
    PremiumFrequency.MONTHLY -> 12
    PremiumFrequency.QUARTERLY -> 4
    PremiumFrequency.HALF_YEARLY -> 2
    PremiumFrequency.YEARLY -> 1
}

private fun insuranceTypeColor(type: InsuranceType): Color = when (type) {
    InsuranceType.LIFE -> Color(0xFF1565C0)
    InsuranceType.HEALTH -> Color(0xFF2E7D32)
    InsuranceType.TERM -> Color(0xFF6A1B9A)
}

private fun insuranceTypeLabel(type: InsuranceType): String = when (type) {
    InsuranceType.LIFE -> "Life"
    InsuranceType.HEALTH -> "Health"
    InsuranceType.TERM -> "Term"
}

private fun premiumFrequencyLabel(freq: PremiumFrequency): String = when (freq) {
    PremiumFrequency.MONTHLY -> "Monthly"
    PremiumFrequency.QUARTERLY -> "Quarterly"
    PremiumFrequency.HALF_YEARLY -> "Half-Yearly"
    PremiumFrequency.YEARLY -> "Yearly"
}

data class InsuranceCalculations(
    val premiumsPerYear: Int,
    val monthsActive: Long,
    val numberOfPremiumsPaid: Int,
    val totalPremiumsPaid: Double,
    val totalPremiums: Int,
    val remainingPremiums: Int,
    val remainingAmount: Double,
    val daysToMaturity: Long,
    val policyAgeYears: Long
)

private fun calculateInsurance(entity: InsuranceEntity): InsuranceCalculations {
    val today = LocalDate.now()
    val ppy = premiumsPerYear(entity.premiumFrequency)
    val monthsActive = ChronoUnit.MONTHS.between(entity.startDate, today)
    val numberOfPremiumsPaid = (monthsActive / (12 / ppy)).toInt()
    val totalPremiumsPaid = numberOfPremiumsPaid * entity.premiumAmount
    val totalPremiums = entity.policyTermYears * ppy
    val remainingPremiums = (totalPremiums - numberOfPremiumsPaid).coerceAtLeast(0)
    val remainingAmount = remainingPremiums * entity.premiumAmount
    val daysToMaturity = ChronoUnit.DAYS.between(today, entity.maturityDate)
    val policyAgeYears = ChronoUnit.YEARS.between(entity.startDate, today)
    return InsuranceCalculations(
        premiumsPerYear = ppy,
        monthsActive = monthsActive,
        numberOfPremiumsPaid = numberOfPremiumsPaid,
        totalPremiumsPaid = totalPremiumsPaid,
        totalPremiums = totalPremiums,
        remainingPremiums = remainingPremiums,
        remainingAmount = remainingAmount,
        daysToMaturity = daysToMaturity,
        policyAgeYears = policyAgeYears
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// InsurancesViewModel
// ──────────────────────────────────────────────────────────────────────────────

data class InsurancesState(
    val policies: List<InsuranceEntity> = emptyList(),
    val filteredPolicies: List<InsuranceEntity> = emptyList(),
    val selectedFilter: InsuranceType? = null,
    val totalCoverage: Double = 0.0,
    val totalAnnualPremium: Double = 0.0,
    val activeCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class InsurancesViewModel @Inject constructor(
    private val repository: InsuranceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InsurancesState())
    val state: StateFlow<InsurancesState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAll(),
                repository.getTotalCoverage()
            ) { policies, totalCoverage ->
                val activeCount = policies.count { it.isActive }
                val totalAnnualPremium = policies
                    .filter { it.isActive }
                    .sumOf { it.premiumAmount * premiumsPerYear(it.premiumFrequency) }
                val filter = _state.value.selectedFilter
                val filtered = if (filter == null) policies else policies.filter { it.insuranceType == filter }
                InsurancesState(
                    policies = policies,
                    filteredPolicies = filtered,
                    selectedFilter = filter,
                    totalCoverage = totalCoverage,
                    totalAnnualPremium = totalAnnualPremium,
                    activeCount = activeCount,
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun setFilter(type: InsuranceType?) {
        val filtered = if (type == null) _state.value.policies
        else _state.value.policies.filter { it.insuranceType == type }
        _state.value = _state.value.copy(selectedFilter = type, filteredPolicies = filtered)
    }

    fun delete(entity: InsuranceEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// InsurancesScreen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsurancesScreen(
    onAddInsurance: () -> Unit,
    onInsuranceDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: InsurancesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            PTTopBar(title = "Insurances", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddInsurance,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Insurance", tint = Color.White)
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsuranceSummaryCard(
                        label = "Total Coverage",
                        value = formatCurrency(state.totalCoverage),
                        icon = Icons.Default.Shield,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )
                    InsuranceSummaryCard(
                        label = "Annual Premium",
                        value = formatCurrency(state.totalAnnualPremium),
                        icon = Icons.Default.Payments,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                InsuranceSummaryCard(
                    label = "Active Policies",
                    value = "${state.activeCount}",
                    icon = Icons.Default.Policy,
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(InsuranceType.values()) { type ->
                        FilterChip(
                            selected = state.selectedFilter == type,
                            onClick = { viewModel.setFilter(type) },
                            label = { Text(insuranceTypeLabel(type)) }
                        )
                    }
                }
            }

            if (state.filteredPolicies.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Shield,
                        title = "No insurance policies",
                        subtitle = "Tap + to add your first policy"
                    )
                }
            } else {
                items(state.filteredPolicies, key = { it.id }) { policy ->
                    InsurancePolicyCard(
                        entity = policy,
                        onClick = { onInsuranceDetail(policy.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsurancePolicyCard(
    entity: InsuranceEntity,
    onClick: () -> Unit
) {
    val calc = remember(entity) { calculateInsurance(entity) }
    val typeColor = insuranceTypeColor(entity.insuranceType)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = typeColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entity.policyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entity.providerCompany.isNotBlank()) {
                    Text(
                        entity.providerCompany,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SugarChip(label = insuranceTypeLabel(entity.insuranceType), color = typeColor)
                    if (calc.daysToMaturity > 0) {
                        SugarChip(
                            label = "${calc.daysToMaturity}d left",
                            color = if (calc.daysToMaturity < 90) Color(0xFFE65100) else Color(0xFF546E7A)
                        )
                    } else {
                        SugarChip(label = "Matured", color = Color(0xFF757575))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(entity.sumAssured),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Text(
                    formatCurrency(entity.premiumAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    premiumFrequencyLabel(entity.premiumFrequency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsuranceSummaryCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@Composable
private fun SugarChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// AddInsuranceViewModel
// ──────────────────────────────────────────────────────────────────────────────

data class AddInsuranceState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    // form fields
    val policyName: String = "",
    val insuranceType: InsuranceType = InsuranceType.LIFE,
    val policyNumber: String = "",
    val providerCompany: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val maturityDate: LocalDate = LocalDate.now().plusYears(10),
    val sumAssured: String = "",
    val premiumAmount: String = "",
    val premiumFrequency: PremiumFrequency = PremiumFrequency.YEARLY,
    val policyTermYears: String = "",
    val nomineeName: String = "",
    val notes: String = "",
    val isEditMode: Boolean = false,
    val editId: Long = 0L
)

@HiltViewModel
class AddInsuranceViewModel @Inject constructor(
    private val repository: InsuranceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddInsuranceState())
    val state: StateFlow<AddInsuranceState> = _state.asStateFlow()

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val entity = repository.getById(id) ?: return@launch
            _state.value = AddInsuranceState(
                isEditMode = true,
                editId = id,
                policyName = entity.policyName,
                insuranceType = entity.insuranceType,
                policyNumber = entity.policyNumber,
                providerCompany = entity.providerCompany,
                startDate = entity.startDate,
                maturityDate = entity.maturityDate,
                sumAssured = entity.sumAssured.toString(),
                premiumAmount = entity.premiumAmount.toString(),
                premiumFrequency = entity.premiumFrequency,
                policyTermYears = entity.policyTermYears.toString(),
                nomineeName = entity.nomineeName,
                notes = entity.notes
            )
        }
    }

    fun update(field: AddInsuranceState.() -> AddInsuranceState) {
        _state.value = _state.value.field()
    }

    fun save() {
        val s = _state.value
        if (s.policyName.isBlank()) {
            _state.value = s.copy(error = "Policy name is required")
            return
        }
        val sumAssured = s.sumAssured.toDoubleOrNull()
        if (sumAssured == null || sumAssured <= 0) {
            _state.value = s.copy(error = "Enter a valid sum assured")
            return
        }
        val premiumAmount = s.premiumAmount.toDoubleOrNull()
        if (premiumAmount == null || premiumAmount <= 0) {
            _state.value = s.copy(error = "Enter a valid premium amount")
            return
        }
        val termYears = s.policyTermYears.toIntOrNull() ?: 0
        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val entity = InsuranceEntity(
                id = if (s.isEditMode) s.editId else 0L,
                policyName = s.policyName.trim(),
                insuranceType = s.insuranceType,
                policyNumber = s.policyNumber.trim(),
                providerCompany = s.providerCompany.trim(),
                startDate = s.startDate,
                maturityDate = s.maturityDate,
                sumAssured = sumAssured,
                premiumAmount = premiumAmount,
                premiumFrequency = s.premiumFrequency,
                policyTermYears = termYears,
                nomineeName = s.nomineeName.trim(),
                notes = s.notes.trim()
            )
            if (s.isEditMode) repository.update(entity) else repository.insert(entity)
            _state.value = _state.value.copy(isLoading = false, isSaved = true)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// AddInsuranceScreen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInsuranceScreen(
    editId: Long = 0L,
    onBack: () -> Unit,
    viewModel: AddInsuranceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(editId) {
        if (editId != 0L) viewModel.loadForEdit(editId)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showMaturityDatePicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showFreqDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PTTopBar(
                title = if (state.isEditMode) "Edit Insurance" else "Add Insurance",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Policy Name
            OutlinedTextField(
                value = state.policyName,
                onValueChange = { viewModel.update { copy(policyName = it) } },
                label = { Text("Policy Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) }
            )

            // Insurance Type
            ExposedDropdownMenuBox(
                expanded = showTypeDropdown,
                onExpandedChange = { showTypeDropdown = it }
            ) {
                OutlinedTextField(
                    value = insuranceTypeLabel(state.insuranceType),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Insurance Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    InsuranceType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(insuranceTypeLabel(type)) },
                            onClick = {
                                viewModel.update { copy(insuranceType = type) }
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            // Policy Number
            OutlinedTextField(
                value = state.policyNumber,
                onValueChange = { viewModel.update { copy(policyNumber = it) } },
                label = { Text("Policy Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) }
            )

            // Provider Company
            OutlinedTextField(
                value = state.providerCompany,
                onValueChange = { viewModel.update { copy(providerCompany = it) } },
                label = { Text("Provider / Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            // Start Date
            OutlinedTextField(
                value = state.startDate.format(DATE_FMT),
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Pick start date")
                    }
                }
            )

            // Maturity Date
            OutlinedTextField(
                value = state.maturityDate.format(DATE_FMT),
                onValueChange = {},
                readOnly = true,
                label = { Text("Maturity Date *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showMaturityDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Pick maturity date")
                    }
                }
            )

            // Sum Assured
            OutlinedTextField(
                value = state.sumAssured,
                onValueChange = { viewModel.update { copy(sumAssured = it) } },
                label = { Text("Sum Assured *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) }
            )

            // Premium Amount
            OutlinedTextField(
                value = state.premiumAmount,
                onValueChange = { viewModel.update { copy(premiumAmount = it) } },
                label = { Text("Premium Amount *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
            )

            // Premium Frequency
            ExposedDropdownMenuBox(
                expanded = showFreqDropdown,
                onExpandedChange = { showFreqDropdown = it }
            ) {
                OutlinedTextField(
                    value = premiumFrequencyLabel(state.premiumFrequency),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Premium Frequency *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFreqDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = showFreqDropdown,
                    onDismissRequest = { showFreqDropdown = false }
                ) {
                    PremiumFrequency.values().forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(premiumFrequencyLabel(freq)) },
                            onClick = {
                                viewModel.update { copy(premiumFrequency = freq) }
                                showFreqDropdown = false
                            }
                        )
                    }
                }
            }

            // Policy Term Years
            OutlinedTextField(
                value = state.policyTermYears,
                onValueChange = { viewModel.update { copy(policyTermYears = it) } },
                label = { Text("Policy Term (Years)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Timeline, contentDescription = null) }
            )

            // Nominee Name
            OutlinedTextField(
                value = state.nomineeName,
                onValueChange = { viewModel.update { copy(nomineeName = it) } },
                label = { Text("Nominee Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { copy(notes = it) } },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
            )

            // Error
            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isEditMode) "Update Policy" else "Save Policy")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Start Date Picker
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

    // Maturity Date Picker
    if (showMaturityDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.maturityDate,
            title = "Select Maturity Date",
            onDateSelected = { date ->
                viewModel.update { copy(maturityDate = date) }
                showMaturityDatePicker = false
            },
            onDismiss = { showMaturityDatePicker = false }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SimpleDatePickerDialog
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    title: String = "Select Date",
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate
        .atStartOfDay()
        .toInstant(java.time.ZoneOffset.UTC)
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selected = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    onDateSelected(selected)
                }
            }) { Text("OK") }
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

// ──────────────────────────────────────────────────────────────────────────────
// InsuranceDetailViewModel
// ──────────────────────────────────────────────────────────────────────────────

data class InsuranceDetailState(
    val entity: InsuranceEntity? = null,
    val calculations: InsuranceCalculations? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class InsuranceDetailViewModel @Inject constructor(
    private val repository: InsuranceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InsuranceDetailState())
    val state: StateFlow<InsuranceDetailState> = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val entity = repository.getById(id)
            if (entity != null) {
                _state.value = InsuranceDetailState(
                    entity = entity,
                    calculations = calculateInsurance(entity),
                    isLoading = false
                )
            } else {
                _state.value = InsuranceDetailState(isLoading = false)
            }
        }
    }

    fun delete() {
        val entity = _state.value.entity ?: return
        viewModelScope.launch {
            repository.delete(entity)
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// InsuranceDetailScreen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceDetailScreen(
    id: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: InsuranceDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.load(id) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = state.entity?.policyName ?: "Insurance Detail",
                onBack = onBack,
                actions = {
                    if (state.entity != null) {
                        IconButton(onClick = { onEdit(id) }) {
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
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.entity == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Policy not found", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                val entity = state.entity!!
                val calc = state.calculations!!
                val typeColor = insuranceTypeColor(entity.insuranceType)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(typeColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = typeColor, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            entity.policyName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (entity.providerCompany.isNotBlank()) {
                                            Text(
                                                entity.providerCompany,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        SugarChip(label = insuranceTypeLabel(entity.insuranceType), color = typeColor)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    DetailStat(
                                        label = "Sum Assured",
                                        value = formatCurrency(entity.sumAssured),
                                        color = typeColor
                                    )
                                    DetailStat(
                                        label = "Premium",
                                        value = formatCurrency(entity.premiumAmount),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    DetailStat(
                                        label = "Days Left",
                                        value = if (calc.daysToMaturity > 0) "${calc.daysToMaturity}" else "Matured",
                                        color = if (calc.daysToMaturity > 0 && calc.daysToMaturity < 90) Color(0xFFE65100)
                                               else if (calc.daysToMaturity <= 0) Color(0xFF757575)
                                               else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    // Calculated Summary
                    item {
                        DetailSection(title = "Premium Summary") {
                            DetailRow("Premiums Per Year", "${calc.premiumsPerYear}")
                            DetailRow("Policy Age", "${calc.policyAgeYears} year(s)")
                            DetailRow("Premiums Paid", "${calc.numberOfPremiumsPaid}")
                            DetailRow("Total Paid So Far", formatCurrency(calc.totalPremiumsPaid))
                            DetailRow("Total Premiums", "${calc.totalPremiums}")
                            DetailRow("Remaining Premiums", "${calc.remainingPremiums}")
                            DetailRow("Remaining Amount", formatCurrency(calc.remainingAmount))
                        }
                    }

                    // Policy Info
                    item {
                        DetailSection(title = "Policy Information") {
                            if (entity.policyNumber.isNotBlank()) {
                                DetailRow("Policy Number", entity.policyNumber)
                            }
                            DetailRow("Start Date", entity.startDate.format(DATE_FMT))
                            DetailRow("Maturity Date", entity.maturityDate.format(DATE_FMT))
                            DetailRow("Policy Term", "${entity.policyTermYears} year(s)")
                            DetailRow("Frequency", premiumFrequencyLabel(entity.premiumFrequency))
                            if (entity.nomineeName.isNotBlank()) {
                                DetailRow("Nominee", entity.nomineeName)
                            }
                            DetailRow("Status", if (entity.isActive) "Active" else "Inactive")
                        }
                    }

                    // Notes
                    if (entity.notes.isNotBlank()) {
                        item {
                            DetailSection(title = "Notes") {
                                Text(
                                    entity.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            itemName = state.entity?.policyName ?: "this policy",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun DetailStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
