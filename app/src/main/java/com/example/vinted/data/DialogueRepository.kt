package com.example.vinted.data

import androidx.compose.ui.graphics.Color
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.DialogueEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.ChatMessage
import com.example.vinted.ui.models.Conversation
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface IDialogueRepository {
    suspend fun getConversations(accountId: Int): List<Conversation>
    suspend fun getMessages(dialogueId: Int): List<ChatMessage>
    suspend fun sendMessage(dialogueId: Int, senderId: Int, text: String)
    suspend fun markDialogueRead(dialogueId: Int, accountId: Int)
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

            val entities = client.from("dialogue_message")
                .select { filter { eq("dialogue_id", dialogue.dialogueId) } }
                .decodeList<DialogueMessageEntity>()

            val messages = entities.map { msg ->
                ChatMessage(
                    id = msg.messageId.toString(),
                    text = msg.text,
                    isFromMe = msg.senderId == accountId,
                    time = msg.timestamp.take(16).replace("T", " "),
                )
            }

            // Unread = incoming messages still flagged unread in the DB, not every received message.
            val unreadCount = entities.count { it.senderId != accountId && !it.isRead }

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

    override suspend fun getMessages(dialogueId: Int): List<ChatMessage> {
        return client.from("dialogue_message")
            .select { filter { eq("dialogue_id", dialogueId) } }
            .decodeList<DialogueMessageEntity>()
            .map { msg ->
                ChatMessage(
                    id = msg.messageId.toString(),
                    text = msg.text,
                    isFromMe = msg.senderId == SessionManager.currentAccountId,
                    time = msg.timestamp.take(16).replace("T", " "),
                )
            }
    }

    override suspend fun markDialogueRead(dialogueId: Int, accountId: Int) {
        // Flag every incoming (not-from-me) message in this dialogue as read. A homogeneous
        // Map<String, Boolean> serializes fine, unlike a mixed-type map.
        client.from("dialogue_message").update(mapOf("is_read" to true)) {
            filter {
                eq("dialogue_id", dialogueId)
                neq("sender_id", accountId)
            }
        }
    }

    override suspend fun sendMessage(dialogueId: Int, senderId: Int, text: String) {
        // Build a JsonObject rather than a Map<String, Any>: kotlinx.serialization has no
        // serializer for `Any`, so a heterogeneous map throws during serialization before the
        // request is ever sent. `timestamp` is omitted so the DB default (now()) applies.
        client.from("dialogue_message").insert(
            buildJsonObject {
                put("dialogue_id", dialogueId)
                put("sender_id", senderId)
                put("message_text", text)
                put("is_read", false)
            }
        )
    }
}
