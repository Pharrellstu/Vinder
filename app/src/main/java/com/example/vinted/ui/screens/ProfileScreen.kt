package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.components.ProfileListingGrid
import com.example.vinted.ui.components.ProfileStatsCard
import com.example.vinted.ui.components.ProfileTabRow
import com.example.vinted.ui.models.ProfileUiState
import com.example.vinted.ui.models.ProfileViewModel
import com.example.vinted.ui.models.UserProfile
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

private val profileTabs = listOf("Listings", "Sold", "Reviews")

@Composable
fun ProfileScreen(
    onTabSelected: (Int) -> Unit = {},
    accountId: Int? = null,
    viewModel: ProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(accountId = accountId) as T
        }
    ),
) {
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { BottomNavBar(selectedIndex = 4, onItemSelected = onTabSelected) },
        containerColor = Grey97,
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = VinderAzure)
                }
            }
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Grey57, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.load() }) {
                            Text("Retry", color = VinderAzure)
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item { ProfileHeader(profile = state.profile) }
                    item {
                        ProfileStatsCard(
                            listed = state.profile.listedCount,
                            sold = state.profile.soldCount,
                            followers = state.profile.followerCount,
                        )
                    }
                    item { ProfileActionButtons() }
                    item {
                        ProfileTabRow(
                            tabs = profileTabs,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                        )
                    }
                    if (selectedTab == 0) {
                        item {
                            ProfileListingGrid(
                                listings = state.listings,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    } else if (selectedTab == 1) {
                        item {
                            ProfileListingGrid(
                                listings = state.soldItems,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SettingsButton()
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProfileAvatar(initial = profile.initial, color = profile.avatarColor)
        Text(
            text = profile.handle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Grey11,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("★ ${profile.rating} · ${profile.reviewCount} reviews", fontSize = 13.sp, color = Grey57)
            Text("·", fontSize = 13.sp, color = Grey57.copy(alpha = 0.5f))
            Text(profile.location, fontSize = 13.sp, color = Grey57)
        }
        Text(
            text = profile.bio,
            fontSize = 13.sp,
            color = Grey36,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 7.dp, bottom = 15.dp),
        )
    }
}

@Composable
private fun SettingsButton() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(elevation = 1.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(CircleShape)
            .background(Color.White)
            .clickable {}
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = Grey11,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileAvatar(initial: String, color: Color) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Grey97)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun ProfileActionButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            label = "Edit profile",
            bgColor = Color.White,
            textColor = Color.Black,
            borderColor = Grey91,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Share profile",
            bgColor = VinderAzure,
            textColor = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable {}
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    VintedTheme {
        ProfileScreen()
    }
}
