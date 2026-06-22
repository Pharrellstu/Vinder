package com.example.vinted.ui.screens
import com.example.vinted.ui.theme.SurfaceColor

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.components.ProfileListingGrid
import com.example.vinted.ui.components.ProfileStatsCard
import com.example.vinted.ui.components.ProfileTabRow
import com.example.vinted.data.SessionManager
import com.example.vinted.ui.models.ListingItem
import com.example.vinted.ui.models.ProfileUiState
import com.example.vinted.ui.models.ProfileViewModel
import com.example.vinted.ui.models.Product
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
    onEditProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onShowOffers: () -> Unit = {},
    onShowOrders: () -> Unit = {},
    onShowWishlist: () -> Unit = {},
    onShowMyListings: () -> Unit = {},
    onOpenListing: (Product) -> Unit = {},
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
    val context = LocalContext.current

    // The ViewModel is Activity-scoped, so its init-time load won't re-run when we
    // return here (e.g. after editing the profile). Reload whenever the screen is
    // (re)entered so saved changes are reflected.
    LaunchedEffect(Unit) { viewModel.load() }

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
                    item { ProfileHeader(profile = state.profile, onOpenSettings = onOpenSettings) }
                    item {
                        ProfileStatsCard(
                            listed = state.profile.listedCount,
                            sold = state.profile.soldCount,
                            followers = state.profile.followerCount,
                        )
                    }
                    item {
                        ProfileActionButtons(
                            onEditProfile = onEditProfile,
                            onShareProfile = { shareProfile(context, state.profile.handle) },
                            onShowOffers = onShowOffers,
                            onShowOrders = onShowOrders,
                            onShowWishlist = onShowWishlist,
                            onShowMyListings = onShowMyListings,
                        )
                    }
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
                                onListingClick = { onOpenListing(it.toProduct()) },
                            )
                        }
                    } else if (selectedTab == 1) {
                        item {
                            ProfileListingGrid(
                                listings = state.soldItems,
                                modifier = Modifier.padding(top = 10.dp),
                                onListingClick = { onOpenListing(it.toProduct()) },
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
private fun ProfileHeader(profile: UserProfile, onOpenSettings: () -> Unit = {}) {
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
            SettingsButton(onClick = onOpenSettings)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProfileAvatar(initial = profile.initial, color = profile.avatarColor, avatarUrl = profile.avatarUrl)
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
private fun SettingsButton(onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(elevation = 1.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(CircleShape)
            .background(SurfaceColor)
            .clickable(onClick = onClick)
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
private fun ProfileAvatar(initial: String, color: Color, avatarUrl: String? = null) {
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
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Text(
                text = initial.uppercase(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ProfileActionButtons(
    onEditProfile: () -> Unit,
    onShareProfile: () -> Unit,
    onShowOffers: () -> Unit,
    onShowOrders: () -> Unit,
    onShowWishlist: () -> Unit,
    onShowMyListings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                label = "Edit profile",
                bgColor = SurfaceColor,
                textColor = Grey11,
                borderColor = Grey91,
                onClick = onEditProfile,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = "Share profile",
                bgColor = VinderAzure,
                textColor = Color.White,
                onClick = onShareProfile,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                label = "Offers received",
                bgColor = SurfaceColor,
                textColor = Grey11,
                borderColor = Grey91,
                onClick = onShowOffers,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = "My orders",
                bgColor = SurfaceColor,
                textColor = Grey11,
                borderColor = Grey91,
                onClick = onShowOrders,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                label = "My listings",
                bgColor = SurfaceColor,
                textColor = Grey11,
                borderColor = Grey91,
                onClick = onShowMyListings,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = "Wishlist",
                bgColor = SurfaceColor,
                textColor = Grey11,
                borderColor = Grey91,
                onClick = onShowWishlist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Builds the minimal [Product] the Item Detail screen needs to open a profile listing.
 * The detail screen fetches photos/description/seller by id; since this is the signed-in
 * user's own profile, the seller is the current account.
 */
private fun ListingItem.toProduct(): Product = Product(
    id = id,
    name = name,
    price = price.toFloat(),
    sellerInitial = "",
    sellerName = "",
    rating = 0f,
    sellerId = SessionManager.currentAccountId,
)

private fun shareProfile(context: Context, handle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out $handle's profile on Vinder!")
    }
    context.startActivity(Intent.createChooser(send, "Share profile"))
}

@Composable
private fun ActionButton(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit = {},
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
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    VintedTheme {
        ProfileScreen()
    }
}
