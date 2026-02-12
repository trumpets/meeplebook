package app.meeplebook.feature.plays

import app.meeplebook.R
import app.meeplebook.core.plays.domain.DomainPlayItem
import app.meeplebook.core.plays.domain.DomainPlayStatsSummary
import app.meeplebook.core.plays.domain.DomainPlayerItem
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.core.ui.asString
import app.meeplebook.core.ui.isNotEmpty
import app.meeplebook.feature.plays.domain.DomainPlaysSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

/**
 * Unit tests for plays mapper functions.
 */
class PlaysMappersTest {

    private lateinit var fakeStringProvider: FakeStringProvider

    @Before
    fun setUp() {
        fakeStringProvider = FakeStringProvider()
        fakeStringProvider.setPlural(R.plurals.play_players_formatted, -1, "%d players: %s")
        fakeStringProvider.setPlural(R.plurals.play_players_formatted, 1, "%d player: %s")
        fakeStringProvider.setString(R.string.play_player_name_only, "%1\$s")
        fakeStringProvider.setString(R.string.play_player_with_details, "%1\$s (%2\$s)")
        fakeStringProvider.setString(R.string.play_player_won, "won")
        fakeStringProvider.setString(R.string.play_player_score, "%1\$d")
        fakeStringProvider.setString(R.string.date_today, "Today")
        fakeStringProvider.setString(R.string.date_yesterday, "Yesterday")
        fakeStringProvider.setString(R.string.date_days_ago, "%d days ago")
        fakeStringProvider.setString(R.string.duration_format_h_min, "%dh %dmin")
        fakeStringProvider.setString(R.string.duration_format_min, "%dmin")
    }

    // toPlayItem tests

    @Test
    fun `toPlayItem maps complete play data correctly`() {
        // Given
        val domain = DomainPlayItem(
            playId = PlayId.Local(100),
            gameName = "Azul",
            thumbnailUrl = "https://example.com/azul.jpg",
            date = Instant.parse("2024-01-15T20:00:00Z"),
            durationMinutes = 45,
            players = listOf(
                DomainPlayerItem(name = "Alice", startPosition = "1", score = 80, win = true),
                DomainPlayerItem(name = "Bob", startPosition = "2", score = 65, win = false)
            ),
            location = "Home",
            comments = "Great game!",
            syncStatus = PlaySyncStatus.SYNCED
        )

        // When
        val result = domain.toPlayItem()

        // Then
        assertEquals(100L, result.playId.localId)
        assertEquals("Azul", result.gameName)
        assertEquals("https://example.com/azul.jpg", result.thumbnailUrl)
        assertEquals("45min", result.durationUiText.asString(fakeStringProvider))
        assertEquals("Home", result.location)
        assertEquals("Great game!", result.comments)
        assertEquals(PlaySyncStatus.SYNCED, result.syncStatus)
        // Player summary should be formatted with player details
        val playerSummary = result.playerSummaryUiText.asString(fakeStringProvider)
        assertEquals("2 players: Alice (won, 80), Bob (65)", playerSummary)
        assertTrue(result.dateUiText.isNotEmpty())
    }

    @Test
    fun `toPlayItem maps play without duration correctly`() {
        // Given
        val domain = DomainPlayItem(
            playId = PlayId.Local(200),
            gameName = "Wingspan",
            thumbnailUrl = null,
            date = Instant.parse("2024-01-14T18:00:00Z"),
            durationMinutes = null,
            players = listOf(
                DomainPlayerItem(name = "Charlie", startPosition = null, score = null, win = false)
            ),
            location = null,
            comments = null,
            syncStatus = PlaySyncStatus.PENDING
        )

        // When
        val result = domain.toPlayItem()

        // Then
        assertEquals(200L, result.playId.localId)
        assertEquals("Wingspan", result.gameName)
        assertEquals(null, result.thumbnailUrl)
        assertEquals("", result.durationUiText.asString(fakeStringProvider))
        assertEquals(null, result.location)
        assertEquals(null, result.comments)
        assertEquals(PlaySyncStatus.PENDING, result.syncStatus)
        assertTrue(result.dateUiText.isNotEmpty())
    }

    @Test
    fun `toPlayItem maps play with long duration correctly`() {
        // Given
        val domain = DomainPlayItem(
            playId = PlayId.Local(300),
            gameName = "Twilight Imperium",
            thumbnailUrl = "https://example.com/ti.jpg",
            date = Instant.parse("2024-01-13T10:00:00Z"),
            durationMinutes = 360,
            players = listOf(
                DomainPlayerItem(name = "Dave", startPosition = "1", score = 10, win = true)
            ),
            location = "Game Store",
            comments = "Epic game session!",
            syncStatus = PlaySyncStatus.SYNCED
        )

        // When
        val result = domain.toPlayItem()

        // Then
        assertEquals(300L, result.playId.localId)
        assertEquals("Twilight Imperium", result.gameName)
        assertEquals("6h 0min", result.durationUiText.asString(fakeStringProvider))
        assertEquals("Game Store", result.location)
        assertTrue(result.dateUiText.isNotEmpty())
    }

