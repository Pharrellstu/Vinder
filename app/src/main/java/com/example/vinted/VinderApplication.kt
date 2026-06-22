package com.example.vinted

import android.app.Application
import com.example.vinted.data.AccountPreferences
import com.example.vinted.data.NotificationPreferences
import com.example.vinted.data.ThemePreferences
import com.example.vinted.ui.theme.VinderPalette

class VinderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AccountPreferences.init(this)
        NotificationPreferences.init(this)
        ThemePreferences.init(this)
        // Apply the saved theme before any UI composes so the app opens in the chosen mode.
        VinderPalette.dark = ThemePreferences.isDark()
    }
}
