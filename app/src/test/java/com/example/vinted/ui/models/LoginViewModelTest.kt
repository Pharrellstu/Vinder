package com.example.vinted.ui.models

import com.example.vinted.auth.IAuthRepository
import com.example.vinted.data.dto.AccountEntity
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LoginViewModel].
 *
 * The point of these tests is to show that the app's logic is testable in isolation. The ViewModel
 * depends on the [IAuthRepository] interface, so [FakeAuthRepository] stands in for the real
 * AuthRepository, and therefore for the Supabase client, with no network access at all. That is the
 * benefit of injecting the client/repository instead of reaching for the global singleton directly.
 *
 * Structure follows Arrange / Act / Assert.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // viewModelScope runs on Dispatchers.Main, so we swap in a controllable test dispatcher.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_withBlankCredentials_showsErrorAndNeverCallsRepository() = runTest(testDispatcher) {
        // Arrange
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        // Act
        viewModel.login(email = "", password = "")
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState.Error(LoginViewModel.EMPTY_CREDENTIALS_MESSAGE),
            viewModel.uiState.value,
        )
        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun login_whenRepositorySucceeds_emitsSuccess() = runTest(testDispatcher) {
        // Arrange
        val repository = FakeAuthRepository(loginResult = Result.success(Unit))
        val viewModel = LoginViewModel(repository)

        // Act
        viewModel.login(email = "alice@vinder.dev", password = "Alice123!")
        advanceUntilIdle()

        // Assert
        assertEquals(LoginUiState.Success, viewModel.uiState.value)
        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun login_whenRepositoryFails_emitsErrorWithFallbackMessage() = runTest(testDispatcher) {
        // Arrange
        val repository = FakeAuthRepository(loginResult = Result.failure(IllegalStateException("boom")))
        val viewModel = LoginViewModel(repository)

        // Act
        viewModel.login(email = "alice@vinder.dev", password = "wrong-password")
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(LoginViewModel.LOGIN_FAILED_MESSAGE, (state as LoginUiState.Error).message)
    }

    /**
     * In-memory [IAuthRepository] with no Supabase dependency. Only [login] carries test behaviour and
     * records how often it was called; the remaining members return harmless defaults so the fake
     * satisfies the interface.
     */
    private class FakeAuthRepository(
        private val loginResult: Result<Unit> = Result.success(Unit),
    ) : IAuthRepository {

        var loginCallCount = 0
            private set

        override suspend fun login(email: String, password: String): Result<Unit> {
            loginCallCount++
            return loginResult
        }

        override suspend fun logout(): Result<Unit> = Result.success(Unit)
        override suspend fun register(nickname: String, email: String, password: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = Result.success(false)
        override suspend fun isEmailTaken(email: String): Result<Boolean> = Result.success(false)
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyPasswordResetOtp(email: String, token: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun updatePassword(newPassword: String): Result<Unit> = Result.success(Unit)
        override fun currentSession(): UserSession? = null
        override suspend fun awaitResolvedSessionStatus(timeoutMillis: Long): SessionStatus? = null
        override suspend fun findAccountByEmail(email: String): AccountEntity? = null
    }
}
