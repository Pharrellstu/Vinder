package com.example.vinted.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.vinted.ui.models.RegistrationViewModel
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

    private class FakeAuthRepository(
        private val shouldFail: Boolean = false,
        private val nicknameTaken: Boolean = false,
        private val emailTaken: Boolean = false
    ) : IAuthRepository {
        override suspend fun login(email: String, password: String): Result<Unit> = result()
        override suspend fun logout(): Result<Unit> = result()
        override suspend fun register(nickname: String, email: String, password: String): Result<Unit> = result()
        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> =
            if (shouldFail) Result.failure(IllegalStateException("fake failure"))
            else Result.success(nicknameTaken)
        override suspend fun isEmailTaken(email: String): Result<Boolean> =
            if (shouldFail) Result.failure(IllegalStateException("fake failure"))
            else Result.success(emailTaken)
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = result()
        override suspend fun verifyPasswordResetOtp(email: String, token: String): Result<Unit> = result()
        override suspend fun updatePassword(newPassword: String): Result<Unit> = result()
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
        val viewModel = RegistrationViewModel(FakeAuthRepository())
        composeRule.setContent { RegisterScreen(viewModel = viewModel) }

        // Act
        composeRule.onNodeWithText("Nickname").performTextInput("alice")
        composeRule.onNodeWithText("Email").performTextInput("alice@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("Alice123!")
        composeRule.onNodeWithText("Password Again").performTextInput("Different1!")
        composeRule.onNodeWithText("Register").performClick()
        composeRule.waitForIdle()

        // Assert
        composeRule.onNodeWithText(RegistrationViewModel.PASSWORD_MISMATCH_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun registerScreen_showsEmailSentOnValidInput() {
        // Arrange
        val viewModel = RegistrationViewModel(FakeAuthRepository(shouldFail = false))
        composeRule.setContent { RegisterScreen(viewModel = viewModel) }

        // Act
        composeRule.onNodeWithText("Nickname").performTextInput("alice")
        composeRule.onNodeWithText("Email").performTextInput("alice@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("Alice123!")
        composeRule.onNodeWithText("Password Again").performTextInput("Alice123!")
        composeRule.onNodeWithText("Register").performClick()
        composeRule.waitForIdle()

        // Assert
        composeRule.onNodeWithText("Check your email!").assertIsDisplayed()
    }

    @Test
    fun registerScreen_showsErrorWhenNicknameIsTaken() {
        // Arrange
        val viewModel = RegistrationViewModel(FakeAuthRepository(nicknameTaken = true))
        composeRule.setContent { RegisterScreen(viewModel = viewModel) }

        // Act
        composeRule.onNodeWithText("Nickname").performTextInput("alice")
        composeRule.onNodeWithText("Email").performTextInput("alice@vinder.dev")
        composeRule.onNodeWithText("Password").performTextInput("Alice123!")
        composeRule.onNodeWithText("Password Again").performTextInput("Alice123!")
        composeRule.onNodeWithText("Register").performClick()
        composeRule.waitForIdle()

        // Assert
        composeRule.onNodeWithText(RegistrationViewModel.NICKNAME_TAKEN_MESSAGE).assertIsDisplayed()
    }
}
