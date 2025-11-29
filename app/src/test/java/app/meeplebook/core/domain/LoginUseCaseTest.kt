package app.meeplebook.core.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class LoginUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun setUp() {
        authRepository = mockk()
        loginUseCase = LoginUseCase(authRepository)
    }

    @Test
    fun `invoke with valid credentials returns success`() = runTest {
        val username = "testUser"
        val password = "testPass"
        val expectedCredentials = AuthCredentials(username, password)

        coEvery { authRepository.login(username, password) } returns Result.success(expectedCredentials)

        val result = loginUseCase(username, password)

        assertTrue(result.isSuccess)
        assertEquals(expectedCredentials, result.getOrNull())
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `invoke with blank username returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("", "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with blank password returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("username", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with whitespace-only username returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("   ", "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with whitespace-only password returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("username", "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with both blank credentials returns EmptyCredentials error`() = runTest {
        val result = loginUseCase("", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.EmptyCredentials)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke when repository returns UnknownHostException returns NetworkError`() = runTest {
        val username = "testUser"
        val password = "testPass"

        coEvery { authRepository.login(username, password) } returns Result.failure(UnknownHostException())

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.NetworkError)
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `invoke when repository returns IllegalStateException returns InvalidCredentials`() = runTest {
        val username = "testUser"
        val password = "testPass"

        coEvery { authRepository.login(username, password) } returns Result.failure(IllegalStateException("Bad credentials"))

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AuthError.InvalidCredentials)
        assertEquals("Bad credentials", (error as AuthError.InvalidCredentials).message)
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `invoke when repository returns other exception returns Unknown error`() = runTest {
        val username = "testUser"
        val password = "testPass"
        val originalException = RuntimeException("Unexpected error")

        coEvery { authRepository.login(username, password) } returns Result.failure(originalException)

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AuthError.Unknown)
        assertEquals(originalException, (error as AuthError.Unknown).throwable)
        coVerify { authRepository.login(username, password) }
    }
}
