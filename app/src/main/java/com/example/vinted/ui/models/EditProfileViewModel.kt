package com.example.vinted.ui.models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.AccountRepository
import com.example.vinted.data.IAccountRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Editing(
        val name: String,
        val location: String,
        val bio: String,
        val avatarUrl: String? = null,
        val isSaving: Boolean = false,
        val isUploadingPhoto: Boolean = false,
        val errorMessage: String? = null,
    ) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

class EditProfileViewModel(
    private val accountId: Int? = null,
    private val repository: IAccountRepository = AccountRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // One-shot "saved" signal. A sticky Saved state would persist on this
    // Activity-scoped ViewModel and re-fire navigation every time the screen
    // is reopened (the screen would close itself instantly). The screen calls
    // load() on entry, so a buffered channel delivers the event exactly once.
    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    private val resolvedId: Int = accountId ?: SessionManager.currentAccountId

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
                    avatarUrl = profile.avatarUrl,
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
                _uiState.value = current.copy(isSaving = false)
                _saved.trySend(Unit)
            }.onFailure {
                _uiState.value = current.copy(
                    isSaving = false,
                    errorMessage = it.message ?: "Failed to save changes",
                )
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val current = _uiState.value as? EditProfileUiState.Editing ?: return
        if (current.isUploadingPhoto || resolvedId == -1) return
        _uiState.value = current.copy(isUploadingPhoto = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: error("Couldn't read the selected image")
                }
                repository.updateAvatar(resolvedId, bytes)
            }.onSuccess { url ->
                (_uiState.value as? EditProfileUiState.Editing)?.let {
                    _uiState.value = it.copy(avatarUrl = url, isUploadingPhoto = false)
                }
            }.onFailure { err ->
                (_uiState.value as? EditProfileUiState.Editing)?.let {
                    _uiState.value = it.copy(
                        isUploadingPhoto = false,
                        errorMessage = err.message ?: "Failed to upload photo",
                    )
                }
            }
        }
    }
}
