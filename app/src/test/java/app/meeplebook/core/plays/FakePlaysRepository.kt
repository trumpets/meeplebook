package app.meeplebook.core.plays

import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.model.Player
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

    var syncPlaysResult: AppResult<Unit, PlayError> =
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

    override suspend fun syncPlays(username: String): AppResult<Unit, PlayError> {
        syncCallCount++
        lastSyncUsername = username

        return syncPlaysResult
    }

    override suspend fun createPlay(command: CreatePlayCommand) {
        val currentPlays = _plays.value.toMutableList()
        
        // Generate a new local ID
        val newLocalId = (currentPlays.maxOfOrNull { it.playId.localId } ?: 0L) + 1L
        
        // Create the play from the command
        val newPlay = Play(
            playId = PlayId.Local(newLocalId),
            date = command.date,
            quantity = command.quantity,
            length = command.length,
            incomplete = command.incomplete,
            location = command.location,
            gameId = command.gameId,
            gameName = command.gameName,
            comments = command.comments,
            players = command.players.mapIndexed { index, playerCommand ->
                Player(
                    id = newLocalId * 100 + index,
                    playId = newLocalId,
                    username = playerCommand.username,
                    userId = playerCommand.userId,
                    name = playerCommand.name,
                    startPosition = playerCommand.startPosition,
                    color = playerCommand.color,
                    score = playerCommand.score,
                    win = playerCommand.win
                )
            },
            syncStatus = PlaySyncStatus.PENDING
        )
        
        currentPlays.add(newPlay)
        _plays.value = currentPlays
        updateComputedValues(currentPlays)
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
