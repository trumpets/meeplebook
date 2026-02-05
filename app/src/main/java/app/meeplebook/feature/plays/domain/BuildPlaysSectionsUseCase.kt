package app.meeplebook.feature.plays.domain

import app.meeplebook.core.plays.domain.DomainPlayItem
import java.time.YearMonth
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Organizes play items into chronological sections by month/year for display.
 *
 * Groups plays by the month and year they were recorded, creating sections
 * for each unique month (e.g., January 2026, December 2025). Sections are
 * sorted in reverse chronological order (most recent first).
 */
class BuildPlaysSectionsUseCase @Inject constructor() {

    /**
     * Organizes the provided play items into chronological sections by month/year.
     *
     * @param items The play items to organize into sections.
     * @return A list of [DomainPlaysSection] sorted in reverse chronological order
     * (most recent month first). Preserves the existing order of items within each section.
     */
    operator fun invoke(
        items: List<DomainPlayItem>
    ): List<DomainPlaysSection> {
        return items
            .groupBy { it.sectionKey() }
            .toSortedMap(reverseOrder())
            .map { (monthYear, items) ->
                DomainPlaysSection(
                    monthYearDate = monthYear,
                    items = items.sortedByDescending { it.date }
                )
            }
    }

    /**
     * Extracts the year-month key from a play item's date for grouping.
     *
     * @return The [YearMonth] representing when this play occurred.
     */
    private fun DomainPlayItem.sectionKey(): YearMonth {
        return YearMonth.from(date.atZone(ZoneOffset.UTC))
    }
}