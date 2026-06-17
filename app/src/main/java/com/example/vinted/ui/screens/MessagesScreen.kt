package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.components.InitialAvatar
import com.example.vinted.ui.models.Conversation
import com.example.vinted.ui.models.MessagesUiState
import com.example.vinted.ui.models.MessagesViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
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

    when (val state = uiState) {
        is MessagesUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is MessagesUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = Grey57)
                    TextButton(onClick = { viewModel.load() }) {
                        Text("Retry")
                    }
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
                        inboxUnreadCount = conversations.sumOf { it.unreadCount },
                    )
                },
            ) { padding ->
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
                        HorizontalDivider(color = Grey91, modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialAvatar(initial = conversation.initial, color = conversation.avatarColor)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.handle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Grey11,
                    modifier = Modifier.weight(1f),
                )
                Text(text = conversation.timeLabel, fontSize = 11.sp, color = Grey57)
            }
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = conversation.lastMessage,
                fontSize = 13.sp,
                color = Grey57,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
        }
        if (conversation.unreadCount > 0) {
            Spacer(modifier = Modifier.size(8.dp))
            UnreadBadge(conversation.unreadCount)
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
        // Preview shows loading state; live data requires a running Supabase instance
        MessagesScreen()
    }
}
