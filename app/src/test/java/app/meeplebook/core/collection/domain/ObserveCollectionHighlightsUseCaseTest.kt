package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionHighlights
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObserveCollectionHighlightsUseCase].
 */
class ObserveCollectionHighlightsUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var useCase: ObserveCollectionHighlightsUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        useCase = ObserveCollectionHighlightsUseCase(fakeCollectionRepository)
    }

    @Test
    fun `invoke returns highlights with both games present`() = runTest {
        // Given
        val recentlyAdded = CollectionItem(
            gameId = 1,
            subtype = GameSubtype.BOARDGAME,
            name = "Azul",
            yearPublished = 2017,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-10T10:00:00Z"),
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )
        val suggested = CollectionItem(
            gameId = 2,
            subtype = GameSubtype.BOARDGAME,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-01T10:00:00Z"),
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )

        fakeCollectionRepository.setMostRecentlyAdded(recentlyAdded)
        fakeCollectionRepository.setFirstUnplayed(suggested)

        // When
        val highlights = useCase().first()

        // Then
        assertEquals(
            CollectionHighlights(
                recentlyAdded = recentlyAdded,
                suggested = suggested
            ),
            highlights
        )
    }

    @Test
    fun `invoke returns null highlights when no data`() = runTest {
        // Given - no data set in repository

        // When
        val highlights = useCase().first()

        // Then
        assertNull(highlights.recentlyAdded)
        assertNull(highlights.suggested)
    }

    @Test
    fun `invoke returns only recently added when no unplayed games`() = runTest {
        // Given
        val recentlyAdded = CollectionItem(
            gameId = 1,
            subtype = GameSubtype.BOARDGAME,
            name = "Azul",
            yearPublished = 2017,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-10T10:00:00Z"),
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )

        fakeCollectionRepository.setMostRecentlyAdded(recentlyAdded)
        fakeCollectionRepository.setFirstUnplayed(null)

        // When
        val highlights = useCase().first()

        // Then
        assertEquals(recentlyAdded, highlights.recentlyAdded)
        assertNull(highlights.suggested)
    }

    @Test
    fun `invoke returns only suggested when no recently added`() = runTest {
        // Given
        val suggested = CollectionItem(
            gameId = 2,
            subtype = GameSubtype.BOARDGAME,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnail = null,
            lastModifiedDate = null,
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )

        fakeCollectionRepository.setMostRecentlyAdded(null)
        fakeCollectionRepository.setFirstUnplayed(suggested)

        // When
        val highlights = useCase().first()

        // Then
        assertNull(highlights.recentlyAdded)
        assertEquals(suggested, highlights.suggested)
    }

    @Test
    fun `invoke updates when repository data changes`() = runTest {
        // Given - initial state
        val item1 = CollectionItem(
            gameId = 1,
            subtype = GameSubtype.BOARDGAME,
            name = "Game 1",
            yearPublished = 2020,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-10T10:00:00Z"),
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )
        fakeCollectionRepository.setMostRecentlyAdded(item1)

        // When - first observation
        val highlights1 = useCase().first()

        // Then
        assertEquals(item1, highlights1.recentlyAdded)

        // When - data changes
        val item2 = CollectionItem(
            gameId = 2,
            subtype = GameSubtype.BOARDGAME,
            name = "Game 2",
            yearPublished = 2021,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-15T10:00:00Z"),
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )
        fakeCollectionRepository.setMostRecentlyAdded(item2)
        val highlights2 = useCase().first()

        // Then - highlights are updated
        assertEquals(item2, highlights2.recentlyAdded)
    }
}
