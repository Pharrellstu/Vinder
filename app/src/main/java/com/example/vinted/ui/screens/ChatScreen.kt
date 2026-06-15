package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.components.InitialAvatar
import com.example.vinted.ui.models.ChatMessage
import com.example.vinted.ui.models.ChatUiState
import com.example.vinted.ui.models.ChatViewModel
import com.example.vinted.ui.models.Conversation
import com.example.vinted.ui.models.sampleConversations
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey95
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversation: Conversation,
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(
        key = "chat-${conversation.dialogueId}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(dialogueId = conversation.dialogueId) as T
        },
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(
                            initial = conversation.initial,
                            color = conversation.avatarColor,
                            size = 32.dp,
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = conversation.handle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Grey11,
                        )
                    }
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
            ChatInputBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendMessage(text)
                        draft = ""
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { DateSeparator("Today, 14:12") }
            when (val state = uiState) {
                is ChatUiState.Loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = VinderAzure)
                    }
                }
                is ChatUiState.Error -> item {
                    Text(
                        text = state.message,
                        color = Grey57,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                }
                is ChatUiState.Success -> {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }
            item { Spacer(modifier = Modifier.size(8.dp)) }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        color = Grey57,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isFromMe) VinderAzure else Color.White
    val textColor = if (message.isFromMe) Color.White else Grey11
    val shape = if (message.isFromMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(text = message.text, fontSize = 13.sp, color = textColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Grey95),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message…", color = Grey57, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Grey95,
                unfocusedContainerColor = Grey95,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Grey11,
                unfocusedTextColor = Grey11,
            ),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(VinderAzure)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    VintedTheme {
        ChatScreen(conversation = sampleConversations.first())
    }
}
