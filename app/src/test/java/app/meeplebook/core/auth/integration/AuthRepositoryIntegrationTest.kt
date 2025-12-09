package app.meeplebook.core.auth.integration

import app.meeplebook.core.auth.AuthRepositoryImpl
import app.meeplebook.core.auth.local.FakeAuthLocalDataSource
import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSourceImpl
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [AuthRepositoryImpl] with real network calls via MockWebServer.
 *
 * These tests verify the complete authentication flow from repository through
 * remote data source to network, and back to local storage. This provides
 * end-to-end verification of the authentication behavior.
 */
class AuthRepositoryIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeLocalDataSource: FakeAuthLocalDataSource
    private lateinit var remoteDataSource: BggAuthRemoteDataSourceImpl
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        fakeLocalDataSource = FakeAuthLocalDataSource()
        remoteDataSource = BggAuthRemoteDataSourceImpl(
            okHttpClient,
            mockWebServer.url("/").toString()
        )
        repository = AuthRepositoryImpl(fakeLocalDataSource, remoteDataSource)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Successful login flow ---

    @Test
    fun `login success stores credentials locally`() = runTest {
        // Given - successful server response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token123; Path=/")
        )

        // When
        val result = repository.login("testuser", "testpass")

        // Then
        assertTrue(result is AppResult.Success)
        assertEquals("testuser", (result as AppResult.Success).data.username)
        assertEquals("testpass", result.data.password)

        // Verify credentials were saved locally
        assertEquals(1, fakeLocalDataSource.saveCredentialsCallCount)
        assertEquals("testuser", fakeLocalDataSource.lastSavedCredentials?.username)
    }

    @Test
    fun `login success updates isLoggedIn flow`() = runTest {
        // Given
        assertFalse(repository.isLoggedIn().first())

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
        )

        // When
        repository.login("user", "pass")

        // Then
        assertTrue(repository.isLoggedIn().first())
    }

    @Test
    fun `login success updates currentUser flow`() = runTest {
        // Given
        assertNull(repository.currentUser().first())

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
        )

        // When
        repository.login("myuser", "mypass")

        // Then
        val currentUser = repository.currentUser().first()
        assertEquals("myuser", currentUser?.username)
        assertEquals("mypass", currentUser?.password)
    }

    // --- Failed login flow ---

    @Test
    fun `login with invalid credentials returns failure and does not store credentials`() = runTest {
        // Given - unauthorized response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
        )

        // When
        val result = repository.login("user", "wrongpass")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.InvalidCredentials)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
        assertFalse(repository.isLoggedIn().first())
    }

    @Test
    fun `login with empty username returns failure immediately`() = runTest {
        // When
        val result = repository.login("", "password")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, mockWebServer.requestCount)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
    }

    @Test
    fun `login with empty password returns failure immediately`() = runTest {
        // When
        val result = repository.login("username", "")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.EmptyCredentials)
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `login with missing cookie returns failure`() = runTest {
        // Given - success response but no bggpassword cookie
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "sessionid=xyz; Path=/")
        )

        // When
        val result = repository.login("user", "pass")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AuthError.InvalidCredentials)
        assertEquals(0, fakeLocalDataSource.saveCredentialsCallCount)
    }

    @Test
    fun `login with server error returns network error`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        // When
        val result = repository.login("user", "pass")

        // Then
        assertTrue(result is AppResult.Failure)
        // 500 error results in AuthenticationException which maps to InvalidCredentials
        assertTrue((result as AppResult.Failure).error is AuthError.InvalidCredentials)
    }

    // --- Logout flow ---

    @Test
    fun `logout after login clears local credentials`() = runTest {
        // Given - user is logged in
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
        )
        repository.login("user", "pass")
        assertTrue(repository.isLoggedIn().first())

        // When
        repository.logout()

        // Then
        assertFalse(repository.isLoggedIn().first())
        assertNull(repository.currentUser().first())
        assertEquals(1, fakeLocalDataSource.clearCallCount)
    }

    // --- Multiple login attempts ---

    @Test
    fun `multiple login attempts update credentials correctly`() = runTest {
        // Given - first login
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token1; Path=/")
        )
        repository.login("user1", "pass1")

        // When - second login with different user
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token2; Path=/")
        )
        repository.login("user2", "pass2")

        // Then
        val currentUser = repository.currentUser().first()
        assertEquals("user2", currentUser?.username)
        assertEquals(2, fakeLocalDataSource.saveCredentialsCallCount)
    }

    @Test
    fun `failed login after successful login does not clear existing credentials`() = runTest {
        // Given - first successful login
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
        )
        repository.login("user", "pass")

        // When - second login fails
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
        )
        val result = repository.login("other", "wrongpass")

        // Then - original user still logged in
        assertTrue(result is AppResult.Failure)
        val currentUser = repository.currentUser().first()
        assertEquals("user", currentUser?.username)
    }

    // --- Request verification ---

    @Test
    fun `login sends correct credentials in request body`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
        )

        // When
        repository.login("myUsername", "myPassword")

        // Then
        val request = mockWebServer.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"username\":\"myUsername\""))
        assertTrue(body.contains("\"password\":\"myPassword\""))
    }
}
