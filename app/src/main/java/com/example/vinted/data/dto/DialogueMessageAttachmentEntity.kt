package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DialogueMessageAttachmentEntity(
    @SerialName("dialogue_message_attachment_id") val attachmentId: Int,
    @SerialName("dialogue_message_id") val messageId: Int,
    @SerialName("attachment_link") val attachmentLink: String,
)
