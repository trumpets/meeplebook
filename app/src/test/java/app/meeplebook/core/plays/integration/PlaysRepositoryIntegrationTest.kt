package app.meeplebook.core.plays.integration

import app.meeplebook.core.network.BggApi
import app.meeplebook.core.plays.PlaysRepositoryImpl
import app.meeplebook.core.plays.local.FakePlaysLocalDataSource
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.remote.PlaysRemoteDataSourceImpl
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [PlaysRepositoryImpl] with real network calls via MockWebServer.
 */
class PlaysRepositoryIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeLocalDataSource: FakePlaysLocalDataSource
    private lateinit var remoteDataSource: PlaysRemoteDataSourceImpl
    private lateinit var repository: PlaysRepositoryImpl

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

        fakeLocalDataSource = FakePlaysLocalDataSource()
        remoteDataSource = PlaysRemoteDataSourceImpl(bggApi)
        repository = PlaysRepositoryImpl(fakeLocalDataSource, remoteDataSource)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `syncPlays success with single page`() = runTest {
        // Given - single page response with 2 plays
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
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val plays = repository.getPlays()
        assertEquals(2, plays.size)
        assertEquals("Gloomhaven", plays[0].gameName)
        assertEquals("Catan", plays[1].gameName)
        assertEquals(1, plays[0].players.size)
    }

    @Test
    fun `syncPlays success with multiple pages`() = runTest {
        // Given - two pages of plays (100 on page 1, 50 on page 2)
        val page1Plays = buildPlaysXml(1, 100)
        val page2Plays = buildPlaysXml(101, 50)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page1Plays))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page2Plays))

        // When
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val plays = repository.getPlays()
        assertEquals(150, plays.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `syncPlays stops fetching when page has less than 100 plays`() = runTest {
        // Given - first page has 100 plays, second page has 30
        val page1Plays = buildPlaysXml(1, 100)
        val page2Plays = buildPlaysXml(101, 30)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page1Plays))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page2Plays))

        // When
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val plays = repository.getPlays()
        assertEquals(130, plays.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `syncPlays stops fetching when page is empty`() = runTest {
        // Given - first page has plays, second page is empty
        val page1Plays = buildPlaysXml(1, 100)
        val emptyPlays = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="0" page="2">
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page1Plays))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(emptyPlays))

        // When
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val plays = repository.getPlays()
        assertEquals(100, plays.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `syncPlays stores plays locally incrementally`() = runTest {
        // Given - two pages of plays
        val page1Plays = buildPlaysXml(1, 100)
        val page2Plays = buildPlaysXml(101, 50)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page1Plays))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(page2Plays))

        // When
        repository.syncPlays("testuser")

        // Then - all plays stored locally
        val localPlays = repository.observePlays().first()
        assertEquals(150, localPlays.size)
    }

    @Test
    fun `syncPlays with server error returns MaxRetriesExceeded`() = runTest {
        // Given - server errors will trigger retry logic until max attempts
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(503))
        }

        // When
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlayError.MaxRetriesExceeded)
    }

    @Test
    fun `syncPlays sends correct query parameters`() = runTest {
        // Given
        val playsXml = buildPlaysXml(1, 10)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playsXml))

        // When
        repository.syncPlays("myuser")

        // Then
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("username=myuser"))
        assertTrue(request.path!!.contains("type=thing"))
        assertTrue(request.path!!.contains("page=1"))
    }

    @Test
    fun `syncPlays handles retry on 202 for first page`() = runTest {
        // Given - first request returns 202, second returns success
        val playsXml = buildPlaysXml(1, 10)

        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody("queued"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playsXml))

        // When
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val plays = repository.getPlays()
        assertEquals(10, plays.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `syncPlays with empty username returns NotLoggedIn`() = runTest {
        // When
        val result = repository.syncPlays("")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlayError.NotLoggedIn)
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `syncPlays normalizes play data correctly`() = runTest {
        // Given - plays with blank/zero optional fields
        val playsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1">
                <play id="1" date="2024-01-15" quantity="1" length="0" incomplete="0" location="">
                    <item name="Test Game" objecttype="thing" objectid="1"></item>
                    <comments></comments>
                </play>
            </plays>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playsXml))

        // When
        val result = repository.syncPlays("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        assertEquals(null, plays[0].length) // 0 normalized to null
        assertEquals(null, plays[0].location) // blank normalized to null
        assertEquals(null, plays[0].comments) // blank normalized to null
    }

    // --- Helper methods ---

    private fun buildPlaysXml(startId: Int, count: Int): String {
        val plays = (startId until startId + count).joinToString("\n") { id ->
            """<play id="$id" date="2024-01-01" quantity="1" length="60" incomplete="0" location="">
    <item name="Game $id" objecttype="thing" objectid="$id"></item>
</play>"""
        }

        return """<?xml version="1.0" encoding="utf-8"?>
<plays username="testuser" userid="123" total="$count" page="1">
    $plays
</plays>"""
    }
}
