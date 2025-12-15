package app.meeplebook.core.collection.integration

import app.meeplebook.core.collection.CollectionRepositoryImpl
import app.meeplebook.core.collection.local.FakeCollectionLocalDataSource
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.remote.CollectionRemoteDataSourceImpl
import app.meeplebook.core.network.BggApi
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
 * Integration tests for [CollectionRepositoryImpl] with real network calls via MockWebServer.
 */
class CollectionRepositoryIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeLocalDataSource: FakeCollectionLocalDataSource
    private lateinit var remoteDataSource: CollectionRemoteDataSourceImpl
    private lateinit var repository: CollectionRepositoryImpl

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

        fakeLocalDataSource = FakeCollectionLocalDataSource()
        remoteDataSource = CollectionRemoteDataSourceImpl(bggApi)
        repository = CollectionRepositoryImpl(fakeLocalDataSource, remoteDataSource)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `syncCollection success with boardgames only`() = runTest {
        // Given - two successful responses (boardgames and expansions)
        val boardgamesXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="2">
                <item objectid="1" subtype="boardgame">
                    <name>Game 1</name>
                    <yearpublished>2020</yearpublished>
                    <thumbnail>https://example.com/1.jpg</thumbnail>
                </item>
                <item objectid="2" subtype="boardgame">
                    <name>Game 2</name>
                    <yearpublished>2021</yearpublished>
                    <thumbnail>https://example.com/2.jpg</thumbnail>
                </item>
            </items>
        """.trimIndent()

        val expansionsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="3" subtype="boardgameexpansion">
                    <name>Expansion 1</name>
                    <yearpublished>2022</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(boardgamesXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionsXml))

        // When
        val result = repository.syncCollection("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val items = (result as AppResult.Success).data
        assertEquals(3, items.size)
        assertEquals("Game 1", items[0].name)
        assertEquals("Game 2", items[1].name)
        assertEquals(1, fakeLocalDataSource.saveCollectionCallCount)
    }

    @Test
    fun `syncCollection success with expansions`() = runTest {
        // Given
        val boardgamesXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Base Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="100" subtype="boardgameexpansion">
                    <name>Expansion 1</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(boardgamesXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionsXml))

        // When
        val result = repository.syncCollection("testuser")

        // Then
        assertTrue(result is AppResult.Success)
        val items = (result as AppResult.Success).data
        assertEquals(2, items.size)

        val baseGame = items.find { it.gameId == 1L }!!
        assertEquals(GameSubtype.BOARDGAME, baseGame.subtype)

        val expansion = items.find { it.gameId == 100L }!!
        assertEquals(GameSubtype.BOARDGAME_EXPANSION, expansion.subtype)
    }

    @Test
    fun `syncCollection stores items locally`() = runTest {
        // Given
        val boardgamesXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Test Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Test Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(boardgamesXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionsXml))

        // When
        repository.syncCollection("testuser")

        // Then
        val localItems = repository.observeCollection().first()
        assertEquals(2, localItems.size)
        assertEquals("Test Game", localItems[0].name)
    }

    @Test
    fun `syncCollection with server error returns MaxRetriesExceeded`() = runTest {
        // Given - server errors will trigger retry logic until max attempts
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(503))
        }

        // When
        val result = repository.syncCollection("testuser")

        // Then
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.MaxRetriesExceeded)
    }

    @Test
    fun `syncCollection sends correct query parameters`() = runTest {
        // Given - valid responses with items to avoid retry logic
        val boardgamesXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="1" subtype="boardgame">
                    <name>Test Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val expansionsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1">
                <item objectid="2" subtype="boardgameexpansion">
                    <name>Test Expansion</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(boardgamesXml))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expansionsXml))

        // When
        repository.syncCollection("myuser")

        // Then
        val request1 = mockWebServer.takeRequest()
        assertTrue(request1.path!!.contains("username=myuser"))
        assertTrue(request1.path!!.contains("own=1"))
        assertTrue(request1.path!!.contains("showprivate=1"))
        assertTrue(request1.path!!.contains("excludesubtype=boardgameexpansion"))

        val request2 = mockWebServer.takeRequest()
        assertTrue(request2.path!!.contains("subtype=boardgameexpansion"))
    }
}
