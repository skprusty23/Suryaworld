package com.personaltracker.ui.screens.backup

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.backup.BackupManager
import com.personaltracker.backup.DriveBackupFile
import com.personaltracker.backup.GoogleDriveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class BackupUiState {
    object Idle : BackupUiState()
    object Loading : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

data class BackupScreenState(
    val backupState: BackupUiState = BackupUiState.Idle,
    val lastBackupTime: String? = null,
    val availableBackups: List<File> = emptyList(),
    val driveBackups: List<DriveBackupFile> = emptyList(),
    val autoBackupEnabled: Boolean = false,
    val googleSignedIn: Boolean = false,
    val googleUserEmail: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val driveManager: GoogleDriveManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupScreenState())
    val state: StateFlow<BackupScreenState> = _state.asStateFlow()

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

    init {
        loadBackupInfo()
    }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            val account = driveManager.getSignedInAccount()
            val backups = backupManager.getBackupFiles()
            val lastBackup = backups.firstOrNull()?.let { file ->
                val instant = Instant.ofEpochMilli(file.lastModified())
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                dateTime.format(displayFormatter)
            }
            _state.value = _state.value.copy(
                availableBackups = backups,
                lastBackupTime = lastBackup,
                googleSignedIn = account != null,
                googleUserEmail = account?.email
            )
            if (account != null) {
                loadDriveBackups()
            }
        }
    }

    private fun loadDriveBackups() {
        viewModelScope.launch {
            val driveBackups = driveManager.listBackups()
            _state.value = _state.value.copy(driveBackups = driveBackups)
        }
    }

    /**
     * Returns the Google Sign-In intent. The caller (Activity) must launch it via
     * startActivityForResult with the returned intent.
     */
    fun getSignInIntent(activity: Activity) = driveManager.getSignInIntent(activity)

    /**
     * Called after the user completes Google Sign-In (from Activity.onActivityResult).
     * Refreshes account state and loads Drive backups.
     */
    fun onSignInResult() {
        viewModelScope.launch {
            val account = driveManager.getSignedInAccount()
            _state.value = _state.value.copy(
                googleSignedIn = account != null,
                googleUserEmail = account?.email
            )
            if (account != null) loadDriveBackups()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            driveManager.signOut()
            _state.value = _state.value.copy(
                googleSignedIn = false,
                googleUserEmail = null,
                driveBackups = emptyList()
            )
        }
    }

    /**
     * Creates an encrypted local backup, then uploads it to Google Drive appDataFolder.
     */
    fun startBackup() {
        viewModelScope.launch {
            _state.value = _state.value.copy(backupState = BackupUiState.Loading)
            runCatching { backupManager.createBackup() }
                .onSuccess { file ->
                    val instant = Instant.ofEpochMilli(file.lastModified())
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                    val formattedTime = dateTime.format(displayFormatter)
                    val updatedBackups = backupManager.getBackupFiles()

                    // Upload to Google Drive if signed in
                    val uploaded = if (driveManager.isSignedIn()) {
                        driveManager.uploadBackup(file)
                    } else false

                    _state.value = _state.value.copy(
                        backupState = BackupUiState.Success(
                            if (uploaded) "Backup created & uploaded to Google Drive"
                            else "Local backup created successfully"
                        ),
                        lastBackupTime = formattedTime,
                        availableBackups = updatedBackups
                    )

                    if (uploaded) {
                        driveManager.pruneOldBackups(keepCount = 5)
                        loadDriveBackups()
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        backupState = BackupUiState.Error("Backup failed: ${e.message ?: "Unknown error"}")
                    )
                }
        }
    }

    /**
     * Restores the database from the most recent local backup file.
     */
    fun restore() {
        viewModelScope.launch {
            val backupFile = _state.value.availableBackups.firstOrNull()
                ?: run {
                    _state.value = _state.value.copy(
                        backupState = BackupUiState.Error("No backup files found")
                    )
                    return@launch
                }
            _state.value = _state.value.copy(backupState = BackupUiState.Loading)
            val success = backupManager.restoreFromFile(backupFile)
            _state.value = if (success) {
                _state.value.copy(
                    backupState = BackupUiState.Success("Data restored successfully. Please restart the app.")
                )
            } else {
                _state.value.copy(
                    backupState = BackupUiState.Error("Restore failed. The backup file may be corrupted.")
                )
            }
        }
    }

    /**
     * Restores data from a specific local backup file chosen by the user.
     */
    fun restoreFromFile(file: File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(backupState = BackupUiState.Loading)
            val success = backupManager.restoreFromFile(file)
            _state.value = if (success) {
                _state.value.copy(
                    backupState = BackupUiState.Success("Data restored successfully. Please restart the app.")
                )
            } else {
                _state.value.copy(
                    backupState = BackupUiState.Error("Restore failed. The backup file may be corrupted.")
                )
            }
        }
    }

    /**
     * Downloads a Drive backup to the local cache dir, then restores from it.
     */
    fun restoreFromDrive(driveFile: DriveBackupFile, cacheDir: File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(backupState = BackupUiState.Loading)
            val localFile = driveManager.downloadBackup(driveFile, cacheDir)
            if (localFile == null) {
                _state.value = _state.value.copy(
                    backupState = BackupUiState.Error("Failed to download backup from Google Drive")
                )
                return@launch
            }
            val success = backupManager.restoreFromFile(localFile)
            _state.value = if (success) {
                _state.value.copy(
                    backupState = BackupUiState.Success("Drive backup restored. Please restart the app.")
                )
            } else {
                _state.value.copy(
                    backupState = BackupUiState.Error("Restore failed. The backup file may be corrupted.")
                )
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        _state.value = _state.value.copy(autoBackupEnabled = enabled)
        // TODO: schedule/cancel WorkManager periodic backup task
    }

    fun resetState() {
        _state.value = _state.value.copy(backupState = BackupUiState.Idle)
    }
}
