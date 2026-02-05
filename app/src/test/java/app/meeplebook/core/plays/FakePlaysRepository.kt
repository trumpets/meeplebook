package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Fake implementation of [PlaysRepository] for testing purposes.
 */
class FakePlaysRepository : PlaysRepository {

    private val _plays = MutableStateFlow<List<Play>>(emptyList())
    private val _totalPlaysCount = MutableStateFlow(0L)
    private val _playsCountForPeriod = MutableStateFlow(0L)
    private val _recentPlays = MutableStateFlow<List<Play>>(emptyList())
    private val _uniqueGamesCount = MutableStateFlow(0L)

    var syncPlaysResult: AppResult<List<Play>, PlayError> =
        AppResult.Failure(PlayError.Unknown(IllegalStateException("FakePlaysRepository not configured")))

    var syncCallCount = 0
        private set

    var lastSyncUsername: String? = null
        private set

    var lastObservePlaysQuery: String? = null
        private set

    override fun observePlays(gameNameOrLocationQuery: String?): Flow<List<Play>> {
        lastObservePlaysQuery = gameNameOrLocationQuery
        return _plays
    }

    override fun observePlaysForGame(gameId: Long): Flow<List<Play>> {
        return MutableStateFlow(_plays.value.filter { it.gameId == gameId })
    }

    override suspend fun getPlays(): List<Play> = _plays.value

    override suspend fun getPlaysForGame(gameId: Long): List<Play> {
        return _plays.value.filter { it.gameId == gameId }
    }

    override suspend fun syncPlays(username: String): AppResult<List<Play>, PlayError> {
        syncCallCount++
        lastSyncUsername = username

        when (val result = syncPlaysResult) {
            is AppResult.Success -> {
                _plays.value = result.data
                updateComputedValues(result.data)
            }
            is AppResult.Failure -> { /* no-op */ }
        }

        return syncPlaysResult
    }

    override suspend fun clearPlays() {
        _plays.value = emptyList()
        _totalPlaysCount.value = 0L
        _playsCountForPeriod.value = 0L
        _recentPlays.value = emptyList()
        _uniqueGamesCount.value = 0L
    }

    override fun observeTotalPlaysCount(): Flow<Long> = _totalPlaysCount

    override fun observePlaysCountForPeriod(start: Instant, end: Instant): Flow<Long> = _playsCountForPeriod

    override fun observeRecentPlays(limit: Int): Flow<List<Play>> = _recentPlays

    override fun observeUniqueGamesCount(): Flow<Long> = _uniqueGamesCount

    /**
     * Sets the plays directly for testing purposes.
     */
    fun setPlays(plays: List<Play>) {
        _plays.value = plays
        updateComputedValues(plays)
    }

    /**
     * Sets the total plays count directly for testing purposes.
     */
    fun setTotalPlaysCount(count: Long) {
        _totalPlaysCount.value = count
    }

    /**
     * Sets the plays count for period directly for testing purposes.
     */
    fun setPlaysCountForPeriod(count: Long) {
        _playsCountForPeriod.value = count
    }

    /**
     * Sets the recent plays directly for testing purposes.
     */
    fun setRecentPlays(plays: List<Play>) {
        _recentPlays.value = plays
    }

    /**
     * Sets the unique games count directly for testing purposes.
     */
    fun setUniqueGamesCount(count: Long) {
        _uniqueGamesCount.value = count
    }

    private fun updateComputedValues(plays: List<Play>) {
        _totalPlaysCount.value = plays.sumOf { it.quantity.toLong() }
        _playsCountForPeriod.value = plays.size.toLong() // Simplified; in real case, filter by period
        _recentPlays.value = plays.sortedByDescending { it.date }.take(5)
        _uniqueGamesCount.value = plays.map { it.gameId }.distinct().count().toLong()
    }
}
