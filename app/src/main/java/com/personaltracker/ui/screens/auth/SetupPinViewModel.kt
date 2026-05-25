package com.personaltracker.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    object Success : SetupState()
    data class Error(val message: String) : SetupState()
}

@HiltViewModel
class SetupPinViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()

    /**
     * Saves the given PIN via [PinManager.setAppPin].
     * Updates [setupState] with [SetupState.Success] or [SetupState.Error].
     */
    fun setPin(pin: String) {
        if (_setupState.value is SetupState.Loading) return
        viewModelScope.launch {
            _setupState.value = SetupState.Loading
            try {
                pinManager.setAppPin(pin)
                _setupState.value = SetupState.Success
            } catch (e: Exception) {
                _setupState.value = SetupState.Error(
                    e.message ?: "Failed to save PIN. Please try again."
                )
            }
        }
    }

    /** Resets state back to Idle so the UI can re-enter the flow. */
    fun resetState() {
        _setupState.value = SetupState.Idle
    }
}
