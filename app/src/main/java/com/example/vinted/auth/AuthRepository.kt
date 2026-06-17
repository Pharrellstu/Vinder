package com.example.vinted.auth

import android.util.Log
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
            // Step 1: authenticate with GoTrue. A wrong password fails here and
            // surfaces as "invalid credentials" — never reaching the profile fetch.
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Step 2: load the app-level profile row. Use the authenticated
            // session's email (canonical, server-side) rather than the raw input.
            val authenticatedEmail = supabase.auth.currentSessionOrNull()?.user?.email ?: email

            // decodeSingleOrNull (not decodeSingle) so that a missing or
            // RLS-filtered row yields null instead of throwing a cryptic
            // "list is empty" error. Auth has already succeeded at this point,
            // so an absent profile row must not fail the whole login.
            val account = supabase.from("account")
                .select { filter { eq("account_email", authenticatedEmail) } }
                .decodeSingleOrNull<AccountEntity>()

            SessionManager.currentAccountId = account?.accountId ?: SessionManager.NO_ACCOUNT_ID
            SessionManager.currentEmail = account?.accountEmail ?: authenticatedEmail
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
