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
import com.example.vinted.data.SessionManager
import com.example.vinted.notifications.MessageListenerService
import com.example.vinted.notifications.VinderNotifications
import com.example.vinted.ui.models.ChatTarget
import com.example.vinted.ui.models.Conversation
import com.example.vinted.ui.models.MainOverlay
import com.example.vinted.ui.models.MainViewModel
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
private const val FAB_TAB_INDEX = 2

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

@Composable
private fun MainTabs(onLoggedOut: () -> Unit) {
    val vm: MainViewModel = viewModel()
    val overlay = vm.overlay
    if (overlay != null) {
        OverlayRouter(overlay = overlay, vm = vm, onLoggedOut = onLoggedOut)
    } else {
        TabContent(selectedTab = vm.selectedTab, vm = vm)
    }
}

@Composable
private fun OverlayRouter(
    overlay: MainOverlay,
    vm: MainViewModel,
    onLoggedOut: () -> Unit,
) {
    when (overlay) {
        MainOverlay.AddProduct -> AddProductScreen(
            onBack = { vm.clearOverlay() },
            onPosted = { product -> vm.showOverlay(MainOverlay.ProductDetail(product)) },
        )

        MainOverlay.EditProfile -> {
            BackHandler { vm.clearOverlay() }
            EditProfileScreen(
                onBack = { vm.clearOverlay() },
                onSaved = {
                    vm.clearOverlay()
                    vm.reloadProfile()
                },
            )
        }

        MainOverlay.Offers -> {
            BackHandler { vm.clearOverlay() }
            OffersScreen(onBack = { vm.clearOverlay() })
        }

        MainOverlay.OrderHistory -> {
            BackHandler { vm.clearOverlay() }
            OrderHistoryScreen(onBack = { vm.clearOverlay() })
        }

        MainOverlay.Wishlist -> {
            BackHandler { vm.clearOverlay() }
            WishlistScreen(
                onBack = { vm.clearOverlay() },
                onProductClick = { vm.showOverlay(MainOverlay.ProductDetail(it)) },
            )
        }

        // Editing a listing reuses the Add Product flow in edit mode. Its own ViewModel key
        // keeps it separate from the create flow's state. Sits on top of My Listings.
        is MainOverlay.EditItem -> {
            BackHandler { vm.clearOverlay() }
            AddProductScreen(
                editItemId = overlay.itemId,
                viewModel = viewModel(key = "addProductEdit"),
                onBack = { vm.clearOverlay() },
                onPosted = { vm.showOverlay(MainOverlay.MyListings) },
            )
        }

        MainOverlay.MyListings -> {
            BackHandler { vm.clearOverlay() }
            MyListingsScreen(
                onBack = { vm.clearOverlay() },
                onEditListing = { itemId -> vm.showOverlay(MainOverlay.EditItem(itemId)) },
            )
        }

        // Notifications and Change Password are sub-screens of Settings; backing out returns
        // to Settings rather than clearing the overlay entirely.
        MainOverlay.Notifications -> {
            BackHandler { vm.showOverlay(MainOverlay.Settings) }
            NotificationSettingsScreen(onBack = { vm.showOverlay(MainOverlay.Settings) })
        }

        MainOverlay.ChangePassword -> {
            BackHandler { vm.showOverlay(MainOverlay.Settings) }
            ChangePasswordScreen(onBack = { vm.showOverlay(MainOverlay.Settings) })
        }

        MainOverlay.Settings -> SettingsScreen(
            onBack = { vm.clearOverlay() },
            onLoggedOut = { vm.loggedOut(onLoggedOut) },
            onOpenNotifications = { vm.showOverlay(MainOverlay.Notifications) },
            onChangePassword = { vm.showOverlay(MainOverlay.ChangePassword) },
        )

        // A chat opened from a seller profile or item detail sits on top of them, so
        // backing out of the chat returns to wherever it was launched from.
        is MainOverlay.Chat -> {
            BackHandler { vm.clearOverlay() }
            SellerChatScreen(
                sellerId = overlay.target.sellerId,
                itemId = overlay.target.itemId,
                dialogueRepository = vm.dialogueRepository,
                onBack = { vm.clearOverlay() },
            )
        }

        // A seller profile opened from item detail sits on top, so back returns to the item.
        is MainOverlay.Seller -> {
            BackHandler { vm.clearOverlay() }
            SellerPublicProfileScreen(
                accountId = overlay.accountId,
                onBack = { vm.clearOverlay() },
                onMessageSeller = {
                    vm.showOverlay(MainOverlay.Chat(ChatTarget(sellerId = overlay.accountId, itemId = null)))
                },
            )
        }

        // Tapping a product on the Home feed opens the Item Detail page over the tabs.
        is MainOverlay.ProductDetail -> {
            BackHandler { vm.clearOverlay() }
            val product = overlay.product
            ItemDetailScreen(
                product = product,
                onBack = { vm.clearOverlay() },
                onViewSellerProfile = { vm.showOverlay(MainOverlay.Seller(product.sellerId)) },
                onMessageSeller = {
                    vm.showOverlay(
                        MainOverlay.Chat(
                            ChatTarget(sellerId = product.sellerId, itemId = product.id.toIntOrNull())
                        )
                    )
                },
                // A non-numeric id can't be edited, so closing the detail is the fallback.
                onEditListing = {
                    val editItemId = product.id.toIntOrNull()
                    if (editItemId != null) vm.showOverlay(MainOverlay.EditItem(editItemId)) else vm.clearOverlay()
                },
            )
        }
    }
}

@Composable
private fun TabContent(selectedTab: Int, vm: MainViewModel) {
    val onTabSelected: (Int) -> Unit = { index ->
        if (index == FAB_TAB_INDEX) vm.selectTabFab() else vm.selectTab(index)
    }
    when (selectedTab) {
        1 -> SearchResultsScreen(
            onTabSelected = onTabSelected,
            onProductClick = { vm.showOverlay(MainOverlay.ProductDetail(it)) },
        )
        3 -> MessagesScreen(onBack = { onTabSelected(0) }, onTabSelected = onTabSelected)
        4 -> key(vm.profileReloadToken) {
            ProfileScreen(
                onTabSelected = onTabSelected,
                onEditProfile = { vm.showOverlay(MainOverlay.EditProfile) },
                onOpenSettings = { vm.showOverlay(MainOverlay.Settings) },
                onShowOffers = { vm.showOverlay(MainOverlay.Offers) },
                onShowOrders = { vm.showOverlay(MainOverlay.OrderHistory) },
                onShowWishlist = { vm.showOverlay(MainOverlay.Wishlist) },
                onShowMyListings = { vm.showOverlay(MainOverlay.MyListings) },
                onOpenListing = { vm.showOverlay(MainOverlay.ProductDetail(it)) },
            )
        }
        else -> HomeScreen(
            onTabSelected = onTabSelected,
            onProductClick = { vm.showOverlay(MainOverlay.ProductDetail(it)) },
            onNotificationsClick = { vm.showOverlay(MainOverlay.Notifications) },
            onSellClick = { vm.showOverlay(MainOverlay.AddProduct) },
        )
    }
}

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
