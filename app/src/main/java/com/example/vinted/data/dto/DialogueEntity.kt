package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DialogueEntity(
    @SerialName("dialogue_id") val dialogueId: Int,
    @SerialName("dialogue_creator_id") val creatorId: Int,
    @SerialName("dialogue_receiver_id") val receiverId: Int,
)
