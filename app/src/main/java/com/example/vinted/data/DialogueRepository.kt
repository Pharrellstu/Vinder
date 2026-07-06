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
import io.github.jan.supabase.SupabaseClient
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
    suspend fun markRead(dialogueId: Int, accountId: Int)
    suspend fun getUnreadCount(accountId: Int): Int
}

class DialogueRepository(
    private val client: SupabaseClient = SupabaseClientInitialiser.client,
) : IDialogueRepository {

    private companion object {
        // Image attachments reuse the public, non-path-restricted `item-photos` bucket
        // under a `chat/` prefix, so no new bucket or storage policy is required.
        const val ATTACHMENT_BUCKET = "item-photos"
    }

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
    ): List<ChatMessage> = toChatMessages(msgDtos, accountId, attachmentUrlsFor(msgDtos.map { it.messageId }))

    // Same as above but takes an already-fetched attachment map, so callers that batch across
    // multiple dialogues (getConversations) can resolve attachments in one query instead of one
    // per dialogue.
    private fun toChatMessages(
        msgDtos: List<DialogueMessageEntity>,
        accountId: Int,
        attachmentMap: Map<Int, String>,
    ): List<ChatMessage> {
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

    // Holds everything getConversations pre-fetches in bulk, so the per-dialogue mapping that
    // follows is pure lookups with no further I/O.
    private class ConversationLookups(
        val accountMap: Map<Int, AccountEntity>,
        val locationMap: Map<Int, String>,
        val itemMap: Map<Int, ItemEntity>,
        val coverMap: Map<Int, String>,
        val messagesByDialogue: Map<Int, List<DialogueMessageEntity>>,
        val attachmentMap: Map<Int, String>,
    )

    // Runs every bulk fetch getConversations needs for the given dialogues and returns them as one
    // holder. Fetching responsibility only — no Conversation is assembled here.
    private suspend fun fetchConversationLookups(
        allDialogues: List<DialogueEntity>,
        accountId: Int,
    ): ConversationLookups {
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

        // Bulk-fetch every dialogue's messages and their attachments in two queries total,
        // instead of once per dialogue, then group in Kotlin — same batching pattern as the
        // account/item/item_photo lookups above.
        val allDialogueIds = allDialogues.map { it.dialogueId }
        val messagesByDialogue: Map<Int, List<DialogueMessageEntity>> = client.from("dialogue_message")
            .select {
                filter { isIn("dialogue_id", allDialogueIds) }
                order("timestamp", Order.ASCENDING)
                order("dialogue_message_id", Order.ASCENDING)
            }
            .decodeList<DialogueMessageEntity>()
            .groupBy { it.dialogueId }
        val attachmentMap = attachmentUrlsFor(messagesByDialogue.values.flatten().map { it.messageId })

        return ConversationLookups(
            accountMap = accountMap,
            locationMap = locationMap,
            itemMap = itemMap,
            coverMap = coverMap,
            messagesByDialogue = messagesByDialogue,
            attachmentMap = attachmentMap,
        )
    }

    // Builds ONE Conversation from a dialogue, the viewer's accountId, and already-resolved inputs.
    // Pure (no I/O) so both getConversations (batched) and getOrCreateDialogue (single) share this
    // one construction site. Avatar colour is keyed off the other account id (not a list position)
    // so it stays stable as threads reorder. Unread = incoming messages not yet marked read in the
    // DB. itemTitle falls back to the other participant's name when the thread has no item.
    private fun assembleConversation(
        dialogue: DialogueEntity,
        accountId: Int,
        otherName: String,
        location: String,
        itemName: String?,
        coverUrl: String?,
        messages: List<ChatMessage>,
        msgDtos: List<DialogueMessageEntity>,
    ): Conversation {
        val otherAccountId = if (dialogue.creatorId == accountId)
            dialogue.receiverId else dialogue.creatorId

        return Conversation(
            id = dialogue.dialogueId.toString(),
            dialogueId = dialogue.dialogueId,
            handle = otherName,
            initial = otherName.firstOrNull()?.uppercase() ?: "?",
            avatarColor = avatarColors[otherAccountId % avatarColors.size],
            lastMessage = messages.lastOrNull()?.text ?: "",
            timeLabel = messages.lastOrNull()?.time ?: "",
            unreadCount = msgDtos.count { it.senderId != accountId && !it.isRead },
            itemTitle = itemName ?: otherName,
            fromLocation = location,
            coverImageUrl = coverUrl,
            messages = messages,
        )
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

        val lookups = fetchConversationLookups(allDialogues, accountId)

        // Pair each conversation with its last-activity instant so the list can be ordered with the
        // most recently active thread first.
        return allDialogues.map { dialogue ->
            val otherAccountId = if (dialogue.creatorId == accountId)
                dialogue.receiverId else dialogue.creatorId

            val msgDtos = lookups.messagesByDialogue[dialogue.dialogueId].orEmpty()
            val messages = toChatMessages(msgDtos, accountId, lookups.attachmentMap)
            val lastActivity = msgDtos.lastOrNull()?.let { TimeFormat.toEpochMillis(it.timestamp) } ?: 0L

            assembleConversation(
                dialogue = dialogue,
                accountId = accountId,
                otherName = lookups.accountMap[otherAccountId]?.accountName ?: "Unknown",
                location = lookups.locationMap[otherAccountId].orEmpty(),
                itemName = dialogue.itemId?.let { lookups.itemMap[it] }?.name,
                coverUrl = dialogue.itemId?.let { lookups.coverMap[it] },
                messages = messages,
                msgDtos = msgDtos,
            ) to lastActivity
        }
            .sortedByDescending { (_, lastActivity) -> lastActivity }
            .map { (conversation, _) -> conversation }
    }

    // All dialogue rows between the two accounts, matched in either creator/receiver direction.
    // Both the initial lookup and the post-insert concurrency re-fetch go through this one method,
    // so the participant predicate is written exactly once.
    // A dialogue may have been created by either participant, so match both directions rather than
    // canonicalising to (min, max). Canonicalising would break the insert: the
    // `dialogue_insert_creator` RLS policy requires dialogue_creator_id = the signed-in user, so
    // whenever accountId > otherAccountId the canonical pair would put the *other* user in the
    // creator slot and the insert would be silently rejected by RLS.
    private suspend fun findExistingDialogue(accountId: Int, otherAccountId: Int): List<DialogueEntity> =
        client.from("dialogue")
            .select {
                filter {
                    or {
                        and {
                            eq("dialogue_creator_id", accountId)
                            eq("dialogue_receiver_id", otherAccountId)
                        }
                        and {
                            eq("dialogue_creator_id", otherAccountId)
                            eq("dialogue_receiver_id", accountId)
                        }
                    }
                }
            }
            .decodeList<DialogueEntity>()

    // Creates a new dialogue with the caller as creator, falling back to a re-fetch if a concurrent
    // insert by the other participant beat us.
    private suspend fun insertDialogue(accountId: Int, otherAccountId: Int): DialogueEntity =
        // `item_id` is intentionally omitted: it's optional thread metadata and is
        // absent from the deployed `dialogue` schema, so naming it here is rejected.
        try {
            client.from("dialogue").insert(
                buildJsonObject {
                    put("dialogue_creator_id", accountId)
                    put("dialogue_receiver_id", otherAccountId)
                }
            ) { select() }.decodeSingle<DialogueEntity>()
        } catch (_: Exception) {
            // Concurrent insert by the other participant beat us — re-fetch the row.
            findExistingDialogue(accountId, otherAccountId).first()
        }

    // Single-row counterparts to the batched lookups in fetchConversationLookups. getOrCreateDialogue
    // resolves one dialogue at a time, so these fetch by a single id rather than an isIn() list.
    private suspend fun fetchAccount(accountId: Int): AccountEntity? =
        client.from("account")
            .select { filter { eq("account_id", accountId) } }
            .decodeList<AccountEntity>()
            .firstOrNull()

    private suspend fun fetchLocation(accountId: Int): String =
        client.from("account_side_information")
            .select { filter { eq("account_id", accountId) } }
            .decodeList<AccountSideInfoEntity>()
            .firstOrNull()
            ?.location.orEmpty()

    // itemId is nullable: a thread need not reference an item, in which case there's nothing to fetch.
    private suspend fun fetchItem(itemId: Int?): ItemEntity? =
        itemId?.let { id ->
            client.from("item")
                .select { filter { eq("item_id", id) } }
                .decodeList<ItemEntity>()
                .firstOrNull()
        }

    private suspend fun fetchCoverUrl(itemId: Int?): String? =
        itemId?.let { id ->
            client.from("item_photo")
                .select { filter { eq("item_id", id) } }
                .decodeList<ItemPhotoEntity>()
                .firstOrNull()?.photoUrl
        }

    override suspend fun getOrCreateDialogue(
        accountId: Int,
        otherAccountId: Int,
        itemId: Int?,
    ): Conversation {
        val existing = findExistingDialogue(accountId, otherAccountId)

        // Prefer a thread already tied to this item; otherwise reuse any existing
        // thread between the two users, and only create a new one if none exists.
        val dialogue = existing.firstOrNull { itemId != null && it.itemId == itemId }
            ?: existing.firstOrNull()
            ?: insertDialogue(accountId, otherAccountId)

        val otherAccount = fetchAccount(otherAccountId)
        val location = fetchLocation(otherAccountId)
        val item = fetchItem(dialogue.itemId)
        val coverUrl = fetchCoverUrl(dialogue.itemId)

        val msgDtos = fetchMessages(dialogue.dialogueId)
        val messages = toChatMessages(msgDtos, accountId)

        return assembleConversation(
            dialogue = dialogue,
            accountId = accountId,
            otherName = otherAccount?.accountName ?: "Unknown",
            location = location,
            itemName = item?.name,
            coverUrl = coverUrl,
            messages = messages,
            msgDtos = msgDtos,
        )
    }

    override suspend fun getMessages(dialogueId: Int): List<ChatMessage> =
        toChatMessages(fetchMessages(dialogueId), SessionManager.currentAccountId)

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
