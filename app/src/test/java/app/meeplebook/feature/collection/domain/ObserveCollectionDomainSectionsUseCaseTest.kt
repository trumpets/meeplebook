package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.model.QuickFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObserveCollectionDomainSectionsUseCase].
 */
class ObserveCollectionDomainSectionsUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var observeCollectionUseCase: ObserveCollectionUseCase
    private lateinit var buildSectionsUseCase: BuildCollectionSectionsUseCase
    private lateinit var useCase: ObserveCollectionDomainSectionsUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        observeCollectionUseCase = ObserveCollectionUseCase(fakeCollectionRepository)
        buildSectionsUseCase = BuildCollectionSectionsUseCase()
        useCase = ObserveCollectionDomainSectionsUseCase(
            observeCollection = observeCollectionUseCase,
            sectionBuilder = buildSectionsUseCase
        )
    }

    @Test
    fun `invoke returns sections organized alphabetically`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham"),
            createCollectionItem(gameId = 3, name = "Catan"),
            createCollectionItem(gameId = 4, name = "Ark Nova"),
            createCollectionItem(gameId = 5, name = "Cascadia")
        )
        fakeCollectionRepository.setCollection(items)

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then
        assertEquals(3, sections.size)

        // Section A
        assertEquals('A', sections[0].key)
        assertEquals(2, sections[0].items.size)
        assertEquals("Ark Nova", sections[0].items[0].name)
        assertEquals("Azul", sections[0].items[1].name)

        // Section B
        assertEquals('B', sections[1].key)
        assertEquals(1, sections[1].items.size)
        assertEquals("Brass: Birmingham", sections[1].items[0].name)

        // Section C
        assertEquals('C', sections[2].key)
        assertEquals(2, sections[2].items.size)
        assertEquals("Cascadia", sections[2].items[0].name)
        assertEquals("Catan", sections[2].items[1].name)
    }

    @Test
    fun `invoke returns empty list when collection is empty`() = runTest {
        // Given - empty collection
        fakeCollectionRepository.setCollection(emptyList())

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `invoke groups games with non-alphabetic names into # section`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "7 Wonders"),
            createCollectionItem(gameId = 3, name = "1775: Rebellion"),
            createCollectionItem(gameId = 4, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then
        assertEquals(3, sections.size)

        // Section # should come first
        assertEquals('#', sections[0].key)
        assertEquals(2, sections[0].items.size)
        assertEquals("1775: Rebellion", sections[0].items[0].name)
        assertEquals("7 Wonders", sections[0].items[1].name)

        // Then alphabetic sections
        assertEquals('A', sections[1].key)
        assertEquals('B', sections[2].key)
    }

    @Test
    fun `invoke handles mixed case game names correctly`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "azul"),
            createCollectionItem(gameId = 2, name = "BRASS"),
            createCollectionItem(gameId = 3, name = "Catan")
        )
        fakeCollectionRepository.setCollection(items)

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then - all should be grouped under their uppercase letter
        assertEquals(3, sections.size)
        assertEquals('A', sections[0].key)
        assertEquals('B', sections[1].key)
        assertEquals('C', sections[2].key)
    }

    @Test
    fun `invoke returns single section when all games start with same letter`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Ark Nova"),
            createCollectionItem(gameId = 3, name = "Agricola")
        )
        fakeCollectionRepository.setCollection(items)

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then
        assertEquals(1, sections.size)
        assertEquals('A', sections[0].key)
        assertEquals(3, sections[0].items.size)
    }

    @Test
    fun `invoke preserves game metadata in domain items`() = runTest {
        // Given
        val item = createCollectionItem(
            gameId = 123,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnail = "https://example.com/wingspan.jpg",
            numPlays = 15,
            minPlayers = 1,
            maxPlayers = 5,
            minPlayTimeMinutes = 40,
            maxPlayTimeMinutes = 70
        )
        fakeCollectionRepository.setCollection(listOf(item))

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then
        assertEquals(1, sections.size)
        val domainItem = sections[0].items[0]
        assertEquals(123L, domainItem.gameId)
        assertEquals("Wingspan", domainItem.name)
        assertEquals(2019, domainItem.yearPublished)
        assertEquals("https://example.com/wingspan.jpg", domainItem.thumbnailUrl)
        assertEquals(15, domainItem.playCount)
        assertEquals(1, domainItem.minPlayers)
        assertEquals(5, domainItem.maxPlayers)
        assertEquals(40, domainItem.minPlayTimeMinutes)
        assertEquals(70, domainItem.maxPlayTimeMinutes)
    }

    @Test
    fun `invoke updates when repository data changes`() = runTest {
        // Given - initial state
        val initialItems = listOf(
            createCollectionItem(gameId = 1, name = "Azul")
        )
        fakeCollectionRepository.setCollection(initialItems)

        val query = createQuery()

        // When - first observation
        val sections1 = useCase(query).first()

        // Then
        assertEquals(1, sections1.size)
        assertEquals('A', sections1[0].key)
        assertEquals(1, sections1[0].items.size)

        // When - data changes
        val updatedItems = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(updatedItems)
        val sections2 = useCase(query).first()

        // Then - sections are updated
        assertEquals(2, sections2.size)
        assertEquals('A', sections2[0].key)
        assertEquals('B', sections2[1].key)
    }

    @Test
    fun `invoke handles games with empty names`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = ""),
            createCollectionItem(gameId = 2, name = "Azul")
        )
        fakeCollectionRepository.setCollection(items)

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then - empty name should go to # section
        assertEquals(2, sections.size)
        assertEquals('#', sections[0].key)
        assertEquals(1, sections[0].items.size)
        assertEquals('A', sections[1].key)
    }

    @Test
    fun `invoke handles special characters at start of game names`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "!Bang"),
            createCollectionItem(gameId = 2, name = "@Home"),
            createCollectionItem(gameId = 3, name = "Azul"),
            createCollectionItem(gameId = 4, name = "#Money")
        )
        fakeCollectionRepository.setCollection(items)

        val query = createQuery()

        // When
        val sections = useCase(query).first()

        // Then - special characters should go to # section
        assertEquals(2, sections.size)
        assertEquals('#', sections[0].key)
        assertEquals(3, sections[0].items.size)
        assertEquals('A', sections[1].key)
        assertEquals(1, sections[1].items.size)
    }

    private fun createCollectionItem(
        gameId: Long,
        name: String,
        yearPublished: Int? = null,
        thumbnail: String? = null,
        numPlays: Int = 0,
        minPlayers: Int? = null,
        maxPlayers: Int? = null,
        minPlayTimeMinutes: Int? = null,
        maxPlayTimeMinutes: Int? = null
    ): CollectionItem {
        return CollectionItem(
            gameId = gameId,
            subtype = GameSubtype.BOARDGAME,
            name = name,
            yearPublished = yearPublished,
            thumbnail = thumbnail,
            lastModifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            minPlayTimeMinutes = minPlayTimeMinutes,
            maxPlayTimeMinutes = maxPlayTimeMinutes,
            numPlays = numPlays
        )
    }

    private fun createQuery(
        searchQuery: String = "",
        quickFilter: QuickFilter = QuickFilter.ALL,
        sort: CollectionSort = CollectionSort.ALPHABETICAL
    ): CollectionDataQuery {
        return CollectionDataQuery(
            searchQuery = searchQuery,
            quickFilter = quickFilter,
            sort = sort
        )
    }
}
