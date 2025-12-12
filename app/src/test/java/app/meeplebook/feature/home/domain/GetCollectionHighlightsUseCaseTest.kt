package app.meeplebook.feature.home.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class GetCollectionHighlightsUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var useCase: GetCollectionHighlightsUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        useCase = GetCollectionHighlightsUseCase(fakeCollectionRepository)
    }

    @Test
    fun `returns null for both when collection is empty`() = runTest {
        val (recentlyAdded, suggested) = useCase()

        assertNull(recentlyAdded)
        assertNull(suggested)
    }

    @Test
    fun `returns most recently modified game as recently added`() = runTest {
        val now = LocalDateTime.now()
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1", lastModified = now.minusDays(3)),
                createCollectionItem(2, "Game 2", lastModified = now.minusDays(1)),
                createCollectionItem(3, "Game 3", lastModified = now.minusDays(2))
            )
        )

        val (recentlyAdded, _) = useCase()

        assertNotNull(recentlyAdded)
        assertEquals("Game 2", recentlyAdded?.gameName)
        assertEquals(app.meeplebook.R.string.game_highlight_recently_added, recentlyAdded?.subtitleResId)
    }

    @Test
    fun `returns null for recently added when no items have lastModified`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1", lastModified = null),
                createCollectionItem(2, "Game 2", lastModified = null)
            )
        )

        val (recentlyAdded, _) = useCase()

        assertNull(recentlyAdded)
    }

    @Test
    fun `returns first unplayed game as suggested`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Unplayed Game 1"),
                createCollectionItem(2, "Unplayed Game 2")
            )
        )

        val (_, suggested) = useCase()

    @Test
    fun `returns null for suggested when collection is empty`() = runTest {
        val (_, suggested) = useCase()

        assertNull(suggested)
    }

    @Test
    fun `preserves thumbnail URL in highlights`() = runTest {
        val now = LocalDateTime.now()
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(
                    id = 1,
                    name = "Game with Thumbnail",
                    thumbnail = "https://example.com/thumbnail.jpg",
                    lastModified = now
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
        val now = LocalDateTime.now()
        fakeCollectionRepository.setCollection(
            listOf(createCollectionItem(1, "Solo Game", lastModified = now))
        )

        val (recentlyAdded, suggested) = useCase()

        assertNotNull(recentlyAdded)
        assertEquals("Solo Game", recentlyAdded?.gameName)
        assertNotNull(suggested)
        assertEquals("Solo Game", suggested?.gameName)
    }

    @Test
    fun `returns game with most recent lastModified date as recently added`() = runTest {
        val oldDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        val middleDate = LocalDateTime.of(2024, 6, 15, 14, 30)
        val recentDate = LocalDateTime.of(2024, 12, 1, 8, 15)
        
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
        assertEquals(app.meeplebook.R.string.game_highlight_recently_added, recentlyAdded?.subtitleResId)
    }

    private fun createCollectionItem(
        id: Int,
        name: String,
        thumbnail: String? = null,
        lastModified: LocalDateTime? = null
    ) = CollectionItem(
        gameId = id,
        subtype = GameSubtype.BOARDGAME,
        name = name,
        yearPublished = 2020,
        thumbnail = thumbnail,
        lastModified = lastModified
    )
}
