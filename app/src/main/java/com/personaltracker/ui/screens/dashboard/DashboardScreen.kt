package com.personaltracker.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.EmiEntity
import com.personaltracker.data.database.entity.ExpenseEntity
import com.personaltracker.ui.components.SummaryCard
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.navigation.NavRoutes
import com.personaltracker.ui.theme.EmiOrange
import com.personaltracker.ui.theme.ExpenseRed
import com.personaltracker.ui.theme.GoldColor
import com.personaltracker.ui.theme.InvestmentPurple
import com.personaltracker.ui.theme.SecondaryTeal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Data model for module grid items
// ---------------------------------------------------------------------------

private data class ModuleItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

private val modules = listOf(
    ModuleItem("Documents", Icons.Default.Description, Color(0xFF1565C0), NavRoutes.DOCUMENTS),
    ModuleItem("Credentials", Icons.Default.Badge, Color(0xFF00897B), NavRoutes.CREDENTIALS),
    ModuleItem("Expenses", Icons.Default.ShoppingCart, ExpenseRed, NavRoutes.EXPENSES),
    ModuleItem("Investments", Icons.Default.TrendingUp, InvestmentPurple, NavRoutes.INVESTMENTS),
    ModuleItem("EMI", Icons.Default.CreditCard, EmiOrange, NavRoutes.EMI),
    ModuleItem("Gold", Icons.Default.Star, GoldColor, NavRoutes.GOLD),
    ModuleItem("School", Icons.Default.School, Color(0xFF00ACC1), NavRoutes.SCHOOL),
    ModuleItem("Travel", Icons.Default.Flight, Color(0xFF558B2F), NavRoutes.TRAVEL),
    ModuleItem("Groups", Icons.Default.Group, Color(0xFF6D4C41), NavRoutes.GROUP_EXPENSES),
    ModuleItem("Reports", Icons.Default.Assessment, Color(0xFF37474F), NavRoutes.EXPENSE_REPORTS)
)

// ---------------------------------------------------------------------------
// Bottom nav items
// ---------------------------------------------------------------------------

private data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, NavRoutes.DASHBOARD),
    BottomNavItem("Expenses", Icons.Default.ShoppingCart, NavRoutes.EXPENSES),
    BottomNavItem("Invest", Icons.Default.TrendingUp, NavRoutes.INVESTMENTS),
    BottomNavItem("More", Icons.Default.MoreHoriz, NavRoutes.SETTINGS)
)

// ---------------------------------------------------------------------------
// DashboardScreen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.dashboardState.collectAsState()
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showLockDialog by remember { mutableStateOf(false) }

    val greeting = remember { buildGreeting() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "SuryaWorld",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(NavRoutes.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showLockDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock App",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedNavIndex == index,
                        onClick = {
                            selectedNavIndex = index
                            if (item.route != NavRoutes.DASHBOARD) {
                                onNavigate(item.route)
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(NavRoutes.ADD_EXPENSE) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                text = { Text("Add Expense") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Summary cards row
                item {
                    SummaryCardsRow(state = state)
                }

                // Module grid
                item {
                    SectionHeader(title = "Modules")
                    ModuleGrid(modules = modules, onNavigate = onNavigate)
                }

                // Recent expenses
                item {
                    SectionHeader(title = "Recent Expenses")
                }
                if (state.recentExpenses.isEmpty()) {
                    item {
                        EmptyPlaceholder(message = "No recent expenses")
                    }
                } else {
                    items(state.recentExpenses) { expense ->
                        ExpenseListItem(expense = expense)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                // Upcoming EMIs
                item {
                    SectionHeader(title = "Upcoming EMIs")
                }
                if (state.upcomingEmis.isEmpty()) {
                    item {
                        EmptyPlaceholder(message = "No upcoming EMIs in the next 7 days")
                    }
                } else {
                    items(state.upcomingEmis) { emi ->
                        EmiReminderItem(emi = emi)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // ── Lock confirmation dialog ──────────────────────────────────────────────
    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Lock App?") },
            text = {
                Text("You will be returned to the PIN screen. Your data will not be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLockDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Summary cards (horizontal scroll row)
// ---------------------------------------------------------------------------

@Composable
private fun SummaryCardsRow(state: DashboardState) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryCard(
                title = "Monthly Expenses",
                amount = state.monthlyExpenses,
                color = ExpenseRed,
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.size(width = 180.dp, height = 110.dp)
            )
        }
        item {
            SummaryCard(
                title = "Investments",
                amount = state.totalInvestments,
                color = InvestmentPurple,
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.size(width = 180.dp, height = 110.dp)
            )
        }
        item {
            SummaryCard(
                title = "Active EMIs",
                amount = state.activeEmiTotal,
                color = EmiOrange,
                icon = Icons.Default.CreditCard,
                modifier = Modifier.size(width = 180.dp, height = 110.dp),
                subtitle = "Monthly total"
            )
        }
        item {
            SummaryCard(
                title = "Gold",
                amount = state.totalGoldGrams,
                color = Color(0xFFB8860B),
                icon = Icons.Default.Star,
                modifier = Modifier.size(width = 180.dp, height = 110.dp),
                subtitle = "Total grams"
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Module 2-column grid
// ---------------------------------------------------------------------------

@Composable
private fun ModuleGrid(
    modules: List<ModuleItem>,
    onNavigate: (String) -> Unit
) {
    val rows = modules.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { module ->
                    ModuleGridCard(
                        module = module,
                        onClick = { onNavigate(module.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ModuleGridCard(
    module: ModuleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(module.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = module.title,
                    tint = module.color,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = module.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Recent expense row
// ---------------------------------------------------------------------------

@Composable
private fun ExpenseListItem(expense: ExpenseEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ExpenseRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = expense.description ?: expense.date.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = formatCurrency(expense.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ExpenseRed
        )
    }
}

// ---------------------------------------------------------------------------
// Upcoming EMI reminder row
// ---------------------------------------------------------------------------

@Composable
private fun EmiReminderItem(emi: EmiEntity) {
    val today = LocalDate.now()
    val safeDay = minOf(emi.dueDay, today.lengthOfMonth())
    val dueDate = today.withDayOfMonth(safeDay)
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
    val dueDateStr = dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmiOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = EmiOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = emi.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Due $dueDateStr · ${if (daysLeft == 0L) "Today" else "in $daysLeft days"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysLeft <= 2) EmiOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = formatCurrency(emi.emiAmount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = EmiOrange
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun EmptyPlaceholder(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private fun buildGreeting(): String {
    return when (LocalDate.now().let { java.time.LocalTime.now().hour }) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
