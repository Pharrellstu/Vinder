package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.DialogueRepository
import com.example.vinted.data.IDialogueRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MessagesUiState {
    object Loading : MessagesUiState()
    data class Success(val conversations: List<Conversation>) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

class MessagesViewModel(
    private val repository: IDialogueRepository = DialogueRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val accountId = SessionManager.currentAccountId
        if (accountId == -1) {
            _uiState.value = MessagesUiState.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _uiState.value = MessagesUiState.Loading
            runCatching {
                repository.getConversations(accountId)
            }.onSuccess { _uiState.value = MessagesUiState.Success(it) }
             .onFailure { _uiState.value = MessagesUiState.Error(it.message ?: "Failed to load messages") }
        }
    }
}
