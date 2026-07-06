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

sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object Loading : RegistrationUiState()
    data class Success(val email: String) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}

class RegistrationViewModel(
    private val repository: IAuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun register(nickname: String, email: String, password: String, confirmPassword: String) {
        val syncError = validateInputs(nickname, email, password, confirmPassword)
        if (syncError != null) { _uiState.value = RegistrationUiState.Error(syncError); return }

        viewModelScope.launch {
            _uiState.value = RegistrationUiState.Loading

            val nicknameTaken = repository.isNicknameTaken(nickname)
                .getOrElse { _uiState.value = RegistrationUiState.Error(registrationError(it)); return@launch }
            if (nicknameTaken) { _uiState.value = RegistrationUiState.Error(NICKNAME_TAKEN_MESSAGE); return@launch }

            val emailTaken = repository.isEmailTaken(email)
                .getOrElse { _uiState.value = RegistrationUiState.Error(registrationError(it)); return@launch }
            if (emailTaken) { _uiState.value = RegistrationUiState.Error(EMAIL_TAKEN_MESSAGE); return@launch }

            repository.register(nickname, email, password)
                .onSuccess { _uiState.value = RegistrationUiState.Success(email) }
                .onFailure { _uiState.value = RegistrationUiState.Error(registrationError(it)) }
        }
    }

    fun resetState() {
        _uiState.value = RegistrationUiState.Idle
    }

    private fun validateInputs(
        nickname: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): String? {
        if (nickname.isBlank() || email.isBlank() || password.isBlank()) return EMPTY_CREDENTIALS_MESSAGE
        if (password != confirmPassword) return PASSWORD_MISMATCH_MESSAGE
        val missing = findMissingPasswordRequirements(password)
        if (missing.size > MAX_MISSING_REQUIREMENTS_FOR_WEAK) return buildWeakPasswordMessage(missing)
        return null
    }

    private fun buildWeakPasswordMessage(missing: List<String>): String =
        WEAK_PASSWORD_PREFIX + missing.joinToString(", ") + "."

    private fun registrationError(throwable: Throwable): String =
        ErrorMessages.friendlyMessage(throwable, REGISTRATION_FAILED_MESSAGE)

    companion object {
        const val EMPTY_CREDENTIALS_MESSAGE = "All fields must not be empty"
        const val REGISTRATION_FAILED_MESSAGE = "Couldn't create your account. Please try again."
        const val PASSWORD_MISMATCH_MESSAGE = "Passwords do not match"
        const val NICKNAME_TAKEN_MESSAGE = "This nickname is already taken"
        const val EMAIL_TAKEN_MESSAGE = "This email is already registered"
        const val EMAIL_SENT_MESSAGE = "A confirmation link has been sent to"
        const val WEAK_PASSWORD_PREFIX = "Password is too weak. It needs: "

        private const val MIN_PASSWORD_LENGTH = 8

        // A password is WEAK when it satisfies fewer than 2 of the 4 rules,
        // i.e. when more than 2 of the 4 requirements are still missing.
        private const val MAX_MISSING_REQUIREMENTS_FOR_WEAK = 2

        fun findMissingPasswordRequirements(password: String): List<String> {
            val missing = mutableListOf<String>()
            if (password.length < MIN_PASSWORD_LENGTH) missing.add("at least $MIN_PASSWORD_LENGTH characters")
            if (password.none { it.isUpperCase() }) missing.add("an uppercase letter")
            if (password.none { it.isDigit() }) missing.add("a digit")
            if (password.none { !it.isLetterOrDigit() }) missing.add("a special character")
            return missing
        }
    }
}
