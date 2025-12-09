package app.meeplebook.core.network.interceptor

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInterceptorTest {

    private lateinit var chain: Interceptor.Chain
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var lazyRepository: Lazy<FakeAuthRepository>
    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setUp() {
        chain = mockk(relaxed = true)
        fakeAuthRepository = FakeAuthRepository()
        lazyRepository = Lazy { fakeAuthRepository }
    }

    @After
    fun tearDown() {
        if (::interceptor.isInitialized) {
            interceptor.cleanup()
        }
    }

    @Test
    fun `intercept adds cookie header when user is logged in`() = runTest {
        // Given
        val testUser = AuthCredentials("testuser", "testpass")
        fakeAuthRepository.loginResult = AppResult.Success(testUser)
        fakeAuthRepository.login(testUser.username, testUser.password)
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        val cookieHeader = capturedRequest.header("Cookie")
        assertEquals("bggusername=testuser; bggpassword=testpass", cookieHeader)
    }

    @Test
    fun `intercept does not add cookie header when user is not logged in`() = runTest {
        // Given - no login, so current user is null
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection")
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
        val testUser = AuthCredentials("user+name@test", "password123")
        fakeAuthRepository.loginResult = AppResult.Success(testUser)
        fakeAuthRepository.login(testUser.username, testUser.password)
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        val cookieHeader = capturedRequest.header("Cookie")
        // URI.encode should encode special characters
        assertEquals("bggusername=user%2Bname%40test; bggpassword=password123", cookieHeader)
    }

    @Test
    fun `intercept properly encodes password with special characters`() = runTest {
        // Given
        val testUser = AuthCredentials("username", "pass word!@#")
        fakeAuthRepository.loginResult = AppResult.Success(testUser)
        fakeAuthRepository.login(testUser.username, testUser.password)
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        val cookieHeader = capturedRequest.header("Cookie")
        // URI.encode should encode special characters
        assertEquals("bggusername=username; bggpassword=pass%20word%21%40%23", cookieHeader)
    }

    @Test
    fun `intercept proceeds with modified request and returns response`() = runTest {
        // Given
        val testUser = AuthCredentials("testuser", "testpass")
        fakeAuthRepository.loginResult = AppResult.Success(testUser)
        fakeAuthRepository.login(testUser.username, testUser.password)
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection")
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
    fun `intercept preserves original request URL and method`() = runTest {
        // Given
        val testUser = AuthCredentials("testuser", "testpass")
        fakeAuthRepository.loginResult = AppResult.Success(testUser)
        fakeAuthRepository.login(testUser.username, testUser.password)
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection?username=test")
            .post(mockk(relaxed = true))
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        assertEquals(originalRequest.url, capturedRequest.url)
        assertEquals(originalRequest.method, capturedRequest.method)
    }

    @Test
    fun `intercept does not modify request when credentials are null`() = runTest {
        // Given - no login, so current user is null
        
        interceptor = AuthInterceptor(lazyRepository)
        advanceUntilIdle() // Allow the Flow to be collected

        val originalRequest = Request.Builder()
            .url("https://api.boardgamegeek.com/collection")
            .build()
        every { chain.request() } returns originalRequest

        val requestSlot = slot<Request>()
        val mockResponse = mockk<Response>()
        every { chain.proceed(capture(requestSlot)) } returns mockResponse

        // When
        interceptor.intercept(chain)

        // Then
        val capturedRequest = requestSlot.captured
        assertEquals(originalRequest.url, capturedRequest.url)
        assertEquals(originalRequest.headers, capturedRequest.headers)
    }
}
