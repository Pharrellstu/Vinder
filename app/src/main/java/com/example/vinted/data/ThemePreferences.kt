package com.example.vinted.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user's light/dark choice. Initialise once from the application context (see
 * [com.example.vinted.VinderApplication]) before reading, mirroring the other *Preferences objects.
 */
object ThemePreferences {

    private const val PREFS_NAME = "vinder_theme"
    private const val KEY_DARK = "dark"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDark(): Boolean = prefs.getBoolean(KEY_DARK, false)

    fun setDark(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, enabled).apply()
    }
}
