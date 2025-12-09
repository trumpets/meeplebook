package app.meeplebook.core.network.interceptor.integration

import app.meeplebook.core.network.interceptor.BearerInterceptor
import app.meeplebook.core.network.interceptor.UserAgentInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [UserAgentInterceptor] using MockWebServer.
 *
 * These tests verify the interceptor works correctly in a real OkHttp request chain,
 * providing more confidence than unit tests with mocked chains.
 */
class UserAgentInterceptorIntegrationTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `interceptor adds user agent header to request in real chain`() {
        // Given - no context available, so we get just "MeepleBook"
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
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
        assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
    }

    @Test
    fun `interceptor preserves existing headers`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
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
        assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
        assertEquals("custom-value", recordedRequest.getHeader("X-Custom-Header"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `interceptor returns response correctly`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": "test"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        val response = client.newCall(request).execute()

        // Then
        assertEquals(200, response.code)
        assertEquals("""{"data": "test"}""", response.body?.string())
    }

    @Test
    fun `multiple requests all receive user agent header`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
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

        // Then - verify all requests had the user agent
        repeat(3) {
            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
        }
    }

    @Test
    fun `interceptor works with both bearer and user agent interceptors together`() {
        // Given - both interceptors in the chain
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
            .addInterceptor(BearerInterceptor("test-token"))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        client.newCall(request).execute()

        // Then - both headers should be present
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
        assertEquals("Bearer test-token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor handles error responses correctly`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()
        val response = client.newCall(request).execute()

        // Then - request still had the header
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
        assertEquals(500, response.code)
    }

    @Test
    fun `interceptor handles POST requests correctly`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(null))
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
        assertEquals("MeepleBook", recordedRequest.getHeader("User-Agent"))
    }

    @Test
    fun `userAgent property is consistent with header value`() {
        // Given
        val interceptor = UserAgentInterceptor(null)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
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
        assertEquals(interceptor.userAgent, recordedRequest.getHeader("User-Agent"))
    }
}
