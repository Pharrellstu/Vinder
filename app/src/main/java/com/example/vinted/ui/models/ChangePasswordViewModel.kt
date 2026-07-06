package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.IAuthRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChangePasswordUiState {
    object Idle : ChangePasswordUiState()
    object Loading : ChangePasswordUiState()
    object Success : ChangePasswordUiState()
    data class Error(val message: String) : ChangePasswordUiState()
}

class ChangePasswordViewModel(
    private val repository: IAuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun clearError() {
        if (_uiState.value is ChangePasswordUiState.Error) {
            _uiState.value = ChangePasswordUiState.Idle
        }
    }

    fun changePassword(current: String, new: String, confirm: String) {
        val validationError = validate(current, new, confirm)
        if (validationError != null) { _uiState.value = ChangePasswordUiState.Error(validationError); return }

        val email = SessionManager.currentEmail
        if (email.isBlank()) { _uiState.value = ChangePasswordUiState.Error("You're not signed in."); return }

        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState.Loading
            // Re-authenticate before allowing the change — GoTrue's updateUser only checks the
            // session, so without this anyone with an open app could reset the password.
            repository.login(email, current)
                .getOrElse { _uiState.value = ChangePasswordUiState.Error("Current password is incorrect."); return@launch }
            repository.updatePassword(new)
                .onSuccess { _uiState.value = ChangePasswordUiState.Success }
                .onFailure {
                    _uiState.value = ChangePasswordUiState.Error(
                        ErrorMessages.friendlyMessage(it, "Couldn't update password. Please try again.")
                    )
                }
        }
    }

    private fun validate(current: String, new: String, confirm: String): String? {
        if (current.isBlank() || new.isBlank() || confirm.isBlank()) return "Please fill in all fields."
        if (new != confirm) return "New passwords don't match."
        if (new == current) return "New password must be different from your current one."
        // Reuse registration rules so strength requirements stay consistent app-wide.
        val missing = RegistrationViewModel.findMissingPasswordRequirements(new)
        if (missing.size > MAX_MISSING_REQUIREMENTS) {
            return "Password is too weak. It needs: ${missing.joinToString(", ")}."
        }
        return null
    }

    private companion object {
        // Matches RegistrationViewModel: a password is too weak past two unmet rules.
        const val MAX_MISSING_REQUIREMENTS = 2
    }
}
