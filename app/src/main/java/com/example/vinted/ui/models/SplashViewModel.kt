package com.example.vinted.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.AccountPreferences
import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed class SplashState {
    object Loading : SplashState()
    object LoggedIn : SplashState()
    object LoggedOut : SplashState()
}

private const val SESSION_RESTORE_TIMEOUT_MS = 5_000L

class SplashViewModel : ViewModel() {

    var state by mutableStateOf<SplashState>(SplashState.Loading)
        private set

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    private suspend fun restoreSession() {
        runCatching {
            val status = withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
                SupabaseClientInitialiser.client.auth.sessionStatus
                    .first { it !is SessionStatus.Initializing }
            } ?: return@runCatching

            if (status is SessionStatus.Authenticated) {
                val saved = AccountPreferences.load()
                if (saved != null) {
                    SessionManager.currentAccountId = saved.first
                    SessionManager.currentEmail = saved.second
                    state = SplashState.LoggedIn
                    return
                }

                val sessionEmail = SupabaseClientInitialiser.client.auth
                    .currentSessionOrNull()?.user?.email ?: return@runCatching
                val account = SupabaseClientInitialiser.client.from("account")
                    .select { filter { eq("account_email", sessionEmail) } }
                    .decodeSingleOrNull<AccountEntity>() ?: return@runCatching

                SessionManager.currentAccountId = account.accountId
                SessionManager.currentEmail = account.accountEmail
                AccountPreferences.save(account.accountId, account.accountEmail)
                state = SplashState.LoggedIn
                return
            }
        }
        state = SplashState.LoggedOut
    }
}
