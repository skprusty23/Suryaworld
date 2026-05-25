package com.personaltracker.ui.screens.credentials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.data.database.entity.CredentialEntity
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.EmptyState
import com.personaltracker.ui.components.PTSearchBar
import com.personaltracker.ui.components.PTTopBar

private val CREDENTIAL_CATEGORIES = listOf("All", "Website", "Bank", "Insurance", "Subscription", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CredentialsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var credentialToDelete by remember { mutableStateOf<CredentialEntity?>(null) }

    // PIN gate
    if (uiState.requiresPin && !uiState.isUnlocked) {
        PinEntryDialog(
            pinError = uiState.pinError,
            onPinSubmit = viewModel::verifyPin,
            onDismiss = onBack
        )
        return
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Credentials",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Credential", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PTSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search credentials...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter tabs
            ScrollableTabRow(
                selectedTabIndex = CREDENTIAL_CATEGORIES.indexOf(uiState.selectedCategory)
                    .coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {}
            ) {
                CREDENTIAL_CATEGORIES.forEachIndexed { index, category ->
                    Tab(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        text = {
                            Text(
                                category,
                                fontWeight = if (uiState.selectedCategory == category)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            HorizontalDivider()

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.credentials.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Lock,
                        message = if (uiState.searchQuery.isNotBlank())
                            "No credentials match your search"
                        else "No credentials yet. Tap + to add one.",
                        actionLabel = if (uiState.searchQuery.isBlank()) "Add Credential" else null,
                        onAction = if (uiState.searchQuery.isBlank()) onNavigateToAdd else null
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.credentials, key = { it.id }) { credential ->
                            CredentialCard(
                                credential = credential,
                                onClick = { onNavigateToDetail(credential.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(credential) },
                                onDelete = { credentialToDelete = credential }
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    credentialToDelete?.let { cred ->
        ConfirmDeleteDialog(
            title = "Delete Credential",
            message = "Delete \"${cred.name}\"? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteCredential(cred)
                credentialToDelete = null
            },
            onDismiss = { credentialToDelete = null }
        )
    }

    uiState.errorMessage?.let {
        LaunchedEffect(it) { viewModel.clearError() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialCard(
    credential: CredentialEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = categoryColor(credential.category).copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryIcon(credential.category),
                        contentDescription = null,
                        tint = categoryColor(credential.category),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = credential.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (credential.isFavorite) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favourite",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = credential.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = categoryColor(credential.category)
                )
                credential.username?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Always show masked password indicator
                Text(
                    text = "••••••••",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Actions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (credential.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle favourite",
                        tint = if (credential.isFavorite) Color(0xFFFFC107)
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinEntryDialog(
    pinError: String?,
    onPinSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Enter PIN", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    "Credentials vault is protected. Enter your PIN to continue.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = pinError != null,
                    supportingText = pinError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPinSubmit(pin) },
                enabled = pin.isNotBlank()
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun categoryColor(category: String): Color = when (category) {
    "Website"      -> Color(0xFF1565C0)
    "Bank"         -> Color(0xFF2E7D32)
    "Insurance"    -> Color(0xFF6A1B9A)
    "Subscription" -> Color(0xFFE64A19)
    else           -> Color(0xFF455A64)
}

private fun categoryIcon(category: String) = when (category) {
    "Website"      -> Icons.Default.Language
    "Bank"         -> Icons.Default.AccountBalance
    "Insurance"    -> Icons.Default.Shield
    "Subscription" -> Icons.Default.Subscriptions
    else           -> Icons.Default.Key
}
