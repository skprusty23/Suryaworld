package com.personaltracker.ui.screens.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.security.BiometricHelper
import com.personaltracker.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Verifies the given PIN against the stored hash and updates [authState].
     */
    fun verifyPin(pin: String) {
        if (_authState.value is AuthState.Loading) return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val isValid = try {
                pinManager.verifyAppPin(pin)
            } catch (e: Exception) {
                false
            }
            _authState.value = if (isValid) AuthState.Success else AuthState.Error("Wrong PIN")
        }
    }

    /** Resets authentication state back to Idle (e.g. after showing an error). */
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    /** Returns true if hardware biometric authentication is available on this device. */
    fun isBiometricAvailable(): Boolean = biometricHelper.isBiometricAvailable()

    /** Returns true if the user has enabled biometric unlock in settings. */
    suspend fun isBiometricEnabled(): Boolean = pinManager.isBiometricEnabled()

    /**
     * Triggers the system biometric prompt.
     * The Hilt-injected [BiometricHelper] singleton is reused — no manual construction.
     *
     * @param activity Required by [BiometricPrompt] to attach the UI to the current window.
     * @param onSuccess Called when authentication succeeds.
     * @param onFallback Called when the user dismisses biometric (falls back to PIN).
     * @param onError Called on hardware/lock-out errors.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFallback: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        biometricHelper.authenticate(
            activity = activity,
            title = "Biometric Login",
            subtitle = "Confirm your identity to access SuryaWorld",
            onSuccess = onSuccess,
            onError = onError,
            onFallback = onFallback
        )
    }
}
