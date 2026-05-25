package com.personaltracker.ui.screens.gold

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.GoldInvestmentEntity
import com.personaltracker.ui.components.PTTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GOLD_TYPES = listOf("Physical", "Digital", "Jewelry")
private val GOLD_PURITIES = listOf("24K", "22K", "18K", "14K")
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoldScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: GoldViewModel = hiltViewModel()
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DATE_FORMATTER)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedGoldType by remember { mutableStateOf(GOLD_TYPES[0]) }
    var goldTypeExpanded by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("") }
    var pricePerGramText by remember { mutableStateOf("") }

    var selectedPurity by remember { mutableStateOf(GOLD_PURITIES[0]) }
    var purityExpanded by remember { mutableStateOf(false) }

    var storageLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var quantityError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val pricePerGram = pricePerGramText.toDoubleOrNull() ?: 0.0
    val totalAmount = quantity * pricePerGram

    // Date picker state
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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Gold Investment", onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Investment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Date field
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

            // Gold Type dropdown
            ExposedDropdownMenuBox(
                expanded = goldTypeExpanded,
                onExpandedChange = { goldTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedGoldType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gold Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goldTypeExpanded) },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = goldTypeExpanded,
                    onDismissRequest = { goldTypeExpanded = false }
                ) {
                    GOLD_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedGoldType = type
                                goldTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Quantity
            OutlinedTextField(
                value = quantityText,
                onValueChange = {
                    quantityText = it
                    quantityError = if (it.isNotBlank() && it.toDoubleOrNull() == null) "Enter a valid number" else null
                },
                label = { Text("Quantity (grams)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                suffix = { Text("g") },
                isError = quantityError != null,
                supportingText = quantityError?.let { { Text(it) } }
            )

            // Price per gram
            OutlinedTextField(
                value = pricePerGramText,
                onValueChange = {
                    pricePerGramText = it
                    priceError = if (it.isNotBlank() && it.toDoubleOrNull() == null) "Enter a valid amount" else null
                },
                label = { Text("Price Per Gram (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                prefix = { Text("₹") },
                isError = priceError != null,
                supportingText = priceError?.let { { Text(it) } }
            )

            // Total (read-only, auto-calculated)
            OutlinedTextField(
                value = if (totalAmount > 0) "₹%.2f".format(totalAmount) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Total Amount (auto-calculated)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            HorizontalDivider()
            Text("Quality & Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Purity dropdown
            ExposedDropdownMenuBox(
                expanded = purityExpanded,
                onExpandedChange = { purityExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPurity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Purity") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purityExpanded) },
                    leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = purityExpanded,
                    onDismissRequest = { purityExpanded = false }
                ) {
                    GOLD_PURITIES.forEach { purity ->
                        DropdownMenuItem(
                            text = { Text(purity) },
                            onClick = {
                                selectedPurity = purity
                                purityExpanded = false
                            }
                        )
                    }
                }
            }

            // Storage location
            OutlinedTextField(
                value = storageLocation,
                onValueChange = { storageLocation = it },
                label = { Text("Storage Location") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                placeholder = { Text("e.g. Bank locker, Home safe") }
            )

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

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull()
                    val price = pricePerGramText.toDoubleOrNull()

                    if (qty == null || qty <= 0) {
                        quantityError = "Enter a valid quantity"
                        return@Button
                    }
                    if (price == null || price <= 0) {
                        priceError = "Enter a valid price"
                        return@Button
                    }

                    val gold = GoldInvestmentEntity(
                        date = date,
                        goldType = selectedGoldType,
                        quantityGrams = qty,
                        pricePerGram = price,
                        totalAmount = qty * price,
                        purity = selectedPurity,
                        storageLocation = storageLocation.takeIf { it.isNotBlank() },
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    viewModel.addGoldInvestment(gold)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = quantityText.isNotBlank() && pricePerGramText.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Investment")
            }
        }
    }
}
