package com.personaltracker.ui.screens.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.personaltracker.data.database.entity.DocumentEntity
import com.personaltracker.domain.repository.DocumentRepository
import com.personaltracker.security.SecurityManager
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

// ─── ViewModel ──────────────────────────────────────────────────────────────

data class AddDocumentState(
    val name: String = "",
    val documentType: String = "Aadhaar",
    val category: String = "Personal",
    val documentNumber: String = "",
    val issuedBy: String = "",
    val issuedDateText: String = "",
    val expiryDateText: String = "",
    val personName: String = "",
    val notes: String = "",
    val imageUri: Uri? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AddDocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _state = MutableStateFlow(AddDocumentState())
    val state: StateFlow<AddDocumentState> = _state.asStateFlow()

    fun onNameChange(v: String)           { _state.value = _state.value.copy(name = v) }
    fun onDocumentTypeChange(v: String)   { _state.value = _state.value.copy(documentType = v) }
    fun onCategoryChange(v: String)       { _state.value = _state.value.copy(category = v) }
    fun onDocumentNumberChange(v: String) { _state.value = _state.value.copy(documentNumber = v) }
    fun onIssuedByChange(v: String)       { _state.value = _state.value.copy(issuedBy = v) }
    fun onIssuedDateChange(v: String)     { _state.value = _state.value.copy(issuedDateText = v) }
    fun onExpiryDateChange(v: String)     { _state.value = _state.value.copy(expiryDateText = v) }
    fun onPersonNameChange(v: String)     { _state.value = _state.value.copy(personName = v) }
    fun onNotesChange(v: String)          { _state.value = _state.value.copy(notes = v) }
    fun onImageSelected(uri: Uri?)        { _state.value = _state.value.copy(imageUri = uri) }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun parseDate(text: String): LocalDate? {
        if (text.isBlank()) return null
        return try { LocalDate.parse(text.trim(), dateFormatter) } catch (e: DateTimeParseException) { null }
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        if (s.name.isBlank()) errors["name"] = "Name is required"
        if (s.issuedDateText.isNotBlank() && parseDate(s.issuedDateText) == null)
            errors["issuedDate"] = "Use format dd/MM/yyyy"
        if (s.expiryDateText.isNotBlank() && parseDate(s.expiryDateText) == null)
            errors["expiryDate"] = "Use format dd/MM/yyyy"

        if (errors.isNotEmpty()) {
            _state.value = s.copy(validationErrors = errors)
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, validationErrors = emptyMap())
            try {
                val entity = DocumentEntity(
                    name           = s.name.trim(),
                    documentType   = s.documentType,
                    category       = s.category,
                    documentNumber = s.documentNumber.trim().ifBlank { null },
                    issuedBy       = s.issuedBy.trim().ifBlank { null },
                    issuedDate     = parseDate(s.issuedDateText),
                    expiryDate     = parseDate(s.expiryDateText),
                    personName     = s.personName.trim().ifBlank { null },
                    notes          = s.notes.trim().ifBlank { null },
                    fileUri        = s.imageUri?.toString()
                )
                documentRepository.insertDocument(entity)
                _state.value = _state.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}

// ─── Screen ─────────────────────────────────────────────────────────────────

private val DOCUMENT_TYPES = listOf(
    "Aadhaar", "PAN", "Passport", "DL", "Insurance", "Vehicle", "Birth Certificate", "Custom"
)
private val DOC_CATEGORIES = listOf("Personal", "Family", "Financial", "Vehicle", "Medical")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    onBack: () -> Unit,
    viewModel: AddDocumentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var showTypeDropdown     by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> viewModel.onImageSelected(uri) }

    // Navigate back on successful save
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBack()
    }

    Scaffold(
        topBar = {
            PTTopBar(title = "Add Document", onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Document Info")

            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Document Name *") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                isError = state.validationErrors.containsKey("name"),
                supportingText = state.validationErrors["name"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Document Type Dropdown
            ExposedDropdownMenuBox(
                expanded = showTypeDropdown,
                onExpandedChange = { showTypeDropdown = it }
            ) {
                OutlinedTextField(
                    value = state.documentType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Document Type") },
                    leadingIcon = { Icon(Icons.Default.Category, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    DOCUMENT_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.onDocumentTypeChange(type)
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = { Icon(Icons.Default.Folder, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    DOC_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.onCategoryChange(cat)
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            SectionHeader("Details")

            // Document Number
            OutlinedTextField(
                value = state.documentNumber,
                onValueChange = viewModel::onDocumentNumberChange,
                label = { Text("Document Number") },
                leadingIcon = { Icon(Icons.Default.Tag, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Person Name
            OutlinedTextField(
                value = state.personName,
                onValueChange = viewModel::onPersonNameChange,
                label = { Text("Person Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Issued By
            OutlinedTextField(
                value = state.issuedBy,
                onValueChange = viewModel::onIssuedByChange,
                label = { Text("Issued By") },
                leadingIcon = { Icon(Icons.Default.AccountBalance, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Issued Date
            OutlinedTextField(
                value = state.issuedDateText,
                onValueChange = viewModel::onIssuedDateChange,
                label = { Text("Issued Date (dd/MM/yyyy)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                isError = state.validationErrors.containsKey("issuedDate"),
                supportingText = state.validationErrors["issuedDate"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Expiry Date
            OutlinedTextField(
                value = state.expiryDateText,
                onValueChange = viewModel::onExpiryDateChange,
                label = { Text("Expiry Date (dd/MM/yyyy)") },
                leadingIcon = { Icon(Icons.Default.Event, null) },
                isError = state.validationErrors.containsKey("expiryDate"),
                supportingText = state.validationErrors["expiryDate"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SectionHeader("Notes")

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines = 4
            )

            SectionHeader("Document Scan")

            // Image picker
            if (state.imageUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = state.imageUri,
                            contentDescription = "Document scan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.onImageSelected(null) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image",
                                tint = Color.White)
                        }
                    }
                }
            } else {
                OutlinedCard(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to add document scan", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White,
                        strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Document", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
    HorizontalDivider()
}
