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

    /**
     * Marks a conversation as read. Updates the in-memory state immediately so the cleared badge
     * survives navigation, and persists `is_read` to the DB so it survives a reload too.
     */
    fun markRead(conversationId: String) {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        val conversation = current.conversations.firstOrNull { it.id == conversationId } ?: return
        if (conversation.unreadCount == 0) return

        _uiState.value = MessagesUiState.Success(
            current.conversations.map {
                if (it.id == conversationId) it.copy(unreadCount = 0) else it
            }
        )

        val accountId = SessionManager.currentAccountId
        if (accountId == -1) return
        viewModelScope.launch {
            runCatching { repository.markDialogueRead(conversation.dialogueId, accountId) }
        }
    }
}
