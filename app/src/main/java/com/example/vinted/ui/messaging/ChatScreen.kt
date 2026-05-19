package com.example.vinted.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.AppBackground
import com.example.vinted.ui.theme.BrandPrimary
import com.example.vinted.ui.theme.BrandSecondary
import com.example.vinted.ui.theme.DividerColor
import com.example.vinted.ui.theme.SurfaceWhite
import com.example.vinted.ui.theme.TextPrimary
import com.example.vinted.ui.theme.TextSecondary
import com.example.vinted.ui.theme.VintedTheme

data class ChatMessage(
    val id: Long,
    val body: String,
    val timestamp: String,
    val fromMe: Boolean
)

data class ChatPartner(
    val name: String,
    val itemTitle: String,
    val itemPrice: String,
    val avatarInitial: Char
)

@Composable
fun ChatScreen(
    partner: ChatPartner,
    onBack: () -> Unit = {}
) {
    val messages = remember { mutableStateListOf(*sampleMessages.toTypedArray()) }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = { ChatTopBar(partner = partner, onBack = onBack) },
        bottomBar = {
            ChatComposer(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) {
                        messages += ChatMessage(
                            id = (messages.maxOfOrNull { it.id } ?: 0L) + 1,
                            body = trimmed,
                            timestamp = "now",
                            fromMe = true
                        )
                        draft = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ItemContextBanner(partner = partner)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(partner: ChatPartner, onBack: () -> Unit) {
    Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBubble(initial = partner.avatarInitial, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = partner.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Active recently",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
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
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
        )
        HorizontalDivider(color = DividerColor)
    }
}

@Composable
private fun ItemContextBanner(partner: ChatPartner) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandSecondary.copy(alpha = 0.35f))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.itemTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = partner.itemPrice,
                fontSize = 13.sp,
                color = BrandPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = DividerColor)
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.fromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.fromMe) BrandPrimary else SurfaceWhite
    val textColor = if (message.fromMe) SurfaceWhite else TextPrimary
    val shape = if (message.fromMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.body,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = message.timestamp,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(SurfaceWhite)
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(color = DividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Message…",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            val sendEnabled = value.isNotBlank()
            IconButton(
                onClick = onSend,
                enabled = sendEnabled,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (sendEnabled) BrandPrimary else BrandSecondary.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = SurfaceWhite
                )
            }
        }
    }
}

@Composable
internal fun AvatarBubble(initial: Char, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandSecondary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.uppercaseChar().toString(),
            color = SurfaceWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

private val sampleMessages = listOf(
    ChatMessage(1, "Hi! Is this still available?", "10:14", fromMe = false),
    ChatMessage(2, "Yes, it is :)", "10:15", fromMe = true),
    ChatMessage(3, "Would you take €20 for it?", "10:16", fromMe = false),
    ChatMessage(4, "I can do €22 including shipping.", "10:18", fromMe = true)
)

@Preview(showBackground = true, name = "Chat — thread")
@Composable
private fun ChatScreenPreview() {
    VintedTheme {
        ChatScreen(
            partner = ChatPartner(
                name = "Sofia",
                itemTitle = "Vintage denim jacket — size M",
                itemPrice = "€25.00",
                avatarInitial = 'S'
            )
        )
    }
}
