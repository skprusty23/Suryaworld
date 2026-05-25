package com.personaltracker.backup

import android.content.Context
import com.personaltracker.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager
) {
    // Export encrypted backup of the database file
    suspend fun createBackup(): File {
        val dbFile = context.getDatabasePath("personal_tracker.db")
        val backupDir = File(context.filesDir, "backups").also { it.mkdirs() }
        val timestamp = LocalDateTime.now().toString().replace(":", "-")
        val backupFile = File(backupDir, "pt_backup_$timestamp.ptbak")
        // Encrypt the backup file content using AES-GCM
        val dbBytes = dbFile.readBytes()
        val encryptedData = securityManager.encrypt(
            android.util.Base64.encodeToString(dbBytes, android.util.Base64.NO_WRAP)
        )
        backupFile.writeText(encryptedData)
        return backupFile
    }

    fun getBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".ptbak") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun restoreFromFile(backupFile: File): Boolean {
        return try {
            val encryptedData = backupFile.readText()
            val decryptedBase64 = securityManager.decrypt(encryptedData)
            val dbBytes = android.util.Base64.decode(decryptedBase64, android.util.Base64.NO_WRAP)
            val dbFile = context.getDatabasePath("personal_tracker.db")
            dbFile.writeBytes(dbBytes)
            true
        } catch (e: Exception) { false }
    }
}
