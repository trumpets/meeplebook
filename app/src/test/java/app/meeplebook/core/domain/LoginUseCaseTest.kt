package app.meeplebook.core.domain

import app.meeplebook.core.auth.AuthError
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(fakeAuthRepository)
    }

    @Test
    fun `invoke with valid credentials returns success`() = runTest {
        val username = "testUser"
        val password = "testPass"
        val expectedCredentials = AuthCredentials(username, password)
        fakeAuthRepository.loginResult = AppResult.Success(expectedCredentials)

        val result = loginUseCase(username, password)

        assertTrue(result is AppResult.Success)
        assertEquals(expectedCredentials, (result as AppResult.Success).data)
        assertEquals(1, fakeAuthRepository.loginCallCount)
        assertEquals(username, fakeAuthRepository.lastLoginUsername)
        assertEquals(password, fakeAuthRepository.lastLoginPassword)
    }

    @Test
    fun `invoke with blank username returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("", "password")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with blank password returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("username", "")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with whitespace-only username returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("   ", "password")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with whitespace-only password returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("username", "   ")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with both blank credentials returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("", "")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns NetworkError returns NetworkError`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = AppResult.Failure(AuthError.NetworkError)

        val result = loginUseCase(username, password)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.NetworkError)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns InvalidCredentials returns InvalidCredentials`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = AppResult.Failure(AuthError.InvalidCredentials)

        val result = loginUseCase(username, password)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.InvalidCredentials)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns Unknown error returns Unknown error`() = runTest {
        val username = "testUser"
        val password = "testPass"
        val originalException = RuntimeException("Unexpected error")
        fakeAuthRepository.loginResult = AppResult.Failure(AuthError.Unknown(originalException))

        val result = loginUseCase(username, password)

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AuthError.Unknown)
        assertEquals(originalException, (error as AuthError.Unknown).throwable)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke does not call repository when credentials are blank`() = runTest {
        loginUseCase("", "")
        loginUseCase("   ", "pass")
        loginUseCase("user", "   ")

        assertEquals(0, fakeAuthRepository.loginCallCount)
        assertNull(fakeAuthRepository.lastLoginUsername)
        assertNull(fakeAuthRepository.lastLoginPassword)
    }
}
