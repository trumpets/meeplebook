package app.meeplebook.feature.plays.domain

import app.meeplebook.core.plays.domain.DomainPlayItem
import java.time.YearMonth

/**
 * Represents a section of logged plays grouped by Month Year.
 *
 * Used to organize the plays display into date (month) sorted sections,
 * where each section contains items that were played in that specific month.
 *
 * @property monthYearDate The section identifier - a month and a year.
 * @property items The list of plays belonging to this section.
 */
data class DomainPlaysSection(
    val monthYearDate: YearMonth,
    val items: List<DomainPlayItem>
)