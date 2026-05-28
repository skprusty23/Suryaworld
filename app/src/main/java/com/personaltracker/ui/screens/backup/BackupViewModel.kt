package com.personaltracker.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.backup.BackupManager
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
    object Idle    : BackupUiState()
    object Loading : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String)   : BackupUiState()
}

data class BackupScreenState(
    val backupState: BackupUiState = BackupUiState.Idle,
    val lastBackupTime: String?    = null,
    val availableBackups: List<File> = emptyList(),
    val autoBackupEnabled: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupScreenState())
    val state: StateFlow<BackupScreenState> = _state.asStateFlow()

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

    init { loadBackupInfo() }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            val backups = backupManager.getBackupFiles()
            val lastBackup = backups.firstOrNull()?.let { file ->
                val instant  = Instant.ofEpochMilli(file.lastModified())
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                dateTime.format(displayFormatter)
            }
            _state.value = _state.value.copy(
                availableBackups = backups,
                lastBackupTime   = lastBackup
            )
        }
    }

    /** Creates an encrypted local backup of the database. */
    fun startBackup() {
        viewModelScope.launch {
            _state.value = _state.value.copy(backupState = BackupUiState.Loading)
            runCatching { backupManager.createBackup() }
                .onSuccess { file ->
                    val instant      = Instant.ofEpochMilli(file.lastModified())
                    val dateTime     = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                    val formattedTime = dateTime.format(displayFormatter)
                    val updatedBackups = backupManager.getBackupFiles()
                    _state.value = _state.value.copy(
                        backupState      = BackupUiState.Success("Backup created successfully"),
                        lastBackupTime   = formattedTime,
                        availableBackups = updatedBackups
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        backupState = BackupUiState.Error("Backup failed: ${e.message ?: "Unknown error"}")
                    )
                }
        }
    }

    /** Restores the database from a specific local backup file. */
    fun restoreFromFile(file: File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(backupState = BackupUiState.Loading)
            val success = backupManager.restoreFromFile(file)
            _state.value = if (success) {
                _state.value.copy(
                    backupState = BackupUiState.Success("Data restored. Please restart the app.")
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
        // TODO: wire WorkManager periodic backup here
    }

    fun resetState() {
        _state.value = _state.value.copy(backupState = BackupUiState.Idle)
    }
}
