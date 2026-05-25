package com.personaltracker.ui.screens.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.InvestmentEntity
import com.personaltracker.domain.repository.InvestmentRepository
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val DISPLAY_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@HiltViewModel
class AddInvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    fun save(investment: InvestmentEntity) {
        viewModelScope.launch {
            investmentRepository.insertInvestment(investment)
            _saveResult.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddInvestmentViewModel = hiltViewModel()
) {
    val saveResult by viewModel.saveResult.collectAsState()
    LaunchedEffect(saveResult) {
        if (saveResult == true) onSaved()
    }

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var principalAmount by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var maturityDate by remember { mutableStateOf<LocalDate?>(null) }
    var policyNumber by remember { mutableStateOf("") }
    var providerName by remember { mutableStateOf("") }
    var nominee by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var typeError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showMaturityDatePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val investmentTypes = InvestmentsViewModel.INVESTMENT_TYPES.drop(1) // Remove "All"

    fun validate(): Boolean {
        nameError = name.isBlank()
        typeError = selectedType.isBlank()
        amountError = principalAmount.isBlank() || principalAmount.toDoubleOrNull() == null || principalAmount.toDouble() <= 0.0
        return !nameError && !typeError && !amountError
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Investment", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Investment Name *") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Name is required") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
            )

            // Investment Type
            InvSectionLabel("Investment Type *")
            if (typeError) {
                Text("Please select a type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(investmentTypes) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type; typeError = false },
                        label = { Text(type) },
                        leadingIcon = {
                            Icon(investmentTypeIcon(type), contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            // Principal Amount
            OutlinedTextField(
                value = principalAmount,
                onValueChange = { principalAmount = it; amountError = false },
                label = { Text("Principal Amount *") },
                isError = amountError,
                supportingText = if (amountError) { { Text("Enter a valid amount") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Current Value
            OutlinedTextField(
                value = currentValue,
                onValueChange = { currentValue = it },
                label = { Text("Current Value (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Interest Rate
            OutlinedTextField(
                value = interestRate,
                onValueChange = { interestRate = it },
                label = { Text("Interest Rate % (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text("%") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Start Date
            OutlinedTextField(
                value = startDate.format(DISPLAY_DATE_FMT),
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date") },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Start Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartDatePicker = true }
            )

            // Maturity Date
            OutlinedTextField(
                value = maturityDate?.format(DISPLAY_DATE_FMT) ?: "Not set",
                onValueChange = {},
                readOnly = true,
                label = { Text("Maturity Date (optional)") },
                trailingIcon = {
                    Row {
                        if (maturityDate != null) {
                            IconButton(onClick = { maturityDate = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Maturity Date")
                            }
                        }
                        IconButton(onClick = { showMaturityDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Maturity Date")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMaturityDatePicker = true }
            )

            // Policy / Account Number
            OutlinedTextField(
                value = policyNumber,
                onValueChange = { policyNumber = it },
                label = { Text("Policy / Account Number (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) }
            )

            // Provider Name
            OutlinedTextField(
                value = providerName,
                onValueChange = { providerName = it },
                label = { Text("Provider / Institution Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
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
                            InvestmentEntity(
                                name = name.trim(),
                                investmentType = selectedType,
                                principalAmount = principalAmount.toDouble(),
                                currentValue = currentValue.toDoubleOrNull(),
                                interestRate = interestRate.toDoubleOrNull(),
                                startDate = startDate,
                                maturityDate = maturityDate,
                                policyNumber = policyNumber.takeIf { it.isNotBlank() },
                                providerName = providerName.takeIf { it.isNotBlank() },
                                nominee = nominee.takeIf { it.isNotBlank() },
                                notes = notes.takeIf { it.isNotBlank() }
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
                Text("Save Investment", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Start Date Picker
    if (showStartDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // Maturity Date Picker
    if (showMaturityDatePicker) {
        val initialMillis = (maturityDate ?: startDate.plusYears(1))
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showMaturityDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        maturityDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showMaturityDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showMaturityDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun InvSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}
