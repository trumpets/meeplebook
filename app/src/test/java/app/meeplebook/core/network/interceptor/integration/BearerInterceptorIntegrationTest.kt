package app.meeplebook.core.network.interceptor.integration

import app.meeplebook.core.network.interceptor.BearerInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [BearerInterceptor] using MockWebServer.
 *
 * These tests verify the interceptor works correctly in a real OkHttp request chain,
 * which provides more confidence than unit tests with mocked chains.
 */
class BearerInterceptorIntegrationTest {

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
    fun `interceptor adds bearer token to request in real chain`() {
        // Given
        val token = "my-secret-token"
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(token))
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
        assertEquals("Bearer $token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor does not add header when token is null`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(null))
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
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor does not add header when token is empty`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(""))
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
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor does not add header when token is blank`() {
        // Given
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor("   "))
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
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor preserves existing headers`() {
        // Given
        val token = "test-token"
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(token))
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
        assertEquals("Bearer $token", recordedRequest.getHeader("Authorization"))
        assertEquals("custom-value", recordedRequest.getHeader("X-Custom-Header"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `interceptor returns response correctly`() {
        // Given
        val token = "test-token"
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(token))
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
    fun `multiple requests all receive bearer token`() {
        // Given
        val token = "persistent-token"
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(token))
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

        // Then - verify all requests had the token
        repeat(3) {
            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("Bearer $token", recordedRequest.getHeader("Authorization"))
        }
    }
}
