package com.personaltracker.ui.screens.groups

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import java.time.format.DateTimeFormatter

private val GroupPurple = Color(0xFF6A1B9A)
private val GroupLightPurple = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupExpensesScreen(
    onNavigateToAddGroup: () -> Unit,
    onNavigateToGroupDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GroupExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { PTTopBar(title = "Group Expenses", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddGroup,
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                text = { Text("New Group") },
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
            // Summary header
            item {
                GroupSummaryBanner(
                    totalGroups = uiState.groups.size,
                    activeGroups = uiState.groups.count { !it.group.isSettled }
                )
            }

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Groups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.groups.size} groups", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (uiState.groups.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        EmptyState(
                            icon = Icons.Default.Group,
                            message = "No groups yet\nCreate one to split expenses",
                            actionLabel = "Create Group",
                            onAction = onNavigateToAddGroup
                        )
                    }
                }
            } else {
                items(uiState.groups, key = { it.group.id }) { groupSummary ->
                    GroupCard(
                        groupSummary = groupSummary,
                        onClick = { onNavigateToGroupDetail(groupSummary.group.id) },
                        onDelete = { viewModel.deleteGroup(groupSummary.group) },
                        onSettle = { viewModel.settleGroup(groupSummary.group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSummaryBanner(totalGroups: Int, activeGroups: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GroupPurple),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Group Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Split bills with friends & family", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalGroups", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$activeGroups", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Active", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupSummary: GroupWithSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSettle: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val group = groupSummary.group
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Delete '${group.name}'? All expenses will be removed.") },
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
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GroupPurple.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.name.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GroupPurple
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!group.description.isNullOrBlank()) {
                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row {
                    if (!group.isSettled) {
                        IconButton(onClick = onSettle, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Settle", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Members count
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${groupSummary.memberCount} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        group.startDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(groupSummary.totalAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GroupPurple
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(if (group.isSettled) "Settled" else "Active", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (group.isSettled) Color(0xFF4CAF50).copy(alpha = 0.15f) else GroupPurple.copy(alpha = 0.15f),
                            labelColor = if (group.isSettled) Color(0xFF2E7D32) else GroupPurple
                        )
                    )
                }
            }
        }
    }
}
