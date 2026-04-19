package app.meeplebook.core.plays

import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

    /**
     * When non-null, a successful [syncPlays] call will replace [_plays] with this list and
     * update all derived flows — mirroring the production behaviour where a successful sync
     * writes to the local data source and observers pick up the changes.
     *
     * Leave as `null` (the default) to keep existing tests unchanged: sync returns the
     * configured result without touching the observable play data.
     */
    var syncPlaysData: List<Play>? = null

    var syncCallCount = 0
        private set

    var lastSyncUsername: String? = null
        private set

    /** When non-null, [createPlay] throws this exception instead of succeeding. */
    var createPlayException: Throwable? = null

    var beforeCreatePlay: (suspend () -> Unit)? = null

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
        val result = syncPlaysResult
        if (result is AppResult.Success) {
            syncPlaysData?.let { data ->
                _plays.value = data
                updateComputedValues(data)
            }
        }
        return result
    }

    override suspend fun syncPendingPlays(): AppResult<Unit, PlayError> {
        return AppResult.Success(Unit)
    }

    override suspend fun createPlay(command: CreatePlayCommand) {
        beforeCreatePlay?.invoke()
        createPlayException?.let { throw it }
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

    override fun observeLocations(query: String): Flow<List<String>> {
        // Return distinct, case-preserving locations that start with the provided query (case-insensitive),
        // ordered alphabetically (case-insensitive), limited to 10 results.
        return _plays.map { plays ->
            plays
                .asSequence()
                .mapNotNull { it.location }
                .distinct()
                .filter { it.startsWith(query, ignoreCase = true) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .take(10)
                .toList()
        }
    }

    override fun observeRecentLocations(): Flow<List<String>> {
        // Return unique, non-null locations ordered by most recent play date (desc), limited to 10.
        return _plays.map { plays ->
            plays
                .asSequence()
                .filter { it.location != null }
                .sortedByDescending { it.date }
                .mapNotNull { it.location }
                .distinct()
                .take(10)
                .toList()
        }
    }

    override fun observePlayersByLocation(location: String): Flow<List<PlayerIdentity>> {
        // Return players who have played at the specified location,
        // grouped by name+username, ordered by play count (descending).
        return _plays.map { plays ->
            plays
                .filter { it.location == location }
                .flatMap { play -> play.players }
                .groupBy { player -> Pair(player.name, player.username) }
                .map { (key, players) ->
                    val (name, username) = key
                    val playCount = players.size
                    val userId = players.mapNotNull { it.userId }.maxOrNull() ?: 0L
                    Pair(PlayerIdentity(name, username, userId), playCount)
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }

    override fun observeColorsUsedForGame(gameId: Long): Flow<List<String>> {
        return _plays.map { plays ->
            plays
                .asSequence()
                .filter { it.gameId == gameId }
                .flatMap { it.players.asSequence() }
                .mapNotNull { it.color?.lowercase() }
                .distinct()
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .toList()
        }
    }

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

    override fun searchPlayersByName(query: String): Flow<List<PlayerIdentity>> =
        _plays.map { plays ->
            plays
                .flatMap { it.players }
                .filter { it.name.contains(query, ignoreCase = true) }
                .groupBy { it.name.lowercase() to it.username?.lowercase() }
                .map { (_, group) ->
                    group.maxBy { it.userId ?: Long.MIN_VALUE }
                }
                .sortedBy { it.name.lowercase() }
                .take(20)
                .map {
                    PlayerIdentity(
                        name = it.name,
                        username = it.username,
                        userId = it.userId
                    )
                }
        }

    override fun searchPlayersByUsername(query: String): Flow<List<PlayerIdentity>> =
        _plays.map { plays ->
            plays
                .flatMap { it.players }
                .filter {
                    !it.username.isNullOrBlank() &&
                            it.username.contains(query, ignoreCase = true)
                }
                .groupBy {
                    it.name.lowercase() to it.username!!.lowercase()
                }
                .map { (_, group) ->
                    group.maxBy { it.userId ?: Long.MIN_VALUE }
                }
                .sortedBy { it.username?.lowercase() }
                .take(20)
                .map {
                    PlayerIdentity(
                        name = it.name,
                        username = it.username,
                        userId = it.userId
                    )
                }
        }
}
