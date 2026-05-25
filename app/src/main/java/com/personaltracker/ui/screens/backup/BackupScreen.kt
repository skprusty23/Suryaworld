package com.personaltracker.ui.screens.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.personaltracker.backup.DriveBackupFile
import com.personaltracker.backup.GoogleDriveManager
import com.personaltracker.ui.components.PTTopBar
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showRestoreLocalDialog by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var showRestoreDriveDialog by remember { mutableStateOf(false) }
    var pendingDriveFile by remember { mutableStateOf<DriveBackupFile?>(null) }

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSignInResult()
        }
    }

    // Surface success/error messages via Snackbar
    LaunchedEffect(state.backupState) {
        when (val bs = state.backupState) {
            is BackupUiState.Success -> {
                snackbarHostState.showSnackbar(bs.message)
                viewModel.resetState()
            }
            is BackupUiState.Error -> {
                snackbarHostState.showSnackbar(bs.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Backup & Restore", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Google Drive Status Card ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (state.googleSignedIn) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (state.googleSignedIn) Color(0xFF4285F4)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Google Drive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (state.googleSignedIn)
                                    state.googleUserEmail ?: "Signed in"
                                else
                                    "Not connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.googleSignedIn) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Connected",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!state.googleSignedIn) {
                        Button(
                            onClick = {
                                val intent = viewModel.getSignInIntent(context as Activity)
                                signInLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }

            // ── Backup Section ────────────────────────────────────────────────
            BackupSectionHeader("Backup")

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Last backup timestamp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Last Backup",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                state.lastBackupTime ?: "Never backed up",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto-backup toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto Backup",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Automatically backup when connected to Wi-Fi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoBackupEnabled,
                            onCheckedChange = { viewModel.toggleAutoBackup(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Backup Now button
                    Button(
                        onClick = { viewModel.startBackup() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.backupState !is BackupUiState.Loading
                    ) {
                        if (state.backupState is BackupUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Backup, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.googleSignedIn) "Backup Now (+ Google Drive)" else "Backup Now (Local)")
                    }
                }
            }

            HorizontalDivider()

            // ── Local Restore Section ─────────────────────────────────────────
            BackupSectionHeader("Local Backups")

            if (state.availableBackups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No local backup files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                state.availableBackups.forEachIndexed { index, file ->
                    LocalBackupFileCard(
                        file = file,
                        isLatest = index == 0,
                        onRestore = {
                            pendingRestoreFile = file
                            showRestoreLocalDialog = true
                        }
                    )
                }
            }

            // ── Drive Backups Section (only when signed in) ───────────────────
            if (state.googleSignedIn) {
                HorizontalDivider()
                BackupSectionHeader("Google Drive Backups")

                if (state.driveBackups.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No Drive backups found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    state.driveBackups.forEachIndexed { index, driveFile ->
                        DriveBackupFileCard(
                            driveFile = driveFile,
                            isLatest = index == 0,
                            onRestore = {
                                pendingDriveFile = driveFile
                                showRestoreDriveDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Local restore confirmation dialog ─────────────────────────────────────
    if (showRestoreLocalDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreLocalDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Restore Data") },
            text = {
                Text(
                    "This will replace all current data with the selected backup. " +
                        "This action cannot be undone. Do you want to continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreFile?.let { viewModel.restoreFromFile(it) }
                            ?: viewModel.restore()
                        showRestoreLocalDialog = false
                        pendingRestoreFile = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreLocalDialog = false
                    pendingRestoreFile = null
                }) { Text("Cancel") }
            }
        )
    }

    // ── Drive restore confirmation dialog ─────────────────────────────────────
    if (showRestoreDriveDialog) {
        val cacheDir = context.cacheDir
        AlertDialog(
            onDismissRequest = { showRestoreDriveDialog = false },
            icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
            title = { Text("Restore from Google Drive") },
            text = {
                Text(
                    "This will download and restore \"${pendingDriveFile?.displayName}\" from Google Drive, " +
                        "replacing all current data. This action cannot be undone. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDriveFile?.let { viewModel.restoreFromDrive(it, cacheDir) }
                        showRestoreDriveDialog = false
                        pendingDriveFile = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Download & Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDriveDialog = false
                    pendingDriveFile = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BackupSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun LocalBackupFileCard(
    file: File,
    isLatest: Boolean,
    onRestore: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(file.lastModified()),
        ZoneId.systemDefault()
    )
    val formattedDate = dateTime.format(formatter)
    val fileSizeKb = file.length() / 1024

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isLatest) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (isLatest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Latest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "$fileSizeKb KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onRestore) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore")
            }
        }
    }
}

@Composable
private fun DriveBackupFileCard(
    driveFile: DriveBackupFile,
    isLatest: Boolean,
    onRestore: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(driveFile.createdTime),
        ZoneId.systemDefault()
    )
    val formattedDate = dateTime.format(formatter)
    val fileSizeKb = driveFile.size / 1024

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isLatest) CardDefaults.cardColors(
            containerColor = Color(0xFF4285F4).copy(alpha = 0.08f)
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Cloud,
                contentDescription = null,
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        driveFile.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (isLatest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Latest",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "$formattedDate · ${fileSizeKb}KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore")
            }
        }
    }
}
