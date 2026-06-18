package com.example.vinted.data

object SessionManager {
    // Sentinel for "no app-level account row resolved yet". A user can be
    // authenticated by GoTrue while this stays unset (e.g. profile row missing).
    const val NO_ACCOUNT_ID: Int = -1

    var currentAccountId: Int = NO_ACCOUNT_ID
    var currentEmail: String = ""

    fun isLoggedIn(): Boolean = currentAccountId != NO_ACCOUNT_ID

    fun clear() {
        currentAccountId = NO_ACCOUNT_ID
        currentEmail = ""
    }
}
