package app.meeplebook.feature.home.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetHomeStatsUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: GetHomeStatsUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        useCase = GetHomeStatsUseCase(fakeCollectionRepository, fakePlaysRepository)
    }

    @Test
    fun `returns empty stats when no data`() = runTest {
        val stats = useCase()

        assertEquals(0, stats.gamesCount)
        assertEquals(0, stats.totalPlays)
        assertEquals(0, stats.playsThisMonth)
        assertEquals(0, stats.unplayedCount)
    }

    @Test
    fun `calculates games count correctly`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1"),
                createCollectionItem(2, "Game 2"),
                createCollectionItem(3, "Game 3")
            )
        )

        val stats = useCase()

        assertEquals(3, stats.gamesCount)
    }

    @Test
    fun `calculates total plays correctly`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "2024-12-01", quantity = 2),
                createPlay(2, "2024-12-05", quantity = 1),
                createPlay(3, "2024-11-20", quantity = 3)
            )
        )

        val stats = useCase()

        assertEquals(6, stats.totalPlays) // 2 + 1 + 3
    }

    @Test
    fun `calculates plays this month correctly`() = runTest {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))

        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "$currentMonth-05", quantity = 2),
                createPlay(2, "$currentMonth-15", quantity = 1),
                createPlay(3, "$lastMonth-20", quantity = 3)
            )
        )

        val stats = useCase()

        assertEquals(3, stats.playsThisMonth) // Only current month plays
    }

    @Test
    fun `calculates unplayed count correctly`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1"),
                createCollectionItem(2, "Game 2"),
                createCollectionItem(3, "Game 3"),
                createCollectionItem(4, "Game 4")
            )
        )

        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "2024-12-01", gameId = 1),
                createPlay(2, "2024-12-05", gameId = 2)
            )
        )

        val stats = useCase()

        assertEquals(2, stats.unplayedCount) // Games 3 and 4 are unplayed
    }

    @Test
    fun `calculates all stats together correctly`() = runTest {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Catan"),
                createCollectionItem(2, "Wingspan"),
                createCollectionItem(3, "Azul")
            )
        )

        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "$currentMonth-01", gameId = 1, quantity = 2),
                createPlay(2, "$currentMonth-05", gameId = 1, quantity = 1),
                createPlay(3, "2024-11-20", gameId = 2, quantity = 1)
            )
        )

        val stats = useCase()

        assertEquals(3, stats.gamesCount)
        assertEquals(4, stats.totalPlays) // 2 + 1 + 1
        assertEquals(3, stats.playsThisMonth) // 2 + 1
        assertEquals(1, stats.unplayedCount) // Azul is unplayed
    }

    private fun createCollectionItem(gameId: Int, name: String) = CollectionItem(
        gameId = gameId,
        subtype = GameSubtype.BOARDGAME,
        name = name,
        yearPublished = 2020,
        thumbnail = null
    )

    private fun createPlay(
        id: Int,
        date: String,
        gameId: Int = 1,
        quantity: Int = 1
    ) = Play(
        id = id,
        date = date,
        quantity = quantity,
        length = 60,
        incomplete = false,
        location = null,
        gameId = gameId,
        gameName = "Test Game",
        comments = null,
        players = emptyList()
    )
}
