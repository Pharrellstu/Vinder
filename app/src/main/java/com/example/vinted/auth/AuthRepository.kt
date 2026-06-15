package com.example.vinted.auth

import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from

interface IAuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun verifyOtp(email: String, token: String): Result<Unit>
    fun currentSession(): UserSession?
}

open class AuthRepository : IAuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        SupabaseClientInitialiser.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val account = SupabaseClientInitialiser.client.from("account")
            .select {
                filter { eq("account_email", email) }
            }
            .decodeSingle<AccountEntity>()
        SessionManager.currentAccountId = account.accountId
        SessionManager.currentEmail = account.accountEmail
    }

    override suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        SupabaseClientInitialiser.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun verifyOtp(email: String, token: String): Result<Unit> = runCatching {
        SupabaseClientInitialiser.client.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
    }

    override fun currentSession(): UserSession? =
        SupabaseClientInitialiser.client.auth.currentSessionOrNull()
}
