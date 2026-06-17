package com.example.vinted.data

import androidx.compose.ui.graphics.Color
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.AccountSideInfoEntity
import com.example.vinted.data.dto.DialogueEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.data.dto.ItemPhotoEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.ChatMessage
import com.example.vinted.ui.models.Conversation
import io.github.jan.supabase.postgrest.from

interface IDialogueRepository {
    suspend fun getConversations(accountId: Int): List<Conversation>
    suspend fun getMessages(dialogueId: Int): List<ChatMessage>
    suspend fun sendMessage(dialogueId: Int, senderId: Int, text: String)
    suspend fun markRead(dialogueId: Int, accountId: Int)
    suspend fun getUnreadCount(accountId: Int): Int
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
        if (allDialogues.isEmpty()) return emptyList()

        val otherAccountIds = allDialogues.map { dialogue ->
            if (dialogue.creatorId == accountId) dialogue.receiverId else dialogue.creatorId
        }.distinct()
        val itemIds = allDialogues.mapNotNull { it.itemId }.distinct()

        val accountMap: Map<Int, AccountEntity> = if (otherAccountIds.isNotEmpty()) {
            client.from("account")
                .select { filter { isIn("account_id", otherAccountIds) } }
                .decodeList<AccountEntity>()
                .associateBy { it.accountId }
        } else emptyMap()

        val locationMap: Map<Int, String> = if (otherAccountIds.isNotEmpty()) {
            client.from("account_side_information")
                .select { filter { isIn("account_id", otherAccountIds) } }
                .decodeList<AccountSideInfoEntity>()
                .associate { it.accountId to (it.location ?: "") }
        } else emptyMap()

        val itemMap: Map<Int, ItemEntity> = if (itemIds.isNotEmpty()) {
            client.from("item")
                .select { filter { isIn("item_id", itemIds) } }
                .decodeList<ItemEntity>()
                .associateBy { it.itemId }
        } else emptyMap()

        val coverMap: Map<Int, String> = if (itemIds.isNotEmpty()) {
            client.from("item_photo")
                .select { filter { isIn("item_id", itemIds) } }
                .decodeList<ItemPhotoEntity>()
                .groupBy { it.itemId }
                .mapValues { (_, photos) -> photos.first().photoUrl }
        } else emptyMap()

        return allDialogues.mapIndexed { index, dialogue ->
            val otherAccountId = if (dialogue.creatorId == accountId)
                dialogue.receiverId else dialogue.creatorId

            val otherAccount = accountMap[otherAccountId]
            val item = dialogue.itemId?.let { itemMap[it] }

            val msgDtos = client.from("dialogue_message")
                .select { filter { eq("dialogue_id", dialogue.dialogueId) } }
                .decodeList<DialogueMessageEntity>()
            val messages = msgDtos.map { msg ->
                ChatMessage(
                    id = msg.messageId.toString(),
                    text = msg.text,
                    isFromMe = msg.senderId == accountId,
                    time = msg.timestamp.take(16).replace("T", " "),
                )
            }

            // Unread = incoming messages not yet marked read in the DB.
            val unreadCount = msgDtos.count { it.senderId != accountId && !it.isRead }
            val otherName = otherAccount?.accountName ?: "Unknown"

            Conversation(
                id = dialogue.dialogueId.toString(),
                dialogueId = dialogue.dialogueId,
                handle = otherName,
                initial = otherName.firstOrNull()?.uppercase() ?: "?",
                avatarColor = avatarColors[index % avatarColors.size],
                lastMessage = messages.lastOrNull()?.text ?: "",
                timeLabel = messages.lastOrNull()?.time?.takeLast(5) ?: "",
                unreadCount = unreadCount,
                itemTitle = item?.name ?: otherName,
                fromLocation = locationMap[otherAccountId].orEmpty(),
                coverImageUrl = dialogue.itemId?.let { coverMap[it] },
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

    override suspend fun markRead(dialogueId: Int, accountId: Int) {
        // Mark the other party's messages in this dialogue as read.
        client.from("dialogue_message").update(mapOf("is_read" to true)) {
            filter {
                eq("dialogue_id", dialogueId)
                neq("sender_id", accountId)
            }
        }
    }

    override suspend fun getUnreadCount(accountId: Int): Int {
        val asCreator = client.from("dialogue")
            .select { filter { eq("dialogue_creator_id", accountId) } }
            .decodeList<DialogueEntity>()
        val asReceiver = client.from("dialogue")
            .select { filter { eq("dialogue_receiver_id", accountId) } }
            .decodeList<DialogueEntity>()
        val ids = (asCreator + asReceiver).map { it.dialogueId }.distinct()
        if (ids.isEmpty()) return 0
        return client.from("dialogue_message")
            .select {
                filter {
                    isIn("dialogue_id", ids)
                    neq("sender_id", accountId)
                    eq("is_read", false)
                }
            }
            .decodeList<DialogueMessageEntity>()
            .size
    }
}
