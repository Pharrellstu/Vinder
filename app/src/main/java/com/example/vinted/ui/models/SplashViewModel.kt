package com.example.vinted.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.IAuthRepository
import com.example.vinted.data.AccountPreferences
import com.example.vinted.data.SessionManager
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

sealed class SplashState {
    object Loading : SplashState()
    object LoggedIn : SplashState()
    object LoggedOut : SplashState()
}

private const val SESSION_RESTORE_TIMEOUT_MS = 5_000L

class SplashViewModel(
    private val authRepo: IAuthRepository = AuthRepository(),
) : ViewModel() {

    var state by mutableStateOf<SplashState>(SplashState.Loading)
        private set

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    private suspend fun restoreSession() {
        runCatching {
            val status = authRepo.awaitResolvedSessionStatus(SESSION_RESTORE_TIMEOUT_MS)
                ?: return@runCatching

            if (status is SessionStatus.Authenticated) {
                val saved = AccountPreferences.load()
                if (saved != null) {
                    SessionManager.currentAccountId = saved.first
                    SessionManager.currentEmail = saved.second
                    state = SplashState.LoggedIn
                    return
                }

                val sessionEmail = authRepo.currentSession()?.user?.email ?: return@runCatching
                val account = authRepo.findAccountByEmail(sessionEmail) ?: return@runCatching

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
