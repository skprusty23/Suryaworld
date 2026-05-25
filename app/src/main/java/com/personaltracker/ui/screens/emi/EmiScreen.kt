package com.personaltracker.ui.screens.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.EmiEntity
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.EmiOrange
import java.time.format.DateTimeFormatter

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiScreen(
    onNavigateToAddEmi: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EmiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var emiToDelete by remember { mutableStateOf<EmiEntity?>(null) }

    Scaffold(
        topBar = {
            PTTopBar(title = "EMI Tracker", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddEmi,
                containerColor = EmiOrange
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add EMI", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Total Monthly EMI Summary
            EmiSummaryHeader(totalMonthly = uiState.totalMonthlyEmi)

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.emis.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.CreditScore,
                    message = "No active EMIs",
                    actionLabel = "Add EMI",
                    onAction = onNavigateToAddEmi
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.emis, key = { it.emi.id }) { emiWithStatus ->
                        EmiCard(
                            emiWithStatus = emiWithStatus,
                            onClick = { onNavigateToDetail(emiWithStatus.emi.id) },
                            onDelete = { emiToDelete = emiWithStatus.emi }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    emiToDelete?.let { emi ->
        ConfirmDeleteDialog(
            title = "Delete EMI",
            message = "Delete \"${emi.name}\"? All payment records will also be deleted.",
            onConfirm = {
                viewModel.deleteEmi(emi)
                emiToDelete = null
            },
            onDismiss = { emiToDelete = null }
        )
    }
}

@Composable
private fun EmiSummaryHeader(totalMonthly: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmiOrange.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Monthly EMI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatCurrency(totalMonthly),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmiOrange
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EmiOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CreditScore,
                    contentDescription = null,
                    tint = EmiOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmiCard(
    emiWithStatus: EmiWithStatus,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val emi = emiWithStatus.emi
    var showMenu by remember { mutableStateOf(false) }
    val progress = (emiWithStatus.paidMonths.toFloat() / emi.tenureMonths.toFloat()).coerceIn(0f, 1f)
    val remainingMonths = emi.tenureMonths - emiWithStatus.paidMonths

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmiOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = EmiOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = emi.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (emiWithStatus.isOverdue) {
                            OverdueBadge()
                        }
                    }
                    Text(
                        text = emi.lenderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(emi.emiAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmiOrange
                    )
                    Text(
                        text = "/month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            Spacer(Modifier.height(10.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${emiWithStatus.paidMonths}/${emi.tenureMonths} months paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$remainingMonths remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmiOrange
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmiOrange,
                    trackColor = EmiOrange.copy(alpha = 0.15f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                emiWithStatus.nextDueDate?.let { dueDate ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Due: ${dueDate.format(DATE_FMT)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (emiWithStatus.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Loan: ${formatCurrency(emi.loanAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverdueBadge() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "OVERDUE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
