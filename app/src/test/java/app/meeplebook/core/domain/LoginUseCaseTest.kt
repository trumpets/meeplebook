package app.meeplebook.core.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
        fakeAuthRepository.loginResult = Result.success(expectedCredentials)

        val result = loginUseCase(username, password)

        assertTrue(result.isSuccess)
        assertEquals(expectedCredentials, result.getOrNull())
        assertEquals(1, fakeAuthRepository.loginCallCount)
        assertEquals(username, fakeAuthRepository.lastLoginUsername)
        assertEquals(password, fakeAuthRepository.lastLoginPassword)
    }

    @Test
    fun `invoke with blank username returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("", "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with blank password returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("username", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with whitespace-only username returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("   ", "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with whitespace-only password returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("username", "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke with both blank credentials returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns UnknownHostException returns NetworkError`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = Result.failure(UnknownHostException())

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.NetworkError)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns SocketTimeoutException returns NetworkError`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = Result.failure(SocketTimeoutException())

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.NetworkError)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns ConnectException returns NetworkError`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = Result.failure(ConnectException())

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.NetworkError)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns IOException returns NetworkError`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = Result.failure(IOException("Network failure"))

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.NetworkError)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns IllegalStateException returns InvalidCredentials`() = runTest {
        val username = "testUser"
        val password = "testPass"
        fakeAuthRepository.loginResult = Result.failure(IllegalStateException("Bad credentials"))

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AuthError.InvalidCredentials)
        assertEquals("Bad credentials", (error as AuthError.InvalidCredentials).message)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invoke when repository returns other exception returns Unknown error`() = runTest {
        val username = "testUser"
        val password = "testPass"
        val originalException = RuntimeException("Unexpected error")
        fakeAuthRepository.loginResult = Result.failure(originalException)

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
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
