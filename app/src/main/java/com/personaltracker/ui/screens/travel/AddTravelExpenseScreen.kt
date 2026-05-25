package com.personaltracker.ui.screens.travel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.TravelExpenseEntity
import com.personaltracker.domain.repository.TravelRepository
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---- ViewModel ----

@HiltViewModel
class AddTravelExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val travelRepository: TravelRepository
) : ViewModel() {
    val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    fun addExpense(expense: TravelExpenseEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                travelRepository.insertTravelExpense(expense)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save expense")
            }
        }
    }
}

// ---- Constants ----

private val TRAVEL_CATEGORIES = listOf("Flight", "Train", "Bus", "Cab", "Hotel", "Food", "Miscellaneous")
private val PAYMENT_METHODS = listOf("Cash", "Credit Card", "Debit Card", "UPI", "Net Banking", "Other")
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// ---- Screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTravelExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTravelExpenseViewModel = hiltViewModel()
) {
    var selectedCategory by remember { mutableStateOf(TRAVEL_CATEGORIES[0]) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DATE_FORMATTER)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }

    var selectedPaymentMethod by remember { mutableStateOf(PAYMENT_METHODS[0]) }
    var paymentExpanded by remember { mutableStateOf(false) }

    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.toEpochDay() * 86_400_000L
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = LocalDate.ofEpochDay(millis / 86_400_000L)
                        dateText = date.format(DATE_FORMATTER)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    errorMessage?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Travel Expense", onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Expense Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    TRAVEL_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { selectedCategory = cat; categoryExpanded = false }
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = if (it.isNotBlank() && it.toDoubleOrNull() == null) "Enter a valid amount" else null
                },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                prefix = { Text("₹") },
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } }
            )

            // Date
            OutlinedTextField(
                value = dateText,
                onValueChange = {},
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                    }
                },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                placeholder = { Text("e.g. Indigo flight to Mumbai") }
            )

            HorizontalDivider()
            Text("Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Payment method dropdown
            ExposedDropdownMenuBox(
                expanded = paymentExpanded,
                onExpandedChange = { paymentExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPaymentMethod,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                    leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = paymentExpanded,
                    onDismissRequest = { paymentExpanded = false }
                ) {
                    PAYMENT_METHODS.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method) },
                            onClick = { selectedPaymentMethod = method; paymentExpanded = false }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                minLines = 2,
                maxLines = 4
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = "Enter a valid amount"
                        return@Button
                    }
                    val expense = TravelExpenseEntity(
                        tripId = viewModel.tripId,
                        category = selectedCategory,
                        amount = amount,
                        date = date,
                        description = description.takeIf { it.isNotBlank() },
                        paymentMethod = selectedPaymentMethod,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    viewModel.addExpense(
                        expense = expense,
                        onSuccess = onSaved,
                        onError = { errorMessage = it }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amountText.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Expense")
            }
        }
    }
}
