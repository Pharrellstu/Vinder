package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.IAuthRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val email: String = SessionManager.currentEmail,
    val darkMode: Boolean = false,
    val isLoggingOut: Boolean = false,
)

class SettingsViewModel(
    private val repository: IAuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            repository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            onLoggedOut()
        }
    }
}
