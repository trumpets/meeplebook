package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.model.QuickFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObserveCollectionUseCase].
 */
class ObserveCollectionUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var useCase: ObserveCollectionUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        useCase = ObserveCollectionUseCase(fakeCollectionRepository)
    }

    @Test
    fun `invoke forwards query parameter to repository`() = runTest {
        // Given
        val query = CollectionDataQuery(
            searchQuery = "Wingspan",
            quickFilter = QuickFilter.UNPLAYED,
            sort = CollectionSort.ALPHABETICAL
        )

        // When
        useCase(query).first()

        // Then
        assertEquals(query, fakeCollectionRepository.lastObserveCollectionQuery)
    }

    @Test
    fun `invoke with null query forwards null to repository`() = runTest {
        // When
        useCase(query = null).first()

        // Then
        assertNull(fakeCollectionRepository.lastObserveCollectionQuery)
    }

    @Test
    fun `invoke with default parameter forwards null to repository`() = runTest {
        // When
        useCase().first()

        // Then
        assertNull(fakeCollectionRepository.lastObserveCollectionQuery)
    }

    @Test
    fun `invoke maps CollectionItem to DomainCollectionItem correctly`() = runTest {
        // Given
        val collectionItem = CollectionItem(
            gameId = 123L,
            subtype = GameSubtype.BOARDGAME,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnail = "https://example.com/thumbnail.jpg",
            lastModifiedDate = Instant.parse("2024-01-10T10:00:00Z"),
            minPlayers = 1,
            maxPlayers = 5,
            minPlayTimeMinutes = 40,
            maxPlayTimeMinutes = 70,
            numPlays = 10
        )
        fakeCollectionRepository.setCollection(listOf(collectionItem))

        // When
        val result = useCase().first()

        // Then
        assertEquals(1, result.size)
        val domainItem = result[0]
        assertEquals(123L, domainItem.gameId)
        assertEquals("Wingspan", domainItem.name)
        assertEquals(2019, domainItem.yearPublished)
        assertEquals("https://example.com/thumbnail.jpg", domainItem.thumbnailUrl)
        assertEquals(10, domainItem.playCount)
        assertEquals(1, domainItem.minPlayers)
        assertEquals(5, domainItem.maxPlayers)
        assertEquals(40, domainItem.minPlayTimeMinutes)
        assertEquals(70, domainItem.maxPlayTimeMinutes)
    }

    @Test
    fun `invoke maps CollectionItem with null fields correctly`() = runTest {
        // Given
        val collectionItem = CollectionItem(
            gameId = 456L,
            subtype = GameSubtype.BOARDGAME_EXPANSION,
            name = "Wingspan: European Expansion",
            yearPublished = null,
            thumbnail = null,
            lastModifiedDate = null,
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )
        fakeCollectionRepository.setCollection(listOf(collectionItem))

        // When
        val result = useCase().first()

        // Then
        assertEquals(1, result.size)
        val domainItem = result[0]
        assertEquals(456L, domainItem.gameId)
        assertEquals("Wingspan: European Expansion", domainItem.name)
        assertNull(domainItem.yearPublished)
        assertNull(domainItem.thumbnailUrl)
        assertEquals(0, domainItem.playCount)
        assertNull(domainItem.minPlayers)
        assertNull(domainItem.maxPlayers)
        assertNull(domainItem.minPlayTimeMinutes)
        assertNull(domainItem.maxPlayTimeMinutes)
    }

    @Test
    fun `invoke returns empty list when collection is empty`() = runTest {
        // Given - empty collection

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke maps multiple collection items correctly`() = runTest {
        // Given
        val items = listOf(
            CollectionItem(
                gameId = 1L,
                subtype = GameSubtype.BOARDGAME,
                name = "Catan",
                yearPublished = 1995,
                thumbnail = null,
                lastModifiedDate = null,
                minPlayers = 3,
                maxPlayers = 4,
                minPlayTimeMinutes = 60,
                maxPlayTimeMinutes = 120,
                numPlays = 5
            ),
            CollectionItem(
                gameId = 2L,
                subtype = GameSubtype.BOARDGAME,
                name = "Azul",
                yearPublished = 2017,
                thumbnail = null,
                lastModifiedDate = null,
                minPlayers = 2,
                maxPlayers = 4,
                minPlayTimeMinutes = 30,
                maxPlayTimeMinutes = 45,
                numPlays = 3
            ),
            CollectionItem(
                gameId = 3L,
                subtype = GameSubtype.BOARDGAME,
                name = "Wingspan",
                yearPublished = 2019,
                thumbnail = null,
                lastModifiedDate = null,
                minPlayers = 1,
                maxPlayers = 5,
                minPlayTimeMinutes = 40,
                maxPlayTimeMinutes = 70,
                numPlays = 8
            )
        )
        fakeCollectionRepository.setCollection(items)

        // When
        val result = useCase().first()

        // Then
        assertEquals(3, result.size)
        assertEquals("Catan", result[0].name)
        assertEquals(5, result[0].playCount)
        assertEquals("Azul", result[1].name)
        assertEquals(3, result[1].playCount)
        assertEquals("Wingspan", result[2].name)
        assertEquals(8, result[2].playCount)
    }

    @Test
    fun `invoke updates when repository data changes`() = runTest {
        // Given - initial state
        val initialItems = listOf(
            CollectionItem(
                gameId = 1L,
                subtype = GameSubtype.BOARDGAME,
                name = "Catan",
                yearPublished = 1995,
                thumbnail = null,
                lastModifiedDate = null,
                minPlayers = 3,
                maxPlayers = 4,
                minPlayTimeMinutes = 60,
                maxPlayTimeMinutes = 120,
                numPlays = 5
            )
        )
        fakeCollectionRepository.setCollection(initialItems)

        // When - first observation
        val result1 = useCase().first()

        // Then
        assertEquals(1, result1.size)
        assertEquals("Catan", result1[0].name)

        // When - data changes
        val updatedItems = listOf(
            CollectionItem(
                gameId = 2L,
                subtype = GameSubtype.BOARDGAME,
                name = "Wingspan",
                yearPublished = 2019,
                thumbnail = null,
                lastModifiedDate = null,
                minPlayers = 1,
                maxPlayers = 5,
                minPlayTimeMinutes = 40,
                maxPlayTimeMinutes = 70,
                numPlays = 10
            ),
            CollectionItem(
                gameId = 1L,
                subtype = GameSubtype.BOARDGAME,
                name = "Catan",
                yearPublished = 1995,
                thumbnail = null,
                lastModifiedDate = null,
                minPlayers = 3,
                maxPlayers = 4,
                minPlayTimeMinutes = 60,
                maxPlayTimeMinutes = 120,
                numPlays = 5
            )
        )
        fakeCollectionRepository.setCollection(updatedItems)
        val result2 = useCase().first()

        // Then - collection is updated
        assertEquals(2, result2.size)
        assertEquals("Wingspan", result2[0].name)
        assertEquals("Catan", result2[1].name)
    }

    @Test
    fun `invoke with different queries forwards correct query each time`() = runTest {
        // Given
        val query1 = CollectionDataQuery(
            searchQuery = "Wingspan",
            quickFilter = QuickFilter.ALL,
            sort = CollectionSort.ALPHABETICAL
        )
        val query2 = CollectionDataQuery(
            searchQuery = "Catan",
            quickFilter = QuickFilter.UNPLAYED,
            sort = CollectionSort.MOST_PLAYED
        )

        // When
        useCase(query1).first()

        // Then
        assertEquals(query1, fakeCollectionRepository.lastObserveCollectionQuery)

        // When
        useCase(query2).first()

        // Then
        assertEquals(query2, fakeCollectionRepository.lastObserveCollectionQuery)
    }
}
