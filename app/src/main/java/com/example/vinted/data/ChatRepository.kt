package com.example.vinted.data

import com.example.vinted.ui.models.ChatMessage

object ChatRepository {
    suspend fun getMessages(dialogueId: Int): List<ChatMessage> = emptyList()

    suspend fun sendMessage(dialogueId: Int, senderId: String, text: String) = Unit
}
