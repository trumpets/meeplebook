package app.meeplebook.core.plays

import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.local.FakePlaysLocalDataSource
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.remote.FakePlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PlaysRepositoryImplTest {

    private lateinit var local: FakePlaysLocalDataSource
    private lateinit var remote: FakePlaysRemoteDataSource
    private lateinit var repository: PlaysRepositoryImpl

    private val testPlay = Play(
        id = 1,
        date = "2024-01-01",
        quantity = 1,
        length = 60,
        incomplete = false,
        location = "Home",
        gameId = 123,
        gameName = "Test Game",
        gameSubtype = "boardgame",
        comments = null,
        players = emptyList()
    )

    @Before
    fun setup() {
        local = FakePlaysLocalDataSource()
        remote = FakePlaysRemoteDataSource()
        repository = PlaysRepositoryImpl(local, remote)
    }

    @Test
    fun `observePlays returns flow from local data source`() = runTest {
        val plays = listOf(testPlay)
        local.savePlays(plays)

        val result = repository.observePlays()

        var emittedPlays: List<Play>? = null
        result.collect { emittedPlays = it }

        assertEquals(plays, emittedPlays)
    }

    @Test
    fun `getPlays returns data from local data source`() = runTest {
        val plays = listOf(testPlay)
        local.savePlays(plays)

        val result = repository.getPlays()

        assertEquals(plays, result)
    }

    @Test
    fun `syncPlays success fetches from remote and saves to local`() = runTest {
        val plays = listOf(testPlay)
        remote.playsToReturn = plays

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Success)
        assertEquals(plays, (result as AppResult.Success).value)
        assertTrue(remote.fetchPlaysCalled)
        assertEquals("user123", remote.lastFetchUsername)
        assertEquals(1, remote.lastFetchPage)
        assertEquals(plays, local.getPlays())
    }

    @Test
    fun `syncPlays fetches multiple pages`() = runTest {
        // Simulate multi-page response
        val page1Plays = List(100) { i ->
            testPlay.copy(id = i + 1)
        }
        val page2Plays = List(50) { i ->
            testPlay.copy(id = i + 101)
        }
        
        // Configure fake to return different results per page
        var callCount = 0
        val fakePlaysRemote = object : PlaysRemoteDataSource {
            override suspend fun fetchPlays(username: String, page: Int?): List<Play> {
                callCount++
                return when (page) {
                    1 -> page1Plays
                    2 -> page2Plays
                    else -> emptyList()
                }
            }
        }
        
        val repo = PlaysRepositoryImpl(local, fakePlaysRemote)
        val result = repo.syncPlays("user123")

        assertTrue(result is AppResult.Success)
        val allPlays = (result as AppResult.Success).value
        assertEquals(150, allPlays.size)
        assertEquals(2, callCount)
        assertEquals(150, local.getPlays().size)
    }

    @Test
    fun `syncPlays returns NotLoggedIn on IllegalArgumentException`() = runTest {
        remote.shouldThrowException = IllegalArgumentException("Invalid username")

        val result = repository.syncPlays("")

        assertTrue(result is AppResult.Failure)
        assertEquals(PlayError.NotLoggedIn, (result as AppResult.Failure).error)
        assertTrue(local.getPlays().isEmpty())
    }

    @Test
    fun `syncPlays returns NetworkError on IOException`() = runTest {
        remote.shouldThrowException = IOException("Network error")

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        assertEquals(PlayError.NetworkError, (result as AppResult.Failure).error)
    }

    @Test
    fun `syncPlays returns MaxRetriesExceeded on RetryException`() = runTest {
        val retryException = RetryException("Retry failed", "user123", 202, 5, 1000L)
        remote.shouldThrowException = retryException

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlayError.MaxRetriesExceeded)
        assertEquals(retryException, (error as PlayError.MaxRetriesExceeded).exception)
    }

    @Test
    fun `syncPlays returns Unknown error for other exceptions`() = runTest {
        val exception = RuntimeException("Unknown error")
        remote.shouldThrowException = exception

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlayError.Unknown)
        assertEquals(exception, (error as PlayError.Unknown).throwable)
    }

    @Test
    fun `clearPlays calls local data source`() = runTest {
        local.savePlays(listOf(testPlay))

        repository.clearPlays()

        assertTrue(local.getPlays().isEmpty())
    }
}
