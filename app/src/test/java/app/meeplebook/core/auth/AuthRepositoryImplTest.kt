package app.meeplebook.core.auth

import app.meeplebook.core.auth.local.FakeAuthLocalDataSource
import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.auth.remote.AuthenticationException
import app.meeplebook.core.auth.remote.FakeBggAuthRemoteDataSource
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class AuthRepositoryImplTest {

    private lateinit var fakeLocalDataSource: FakeAuthLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeBggAuthRemoteDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        fakeLocalDataSource = FakeAuthLocalDataSource()
        fakeRemoteDataSource = FakeBggAuthRemoteDataSource()
        repository = AuthRepositoryImpl(fakeLocalDataSource, fakeRemoteDataSource)
    }

    // --- observeCurrentUser tests ---

    @Test
    fun `observeCurrentUser returns flow from local data source`() = runTest {
        val credentials = AuthCredentials("user", "pass")
        fakeLocalDataSource.setCredentials(credentials)

        val result = repository.observeCurrentUser().first()

        assertEquals(credentials, result)
    }

    @Test
    fun `observeCurrentUser returns null when not logged in`() = runTest {
        val result = repository.observeCurrentUser().first()

        assertNull(result)
    }

    // --- getCurrentUser tests ---

    @Test
    fun `getCurrentUser returns credentials from local data source`() = runTest {
        val credentials = AuthCredentials("user", "pass")
        fakeLocalDataSource.setCredentials(credentials)

        val result = repository.getCurrentUser()

        assertEquals(credentials, result)
    }

    @Test
    fun `getCurrentUser returns null when not logged in`() = runTest {
        val result = repository.getCurrentUser()

        assertNull(result)
    }

    // --- login tests ---

    @Test
    fun `login success returns credentials and saves to local`() = runTest {
        val credentials = AuthCredentials("testUser", "testPass")
        fakeRemoteDataSource.loginResult = credentials

        val result = repository.login("testUser", "testPass")

        assertTrue(result is AppResult.Success)
        assertEquals(credentials, (result as AppResult.Success).data)
        assertEquals(1, fakeLocalDataSource.saveCredentialsCallCount)
        assertEquals(credentials, fakeLocalDataSource.lastSavedCredentials)
    }

    @Test
    fun `login passes correct credentials to remote`() = runTest {
        val credentials = AuthCredentials("myUser", "myPass")
        fakeRemoteDataSource.loginResult = credentials

        repository.login("myUser", "myPass")

        assertEquals("myUser", fakeRemoteDataSource.lastLoginUsername)
        assertEquals("myPass", fakeRemoteDataSource.lastLoginPassword)
        assertEquals(1, fakeRemoteDataSource.loginCallCount)
    }

    @Test
    fun `login with IllegalArgumentException returns EmptyCredentials error`() = runTest {
        fakeRemoteDataSource.loginException = IllegalArgumentException("Empty credentials")

        val result = repository.login("", "")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
    }

    @Test
    fun `login with IOException returns NetworkError`() = runTest {
        fakeRemoteDataSource.loginException = IOException("Network error")

        val result = repository.login("user", "pass")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.NetworkError)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
    }

    @Test
    fun `login with UnknownHostException returns NetworkError`() = runTest {
        fakeRemoteDataSource.loginException = UnknownHostException("Unknown host")

        val result = repository.login("user", "pass")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.NetworkError)
    }

    @Test
    fun `login with IllegalStateException returns NetworkError`() = runTest {
        fakeRemoteDataSource.loginException = IllegalStateException("Bad state")

        val result = repository.login("user", "pass")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.NetworkError)
    }

    @Test
    fun `login with AuthenticationException returns InvalidCredentials error`() = runTest {
        fakeRemoteDataSource.loginException = AuthenticationException("Invalid credentials")

        val result = repository.login("user", "wrongPass")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.InvalidCredentials)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
    }

    @Test
    fun `login with unknown exception returns Unknown error`() = runTest {
        val unexpectedException = RuntimeException("Unexpected error")
        fakeRemoteDataSource.loginException = unexpectedException

        val result = repository.login("user", "pass")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AuthError.Unknown)
        assertEquals(unexpectedException, (error as AuthError.Unknown).throwable)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
    }

    // --- logout tests ---

    @Test
    fun `logout clears local data source`() = runTest {
        val credentials = AuthCredentials("user", "pass")
        fakeLocalDataSource.setCredentials(credentials)

        repository.logout()

        assertEquals(1, fakeLocalDataSource.clearCallCount)
    }

    // --- isLoggedIn tests ---

    @Test
    fun `isLoggedIn returns true when credentials exist`() = runTest {
        val credentials = AuthCredentials("user", "pass")
        fakeLocalDataSource.setCredentials(credentials)

        val result = repository.isLoggedIn().first()

        assertTrue(result)
    }

    @Test
    fun `isLoggedIn returns false when no credentials`() = runTest {
        val result = repository.isLoggedIn().first()

        assertFalse(result)
    }

    @Test
    fun `isLoggedIn updates when credentials change`() = runTest {
        assertFalse(repository.isLoggedIn().first())

        fakeLocalDataSource.setCredentials(AuthCredentials("user", "pass"))
        assertTrue(repository.isLoggedIn().first())

        fakeLocalDataSource.setCredentials(null)
        assertFalse(repository.isLoggedIn().first())
    }
}
