package com.example.vinted.ui.models

import androidx.compose.ui.graphics.Color

data class Conversation(
    val id: String,
    val dialogueId: Int,
    val handle: String,
    val initial: String,
    val avatarColor: Color,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
)

val sampleConversations = listOf(
    Conversation(
        id = "c1",
        dialogueId = 1,
        handle = "lena.k",
        initial = "L",
        avatarColor = Color(0xFF6C6FB5),
        lastMessage = "Yes, still available! When would you like to pick up?",
        timeLabel = "2m",
        unreadCount = 2,
        messages = listOf(
            ChatMessage("m1", "Hi! Is this still available?", isFromMe = true, time = "14:12"),
            ChatMessage("m2", "Yes, it is!", isFromMe = false, time = "14:12"),
            ChatMessage("m3", "When would you like to pick up?", isFromMe = false, time = "14:13"),
            ChatMessage("m4", "Saturday afternoon works.", isFromMe = true, time = "14:14"),
        ),
    ),
    Conversation(
        id = "c2",
        dialogueId = 2,
        handle = "thrifted_by_anna",
        initial = "A",
        avatarColor = Color(0xFFB08A4F),
        lastMessage = "Sent the parcel this morning, tracking attached.",
        timeLabel = "1h",
        messages = listOf(
            ChatMessage("m1", "Sent the parcel this morning, tracking attached.", isFromMe = false, time = "09:02"),
        ),
    ),
    Conversation(
        id = "c3",
        dialogueId = 3,
        handle = "noah.dev",
        initial = "N",
        avatarColor = Color(0xFF4E8098),
        lastMessage = "Would you take €100 for it?",
        timeLabel = "3h",
        unreadCount = 1,
        messages = listOf(
            ChatMessage("m1", "Would you take €100 for it?", isFromMe = false, time = "11:40"),
        ),
    ),
    Conversation(
        id = "c4",
        dialogueId = 4,
        handle = "closet_clear",
        initial = "C",
        avatarColor = Color(0xFF7B9E6E),
        lastMessage = "Thanks for the smooth deal! ★★★★★",
        timeLabel = "Yesterday",
        messages = listOf(
            ChatMessage("m1", "Thanks for the smooth deal! ★★★★★", isFromMe = false, time = "18:21"),
        ),
    ),
    Conversation(
        id = "c5",
        dialogueId = 5,
        handle = "vintage_jules",
        initial = "J",
        avatarColor = Color(0xFFB08A4F),
        lastMessage = "Hi! Could I see a photo of the back?",
        timeLabel = "2d",
        messages = listOf(
            ChatMessage("m1", "Hi! Could I see a photo of the back?", isFromMe = false, time = "10:05"),
        ),
    ),
)
