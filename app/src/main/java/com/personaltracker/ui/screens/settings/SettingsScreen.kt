package com.personaltracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.BuildConfig
import com.personaltracker.ui.components.PTListItem
import com.personaltracker.ui.components.PTTopBar

@Composable
fun SettingsScreen(
    onNavigateToSecurity: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val expenseReminderEnabled by viewModel.expenseReminderEnabled.collectAsState()
    val emiReminderEnabled by viewModel.emiReminderEnabled.collectAsState()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // ── Security Section ──────────────────────────────────────────────
            SettingsSectionHeader("Security")

            PTListItem(
                title = "Change PIN",
                subtitle = "Update your app unlock PIN",
                leadingIcon = Icons.Default.Lock,
                leadingIconColor = MaterialTheme.colorScheme.error,
                onClick = onNavigateToSecurity
            )

            PTListItem(
                title = "Module PINs",
                subtitle = "Separate PINs for Investment & Credentials vaults",
                leadingIcon = Icons.Default.Security,
                leadingIconColor = MaterialTheme.colorScheme.error,
                onClick = onNavigateToSecurity
            )

            SettingsToggleItem(
                title = "Biometric Authentication",
                subtitle = "Use fingerprint or face unlock",
                icon = Icons.Default.Fingerprint,
                iconColor = MaterialTheme.colorScheme.error,
                checked = biometricEnabled,
                onCheckedChange = { viewModel.toggleBiometric(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Data Section ──────────────────────────────────────────────────
            SettingsSectionHeader("Data")

            PTListItem(
                title = "Backup",
                subtitle = "Create or restore a local encrypted backup",
                leadingIcon = Icons.Default.Backup,
                leadingIconColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToBackup
            )

            PTListItem(
                title = "Restore",
                subtitle = "Restore data from a previous backup",
                leadingIcon = Icons.Default.Restore,
                leadingIconColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToBackup
            )

            PTListItem(
                title = "Export All",
                subtitle = "Export all data as an encrypted backup file",
                leadingIcon = Icons.Default.Share,
                leadingIconColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToBackup
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Notifications Section ─────────────────────────────────────────
            SettingsSectionHeader("Notifications")

            SettingsToggleItem(
                title = "Expense Reminder",
                subtitle = "Daily reminder at 10 PM to log expenses",
                icon = Icons.Default.Notifications,
                iconColor = MaterialTheme.colorScheme.tertiary,
                checked = expenseReminderEnabled,
                onCheckedChange = { viewModel.toggleExpenseReminder(it) }
            )

            SettingsToggleItem(
                title = "EMI Reminders",
                subtitle = "Notified when EMI due dates are approaching",
                icon = Icons.Default.CreditCard,
                iconColor = MaterialTheme.colorScheme.tertiary,
                checked = emiReminderEnabled,
                onCheckedChange = { viewModel.toggleEmiReminder(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── About Section ─────────────────────────────────────────────────
            SettingsSectionHeader("About")

            PTListItem(
                title = "Version",
                subtitle = "SuryaWorld",
                leadingIcon = Icons.Default.Info,
                leadingIconColor = MaterialTheme.colorScheme.secondary,
                trailingText = BuildConfig.VERSION_NAME
            )

            PTListItem(
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                leadingIcon = Icons.Default.Policy,
                leadingIconColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    runCatching { uriHandler.openUri("https://suryaworld.app/privacy") }
                }
            )

            PTListItem(
                title = "App Info",
                subtitle = "Package: com.personaltracker",
                leadingIcon = Icons.Default.AccountCircle,
                leadingIconColor = MaterialTheme.colorScheme.secondary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Logout / Lock Section ─────────────────────────────────────────
            SettingsSectionHeader("Session")

            PTListItem(
                title = "Lock App",
                subtitle = "Return to PIN screen — your data stays safe",
                leadingIcon = Icons.Default.ExitToApp,
                leadingIconColor = MaterialTheme.colorScheme.error,
                onClick = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Logout confirmation dialog ────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Lock App?") },
            text = {
                Text(
                    "You will be returned to the PIN screen. " +
                    "Your data will not be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * A settings row with a trailing Switch, used where [PTListItem]'s trailingText slot is
 * insufficient.
 */
@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
