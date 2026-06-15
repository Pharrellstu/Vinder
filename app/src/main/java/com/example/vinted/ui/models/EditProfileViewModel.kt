package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.AccountRepository
import com.example.vinted.data.IAccountRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Editing(
        val name: String,
        val location: String,
        val bio: String,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
    ) : EditProfileUiState()
    object Saved : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

class EditProfileViewModel(
    private val accountId: Int? = null,
    private val repository: IAccountRepository = AccountRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val resolvedId: Int = accountId ?: SessionManager.currentAccountId

    init {
        load()
    }

    fun load() {
        if (resolvedId == -1) {
            _uiState.value = EditProfileUiState.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            runCatching {
                repository.getProfile(resolvedId)
            }.onSuccess { profile ->
                _uiState.value = EditProfileUiState.Editing(
                    name = profile.handle,
                    location = profile.location,
                    bio = profile.bio,
                )
            }.onFailure {
                _uiState.value = EditProfileUiState.Error(it.message ?: "Failed to load profile")
            }
        }
    }

    fun onNameChange(value: String) = updateEditing { it.copy(name = value) }
    fun onLocationChange(value: String) = updateEditing { it.copy(location = value) }
    fun onBioChange(value: String) = updateEditing { it.copy(bio = value) }

    private fun updateEditing(transform: (EditProfileUiState.Editing) -> EditProfileUiState.Editing) {
        val current = _uiState.value
        if (current is EditProfileUiState.Editing) {
            _uiState.value = transform(current)
        }
    }

    fun save() {
        val current = _uiState.value as? EditProfileUiState.Editing ?: return
        if (current.isSaving) return
        if (current.name.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Name can't be empty")
            return
        }
        if (resolvedId == -1) {
            _uiState.value = current.copy(errorMessage = "Not logged in")
            return
        }
        _uiState.value = current.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                repository.updateProfile(
                    accountId = resolvedId,
                    name = current.name.trim(),
                    bio = current.bio.trim(),
                    location = current.location.trim(),
                )
            }.onSuccess {
                _uiState.value = EditProfileUiState.Saved
            }.onFailure {
                _uiState.value = current.copy(
                    isSaving = false,
                    errorMessage = it.message ?: "Failed to save changes",
                )
            }
        }
    }
}
