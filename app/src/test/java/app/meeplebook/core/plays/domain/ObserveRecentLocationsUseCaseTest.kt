package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveRecentLocationsUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: ObserveRecentLocationsUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = ObserveRecentLocationsUseCase(fakePlaysRepository)
    }

    @Test
    fun `returns empty list when no plays`() = runTest {
        val result = useCase().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns unique locations ordered by most recent play`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "Game A", location = "Home",
                    date = java.time.Instant.parse("2024-01-01T00:00:00Z")),
                createPlay(2, "Game B", location = "Club",
                    date = java.time.Instant.parse("2024-01-03T00:00:00Z")),
                createPlay(3, "Game C", location = "Home",
                    date = java.time.Instant.parse("2024-01-02T00:00:00Z"))
            )
        )

        val result = useCase().first()

        assertEquals(listOf("Club", "Home"), result)
    }

    @Test
    fun `omits plays with null location`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "Game A", location = null),
                createPlay(2, "Game B", location = "Club")
            )
        )

        val result = useCase().first()

        assertEquals(listOf("Club"), result)
    }
}
