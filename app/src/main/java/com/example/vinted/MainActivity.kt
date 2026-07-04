package com.example.vinted

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.vinted.ui.models.SplashState
import com.example.vinted.ui.models.SplashViewModel
import com.example.vinted.data.DialogueRepository
import com.example.vinted.data.InboxBadge
import com.example.vinted.data.SessionManager
import com.example.vinted.notifications.MessageListenerService
import com.example.vinted.notifications.VinderNotifications
import com.example.vinted.ui.models.Conversation
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.screens.AddProductScreen
import com.example.vinted.ui.screens.ChangePasswordScreen
import com.example.vinted.ui.screens.ChatScreen
import com.example.vinted.ui.screens.ForgotPasswordScreen
import com.example.vinted.ui.screens.EditProfileScreen
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.ItemDetailScreen
import com.example.vinted.ui.screens.LoginScreen
import com.example.vinted.ui.screens.MessagesScreen
import com.example.vinted.ui.screens.MyListingsScreen
import com.example.vinted.ui.screens.NotificationSettingsScreen
import com.example.vinted.ui.screens.OffersScreen
import com.example.vinted.ui.screens.OrderHistoryScreen
import com.example.vinted.ui.screens.WishlistScreen
import com.example.vinted.ui.screens.ProfileScreen
import com.example.vinted.ui.screens.RegisterScreen
import com.example.vinted.ui.screens.SearchResultsScreen
import com.example.vinted.ui.screens.SellerPublicProfileScreen
import com.example.vinted.ui.screens.SettingsScreen
import com.example.vinted.ui.screens.WishlistScreen
import com.example.vinted.ui.theme.VinderPalette
import com.example.vinted.ui.theme.VintedTheme
import com.example.vinted.util.ErrorMessages

private const val KEY_BATTERY_OPT_ASKED = "battery_opt_asked"

class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

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

        // The message listener relies on a foreground service holding a Realtime socket open.
        // Doze freezes that socket unless the app is exempt from battery optimization, so prompt
        // once. The "asked" flag keeps us from nagging on every launch if the user declines.
        val batteryPrefs = getSharedPreferences("vinder_battery_opt", MODE_PRIVATE)
        val requestBatteryExemption: () -> Unit = {
            val powerManager = getSystemService(PowerManager::class.java)
            val exempt = powerManager?.isIgnoringBatteryOptimizations(packageName) == true
            if (!exempt && !batteryPrefs.getBoolean(KEY_BATTERY_OPT_ASKED, false)) {
                batteryPrefs.edit().putBoolean(KEY_BATTERY_OPT_ASKED, true).apply()
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            }
        }

        splashScreen.setKeepOnScreenCondition { splashViewModel.state is SplashState.Loading }
        enableEdgeToEdge()
        setContent {
            // Keep the status-bar icons legible against the themed background.
            val darkTheme = VinderPalette.dark
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !darkTheme
            }
            VintedTheme {
                VinderApp(
                    splashViewModel = splashViewModel,
                    onRequestNotificationPermission = requestNotificationPermission,
                    onRequestBatteryExemption = requestBatteryExemption,
                )
            }
        }
    }
}

private enum class AuthScreen {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    HOME
}

@Composable
fun VinderApp(
    splashViewModel: SplashViewModel = viewModel(),
    onRequestNotificationPermission: () -> Unit = {},
    onRequestBatteryExemption: () -> Unit = {},
) {
    val context = LocalContext.current
    val splashState = splashViewModel.state

    if (splashState is SplashState.Loading) return

    var authScreen by rememberSaveable {
        mutableStateOf(if (splashState is SplashState.LoggedIn) AuthScreen.HOME else AuthScreen.LOGIN)
    }

    when (authScreen) {
        AuthScreen.LOGIN -> LoginScreen(
            onLoginSuccess = { authScreen = AuthScreen.HOME },
            onNavigateToRegister = { authScreen = AuthScreen.REGISTER },
            onNavigateToForgotPassword = { authScreen = AuthScreen.FORGOT_PASSWORD }
        )

        AuthScreen.REGISTER -> RegisterScreen(
            onNavigateToLogin = { authScreen = AuthScreen.LOGIN }
        )

        AuthScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(
            onNavigateToLogin = { authScreen = AuthScreen.LOGIN }
        )

        AuthScreen.HOME -> {
            LaunchedEffect(Unit) {
                onRequestNotificationPermission()
                onRequestBatteryExemption()
                MessageListenerService.start(context)
            }
            MainTabs(onLoggedOut = {
                MessageListenerService.stop(context)
                authScreen = AuthScreen.LOGIN
            })
        }
    }
}

