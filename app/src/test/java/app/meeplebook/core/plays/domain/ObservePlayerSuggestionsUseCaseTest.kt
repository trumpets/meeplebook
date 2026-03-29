package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.PlayTestFactory.createPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObservePlayerSuggestionsUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: ObservePlayerSuggestionsUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = ObservePlayerSuggestionsUseCase(fakePlaysRepository)
    }

    @Test
    fun `returns empty list when no plays at location`() = runTest {
        val result = useCase("Home").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns player identities for players at given location`() = runTest {
        val player = createPlayer(playId = 1, name = "Alice", username = "alice", userId = 10L)
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, "Catan", location = "Home", players = listOf(player)))
        )

        val result = useCase("Home").first()

        assertEquals(1, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals("alice", result[0].username)
        assertEquals(10L, result[0].userId)
    }

    @Test
    fun `does not return players from different locations`() = runTest {
        val player = createPlayer(playId = 1, name = "Bob")
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, "Catan", location = "Club", players = listOf(player)))
        )

        val result = useCase("Home").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `orders players by frequency descending`() = runTest {
        val alice = createPlayer(playId = 1, name = "Alice", username = "alice", userId = 1L)
        val bob = createPlayer(playId = 2, name = "Bob", username = "bob", userId = 2L)
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "Catan", location = "Home", players = listOf(alice, bob)),
                createPlay(2, "Wingspan", location = "Home", players = listOf(alice)),
            )
        )

        val result = useCase("Home").first()

        assertEquals(2, result.size)
        assertEquals("Alice", result[0].name) // played 2 times
        assertEquals("Bob", result[1].name)   // played 1 time
    }
}
