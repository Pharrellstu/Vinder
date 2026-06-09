package com.example.vinted.ui.models

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val time: String,
)
