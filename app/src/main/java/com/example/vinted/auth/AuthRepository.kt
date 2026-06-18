package com.example.vinted.auth

import android.util.Log
import com.example.vinted.data.AccountPreferences
import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface IAuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun register(nickname: String, email: String, password: String): Result<Unit>
    suspend fun isNicknameTaken(nickname: String): Result<Boolean>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun verifyPasswordResetOtp(email: String, token: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    fun currentSession(): UserSession?
}

open class AuthRepository : IAuthRepository {

    private val supabase = SupabaseClientInitialiser.client

    override suspend fun login(email: String, password: String): Result<Unit> =
        runCatching {

            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val authenticatedEmail = supabase.auth.currentSessionOrNull()?.user?.email ?: email

            val account = supabase.from("account")
                .select { filter { eq("account_email", authenticatedEmail) } }
                .decodeSingleOrNull<AccountEntity>()

            SessionManager.currentAccountId = account?.accountId ?: SessionManager.NO_ACCOUNT_ID
            SessionManager.currentEmail = account?.accountEmail ?: authenticatedEmail
            AccountPreferences.save(SessionManager.currentAccountId, SessionManager.currentEmail)
        }.logError("login")

    override suspend fun register(nickname: String, email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject { put("nickname", nickname) }
            }
            Unit
        }.logError("register")

    override suspend fun isNicknameTaken(nickname: String): Result<Boolean> =
        runCatching {
            supabase.from("account")
                .select { filter { eq("account_name", nickname) } }
                .decodeList<AccountEntity>()
                .isNotEmpty()
        }.logError("isNicknameTaken")

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        runCatching {
            supabase.auth.resetPasswordForEmail(email)
        }.logError("sendPasswordResetEmail")

    override suspend fun verifyPasswordResetOtp(email: String, token: String): Result<Unit> =
        runCatching {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.RECOVERY,
                email = email,
                token = token
            )
        }.logError("verifyPasswordResetOtp")

    override suspend fun updatePassword(newPassword: String): Result<Unit> =
        runCatching {
            supabase.auth.updateUser {
                password = newPassword
            }
            Unit
        }.logError("updatePassword")

    override suspend fun logout(): Result<Unit> = runCatching {
        SupabaseClientInitialiser.client.auth.signOut()
        SessionManager.clear()
        AccountPreferences.clear()
    }

    override fun currentSession(): UserSession? =
        supabase.auth.currentSessionOrNull()

    private fun <T> Result<T>.logError(op: String): Result<T> = onFailure { e ->
        when (e) {
            is RestException -> Log.e(
                TAG,
                "[$op] HTTP ${e.statusCode} — ${e.error}: ${e.description}",
                e
            )
            else -> Log.e(TAG, "[$op] ${e::class.simpleName}: ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
