package com.personaltracker.ui.screens.credentials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.CredentialEntity
import com.personaltracker.domain.repository.CredentialRepository
import com.personaltracker.security.SecurityManager
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class CredentialDetailState(
    val credential: CredentialEntity? = null,
    val decryptedPassword: String = "",
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CredentialDetailViewModel @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val securityManager: SecurityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val credentialId: Long = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(CredentialDetailState())
    val state: StateFlow<CredentialDetailState> = _state.asStateFlow()

    init {
        loadCredential()
    }

    private fun loadCredential() {
        viewModelScope.launch {
            try {
                val cred = credentialRepository.getCredentialById(credentialId)
                val decrypted = if (cred?.passwordEncrypted != null) {
                    try { securityManager.decrypt(cred.passwordEncrypted) }
                    catch (e: Exception) { "" }
                } else ""
                _state.value = CredentialDetailState(
                    credential = cred,
                    decryptedPassword = decrypted,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = CredentialDetailState(isLoading = false, error = e.message)
            }
        }
    }

    fun delete() {
        val cred = _state.value.credential ?: return
        viewModelScope.launch {
            try {
                credentialRepository.deleteCredential(cred)
                _state.value = _state.value.copy(isDeleted = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to delete: ${e.message}")
            }
        }
    }

    fun toggleFavorite() {
        val cred = _state.value.credential ?: return
        viewModelScope.launch {
            try {
                val updated = cred.copy(isFavorite = !cred.isFavorite)
                credentialRepository.updateCredential(updated)
                _state.value = _state.value.copy(credential = updated)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to update: ${e.message}")
            }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit = {},
    viewModel: CredentialDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Apply FLAG_SECURE to prevent screenshots of this sensitive screen
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Credential Details",
                onBack = onBack,
                actions = {
                    state.credential?.let { cred ->
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (cred.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle favourite",
                                tint = if (cred.isFavorite) Color(0xFFFFC107) else Color.White
                            )
                        }
                        IconButton(onClick = { onEdit(cred.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.credential == null -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Credential not found", style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                val cred = state.credential!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Key,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                cred.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                cred.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            if (cred.isFavorite) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null,
                                        tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Favourite", style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFC107))
                                }
                            }
                        }
                    }

                    // Details card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            // Username with copy
                            cred.username?.takeIf { it.isNotBlank() }?.let { username ->
                                CredDetailRowWithCopy(
                                    icon = Icons.Default.Person,
                                    label = "Username",
                                    value = username,
                                    onCopy = {
                                        copyToClipboard(context, "Username", username)
                                        scope.launch { snackbarHostState.showSnackbar("Username copied") }
                                    }
                                )
                            }

                            // Password with show/hide and copy
                            if (state.decryptedPassword.isNotEmpty()) {
                                CredPasswordRow(
                                    password = state.decryptedPassword,
                                    showPassword = showPassword,
                                    onToggleVisibility = { showPassword = !showPassword },
                                    onCopy = {
                                        copyToClipboard(context, "Password", state.decryptedPassword)
                                        scope.launch { snackbarHostState.showSnackbar("Password copied") }
                                    }
                                )
                            }

                            cred.email?.takeIf { it.isNotBlank() }?.let {
                                CredDetailRowWithCopy(
                                    icon = Icons.Default.Email,
                                    label = "Email",
                                    value = it,
                                    onCopy = {
                                        copyToClipboard(context, "Email", it)
                                        scope.launch { snackbarHostState.showSnackbar("Email copied") }
                                    }
                                )
                            }

                            cred.url?.takeIf { it.isNotBlank() }?.let {
                                CredDetailRow(Icons.Default.Language, "URL", it)
                            }

                            cred.phone?.takeIf { it.isNotBlank() }?.let {
                                CredDetailRowWithCopy(
                                    icon = Icons.Default.Phone,
                                    label = "Phone",
                                    value = it,
                                    onCopy = {
                                        copyToClipboard(context, "Phone", it)
                                        scope.launch { snackbarHostState.showSnackbar("Phone copied") }
                                    }
                                )
                            }

                            cred.accountNumber?.takeIf { it.isNotBlank() }?.let {
                                CredDetailRowWithCopy(
                                    icon = Icons.Default.CreditCard,
                                    label = "Account Number",
                                    value = it,
                                    onCopy = {
                                        copyToClipboard(context, "Account Number", it)
                                        scope.launch { snackbarHostState.showSnackbar("Account number copied") }
                                    }
                                )
                            }

                            cred.notes?.takeIf { it.isNotBlank() }?.let {
                                CredDetailRow(Icons.Default.Notes, "Notes", it)
                            }

                            CredDetailRow(
                                icon = Icons.Default.Schedule,
                                label = "Added",
                                value = cred.createdAt.format(
                                    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Credential",
            message = "Delete \"${state.credential?.name}\"? This cannot be undone.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ─── Helper composables ──────────────────────────────────────────────────────

@Composable
private fun CredDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp)
}

@Composable
private fun CredDetailRowWithCopy(
    icon: ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label",
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp)
}

@Composable
private fun CredPasswordRow(
    password: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Password", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (showPassword) password else "•".repeat(password.length.coerceAtMost(16)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = if (showPassword) null
                             else androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        IconButton(onClick = onToggleVisibility, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showPassword) "Hide password" else "Show password",
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy password",
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp)
}

// ─── Utility functions ───────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