    @Test
    fun `toPlayItem maps play with empty players list correctly`() {
        // Given
        val domain = DomainPlayItem(
            playId = PlayId.Local(400),
            gameName = "Solo Game",
            thumbnailUrl = null,
            date = Instant.parse("2024-01-12T15:00:00Z"),
            durationMinutes = 30,
            players = emptyList(),
            location = null,
            comments = null,
            syncStatus = PlaySyncStatus.FAILED
        )

        // When
        val result = domain.toPlayItem()

        // Then
        assertEquals(400L, result.playId.localId)
        assertEquals("Solo Game", result.gameName)
        assertEquals(PlaySyncStatus.FAILED, result.syncStatus)
        // Player summary should handle empty list
        val playerSummary = result.playerSummaryUiText.asString(fakeStringProvider)
        assertEquals("", playerSummary)
        assertTrue(result.dateUiText.isNotEmpty())
    }

    // formatPlayerSummary tests

    @Test
    fun `formatPlayerSummary formats single player without details`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Alice", startPosition = null, score = null, win = false)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        assertEquals("1 player: Alice", formatted)
    }

    @Test
    fun `formatPlayerSummary formats single player with win`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Bob", startPosition = "1", score = null, win = true)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        assertEquals("1 player: Bob (won)", formatted)
    }

    @Test
    fun `formatPlayerSummary formats single player with score`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Charlie", startPosition = null, score = 75, win = false)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        assertEquals("1 player: Charlie (75)", formatted)
    }

    @Test
    fun `formatPlayerSummary formats single player with win and score`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Diana", startPosition = "1", score = 100, win = true)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        assertEquals("1 player: Diana (won, 100)", formatted)
    }

    @Test
    fun `formatPlayerSummary formats multiple players sorted by start position`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Charlie", startPosition = "3", score = 65, win = false),
            DomainPlayerItem(name = "Alice", startPosition = "1", score = 80, win = true),
            DomainPlayerItem(name = "Bob", startPosition = "2", score = 70, win = false)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        // Should be sorted by start position
        assertEquals("3 players: Alice (won, 80), Bob (70), Charlie (65)", formatted)
    }

    @Test
    fun `formatPlayerSummary handles players without start position`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Zoe", startPosition = null, score = 50, win = false),
            DomainPlayerItem(name = "Alice", startPosition = "1", score = 80, win = true)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        // Players with position should come first
        assertTrue(formatted.indexOf("Alice") < formatted.indexOf("Zoe"))
    }

    @Test
    fun `formatPlayerSummary handles players with non-numeric start position`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Charlie", startPosition = "3", score = 65, win = false),
            DomainPlayerItem(name = "Bob", startPosition = "unknown", score = 70, win = false),
            DomainPlayerItem(name = "Alice", startPosition = "1", score = 80, win = true)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        // Numeric positions should come first, then non-numeric
        assertTrue(formatted.indexOf("Alice") < formatted.indexOf("Charlie"))
        assertTrue(formatted.indexOf("Charlie") < formatted.indexOf("Bob"))
    }

    @Test
    fun `formatPlayerSummary formats empty players list`() {
        // Given
        val players = emptyList<DomainPlayerItem>()

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        assertEquals("", formatted)
    }

    @Test
    fun `formatPlayerSummary formats multiple players with mixed details`() {
        // Given
        val players = listOf(
            DomainPlayerItem(name = "Alice", startPosition = "1", score = 80, win = true),
            DomainPlayerItem(name = "Bob", startPosition = "2", score = null, win = false),
            DomainPlayerItem(name = "Charlie", startPosition = "3", score = 65, win = false),
            DomainPlayerItem(name = "Diana", startPosition = "4", score = null, win = false)
        )

        // When
        val result = formatPlayerSummary(players)

        // Then
        val formatted = result.asString(fakeStringProvider)
        assertEquals("4 players: Alice (won, 80), Bob, Charlie (65), Diana", formatted)
    }

    // toPlaysSection tests

    @Test
    fun `toPlaysSection maps section with multiple plays correctly`() {
        // Given
        val items = listOf(
            DomainPlayItem(
                playId = PlayId.Local(100),
                gameName = "Azul",
                thumbnailUrl = "https://example.com/azul.jpg",
                date = Instant.parse("2024-01-15T20:00:00Z"),
                durationMinutes = 45,
                players = listOf(
                    DomainPlayerItem(name = "Alice", startPosition = "1", score = 80, win = true)
                ),
                location = "Home",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            DomainPlayItem(
                playId = PlayId.Local(200),
                gameName = "Wingspan",
                thumbnailUrl = null,
                date = Instant.parse("2024-01-14T18:00:00Z"),
                durationMinutes = 60,
                players = listOf(
                    DomainPlayerItem(name = "Bob", startPosition = "1", score = 90, win = false)
                ),
                location = null,
                comments = "Fun game",
                syncStatus = PlaySyncStatus.PENDING
            )
        )
        val domainSection = DomainPlaysSection(
            monthYearDate = YearMonth.of(2024, 1),
            items = items
        )

        // When
        val result = domainSection.toPlaysSection()

        // Then
        assertEquals(YearMonth.of(2024, 1), result.monthYearDate)
        assertEquals(2, result.plays.size)
        assertEquals("Azul", result.plays[0].gameName)
        assertEquals("Wingspan", result.plays[1].gameName)
        assertEquals(100L, result.plays[0].playId.localId)
        assertEquals(200L, result.plays[1].playId.localId)
    }

    @Test
    fun `toPlaysSection maps section with single play correctly`() {
        // Given
        val items = listOf(
            DomainPlayItem(
                playId = PlayId.Local(300),
                gameName = "Catan",
                thumbnailUrl = "https://example.com/catan.jpg",
                date = Instant.parse("2024-02-10T15:00:00Z"),
                durationMinutes = 90,
                players = listOf(
                    DomainPlayerItem(name = "Charlie", startPosition = "1", score = 10, win = true)
                ),
                location = "Friend's house",
                comments = "Close game",
                syncStatus = PlaySyncStatus.SYNCED
            )
        )
        val domainSection = DomainPlaysSection(
            monthYearDate = YearMonth.of(2024, 2),
            items = items
        )

        // When
        val result = domainSection.toPlaysSection()

        // Then
        assertEquals(YearMonth.of(2024, 2), result.monthYearDate)
        assertEquals(1, result.plays.size)
        assertEquals("Catan", result.plays[0].gameName)
    }

    @Test
    fun `toPlaysSection maps empty section correctly`() {
        // Given
        val domainSection = DomainPlaysSection(
            monthYearDate = YearMonth.of(2024, 3),
            items = emptyList()
        )

        // When
        val result = domainSection.toPlaysSection()

        // Then
        assertEquals(YearMonth.of(2024, 3), result.monthYearDate)
        assertEquals(0, result.plays.size)
    }

    @Test
    fun `toPlaysSection preserves month and year correctly`() {
        // Given
        val items = listOf(
            DomainPlayItem(
                playId = PlayId.Local(400),
                gameName = "Test Game",
                thumbnailUrl = null,
                date = Instant.parse("2023-12-25T12:00:00Z"),
                durationMinutes = 30,
                players = emptyList(),
                location = null,
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            )
        )
        val domainSection = DomainPlaysSection(
            monthYearDate = YearMonth.of(2023, 12),
            items = items
        )

        // When
        val result = domainSection.toPlaysSection()

        // Then
        assertEquals(YearMonth.of(2023, 12), result.monthYearDate)
        assertEquals(2023, result.monthYearDate.year)
        assertEquals(12, result.monthYearDate.monthValue)
    }

    // toPlayStats tests

    @Test
    fun `toPlayStats maps all stats correctly`() {
        // Given
        val domain = DomainPlayStatsSummary(
            uniqueGamesCount = 42,
            totalPlays = 156,
            playsThisYear = 23,
            currentYear = 2024
        )

        // When
        val result = domain.toPlayStats()

        // Then
        assertEquals(42L, result.uniqueGamesCount)
        assertEquals(156L, result.totalPlays)
        assertEquals(23L, result.playsThisYear)
        assertEquals(2024, result.currentYear)
    }

    @Test
    fun `toPlayStats maps zero stats correctly`() {
        // Given
        val domain = DomainPlayStatsSummary(
            uniqueGamesCount = 0,
            totalPlays = 0,
            playsThisYear = 0,
            currentYear = 2024
        )

        // When
        val result = domain.toPlayStats()

        // Then
        assertEquals(0L, result.uniqueGamesCount)
        assertEquals(0L, result.totalPlays)
        assertEquals(0L, result.playsThisYear)
        assertEquals(2024, result.currentYear)
    }

    @Test
    fun `toPlayStats maps large numbers correctly`() {
        // Given
        val domain = DomainPlayStatsSummary(
            uniqueGamesCount = 999,
            totalPlays = 5000,
            playsThisYear = 250,
            currentYear = 2026
        )

        // When
        val result = domain.toPlayStats()

        // Then
        assertEquals(999L, result.uniqueGamesCount)
        assertEquals(5000L, result.totalPlays)
        assertEquals(250L, result.playsThisYear)
        assertEquals(2026, result.currentYear)
    }

    @Test
    fun `toPlayStats handles year in the past correctly`() {
        // Given
        val domain = DomainPlayStatsSummary(
            uniqueGamesCount = 10,
            totalPlays = 50,
            playsThisYear = 0,
            currentYear = 2020
        )

        // When
        val result = domain.toPlayStats()

        // Then
        assertEquals(10L, result.uniqueGamesCount)
        assertEquals(50L, result.totalPlays)
        assertEquals(0L, result.playsThisYear)
        assertEquals(2020, result.currentYear)
    }
}
