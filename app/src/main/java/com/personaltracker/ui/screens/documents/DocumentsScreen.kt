package com.personaltracker.ui.screens.documents

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.data.database.entity.DocumentEntity
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTSearchBar
import com.personaltracker.ui.components.PTTopBar
import java.time.format.DateTimeFormatter

private val DOCUMENT_CATEGORIES = listOf("All", "Personal", "Family", "Financial", "Vehicle", "Medical")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DocumentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var documentToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Documents",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            PTSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search documents...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DOCUMENT_CATEGORIES) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category) },
                        leadingIcon = if (uiState.selectedCategory == category) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Document list
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.documents.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Description,
                    message = if (uiState.searchQuery.isNotBlank()) "No documents match your search"
                              else "No documents yet. Tap + to add one.",
                    actionLabel = if (uiState.searchQuery.isBlank()) "Add Document" else null,
                    onAction = if (uiState.searchQuery.isBlank()) onNavigateToAdd else null
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.documents, key = { it.id }) { document ->
                        DocumentCard(
                            document = document,
                            isExpiringSoon = viewModel.isExpiringSoon(document),
                            isExpired = viewModel.isExpired(document),
                            onClick = { onNavigateToDetail(document.id) },
                            onDelete = { documentToDelete = document }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Delete confirmation dialog
    documentToDelete?.let { doc ->
        ConfirmDeleteDialog(
            title = "Delete Document",
            message = "Delete \"${doc.name}\"? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteDocument(doc)
                documentToDelete = null
            },
            onDismiss = { documentToDelete = null }
        )
    }

    // Error snackbar
    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentCard(
    document: DocumentEntity,
    isExpiringSoon: Boolean,
    isExpired: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document type icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = categoryColor(document.category).copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = documentTypeIcon(document.documentType),
                            contentDescription = null,
                            tint = categoryColor(document.category),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${document.documentType} • ${document.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                document.personName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                document.expiryDate?.let { expiry ->
                    Spacer(Modifier.height(4.dp))
                    val (badgeColor, badgeText) = when {
                        isExpired -> MaterialTheme.colorScheme.error to "Expired ${expiry.format(dateFormatter)}"
                        isExpiringSoon -> Color(0xFFF57C00) to "Expires ${expiry.format(dateFormatter)}"
                        else -> MaterialTheme.colorScheme.tertiary to "Valid till ${expiry.format(dateFormatter)}"
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExpired || isExpiringSoon) Icons.Default.Warning else Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun categoryColor(category: String): Color = when (category) {
    "Personal"   -> Color(0xFF1976D2)
    "Family"     -> Color(0xFF388E3C)
    "Financial"  -> Color(0xFF7B1FA2)
    "Vehicle"    -> Color(0xFFE64A19)
    "Medical"    -> Color(0xFFC62828)
    else         -> Color(0xFF455A64)
}

private fun documentTypeIcon(type: String) = when (type) {
    "Aadhaar"           -> Icons.Default.Badge
    "PAN"               -> Icons.Default.CreditCard
    "Passport"          -> Icons.Default.Book
    "DL"                -> Icons.Default.DirectionsCar
    "Insurance"         -> Icons.Default.Shield
    "Vehicle"           -> Icons.Default.DirectionsCar
    "Birth Certificate" -> Icons.Default.ChildCare
    else                -> Icons.Default.Description
}
