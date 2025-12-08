package app.meeplebook.core.collection.remote.integration

import app.meeplebook.core.collection.remote.CollectionRemoteDataSourceImpl
import app.meeplebook.core.network.BggApi
import app.meeplebook.core.network.RetryException
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
 * Integration tests for [CollectionRemoteDataSourceImpl] using MockWebServer.
 *
 * These tests verify the retry logic, exponential backoff, and proper resource cleanup
 * for various response codes from the BGG API.
 */
class CollectionRemoteDataSourceImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: CollectionRemoteDataSourceImpl

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

        dataSource = CollectionRemoteDataSourceImpl(bggApi)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Success scenarios ---

    @Test
    fun `fetchCollection returns items on 200 response`() = runTest {
        // Given - successful responses for both boardgames and expansions
        val boardgamesXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="123" subtype="boardgame">
                    <name>Test Game</name>
                    <yearpublished>2020</yearpublished>
                    <thumbnail>https://example.com/thumb.jpg</thumbnail>
                </item>
            </items>
        """.trimIndent()

        val expansionsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="456" subtype="boardgameexpansion">
                    <name>Test Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(boardgamesXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionsXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
        assertEquals(123, result[0].gameId)
        assertEquals("Test Game", result[0].name)
        assertEquals(456, result[1].gameId)
        assertEquals("Test Expansion", result[1].name)
    }

    // --- 202 Retry scenarios ---

    @Test
    fun `fetchCollection retries on 202 response and succeeds`() = runTest {
        // Given - first response is 202 (queued), second is 200 (success)
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        // Boardgames: 202 then 200
        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))
        // Expansions: direct 200
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
        // Verify 3 requests were made (2 for boardgames retry, 1 for expansions)
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun `fetchCollection retries multiple 202 responses with exponential backoff`() = runTest {
        // Given - multiple 202 responses followed by success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        // Boardgames: 3x 202, then 200
        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))
        // Expansions: direct 200
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
        // Verify 5 requests were made (4 for boardgames retries, 1 for expansions)
        assertEquals(5, mockWebServer.requestCount)
    }

    @Test
    fun `fetchCollection throws RetryException after max retry attempts on 202`() = runTest {
        // Given - 10 consecutive 202 responses (max attempts)
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        }

        // When / Then
        try {
            dataSource.fetchCollection("testuser")
            fail("Expected RetryException")
        } catch (e: RetryException) {
            assertEquals(10, e.attempts)
            assertEquals(202, e.lastHttpCode)
            assertTrue(e.message?.contains("Retry attempts exceeded") == true)
        }

        // Verify exactly 10 requests were made
        assertEquals(10, mockWebServer.requestCount)
    }

    // --- 5xx Server error scenarios ---

    @Test
    fun `fetchCollection retries on 500 server error and succeeds`() = runTest {
        // Given - first response is 500, then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun `fetchCollection retries on 503 service unavailable and succeeds`() = runTest {
        // Given - 503 response then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `fetchCollection throws RetryException after max retry attempts on 5xx`() = runTest {
        // Given - 10 consecutive 503 responses
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        }

        // When / Then
        try {
            dataSource.fetchCollection("testuser")
            fail("Expected RetryException")
        } catch (e: RetryException) {
            assertEquals(10, e.attempts)
            assertEquals(503, e.lastHttpCode)
        }

        assertEquals(10, mockWebServer.requestCount)
    }

    // --- 429 Rate limit scenarios ---

    @Test
    fun `fetchCollection retries on 429 rate limit and succeeds`() = runTest {
        // Given - 429 then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("Too Many Requests"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
    }

    // --- Unexpected response code scenarios ---

    @Test
    fun `fetchCollection throws RetryException on unexpected response code`() = runTest {
        // Given - unexpected 400 response
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        // When / Then
        try {
            dataSource.fetchCollection("testuser")
            fail("Expected RetryException")
        } catch (e: RetryException) {
            assertEquals(400, e.lastHttpCode)
            assertTrue(e.message?.contains("Unexpected HTTP") == true)
        }
    }

    @Test
    fun `fetchCollection throws RetryException on 404 not found`() = runTest {
        // Given - 404 response
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        // When / Then
        try {
            dataSource.fetchCollection("testuser")
            fail("Expected RetryException")
        } catch (e: RetryException) {
            assertEquals(404, e.lastHttpCode)
        }
    }

    // --- Input validation scenarios ---

    @Test
    fun `fetchCollection throws IllegalArgumentException for empty username`() = runTest {
        try {
            dataSource.fetchCollection("")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }

        // No requests should be made
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `fetchCollection throws IllegalArgumentException for blank username`() = runTest {
        try {
            dataSource.fetchCollection("   ")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }

        assertEquals(0, mockWebServer.requestCount)
    }

    // --- Query parameter verification ---

    @Test
    fun `fetchCollection sends correct query parameters for boardgames`() = runTest {
        // Given
        val boardgamesXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(boardgamesXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        dataSource.fetchCollection("myuser")

        // Then - verify first request (boardgames)
        val request1 = mockWebServer.takeRequest()
        assertTrue(request1.path!!.contains("username=myuser"))
        assertTrue(request1.path!!.contains("own=1"))
        assertTrue(request1.path!!.contains("showprivate=1"))
        assertTrue(request1.path!!.contains("excludesubtype=boardgameexpansion"))

        // Verify second request (expansions)
        val request2 = mockWebServer.takeRequest()
        assertTrue(request2.path!!.contains("username=myuser"))
        assertTrue(request2.path!!.contains("subtype=boardgameexpansion"))
    }

    // --- Mixed retry scenarios ---

    @Test
    fun `fetchCollection handles mixed 202 and 5xx errors before success`() = runTest {
        // Given - alternating errors then success
        val successXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionXml))

        // When
        val result = dataSource.fetchCollection("testuser")

        // Then
        assertEquals(2, result.size)
        assertEquals(5, mockWebServer.requestCount)
    }
}
