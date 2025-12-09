package app.meeplebook.core.network.interceptor.integration

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.network.interceptor.AuthInterceptor
import app.meeplebook.core.network.interceptor.BearerInterceptor
import app.meeplebook.core.network.interceptor.UserAgentInterceptor
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [AuthInterceptor] using MockWebServer.
 *
 * These tests verify the interceptor works correctly in a real OkHttp request chain,
 * which provides more confidence than unit tests with mocked chains.
 */
class AuthInterceptorIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockRepository: AuthRepository
    private lateinit var lazyRepository: Lazy<AuthRepository>

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        mockRepository = mockk()
        lazyRepository = Lazy { mockRepository }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `interceptor adds cookie header to request in real chain when user is authenticated`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpassword")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("bggusername=testuser; bggpassword=testpassword", recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor does not add cookie header when user is null`() {
        // Given
        coEvery { mockRepository.getCurrentUser() } returns null

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor properly encodes username and password with special characters`() {
        // Given
        val credentials = AuthCredentials("user@example.com", "p@ss word!")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("bggusername=user%40example.com; bggpassword=p%40ss%20word%21", recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor preserves existing headers`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .header("X-Custom-Header", "custom-value")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest.getHeader("Cookie"))
        assertEquals("custom-value", recordedRequest.getHeader("X-Custom-Header"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `interceptor returns response correctly`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"success": true}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        val response = client.newCall(request).execute()

        // Then
        assertEquals(201, response.code)
        assertEquals("""{"success": true}""", response.body?.string())
    }

    @Test
    fun `multiple requests all receive cookie header`() {
        // Given
        val credentials = AuthCredentials("persistentuser", "persistentpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200))
        }

        // When - make multiple requests
        repeat(3) { i ->
            val request = Request.Builder()
                .url(mockWebServer.url("/api/resource/$i"))
                .build()
            client.newCall(request).execute()
        }

        // Then - verify all requests had the cookie
        repeat(3) {
            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("bggusername=persistentuser; bggpassword=persistentpass", recordedRequest.getHeader("Cookie"))
        }
    }

    @Test
    fun `interceptor works with bearer and user agent interceptors together`() {
        // Given - all three interceptors in the chain
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
            .addInterceptor(BearerInterceptor("test-token"))
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        client.newCall(request).execute()

        // Then - all headers should be present
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
        assertEquals("Bearer test-token", recordedRequest.getHeader("Authorization"))
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor handles error responses correctly`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        val response = client.newCall(request).execute()

        // Then - request still had the header
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest.getHeader("Cookie"))
        assertEquals(401, response.code)
    }

    @Test
    fun `interceptor handles POST requests correctly`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .post("""{"test": "data"}""".toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor handles PUT requests correctly`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .put("""{"test": "data"}""".toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor handles DELETE requests correctly`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returns credentials

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .delete()
            .build()
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest.getHeader("Cookie"))
    }

    @Test
    fun `interceptor does not add cookie to subsequent requests when user becomes null`() {
        // Given - first request with user, second without
        val credentials = AuthCredentials("testuser", "testpass")
        coEvery { mockRepository.getCurrentUser() } returnsMany listOf(credentials, null)

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(lazyRepository))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When - make two requests
        val request1 = Request.Builder()
            .url(mockWebServer.url("/api/test1"))
            .build()
        client.newCall(request1).execute()

        val request2 = Request.Builder()
            .url(mockWebServer.url("/api/test2"))
            .build()
        client.newCall(request2).execute()

        // Then - first has cookie, second does not
        val recordedRequest1 = mockWebServer.takeRequest()
        assertEquals("bggusername=testuser; bggpassword=testpass", recordedRequest1.getHeader("Cookie"))

        val recordedRequest2 = mockWebServer.takeRequest()
        assertNull(recordedRequest2.getHeader("Cookie"))
    }
}
