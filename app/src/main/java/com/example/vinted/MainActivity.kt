package com.example.vinted

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.vinted.data.DialogueRepository
import com.example.vinted.data.InboxBadge
import com.example.vinted.data.SessionManager
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.screens.AddProductScreen
import com.example.vinted.ui.screens.EditProfileScreen
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.ItemDetailScreen
import com.example.vinted.ui.screens.LoginScreen
import com.example.vinted.ui.screens.MessagesScreen
import com.example.vinted.ui.screens.NotificationSettingsScreen
import com.example.vinted.ui.screens.ProfileScreen
import com.example.vinted.ui.screens.RegisterScreen
import com.example.vinted.ui.screens.SearchResultsScreen
import com.example.vinted.ui.screens.SellerPublicProfileScreen
import com.example.vinted.ui.screens.SettingsScreen
import com.example.vinted.ui.theme.VintedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VintedTheme {
                VinderApp()
            }
        }
    }
}

private enum class AuthScreen {
    LOGIN,
    REGISTER,
    HOME
}

@Composable
fun VinderApp() {
    var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }

    when (authScreen) {
        AuthScreen.LOGIN -> LoginScreen(
            onLoginSuccess = { authScreen = AuthScreen.HOME },
            onNavigateToRegister = { authScreen = AuthScreen.REGISTER }
        )

        AuthScreen.REGISTER -> RegisterScreen(
            onRegisterSuccess = { authScreen = AuthScreen.LOGIN },
            onNavigateToLogin = { authScreen = AuthScreen.LOGIN }
        )

        AuthScreen.HOME -> MainTabs(onLoggedOut = { authScreen = AuthScreen.LOGIN })
    }
}

@Composable
private fun MainTabs(onLoggedOut: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAddProduct by rememberSaveable { mutableStateOf(false) }
    var openProduct by remember { mutableStateOf<Product?>(null) }
    var openSellerId by remember { mutableStateOf<Int?>(null) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    // Bumped after a profile edit so the Profile tab remounts and reloads fresh data.
    var profileReloadToken by rememberSaveable { mutableStateOf(0) }

    // Fetch the inbox unread count once on entry so the bottom-nav badge is
    // visible from every tab, not just after opening the Messages screen.
    LaunchedEffect(Unit) {
        val accountId = SessionManager.currentAccountId
        if (accountId != -1) {
            runCatching { InboxBadge.unread.value = DialogueRepository().getUnreadCount(accountId) }
        }
    }

    // The "+" FAB (index 2) opens the Add Product flow as a modal over the current tab.
    val onTabSelected: (Int) -> Unit = { index ->
        if (index == 2) showAddProduct = true else selectedTab = index
    }

    if (showAddProduct) {
        AddProductScreen(onBack = { showAddProduct = false })
        return
    }

    if (showEditProfile) {
        BackHandler { showEditProfile = false }
        EditProfileScreen(
            onBack = { showEditProfile = false },
            onSaved = {
                showEditProfile = false
                profileReloadToken++
            },
        )
        return
    }

    // Notifications is a sub-screen of Settings; backing out returns to Settings.
    if (showNotifications) {
        NotificationSettingsScreen(onBack = { showNotifications = false })
        return
    }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            onLoggedOut = onLoggedOut,
            onOpenNotifications = { showNotifications = true },
        )
        return
    }

    // A seller profile opened from item detail sits on top, so back returns to the item.
    val sellerId = openSellerId
    if (sellerId != null) {
        BackHandler { openSellerId = null }
        SellerPublicProfileScreen(
            accountId = sellerId,
            onBack = { openSellerId = null },
        )
        return
    }

    // Tapping a product on the Home feed opens the Item Detail page over the tabs.
    val product = openProduct
    if (product != null) {
        BackHandler { openProduct = null }
        ItemDetailScreen(
            product = product,
            onBack = { openProduct = null },
            onViewSellerProfile = { openSellerId = product.sellerId },
        )
        return
    }

    when (selectedTab) {
        1 -> SearchResultsScreen(onTabSelected = onTabSelected)
        3 -> MessagesScreen(onTabSelected = onTabSelected)
        4 -> key(profileReloadToken) {
            ProfileScreen(
                onTabSelected = onTabSelected,
                onEditProfile = { showEditProfile = true },
                onOpenSettings = { showSettings = true },
            )
        }
        else -> HomeScreen(onTabSelected = onTabSelected, onProductClick = { openProduct = it })
    }
}

