package com.example.vinted.data

import androidx.compose.ui.graphics.Color
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.DialogueEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.ChatMessage
import com.example.vinted.ui.models.Conversation
import io.github.jan.supabase.postgrest.from

interface IDialogueRepository {
    suspend fun getConversations(accountId: Int): List<Conversation>
    suspend fun sendMessage(dialogueId: Int, senderId: Int, text: String)
}

class DialogueRepository : IDialogueRepository {

    private val client = SupabaseClientInitialiser.client

    private val avatarColors = listOf(
        Color(0xFF6C6FB5),
        Color(0xFFB08A4F),
        Color(0xFF4E8098),
        Color(0xFF7B9E6E),
        Color(0xFF9B6D7A),
    )

    override suspend fun getConversations(accountId: Int): List<Conversation> {
        val asCreator = client.from("dialogue")
            .select { filter { eq("dialogue_creator_id", accountId) } }
            .decodeList<DialogueEntity>()

        val asReceiver = client.from("dialogue")
            .select { filter { eq("dialogue_receiver_id", accountId) } }
            .decodeList<DialogueEntity>()

        val allDialogues = (asCreator + asReceiver).distinctBy { it.dialogueId }

        val otherAccountIds = allDialogues.map { dialogue ->
            if (dialogue.creatorId == accountId) dialogue.receiverId else dialogue.creatorId
        }.distinct()

        val accountMap: Map<Int, AccountEntity> = if (otherAccountIds.isNotEmpty()) {
            client.from("account")
                .select { filter { isIn("account_id", otherAccountIds) } }
                .decodeList<AccountEntity>()
                .associateBy { it.accountId }
        } else emptyMap()

        return allDialogues.mapIndexed { index, dialogue ->
            val otherAccountId = if (dialogue.creatorId == accountId)
                dialogue.receiverId else dialogue.creatorId

            val otherAccount = accountMap[otherAccountId]

            val messages = client.from("dialogue_message")
                .select { filter { eq("dialogue_id", dialogue.dialogueId) } }
                .decodeList<DialogueMessageEntity>()
                .map { msg ->
                    ChatMessage(
                        id = msg.messageId.toString(),
                        text = msg.text,
                        isFromMe = msg.senderId == accountId,
                        time = msg.timestamp.take(16).replace("T", " "),
                    )
                }

            val unreadCount = messages.count { !it.isFromMe && true }

            Conversation(
                id = dialogue.dialogueId.toString(),
                dialogueId = dialogue.dialogueId,
                handle = otherAccount?.accountName ?: "Unknown",
                initial = otherAccount?.accountName?.firstOrNull()?.uppercase() ?: "?",
                avatarColor = avatarColors[index % avatarColors.size],
                lastMessage = messages.lastOrNull()?.text ?: "",
                timeLabel = messages.lastOrNull()?.time?.takeLast(5) ?: "",
                unreadCount = unreadCount,
                messages = messages,
            )
        }
    }

    override suspend fun sendMessage(dialogueId: Int, senderId: Int, text: String) {
        client.from("dialogue_message").insert(
            mapOf(
                "dialogue_id" to dialogueId,
                "sender_id" to senderId,
                "message_text" to text,
                "timestamp" to java.time.Instant.now().toString(),
                "is_read" to false,
            )
        )
    }
}