// One nullable route replaces the 13 boolean/nullable state vars + priority-ordered if-chain
// this used to be. Adding a screen means adding one sealed variant + one `when` branch instead
// of inserting an `if...return` block at the right priority position.
private sealed interface MainOverlay {
    object AddProduct : MainOverlay
    object EditProfile : MainOverlay
    object Offers : MainOverlay
    object OrderHistory : MainOverlay
    object Wishlist : MainOverlay
    data class EditItem(val itemId: Int) : MainOverlay
    object MyListings : MainOverlay
    object Notifications : MainOverlay
    object ChangePassword : MainOverlay
    object Settings : MainOverlay
    data class Chat(val target: ChatTarget) : MainOverlay
    data class Seller(val accountId: Int) : MainOverlay
    data class ProductDetail(val product: Product) : MainOverlay
}

@Composable
private fun MainTabs(onLoggedOut: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    // Not rememberSaveable: ProductDetail/Chat carry non-Parcelable payloads (Product,
    // ChatTarget) and were already plain `remember` before this refactor, so this preserves
    // that behavior. The simple overlays that used to be individually rememberSaveable now
    // share this same non-saveable state — an accepted trade-off for collapsing 13 vars into 1.
    var currentOverlay by remember { mutableStateOf<MainOverlay?>(null) }
    var profileReloadToken by rememberSaveable { mutableStateOf(0) }
    val dialogueRepository = remember { DialogueRepository() }

    // Fetch the inbox unread count once on entry so the bottom-nav badge is
    // visible from every tab, not just after opening the Messages screen.
    LaunchedEffect(Unit) {
        val accountId = SessionManager.currentAccountId
        if (accountId != -1) {
            runCatching { InboxBadge.unread.value = dialogueRepository.getUnreadCount(accountId) }
        }
    }

    // The "+" FAB (index 2) opens the Add Product flow as a modal over the current tab.
    val onTabSelected: (Int) -> Unit = { index ->
        if (index == 2) currentOverlay = MainOverlay.AddProduct else selectedTab = index
    }

    when (val overlay = currentOverlay) {
        MainOverlay.AddProduct -> {
            AddProductScreen(
                onBack = { currentOverlay = null },
                onPosted = { product -> currentOverlay = MainOverlay.ProductDetail(product) },
            )
            return
        }

        MainOverlay.EditProfile -> {
            BackHandler { currentOverlay = null }
            EditProfileScreen(
                onBack = { currentOverlay = null },
                onSaved = {
                    currentOverlay = null
                    profileReloadToken++
                },
            )
            return
        }

        MainOverlay.Offers -> {
            BackHandler { currentOverlay = null }
            OffersScreen(onBack = { currentOverlay = null })
            return
        }

        MainOverlay.OrderHistory -> {
            BackHandler { currentOverlay = null }
            OrderHistoryScreen(onBack = { currentOverlay = null })
            return
        }

        MainOverlay.Wishlist -> {
            BackHandler { currentOverlay = null }
            WishlistScreen(
                onBack = { currentOverlay = null },
                onProductClick = { currentOverlay = MainOverlay.ProductDetail(it) },
            )
            return
        }

        // Editing a listing reuses the Add Product flow in edit mode. Its own ViewModel key
        // keeps it separate from the create flow's state. Sits on top of My Listings.
        is MainOverlay.EditItem -> {
            BackHandler { currentOverlay = null }
            AddProductScreen(
                editItemId = overlay.itemId,
                viewModel = viewModel(key = "addProductEdit"),
                onBack = { currentOverlay = null },
                onPosted = { currentOverlay = MainOverlay.MyListings },
            )
            return
        }

        MainOverlay.MyListings -> {
            BackHandler { currentOverlay = null }
            MyListingsScreen(
                onBack = { currentOverlay = null },
                onEditListing = { itemId -> currentOverlay = MainOverlay.EditItem(itemId) },
            )
            return
        }

        // Notifications and Change Password are sub-screens of Settings; backing out returns
        // to Settings rather than clearing the overlay entirely.
        MainOverlay.Notifications -> {
            BackHandler { currentOverlay = MainOverlay.Settings }
            NotificationSettingsScreen(onBack = { currentOverlay = MainOverlay.Settings })
            return
        }

        MainOverlay.ChangePassword -> {
            BackHandler { currentOverlay = MainOverlay.Settings }
            ChangePasswordScreen(onBack = { currentOverlay = MainOverlay.Settings })
            return
        }

        MainOverlay.Settings -> {
            SettingsScreen(
                onBack = { currentOverlay = null },
                onLoggedOut = onLoggedOut,
                onOpenNotifications = { currentOverlay = MainOverlay.Notifications },
                onChangePassword = { currentOverlay = MainOverlay.ChangePassword },
            )
            return
        }

        // A chat opened from a seller profile or item detail sits on top of them, so
        // backing out of the chat returns to wherever it was launched from.
        is MainOverlay.Chat -> {
            BackHandler { currentOverlay = null }
            SellerChatScreen(
                sellerId = overlay.target.sellerId,
                itemId = overlay.target.itemId,
                dialogueRepository = dialogueRepository,
                onBack = { currentOverlay = null },
            )
            return
        }

        // A seller profile opened from item detail sits on top, so back returns to the item.
        is MainOverlay.Seller -> {
            BackHandler { currentOverlay = null }
            SellerPublicProfileScreen(
                accountId = overlay.accountId,
                onBack = { currentOverlay = null },
                onMessageSeller = {
                    currentOverlay = MainOverlay.Chat(ChatTarget(sellerId = overlay.accountId, itemId = null))
                },
            )
            return
        }

        // Tapping a product on the Home feed opens the Item Detail page over the tabs.
        is MainOverlay.ProductDetail -> {
            BackHandler { currentOverlay = null }
            val product = overlay.product
            ItemDetailScreen(
                product = product,
                onBack = { currentOverlay = null },
                onViewSellerProfile = { currentOverlay = MainOverlay.Seller(product.sellerId) },
                onMessageSeller = {
                    currentOverlay = MainOverlay.Chat(
                        ChatTarget(sellerId = product.sellerId, itemId = product.id.toIntOrNull())
                    )
                },
                onEditListing = {
                    currentOverlay = product.id.toIntOrNull()?.let { MainOverlay.EditItem(it) }
                },
            )
            return
        }

        null -> Unit
    }

    when (selectedTab) {
        1 -> SearchResultsScreen(
            onTabSelected = onTabSelected,
            onProductClick = { currentOverlay = MainOverlay.ProductDetail(it) },
        )
        3 -> MessagesScreen(onBack = { onTabSelected(0) }, onTabSelected = onTabSelected)
        4 -> key(profileReloadToken) {
            ProfileScreen(
                onTabSelected = onTabSelected,
                onEditProfile = { currentOverlay = MainOverlay.EditProfile },
                onOpenSettings = { currentOverlay = MainOverlay.Settings },
                onShowOffers = { currentOverlay = MainOverlay.Offers },
                onShowOrders = { currentOverlay = MainOverlay.OrderHistory },
                onShowWishlist = { currentOverlay = MainOverlay.Wishlist },
                onShowMyListings = { currentOverlay = MainOverlay.MyListings },
                onOpenListing = { currentOverlay = MainOverlay.ProductDetail(it) },
            )
        }
        else -> HomeScreen(
            onTabSelected = onTabSelected,
            onProductClick = { currentOverlay = MainOverlay.ProductDetail(it) },
            onNotificationsClick = { currentOverlay = MainOverlay.Notifications },
            onSellClick = { currentOverlay = MainOverlay.AddProduct },
        )
    }
}

/** Identifies the seller (and optionally the item) a chat was opened for. */
private data class ChatTarget(val sellerId: Int, val itemId: Int?)

/**
 * Resolves (or creates) the dialogue with a seller before showing [ChatScreen].
 * Opened from a seller profile or item detail, where only the seller id is known.
 */
@Composable
private fun SellerChatScreen(
    sellerId: Int,
    itemId: Int?,
    dialogueRepository: DialogueRepository,
    onBack: () -> Unit,
) {
    var conversation by remember(sellerId, itemId) { mutableStateOf<Conversation?>(null) }
    var error by remember(sellerId, itemId) { mutableStateOf<String?>(null) }
    LaunchedEffect(sellerId, itemId) {
        val accountId = SessionManager.currentAccountId
        if (accountId == -1) {
            error = "You need to be signed in to message a seller."
            return@LaunchedEffect
        }
        runCatching { dialogueRepository.getOrCreateDialogue(accountId, sellerId, itemId) }
            .onSuccess { conversation = it }
            .onFailure {
                Log.e("SellerChatScreen", "Failed to open chat with seller $sellerId", it)
                error = ErrorMessages.friendlyMessage(it, "Couldn't open this chat.")
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
