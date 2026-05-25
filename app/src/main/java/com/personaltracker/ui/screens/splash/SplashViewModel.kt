package com.personaltracker.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _isPinSetup = MutableStateFlow<Boolean?>(null)
    val isPinSetup: StateFlow<Boolean?> = _isPinSetup.asStateFlow()

    init {
        viewModelScope.launch {
            _isPinSetup.value = pinManager.isPinSetup()
        }
    }
}
