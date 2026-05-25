package com.personaltracker.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager,
    private val dataStore: DataStore<Preferences>
) {
    private val APP_PIN_KEY = stringPreferencesKey("app_pin_hash")
    private val INVESTMENT_PIN_KEY = stringPreferencesKey("investment_pin_hash")
    private val CREDENTIALS_PIN_KEY = stringPreferencesKey("credentials_pin_hash")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val PIN_SETUP_KEY = booleanPreferencesKey("pin_setup_done")

    suspend fun isPinSetup(): Boolean =
        dataStore.data.map { it[PIN_SETUP_KEY] ?: false }.first()

    suspend fun setAppPin(pin: String) {
        val hash = hashPin(pin)
        val encrypted = securityManager.encrypt(hash)
        dataStore.edit { prefs ->
            prefs[APP_PIN_KEY] = encrypted
            prefs[PIN_SETUP_KEY] = true
        }
    }

    suspend fun verifyAppPin(pin: String): Boolean {
        val stored = dataStore.data.map { it[APP_PIN_KEY] }.first() ?: return false
        return try {
            val decrypted = securityManager.decrypt(stored)
            decrypted == hashPin(pin)
        } catch (e: Exception) { false }
    }

    suspend fun setModulePin(module: PinModule, pin: String) {
        val key = module.prefKey
        val hash = hashPin(pin)
        val encrypted = securityManager.encrypt(hash)
        dataStore.edit { prefs -> prefs[key] = encrypted }
    }

    suspend fun verifyModulePin(module: PinModule, pin: String): Boolean {
        val key = module.prefKey
        val stored = dataStore.data.map { it[key] }.first() ?: return verifyAppPin(pin)
        return try {
            val decrypted = securityManager.decrypt(stored)
            decrypted == hashPin(pin)
        } catch (e: Exception) { false }
    }

    suspend fun isModulePinSet(module: PinModule): Boolean {
        val key = module.prefKey
        return dataStore.data.map { it[key] != null }.first()
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun isBiometricEnabled(): Boolean =
        dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }.first()

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = "pt_pin_salt_v1"
        return digest.digest((pin + salt).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    enum class PinModule(val prefKey: Preferences.Key<String>) {
        INVESTMENT(stringPreferencesKey("investment_pin_hash")),
        CREDENTIALS(stringPreferencesKey("credentials_pin_hash"))
    }
}
