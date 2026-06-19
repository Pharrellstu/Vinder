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
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private val channel = SupabaseClientInitialiser.client.channel("dialogue-$dialogueId")

    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        // Only dialogue_message is in the supabase_realtime publication, so only its inserts
        // stream here. dialogue_message_attachment is NOT published, so an image's attachment URL
        // can't arrive via realtime — we resolve it with one extra single-row query per incoming
        // message. Chat messages arrive at human-typing speed, so the cost is negligible and this
        // avoids guessing whether a message is an image from its text (a caption hides that signal).
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "dialogue_message"
        }.onEach { action ->
            val entity = action.decodeRecord<DialogueMessageEntity>()
            if (entity.dialogueId != dialogueId) return@onEach
            val attachmentUrl = repository.getAttachmentUrl(entity.messageId)
            val newMessage = ChatMessage(
                id = entity.messageId.toString(),
                text = entity.text,
                isFromMe = entity.senderId == SessionManager.currentAccountId,
                time = entity.timestamp.take(16).replace("T", " "),
                attachmentUrl = attachmentUrl,
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
            }.onFailure {
                _sendError.value = it.message ?: "Failed to send message"
            }
        }
    }

    fun sendImageMessage(bytes: ByteArray, caption: String = "") {
        val senderId = SessionManager.currentAccountId
        if (senderId == -1 || bytes.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.sendImageMessage(dialogueId, senderId, bytes, caption.trim())
            }.onFailure {
                _sendError.value = it.message ?: "Failed to send image"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupScope.launch {
            channel.unsubscribe()
            cleanupScope.cancel()
        }
    }
}
