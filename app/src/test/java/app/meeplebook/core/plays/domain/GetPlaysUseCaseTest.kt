package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlaysResponse
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayGame
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetPlaysUseCaseTest {

    private lateinit var fakeRepository: FakePlaysRepository
    private lateinit var useCase: GetPlaysUseCase

    @Before
    fun setUp() {
        fakeRepository = FakePlaysRepository()
        useCase = GetPlaysUseCase(fakeRepository)
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
    fun `invoke with blank username returns NotLoggedIn error`() = runTest {
        val result = useCase("")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlaysError.NotLoggedIn)
        assertEquals(0, fakeRepository.getPlaysCallCount)
    }

    @Test
    fun `invoke with whitespace username returns NotLoggedIn error`() = runTest {
        val result = useCase("   ")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlaysError.NotLoggedIn)
        assertEquals(0, fakeRepository.getPlaysCallCount)
    }

    @Test
    fun `invoke with valid username calls repository with default page`() = runTest {
        val plays = listOf(createTestPlay())
        fakeRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(
                plays = plays,
                totalPlays = 50,
                currentPage = 1,
                hasMorePages = false
            )
        )

        val result = useCase("testUser")

        assertTrue(result is AppResult.Success)
        assertEquals(plays, (result as AppResult.Success).data.plays)
        assertEquals(1, fakeRepository.getPlaysCallCount)
        assertEquals("testUser", fakeRepository.lastUsername)
        assertEquals(1, fakeRepository.lastPage)
    }

    @Test
    fun `invoke with page parameter passes it to repository`() = runTest {
        val plays = listOf(createTestPlay())
        fakeRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(
                plays = plays,
                totalPlays = 50,
                currentPage = 3,
                hasMorePages = false
            )
        )

        useCase("testUser", page = 3)

        assertEquals(3, fakeRepository.lastPage)
    }

    @Test
    fun `invoke with repository error returns error`() = runTest {
        fakeRepository.getPlaysResult = AppResult.Failure(PlaysError.NetworkError)

        val result = useCase("testUser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is PlaysError.NetworkError)
    }
}
