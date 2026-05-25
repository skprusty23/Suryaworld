package com.personaltracker.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.components.PTTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

data class MemberInput(val name: String = "", val phone: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: GroupExpensesViewModel = hiltViewModel()
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var startDateText by remember { mutableStateOf(LocalDate.now().format(DATE_FORMATTER)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var members by remember { mutableStateOf(listOf(MemberInput(), MemberInput())) }

    var nameError by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.toEpochDay() * 86_400_000L
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                        startDateText = startDate.format(DATE_FORMATTER)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Create Group", onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Group Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Group Name
            OutlinedTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                    nameError = if (it.isBlank()) "Group name is required" else null
                },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                placeholder = { Text("e.g. Goa Trip 2025, Office Lunch") }
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                maxLines = 3
            )

            // Start Date
            OutlinedTextField(
                value = startDateText,
                onValueChange = {},
                label = { Text("Start Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                    }
                },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
            )

            HorizontalDivider()

            // Members section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = { members = members + MemberInput() }
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Member")
                }
            }

            members.forEachIndexed { index, member ->
                MemberInputRow(
                    index = index,
                    member = member,
                    canRemove = members.size > 1,
                    onNameChange = { newName ->
                        members = members.toMutableList().also { it[index] = it[index].copy(name = newName) }
                    },
                    onPhoneChange = { newPhone ->
                        members = members.toMutableList().also { it[index] = it[index].copy(phone = newPhone) }
                    },
                    onRemove = {
                        members = members.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        nameError = "Group name is required"
                        return@Button
                    }
                    val validMembers = members.filter { it.name.isNotBlank() }
                    if (validMembers.isEmpty()) return@Button

                    viewModel.createGroup(
                        name = groupName.trim(),
                        description = description.takeIf { it.isNotBlank() },
                        startDate = startDate,
                        members = validMembers.map { it.name.trim() to it.phone.trim().takeIf { p -> p.isNotBlank() } }
                    )
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank() && members.any { it.name.isNotBlank() }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Group")
            }
        }
    }
}

@Composable
private fun MemberInputRow(
    index: Int,
    member: MemberInput,
    canRemove: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Member ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            OutlinedTextField(
                value = member.name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            OutlinedTextField(
                value = member.phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone (optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
        }
    }
}
