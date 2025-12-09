package app.meeplebook.core.network.interceptor

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import dagger.Lazy
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

class AuthInterceptorTest {

    private lateinit var chain: Interceptor.Chain
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var lazyRepository: Lazy<AuthRepository>

    @Before
    fun setUp() {
        chain = mockk(relaxed = true)
        fakeAuthRepository = FakeAuthRepository()
        lazyRepository = Lazy { fakeAuthRepository }
    }

    @Test
    fun `intercept adds cookie header when user is authenticated`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        fakeAuthRepository.setCurrentUser(credentials)

        val interceptor = AuthInterceptor(lazyRepository)

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
        assertEquals("bggusername=testuser; bggpassword=testpass", capturedRequest.header("Cookie"))
    }

    @Test
    fun `intercept does not add cookie header when user is null`() {
        // Given
        fakeAuthRepository.setCurrentUser(null)

        val interceptor = AuthInterceptor(lazyRepository)

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
        assertNull(capturedRequest.header("Cookie"))
    }

    @Test
    fun `intercept properly encodes username with special characters`() {
        // Given
        val credentials = AuthCredentials("user@test.com", "password")
        fakeAuthRepository.setCurrentUser(credentials)

        val interceptor = AuthInterceptor(lazyRepository)

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
        // @ should be encoded to %40
        assertEquals("bggusername=user%40test.com; bggpassword=password", capturedRequest.header("Cookie"))
    }

    @Test
    fun `intercept properly encodes password with special characters`() {
        // Given
        val credentials = AuthCredentials("username", "p@ss word!")
        fakeAuthRepository.setCurrentUser(credentials)

        val interceptor = AuthInterceptor(lazyRepository)

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
        // @ should be encoded to %40, space to %20, ! to %21
        assertEquals("bggusername=username; bggpassword=p%40ss%20word%21", capturedRequest.header("Cookie"))
    }

    @Test
    fun `intercept encodes both username and password with special characters`() {
        // Given
        val credentials = AuthCredentials("user+name", "pass=word&test")
        fakeAuthRepository.setCurrentUser(credentials)

        val interceptor = AuthInterceptor(lazyRepository)

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
        // + should be encoded to %2B, = to %3D, & to %26
        assertEquals("bggusername=user%2Bname; bggpassword=pass%3Dword%26test", capturedRequest.header("Cookie"))
    }

    @Test
    fun `intercept proceeds with modified request`() {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        fakeAuthRepository.setCurrentUser(credentials)

        val interceptor = AuthInterceptor(lazyRepository)

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

    @Test
    fun `intercept proceeds with original request when user is null`() {
        // Given
        fakeAuthRepository.setCurrentUser(null)

        val interceptor = AuthInterceptor(lazyRepository)

        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        verify { chain.proceed(any()) }
        assertEquals(mockResponse, result)
        assertEquals(originalRequest, requestSlot.captured)
    }
}
