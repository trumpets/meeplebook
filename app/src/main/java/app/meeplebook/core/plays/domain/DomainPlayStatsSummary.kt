package app.meeplebook.core.plays.domain

/**
 * Domain model representing aggregated statistics about a user's play history.
 *
 * This summary provides key metrics and is computed from the complete play
 * history in the repository.
 *
 * @property uniqueGamesCount Total number of unique games that have been played.
 * @property totalPlays Total number of play records across all games.
 * @property playsThisYear Number of plays recorded in the current calendar year.
 * @property currentYear The current calendar year for which [playsThisYear] is calculated.
 */
data class DomainPlayStatsSummary (
    val uniqueGamesCount: Long ,
    val totalPlays: Long,
    val playsThisYear: Long,
    val currentYear: Int
)