package com.personaltracker.ui.screens.travel

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
import com.personaltracker.data.database.entity.TripEntity
import com.personaltracker.ui.components.PTTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TravelViewModel = hiltViewModel()
) {
    var tripName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var startDateText by remember { mutableStateOf(LocalDate.now().format(DATE_FORMATTER)) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDateText by remember { mutableStateOf("") }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var budgetText by remember { mutableStateOf("") }
    var budgetError by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.toEpochDay() * 86_400_000L
    )
    val endDatePickerState = rememberDatePickerState()

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        startDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                        startDateText = startDate.format(DATE_FORMATTER)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = startDatePickerState) }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        endDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                        endDateText = endDate!!.format(DATE_FORMATTER)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = endDatePickerState) }
    }

    Scaffold(
        topBar = { PTTopBar(title = "New Trip", onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Trip Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Trip Name
            OutlinedTextField(
                value = tripName,
                onValueChange = {
                    tripName = it
                    nameError = if (it.isBlank()) "Trip name is required" else null
                },
                label = { Text("Trip Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Luggage, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                placeholder = { Text("e.g. Summer Vacation 2025") }
            )

            // Destination
            OutlinedTextField(
                value = destination,
                onValueChange = {
                    destination = it
                    destinationError = if (it.isBlank()) "Destination is required" else null
                },
                label = { Text("Destination") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = destinationError != null,
                supportingText = destinationError?.let { { Text(it) } },
                placeholder = { Text("e.g. Goa, Paris, Kerala") }
            )

            HorizontalDivider()
            Text("Dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Start Date
            OutlinedTextField(
                value = startDateText,
                onValueChange = {},
                label = { Text("Start Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick start date")
                    }
                },
                leadingIcon = { Icon(Icons.Default.FlightTakeoff, contentDescription = null) }
            )

            // End Date
            OutlinedTextField(
                value = endDateText,
                onValueChange = {},
                label = { Text("End Date (optional)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Row {
                        if (endDate != null) {
                            IconButton(onClick = { endDate = null; endDateText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear end date")
                            }
                        }
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick end date")
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Default.FlightLand, contentDescription = null) },
                placeholder = { Text("Select return date") }
            )

            HorizontalDivider()
            Text("Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Budget
            OutlinedTextField(
                value = budgetText,
                onValueChange = {
                    budgetText = it
                    budgetError = if (it.isNotBlank() && it.toDoubleOrNull() == null) "Enter a valid amount" else null
                },
                label = { Text("Budget (₹) — optional") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                prefix = { Text("₹") },
                isError = budgetError != null,
                supportingText = budgetError?.let { { Text(it) } },
                placeholder = { Text("Set a travel budget") }
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

            Button(
                onClick = {
                    if (tripName.isBlank()) { nameError = "Trip name is required"; return@Button }
                    if (destination.isBlank()) { destinationError = "Destination is required"; return@Button }

                    val budget = budgetText.toDoubleOrNull()
                    if (budgetText.isNotBlank() && budget == null) { budgetError = "Enter a valid amount"; return@Button }

                    val trip = TripEntity(
                        name = tripName.trim(),
                        destination = destination.trim(),
                        startDate = startDate,
                        endDate = endDate,
                        budget = budget,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    viewModel.addTrip(trip)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = tripName.isNotBlank() && destination.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Trip")
            }
        }
    }
}
