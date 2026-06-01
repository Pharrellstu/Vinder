package com.example.vinted

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.vinted.ui.screens.AddProductScreen
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.MessagesScreen
import com.example.vinted.ui.screens.ProfileScreen
import com.example.vinted.ui.screens.SearchResultsScreen
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

@Composable
fun VinderApp() {
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
