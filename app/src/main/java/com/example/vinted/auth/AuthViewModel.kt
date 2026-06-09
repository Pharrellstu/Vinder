package com.example.vinted.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: IAuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(EMPTY_CREDENTIALS_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.login(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success(LOGIN_SUCCESS_MESSAGE) }
                .onFailure { _uiState.value = AuthUiState.Error(it.toUserMessage()) }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(EMPTY_CREDENTIALS_MESSAGE)
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error(PASSWORD_MISMATCH_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.register(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success(REGISTER_SUCCESS_MESSAGE) }
                .onFailure { _uiState.value = AuthUiState.Error(it.toUserMessage()) }
        }
    }

    fun verifyOtp(email: String, token: String) {
        if (token.isBlank()) {
            _uiState.value = AuthUiState.Error(EMPTY_CODE_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.verifyOtp(email, token)
                .onSuccess { _uiState.value = AuthUiState.Success(VERIFY_SUCCESS_MESSAGE) }
                .onFailure { _uiState.value = AuthUiState.Error(it.toUserMessage()) }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun Throwable.toUserMessage(): String = message ?: GENERIC_ERROR_MESSAGE

    companion object {
        const val EMPTY_CREDENTIALS_MESSAGE = "Email and password must not be empty"
        const val EMPTY_CODE_MESSAGE = "Verification code must not be empty"
        const val PASSWORD_MISMATCH_MESSAGE = "Passwords do not match"
        const val LOGIN_SUCCESS_MESSAGE = "Logged in successfully"
        const val REGISTER_SUCCESS_MESSAGE = "Registered successfully"
        const val VERIFY_SUCCESS_MESSAGE = "Account verified successfully"
        const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again"
    }
}
