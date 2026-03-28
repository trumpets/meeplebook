package app.meeplebook.core.plays

import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FakePlaysRepository], specifically verifying that [FakePlaysRepository.syncPlays]
 * correctly mirrors production behaviour: when [FakePlaysRepository.syncPlaysData] is set and
 * sync succeeds, observable flows are updated.
 */
class FakePlaysRepositoryTest {

    private lateinit var repository: FakePlaysRepository

    @Before
    fun setUp() {
        repository = FakePlaysRepository()
    }

    @Test
    fun `syncPlays success with syncPlaysData set updates plays flow`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Wingspan"),
            createPlay(localPlayId = 2, gameName = "Azul"),
        )
        repository.syncPlaysResult = AppResult.Success(Unit)
        repository.syncPlaysData = plays

        repository.syncPlays("testuser")

        val observed = repository.observePlays(null).first()
        assertEquals(2, observed.size)
        assertEquals("Wingspan", observed[0].gameName)
        assertEquals("Azul", observed[1].gameName)
    }

    @Test
    fun `syncPlays success with syncPlaysData set updates derived flows`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Wingspan", quantity = 3),
            createPlay(localPlayId = 2, gameName = "Azul", quantity = 1),
        )
        repository.syncPlaysResult = AppResult.Success(Unit)
        repository.syncPlaysData = plays

        repository.syncPlays("testuser")

        assertEquals(4L, repository.observeTotalPlaysCount().first())
        assertEquals(2L, repository.observeUniqueGamesCount().first())
    }

    @Test
    fun `syncPlays success with null syncPlaysData leaves plays unchanged`() = runTest {
        val initialPlays = listOf(createPlay(localPlayId = 1, gameName = "Gloomhaven"))
        repository.setPlays(initialPlays)
        repository.syncPlaysResult = AppResult.Success(Unit)
        // syncPlaysData left as null (default)

        repository.syncPlays("testuser")

        val observed = repository.observePlays(null).first()
        assertEquals(1, observed.size)
        assertEquals("Gloomhaven", observed[0].gameName)
    }

    @Test
    fun `syncPlays failure with syncPlaysData set does not update plays flow`() = runTest {
        val initialPlays = listOf(createPlay(localPlayId = 1, gameName = "Gloomhaven"))
        repository.setPlays(initialPlays)
        repository.syncPlaysResult = AppResult.Failure(PlayError.NetworkError)
        repository.syncPlaysData = listOf(createPlay(localPlayId = 2, gameName = "Catan"))

        repository.syncPlays("testuser")

        val observed = repository.observePlays(null).first()
        assertEquals(1, observed.size)
        assertEquals("Gloomhaven", observed[0].gameName)
    }

    @Test
    fun `syncPlays returns the configured result`() = runTest {
        repository.syncPlaysResult = AppResult.Success(Unit)

        val result = repository.syncPlays("testuser")

        assertEquals(AppResult.Success(Unit), result)
    }

    @Test
    fun `syncPlays tracks call count and username`() = runTest {
        repository.syncPlaysResult = AppResult.Success(Unit)

        repository.syncPlays("alice")
        repository.syncPlays("alice")

        assertEquals(2, repository.syncCallCount)
        assertEquals("alice", repository.lastSyncUsername)
    }
}
