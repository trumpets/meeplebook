package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.DomainCollectionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BuildCollectionSectionsUseCase].
 */
class BuildCollectionSectionsUseCaseTest {

    private lateinit var useCase: BuildCollectionSectionsUseCase

    @Before
    fun setUp() {
        useCase = BuildCollectionSectionsUseCase()
    }

    @Test
    fun `invoke with empty list returns empty list`() {
        // Given
        val items = emptyList<DomainCollectionItem>()

        // When
        val result = useCase(items)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke groups games by first letter`() {
        // Given
        val items = listOf(
            createItem(name = "Azul"),
            createItem(name = "Ark Nova"),
            createItem(name = "Brass Birmingham"),
            createItem(name = "Wingspan")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(3, result.size)
        assertEquals('A', result[0].key)
        assertEquals(2, result[0].items.size)
        assertEquals('B', result[1].key)
        assertEquals(1, result[1].items.size)
        assertEquals('W', result[2].key)
        assertEquals(1, result[2].items.size)
    }

    @Test
    fun `invoke converts lowercase to uppercase for sectioning`() {
        // Given
        val items = listOf(
            createItem(name = "azul"),
            createItem(name = "Ark Nova")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(1, result.size)
        assertEquals('A', result[0].key)
        assertEquals(2, result[0].items.size)
    }

    @Test
    fun `invoke groups games starting with numbers into hash section`() {
        // Given
        val items = listOf(
            createItem(name = "7 Wonders"),
            createItem(name = "1989: Dawn of Freedom"),
            createItem(name = "Azul")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(2, result.size)
        assertEquals('#', result[0].key)
        assertEquals(2, result[0].items.size)
        assertEquals('A', result[1].key)
        assertEquals(1, result[1].items.size)
    }

    @Test
    fun `invoke groups games starting with special characters into hash section`() {
        // Given
        val items = listOf(
            createItem(name = "?Mystery Game"),
            createItem(name = "@Home"),
            createItem(name = "!Bang!"),
            createItem(name = "Azul")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(2, result.size)
        assertEquals('#', result[0].key)
        assertEquals(3, result[0].items.size)
        assertEquals('A', result[1].key)
        assertEquals(1, result[1].items.size)
    }

    @Test
    fun `invoke handles empty name by putting in hash section`() {
        // Given
        val items = listOf(
            createItem(name = ""),
            createItem(name = "Azul")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(2, result.size)
        assertEquals('#', result[0].key)
        assertEquals(1, result[0].items.size)
        assertEquals('A', result[1].key)
        assertEquals(1, result[1].items.size)
    }

    @Test
    fun `invoke sorts sections with hash first then alphabetically`() {
        // Given
        val items = listOf(
            createItem(name = "Wingspan"),
            createItem(name = "Azul"),
            createItem(name = "7 Wonders"),
            createItem(name = "Brass Birmingham"),
            createItem(name = "Catan")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(5, result.size)
        assertEquals('#', result[0].key)
        assertEquals('A', result[1].key)
        assertEquals('B', result[2].key)
        assertEquals('C', result[3].key)
        assertEquals('W', result[4].key)
    }

    @Test
    fun `invoke preserves game order within each section`() {
        // Given
        val items = listOf(
            createItem(gameId = 1, name = "Azul"),
            createItem(gameId = 2, name = "Ark Nova"),
            createItem(gameId = 3, name = "Agricola"),
            createItem(gameId = 4, name = "Brass Birmingham")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(2, result.size)
        // Check A section
        assertEquals('A', result[0].key)
        assertEquals(3, result[0].items.size)
        assertEquals(1L, result[0].items[0].gameId)
        assertEquals(2L, result[0].items[1].gameId)
        assertEquals(3L, result[0].items[2].gameId)
        // Check B section
        assertEquals('B', result[1].key)
        assertEquals(1, result[1].items.size)
        assertEquals(4L, result[1].items[0].gameId)
    }

    @Test
    fun `invoke handles unicode and accented characters`() {
        // Given
        val items = listOf(
            createItem(name = "Über"),
            createItem(name = "Çağ"),
            createItem(name = "Azul")
        )

        // When
        val result = useCase(items)

        // Then
        // Unicode characters that aren't A-Z go to hash section
        assertEquals(2, result.size)
        assertEquals('#', result[0].key)
        assertEquals(2, result[0].items.size)
        assertEquals('A', result[1].key)
        assertEquals(1, result[1].items.size)
    }

    @Test
    fun `invoke handles only hash section games`() {
        // Given
        val items = listOf(
            createItem(name = "7 Wonders"),
            createItem(name = "1989: Dawn of Freedom"),
            createItem(name = "51st State")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(1, result.size)
        assertEquals('#', result[0].key)
        assertEquals(3, result[0].items.size)
    }

    @Test
    fun `invoke handles all games in same letter section`() {
        // Given
        val items = listOf(
            createItem(name = "Azul"),
            createItem(name = "Ark Nova"),
            createItem(name = "Agricola"),
            createItem(name = "A Feast for Odin")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(1, result.size)
        assertEquals('A', result[0].key)
        assertEquals(4, result[0].items.size)
    }

    @Test
    fun `invoke handles whitespace at start of name`() {
        // Given
        val items = listOf(
            createItem(name = " Azul"),
            createItem(name = "  Brass Birmingham")
        )

        // When
        val result = useCase(items)

        // Then
        // Whitespace is not A-Z, so goes to hash
        assertEquals(1, result.size)
        assertEquals('#', result[0].key)
        assertEquals(2, result[0].items.size)
    }

    @Test
    fun `invoke handles mix of all edge cases`() {
        // Given
        val items = listOf(
            createItem(gameId = 1, name = "Wingspan"),
            createItem(gameId = 2, name = "7 Wonders"),
            createItem(gameId = 3, name = "azul"),
            createItem(gameId = 4, name = "!Bang!"),
            createItem(gameId = 5, name = ""),
            createItem(gameId = 6, name = "Brass Birmingham"),
            createItem(gameId = 7, name = "Catan"),
            createItem(gameId = 8, name = "Über")
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(5, result.size)
        // Hash section first
        assertEquals('#', result[0].key)
        assertEquals(4, result[0].items.size) // 7 Wonders, !Bang!, empty, Über
        // Then A section
        assertEquals('A', result[1].key)
        assertEquals(1, result[1].items.size) // azul
        // Then B section
        assertEquals('B', result[2].key)
        assertEquals(1, result[2].items.size) // Brass Birmingham
        // Then C section
        assertEquals('C', result[3].key)
        assertEquals(1, result[3].items.size) // Catan
        // Then W section
        assertEquals('W', result[4].key)
        assertEquals(1, result[4].items.size) // Wingspan
    }

    // Helper function to create test items
    private fun createItem(
        gameId: Long = 1L,
        name: String,
        yearPublished: Int? = 2020,
        thumbnailUrl: String? = null,
        playCount: Int = 0,
        minPlayers: Int? = 1,
        maxPlayers: Int? = 4,
        minPlayTimeMinutes: Int? = 30,
        maxPlayTimeMinutes: Int? = 60
    ): DomainCollectionItem {
        return DomainCollectionItem(
            gameId = gameId,
            name = name,
            yearPublished = yearPublished,
            thumbnailUrl = thumbnailUrl,
            playCount = playCount,
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            minPlayTimeMinutes = minPlayTimeMinutes,
            maxPlayTimeMinutes = maxPlayTimeMinutes
        )
    }
}
