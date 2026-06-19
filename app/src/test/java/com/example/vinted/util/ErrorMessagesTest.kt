package com.example.vinted.util

import io.github.jan.supabase.auth.exception.AuthErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/**
 * Unit tests for [ErrorMessages]. Cover the pure mapping helpers and the network-detection logic;
 * the full [ErrorMessages.friendlyMessage] dispatch over real Supabase exceptions needs a ktor
 * HttpResponse to construct and is exercised by the instrumented auth tests instead.
 */
class ErrorMessagesTest {

    @Test
    fun messageForAuthCode_mapsInvalidCredentialsToFriendlyText() {
        // Arrange
        val code = AuthErrorCode.InvalidCredentials

        // Act
        val message = ErrorMessages.messageForAuthCode(code)

        // Assert
        assertEquals("Incorrect email or password.", message)
    }

    @Test
    fun messageForAuthCode_mapsExistingEmailVariantsToSameText() {
        // Arrange
        val codes = listOf(AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists)

        // Act
        val messages = codes.map { ErrorMessages.messageForAuthCode(it) }

        // Assert
        assertTrue(messages.all { it == "This email is already registered." })
    }

    @Test
    fun messageForAuthCode_mapsWeakPasswordToSharedConstant() {
        // Arrange
        val code = AuthErrorCode.WeakPassword

        // Act
        val message = ErrorMessages.messageForAuthCode(code)

        // Assert
        assertEquals(ErrorMessages.WEAK_PASSWORD, message)
    }

    @Test
    fun messageForAuthCode_returnsNullForUnknownCode() {
        // Arrange
        val code: AuthErrorCode? = null

        // Act
        val message = ErrorMessages.messageForAuthCode(code)

        // Assert
        assertNull(message)
    }

    @Test
    fun messageForStatus_mapsRateLimitAndServerErrors() {
        // Arrange
        val tooManyRequests = 429
        val serverError = 503

        // Act
        val rateLimitMessage = ErrorMessages.messageForStatus(tooManyRequests)
        val serverMessage = ErrorMessages.messageForStatus(serverError)

        // Assert
        assertEquals("Too many requests. Please wait a moment and try again.", rateLimitMessage)
        assertEquals("Our servers are having trouble right now. Please try again shortly.", serverMessage)
    }

    @Test
    fun messageForStatus_returnsNullForSuccessCode() {
        // Arrange
        val ok = 200

        // Act
        val message = ErrorMessages.messageForStatus(ok)

        // Assert
        assertNull(message)
    }

    @Test
    fun isNetworkError_trueForUnknownHost() {
        // Arrange
        val throwable = UnknownHostException("10.0.2.2")

        // Act
        val result = ErrorMessages.isNetworkError(throwable)

        // Assert
        assertTrue(result)
    }

    @Test
    fun isNetworkError_trueWhenIoExceptionIsNestedInCauseChain() {
        // Arrange
        val throwable = RuntimeException("wrapper", IllegalStateException("inner", IOException("socket closed")))

        // Act
        val result = ErrorMessages.isNetworkError(throwable)

        // Assert
        assertTrue(result)
    }

    @Test
    fun isNetworkError_falseForPlainException() {
        // Arrange
        val throwable = IllegalStateException("not a network problem")

        // Act
        val result = ErrorMessages.isNetworkError(throwable)

        // Assert
        assertFalse(result)
    }

    @Test
    fun friendlyMessage_usesFallbackForUnrecognisedThrowable() {
        // Arrange
        val throwable = IllegalStateException("raw technical detail")
        val fallback = "Couldn't load this screen."

        // Act
        val message = ErrorMessages.friendlyMessage(throwable, fallback)

        // Assert
        assertEquals(fallback, message)
    }

    @Test
    fun friendlyMessage_returnsNetworkMessageForConnectivityFailure() {
        // Arrange
        val throwable = UnknownHostException("no route to host")

        // Act
        val message = ErrorMessages.friendlyMessage(throwable, fallback = "ignored fallback")

        // Assert
        assertEquals(ErrorMessages.NETWORK, message)
    }
}
