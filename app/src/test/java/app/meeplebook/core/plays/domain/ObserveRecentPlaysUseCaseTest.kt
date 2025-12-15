package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObserveRecentPlaysUseCase].
 */
class ObserveRecentPlaysUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: ObserveRecentPlaysUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = ObserveRecentPlaysUseCase(fakePlaysRepository)
    }

    @Test
    fun `invoke returns recent plays with default limit`() = runTest {
        // Given
        val recentPlays = listOf(
            createPlay(id = 1, gameName = "Catan", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(id = 2, gameName = "Wingspan", date = Instant.parse("2024-01-14T19:00:00Z")),
            createPlay(id = 3, gameName = "Azul", date = Instant.parse("2024-01-13T18:00:00Z"))
        )
        fakePlaysRepository.setRecentPlays(recentPlays)

        // When
        val plays = useCase().first()

        // Then
        assertEquals(3, plays.size)
        assertEquals("Catan", plays[0].gameName)
        assertEquals("Wingspan", plays[1].gameName)
        assertEquals("Azul", plays[2].gameName)
    }

    @Test
    fun `invoke returns recent plays with custom limit`() = runTest {
        // Given
        val recentPlays = listOf(
            createPlay(id = 1, gameName = "Game 1"),
            createPlay(id = 2, gameName = "Game 2"),
            createPlay(id = 3, gameName = "Game 3")
        )
        fakePlaysRepository.setRecentPlays(recentPlays)

        // When
        val plays = useCase(limit = 10).first()

        // Then - repository should have been called with correct limit
        assertEquals(3, plays.size)
    }

    @Test
    fun `invoke returns empty list when no plays`() = runTest {
        // Given - no plays in repository

        // When
        val plays = useCase().first()

        // Then
        assertTrue(plays.isEmpty())
    }

    @Test
    fun `invoke updates when repository data changes`() = runTest {
        // Given - initial state
        val initialPlays = listOf(
            createPlay(id = 1, gameName = "Catan")
        )
        fakePlaysRepository.setRecentPlays(initialPlays)

        // When - first observation
        val plays1 = useCase().first()

        // Then
        assertEquals(1, plays1.size)
        assertEquals("Catan", plays1[0].gameName)

        // When - data changes
        val updatedPlays = listOf(
            createPlay(id = 2, gameName = "Wingspan"),
            createPlay(id = 1, gameName = "Catan")
        )
        fakePlaysRepository.setRecentPlays(updatedPlays)
        val plays2 = useCase().first()

        // Then - plays are updated
        assertEquals(2, plays2.size)
        assertEquals("Wingspan", plays2[0].gameName)
    }

    private fun createPlay(
        id: Long,
        gameName: String,
        date: Instant = Instant.parse("2024-01-15T20:00:00Z")
    ): Play {
        return Play(
            id = id,
            date = date,
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = id * 100,
            gameName = gameName,
            comments = null,
            players = listOf(
                Player(
                    id = 0,
                    playId = id,
                    username = "testuser",
                    userId = 12345,
                    name = "Test User",
                    startPosition = null,
                    color = null,
                    score = null,
                    win = false
                )
            )
        )
    }
}
