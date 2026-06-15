package com.example.vinted.data

@Deprecated("Use SessionManager.currentAccountId instead")
object SupabaseConfig {
    val CURRENT_ACCOUNT_ID: Int get() = SessionManager.currentAccountId
}
