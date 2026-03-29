package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchLocationsUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: SearchLocationsUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = SearchLocationsUseCase(fakePlaysRepository)
    }

    @Test
    fun `returns empty list when no plays`() = runTest {
        val result = useCase("").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns all locations for empty query`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "Game A", location = "Home"),
                createPlay(2, "Game B", location = "Club"),
                createPlay(3, "Game C", location = "Work")
            )
        )

        val result = useCase("").first()

        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf("Home", "Club", "Work")))
    }

    @Test
    fun `filters locations by case-insensitive prefix`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "Game A", location = "Home"),
                createPlay(2, "Game B", location = "hotel"),
                createPlay(3, "Game C", location = "Club")
            )
        )

        val result = useCase("ho").first()

        assertEquals(listOf("Home", "hotel"), result)
    }

    @Test
    fun `returns deduplicated locations`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "Game A", location = "Home"),
                createPlay(2, "Game B", location = "Home"),
                createPlay(3, "Game C", location = "Club")
            )
        )

        val result = useCase("").first()

        assertEquals(2, result.size)
    }
}
