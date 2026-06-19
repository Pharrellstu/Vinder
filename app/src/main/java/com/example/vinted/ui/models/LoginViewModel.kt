package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.IAuthRepository
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val repository: IAuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(EMPTY_CREDENTIALS_MESSAGE)
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.login(email, password)
                .onSuccess { _uiState.value = LoginUiState.Success }
                .onFailure {
                    _uiState.value = LoginUiState.Error(ErrorMessages.friendlyMessage(it, LOGIN_FAILED_MESSAGE))
                }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        const val EMPTY_CREDENTIALS_MESSAGE = "Email and password must not be empty"
        const val LOGIN_FAILED_MESSAGE = "Couldn't sign you in. Please try again."
    }
}
