package app.meeplebook.core.network.interceptor

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BearerInterceptorTest {

    private lateinit var chain: Interceptor.Chain

    @Before
    fun setUp() {
        chain = mockk(relaxed = true)
    }

    @Test
    fun `intercept adds bearer token header when token is available`() {
        // Given
        val testToken = "test_bearer_token"
        val interceptor = BearerInterceptor(testToken)

        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        assertEquals("Bearer $testToken", capturedRequest.header("Authorization"))
    }

    @Test
    fun `intercept does not add bearer token header when token is empty`() {
        // Given
        val interceptor = BearerInterceptor("")

        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        assertNull(capturedRequest.header("Authorization"))
    }

    @Test
    fun `intercept proceeds with modified request`() {
        // Given
        val testToken = "test_token"
        val interceptor = BearerInterceptor(testToken)

        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        every { chain.request() } returns originalRequest

        val mockResponse = mockk<Response>()
        every { chain.proceed(any()) } returns mockResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        verify { chain.proceed(any()) }
        assertEquals(mockResponse, result)
    }
}