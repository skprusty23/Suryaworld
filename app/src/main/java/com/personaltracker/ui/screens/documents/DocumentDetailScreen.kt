package com.personaltracker.ui.screens.documents

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.personaltracker.data.database.entity.DocumentEntity
import com.personaltracker.domain.repository.DocumentRepository
import com.personaltracker.security.SecurityManager
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class DocumentDetailState(
    val document: DocumentEntity? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val securityManager: SecurityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val documentId: Long = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(DocumentDetailState())
    val state: StateFlow<DocumentDetailState> = _state.asStateFlow()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            try {
                val doc = documentRepository.getDocumentById(documentId)
                _state.value = DocumentDetailState(document = doc, isLoading = false)
            } catch (e: Exception) {
                _state.value = DocumentDetailState(isLoading = false, error = e.message)
            }
        }
    }

    fun delete() {
        val doc = _state.value.document ?: return
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(doc)
                _state.value = _state.value.copy(isDeleted = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to delete: ${e.message}")
            }
        }
    }

    fun isExpiringSoon(): Boolean {
        val expiry = _state.value.document?.expiryDate ?: return false
        val today = LocalDate.now()
        return expiry.isAfter(today) && expiry.isBefore(today.plusDays(90))
    }

    fun isExpired(): Boolean {
        val expiry = _state.value.document?.expiryDate ?: return false
        return expiry.isBefore(LocalDate.now())
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit = {},
    viewModel: DocumentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    // Navigate back after delete
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Document Details",
                onBack = onBack,
                actions = {
                    state.document?.let { doc ->
                        IconButton(onClick = { onEdit(doc.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.document == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Document not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
            else -> {
                val doc = state.document!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Expiry warning banner
                    when {
                        viewModel.isExpired() -> {
                            ExpiryBanner(
                                message = "This document has expired",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                icon = Icons.Default.Warning
                            )
                        }
                        viewModel.isExpiringSoon() -> {
                            ExpiryBanner(
                                message = "Expiring soon — ${doc.expiryDate?.format(dateFormatter)}",
                                containerColor = Color(0xFFFFF3E0),
                                contentColor = Color(0xFFE65100),
                                icon = Icons.Default.AccessTime
                            )
                        }
                    }

                    // Document image
                    doc.fileUri?.let { uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Document image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Header card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                doc.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${doc.documentType} • ${doc.category}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Details section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            doc.personName?.let {
                                DetailRow(Icons.Default.Person, "Person", it)
                            }
                            doc.documentNumber?.let {
                                DetailRow(Icons.Default.Tag, "Document Number", it)
                            }
                            doc.issuedBy?.let {
                                DetailRow(Icons.Default.AccountBalance, "Issued By", it)
                            }
                            doc.issuedDate?.let {
                                DetailRow(Icons.Default.CalendarToday, "Issued Date", it.format(dateFormatter))
                            }
                            doc.expiryDate?.let {
                                val color = when {
                                    viewModel.isExpired() -> MaterialTheme.colorScheme.error
                                    viewModel.isExpiringSoon() -> Color(0xFFE65100)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                DetailRow(Icons.Default.Event, "Expiry Date", it.format(dateFormatter), valueColor = color)
                            }
                            doc.notes?.let {
                                DetailRow(Icons.Default.Notes, "Notes", it)
                            }
                            DetailRow(
                                Icons.Default.Schedule, "Added",
                                doc.createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
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
            title = "Delete Document",
            message = "Delete \"${state.document?.name}\"? This cannot be undone.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun ExpiryBanner(
    message: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector
) {
    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = contentColor, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 1.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = valueColor)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp)
}
