package com.personaltracker.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val RC_SIGN_IN = 9001
        private const val APP_NAME = "SuryaWorld"
        private const val BACKUP_FOLDER_NAME = "SuryaWorld Backups"
    }

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()

    fun getSignInClient(context: Context): GoogleSignInClient =
        GoogleSignIn.getClient(context, signInOptions)

    fun getSignInIntent(activity: Activity): Intent =
        getSignInClient(activity).signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)?.takeIf {
            it.grantedScopes.contains(Scope(DriveScopes.DRIVE_APPDATA))
        }

    fun isSignedIn(): Boolean = getSignedInAccount() != null

    suspend fun signOut() = withContext(Dispatchers.IO) {
        getSignInClient(context).signOut()
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    /**
     * Upload a backup file to the Google Drive appDataFolder.
     * Returns true on success.
     */
    suspend fun uploadBackup(backupFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        val account = getSignedInAccount() ?: return@withContext false
        try {
            val drive = getDriveService(account)
            val fileMetadata = File().apply {
                name = backupFile.name
                parents = listOf("appDataFolder")
            }
            val mediaContent = FileContent("application/octet-stream", backupFile)
            drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size")
                .execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * List all .ptbak files stored in the app's Google Drive appDataFolder.
     */
    suspend fun listBackups(): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount() ?: return@withContext emptyList()
        try {
            val drive = getDriveService(account)
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, size, createdTime)")
                .setQ("name contains '.ptbak'")
                .execute()
            result.files.map {
                DriveBackupFile(
                    id = it.id,
                    name = it.name,
                    size = it.getSize() ?: 0L,
                    createdTime = it.createdTime?.value ?: 0L
                )
            }.sortedByDescending { it.createdTime }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Download a backup file from Google Drive to local storage.
     */
    suspend fun downloadBackup(driveFile: DriveBackupFile, destDir: java.io.File): java.io.File? =
        withContext(Dispatchers.IO) {
            val account = getSignedInAccount() ?: return@withContext null
            try {
                val drive = getDriveService(account)
                val dest = java.io.File(destDir, driveFile.name)
                FileOutputStream(dest).use { out ->
                    drive.files().get(driveFile.id).executeMediaAndDownloadTo(out)
                }
                dest
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Delete old backups keeping only the latest [keepCount].
     */
    suspend fun pruneOldBackups(keepCount: Int = 5): Unit = withContext(Dispatchers.IO) {
        val account = getSignedInAccount() ?: return@withContext
        try {
            val drive = getDriveService(account)
            val backups = listBackups()
            if (backups.size > keepCount) {
                backups.drop(keepCount).forEach { file ->
                    runCatching { drive.files().delete(file.id).execute() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class DriveBackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: Long
) {
    val displayName: String get() = name.removePrefix("pt_backup_").removeSuffix(".ptbak")
}
