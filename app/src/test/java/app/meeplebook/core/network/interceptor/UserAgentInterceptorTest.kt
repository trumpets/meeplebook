package app.meeplebook.core.network.interceptor

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserAgentInterceptorTest {

    private lateinit var chain: Interceptor.Chain

    @Before
    fun setUp() {
        chain = mockk(relaxed = true)
    }

    @Test
    fun `intercept adds user agent header with version when context is available`() {
        // Given
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        val mockPackageInfo = mockk<PackageInfo>()

        every { mockContext.packageName } returns "app.meeplebook"
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getPackageInfo("app.meeplebook", 0) } returns mockPackageInfo
        every { mockPackageInfo.versionName } returns "1.0.0"

        val interceptor = UserAgentInterceptor(mockContext)

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
        assertEquals("MeepleBook/1.0.0", capturedRequest.header("User-Agent"))
    }

    @Test
    fun `intercept adds user agent header without version when context is null`() {
        // Given
        val interceptor = UserAgentInterceptor(null)

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
        assertEquals("MeepleBook", capturedRequest.header("User-Agent"))
    }

    @Test
    fun `intercept adds fallback version when PackageManager throws`() {
        // Given
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()

        every { mockContext.packageName } returns "app.meeplebook"
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getPackageInfo("app.meeplebook", 0) } throws PackageManager.NameNotFoundException()

        val interceptor = UserAgentInterceptor(mockContext)

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
        assertEquals("MeepleBook/?.?", capturedRequest.header("User-Agent"))
    }

    @Test
    fun `intercept adds fallback version when versionName is null`() {
        // Given
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        val mockPackageInfo = mockk<PackageInfo>()

        every { mockContext.packageName } returns "app.meeplebook"
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getPackageInfo("app.meeplebook", 0) } returns mockPackageInfo
        every { mockPackageInfo.versionName } returns null

        val interceptor = UserAgentInterceptor(mockContext)

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
        assertEquals("MeepleBook/?.?", capturedRequest.header("User-Agent"))
    }

    @Test
    fun `intercept proceeds with modified request`() {
        // Given
        val interceptor = UserAgentInterceptor(null)

        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        every { chain.request() } returns originalRequest

        val mockResponse = mockk<Response>()
        every { chain.proceed(any()) } returns mockResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(mockResponse, result)
    }

    @Test
    fun `userAgent property returns correct value when context is null`() {
        // Given
        val interceptor = UserAgentInterceptor(null)

        // Then
        assertEquals("MeepleBook", interceptor.userAgent)
    }

    @Test
    fun `userAgent property returns correct value when context has version`() {
        // Given
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        val mockPackageInfo = mockk<PackageInfo>()

        every { mockContext.packageName } returns "app.meeplebook"
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getPackageInfo("app.meeplebook", 0) } returns mockPackageInfo
        every { mockPackageInfo.versionName } returns "2.5.0"

        val interceptor = UserAgentInterceptor(mockContext)

        // Then
        assertEquals("MeepleBook/2.5.0", interceptor.userAgent)
    }
}
