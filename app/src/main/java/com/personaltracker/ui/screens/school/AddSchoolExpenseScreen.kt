package com.personaltracker.ui.screens.school

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
import com.personaltracker.data.database.entity.SchoolExpenseEntity
import com.personaltracker.ui.components.PTTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SCHOOL_CATEGORIES = listOf(
    "Fees", "Tuition", "Books", "Uniform", "Transportation", "Activity", "Other"
)
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSchoolExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SchoolViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Form state
    var childName by remember { mutableStateOf("") }
    var childNameExpanded by remember { mutableStateOf(false) }
    var isNewChild by remember { mutableStateOf(false) }
    var newChildName by remember { mutableStateOf("") }

    var schoolName by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf(SCHOOL_CATEGORIES[0]) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DATE_FORMATTER)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedYear by remember { mutableStateOf(SchoolViewModel.currentAcademicYear()) }
    var yearExpanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add School Expense", onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Child Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Child name — existing or new
            if (uiState.children.isNotEmpty() && !isNewChild) {
                ExposedDropdownMenuBox(
                    expanded = childNameExpanded,
                    onExpandedChange = { childNameExpanded = it }
                ) {
                    OutlinedTextField(
                        value = childName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Child Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = childNameExpanded) },
                        leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = childNameExpanded,
                        onDismissRequest = { childNameExpanded = false }
                    ) {
                        uiState.children.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    childName = name
                                    childNameExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Add New Child") },
                            onClick = {
                                isNewChild = true
                                childNameExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = newChildName,
                    onValueChange = { newChildName = it },
                    label = { Text("Child Name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    trailingIcon = if (uiState.children.isNotEmpty()) {
                        {
                            TextButton(onClick = { isNewChild = false; newChildName = "" }) {
                                Text("Existing")
                            }
                        }
                    } else null
                )
            }

            // School name
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            HorizontalDivider()
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
                    SCHOOL_CATEGORIES.forEach { cat ->
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

            // Academic Year dropdown
            ExposedDropdownMenuBox(
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Academic Year") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    SchoolViewModel.buildAcademicYears().forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year) },
                            onClick = { selectedYear = year; yearExpanded = false }
                        )
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
                    val amount = amountText.toDoubleOrNull()
                    val finalChild = if (isNewChild || uiState.children.isEmpty()) newChildName.trim() else childName.trim()

                    if (finalChild.isBlank()) return@Button
                    if (amount == null || amount <= 0) {
                        amountError = "Enter a valid amount"
                        return@Button
                    }

                    val expense = SchoolExpenseEntity(
                        childName = finalChild,
                        schoolName = schoolName.takeIf { it.isNotBlank() },
                        category = selectedCategory,
                        amount = amount,
                        date = date,
                        academicYear = selectedYear,
                        description = description.takeIf { it.isNotBlank() },
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    viewModel.addSchoolExpense(expense)
                    onSaved()
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
