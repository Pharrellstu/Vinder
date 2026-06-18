package com.example.vinted

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.vinted.data.DialogueRepository
import com.example.vinted.data.InboxBadge
import com.example.vinted.data.NotificationPreferences
import com.example.vinted.data.SessionManager
import com.example.vinted.notifications.MessageNotificationController
import com.example.vinted.notifications.VinderNotifications
import com.example.vinted.ui.models.Conversation
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.screens.AddProductScreen
import com.example.vinted.ui.screens.ChatScreen
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

        NotificationPreferences.init(this)
        VinderNotifications.createChannels(this)

        // Notifications simply stay off if the user declines; no further action needed.
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* result ignored */ }

        val requestNotificationPermission: () -> Unit = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            VintedTheme {
                VinderApp(onRequestNotificationPermission = requestNotificationPermission)
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
fun VinderApp(onRequestNotificationPermission: () -> Unit = {}) {
    var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }
    val context = LocalContext.current

    when (authScreen) {
        AuthScreen.LOGIN -> LoginScreen(
            onLoginSuccess = { authScreen = AuthScreen.HOME },
            onNavigateToRegister = { authScreen = AuthScreen.REGISTER }
        )

        AuthScreen.REGISTER -> RegisterScreen(
            onRegisterSuccess = { authScreen = AuthScreen.LOGIN },
            onNavigateToLogin = { authScreen = AuthScreen.LOGIN }
        )

        AuthScreen.HOME -> {
            LaunchedEffect(Unit) {
                onRequestNotificationPermission()
                MessageNotificationController.start(context)
            }
            MainTabs(onLoggedOut = {
                MessageNotificationController.stop()
                authScreen = AuthScreen.LOGIN
            })
        }
    }
}

@Composable
private fun MainTabs(onLoggedOut: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAddProduct by rememberSaveable { mutableStateOf(false) }
    var openProduct by remember { mutableStateOf<Product?>(null) }
    var openSellerId by remember { mutableStateOf<Int?>(null) }
    var openChat by remember { mutableStateOf<ChatTarget?>(null) }
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

    // A chat opened from a seller profile or item detail sits on top of them, so
    // backing out of the chat returns to wherever it was launched from.
    val chatTarget = openChat
    if (chatTarget != null) {
        BackHandler { openChat = null }
        SellerChatScreen(
            sellerId = chatTarget.sellerId,
            itemId = chatTarget.itemId,
            onBack = { openChat = null },
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
            onMessageSeller = { openChat = ChatTarget(sellerId = sellerId, itemId = null) },
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
            onMessageSeller = {
                openChat = ChatTarget(sellerId = product.sellerId, itemId = product.id.toIntOrNull())
            },
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

/** Identifies the seller (and optionally the item) a chat was opened for. */
private data class ChatTarget(val sellerId: Int, val itemId: Int?)

/**
 * Resolves (or creates) the dialogue with a seller before showing [ChatScreen].
 * Opened from a seller profile or item detail, where only the seller id is known.
 */
@Composable
private fun SellerChatScreen(sellerId: Int, itemId: Int?, onBack: () -> Unit) {
    var conversation by remember(sellerId, itemId) { mutableStateOf<Conversation?>(null) }
    var error by remember(sellerId, itemId) { mutableStateOf<String?>(null) }
    LaunchedEffect(sellerId, itemId) {
        val accountId = SessionManager.currentAccountId
        if (accountId == -1) {
            error = "You need to be signed in to message a seller."
            return@LaunchedEffect
        }
        runCatching { DialogueRepository().getOrCreateDialogue(accountId, sellerId, itemId) }
            .onSuccess { conversation = it }
            .onFailure {
                Log.e("SellerChatScreen", "Failed to open chat with seller $sellerId", it)
                error = it.message ?: "Couldn't open this chat."
            }
    }

    val convo = conversation
    val currentError = error
    when {
        currentError != null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = currentError)
        }
        convo == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        else -> ChatScreen(conversation = convo, onBack = onBack)
    }
}

