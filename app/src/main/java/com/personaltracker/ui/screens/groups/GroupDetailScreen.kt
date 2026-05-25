package com.personaltracker.ui.screens.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.ExpenseGroupEntity
import com.personaltracker.data.database.entity.GroupExpenseEntity
import com.personaltracker.data.database.entity.GroupMemberEntity
import com.personaltracker.domain.repository.GroupExpenseRepository
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

// ---- Data models for split calculation ----

data class MemberBalance(
    val member: GroupMemberEntity,
    val paid: Double,
    val share: Double,
    val balance: Double  // positive = should receive, negative = should pay
)

data class Settlement(
    val fromMember: GroupMemberEntity,
    val toMember: GroupMemberEntity,
    val amount: Double
)

data class GroupDetailUiState(
    val group: ExpenseGroupEntity? = null,
    val members: List<GroupMemberEntity> = emptyList(),
    val expenses: List<GroupExpenseEntity> = emptyList(),
    val memberBalances: List<MemberBalance> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

// ---- ViewModel ----

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupExpenseRepository: GroupExpenseRepository
) : ViewModel() {

    private val groupId: Long = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        loadGroup()
        loadData()
    }

    private fun loadGroup() {
        viewModelScope.launch {
            val group = groupExpenseRepository.getGroupById(groupId)
            _uiState.update { it.copy(group = group) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                groupExpenseRepository.getMembersForGroup(groupId),
                groupExpenseRepository.getExpensesForGroup(groupId),
                groupExpenseRepository.getTotalForGroup(groupId)
            ) { members, expenses, total ->
                Triple(members, expenses, total)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { (members, expenses, total) ->
                val balances = calculateBalances(members, expenses)
                val settlements = calculateSettlements(balances)
                _uiState.update {
                    it.copy(
                        members = members,
                        expenses = expenses,
                        memberBalances = balances,
                        settlements = settlements,
                        totalAmount = total ?: 0.0,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun calculateBalances(
        members: List<GroupMemberEntity>,
        expenses: List<GroupExpenseEntity>
    ): List<MemberBalance> {
        if (members.isEmpty()) return emptyList()

        val totalExpense = expenses.sumOf { it.amount }
        val equalShare = if (members.isNotEmpty()) totalExpense / members.size else 0.0

        val paidByMember = mutableMapOf<Long, Double>()
        members.forEach { paidByMember[it.id] = 0.0 }
        expenses.forEach { expense ->
            paidByMember[expense.paidByMemberId] = (paidByMember[expense.paidByMemberId] ?: 0.0) + expense.amount
        }

        return members.map { member ->
            val paid = paidByMember[member.id] ?: 0.0
            val balance = paid - equalShare
            MemberBalance(
                member = member,
                paid = paid,
                share = equalShare,
                balance = balance
            )
        }
    }

    private fun calculateSettlements(balances: List<MemberBalance>): List<Settlement> {
        val settlements = mutableListOf<Settlement>()
        val creditors = balances.filter { it.balance > 0.01 }.sortedByDescending { it.balance }.toMutableList()
        val debtors = balances.filter { it.balance < -0.01 }.sortedBy { it.balance }.toMutableList()

        var ci = 0
        var di = 0

        while (ci < creditors.size && di < debtors.size) {
            val creditorBalance = creditors[ci].balance
            val debtorBalance = abs(debtors[di].balance)
            val amount = minOf(creditorBalance, debtorBalance)

            if (amount > 0.01) {
                settlements.add(
                    Settlement(
                        fromMember = debtors[di].member,
                        toMember = creditors[ci].member,
                        amount = amount
                    )
                )
            }

            if (creditorBalance <= debtorBalance) ci++ else di++
            if (creditorBalance >= debtorBalance) di++ else ci++

            // Prevent double increment when equal
            if (creditorBalance == debtorBalance) {
                // both already incremented
            }
        }

        return settlements
    }

    fun addExpense(
        paidByMemberId: Long,
        amount: Double,
        description: String,
        category: String,
        date: LocalDate
    ) {
        viewModelScope.launch {
            try {
                val expense = GroupExpenseEntity(
                    groupId = groupId,
                    paidByMemberId = paidByMemberId,
                    amount = amount,
                    description = description,
                    category = category,
                    date = date
                )
                groupExpenseRepository.insertGroupExpense(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteExpense(expense: GroupExpenseEntity) {
        viewModelScope.launch {
            try {
                groupExpenseRepository.deleteGroupExpense(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

// ---- Constants ----

private val GroupPurple = Color(0xFF6A1B9A)
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val GROUP_EXPENSE_CATEGORIES = listOf("Food", "Transport", "Hotel", "Activities", "Shopping", "General", "Other")

// ---- Screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    if (showAddExpenseDialog) {
        AddGroupExpenseDialog(
            members = uiState.members,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { paidById, amount, description, category, date ->
                viewModel.addExpense(paidById, amount, description, category, date)
                showAddExpenseDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = uiState.group?.name ?: "Group",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expense") },
                containerColor = GroupPurple,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Group header
            uiState.group?.let { group ->
                item { GroupDetailHeader(group = group, totalAmount = uiState.totalAmount) }
            }

            // Members & Balances section
            if (uiState.memberBalances.isNotEmpty()) {
                item {
                    Text(
                        "Members & Balances",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.memberBalances, key = { it.member.id }) { balance ->
                    MemberBalanceCard(balance = balance)
                }
            }

            // Settlement section
            if (uiState.settlements.isNotEmpty()) {
                item {
                    Text(
                        "Settlements",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.settlements) { settlement ->
                    SettlementCard(settlement = settlement)
                }
            } else if (uiState.memberBalances.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Text("All settled up!", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            // Expenses section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Expenses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.expenses.size} entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (uiState.expenses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        EmptyState(
                            icon = Icons.Default.Receipt,
                            message = "No expenses yet",
                            actionLabel = "Add Expense",
                            onAction = { showAddExpenseDialog = true }
                        )
                    }
                }
            } else {
                items(uiState.expenses, key = { it.id }) { expense ->
                    GroupExpenseCard(
                        expense = expense,
                        members = uiState.members,
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupDetailHeader(group: ExpenseGroupEntity, totalAmount: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GroupPurple),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.name.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column {
                    Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    if (!group.description.isNullOrBlank()) {
                        Text(group.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(formatCurrency(totalAmount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(group.startDate.format(DISPLAY_DATE_FORMATTER), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun MemberBalanceCard(balance: MemberBalance) {
    val isPositive = balance.balance >= 0
    val balanceColor = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GroupPurple.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = balance.member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GroupPurple
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(balance.member.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Paid: ${formatCurrency(balance.paid)}  |  Share: ${formatCurrency(balance.share)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+${formatCurrency(balance.balance)}" else "-${formatCurrency(abs(balance.balance))}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
                Text(
                    text = if (isPositive) "gets back" else "owes",
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SettlementCard(settlement: Settlement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                settlement.fromMember.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                settlement.toMember.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E7D32),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatCurrency(settlement.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = GroupPurple
            )
        }
    }
}

@Composable
private fun GroupExpenseCard(
    expense: GroupExpenseEntity,
    members: List<GroupMemberEntity>,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val paidBy = members.find { it.id == expense.paidByMemberId }?.name ?: "Unknown"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Remove '${expense.description}'?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GroupPurple.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = GroupPurple, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Paid by $paidBy  •  ${expense.date.format(DISPLAY_DATE_FORMATTER)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(expense.category, style = MaterialTheme.typography.labelSmall, color = GroupPurple.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(expense.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GroupPurple)
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ---- Add Expense Dialog ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupExpenseDialog(
    members: List<GroupMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (paidById: Long, amount: Double, description: String, category: String, date: LocalDate) -> Unit
) {
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var memberExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GROUP_EXPENSE_CATEGORIES[5]) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DATE_FORMATTER)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var amountError by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Group Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Paid by
                if (members.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                        OutlinedTextField(
                            value = selectedMember?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Paid By") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name) },
                                    onClick = { selectedMember = member; memberExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = if (it.isNotBlank() && it.toDoubleOrNull() == null) "Invalid" else null
                    },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹") },
                    isError = amountError != null,
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true
                )

                // Category
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        GROUP_EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; categoryExpanded = false })
                        }
                    }
                }

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val memberId = selectedMember?.id
                    if (amount == null || amount <= 0 || memberId == null || description.isBlank()) return@Button
                    onSave(memberId, amount, description.trim(), selectedCategory, date)
                },
                enabled = amountText.isNotBlank() && description.isNotBlank() && selectedMember != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
