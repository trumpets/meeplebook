package app.meeplebook.core.auth.remote.integration

import app.meeplebook.core.auth.remote.AuthenticationException
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSourceImpl
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [BggAuthRemoteDataSourceImpl] using MockWebServer.
 *
 * These tests verify the actual HTTP communication and cookie parsing behavior
 * that cannot be properly tested with unit tests using mocks.
 */
class BggAuthRemoteDataSourceIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var dataSource: BggAuthRemoteDataSourceTestable

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        dataSource = BggAuthRemoteDataSourceTestable(okHttpClient, mockWebServer.url("/").toString())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Success scenarios ---

    @Test
    fun `login with valid credentials and cookie returns AuthCredentials`() = runTest {
        // Given - successful login response with bggpassword cookie
        val cookieValue = "abc123token"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=$cookieValue; Path=/; HttpOnly")
        )

        // When
        val result = dataSource.login("testuser", "testpass")

        // Then
        assertEquals("testuser", result.username)
        assertEquals("testpass", result.password)

        // Verify the request was made correctly
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/login/api/v1", request.path)
        assertTrue(request.body.readUtf8().contains("testuser"))
        assertTrue(request.body.readUtf8().isEmpty().not()) // Body was consumed above
    }

    @Test
    fun `login extracts cookie value correctly when multiple cookies present`() = runTest {
        // Given - response with multiple cookies
        val tokenValue = "mySecureToken123"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "sessionid=xyz789; Path=/")
                .addHeader("Set-Cookie", "bggpassword=$tokenValue; Path=/; HttpOnly; Secure")
                .addHeader("Set-Cookie", "tracking=abc; Path=/")
        )

        // When
        val result = dataSource.login("user", "pass")

        // Then
        assertEquals("user", result.username)
        assertEquals("pass", result.password)
    }

    @Test
    fun `login handles cookie with extra attributes correctly`() = runTest {
        // Given - cookie with many attributes
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token123; Path=/; Domain=boardgamegeek.com; HttpOnly; Secure; SameSite=Lax; Max-Age=31536000")
        )

        // When
        val result = dataSource.login("user", "pass")

        // Then
        assertEquals("user", result.username)
    }

    // --- Failure scenarios ---

    @Test
    fun `login with empty username throws IllegalArgumentException`() = runTest {
        try {
            dataSource.login("", "password")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }

        // No request should have been made
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `login with blank username throws IllegalArgumentException`() = runTest {
        try {
            dataSource.login("   ", "password")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }

        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `login with empty password throws IllegalArgumentException`() = runTest {
        try {
            dataSource.login("username", "")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }

        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `login with non-204 response throws AuthenticationException`() = runTest {
        // Given - unauthorized response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Invalid credentials"}""")
        )

        // When / Then
        try {
            dataSource.login("user", "wrongpass")
            fail("Expected AuthenticationException")
        } catch (e: AuthenticationException) {
            assertTrue(e.message?.contains("401") == true)
        }
    }

    @Test
    fun `login with 200 response throws AuthenticationException`() = runTest {
        // Given - 200 instead of expected 204
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
                .setBody("""{"status": "ok"}""")
        )

        // When / Then
        try {
            dataSource.login("user", "pass")
            fail("Expected AuthenticationException")
        } catch (e: AuthenticationException) {
            assertTrue(e.message?.contains("200") == true)
        }
    }

    @Test
    fun `login with 204 but no bggpassword cookie throws AuthenticationException`() = runTest {
        // Given - success response but missing the expected cookie
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "sessionid=xyz; Path=/")
        )

        // When / Then
        try {
            dataSource.login("user", "pass")
            fail("Expected AuthenticationException")
        } catch (e: AuthenticationException) {
            assertTrue(e.message?.contains("cookie") == true)
        }
    }

    @Test
    fun `login with deleted bggpassword cookie throws AuthenticationException`() = runTest {
        // Given - cookie is marked as deleted
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=deleted; Path=/; Max-Age=0")
        )

        // When / Then
        try {
            dataSource.login("user", "pass")
            fail("Expected AuthenticationException")
        } catch (e: AuthenticationException) {
            assertTrue(e.message?.contains("cookie") == true)
        }
    }

    @Test
    fun `login with 500 server error throws AuthenticationException`() = runTest {
        // Given - server error
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        // When / Then
        try {
            dataSource.login("user", "pass")
            fail("Expected AuthenticationException")
        } catch (e: AuthenticationException) {
            assertTrue(e.message?.contains("500") == true)
        }
    }

    // --- Request verification tests ---

    @Test
    fun `login sends correct JSON payload`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token123; Path=/")
        )

        // When
        dataSource.login("myUsername", "myPassword")

        // Then - verify request body
        val request = mockWebServer.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"username\":\"myUsername\""))
        assertTrue(body.contains("\"password\":\"myPassword\""))
        assertTrue(body.contains("\"credentials\""))
    }

    @Test
    fun `login sends correct content type header`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "bggpassword=token; Path=/")
        )

        // When
        dataSource.login("user", "pass")

        // Then
        val request = mockWebServer.takeRequest()
        assertNotNull(request.getHeader("Content-Type"))
        assertTrue(request.getHeader("Content-Type")?.contains("application/json") == true)
    }
}
