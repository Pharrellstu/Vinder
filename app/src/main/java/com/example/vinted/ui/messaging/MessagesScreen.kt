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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
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
    Scaffold(
        containerColor = AppBackground,
        topBar = { MessagesTopBar() }
    ) { innerPadding ->
        if (conversations.isEmpty()) {
            EmptyInbox(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(SurfaceWhite),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
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
private fun MessagesTopBar() {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "Messages",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
        )
        HorizontalDivider(color = DividerColor)
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
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(BrandPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            color = SurfaceWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
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

@Preview(showBackground = true, name = "Messages — inbox")
@Composable
private fun MessagesScreenPreview() {
    VintedTheme {
        MessagesScreen(
            conversations = sampleConversations,
            onOpenConversation = {}
        )
    }
}

@Preview(showBackground = true, name = "Messages — empty")
@Composable
private fun MessagesEmptyPreview() {
    VintedTheme {
        MessagesScreen(
            conversations = emptyList(),
            onOpenConversation = {}
        )
    }
}

@Preview(showBackground = true, name = "Messaging — full flow")
@Composable
private fun MessagingFlowPreview() {
    VintedTheme {
        MessagingScreen()
    }
}
