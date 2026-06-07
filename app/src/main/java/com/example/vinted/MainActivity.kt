package com.example.vinted

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.LoginScreen
import com.example.vinted.ui.theme.VintedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VintedTheme {
                var loggedIn by remember { mutableStateOf(false) }
                if (loggedIn) {
                    HomeScreen()
                } else {
                    LoginScreen(onLoginSuccess = { loggedIn = true })
                }
            }
        }
    }
}
