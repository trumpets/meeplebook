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
    fun `invoke with blank username returns failure`() = runTest {
        val result = loginUseCase("", "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with blank password returns failure`() = runTest {
        val result = loginUseCase("username", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with whitespace-only username returns failure`() = runTest {
        val result = loginUseCase("   ", "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with whitespace-only password returns failure`() = runTest {
        val result = loginUseCase("username", "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke with both blank credentials returns failure`() = runTest {
        val result = loginUseCase("", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `invoke when repository returns failure propagates failure`() = runTest {
        val username = "testUser"
        val password = "testPass"
        val expectedException = RuntimeException("Network error")

        coEvery { authRepository.login(username, password) } returns Result.failure(expectedException)

        val result = loginUseCase(username, password)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
        coVerify { authRepository.login(username, password) }
    }
}
