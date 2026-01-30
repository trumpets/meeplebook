package app.meeplebook.feature.plays.domain

import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.DomainPlayItem
import app.meeplebook.core.plays.domain.toDomainPlayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

/**
 * Unit tests for [BuildPlaysSectionsUseCase].
 */
class BuildPlaysSectionsUseCaseTest {

    private lateinit var useCase: BuildPlaysSectionsUseCase

    @Before
    fun setUp() {
        useCase = BuildPlaysSectionsUseCase()
    }

    @Test
    fun `invoke with empty list returns empty list`() {
        // Given
        val items = emptyList<DomainPlayItem>()

        // When
        val result = useCase(items)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke groups plays by month and year`() {
        // Given
        val items = listOf(
            createPlay(id = 1, gameName = "Catan", date = Instant.parse("2024-01-15T20:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Wingspan", date = Instant.parse("2024-01-20T18:00:00Z")).toDomainPlayItem(),
            createPlay(id = 3, gameName = "Azul", date = Instant.parse("2024-02-10T19:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(2, result.size)
        // February 2024 should come first (reverse chronological)
        assertEquals(YearMonth.of(2024, 2), result[0].monthYearDate)
        assertEquals(1, result[0].items.size)
        assertEquals("Azul", result[0].items[0].gameName)
        // January 2024 should come second
        assertEquals(YearMonth.of(2024, 1), result[1].monthYearDate)
        assertEquals(2, result[1].items.size)
        assertEquals("Catan", result[1].items[0].gameName)
        assertEquals("Wingspan", result[1].items[1].gameName)
    }

    @Test
    fun `invoke sorts sections in reverse chronological order`() {
        // Given - plays from different months, intentionally out of order
        val items = listOf(
            createPlay(id = 1, gameName = "Catan", date = Instant.parse("2024-01-15T20:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Wingspan", date = Instant.parse("2024-03-10T18:00:00Z")).toDomainPlayItem(),
            createPlay(id = 3, gameName = "Azul", date = Instant.parse("2024-02-20T19:00:00Z")).toDomainPlayItem(),
            createPlay(id = 4, gameName = "Splendor", date = Instant.parse("2023-12-25T17:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then - most recent month first
        assertEquals(4, result.size)
        assertEquals(YearMonth.of(2024, 3), result[0].monthYearDate) // March 2024
        assertEquals(YearMonth.of(2024, 2), result[1].monthYearDate) // February 2024
        assertEquals(YearMonth.of(2024, 1), result[2].monthYearDate) // January 2024
        assertEquals(YearMonth.of(2023, 12), result[3].monthYearDate) // December 2023
    }

    @Test
    fun `invoke preserves play order within each section`() {
        // Given - multiple plays in the same month with specific order
        val items = listOf(
            createPlay(id = 1, gameName = "First", date = Instant.parse("2024-01-15T20:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Second", date = Instant.parse("2024-01-20T18:00:00Z")).toDomainPlayItem(),
            createPlay(id = 3, gameName = "Third", date = Instant.parse("2024-01-25T19:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then - order within section should be preserved
        assertEquals(1, result.size)
        assertEquals(YearMonth.of(2024, 1), result[0].monthYearDate)
        assertEquals(3, result[0].items.size)
        assertEquals(1L, result[0].items[0].id)
        assertEquals("First", result[0].items[0].gameName)
        assertEquals(2L, result[0].items[1].id)
        assertEquals("Second", result[0].items[1].gameName)
        assertEquals(3L, result[0].items[2].id)
        assertEquals("Third", result[0].items[2].gameName)
    }

    @Test
    fun `invoke handles plays on same day at different times`() {
        // Given - multiple plays on the same day
        val items = listOf(
            createPlay(id = 1, gameName = "Morning", date = Instant.parse("2024-01-15T09:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Afternoon", date = Instant.parse("2024-01-15T14:00:00Z")).toDomainPlayItem(),
            createPlay(id = 3, gameName = "Evening", date = Instant.parse("2024-01-15T20:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then - all should be in same section
        assertEquals(1, result.size)
        assertEquals(YearMonth.of(2024, 1), result[0].monthYearDate)
        assertEquals(3, result[0].items.size)
    }

    @Test
    fun `invoke handles plays across year boundary`() {
        // Given - plays spanning New Year
        val items = listOf(
            createPlay(id = 1, gameName = "Old Year", date = Instant.parse("2023-12-31T23:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "New Year", date = Instant.parse("2024-01-01T01:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then - should be in separate sections
        assertEquals(2, result.size)
        assertEquals(YearMonth.of(2024, 1), result[0].monthYearDate)
        assertEquals("New Year", result[0].items[0].gameName)
        assertEquals(YearMonth.of(2023, 12), result[1].monthYearDate)
        assertEquals("Old Year", result[1].items[0].gameName)
    }

    @Test
    fun `invoke handles single play`() {
        // Given
        val items = listOf(
            createPlay(id = 1, gameName = "Solo", date = Instant.parse("2024-01-15T20:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(1, result.size)
        assertEquals(YearMonth.of(2024, 1), result[0].monthYearDate)
        assertEquals(1, result[0].items.size)
        assertEquals("Solo", result[0].items[0].gameName)
    }

    @Test
    fun `invoke handles all plays in same month`() {
        // Given - all plays in January 2024
        val items = listOf(
            createPlay(id = 1, gameName = "Game1", date = Instant.parse("2024-01-05T10:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Game2", date = Instant.parse("2024-01-15T14:00:00Z")).toDomainPlayItem(),
            createPlay(id = 3, gameName = "Game3", date = Instant.parse("2024-01-25T18:00:00Z")).toDomainPlayItem(),
            createPlay(id = 4, gameName = "Game4", date = Instant.parse("2024-01-31T22:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then
        assertEquals(1, result.size)
        assertEquals(YearMonth.of(2024, 1), result[0].monthYearDate)
        assertEquals(4, result[0].items.size)
    }

    @Test
    fun `invoke handles plays at exact same instant`() {
        // Given - two plays at the exact same instant
        val instant = Instant.parse("2024-01-15T20:00:00Z")
        val items = listOf(
            createPlay(id = 1, gameName = "First", date = instant).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Second", date = instant).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then - both in same section, order preserved
        assertEquals(1, result.size)
        assertEquals(YearMonth.of(2024, 1), result[0].monthYearDate)
        assertEquals(2, result[0].items.size)
        assertEquals(1L, result[0].items[0].id)
        assertEquals(2L, result[0].items[1].id)
    }

    @Test
    fun `invoke handles plays from far past and future`() {
        // Given - plays spanning decades
        val items = listOf(
            createPlay(id = 1, gameName = "Ancient", date = Instant.parse("2000-01-01T12:00:00Z")).toDomainPlayItem(),
            createPlay(id = 2, gameName = "Recent", date = Instant.parse("2024-06-15T14:00:00Z")).toDomainPlayItem(),
            createPlay(id = 3, gameName = "Future", date = Instant.parse("2030-12-31T23:00:00Z")).toDomainPlayItem()
        )

        // When
        val result = useCase(items)

        // Then - reverse chronological order
        assertEquals(3, result.size)
        assertEquals(YearMonth.of(2030, 12), result[0].monthYearDate)
        assertEquals("Future", result[0].items[0].gameName)
        assertEquals(YearMonth.of(2024, 6), result[1].monthYearDate)
        assertEquals("Recent", result[1].items[0].gameName)
        assertEquals(YearMonth.of(2000, 1), result[2].monthYearDate)
        assertEquals("Ancient", result[2].items[0].gameName)
    }

    @Test
    fun `invoke handles complete year of monthly plays`() {
        // Given - one play per month for a full year
        val items = (1..12).map { month ->
            createPlay(
                id = month.toLong(),
                gameName = "Game$month",
                date = Instant.parse("2024-%02d-15T12:00:00Z".format(month))
            ).toDomainPlayItem()
        }

        // When
        val result = useCase(items)

        // Then - 12 sections in reverse order
        assertEquals(12, result.size)
        // Verify December is first (most recent)
        assertEquals(YearMonth.of(2024, 12), result[0].monthYearDate)
        assertEquals("Game12", result[0].items[0].gameName)
        // Verify January is last (oldest)
        assertEquals(YearMonth.of(2024, 1), result[11].monthYearDate)
        assertEquals("Game1", result[11].items[0].gameName)
    }
}
