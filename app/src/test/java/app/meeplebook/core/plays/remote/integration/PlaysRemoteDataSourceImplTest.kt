package app.meeplebook.core.plays.remote.integration

import app.meeplebook.core.network.BggApi
import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.remote.PlaysFetchException
import app.meeplebook.core.plays.remote.PlaysRemoteDataSourceImpl
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [PlaysRemoteDataSourceImpl] using MockWebServer.
 *
 * These tests verify the retry logic, exponential backoff, and proper resource cleanup
 * for various response codes from the BGG API.
 */
class PlaysRemoteDataSourceImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: PlaysRemoteDataSourceImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val bggApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .build()
            .create(BggApi::class.java)

        dataSource = PlaysRemoteDataSourceImpl(bggApi)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Success scenarios ---

    @Test
    fun `fetchPlays returns items on 200 response`() = runTest {
        // Given - successful response
        val playsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="2" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="Home">
                    <item name="Gloomhaven" objecttype="thing" objectid="174430"></item>
                    <comments>Great game!</comments>
                    <players>
                        <player username="player1" userid="111" name="Alice" startposition="1" color="Red" score="10" win="1" />
                    </players>
                </play>
                <play id="2" date="2024-01-16" quantity="1" length="90" incomplete="0" location="">
                    <item name="Catan" objecttype="thing" objectid="13"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playsXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Gloomhaven", result[0].gameName)
        assertEquals(1, result[0].players.size)
        assertEquals("Alice", result[0].players[0].name)
        assertEquals(2, result[1].id)
        assertEquals("Catan", result[1].gameName)
    }

    @Test
    fun `fetchPlays returns empty list for no plays`() = runTest {
        // Given - empty plays response
        val playsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="0" page="1">
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playsXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(0, result.size)
    }

    // --- 202 Retry scenarios ---

    @Test
    fun `fetchPlays retries on 202 response and succeeds`() = runTest {
        // Given - first response is 202 (queued), second is 200 (success)
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `fetchPlays retries multiple 202 responses with exponential backoff`() = runTest {
        // Given - multiple 202 responses followed by success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(4, mockWebServer.requestCount)
    }

    @Test
    fun `fetchPlays throws RetryException after max retry attempts on 202`() = runTest {
        // Given - 10 consecutive 202 responses (max attempts)
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        }

        // When / Then
        try {
            dataSource.fetchPlays("testuser", 1)
            fail("Expected RetryException")
        } catch (e: RetryException) {
            assertEquals(10, e.attempts)
            assertEquals(202, e.lastHttpCode)
            assertTrue(e.message?.contains("Retry attempts exceeded") == true)
        }

        assertEquals(10, mockWebServer.requestCount)
    }

    // --- 5xx Server error scenarios ---

    @Test
    fun `fetchPlays retries on 500 server error and succeeds`() = runTest {
        // Given - first response is 500, then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `fetchPlays retries on 503 service unavailable and succeeds`() = runTest {
        // Given - 503 response then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `fetchPlays throws RetryException after max retry attempts on 5xx`() = runTest {
        // Given - 10 consecutive 503 responses
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        }

        // When / Then
        try {
            dataSource.fetchPlays("testuser", 1)
            fail("Expected RetryException")
        } catch (e: RetryException) {
            assertEquals(10, e.attempts)
            assertEquals(503, e.lastHttpCode)
        }

        assertEquals(10, mockWebServer.requestCount)
    }

    // --- 429 Rate limit scenarios ---

    @Test
    fun `fetchPlays retries on 429 rate limit and succeeds`() = runTest {
        // Given - 429 then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("Too Many Requests"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    // --- Unexpected response code scenarios ---

    @Test
    fun `fetchPlays throws PlaysFetchException on unexpected response code`() = runTest {
        // Given - unexpected 400 response
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        // When / Then
        try {
            dataSource.fetchPlays("testuser", 1)
            fail("Expected PlaysFetchException")
        } catch (e: PlaysFetchException) {
            assertTrue(e.message?.contains("Unexpected HTTP") == true)
            assertTrue(e.message?.contains("400") == true)
        }
    }

    @Test
    fun `fetchPlays throws PlaysFetchException on 404 not found`() = runTest {
        // Given - 404 response
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        // When / Then
        try {
            dataSource.fetchPlays("testuser", 1)
            fail("Expected PlaysFetchException")
        } catch (e: PlaysFetchException) {
            assertTrue(e.message?.contains("404") == true)
        }
    }

    // --- Input validation scenarios ---

    @Test
    fun `fetchPlays throws IllegalArgumentException for empty username`() = runTest {
        try {
            dataSource.fetchPlays("", 1)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }

        // No requests should be made
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `fetchPlays throws IllegalArgumentException for blank username`() = runTest {
        try {
            dataSource.fetchPlays("   ", 1)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }

        assertEquals(0, mockWebServer.requestCount)
    }

    // --- Query parameter verification ---

    @Test
    fun `fetchPlays sends correct query parameters`() = runTest {
        // Given
        val playsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playsXml))

        // When
        dataSource.fetchPlays("myuser", 5)

        // Then
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("username=myuser"))
        assertTrue(request.path!!.contains("type=thing"))
        assertTrue(request.path!!.contains("page=5"))
    }

    // --- Mixed retry scenarios ---

    @Test
    fun `fetchPlays handles mixed 202 and 5xx errors before success`() = runTest {
        // Given - alternating errors then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="120" incomplete="0" location="">
                    <item name="Game" objecttype="thing" objectid="1"></item>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))

        // When
        val result = dataSource.fetchPlays("testuser", 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(4, mockWebServer.requestCount)
    }
}
