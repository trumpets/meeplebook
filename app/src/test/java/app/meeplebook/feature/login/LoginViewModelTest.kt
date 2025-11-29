package app.meeplebook.feature.login

import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.domain.LoginUseCase
import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(fakeAuthRepository)
        viewModel = LoginViewModel(loginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.errorMessageResId)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun `onUsernameChange updates username in state`() {
        viewModel.onUsernameChange("testUser")
        assertEquals("testUser", viewModel.uiState.value.username)
    }

    @Test
    fun `onPasswordChange updates password in state`() {
        viewModel.onPasswordChange("testPass")
        assertEquals("testPass", viewModel.uiState.value.password)
    }

    @Test
    fun `login success sets isLoggedIn to true`() = runTest {
        val credentials = AuthCredentials("user", "pass")
        fakeAuthRepository.loginResult = Result.success(credentials)

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessageResId)
    }

    @Test
    fun `login with empty credentials maps to empty credentials error`() = runTest {
        // Empty credentials are validated by LoginUseCase before reaching repository
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals(R.string.msg_empty_credentials_error, state.errorMessageResId)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `login with network error maps to login failed error`() = runTest {
        fakeAuthRepository.loginResult = Result.failure(UnknownHostException())

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals(R.string.msg_login_failed_error, state.errorMessageResId)
    }

    @Test
    fun `login with invalid credentials maps to invalid credentials error`() = runTest {
        fakeAuthRepository.loginResult = Result.failure(IllegalStateException("Wrong password"))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals(R.string.msg_invalid_credentials_error, state.errorMessageResId)
    }

    @Test
    fun `login with unknown error maps to login failed error`() = runTest {
        fakeAuthRepository.loginResult = Result.failure(RuntimeException("Unexpected"))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals(R.string.msg_login_failed_error, state.errorMessageResId)
    }

    @Test
    fun `login sets isLoading to true during operation`() = runTest {
        fakeAuthRepository.loginResult = Result.success(AuthCredentials("user", "pass"))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()

        // Before advancing, isLoading should be true
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // After completion, isLoading should be false
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login clears previous error before attempting login`() = runTest {
        // First login fails with empty credentials
        viewModel.login()
        advanceUntilIdle()
        assertEquals(R.string.msg_empty_credentials_error, viewModel.uiState.value.errorMessageResId)

        // Second login attempt should clear the error
        fakeAuthRepository.loginResult = Result.success(AuthCredentials("user", "pass"))
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()

        // Error should be cleared when login starts
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessageResId)
    }

    @Test
    fun `login passes correct credentials to repository`() = runTest {
        fakeAuthRepository.loginResult = Result.success(AuthCredentials("myUser", "myPass"))

        viewModel.onUsernameChange("myUser")
        viewModel.onPasswordChange("myPass")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("myUser", fakeAuthRepository.lastLoginUsername)
        assertEquals("myPass", fakeAuthRepository.lastLoginPassword)
    }
}
