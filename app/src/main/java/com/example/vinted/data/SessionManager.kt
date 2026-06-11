package com.example.vinted.data

object SessionManager {
    var currentAccountId: Int = -1
    var currentEmail: String = ""

    fun isLoggedIn(): Boolean = currentAccountId != -1

    fun clear() {
        currentAccountId = -1
        currentEmail = ""
    }
}
