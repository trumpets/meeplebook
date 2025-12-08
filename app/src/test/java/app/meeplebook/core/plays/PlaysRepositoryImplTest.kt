package app.meeplebook.core.plays

import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.local.PlaysLocalDataSource
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.remote.PlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PlaysRepositoryImplTest {

    private lateinit var local: PlaysLocalDataSource
    private lateinit var remote: PlaysRemoteDataSource
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
        local = mockk(relaxed = true)
        remote = mockk(relaxed = true)
        repository = PlaysRepositoryImpl(local, remote)
    }

    @Test
    fun `observePlays returns flow from local data source`() = runTest {
        val plays = listOf(testPlay)
        coEvery { local.observePlays() } returns flowOf(plays)

        val result = repository.observePlays()

        var emittedPlays: List<Play>? = null
        result.collect { emittedPlays = it }

        assertEquals(plays, emittedPlays)
    }

    @Test
    fun `getPlays returns data from local data source`() = runTest {
        val plays = listOf(testPlay)
        coEvery { local.getPlays() } returns plays

        val result = repository.getPlays()

        assertEquals(plays, result)
    }

    @Test
    fun `syncPlays success fetches from remote and saves to local`() = runTest {
        val plays = listOf(testPlay)
        coEvery { remote.fetchPlays("user123", null) } returns plays

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Success)
        assertEquals(plays, (result as AppResult.Success).value)
        coVerify { remote.fetchPlays("user123", null) }
        coVerify { local.savePlays(plays) }
    }

    @Test
    fun `syncPlays with page number`() = runTest {
        val plays = listOf(testPlay)
        coEvery { remote.fetchPlays("user123", 2) } returns plays

        val result = repository.syncPlays("user123", 2)

        assertTrue(result is AppResult.Success)
        coVerify { remote.fetchPlays("user123", 2) }
    }

    @Test
    fun `syncPlays returns NotLoggedIn on IllegalArgumentException`() = runTest {
        coEvery { remote.fetchPlays(any(), any()) } throws IllegalArgumentException("Invalid username")

        val result = repository.syncPlays("", null)

        assertTrue(result is AppResult.Failure)
        assertEquals(PlayError.NotLoggedIn, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { local.savePlays(any()) }
    }

    @Test
    fun `syncPlays returns NetworkError on IOException`() = runTest {
        coEvery { remote.fetchPlays(any(), any()) } throws IOException("Network error")

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        assertEquals(PlayError.NetworkError, (result as AppResult.Failure).error)
    }

    @Test
    fun `syncPlays returns MaxRetriesExceeded on RetryException`() = runTest {
        val retryException = RetryException("Retry failed", "user123", 202, 5, 1000L)
        coEvery { remote.fetchPlays(any(), any()) } throws retryException

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlayError.MaxRetriesExceeded)
        assertEquals(retryException, (error as PlayError.MaxRetriesExceeded).exception)
    }

    @Test
    fun `syncPlays returns Unknown error for other exceptions`() = runTest {
        val exception = RuntimeException("Unknown error")
        coEvery { remote.fetchPlays(any(), any()) } throws exception

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlayError.Unknown)
        assertEquals(exception, (error as PlayError.Unknown).throwable)
    }

    @Test
    fun `clearPlays calls local data source`() = runTest {
        repository.clearPlays()

        coVerify { local.clearPlays() }
    }
}
