package com.example.vinted.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.vinted.ui.screens.AuthenticateAccountScreen
import com.example.vinted.ui.screens.LoginScreen
import com.example.vinted.ui.models.LoginViewModel
import com.example.vinted.ui.screens.RegisterScreen
import io.github.jan.supabase.auth.user.UserSession
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeAuthRepository(private val shouldFail: Boolean = false) : IAuthRepository {
        override suspend fun login(email: String, password: String): Result<Unit> = result()
        override suspend fun register(email: String, password: String): Result<Unit> = result()
        override suspend fun verifyOtp(email: String, token: String): Result<Unit> = result()
        override suspend fun logout(): Result<Unit> = result()
        override fun currentSession(): UserSession? = null

        private fun result(): Result<Unit> =
            if (shouldFail) Result.failure(IllegalStateException("fake failure"))
            else Result.success(Unit)
    }

    // ─── Login screen ──────────────────────────────────────────────────────────

    @Test
    fun loginScreen_showsErrorOnEmptyCredentials() {
        // Arrange
        val viewModel = LoginViewModel(FakeAuthRepository())
        composeRule.setContent { LoginScreen(viewModel = viewModel) }

        // Act — click without entering anything
        composeRule.onNodeWithText("Log In").performClick()
        composeRule.waitForIdle()

        // Assert
        composeRule.onNodeWithText(LoginViewModel.EMPTY_CREDENTIALS_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun loginScreen_navigatesToHomeOnValidCredentials() {
        // Arrange
        var loginSucceeded = false
        val viewModel = LoginViewModel(FakeAuthRepository(shouldFail = false))
        composeRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { loginSucceeded = true }
            )
        }

        // Act
        composeRule.onNodeWithText("Username").performTextInput("alice@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("Alice123!")
        composeRule.onNodeWithText("Log In").performClick()
        composeRule.waitForIdle()

        // Assert
        assertTrue(loginSucceeded)
    }

    @Test
    fun loginScreen_showsErrorOnFailedLogin() {
        // Arrange
        val viewModel = LoginViewModel(FakeAuthRepository(shouldFail = true))
        composeRule.setContent { LoginScreen(viewModel = viewModel) }

        // Act
        composeRule.onNodeWithText("Username").performTextInput("wrong@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("wrongpassword")
        composeRule.onNodeWithText("Log In").performClick()
        composeRule.waitForIdle()

        // Assert — error text visible, success not triggered
        composeRule.onNodeWithText("fake failure").assertIsDisplayed()
    }

    // ─── Register screen ───────────────────────────────────────────────────────

    @Test
    fun registerScreen_showsErrorWhenPasswordsMismatch() {
        // Arrange
        var registerSucceeded = false
        val viewModel = AuthViewModel(FakeAuthRepository())
        composeRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = { registerSucceeded = true }
            )
        }

        // Act
        composeRule.onNodeWithText("Email").performTextInput("alice@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("Alice123!")
        composeRule.onNodeWithText("Password Again").performTextInput("Different1!")
        composeRule.onNodeWithText("Register").performClick()
        composeRule.waitForIdle()

        // Assert
        composeRule.onNodeWithText(AuthViewModel.PASSWORD_MISMATCH_MESSAGE).assertIsDisplayed()
        assertTrue(!registerSucceeded)
    }

    @Test
    fun registerScreen_callsOnRegisterSuccessOnValidInput() {
        // Arrange
        var registerSucceeded = false
        val viewModel = AuthViewModel(FakeAuthRepository(shouldFail = false))
        composeRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = { registerSucceeded = true }
            )
        }

        // Act
        composeRule.onNodeWithText("Email").performTextInput("alice@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("Alice123!")
        composeRule.onNodeWithText("Password Again").performTextInput("Alice123!")
        composeRule.onNodeWithText("Register").performClick()
        composeRule.waitForIdle()

        // Assert
        assertTrue(registerSucceeded)
    }

    // ─── Authenticate (OTP) screen ─────────────────────────────────────────────

    @Test
    fun authenticateScreen_callsOnVerifySuccessOnValidCode() {
        // Arrange
        var verifySucceeded = false
        val viewModel = AuthViewModel(FakeAuthRepository(shouldFail = false))
        composeRule.setContent {
            AuthenticateAccountScreen(
                viewModel = viewModel,
                email = "alice@vinder.dev",
                onVerifySuccess = { verifySucceeded = true }
            )
        }

        // Act
        composeRule.onNodeWithText("Code").performTextInput("123456")
        composeRule.onNodeWithText("Proceed").performClick()
        composeRule.waitForIdle()

        // Assert
        assertTrue(verifySucceeded)
    }
}
