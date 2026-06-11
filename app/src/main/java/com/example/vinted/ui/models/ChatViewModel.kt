package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.DialogueRepository
import com.example.vinted.data.IDialogueRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatViewModel(
    private val dialogueId: Int,
    private val repository: IDialogueRepository = DialogueRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val channel = SupabaseClientInitialiser.client.channel("dialogue-$dialogueId")

    init {
        loadMessages()
        subscribeToNewMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            runCatching {
                repository.getMessages(dialogueId)
            }.onSuccess { messages ->
                _uiState.value = ChatUiState.Success(messages)
            }.onFailure {
                _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages")
            }
        }
    }

    private fun subscribeToNewMessages() {
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "dialogue_message"
            filter = "dialogue_id=eq.$dialogueId"
        }.onEach { action ->
            val entity = action.decodeRecord<DialogueMessageEntity>()
            val newMessage = ChatMessage(
                id = entity.messageId.toString(),
                text = entity.text,
                isFromMe = entity.senderId == SessionManager.currentAccountId,
                time = entity.timestamp.take(16).replace("T", " "),
            )
            val current = _uiState.value
            if (current is ChatUiState.Success) {
                _uiState.value = current.copy(messages = current.messages + newMessage)
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun sendMessage(text: String) {
        val senderId = SessionManager.currentAccountId
        if (senderId == -1 || text.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.sendMessage(dialogueId, senderId, text)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            channel.unsubscribe()
        }
    }
}
