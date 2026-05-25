package com.personaltracker.ui.screens.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.data.database.entity.TripEntity
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.navigation.NavRoutes
import java.time.format.DateTimeFormatter

private val TravelBlue = Color(0xFF0277BD)
private val TravelCyan = Color(0xFF00BCD4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScreen(
    onNavigateToAddTrip: () -> Unit,
    onNavigateToTripDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TravelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { PTTopBar(title = "Travel", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddTrip,
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                text = { Text("New Trip") },
                containerColor = TravelBlue,
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
            // Active trips header
            if (uiState.activeTrips.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF4CAF50), shape = RoundedCornerShape(50))
                                )
                                Text(
                                    "Active Trips",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "${uiState.activeTrips.size} ongoing",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        // Horizontal scroll active trips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.activeTrips.forEach { trip ->
                                ActiveTripCard(
                                    trip = trip,
                                    onClick = { onNavigateToTripDetail(trip.id) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            // All trips section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Trips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.trips.size} trips", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (uiState.trips.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        EmptyState(
                            icon = Icons.Default.Flight,
                            message = "No trips yet\nStart planning your next adventure!",
                            actionLabel = "Add Trip",
                            onAction = onNavigateToAddTrip
                        )
                    }
                }
            } else {
                items(uiState.trips, key = { it.id }) { trip ->
                    TripCard(
                        trip = trip,
                        onClick = { onNavigateToTripDetail(trip.id) },
                        onDelete = { viewModel.deleteTrip(trip) },
                        onMarkCompleted = { viewModel.markTripCompleted(trip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTripCard(
    trip: TripEntity,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(TravelBlue, TravelCyan)))
                .padding(16.dp)
        ) {
            Column {
                Icon(Icons.Default.Flight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    Text(
                        text = "${trip.startDate.format(dateFormatter)}${trip.endDate?.let { " – ${it.format(dateFormatter)}" } ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                if (trip.budget != null) {
                    Text(
                        text = "Budget: ${formatCurrency(trip.budget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(
    trip: TripEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkCompleted: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip") },
            text = { Text("Remove '${trip.name}'?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(TravelBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Flight, contentDescription = null, tint = TravelBlue, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trip.destination,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = trip.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row {
                    if (!trip.isCompleted) {
                        IconButton(onClick = onMarkCompleted, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Mark completed", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Dates row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${trip.startDate.format(dateFormatter)}${trip.endDate?.let { " – ${it.format(dateFormatter)}" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trip.isCompleted) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Completed", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            labelColor = Color(0xFF2E7D32)
                        )
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("Active", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = TravelBlue.copy(alpha = 0.15f),
                            labelColor = TravelBlue
                        )
                    )
                }
            }

            if (trip.budget != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(trip.budget),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TravelBlue
                    )
                }
            }
        }
    }
}
