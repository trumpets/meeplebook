package app.meeplebook.feature.home.domain

import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.util.FakeDateFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetRecentPlaysUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeDateFormatter: FakeDateFormatter
    private lateinit var useCase: GetRecentPlaysUseCase

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        fakeDateFormatter = FakeDateFormatter()
        useCase = GetRecentPlaysUseCase(fakePlaysRepository, fakeDateFormatter)
    }

    @Test
    fun `returns empty list when no plays`() = runTest {
        val recentPlays = useCase()

        assertEquals(0, recentPlays.size)
    }

    @Test
    fun `returns plays sorted by date descending`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "2024-12-01", "Game A"),
                createPlay(2, "2024-12-05", "Game B"),
                createPlay(3, "2024-12-03", "Game C")
            )
        )

        val recentPlays = useCase()

        assertEquals(3, recentPlays.size)
        assertEquals("Game B", recentPlays[0].gameName) // Most recent
        assertEquals("Game C", recentPlays[1].gameName)
        assertEquals("Game A", recentPlays[2].gameName) // Oldest
    }

    @Test
    fun `limits to specified number of plays`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "2024-12-01", "Game A"),
                createPlay(2, "2024-12-02", "Game B"),
                createPlay(3, "2024-12-03", "Game C"),
                createPlay(4, "2024-12-04", "Game D"),
                createPlay(5, "2024-12-05", "Game E"),
                createPlay(6, "2024-12-06", "Game F")
            )
        )

        val recentPlays = useCase(limit = 3)

        assertEquals(3, recentPlays.size)
        assertEquals("Game F", recentPlays[0].gameName)
        assertEquals("Game E", recentPlays[1].gameName)
        assertEquals("Game D", recentPlays[2].gameName)
    }

    @Test
    fun `formats date as Today for today's date`() = runTest {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, today, "Game A"))
        )

        val recentPlays = useCase()

        assertEquals("Today", recentPlays[0].dateText)
    }

    @Test
    fun `formats date as Yesterday for yesterday's date`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, yesterday, "Game A"))
        )

        val recentPlays = useCase()

        assertEquals("Yesterday", recentPlays[0].dateText)
    }

    @Test
    fun `formats date as days ago for recent dates`() = runTest {
        val threeDaysAgo = LocalDate.now().minusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, threeDaysAgo, "Game A"))
        )

        val recentPlays = useCase()

        assertEquals("3 days ago", recentPlays[0].dateText)
    }

    @Test
    fun `formats date as MMM d for older dates`() = runTest {
        val twoWeeksAgo = LocalDate.now().minusDays(14)
        val dateString = twoWeeksAgo.format(DateTimeFormatter.ISO_LOCAL_DATE)
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, dateString, "Game A"))
        )

        val recentPlays = useCase()

        val expectedFormat = twoWeeksAgo.format(DateTimeFormatter.ofPattern("MMM d"))
        assertEquals(expectedFormat, recentPlays[0].dateText)
    }

    @Test
    fun `calculates player count correctly`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(
                    id = 1,
                    date = "2024-12-05",
                    gameName = "Game A",
                    players = listOf(
                        createPlayer(1, "Alice"),
                        createPlayer(2, "Bob"),
                        createPlayer(3, "Charlie")
                    )
                )
            )
        )

        val recentPlays = useCase()

        assertEquals(3, recentPlays[0].playerCount)
    }

    @Test
    fun `formats player names when 3 or fewer players`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(
                    id = 1,
                    date = "2024-12-05",
                    gameName = "Game A",
                    players = listOf(
                        createPlayer(1, "Alice"),
                        createPlayer(2, "Bob")
                    )
                )
            )
        )

        val recentPlays = useCase()

        assertEquals("Alice, Bob", recentPlays[0].playerNames)
    }

    @Test
    fun `formats player names with truncation for more than 3 players`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(
                    id = 1,
                    date = "2024-12-05",
                    gameName = "Game A",
                    players = listOf(
                        createPlayer(1, "Alice"),
                        createPlayer(2, "Bob"),
                        createPlayer(3, "Charlie"),
                        createPlayer(4, "Dave"),
                        createPlayer(5, "Eve")
                    )
                )
            )
        )

        val recentPlays = useCase()

        assertEquals("Alice, Bob, Charlie, +2", recentPlays[0].playerNames)
    }

    @Test
    fun `handles no players gracefully`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, "2024-12-05", "Game A", players = emptyList()))
        )

        val recentPlays = useCase()

        assertEquals("No players", recentPlays[0].playerNames)
    }

    private fun createPlay(
        id: Int,
        date: String,
        gameName: String,
        players: List<Player> = emptyList()
    ) = Play(
        id = id,
        date = date,
        quantity = 1,
        length = 60,
        incomplete = false,
        location = null,
        gameId = 1,
        gameName = gameName,
        comments = null,
        players = players
    )

    private fun createPlayer(id: Int, name: String) = Player(
        id = id,
        playId = 1,
        username = null,
        userId = null,
        name = name,
        startPosition = null,
        color = null,
        score = null,
        win = false
    )
}
