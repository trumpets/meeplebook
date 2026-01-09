package app.meeplebook.feature.collection

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.feature.collection.domain.DomainCollectionSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for collection mapper functions.
 */
class CollectionMappersTest {

    private lateinit var fakeStringProvider: FakeStringProvider

    @Before
    fun setUp() {
        fakeStringProvider = FakeStringProvider()
        fakeStringProvider.setString(R.string.collection_plays_count, "%d plays")
        fakeStringProvider.setString(R.string.collection_player_count, "%d-%dp")
        fakeStringProvider.setString(R.string.collection_play_time, "%d-%dm")
    }

    // toCollectionGameItem tests

    @Test
    fun `toCollectionGameItem maps complete game data correctly`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 100,
            name = "Azul",
            yearPublished = 2017,
            thumbnailUrl = "https://example.com/azul.jpg",
            playCount = 42,
            minPlayers = 2,
            maxPlayers = 4,
            minPlayTimeMinutes = 30,
            maxPlayTimeMinutes = 45
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(100L, result.gameId)
        assertEquals("Azul", result.name)
        assertEquals(2017, result.yearPublished)
        assertEquals("https://example.com/azul.jpg", result.thumbnailUrl)
        assertEquals("42 plays", result.playsSubtitle)
        assertEquals("2-4p", result.playersSubtitle)
        assertEquals("30-45m", result.playTimeSubtitle)
        assertFalse(result.isNew)
    }

    @Test
    fun `toCollectionGameItem maps game with zero plays as new`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 200,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnailUrl = null,
            playCount = 0,
            minPlayers = 1,
            maxPlayers = 5,
            minPlayTimeMinutes = 40,
            maxPlayTimeMinutes = 70
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(200L, result.gameId)
        assertEquals("Wingspan", result.name)
        assertEquals("0 plays", result.playsSubtitle)
        assertTrue(result.isNew)
    }

    @Test
    fun `toCollectionGameItem handles null yearPublished`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 300,
            name = "Mystery Game",
            yearPublished = null,
            thumbnailUrl = null,
            playCount = 5,
            minPlayers = 2,
            maxPlayers = 6,
            minPlayTimeMinutes = 60,
            maxPlayTimeMinutes = 90
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(300L, result.gameId)
        assertEquals("Mystery Game", result.name)
        assertEquals(null, result.yearPublished)
        assertEquals("5 plays", result.playsSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles both player counts null`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 400,
            name = "Solo Adventure",
            yearPublished = 2020,
            thumbnailUrl = "https://example.com/solo.jpg",
            playCount = 10,
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = 15,
            maxPlayTimeMinutes = 30
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(400L, result.gameId)
        assertEquals("Solo Adventure", result.name)
        assertEquals("", result.playersSubtitle)
        assertEquals("15-30m", result.playTimeSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles only minPlayers available`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 500,
            name = "Party Game",
            yearPublished = 2018,
            thumbnailUrl = null,
            playCount = 3,
            minPlayers = 4,
            maxPlayers = null,
            minPlayTimeMinutes = 20,
            maxPlayTimeMinutes = 40
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(500L, result.gameId)
        assertEquals("Party Game", result.name)
        assertEquals("4-4p", result.playersSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles only maxPlayers available`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 600,
            name = "Strategy Game",
            yearPublished = 2021,
            thumbnailUrl = "https://example.com/strategy.jpg",
            playCount = 7,
            minPlayers = null,
            maxPlayers = 6,
            minPlayTimeMinutes = 60,
            maxPlayTimeMinutes = 120
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(600L, result.gameId)
        assertEquals("Strategy Game", result.name)
        assertEquals("6-6p", result.playersSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles both play times null`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 700,
            name = "Unknown Duration",
            yearPublished = 2022,
            thumbnailUrl = null,
            playCount = 2,
            minPlayers = 2,
            maxPlayers = 4,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(700L, result.gameId)
        assertEquals("Unknown Duration", result.name)
        assertEquals("2-4p", result.playersSubtitle)
        assertEquals("", result.playTimeSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles only minPlayTimeMinutes available`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 800,
            name = "Quick Game",
            yearPublished = 2023,
            thumbnailUrl = "https://example.com/quick.jpg",
            playCount = 15,
            minPlayers = 1,
            maxPlayers = 2,
            minPlayTimeMinutes = 15,
            maxPlayTimeMinutes = null
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(800L, result.gameId)
        assertEquals("Quick Game", result.name)
        assertEquals("15-15m", result.playTimeSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles only maxPlayTimeMinutes available`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 900,
            name = "Long Game",
            yearPublished = 2024,
            thumbnailUrl = null,
            playCount = 1,
            minPlayers = 3,
            maxPlayers = 5,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = 180
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(900L, result.gameId)
        assertEquals("Long Game", result.name)
        assertEquals("180-180m", result.playTimeSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles single player game`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 1000,
            name = "Solo Card Game",
            yearPublished = 2020,
            thumbnailUrl = "https://example.com/card.jpg",
            playCount = 20,
            minPlayers = 1,
            maxPlayers = 1,
            minPlayTimeMinutes = 10,
            maxPlayTimeMinutes = 15
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(1000L, result.gameId)
        assertEquals("Solo Card Game", result.name)
        assertEquals("1-1p", result.playersSubtitle)
        assertEquals("10-15m", result.playTimeSubtitle)
        assertEquals("20 plays", result.playsSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles large player count game`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 1100,
            name = "Party Game Extreme",
            yearPublished = 2015,
            thumbnailUrl = null,
            playCount = 8,
            minPlayers = 5,
            maxPlayers = 20,
            minPlayTimeMinutes = 30,
            maxPlayTimeMinutes = 60
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(1100L, result.gameId)
        assertEquals("Party Game Extreme", result.name)
        assertEquals("5-20p", result.playersSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles very short play time`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 1200,
            name = "Micro Game",
            yearPublished = 2019,
            thumbnailUrl = "https://example.com/micro.jpg",
            playCount = 50,
            minPlayers = 2,
            maxPlayers = 2,
            minPlayTimeMinutes = 5,
            maxPlayTimeMinutes = 10
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(1200L, result.gameId)
        assertEquals("Micro Game", result.name)
        assertEquals("5-10m", result.playTimeSubtitle)
    }

    @Test
    fun `toCollectionGameItem handles very long play time`() {
        // Given
        val domain = DomainCollectionItem(
            gameId = 1300,
            name = "Epic Campaign",
            yearPublished = 2016,
            thumbnailUrl = null,
            playCount = 2,
            minPlayers = 1,
            maxPlayers = 4,
            minPlayTimeMinutes = 240,
            maxPlayTimeMinutes = 480
        )

        // When
        val result = domain.toCollectionGameItem(fakeStringProvider)

        // Then
        assertEquals(1300L, result.gameId)
        assertEquals("Epic Campaign", result.name)
        assertEquals("240-480m", result.playTimeSubtitle)
    }

    // toCollectionSection tests

    @Test
    fun `toCollectionSection maps section with multiple items correctly`() {
        // Given
        val items = listOf(
            DomainCollectionItem(
                gameId = 100,
                name = "Azul",
                yearPublished = 2017,
                thumbnailUrl = "https://example.com/azul.jpg",
                playCount = 10,
                minPlayers = 2,
                maxPlayers = 4,
                minPlayTimeMinutes = 30,
                maxPlayTimeMinutes = 45
            ),
            DomainCollectionItem(
                gameId = 200,
                name = "Ark Nova",
                yearPublished = 2021,
                thumbnailUrl = null,
                playCount = 5,
                minPlayers = 1,
                maxPlayers = 4,
                minPlayTimeMinutes = 90,
                maxPlayTimeMinutes = 150
            )
        )
        val domainSection = DomainCollectionSection(
            key = 'A',
            items = items
        )

        // When
        val result = domainSection.toCollectionSection(fakeStringProvider)

        // Then
        assertEquals('A', result.key)
        assertEquals(2, result.games.size)
        assertEquals("Azul", result.games[0].name)
        assertEquals("Ark Nova", result.games[1].name)
        assertEquals("10 plays", result.games[0].playsSubtitle)
        assertEquals("5 plays", result.games[1].playsSubtitle)
    }

    @Test
    fun `toCollectionSection maps section with single item correctly`() {
        // Given
        val items = listOf(
            DomainCollectionItem(
                gameId = 300,
                name = "Wingspan",
                yearPublished = 2019,
                thumbnailUrl = "https://example.com/wingspan.jpg",
                playCount = 15,
                minPlayers = 1,
                maxPlayers = 5,
                minPlayTimeMinutes = 40,
                maxPlayTimeMinutes = 70
            )
        )
        val domainSection = DomainCollectionSection(
            key = 'W',
            items = items
        )

        // When
        val result = domainSection.toCollectionSection(fakeStringProvider)

        // Then
        assertEquals('W', result.key)
        assertEquals(1, result.games.size)
        assertEquals("Wingspan", result.games[0].name)
        assertEquals("15 plays", result.games[0].playsSubtitle)
    }

    @Test
    fun `toCollectionSection maps empty section correctly`() {
        // Given
        val domainSection = DomainCollectionSection(
            key = 'Z',
            items = emptyList()
        )

        // When
        val result = domainSection.toCollectionSection(fakeStringProvider)

        // Then
        assertEquals('Z', result.key)
        assertEquals(0, result.games.size)
        assertTrue(result.games.isEmpty())
    }

    @Test
    fun `toCollectionSection maps section with non-alphabetic key correctly`() {
        // Given
        val items = listOf(
            DomainCollectionItem(
                gameId = 400,
                name = "7 Wonders",
                yearPublished = 2010,
                thumbnailUrl = "https://example.com/7wonders.jpg",
                playCount = 25,
                minPlayers = 2,
                maxPlayers = 7,
                minPlayTimeMinutes = 30,
                maxPlayTimeMinutes = 30
            )
        )
        val domainSection = DomainCollectionSection(
            key = '#',
            items = items
        )

        // When
        val result = domainSection.toCollectionSection(fakeStringProvider)

        // Then
        assertEquals('#', result.key)
        assertEquals(1, result.games.size)
        assertEquals("7 Wonders", result.games[0].name)
    }
}
