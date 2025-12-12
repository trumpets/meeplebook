package app.meeplebook.feature.home.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetCollectionHighlightsUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: GetCollectionHighlightsUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        useCase = GetCollectionHighlightsUseCase(fakeCollectionRepository, fakePlaysRepository)
    }

    @Test
    fun `returns null for both when collection is empty`() = runTest {
        val (recentlyAdded, suggested) = useCase()

        assertNull(recentlyAdded)
        assertNull(suggested)
    }

    @Test
    fun `returns last game as recently added`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1"),
                createCollectionItem(2, "Game 2"),
                createCollectionItem(3, "Game 3")
            )
        )

        val (recentlyAdded, _) = useCase()

        assertNotNull(recentlyAdded)
        assertEquals("Game 3", recentlyAdded?.gameName)
        assertEquals("Recently Added", recentlyAdded?.subtitle)
    }

    @Test
    fun `returns unplayed game as suggested`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Played Game"),
                createCollectionItem(2, "Unplayed Game")
            )
        )

        fakePlaysRepository.setPlays(
            listOf(createPlay(1, "2024-12-05", gameId = 1))
        )

        val (_, suggested) = useCase()

        assertNotNull(suggested)
        assertEquals("Unplayed Game", suggested?.gameName)
        assertEquals("Try Tonight?", suggested?.subtitle)
    }

    @Test
    fun `returns null for suggested when all games are played`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1"),
                createCollectionItem(2, "Game 2")
            )
        )

        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "2024-12-05", gameId = 1),
                createPlay(2, "2024-12-06", gameId = 2)
            )
        )

        val (_, suggested) = useCase()

        assertNull(suggested)
    }

    @Test
    fun `returns first unplayed game when multiple unplayed games exist`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Played Game"),
                createCollectionItem(2, "First Unplayed"),
                createCollectionItem(3, "Second Unplayed")
            )
        )

        fakePlaysRepository.setPlays(
            listOf(createPlay(1, "2024-12-05", gameId = 1))
        )

        val (_, suggested) = useCase()

        assertNotNull(suggested)
        assertEquals("First Unplayed", suggested?.gameName)
    }

    @Test
    fun `preserves thumbnail URL in highlights`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(
                    id = 1,
                    name = "Game with Thumbnail",
                    thumbnail = "https://example.com/thumbnail.jpg"
                )
            )
        )

        val (recentlyAdded, suggested) = useCase()

        assertNotNull(recentlyAdded)
        assertEquals("https://example.com/thumbnail.jpg", recentlyAdded?.thumbnailUrl)
        assertNotNull(suggested)
        assertEquals("https://example.com/thumbnail.jpg", suggested?.thumbnailUrl)
    }

    @Test
    fun `works correctly with single game in collection`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(createCollectionItem(1, "Solo Game"))
        )

        val (recentlyAdded, suggested) = useCase()

        assertNotNull(recentlyAdded)
        assertEquals("Solo Game", recentlyAdded?.gameName)
        assertNotNull(suggested)
        assertEquals("Solo Game", suggested?.gameName)
    }

    @Test
    fun `returns game with most recent lastModified as recently added`() = runTest {
        val oldDate = java.time.LocalDateTime.of(2024, 1, 1, 10, 0)
        val middleDate = java.time.LocalDateTime.of(2024, 6, 15, 14, 30)
        val recentDate = java.time.LocalDateTime.of(2024, 12, 1, 8, 15)
        
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Old Game", lastModified = oldDate),
                createCollectionItem(2, "Recent Game", lastModified = recentDate),
                createCollectionItem(3, "Middle Game", lastModified = middleDate)
            )
        )

        val (recentlyAdded, _) = useCase()

        assertNotNull(recentlyAdded)
        assertEquals("Recent Game", recentlyAdded?.gameName)
        assertEquals("Recently Added", recentlyAdded?.subtitle)
    }

    @Test
    fun `returns null for recently added when no games have lastModified`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1", lastModified = null),
                createCollectionItem(2, "Game 2", lastModified = null)
            )
        )

        val (recentlyAdded, _) = useCase()

        assertNull(recentlyAdded)
    }

    private fun createCollectionItem(
        id: Int,
        name: String,
        thumbnail: String? = null,
        lastModified: java.time.LocalDateTime? = null
    ) = CollectionItem(
        gameId = id,
        subtype = GameSubtype.BOARDGAME,
        name = name,
        yearPublished = 2020,
        thumbnail = thumbnail,
        lastModified = lastModified
    )

    private fun createPlay(
        id: Int,
        date: String,
        gameId: Int = 1
    ) = Play(
        id = id,
        date = date,
        quantity = 1,
        length = 60,
        incomplete = false,
        location = null,
        gameId = gameId,
        gameName = "Test Game",
        comments = null,
        players = emptyList()
    )
}
