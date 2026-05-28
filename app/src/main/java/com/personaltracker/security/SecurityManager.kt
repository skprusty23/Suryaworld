package com.personaltracker.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val securePrefs: SharedPreferences
        get() = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the stable 32-byte AES passphrase used by Room + SQLCipher.
     *
     * ROOT CAUSE FIX:
     * The old `deriveKeyBytes()` called `Cipher.ENCRYPT_MODE` on every invocation.
     * AES/GCM generates a NEW RANDOM IV per init, so the resulting 32 bytes changed
     * on every app launch → SQLCipher threw "file is not a database" on second open.
     *
     * FIX: Generate a random 32-byte key ONCE, encrypt it with the AndroidKeyStore AES key
     * (storing IV + ciphertext in SharedPreferences), decrypt the same blob on every
     * subsequent launch → the passphrase is always identical.
     *
     * Migration: If no blob is stored but a database file exists, that database was created
     * with an irrecoverable random passphrase. We delete it so Room creates a fresh,
     * properly-keyed database.
     */
    fun getOrCreateDatabaseKey(): ByteArray {
        val prefs = securePrefs
        val storedBlob = prefs.getString(DB_KEY_BLOB_PREF, null)
        val androidKey = getOrCreateSecretKey(DB_KEY_ALIAS)

        if (storedBlob != null) {
            return try {
                decryptKeyBlob(storedBlob, androidKey)
            } catch (e: Exception) {
                // AndroidKeyStore key was invalidated (e.g., biometric re-enrollment).
                // Existing database can no longer be decrypted — start fresh.
                deleteExistingDatabase()
                prefs.edit().remove(DB_KEY_BLOB_PREF).apply()
                generateAndPersistDatabaseKey(prefs, getOrCreateSecretKey(DB_KEY_ALIAS))
            }
        }

        // No blob stored yet — fresh install OR coming from old broken deriveKeyBytes() build.
        // Any existing database file used an irrecoverable passphrase, so delete it.
        deleteExistingDatabase()
        return generateAndPersistDatabaseKey(prefs, androidKey)
    }

    /** AES-256-GCM encrypt [plaintext]; IV is prepended to the ciphertext and Base64-encoded. */
    fun encrypt(plaintext: String, keyAlias: String = DEFAULT_KEY_ALIAS): String {
        val key = getOrCreateSecretKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    /** Decrypts a value previously produced by [encrypt]. */
    fun decrypt(ciphertext: String, keyAlias: String = DEFAULT_KEY_ALIAS): String {
        val key = getOrCreateSecretKey(keyAlias)
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        return if (keyStore.containsAlias(alias)) {
            (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    /**
     * Generates a cryptographically random 32-byte passphrase, encrypts it with [androidKey],
     * persists the IV+ciphertext blob in [prefs], and returns the raw bytes.
     */
    private fun generateAndPersistDatabaseKey(
        prefs: SharedPreferences,
        androidKey: SecretKey
    ): ByteArray {
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val blob = encryptKeyBlob(rawKey, androidKey)
        prefs.edit().putString(DB_KEY_BLOB_PREF, blob).apply()
        return rawKey
    }

    private fun encryptKeyBlob(rawKey: ByteArray, androidKey: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, androidKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(rawKey)
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun decryptKeyBlob(blob: String, androidKey: SecretKey): ByteArray {
        val combined = Base64.decode(blob, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, androidKey, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return cipher.doFinal(encrypted)
    }

    /** Deletes the SQLCipher database file and its WAL/SHM auxiliary files. */
    private fun deleteExistingDatabase() {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) {
            dbFile.delete()
            File("${dbFile.path}-wal").delete()
            File("${dbFile.path}-shm").delete()
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE  = "AndroidKeyStore"
        private const val TRANSFORMATION    = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH     = 12
        private const val GCM_TAG_LENGTH    = 16
        private const val DB_KEY_ALIAS      = "pt_db_key"
        private const val DEFAULT_KEY_ALIAS = "pt_default_key"
        private const val SECURE_PREFS_NAME = "pt_secure_keystore_prefs"
        private const val DB_KEY_BLOB_PREF  = "db_key_blob"
        const val DB_NAME                   = "personal_tracker.db"
    }
}
