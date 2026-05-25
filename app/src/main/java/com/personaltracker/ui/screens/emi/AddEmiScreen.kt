package com.personaltracker.ui.screens.emi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.EmiEntity
import com.personaltracker.domain.repository.EmiRepository
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
class AddEmiViewModel @Inject constructor(
    private val emiRepository: EmiRepository
) : ViewModel() {

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    fun save(emi: EmiEntity) {
        viewModelScope.launch {
            emiRepository.insertEmi(emi)
            _saveResult.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmiScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEmiViewModel = hiltViewModel()
) {
    val saveResult by viewModel.saveResult.collectAsState()
    LaunchedEffect(saveResult) {
        if (saveResult == true) onSaved()
    }

    var name by remember { mutableStateOf("") }
    var lenderName by remember { mutableStateOf("") }
    var loanAmount by remember { mutableStateOf("") }
    var emiAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var tenureMonths by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var dueDay by remember { mutableStateOf("1") }
    var accountNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var lenderError by remember { mutableStateOf(false) }
    var loanAmountError by remember { mutableStateOf(false) }
    var emiAmountError by remember { mutableStateOf(false) }
    var tenureError by remember { mutableStateOf(false) }
    var dueDayError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        nameError = name.isBlank()
        lenderError = lenderName.isBlank()
        loanAmountError = loanAmount.toDoubleOrNull()?.let { it <= 0 } ?: true
        emiAmountError = emiAmount.toDoubleOrNull()?.let { it <= 0 } ?: true
        tenureError = tenureMonths.toIntOrNull()?.let { it <= 0 } ?: true
        dueDayError = dueDay.toIntOrNull()?.let { it < 1 || it > 31 } ?: true
        return !nameError && !lenderError && !loanAmountError && !emiAmountError && !tenureError && !dueDayError
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add EMI", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name / Purpose
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Loan Name / Purpose *") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Name is required") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
            )

            // Lender Name
            OutlinedTextField(
                value = lenderName,
                onValueChange = { lenderName = it; lenderError = false },
                label = { Text("Lender Name *") },
                isError = lenderError,
                supportingText = if (lenderError) { { Text("Lender is required") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            // Loan Amount
            OutlinedTextField(
                value = loanAmount,
                onValueChange = { loanAmount = it; loanAmountError = false },
                label = { Text("Loan Amount *") },
                isError = loanAmountError,
                supportingText = if (loanAmountError) { { Text("Enter valid amount") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // EMI Amount
            OutlinedTextField(
                value = emiAmount,
                onValueChange = { emiAmount = it; emiAmountError = false },
                label = { Text("EMI Amount (per month) *") },
                isError = emiAmountError,
                supportingText = if (emiAmountError) { { Text("Enter valid EMI amount") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Interest Rate
            OutlinedTextField(
                value = interestRate,
                onValueChange = { interestRate = it },
                label = { Text("Interest Rate % (p.a.)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text("%") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tenure
            OutlinedTextField(
                value = tenureMonths,
                onValueChange = { tenureMonths = it; tenureError = false },
                label = { Text("Tenure (months) *") },
                isError = tenureError,
                supportingText = if (tenureError) { { Text("Enter valid tenure") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Text("months") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Start Date
            OutlinedTextField(
                value = startDate.format(DISPLAY_DATE_FMT),
                onValueChange = {},
                readOnly = true,
                label = { Text("EMI Start Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // Due Day
            OutlinedTextField(
                value = dueDay,
                onValueChange = { v ->
                    if (v.isEmpty() || v.toIntOrNull() != null) {
                        dueDay = v
                        dueDayError = false
                    }
                },
                label = { Text("Due Day of Month * (1–31)") },
                isError = dueDayError,
                supportingText = if (dueDayError) { { Text("Enter a day between 1 and 31") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Account Number
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Loan Account Number (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) }
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
                            EmiEntity(
                                name = name.trim(),
                                lenderName = lenderName.trim(),
                                loanAmount = loanAmount.toDouble(),
                                emiAmount = emiAmount.toDouble(),
                                interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                                tenureMonths = tenureMonths.toInt(),
                                startDate = startDate,
                                dueDay = dueDay.toInt(),
                                loanAccountNumber = accountNumber.takeIf { it.isNotBlank() },
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
                Text("Save EMI", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
