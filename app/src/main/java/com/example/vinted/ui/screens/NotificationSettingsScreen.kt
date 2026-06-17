package com.example.vinted.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.components.RowDivider
import com.example.vinted.ui.components.SettingsSection
import com.example.vinted.ui.components.SettingsToggleRow
import com.example.vinted.ui.models.NotificationSettingsViewModel
import com.example.vinted.ui.models.NotificationType
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VintedTheme

private fun groupIcon(group: String): ImageVector = when (group) {
    "Messages" -> Icons.Outlined.ChatBubbleOutline
    "Marketing" -> Icons.Outlined.Campaign
    else -> Icons.Outlined.Notifications
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: NotificationSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val groups = NotificationType.entries.groupBy { it.group }

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Grey11,
                    )
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            groups.forEach { (group, types) ->
                SettingsSection(title = group) {
                    types.forEachIndexed { index, type ->
                        if (index > 0) RowDivider()
                        SettingsToggleRow(
                            icon = groupIcon(group),
                            title = type.title,
                            subtitle = type.description,
                            checked = state.enabled[type] ?: type.defaultEnabled,
                            onCheckedChange = { viewModel.setEnabled(type, it) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationSettingsScreenPreview() {
    VintedTheme {
        NotificationSettingsScreen()
    }
}
