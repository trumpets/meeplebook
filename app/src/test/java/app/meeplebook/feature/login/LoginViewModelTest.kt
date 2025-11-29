package app.meeplebook.feature.login

import app.meeplebook.R
import app.meeplebook.core.domain.LoginUseCase
import app.meeplebook.core.model.AuthCredentials
import io.mockk.coEvery
import io.mockk.mockk
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

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
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
        coEvery { loginUseCase("user", "pass") } returns Result.success(credentials)

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
    fun `login with IllegalArgumentException maps to empty credentials error`() = runTest {
        coEvery { loginUseCase("", "") } returns Result.failure(
            IllegalArgumentException("Username and password must not be blank")
        )

        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals(R.string.msg_empty_credentials_error, state.errorMessageResId)
    }

    @Test
    fun `login with UnknownHostException maps to login failed error`() = runTest {
        coEvery { loginUseCase("user", "pass") } returns Result.failure(UnknownHostException())

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
    fun `login with IllegalStateException maps to login failed error`() = runTest {
        coEvery { loginUseCase("user", "pass") } returns Result.failure(IllegalStateException())

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
    fun `login with other exceptions maps to invalid credentials error`() = runTest {
        coEvery { loginUseCase("user", "pass") } returns Result.failure(RuntimeException("Auth error"))

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
    fun `login sets isLoading to true during operation`() = runTest {
        coEvery { loginUseCase("user", "pass") } returns Result.success(AuthCredentials("user", "pass"))

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
        // First login fails
        coEvery { loginUseCase("", "") } returns Result.failure(
            IllegalArgumentException("Username and password must not be blank")
        )
        viewModel.login()
        advanceUntilIdle()
        assertEquals(R.string.msg_empty_credentials_error, viewModel.uiState.value.errorMessageResId)

        // Second login attempt should clear the error
        coEvery { loginUseCase("user", "pass") } returns Result.success(AuthCredentials("user", "pass"))
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()

        // Error should be cleared when login starts
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessageResId)
    }
}
