package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.components.RowDivider
import com.example.vinted.ui.components.SettingsRow
import com.example.vinted.ui.components.SettingsSection
import com.example.vinted.ui.components.SettingsToggleRow
import com.example.vinted.ui.models.SettingsViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VintedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
            SettingsSection(title = "Account") {
                SettingsRow(
                    icon = Icons.Outlined.Person,
                    title = "Email",
                    subtitle = state.email.ifBlank { "Not signed in" },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change password",
                    showChevron = true,
                    onClick = {},
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Preferences") {
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "Choose what you're notified about",
                    showChevron = true,
                    onClick = onOpenNotifications,
                )
                RowDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark mode",
                    checked = state.darkMode,
                    onCheckedChange = viewModel::setDarkMode,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Support") {
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Help centre",
                    showChevron = true,
                    onClick = {},
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy policy",
                    showChevron = true,
                    onClick = {},
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = "Vinder v1.0",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LogoutButton(
                isLoading = state.isLoggingOut,
                onClick = { viewModel.logout(onLoggedOut) },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogoutButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color(0xFFE53935),
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Log out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE53935))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    VintedTheme {
        SettingsScreen()
    }
}
