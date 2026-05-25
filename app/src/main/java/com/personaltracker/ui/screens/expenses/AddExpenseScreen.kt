package com.personaltracker.ui.screens.expenses

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
import com.personaltracker.ui.components.PTTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DISPLAY_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val PAYMENT_METHODS = listOf("Cash", "Card", "UPI", "NetBanking")
private val EXPENSE_TYPES = listOf("Home", "Outside")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var subCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var selectedExpenseType by remember { mutableStateOf("Home") }
    var notes by remember { mutableStateOf("") }

    var amountError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val saveResult by viewModel.saveResult.collectAsState()
    LaunchedEffect(saveResult) {
        if (saveResult == true) onSaved()
    }

    val scrollState = rememberScrollState()
    val categories = ExpensesViewModel.DEFAULT_CATEGORIES.drop(1)

    fun validate(): Boolean {
        amountError = amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0
        categoryError = selectedCategory.isBlank()
        return !amountError && !categoryError
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Expense", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it; amountError = false },
                        placeholder = {
                            Text("0.00", style = MaterialTheme.typography.headlineLarge)
                        },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError,
                        singleLine = true,
                        leadingIcon = {
                            Text("₹", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (amountError) {
                        Text(
                            text = "Enter a valid amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Category
            FormSectionLabel("Category *")
            if (categoryError) {
                Text(
                    text = "Please select a category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat; categoryError = false },
                        label = { Text(cat) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Sub Category
            OutlinedTextField(
                value = subCategory,
                onValueChange = { subCategory = it },
                label = { Text("Sub Category (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Date
            FormSectionLabel("Date")
            OutlinedTextField(
                value = selectedDate.format(DISPLAY_DATE_FMT),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // Payment Method
            FormSectionLabel("Payment Method")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PAYMENT_METHODS) { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }

            // Expense Type
            FormSectionLabel("Expense Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXPENSE_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedExpenseType == type,
                        onClick = { selectedExpenseType = type },
                        label = { Text(type) },
                        leadingIcon = {
                            val icon = if (type == "Home") Icons.Default.Home else Icons.Default.DirectionsWalk
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

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
                            amount = amount.toDouble(),
                            category = selectedCategory,
                            subCategory = subCategory.takeIf { it.isNotBlank() },
                            description = description.takeIf { it.isNotBlank() },
                            date = selectedDate,
                            paymentMethod = selectedPaymentMethod,
                            expenseType = selectedExpenseType,
                            notes = notes.takeIf { it.isNotBlank() }
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
                Text("Save Expense", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
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

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}
