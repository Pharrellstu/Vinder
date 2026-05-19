package com.example.vinted.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.AppBackground
import com.example.vinted.ui.theme.BrandPrimary
import com.example.vinted.ui.theme.DividerColor
import com.example.vinted.ui.theme.SurfaceWhite
import com.example.vinted.ui.theme.TextPrimary
import com.example.vinted.ui.theme.TextSecondary
import com.example.vinted.ui.theme.VintedTheme

data class Conversation(
    val id: String,
    val partner: ChatPartner,
    val lastMessage: String,
    val lastMessageAt: String,
    val unreadCount: Int
)

enum class MessagesFilter { ALL, UNREAD }

/**
 * Entry point for the messaging feature. Hand-rolls navigation between the
 * inbox and a chat thread — same pattern as `AddProductScreen` — so we don't
 * pull in androidx.navigation just for two screens.
 */
@Composable
fun MessagingScreen(onBackToApp: () -> Unit = {}) {
    var openConversationId by remember { mutableStateOf<String?>(null) }
    val conversations = remember { sampleConversations }
    val active = openConversationId?.let { id -> conversations.firstOrNull { it.id == id } }

    if (active == null) {
        MessagesScreen(
            conversations = conversations,
            onOpenConversation = { openConversationId = it.id },
            onBack = onBackToApp
        )
    } else {
        ChatScreen(
            partner = active.partner,
            onBack = { openConversationId = null }
        )
    }
}

@Composable
fun MessagesScreen(
    conversations: List<Conversation>,
    onOpenConversation: (Conversation) -> Unit,
    onBack: () -> Unit = {}
) {
    var filter by remember { mutableStateOf(MessagesFilter.ALL) }
    val unreadTotal = conversations.count { it.unreadCount > 0 }
    val filtered = when (filter) {
        MessagesFilter.ALL -> conversations
        MessagesFilter.UNREAD -> conversations.filter { it.unreadCount > 0 }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column {
                MessagesTopBar(onBack = onBack)
                MessagesFilterRow(
                    selected = filter,
                    unreadTotal = unreadTotal,
                    onSelect = { filter = it }
                )
                HorizontalDivider(color = DividerColor)
            }
        }
    ) { innerPadding ->
        when {
            conversations.isEmpty() -> EmptyInbox(modifier = Modifier.padding(innerPadding))
            filtered.isEmpty() -> AllCaughtUp(modifier = Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(SurfaceWhite),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filtered, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation) }
                    )
                    HorizontalDivider(
                        color = DividerColor,
                        modifier = Modifier.padding(start = 76.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Messages",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BrandPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
    )
}

@Composable
private fun MessagesFilterRow(
    selected: MessagesFilter,
    unreadTotal: Int,
    onSelect: (MessagesFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPill(
            label = "All",
            selected = selected == MessagesFilter.ALL,
            onClick = { onSelect(MessagesFilter.ALL) }
        )
        FilterPill(
            label = if (unreadTotal > 0) "Unread ($unreadTotal)" else "Unread",
            selected = selected == MessagesFilter.UNREAD,
            onClick = { onSelect(MessagesFilter.UNREAD) }
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) BrandPrimary else AppBackground
    val foreground = if (selected) SurfaceWhite else TextPrimary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val unread = conversation.unreadCount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBubble(initial = conversation.partner.avatarInitial, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.partner.name,
                    color = TextPrimary,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conversation.lastMessageAt,
                    color = if (unread) BrandPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = conversation.partner.itemTitle,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastMessage,
                    color = if (unread) TextPrimary else TextSecondary,
                    fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    UnreadBadge(count = conversation.unreadCount)
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    val label = if (count > 9) "9+" else count.toString()
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(BrandPrimary),
        contentAlignment = Alignment.Center
    ) {
        // includeFontPadding adds asymmetric whitespace inside tight glyph boxes;
        // disabling it (with matched lineHeight) lets contentAlignment actually centre.
        Text(
            text = label,
            style = TextStyle(
                color = SurfaceWhite,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
private fun AllCaughtUp(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You're all caught up",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "No unread messages right now.",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun EmptyInbox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No messages yet",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Conversations with buyers and sellers will appear here.",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

private val sampleConversations = listOf(
    Conversation(
        id = "c1",
        partner = ChatPartner(
            name = "Sofia",
            itemTitle = "Vintage denim jacket — size M",
            itemPrice = "€25.00",
            avatarInitial = 'S'
        ),
        lastMessage = "Would you take €20 for it?",
        lastMessageAt = "10:16",
        unreadCount = 2
    ),
    Conversation(
        id = "c2",
        partner = ChatPartner(
            name = "Marco",
            itemTitle = "Nike Air Max 90 — size 43",
            itemPrice = "€60.00",
            avatarInitial = 'M'
        ),
        lastMessage = "Thanks, I'll send the payment tonight.",
        lastMessageAt = "Yesterday",
        unreadCount = 0
    ),
    Conversation(
        id = "c3",
        partner = ChatPartner(
            name = "Elena",
            itemTitle = "IKEA Poäng armchair",
            itemPrice = "€40.00",
            avatarInitial = 'E'
        ),
        lastMessage = "Is pickup still possible this weekend?",
        lastMessageAt = "Mon",
        unreadCount = 1
    ),
    Conversation(
        id = "c4",
        partner = ChatPartner(
            name = "Jasper",
            itemTitle = "Lego Technic 42115",
            itemPrice = "€110.00",
            avatarInitial = 'J'
        ),
        lastMessage = "All pieces included? Looks complete in the photos.",
        lastMessageAt = "Sun",
        unreadCount = 0
    )
)

@Preview(showBackground = true, name = "Inbox — interactive (tap a row)")
@Composable
private fun MessagingInteractivePreview() {
    // Uses the feature entry point so tapping a conversation actually
    // navigates to ChatScreen — static screenshot looks like the inbox.
    VintedTheme {
        MessagingScreen()
    }
}

@Preview(showBackground = true, name = "Inbox — empty")
@Composable
private fun MessagesEmptyPreview() {
    VintedTheme {
        MessagesScreen(
            conversations = emptyList(),
            onOpenConversation = {}
        )
    }
}

@Preview(showBackground = true, name = "Inbox — 9+ unread badge")
@Composable
private fun MessagesOverflowBadgePreview() {
    VintedTheme {
        MessagesScreen(
            conversations = listOf(
                sampleConversations[0].copy(id = "p1", unreadCount = 12),
                sampleConversations[1].copy(id = "p2", unreadCount = 99)
            ),
            onOpenConversation = {}
        )
    }
}
