package app.meeplebook.feature.login

import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.auth.domain.LoginUseCase
import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.login.effect.LoginEffectProducer
import app.meeplebook.feature.login.effect.LoginUiEffect
import app.meeplebook.feature.login.reducer.LoginReducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            reducer = LoginReducer(),
            effectProducer = LoginEffectProducer()
        )
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
        assertEquals(uiTextEmpty(), state.errorMessage)
    }

    @Test
    fun `onUsernameChange updates username in state`() {
        viewModel.onEvent(LoginEvent.UsernameChanged("testUser"))
        assertEquals("testUser", viewModel.uiState.value.username)
    }

    @Test
    fun `onPasswordChange updates password in state`() {
        viewModel.onEvent(LoginEvent.PasswordChanged("testPass"))
        assertEquals("testPass", viewModel.uiState.value.password)
    }

    @Test
    fun `login success emits success effect and clears loading`() = runTest {
        val credentials = AuthCredentials("user", "pass")
        fakeAuthRepository.loginResult = AppResult.Success(credentials)
        val effects = mutableListOf<LoginUiEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onEvent(LoginEvent.UsernameChanged("user"))
        viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(uiTextEmpty(), state.errorMessage)
        assertEquals(listOf(LoginUiEffect.LoginSucceeded), effects)

        job.cancel()
    }

    @Test
    fun `login with empty credentials maps to empty credentials error`() = runTest {
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(uiTextRes(R.string.msg_empty_credentials_error), state.errorMessage)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `login with network error maps to login failed error`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Failure(AuthError.NetworkError)

        viewModel.onEvent(LoginEvent.UsernameChanged("user"))
        viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(uiTextRes(R.string.msg_login_failed_error), state.errorMessage)
    }

    @Test
    fun `login with invalid credentials maps to invalid credentials error`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Failure(AuthError.InvalidCredentials)

        viewModel.onEvent(LoginEvent.UsernameChanged("user"))
        viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(uiTextRes(R.string.msg_invalid_credentials_error), state.errorMessage)
    }

    @Test
    fun `login with unknown error maps to login failed error`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Failure(AuthError.Unknown(RuntimeException("Unexpected")))

        viewModel.onEvent(LoginEvent.UsernameChanged("user"))
        viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(uiTextRes(R.string.msg_login_failed_error), state.errorMessage)
    }

    @Test
    fun `login clears previous error before attempting login`() = runTest {
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()
        assertEquals(uiTextRes(R.string.msg_empty_credentials_error), viewModel.uiState.value.errorMessage)

        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("user", "pass"))
        viewModel.onEvent(LoginEvent.UsernameChanged("user"))
        assertEquals(uiTextEmpty(), viewModel.uiState.value.errorMessage)
        viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        assertEquals(uiTextEmpty(), viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login passes correct credentials to repository`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("myUser", "myPass"))

        viewModel.onEvent(LoginEvent.UsernameChanged("myUser"))
        viewModel.onEvent(LoginEvent.PasswordChanged("myPass"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()

        assertEquals("myUser", fakeAuthRepository.lastLoginUsername)
        assertEquals("myPass", fakeAuthRepository.lastLoginPassword)
    }
}
