package com.personaltracker.ui.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.SchoolExpenseEntity
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import java.time.format.DateTimeFormatter

private val SchoolBlue = Color(0xFF1565C0)
private val SchoolLightBlue = Color(0xFF42A5F5)

private val CategoryIcons: Map<String, ImageVector> = mapOf(
    "Fees" to Icons.Default.Receipt,
    "Tuition" to Icons.Default.School,
    "Books" to Icons.Default.MenuBook,
    "Uniform" to Icons.Default.Checkroom,
    "Transport" to Icons.Default.DirectionsBus,
    "Activities" to Icons.Default.SportsScore,
    "Other" to Icons.Default.MoreHoriz
)

private val CategoryColors: Map<String, Color> = mapOf(
    "Fees" to Color(0xFF1565C0),
    "Tuition" to Color(0xFF7B1FA2),
    "Books" to Color(0xFF00897B),
    "Uniform" to Color(0xFFE65100),
    "Transport" to Color(0xFF00ACC1),
    "Activities" to Color(0xFF558B2F),
    "Other" to Color(0xFF6D4C41)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolScreen(
    onNavigateToAddSchoolExpense: () -> Unit,
    onBack: () -> Unit,
    viewModel: SchoolViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { PTTopBar(title = "School Expenses", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddSchoolExpense,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expense") },
                containerColor = SchoolBlue,
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
            // Child selector tabs
            if (uiState.children.size > 1) {
                item {
                    ScrollableTabRow(
                        selectedTabIndex = uiState.children.indexOf(uiState.selectedChild).coerceAtLeast(0),
                        edgePadding = 16.dp
                    ) {
                        uiState.children.forEach { child ->
                            Tab(
                                selected = child == uiState.selectedChild,
                                onClick = { viewModel.selectChild(child) },
                                text = { Text(child) },
                                icon = { Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            } else if (uiState.children.size == 1 && uiState.selectedChild != uiState.children.first()) {
                item {
                    LaunchedEffect(uiState.children) {
                        viewModel.selectChild(uiState.children.first())
                    }
                }
            }

            // Year filter
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableYears) { year ->
                        FilterChip(
                            selected = year == uiState.selectedYear,
                            onClick = { viewModel.selectYear(year) },
                            label = { Text(year) },
                            leadingIcon = if (year == uiState.selectedYear) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Summary card
            item {
                SchoolSummaryCard(
                    childName = uiState.selectedChild,
                    year = uiState.selectedYear,
                    total = uiState.totalAmount
                )
            }

            // Category breakdown
            if (uiState.categoryTotals.isNotEmpty()) {
                item {
                    CategoryBreakdownSection(categoryTotals = uiState.categoryTotals)
                }
            }

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Expenses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.filteredExpenses.size} entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Expense list
            if (uiState.filteredExpenses.isEmpty()) {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)) {
                        EmptyState(
                            icon = Icons.Default.School,
                            message = "No school expenses for this selection",
                            actionLabel = "Add Expense",
                            onAction = onNavigateToAddSchoolExpense
                        )
                    }
                }
            } else {
                items(uiState.filteredExpenses, key = { it.id }) { expense ->
                    SchoolExpenseCard(
                        expense = expense,
                        onDelete = { viewModel.deleteSchoolExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolSummaryCard(
    childName: String,
    year: String,
    total: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SchoolBlue, SchoolLightBlue)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
                    Text(
                        text = if (childName.isNotBlank()) childName else "All Children",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Academic Year $year",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = formatCurrency(total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownSection(categoryTotals: Map<String, Double>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Category Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val grandTotal = categoryTotals.values.sum()
        categoryTotals.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
            val fraction = if (grandTotal > 0) (amount / grandTotal).toFloat() else 0f
            val icon = CategoryIcons[category] ?: Icons.Default.MoreHoriz
            val color = CategoryColors[category] ?: Color(0xFF6D4C41)

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                            Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = formatCurrency(amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolExpenseCard(
    expense: SchoolExpenseEntity,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val icon = CategoryIcons[expense.category] ?: Icons.Default.MoreHoriz
    val color = CategoryColors[expense.category] ?: Color(0xFF6D4C41)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Remove this school expense?") },
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (!expense.description.isNullOrBlank()) {
                    Text(expense.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(expense.date.format(dateFormatter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!expense.schoolName.isNullOrBlank()) {
                    Text(expense.schoolName, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
