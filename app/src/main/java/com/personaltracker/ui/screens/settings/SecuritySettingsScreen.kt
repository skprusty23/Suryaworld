package com.personaltracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.security.PinManager
import com.personaltracker.ui.components.PTTopBar

@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog visibility flags
    var showChangeAppPinDialog by remember { mutableStateOf(false) }
    var showInvestmentPinDialog by remember { mutableStateOf(false) }
    var showCredentialsPinDialog by remember { mutableStateOf(false) }
    var showTimeoutMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Security Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── App PIN ───────────────────────────────────────────────────
                SecuritySectionHeader("App PIN")

                SecurityActionCard(
                    title = "Change App PIN",
                    subtitle = "Update the PIN used to unlock the app",
                    icon = Icons.Default.Lock,
                    buttonLabel = "Change PIN",
                    onClick = { showChangeAppPinDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Module PINs ───────────────────────────────────────────────
                SecuritySectionHeader("Module PINs")

                SecurityActionCard(
                    title = "Investment Vault PIN",
                    subtitle = if (state.investmentPinSet) "Custom PIN is set" else "Using app PIN (tap to set a separate PIN)",
                    icon = Icons.Default.Security,
                    buttonLabel = if (state.investmentPinSet) "Change PIN" else "Set PIN",
                    onClick = { showInvestmentPinDialog = true }
                )

                SecurityActionCard(
                    title = "Credentials Vault PIN",
                    subtitle = if (state.credentialsPinSet) "Custom PIN is set" else "Using app PIN (tap to set a separate PIN)",
                    icon = Icons.Default.LockOpen,
                    buttonLabel = if (state.credentialsPinSet) "Change PIN" else "Set PIN",
                    onClick = { showCredentialsPinDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Biometric ─────────────────────────────────────────────────
                SecuritySectionHeader("Biometric Authentication")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (state.biometricAvailable)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                "Fingerprint / Face Unlock",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (state.biometricAvailable) "Available on this device"
                                else "Not available on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.biometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometric(it) },
                        enabled = state.biometricAvailable
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Session Timeout ───────────────────────────────────────────
                SecuritySectionHeader("Session Timeout")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text(
                                "Auto-lock after",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Require PIN after inactivity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { showTimeoutMenu = true }) {
                            Text(state.selectedTimeout.label)
                        }
                        DropdownMenu(
                            expanded = showTimeoutMenu,
                            onDismissRequest = { showTimeoutMenu = false }
                        ) {
                            SessionTimeout.entries.forEach { timeout ->
                                DropdownMenuItem(
                                    text = { Text(timeout.label) },
                                    onClick = {
                                        viewModel.setSessionTimeout(timeout)
                                        showTimeoutMenu = false
                                    },
                                    trailingIcon = {
                                        if (timeout == state.selectedTimeout) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showChangeAppPinDialog) {
        ChangePinDialog(
            title = "Change App PIN",
            requireCurrentPin = true,
            onConfirm = { current, newPin ->
                viewModel.changeAppPin(current, newPin)
                showChangeAppPinDialog = false
            },
            onDismiss = { showChangeAppPinDialog = false }
        )
    }

    if (showInvestmentPinDialog) {
        SetModulePinDialog(
            title = "Investment Vault PIN",
            onConfirm = { pin ->
                viewModel.setModulePin(PinManager.PinModule.INVESTMENT, pin)
                showInvestmentPinDialog = false
            },
            onDismiss = { showInvestmentPinDialog = false }
        )
    }

    if (showCredentialsPinDialog) {
        SetModulePinDialog(
            title = "Credentials Vault PIN",
            onConfirm = { pin ->
                viewModel.setModulePin(PinManager.PinModule.CREDENTIALS, pin)
                showCredentialsPinDialog = false
            },
            onDismiss = { showCredentialsPinDialog = false }
        )
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun SecuritySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SecurityActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = onClick) {
            Text(buttonLabel)
        }
    }
}

@Composable
private fun ChangePinDialog(
    title: String,
    requireCurrentPin: Boolean,
    onConfirm: (currentPin: String, newPin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (requireCurrentPin) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 6) currentPin = it },
                        label = { Text("Current PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = visualTransformation,
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6) {
                            newPin = it
                            pinError = null
                        }
                    },
                    label = { Text("New PIN (4-6 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    isError = pinError != null,
                    trailingIcon = {
                        IconButton(onClick = { showPasswords = !showPasswords }) {
                            Icon(
                                if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPasswords) "Hide PIN" else "Show PIN"
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6) {
                            confirmPin = it
                            pinError = null
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    isError = pinError != null,
                    supportingText = pinError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    newPin.length < 4 -> pinError = "PIN must be at least 4 digits"
                    newPin != confirmPin -> pinError = "PINs do not match"
                    else -> onConfirm(currentPin, newPin)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SetModulePinDialog(
    title: String,
    onConfirm: (pin: String) -> Unit,
    onDismiss: () -> Unit
) {
    ChangePinDialog(
        title = title,
        requireCurrentPin = false,
        onConfirm = { _, newPin -> onConfirm(newPin) },
        onDismiss = onDismiss
    )
}
