package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [PlaysRepository] for testing purposes.
 */
class FakePlaysRepository : PlaysRepository {

    private val plays = MutableStateFlow<List<Play>>(emptyList())

    /**
     * Configure this to control the result of [syncPlays] calls.
     */
    var syncPlaysResult: AppResult<List<Play>, PlayError> = 
        AppResult.Failure(PlayError.Unknown(IllegalStateException("FakePlaysRepository not configured")))

    /**
     * Tracks the number of times [syncPlays] was called.
     */
    var syncPlaysCallCount = 0
        private set

    /**
     * Stores the last username passed to [syncPlays].
     */
    var lastSyncUsername: String? = null
        private set

    override fun observePlays(): Flow<List<Play>> = plays

    override fun observePlaysForGame(gameId: Int): Flow<List<Play>> {
        // Not implemented for this fake
        throw NotImplementedError("observePlaysForGame not implemented in FakePlaysRepository")
    }

    override suspend fun getPlays(): List<Play> {
        return plays.value
    }

    override suspend fun getPlaysForGame(gameId: Int): List<Play> {
        return plays.value.filter { it.gameId == gameId }
    }

    override suspend fun syncPlays(username: String): AppResult<List<Play>, PlayError> {
        syncPlaysCallCount++
        lastSyncUsername = username

        when (val result = syncPlaysResult) {
            is AppResult.Success -> plays.value = result.data
            is AppResult.Failure -> { /* no-op */ }
        }

        return syncPlaysResult
    }

    override suspend fun clearPlays() {
        plays.value = emptyList()
    }

    override suspend fun getTotalPlaysCount(): Int {
        return plays.value.sumOf { it.quantity }
    }

    override suspend fun getPlaysCountForMonth(monthPrefix: String): Int {
        return plays.value
            .filter { it.date.startsWith(monthPrefix) }
            .sumOf { it.quantity }
    }

    override suspend fun getRecentPlays(limit: Int): List<Play> {
        return plays.value
            .sortedByDescending { it.date }
            .take(limit)
    }

    /**
     * Sets the plays directly for testing.
     */
    fun setPlays(items: List<Play>) {
        plays.value = items
    }
}
