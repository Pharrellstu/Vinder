package com.example.vinted.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.models.Conversation
import com.example.vinted.ui.models.MessagesUiState
import com.example.vinted.ui.models.MessagesViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey95
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onBack: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    viewModel: MessagesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reload on (re)entry so read state persists when returning to the inbox.
    LaunchedEffect(Unit) { viewModel.load() }

    when (val state = uiState) {
        is MessagesUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VinderAzure)
            }
        }

        is MessagesUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = Grey57)
                    TextButton(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }
        }

        is MessagesUiState.Success -> {
            val conversations = state.conversations
            var openConversationId by remember { mutableStateOf<String?>(null) }

            openConversationId?.let { id ->
                val conversation = conversations.firstOrNull { it.id == id }
                if (conversation != null) {
                    ChatScreen(
                        conversation = conversation,
                        onBack = { openConversationId = null },
                    )
                    return
                }
            }

            Scaffold(
                containerColor = Color.White,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text("Messages", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Grey11,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    )
                },
                bottomBar = {
                    BottomNavBar(
                        selectedIndex = 3,
                        onItemSelected = onTabSelected,
                    )
                },
            ) { padding ->
                if (conversations.isEmpty()) {
                    EmptyInbox(modifier = Modifier.fillMaxSize().padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        items(conversations, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = {
                                    viewModel.markRead(conversation.id)
                                    openConversationId = conversation.id
                                },
                            )
                            HorizontalDivider(color = Grey91, modifier = Modifier.padding(start = 88.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Grey95),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = Grey57,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("No messages yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "When you contact a seller about an item, your conversations will show up here.",
            fontSize = 13.sp,
            color = Grey57,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CoverThumbnail(
            url = conversation.coverImageUrl,
            fallbackColor = conversation.avatarColor,
            initial = conversation.initial,
        )
        Spacer(modifier = Modifier.size(12.dp))
        // Middle: item title / from location / latest message
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.itemTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Grey11,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (conversation.fromLocation.isNotBlank()) {
                Spacer(modifier = Modifier.size(1.dp))
                Text(
                    text = "from ${conversation.fromLocation}",
                    fontSize = 12.sp,
                    color = Grey57,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.size(3.dp))
            Text(
                text = conversation.lastMessage,
                fontSize = 13.sp,
                color = Grey36,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        // Right: share button + timeframe (+ unread badge)
        Column(horizontalAlignment = Alignment.End) {
            IconButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out \"${conversation.itemTitle}\" on Vinder!")
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = Grey57,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = conversation.timeLabel, fontSize = 11.sp, color = Grey57)
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.size(6.dp))
                UnreadBadge(conversation.unreadCount)
            }
        }
    }
}

@Composable
private fun CoverThumbnail(url: String?, fallbackColor: Color, initial: String) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Text(text = initial, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(VinderAzure),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MessagesScreenPreview() {
    VintedTheme {
        MessagesScreen()
    }
}
