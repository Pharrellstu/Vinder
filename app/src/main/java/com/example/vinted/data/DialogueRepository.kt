package com.example.vinted.data

import androidx.compose.ui.graphics.Color
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.AccountSideInfoEntity
import com.example.vinted.data.dto.DialogueEntity
import com.example.vinted.data.dto.DialogueMessageAttachmentEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.data.dto.ItemPhotoEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.ChatMessage
import com.example.vinted.ui.models.Conversation
import com.example.vinted.util.TimeFormat
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// `message_text` is NOT NULL, so an image-only message stores this placeholder. MessageBubble
// suppresses the text line whenever an attachment is present, so it never renders in-bubble — it
// only surfaces as the conversation-list preview. Top-level so ChatViewModel can recognise an
// image message by its text and trigger the attachment lookup.
const val IMAGE_PLACEHOLDER_TEXT = "📷 Photo"

interface IDialogueRepository {
    suspend fun getConversations(accountId: Int): List<Conversation>
    suspend fun getOrCreateDialogue(accountId: Int, otherAccountId: Int, itemId: Int?): Conversation
    suspend fun getMessages(dialogueId: Int): List<ChatMessage>
    suspend fun sendMessage(dialogueId: Int, senderId: Int, text: String)
    suspend fun sendImageMessage(dialogueId: Int, senderId: Int, bytes: ByteArray, caption: String)
    suspend fun getAttachmentUrl(messageId: Int): String?
    suspend fun markDialogueRead(dialogueId: Int, accountId: Int)
    suspend fun markRead(dialogueId: Int, accountId: Int)
    suspend fun getUnreadCount(accountId: Int): Int
}

class DialogueRepository : IDialogueRepository {

    private companion object {
        // Image attachments reuse the public, non-path-restricted `item-photos` bucket
        // under a `chat/` prefix, so no new bucket or storage policy is required.
        const val ATTACHMENT_BUCKET = "item-photos"
    }

    private val client = SupabaseClientInitialiser.client

    private val avatarColors = listOf(
        Color(0xFF6C6FB5),
        Color(0xFFB08A4F),
        Color(0xFF4E8098),
        Color(0xFF7B9E6E),
        Color(0xFF9B6D7A),
    )

    // Bulk-fetch attachment links for the given messages, keyed by message id. There is at
    // most one attachment per message for this feature, so the first row per id wins. Mirrors
    // the item_photo/coverMap pattern to avoid an N+1 query per message.
    private suspend fun attachmentUrlsFor(messageIds: List<Int>): Map<Int, String> {
        if (messageIds.isEmpty()) return emptyMap()
        return client.from("dialogue_message_attachment")
            .select { filter { isIn("dialogue_message_id", messageIds) } }
            .decodeList<DialogueMessageAttachmentEntity>()
            .groupBy { it.messageId }
            .mapValues { (_, attachments) -> attachments.first().attachmentLink }
    }

    // Fetches a dialogue's messages in chronological order. Ordering by timestamp (then by the
    // serial id as a tiebreaker for identical stamps) keeps the conversation in send order.
    private suspend fun fetchMessages(dialogueId: Int): List<DialogueMessageEntity> =
        client.from("dialogue_message")
            .select {
                filter { eq("dialogue_id", dialogueId) }
                order("timestamp", Order.ASCENDING)
                order("dialogue_message_id", Order.ASCENDING)
            }
            .decodeList<DialogueMessageEntity>()

    // Maps message rows to UI models, resolving attachments in one bulk query and rendering each
    // timestamp as a local-time label. `accountId` is the viewer, used to flag own messages.
    private suspend fun toChatMessages(
        msgDtos: List<DialogueMessageEntity>,
        accountId: Int,
    ): List<ChatMessage> {
        val attachmentMap = attachmentUrlsFor(msgDtos.map { it.messageId })
        return msgDtos.map { msg ->
            ChatMessage(
                id = msg.messageId.toString(),
                text = msg.text,
                isFromMe = msg.senderId == accountId,
                time = TimeFormat.toLocalTimeLabel(msg.timestamp),
                attachmentUrl = attachmentMap[msg.messageId],
            )
        }
    }

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

