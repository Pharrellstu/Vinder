package com.example.vinted

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.vinted.data.NotificationPreferences
import com.example.vinted.notifications.MessageNotificationController
import com.example.vinted.notifications.VinderNotifications
import com.example.vinted.ui.screens.AddProductScreen
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.LoginScreen
import com.example.vinted.ui.screens.MessagesScreen
import com.example.vinted.ui.screens.NotificationSettingsScreen
import com.example.vinted.ui.screens.ProfileScreen
import com.example.vinted.ui.screens.RegisterScreen
import com.example.vinted.ui.screens.SearchResultsScreen
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
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }

    // The "+" FAB (index 2) opens the Add Product flow as a modal over the current tab.
    val onTabSelected: (Int) -> Unit = { index ->
        if (index == 2) showAddProduct = true else selectedTab = index
    }

    if (showAddProduct) {
        AddProductScreen(onBack = { showAddProduct = false })
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

    when (selectedTab) {
        1 -> SearchResultsScreen(onTabSelected = onTabSelected)
        3 -> MessagesScreen(onTabSelected = onTabSelected)
        4 -> ProfileScreen(onTabSelected = onTabSelected, onOpenSettings = { showSettings = true })
        else -> HomeScreen(onTabSelected = onTabSelected)
    }
}
