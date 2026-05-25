package com.personaltracker.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.ExpenseEntity
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.ExpenseRed
import java.time.format.DateTimeFormatter

private val MONTH_DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM yyyy")
private val DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun categoryIcon(category: String): ImageVector = when (category) {
    "House Rent" -> Icons.Default.Home
    "Grocery" -> Icons.Default.ShoppingCart
    "Vegetables" -> Icons.Default.Eco
    "Food" -> Icons.Default.Restaurant
    "Fuel" -> Icons.Default.LocalGasStation
    "Shopping" -> Icons.Default.ShoppingBag
    "Utilities" -> Icons.Default.ElectricBolt
    "Medical" -> Icons.Default.MedicalServices
    else -> Icons.Default.AttachMoney
}

fun categoryColor(category: String): Color = when (category) {
    "House Rent" -> Color(0xFF1565C0)
    "Grocery" -> Color(0xFF00897B)
    "Vegetables" -> Color(0xFF558B2F)
    "Food" -> Color(0xFFE53935)
    "Fuel" -> Color(0xFFE65100)
    "Shopping" -> Color(0xFF7B1FA2)
    "Utilities" -> Color(0xFF00ACC1)
    "Medical" -> Color(0xFFD81B60)
    else -> Color(0xFF37474F)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToReports: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Expenses",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToReports) {
                        Icon(Icons.Default.BarChart, contentDescription = "Reports")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Selector + Total Header
            MonthSelectorHeader(
                displayMonth = uiState.selectedMonth.format(MONTH_DISPLAY_FMT),
                monthTotal = uiState.monthTotal,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ExpensesViewModel.DEFAULT_CATEGORIES) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.filterByCategory(category) },
                        label = { Text(category) },
                        leadingIcon = if (category != "All") {
                            { Icon(categoryIcon(category), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.expenses.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    message = "No expenses for this month",
                    actionLabel = "Add Expense",
                    onAction = onNavigateToAddExpense
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseCard(
                            expense = expense,
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    expenseToDelete?.let { expense ->
        ConfirmDeleteDialog(
            title = "Delete Expense",
            message = "Delete \"${expense.description ?: expense.category}\"? This cannot be undone.",
            onConfirm = {
                viewModel.deleteExpense(expense)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

@Composable
private fun MonthSelectorHeader(
    displayMonth: String,
    monthTotal: Double,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = displayMonth,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = formatCurrency(monthTotal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = categoryColor(expense.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description ?: expense.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                    if (expense.subCategory != null) {
                        Text(
                            text = "• ${expense.subCategory}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = expense.date.format(DATE_FMT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
                ExpenseTypeBadge(type = expense.expenseType)
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseTypeBadge(type: String) {
    val color = if (type == "Home") Color(0xFF1565C0) else Color(0xFF00897B)
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
