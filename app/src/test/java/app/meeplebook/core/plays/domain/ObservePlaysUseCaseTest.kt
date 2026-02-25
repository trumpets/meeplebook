package app.meeplebook.core.plays.domain

import app.cash.turbine.test
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.PlayTestFactory.createPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObservePlaysUseCase].
 */
class ObservePlaysUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: ObservePlaysUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = ObservePlaysUseCase(fakePlaysRepository)
    }

    @Test
    fun `invoke with null query passes null to repository`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan"),
            createPlay(localPlayId = 2, gameName = "Wingspan")
        )
        fakePlaysRepository.setPlays(plays)

        // When
        useCase(gameOrLocationQuery = null).first()

        // Then
        assertNull(fakePlaysRepository.lastObservePlaysQuery)
    }

    @Test
    fun `invoke with blank query returns all plays`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan"),
            createPlay(localPlayId = 2, gameName = "Wingspan")
        )
        fakePlaysRepository.setPlays(plays)

        // When
        val result = useCase(gameOrLocationQuery = "   ").first()

        // Then - blank query behaves like no filter, returns all plays
        assertEquals(2, result.size)
        assertEquals("Catan", result[0].gameName)
        assertEquals("Wingspan", result[1].gameName)
    }

    @Test
    fun `invoke with non-blank query forwards query to repository`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Wingspan", location = "Store")
        )
        fakePlaysRepository.setPlays(plays)

        // When
        useCase(gameOrLocationQuery = "Catan").first()

        // Then
        assertEquals("Catan", fakePlaysRepository.lastObservePlaysQuery)
    }

    @Test
    fun `invoke correctly maps Play to DomainPlayItem`() = runTest {
        // Given
        val player1 = createPlayer(id = 1, playId = 1, name = "Alice", score = 10, win = true)
        val player2 = createPlayer(id = 2, playId = 1, name = "Bob", score = 8, win = false)
        val play = createPlay(
            localPlayId = 1,
            gameName = "Catan",
            date = Instant.parse("2024-01-15T20:00:00Z"),
            length = 90,
            location = "Home",
            comments = "Great game!",
            players = listOf(player1, player2)
        )
        fakePlaysRepository.setPlays(listOf(play))

        // When
        val result = useCase().first()

        // Then
        assertEquals(1, result.size)
        val domainPlay = result[0]
        assertEquals(1, domainPlay.playId.localId)
        assertEquals("Catan", domainPlay.gameName)
        assertNull(domainPlay.thumbnailUrl) // Thumbnail not yet mapped from CollectionRepository
        assertEquals(Instant.parse("2024-01-15T20:00:00Z"), domainPlay.date)
        assertEquals(90, domainPlay.durationMinutes)
        assertEquals("Home", domainPlay.location)
        assertEquals("Great game!", domainPlay.comments)
        assertEquals(2, domainPlay.players.size)
        // Verify first player mapping
        assertEquals("Alice", domainPlay.players[0].name)
        assertEquals(10, domainPlay.players[0].score)
        assertTrue(domainPlay.players[0].win)
        assertNull(domainPlay.players[0].startPosition)
        // Verify second player mapping
        assertEquals("Bob", domainPlay.players[1].name)
        assertEquals(8, domainPlay.players[1].score)
        assertFalse(domainPlay.players[1].win)
        assertNull(domainPlay.players[1].startPosition)
    }

    @Test
    fun `invoke maps multiple plays correctly`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Wingspan", date = Instant.parse("2024-01-14T19:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Azul", date = Instant.parse("2024-01-13T18:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)

        // When
        val result = useCase().first()

        // Then
        assertEquals(3, result.size)
        assertEquals("Catan", result[0].gameName)
        assertEquals("Wingspan", result[1].gameName)
        assertEquals("Azul", result[2].gameName)
    }

    @Test
    fun `invoke returns empty list when no plays`() = runTest {
        // Given - no plays in repository

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke updates when repository data changes`() = runTest {
        // Given - initial state
        val initialPlays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan")
        )
        fakePlaysRepository.setPlays(initialPlays)

        // When - observe the flow and update data while observing
        useCase().test {
            // Then - first emission
            val result1 = awaitItem()
            assertEquals(1, result1.size)
            assertEquals("Catan", result1[0].gameName)

            // When - data changes
            val updatedPlays = listOf(
                createPlay(localPlayId = 2, gameName = "Wingspan"),
                createPlay(localPlayId = 1, gameName = "Catan")
            )
            fakePlaysRepository.setPlays(updatedPlays)

            // Then - second emission with updated data
            val result2 = awaitItem()
            assertEquals(2, result2.size)
            assertEquals("Wingspan", result2[0].gameName)
            assertEquals("Catan", result2[1].gameName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke with default parameter uses null query`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan")
        )
        fakePlaysRepository.setPlays(plays)

        // When - invoke without parameters
        useCase().first()

        // Then - null should be passed to repository
        assertNull(fakePlaysRepository.lastObservePlaysQuery)
    }
}
