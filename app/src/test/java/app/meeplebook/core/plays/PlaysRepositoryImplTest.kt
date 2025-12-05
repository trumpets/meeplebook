package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayGame
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.plays.remote.FakeBggPlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.util.xml.PlaysXmlParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PlaysRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeBggPlaysRemoteDataSource
    private lateinit var repository: PlaysRepositoryImpl

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeBggPlaysRemoteDataSource()
        repository = PlaysRepositoryImpl(fakeRemoteDataSource)
    }

    private fun createTestPlay(id: Long = 1L) = Play(
        playId = id,
        date = "2024-01-15",
        quantity = 1,
        length = 60,
        incomplete = false,
        noWinStats = false,
        location = "Home",
        comments = "Great game!",
        game = PlayGame(objectId = 100L, name = "Catan"),
        players = emptyList()
    )

    @Test
    fun `getPlays success returns plays and updates cache`() = runTest {
        val plays = listOf(createTestPlay())
        fakeRemoteDataSource.playsResult = PlaysXmlParser.PlaysResponse(
            plays = plays,
            total = 50,
            page = 1
        )

        val result = repository.getPlays("testUser", page = 1)

        assertTrue(result is AppResult.Success)
        val response = (result as AppResult.Success).data
        assertEquals(plays, response.plays)
        assertEquals(50, response.totalPlays)
        assertEquals(1, response.currentPage)
        assertFalse(response.hasMorePages)
        assertEquals(plays, repository.observePlays().first())
    }

    @Test
    fun `getPlays with more than 100 plays indicates hasMorePages`() = runTest {
        val plays = listOf(createTestPlay())
        fakeRemoteDataSource.playsResult = PlaysXmlParser.PlaysResponse(
            plays = plays,
            total = 150, // More than 100, so page 2 should exist
            page = 1
        )

        val result = repository.getPlays("testUser", page = 1)

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.hasMorePages)
    }

    @Test
    fun `getPlays page 2 appends to cache`() = runTest {
        // First page
        val firstPagePlays = listOf(createTestPlay(1L))
        fakeRemoteDataSource.playsResult = PlaysXmlParser.PlaysResponse(
            plays = firstPagePlays,
            total = 150,
            page = 1
        )
        repository.getPlays("testUser", page = 1)

        // Second page
        val secondPagePlays = listOf(createTestPlay(2L))
        fakeRemoteDataSource.playsResult = PlaysXmlParser.PlaysResponse(
            plays = secondPagePlays,
            total = 150,
            page = 2
        )
        repository.getPlays("testUser", page = 2)

        val cached = repository.observePlays().first()
        assertEquals(2, cached.size)
        assertEquals(1L, cached[0].playId)
        assertEquals(2L, cached[1].playId)
    }

    @Test
    fun `getPlays page 1 replaces cache`() = runTest {
        // First load
        val firstPlays = listOf(createTestPlay(1L))
        fakeRemoteDataSource.playsResult = PlaysXmlParser.PlaysResponse(
            plays = firstPlays,
            total = 50,
            page = 1
        )
        repository.getPlays("testUser", page = 1)

        // Refresh (page 1 again)
        val secondPlays = listOf(createTestPlay(2L))
        fakeRemoteDataSource.playsResult = PlaysXmlParser.PlaysResponse(
            plays = secondPlays,
            total = 50,
            page = 1
        )
        repository.getPlays("testUser", page = 1)

        val cached = repository.observePlays().first()
        assertEquals(1, cached.size)
        assertEquals(2L, cached[0].playId)
    }

    @Test
    fun `getPlays with IOException returns NetworkError`() = runTest {
        fakeRemoteDataSource.exception = IOException("Network error")

        val result = repository.getPlays("testUser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlaysError.NetworkError)
    }

    @Test
    fun `getPlays with IllegalArgumentException returns NotLoggedIn`() = runTest {
        fakeRemoteDataSource.exception = IllegalArgumentException("Empty username")

        val result = repository.getPlays("")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlaysError.NotLoggedIn)
    }

    @Test
    fun `getPlays with unknown exception returns Unknown error`() = runTest {
        val unexpectedException = RuntimeException("Unexpected")
        fakeRemoteDataSource.exception = unexpectedException

        val result = repository.getPlays("testUser")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlaysError.Unknown)
        assertEquals(unexpectedException, (error as PlaysError.Unknown).throwable)
    }

    @Test
    fun `observePlays returns empty list initially`() = runTest {
        val result = repository.observePlays().first()

        assertTrue(result.isEmpty())
    }
}
