package app.meeplebook.feature.plays.domain

import app.meeplebook.core.plays.domain.DomainPlayStatsSummary

/**
 * Domain model containing all data needed to display the Plays screen.
 *
 * This combines organized play sections with aggregated statistics,
 * providing a complete view of the user's play history for presentation.
 *
 * @property sections List of play sections organized by month/year in reverse
 * chronological order (most recent first).
 * @property stats Aggregated statistics summary about the user's play history.
 */
data class DomainPlaysScreenData(
    val sections: List<DomainPlaySection>,
    val stats: DomainPlayStatsSummary
)