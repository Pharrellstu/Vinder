package com.example.vinted.data

import android.content.Context
import android.content.SharedPreferences

object AccountPreferences {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("vinder_account", Context.MODE_PRIVATE)
    }

    fun save(accountId: Int, email: String) {
        prefs.edit()
            .putInt(KEY_ACCOUNT_ID, accountId)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun load(): Pair<Int, String>? {
        val id = prefs.getInt(KEY_ACCOUNT_ID, NO_ID)
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return if (id == NO_ID) null else id to email
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private const val KEY_ACCOUNT_ID = "account_id"
    private const val KEY_EMAIL = "email"
    private const val NO_ID = -1
}
