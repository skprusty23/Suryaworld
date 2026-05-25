package com.personaltracker.ui.screens.gold

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.GoldInvestmentEntity
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.GoldColor
import java.time.format.DateTimeFormatter

private val GoldTypes = listOf("All", "Physical", "Digital", "Jewelry")
private val GOLD_GRADIENT = listOf(Color(0xFFFFD600), Color(0xFFFFB300))
private val GOLD_DARK = Color(0xFF8B6914)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldScreen(
    onNavigateToAddGold: () -> Unit,
    onBack: () -> Unit,
    viewModel: GoldViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PTTopBar(title = "Gold Investments", onBack = onBack)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddGold,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Gold") },
                containerColor = GoldColor,
                contentColor = GOLD_DARK
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Summary cards
            item {
                GoldSummarySection(
                    totalGrams = uiState.totalGrams,
                    totalInvested = uiState.totalInvested
                )
            }

            // Type filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GoldTypes) { type ->
                        FilterChip(
                            selected = uiState.selectedType == type,
                            onClick = { viewModel.setTypeFilter(type) },
                            label = { Text(type) },
                            leadingIcon = if (uiState.selectedType == type) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // List header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.filteredList.size} entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Gold investment cards
            if (uiState.filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        EmptyState(
                            icon = Icons.Default.Diamond,
                            message = "No gold investments yet",
                            actionLabel = "Add Gold",
                            onAction = onNavigateToAddGold
                        )
                    }
                }
            } else {
                items(uiState.filteredList, key = { it.id }) { gold ->
                    GoldInvestmentCard(
                        gold = gold,
                        onDelete = { viewModel.deleteGoldInvestment(gold) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoldSummarySection(
    totalGrams: Double,
    totalInvested: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total invested banner
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(GOLD_GRADIENT))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Invested",
                            style = MaterialTheme.typography.labelLarge,
                            color = GOLD_DARK.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatCurrency(totalInvested),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = GOLD_DARK
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Quantity",
                            style = MaterialTheme.typography.labelLarge,
                            color = GOLD_DARK.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "%.2f g".format(totalGrams),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GOLD_DARK
                        )
                    }
                }
            }
        }

        // Avg price per gram
        if (totalGrams > 0) {
            val avgPrice = totalInvested / totalGrams
            SurfaceCard(
                label = "Avg. Price / Gram",
                value = formatCurrency(avgPrice),
                icon = Icons.Default.TrendingUp
            )
        }
    }
}

@Composable
private fun SurfaceCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = GoldColor, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldInvestmentCard(
    gold: GoldInvestmentEntity,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry") },
            text = { Text("Remove this gold investment entry?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(GoldColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = GoldColor, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = gold.goldType,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = gold.date.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(gold.purity, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GoldColor.copy(alpha = 0.15f),
                            labelColor = GOLD_DARK
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // Details grid
            Row(modifier = Modifier.fillMaxWidth()) {
                GoldDetailItem(label = "Quantity", value = "%.3f g".format(gold.quantityGrams), modifier = Modifier.weight(1f))
                GoldDetailItem(label = "Price/gram", value = "₹%.2f".format(gold.pricePerGram), modifier = Modifier.weight(1f))
                GoldDetailItem(
                    label = "Total",
                    value = formatCurrency(gold.totalAmount),
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
            }

            // Storage location
            if (!gold.storageLocation.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(gold.storageLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Notes
            if (!gold.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = gold.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoldDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) GoldColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
