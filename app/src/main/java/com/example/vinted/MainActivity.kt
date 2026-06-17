package com.example.vinted

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.vinted.ui.models.SplashViewModel
import com.example.vinted.ui.screens.AddProductScreen
import com.example.vinted.ui.screens.ForgotPasswordScreen
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.LoginScreen
import com.example.vinted.ui.screens.MessagesScreen
import com.example.vinted.ui.screens.ProfileScreen
import com.example.vinted.ui.screens.RegisterScreen
import com.example.vinted.ui.screens.SearchResultsScreen
import com.example.vinted.ui.theme.VintedTheme

class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !splashViewModel.isAppReady }
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
    FORGOT_PASSWORD,
    HOME
}

@Composable
fun VinderApp() {
    var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }

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

        AuthScreen.HOME -> MainTabs()
    }
}

@Composable
private fun MainTabs() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAddProduct by rememberSaveable { mutableStateOf(false) }

    // The "+" FAB (index 2) opens the Add Product flow as a modal over the current tab.
    val onTabSelected: (Int) -> Unit = { index ->
        if (index == 2) showAddProduct = true else selectedTab = index
    }

    if (showAddProduct) {
        AddProductScreen(onBack = { showAddProduct = false })
        return
    }

    when (selectedTab) {
        1 -> SearchResultsScreen(onTabSelected = onTabSelected)
        3 -> MessagesScreen(onTabSelected = onTabSelected)
        4 -> ProfileScreen(onTabSelected = onTabSelected)
        else -> HomeScreen(onTabSelected = onTabSelected)
    }
}
