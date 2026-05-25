package com.personaltracker.ui.screens.credentials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.CredentialEntity
import com.personaltracker.domain.repository.CredentialRepository
import com.personaltracker.security.SecurityManager
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Password strength ───────────────────────────────────────────────────────

enum class PasswordStrength(val label: String, val color: Color, val fraction: Float) {
    WEAK("Weak", Color(0xFFD32F2F), 0.25f),
    FAIR("Fair", Color(0xFFF57C00), 0.50f),
    GOOD("Good", Color(0xFF388E3C), 0.75f),
    STRONG("Strong", Color(0xFF1565C0), 1.00f)
}

fun evaluatePasswordStrength(password: String): PasswordStrength? {
    if (password.isEmpty()) return null
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }) score++
    return when (score) {
        0, 1 -> PasswordStrength.WEAK
        2    -> PasswordStrength.FAIR
        3    -> PasswordStrength.GOOD
        else -> PasswordStrength.STRONG
    }
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class AddCredentialState(
    val name: String = "",
    val category: String = "Website",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val email: String = "",
    val phone: String = "",
    val accountNumber: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AddCredentialViewModel @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _state = MutableStateFlow(AddCredentialState())
    val state: StateFlow<AddCredentialState> = _state.asStateFlow()

    fun onNameChange(v: String)          { _state.value = _state.value.copy(name = v) }
    fun onCategoryChange(v: String)      { _state.value = _state.value.copy(category = v) }
    fun onUsernameChange(v: String)      { _state.value = _state.value.copy(username = v) }
    fun onPasswordChange(v: String)      { _state.value = _state.value.copy(password = v) }
    fun onUrlChange(v: String)           { _state.value = _state.value.copy(url = v) }
    fun onEmailChange(v: String)         { _state.value = _state.value.copy(email = v) }
    fun onPhoneChange(v: String)         { _state.value = _state.value.copy(phone = v) }
    fun onAccountNumberChange(v: String) { _state.value = _state.value.copy(accountNumber = v) }
    fun onNotesChange(v: String)         { _state.value = _state.value.copy(notes = v) }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        if (s.name.isBlank()) errors["name"] = "Name is required"

        if (errors.isNotEmpty()) {
            _state.value = s.copy(validationErrors = errors)
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, validationErrors = emptyMap())
            try {
                val encryptedPassword = if (s.password.isNotBlank())
                    securityManager.encrypt(s.password) else null

                val entity = CredentialEntity(
                    name              = s.name.trim(),
                    category          = s.category,
                    username          = s.username.trim().ifBlank { null },
                    passwordEncrypted = encryptedPassword,
                    url               = s.url.trim().ifBlank { null },
                    email             = s.email.trim().ifBlank { null },
                    phone             = s.phone.trim().ifBlank { null },
                    accountNumber     = s.accountNumber.trim().ifBlank { null },
                    notes             = s.notes.trim().ifBlank { null }
                )
                credentialRepository.insertCredential(entity)
                _state.value = _state.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

private val CREDENTIAL_CATEGORIES = listOf(
    "Website", "Bank", "Insurance", "Subscription", "Note", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialScreen(
    onBack: () -> Unit,
    viewModel: AddCredentialViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val passwordStrength = remember(state.password) {
        evaluatePasswordStrength(state.password)
    }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBack()
    }

    Scaffold(
        topBar = { PTTopBar(title = "Add Credential", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CredSectionHeader("Basic Info")

            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name *") },
                leadingIcon = { Icon(Icons.Default.Label, null) },
                isError = state.validationErrors.containsKey("name"),
                supportingText = state.validationErrors["name"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                    leadingIcon = { Icon(Icons.Default.Category, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    CREDENTIAL_CATEGORIES.forEach { cat ->
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

            CredSectionHeader("Login Info")

            // Username
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password with show/hide
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password strength indicator
            if (state.password.isNotEmpty() && passwordStrength != null) {
                PasswordStrengthIndicator(strength = passwordStrength)
            }

            CredSectionHeader("Contact & Additional Info")

            // Email
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // URL
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("URL / Website") },
                leadingIcon = { Icon(Icons.Default.Language, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Phone
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Account Number
            OutlinedTextField(
                value = state.accountNumber,
                onValueChange = viewModel::onAccountNumberChange,
                label = { Text("Account Number") },
                leadingIcon = { Icon(Icons.Default.CreditCard, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            CredSectionHeader("Notes")

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Credential", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Password strength", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(strength.label, style = MaterialTheme.typography.labelSmall,
                color = strength.color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { strength.fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = strength.color,
            trackColor = strength.color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun CredSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
    HorizontalDivider()
}
