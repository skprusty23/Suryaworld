package com.personaltracker.ui.screens.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.CredentialEntity
import com.personaltracker.domain.repository.CredentialRepository
import com.personaltracker.security.PinManager
import com.personaltracker.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CredentialsUiState(
    val credentials: List<CredentialEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isLoading: Boolean = true,
    val isUnlocked: Boolean = false,
    val requiresPin: Boolean = false,
    val pinError: String? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val securityManager: SecurityManager,
    private val pinManager: PinManager
) : ViewModel() {

    private val _searchQuery      = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _isUnlocked       = MutableStateFlow(false)
    private val _requiresPin      = MutableStateFlow(false)
    private val _pinError         = MutableStateFlow<String?>(null)
    private val _errorMessage     = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CredentialsUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _isUnlocked,
        _requiresPin,
        _pinError,
        _errorMessage
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val query    = args[0] as String
        val category = args[1] as String
        val unlocked = args[2] as Boolean
        val needsPin = args[3] as Boolean
        val pinErr   = args[4] as String?
        val errMsg   = args[5] as String?
        CredentialsUiState(
            searchQuery      = query,
            selectedCategory = category,
            isUnlocked       = unlocked,
            requiresPin      = needsPin,
            pinError         = pinErr,
            errorMessage     = errMsg,
            isLoading        = !unlocked && !needsPin
        )
    }.flatMapLatest { baseState ->
        if (!baseState.isUnlocked) {
            flowOf(baseState)
        } else {
            val source = when {
                baseState.searchQuery.isNotBlank() ->
                    credentialRepository.searchCredentials(baseState.searchQuery)
                baseState.selectedCategory != "All" ->
                    credentialRepository.getCredentialsByCategory(baseState.selectedCategory)
                else ->
                    credentialRepository.getAllCredentials()
            }
            source.map { list ->
                baseState.copy(credentials = list, isLoading = false)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CredentialsUiState(isLoading = true)
    )

    init {
        checkPinRequirement()
    }

    private fun checkPinRequirement() {
        viewModelScope.launch {
            val pinSet = pinManager.isModulePinSet(PinManager.PinModule.CREDENTIALS)
            if (pinSet) {
                _requiresPin.value = true
            } else {
                // No module PIN — unlock directly
                _isUnlocked.value = true
            }
        }
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            val valid = pinManager.verifyModulePin(PinManager.PinModule.CREDENTIALS, pin)
            if (valid) {
                _pinError.value = null
                _requiresPin.value = false
                _isUnlocked.value = true
            } else {
                _pinError.value = "Incorrect PIN. Please try again."
            }
        }
    }

    fun clearPinError() {
        _pinError.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun decryptPassword(encrypted: String): String {
        return try {
            securityManager.decrypt(encrypted)
        } catch (e: Exception) {
            "••••••••"
        }
    }

    fun toggleFavorite(credential: CredentialEntity) {
        viewModelScope.launch {
            try {
                credentialRepository.updateCredential(
                    credential.copy(isFavorite = !credential.isFavorite)
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update: ${e.message}"
            }
        }
    }

    fun deleteCredential(credential: CredentialEntity) {
        viewModelScope.launch {
            try {
                credentialRepository.deleteCredential(credential)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
