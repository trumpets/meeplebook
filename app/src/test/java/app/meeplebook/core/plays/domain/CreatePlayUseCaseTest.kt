package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.result.fold
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class CreatePlayUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: CreatePlayUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = CreatePlayUseCase(fakePlaysRepository)
    }

    @Test
    fun `invoke delegates to repository and play is persisted`() = runTest {
        val command = CreatePlayCommand(
            gameId = 42L,
            gameName = "Catan",
            date = Instant.parse("2024-05-01T00:00:00Z"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = "Home",
            comments = null,
            players = emptyList()
        )

        useCase(command)

        val plays = fakePlaysRepository.getPlays()
        assertEquals(1, plays.size)
        assertEquals(42L, plays[0].gameId)
        assertEquals("Catan", plays[0].gameName)
        assertEquals("Home", plays[0].location)
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        fakePlaysRepository.createPlayException = RuntimeException("DB error")

        val command = CreatePlayCommand(
            gameId = 1L,
            gameName = "Wingspan",
            date = Instant.now(),
            quantity = 1,
            length = null,
            incomplete = false,
            location = null,
            comments = null,
            players = emptyList()
        )

        var threw = false
        useCase(command).fold(
            onSuccess = { /* no-op */ },
            onFailure = { error ->
                threw = true
            }
        )

        assertTrue("Expected RuntimeException to be thrown", threw)
    }
}
