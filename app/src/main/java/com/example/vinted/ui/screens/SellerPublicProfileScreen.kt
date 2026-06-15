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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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

private val sellerProfileTabs = listOf("Listings", "Sold", "Reviews")

/**
 * Read-only public profile for another seller, opened from the item detail
 * seller card. Reuses [ProfileViewModel] (keyed per seller so it never collides
 * with the signed-in user's own Profile tab) plus the shared profile components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerPublicProfileScreen(
    accountId: Int,
    onBack: () -> Unit = {},
    onMessageSeller: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(
        key = "seller-$accountId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(accountId = accountId) as T
        },
    ),
) {
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Text("Seller profile", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Grey11)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
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
                    item { SellerHeader(profile = state.profile) }
                    item {
                        ProfileStatsCard(
                            listed = state.profile.listedCount,
                            sold = state.profile.soldCount,
                            followers = state.profile.followerCount,
                        )
                    }
                    item { SellerActionButtons(onMessage = onMessageSeller) }
                    item {
                        ProfileTabRow(
                            tabs = sellerProfileTabs,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                        )
                    }
                    when (selectedTab) {
                        0 -> item {
                            ProfileListingGrid(
                                listings = state.listings,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        1 -> item {
                            ProfileListingGrid(
                                listings = state.soldItems,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        else -> item { EmptyTab(text = "No reviews yet") }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SellerHeader(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SellerAvatar(initial = profile.initial, color = profile.avatarColor)
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
            if (profile.location.isNotBlank()) {
                Text("·", fontSize = 13.sp, color = Grey57.copy(alpha = 0.5f))
                Text(profile.location, fontSize = 13.sp, color = Grey57)
            }
        }
        if (profile.bio.isNotBlank()) {
            Text(
                text = profile.bio,
                fontSize = 13.sp,
                color = Grey36,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 7.dp, bottom = 15.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SellerAvatar(initial: String, color: Color) {
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
private fun SellerActionButtons(onMessage: () -> Unit) {
    // Follow is a local visual toggle for now; persistence is a future ticket.
    var following by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionPill(
            label = if (following) "Following" else "Follow",
            bgColor = if (following) Color.White else VinderAzure,
            textColor = if (following) Grey11 else Color.White,
            borderColor = if (following) Grey91 else Color.Transparent,
            onClick = { following = !following },
            modifier = Modifier.weight(1f),
        )
        ActionPill(
            label = "Message",
            bgColor = Color.White,
            textColor = Grey11,
            borderColor = Grey91,
            onClick = onMessage,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
private fun EmptyTab(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = Grey57)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SellerPublicProfileScreenPreview() {
    VintedTheme {
        SellerPublicProfileScreen(accountId = 1)
    }
}
