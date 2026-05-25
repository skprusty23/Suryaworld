package com.personaltracker.ui.screens.investments

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
import com.personaltracker.data.database.entity.InvestmentEntity
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.InvestmentPurple
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun investmentTypeIcon(type: String): ImageVector = when (type) {
    "LIC" -> Icons.Default.Shield
    "Mutual Fund" -> Icons.Default.TrendingUp
    "SIP" -> Icons.Default.Repeat
    "FD" -> Icons.Default.AccountBalance
    "RD" -> Icons.Default.Savings
    "Stocks" -> Icons.Default.ShowChart
    "PPF" -> Icons.Default.AccountBalanceWallet
    "Bonds" -> Icons.Default.CreditCard
    else -> Icons.Default.AttachMoney
}

fun investmentTypeColor(type: String): Color = when (type) {
    "LIC" -> Color(0xFF1565C0)
    "Mutual Fund" -> Color(0xFF00897B)
    "SIP" -> Color(0xFF7B1FA2)
    "FD" -> Color(0xFFE65100)
    "RD" -> Color(0xFF558B2F)
    "Stocks" -> Color(0xFFE53935)
    "PPF" -> Color(0xFF37474F)
    "Bonds" -> Color(0xFF00ACC1)
    else -> Color(0xFF6D4C41)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onNavigateToAddInvestment: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var investmentToDelete by remember { mutableStateOf<InvestmentEntity?>(null) }

    Scaffold(
        topBar = {
            PTTopBar(title = "Investments", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddInvestment,
                containerColor = InvestmentPurple
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Investment", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InvestmentSummaryCard(
                    label = "Invested",
                    value = formatCurrency(uiState.totalInvested),
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
                InvestmentSummaryCard(
                    label = "Current Value",
                    value = formatCurrency(uiState.totalCurrentValue),
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
                InvestmentSummaryCard(
                    label = "ROI",
                    value = "%.1f%%".format(uiState.roiPercent),
                    icon = Icons.Default.Percent,
                    color = if (uiState.roiPercent >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )
            }

            // Type Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InvestmentsViewModel.INVESTMENT_TYPES) { type ->
                    FilterChip(
                        selected = uiState.selectedType == type,
                        onClick = { viewModel.filterByType(type) },
                        label = { Text(type) },
                        leadingIcon = if (type != "All") {
                            { Icon(investmentTypeIcon(type), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.investments.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AccountBalance,
                    message = "No investments found",
                    actionLabel = "Add Investment",
                    onAction = onNavigateToAddInvestment
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.investments, key = { it.id }) { investment ->
                        InvestmentCard(
                            investment = investment,
                            onClick = { onNavigateToDetail(investment.id) },
                            onDelete = { investmentToDelete = investment }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    investmentToDelete?.let { inv ->
        ConfirmDeleteDialog(
            title = "Delete Investment",
            message = "Delete \"${inv.name}\"? This cannot be undone.",
            onConfirm = {
                viewModel.deleteInvestment(inv)
                investmentToDelete = null
            },
            onDismiss = { investmentToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentCard(
    investment: InvestmentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = investmentTypeColor(investment.investmentType)
    val today = LocalDate.now()
    val isMaturing = investment.maturityDate?.let {
        !it.isBefore(today) && ChronoUnit.DAYS.between(today, it) <= 90
    } ?: false

    Card(
        onClick = onClick,
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = investmentTypeIcon(investment.investmentType),
                    contentDescription = investment.investmentType,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = investment.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    InvestmentTypeBadge(type = investment.investmentType, color = color)
                }
                Text(
                    text = investment.providerName ?: investment.investmentType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                investment.maturityDate?.let { maturity ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isMaturing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Matures: ${maturity.format(DATE_FMT)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMaturing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(investment.principalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InvestmentPurple
                )
                investment.currentValue?.let { cv ->
                    val gain = cv - investment.principalAmount
                    Text(
                        text = (if (gain >= 0) "+" else "") + formatCurrency(gain),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gain >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                    )
                }
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
private fun InvestmentSummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InvestmentTypeBadge(type: String, color: Color) {
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