        // Pair each conversation with its last-activity instant so the list can be ordered with the
        // most recently active thread first. Avatar colour is keyed off the other account id (not a
        // list position) so it stays stable as threads reorder.
        return allDialogues.map { dialogue ->
            val otherAccountId = if (dialogue.creatorId == accountId)
                dialogue.receiverId else dialogue.creatorId

            val otherAccount = accountMap[otherAccountId]
            val item = dialogue.itemId?.let { itemMap[it] }

            val msgDtos = fetchMessages(dialogue.dialogueId)
            val messages = toChatMessages(msgDtos, accountId)
            val lastActivity = msgDtos.lastOrNull()?.let { TimeFormat.toEpochMillis(it.timestamp) } ?: 0L

            // Unread = incoming messages not yet marked read in the DB.
            val unreadCount = msgDtos.count { it.senderId != accountId && !it.isRead }
            val otherName = otherAccount?.accountName ?: "Unknown"

            Conversation(
                id = dialogue.dialogueId.toString(),
                dialogueId = dialogue.dialogueId,
                handle = otherName,
                initial = otherName.firstOrNull()?.uppercase() ?: "?",
                avatarColor = avatarColors[otherAccountId % avatarColors.size],
                lastMessage = messages.lastOrNull()?.text ?: "",
                timeLabel = messages.lastOrNull()?.time ?: "",
                unreadCount = unreadCount,
                itemTitle = item?.name ?: otherName,
                fromLocation = locationMap[otherAccountId].orEmpty(),
                coverImageUrl = dialogue.itemId?.let { coverMap[it] },
                messages = messages,
            ) to lastActivity
        }
            .sortedByDescending { (_, lastActivity) -> lastActivity }
            .map { (conversation, _) -> conversation }
    }

    override suspend fun getOrCreateDialogue(
        accountId: Int,
        otherAccountId: Int,
        itemId: Int?,
    ): Conversation {
        // Normalise to canonical (min, max) pair so the query always matches the DB index.
        val creatorId = minOf(accountId, otherAccountId)
        val receiverId = maxOf(accountId, otherAccountId)
        val existing = client.from("dialogue")
            .select {
                filter {
                    eq("dialogue_creator_id", creatorId)
                    eq("dialogue_receiver_id", receiverId)
                }
            }
            .decodeList<DialogueEntity>()

        // Prefer a thread already tied to this item; otherwise reuse any existing
        // thread between the two users, and only create a new one if none exists.
        val dialogue = existing.firstOrNull { itemId != null && it.itemId == itemId }
            ?: existing.firstOrNull()
            // `item_id` is intentionally omitted: it's optional thread metadata and is
            // absent from the deployed `dialogue` schema, so naming it here is rejected.
            ?: client.from("dialogue").insert(
                buildJsonObject {
                    put("dialogue_creator_id", creatorId)
                    put("dialogue_receiver_id", receiverId)
                }
            ) { select() }.decodeSingle<DialogueEntity>()

        val otherAccount = client.from("account")
            .select { filter { eq("account_id", otherAccountId) } }
            .decodeList<AccountEntity>()
            .firstOrNull()
        val location = client.from("account_side_information")
            .select { filter { eq("account_id", otherAccountId) } }
            .decodeList<AccountSideInfoEntity>()
            .firstOrNull()
            ?.location.orEmpty()
        val item = dialogue.itemId?.let { id ->
            client.from("item")
                .select { filter { eq("item_id", id) } }
                .decodeList<ItemEntity>()
                .firstOrNull()
        }
        val coverUrl = dialogue.itemId?.let { id ->
            client.from("item_photo")
                .select { filter { eq("item_id", id) } }
                .decodeList<ItemPhotoEntity>()
                .firstOrNull()?.photoUrl
        }

        val msgDtos = fetchMessages(dialogue.dialogueId)
        val messages = toChatMessages(msgDtos, accountId)

        val otherName = otherAccount?.accountName ?: "Unknown"
        return Conversation(
            id = dialogue.dialogueId.toString(),
            dialogueId = dialogue.dialogueId,
            handle = otherName,
            initial = otherName.firstOrNull()?.uppercase() ?: "?",
            avatarColor = avatarColors[otherAccountId % avatarColors.size],
            lastMessage = messages.lastOrNull()?.text ?: "",
            timeLabel = messages.lastOrNull()?.time ?: "",
            unreadCount = msgDtos.count { it.senderId != accountId && !it.isRead },
            itemTitle = item?.name ?: otherName,
            fromLocation = location,
            coverImageUrl = coverUrl,
            messages = messages,
        )
    }

    override suspend fun getMessages(dialogueId: Int): List<ChatMessage> =
        toChatMessages(fetchMessages(dialogueId), SessionManager.currentAccountId)

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
        // request is ever sent. `timestamp` is stamped from the sender's device clock.
        client.from("dialogue_message").insert(
            buildJsonObject {
                put("dialogue_id", dialogueId)
                put("sender_id", senderId)
                put("message_text", text)
                put("is_read", false)
                put("timestamp", TimeFormat.nowIso())
            }
        )
    }

    override suspend fun sendImageMessage(dialogueId: Int, senderId: Int, bytes: ByteArray, caption: String) {
        // Insert the message row first (insert-with-select to obtain its new id), then upload the
        // image and link it via a dialogue_message_attachment row. A blank caption falls back to
        // IMAGE_PLACEHOLDER_TEXT so an uncaptioned photo still shows some text in the message list.
        val text = caption.ifBlank { IMAGE_PLACEHOLDER_TEXT }
        val message = client.from("dialogue_message").insert(
            buildJsonObject {
                put("dialogue_id", dialogueId)
                put("sender_id", senderId)
                put("message_text", text)
                put("is_read", false)
                put("timestamp", TimeFormat.nowIso())
            }
        ) { select() }.decodeSingle<DialogueMessageEntity>()

        val path = "chat/$dialogueId/${message.messageId}.jpg"
        client.storage[ATTACHMENT_BUCKET].upload(path, bytes)
        val publicUrl = client.storage[ATTACHMENT_BUCKET].publicUrl(path)

        client.from("dialogue_message_attachment").insert(
            buildJsonObject {
                put("dialogue_message_id", message.messageId)
                put("attachment_link", publicUrl)
            }
        )
    }

    override suspend fun getAttachmentUrl(messageId: Int): String? {
        // Direct Postgrest read, not realtime: dialogue_message_attachment is absent from the
        // supabase_realtime publication, so its inserts never stream to clients. There is at most
        // one attachment per message for this feature, so the first row wins.
        return client.from("dialogue_message_attachment")
            .select { filter { eq("dialogue_message_id", messageId) } }
            .decodeList<DialogueMessageAttachmentEntity>()
            .firstOrNull()
            ?.attachmentLink
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
