package com.example.vinted.util

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException

/**
 * Translates low-level exceptions into short, human-readable messages that are safe to show in the
 * UI.
 *
 * Supabase exceptions are noisy: a [RestException]'s own `message` embeds the request URL, every
 * request header and the HTTP method, and an [AuthRestException] tacks the raw error code onto its
 * description. Surfacing `throwable.message` straight to a screen dumps that whole technical log in
 * front of the user. Repositories already log the full detail for debugging, so the UI only needs a
 * friendly sentence.
 *
 * Pass a [fallback] tailored to the calling screen (e.g. "Failed to load listings") so that errors
 * we don't have a specific message for still read naturally.
 */
object ErrorMessages {

    const val GENERIC = "Something went wrong. Please try again."
    const val NETWORK = "No internet connection. Please check your network and try again."
    const val WEAK_PASSWORD =
        "Password is too weak. Use a longer mix of letters, numbers and symbols."

    /**
     * Maps [throwable] to a user-facing message. Known Supabase auth codes and HTTP status codes
     * get a specific sentence; network failures get [NETWORK]; everything else falls back to
     * [fallback].
     */
    fun friendlyMessage(throwable: Throwable, fallback: String = GENERIC): String = when (throwable) {
        is AuthWeakPasswordException -> WEAK_PASSWORD
        is AuthRestException ->
            messageForAuthCode(throwable.errorCode)
                ?: messageForStatus(throwable.statusCode)
                ?: fallback
        is RestException -> messageForStatus(throwable.statusCode) ?: fallback
        else -> if (isNetworkError(throwable)) NETWORK else fallback
    }

    /**
     * Friendly message for a known Supabase [AuthErrorCode], or `null` when the code has no
     * dedicated message and the caller should fall back to the HTTP status / its own default.
     */
    fun messageForAuthCode(code: AuthErrorCode?): String? = when (code) {
        AuthErrorCode.InvalidCredentials,
        AuthErrorCode.BadCodeVerifier -> "Incorrect email or password."

        AuthErrorCode.EmailNotConfirmed,
        AuthErrorCode.PhoneNotConfirmed -> "Please confirm your email address before logging in."

        AuthErrorCode.UserAlreadyExists,
        AuthErrorCode.EmailExists,
        AuthErrorCode.PhoneExists,
        AuthErrorCode.IdentityAlreadyExists -> "This email is already registered."

        AuthErrorCode.WeakPassword -> WEAK_PASSWORD

        AuthErrorCode.SamePassword -> "Your new password must be different from your current one."

        AuthErrorCode.OtpExpired,
        AuthErrorCode.FlowStateExpired -> "This code has expired. Request a new one and try again."

        AuthErrorCode.OtpDisabled -> "This verification method isn't available right now."

        AuthErrorCode.ValidationFailed,
        AuthErrorCode.BadJson,
        AuthErrorCode.UnexpectedAudience -> "Some details look invalid. Please check them and try again."

        AuthErrorCode.OverRequestRateLimit,
        AuthErrorCode.OverEmailSendRateLimit,
        AuthErrorCode.OverSmsSendRateLimit -> "Too many attempts. Please wait a moment and try again."

        AuthErrorCode.UserNotFound,
        AuthErrorCode.IdentityNotFound -> "We couldn't find an account for those details."

        AuthErrorCode.UserBanned -> "This account has been suspended. Please contact support."

        AuthErrorCode.SignupDisabled,
        AuthErrorCode.EmailProviderDisabled,
        AuthErrorCode.PhoneProviderDisabled,
        AuthErrorCode.ProviderDisabled -> "Sign-up is currently unavailable. Please try again later."

        AuthErrorCode.SessionExpired,
        AuthErrorCode.SessionNotFound,
        AuthErrorCode.RefreshTokenNotFound,
        AuthErrorCode.RefreshTokenAlreadyUsed,
        AuthErrorCode.BadJwt,
        AuthErrorCode.NoAuthorization,
        AuthErrorCode.ReauthenticationNeeded -> "Your session has expired. Please log in again."

        AuthErrorCode.CaptchaFailed -> "Verification failed. Please try again."

        else -> null
    }

    /**
     * Friendly message for an HTTP status code, or `null` when the caller should use its own
     * default (e.g. a successful-but-empty 2xx never reaches here).
     */
    fun messageForStatus(status: Int): String? = when (status) {
        400, 422 -> "Some details look invalid. Please check them and try again."
        401, 403 -> "You're not allowed to do that. Please log in and try again."
        404 -> "We couldn't find what you were looking for."
        409 -> "That conflicts with something that already exists."
        429 -> "Too many requests. Please wait a moment and try again."
        in 500..599 -> "Our servers are having trouble right now. Please try again shortly."
        else -> null
    }

    /**
     * True when [throwable] (or any cause in its chain) is a network/connectivity failure rather
     * than a server response. Supabase surfaces these as [HttpRequestException]; the platform adds
     * the usual [IOException] subtypes (unknown host, connection refused, timeouts).
     */
    fun isNetworkError(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is HttpRequestException || current is IOException) return true
            current = current.cause
        }
        return false
    }
}
