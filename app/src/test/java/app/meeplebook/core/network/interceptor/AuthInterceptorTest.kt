package app.meeplebook.core.network.interceptor

import app.meeplebook.core.auth.CurrentCredentialsStore
import app.meeplebook.core.auth.local.FakeAuthLocalDataSource
import app.meeplebook.core.model.AuthCredentials
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInterceptorTest {

    private lateinit var chain: Interceptor.Chain
    private lateinit var fakeAuthLocalDataSource: FakeAuthLocalDataSource
    private lateinit var testScope: TestScope
    private lateinit var credentialsStore: CurrentCredentialsStore

    @Before
    fun setUp() {
        chain = mockk(relaxed = true)
        fakeAuthLocalDataSource = FakeAuthLocalDataSource()
        testScope = TestScope(UnconfinedTestDispatcher())
        credentialsStore = CurrentCredentialsStore(fakeAuthLocalDataSource, testScope)
    }

    @Test
    fun `intercept adds cookie header when user is authenticated`() = runTest {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        fakeAuthLocalDataSource.setCredentials(credentials)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
    fun `intercept does not add cookie header when user is null`() = runTest {
        // Given
        fakeAuthLocalDataSource.setCredentials(null)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
    fun `intercept properly encodes username with special characters`() = runTest {
        // Given
        val credentials = AuthCredentials("user@test.com", "password")
        fakeAuthLocalDataSource.setCredentials(credentials)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
    fun `intercept properly encodes password with special characters`() = runTest {
        // Given
        val credentials = AuthCredentials("username", "p@ss word!")
        fakeAuthLocalDataSource.setCredentials(credentials)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
    fun `intercept encodes both username and password with special characters`() = runTest {
        // Given
        val credentials = AuthCredentials("user+name", "pass=word&test")
        fakeAuthLocalDataSource.setCredentials(credentials)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
    fun `intercept proceeds with modified request`() = runTest {
        // Given
        val credentials = AuthCredentials("testuser", "testpass")
        fakeAuthLocalDataSource.setCredentials(credentials)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
    fun `intercept proceeds with original request when user is null`() = runTest {
        // Given
        fakeAuthLocalDataSource.setCredentials(null)
        advanceUntilIdle() // Allow the Flow to be collected

        val interceptor = AuthInterceptor(credentialsStore)

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
