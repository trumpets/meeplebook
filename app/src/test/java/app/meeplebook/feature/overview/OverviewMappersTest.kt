package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainGameHighlight
import app.meeplebook.core.collection.domain.HighlightType
import app.meeplebook.core.plays.domain.DomainRecentPlay
import app.meeplebook.core.stats.domain.DomainOverviewStats
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.core.ui.asString
import app.meeplebook.core.ui.isNotEmpty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for overview mapper functions.
 */
class OverviewMappersTest {

    private lateinit var fakeStringProvider: FakeStringProvider

    @Before
    fun setUp() {
        fakeStringProvider = FakeStringProvider()
        fakeStringProvider.setString(R.string.game_highlight_recently_added, "Recently Added")
        fakeStringProvider.setString(R.string.game_highlight_try_tonight, "Try Tonight?")
    }

    @Test
    fun `toGameHighlight maps recently added game correctly`() {
        // Given
        val domain = DomainGameHighlight(
            id = 100,
            gameName = "Azul",
            thumbnailUrl = "https://example.com/azul.jpg",
            highlightType = HighlightType.RECENTLY_ADDED
        )

        // When
        val result = domain.toGameHighlight()

        // Then
        assertEquals(100L, result.id)
        assertEquals("Azul", result.gameName)
        assertEquals("https://example.com/azul.jpg", result.thumbnailUrl)
        assertEquals("Recently Added", result.subtitleUiText.asString(fakeStringProvider))
    }

    @Test
    fun `toGameHighlight maps suggested game correctly`() {
        // Given
        val domain = DomainGameHighlight(
            id = 200,
            gameName = "Wingspan",
            thumbnailUrl = null,
            highlightType = HighlightType.SUGGESTED
        )

        // When
        val result = domain.toGameHighlight()

        // Then
        assertEquals(200L, result.id)
        assertEquals("Wingspan", result.gameName)
        assertNull(result.thumbnailUrl)
        assertEquals("Try Tonight?", result.subtitleUiText.asString(fakeStringProvider))
    }

    @Test
    fun `toRecentPlay maps play with multiple players correctly`() {
        // Given

        val domain = DomainRecentPlay(
            id = 42,
            gameName = "Catan",
            thumbnailUrl = null,
            date = Instant.parse("2024-01-15T20:00:00Z"),
            playerCount = 4,
            playerNames = listOf("Alice", "Bob", "Charlie", "Diana")
        )

        // When
        val result = domain.toRecentPlay()

        // Then
        assertEquals(42L, result.id)
        assertEquals("Catan", result.gameName)
        assertNull(result.thumbnailUrl)
        assertEquals(4, result.playerCount)
        // Date and player names formatting is handled by utility functions
        // Just verify they're not empty
        assert(result.dateUiText.isNotEmpty())
        assert(result.playerNamesUiText.isNotEmpty())
    }

    @Test
    fun `toRecentPlay maps play with single player correctly`() {
        // Given
        val domain = DomainRecentPlay(
            id = 10,
            gameName = "Wingspan",
            thumbnailUrl = "https://example.com/wingspan.jpg",
            date = Instant.parse("2024-01-14T18:00:00Z"),
            playerCount = 1,
            playerNames = listOf("Solo Player")
        )

        // When
        val result = domain.toRecentPlay()

        // Then
        assertEquals(10L, result.id)
        assertEquals("Wingspan", result.gameName)
        assertEquals("https://example.com/wingspan.jpg", result.thumbnailUrl)
        assertEquals(1, result.playerCount)
        assert(result.dateUiText.isNotEmpty())
        assert(result.playerNamesUiText.isNotEmpty())
    }

    @Test
    fun `toOverviewStats maps all stats correctly`() {
        // Given
        val domain = DomainOverviewStats(
            gamesCount = 127,
            totalPlays = 342,
            playsInPeriod = 18,
            unplayedCount = 23
        )

        // When
        val result = domain.toOverviewStats()

        // Then
        assertEquals(127L, result.gamesCount)
        assertEquals(342L, result.totalPlays)
        assertEquals(18L, result.playsThisMonth)
        assertEquals(23L, result.unplayedCount)
    }

    @Test
    fun `toOverviewStats maps zero stats correctly`() {
        // Given
        val domain = DomainOverviewStats(
            gamesCount = 0,
            totalPlays = 0,
            playsInPeriod = 0,
            unplayedCount = 0
        )

        // When
        val result = domain.toOverviewStats()

        // Then
        assertEquals(0L, result.gamesCount)
        assertEquals(0L, result.totalPlays)
        assertEquals(0L, result.playsThisMonth)
        assertEquals(0L, result.unplayedCount)
    }
}
