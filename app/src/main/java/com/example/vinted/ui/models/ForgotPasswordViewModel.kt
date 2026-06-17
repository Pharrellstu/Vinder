package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordState(
    val phase: Phase = Phase.EMAIL,
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    enum class Phase { EMAIL, OTP, NEW_PASSWORD, SUCCESS }
}

class ForgotPasswordViewModel(
    private val repository: IAuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    fun sendOtp(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Email must not be empty") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.sendPasswordResetEmail(email)
                .onSuccess {
                    _state.update {
                        it.copy(phase = ForgotPasswordState.Phase.OTP, email = email, isLoading = false)
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = ERR_SEND_OTP) }
                }
        }
    }

    fun verifyOtp(code: String) {
        if (code.length != OTP_LENGTH) {
            _state.update { it.copy(error = "Enter the full $OTP_LENGTH-digit code") }
            return
        }
        val email = _state.value.email
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.verifyPasswordResetOtp(email, code)
                .onSuccess {
                    _state.update { it.copy(phase = ForgotPasswordState.Phase.NEW_PASSWORD, isLoading = false) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = ERR_VERIFY_OTP) }
                }
        }
    }

    fun updatePassword(newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank()) {
            _state.update { it.copy(error = "Password must not be empty") }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.updatePassword(newPassword)
                .onSuccess {
                    _state.update { it.copy(phase = ForgotPasswordState.Phase.SUCCESS, isLoading = false) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = ERR_UPDATE_PASSWORD) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        const val OTP_LENGTH = 6
        private const val ERR_SEND_OTP = "Could not send the code. Please try again."
        private const val ERR_VERIFY_OTP = "Invalid or expired code. Please try again."
        private const val ERR_UPDATE_PASSWORD = "Could not update the password. Please try again."
    }
}
