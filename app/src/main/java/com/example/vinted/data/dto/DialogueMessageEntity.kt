package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DialogueMessageEntity(
    @SerialName("dialogue_message_id") val messageId: Int,
    @SerialName("dialogue_id") val dialogueId: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("message_text") val text: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("is_read") val isRead: Boolean,
)
