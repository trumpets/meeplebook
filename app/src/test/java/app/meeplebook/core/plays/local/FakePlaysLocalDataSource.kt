package app.meeplebook.core.plays.local

import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.plays.remote.dto.RemotePlayDto
import app.meeplebook.core.plays.remote.dto.RemotePlayerDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Fake implementation of [PlaysLocalDataSource] for testing.
 */
class FakePlaysLocalDataSource : PlaysLocalDataSource {

    private val playsFlow = MutableStateFlow<List<Play>>(emptyList())
    private val totalPlaysCount = MutableStateFlow(0L)
    private val playsCountForMonth = MutableStateFlow<Map<Pair<Instant, Instant>, Long>>(emptyMap())
    private val recentPlays = MutableStateFlow<Map<Int, List<Play>>>(emptyMap())
    private val uniqueGamesCount = MutableStateFlow(0L)

    override fun observePlays(): Flow<List<Play>> = playsFlow
    
    override fun observePlaysByGameNameOrLocation(gameNameOrLocationQuery: String): Flow<List<Play>> {
        return playsFlow.map { items ->
            items.filter { item ->
                item.gameName.contains(gameNameOrLocationQuery, ignoreCase = true)
                        || item.location?.contains(gameNameOrLocationQuery, ignoreCase = true) == true
            }
        }
    }

    override fun observePlaysForGame(gameId: Long): Flow<List<Play>> {
        return playsFlow.map { list -> list.filter { it.gameId == gameId } }
    }

    override suspend fun getPlays(): List<Play> = playsFlow.value
    override suspend fun getPlaysForGame(gameId: Long): List<Play> {
        return playsFlow.value.filter { it.gameId == gameId }
    }

    override suspend fun saveRemotePlays(remotePlays: List<RemotePlayDto>) {
        // Merge plays - replace existing by remoteId, add new ones
        val existingPlays = playsFlow.value.toMutableList()
        var localIdCounter = (existingPlays.maxOfOrNull { it.playId.localId } ?: 0L) + 1L

        remotePlays.forEach { newPlay ->
            val index = existingPlays.indexOfFirst { (it.playId as? PlayId.Remote)?.remoteId == newPlay.remoteId }
            if (index >= 0) {
                existingPlays[index] = newPlay.toPlay(existingPlays[index].playId.localId)
            } else {
                existingPlays.add(newPlay.toPlay(localIdCounter++))
            }
        }

        playsFlow.value = existingPlays
    }

    override suspend fun insertPlay(
        playEntity: PlayEntity,
        playerEntities: List<PlayerEntity>
    ) {
        val existingPlays = playsFlow.value.toMutableList()

        val localPlayId = (existingPlays.maxOfOrNull { it.playId.localId } ?: 0L) + 1L
        existingPlays += Play(
            playId = PlayId.Local(localPlayId),
            gameId = playEntity.gameId,
            gameName = playEntity.gameName,
            date = playEntity.date,
            location = playEntity.location,
            quantity = playEntity.quantity,
            length = playEntity.length,
            incomplete = playEntity.incomplete,
            comments = playEntity.comments,
            players = playerEntities.mapIndexed { index, player ->
                Player(
                    id = localPlayId * 100 + index,
                    playId = localPlayId,
                    name = player.name,
                    win = player.win,
                    score = player.score,
                    color = player.color,
                    startPosition = player.startPosition,
                    username = player.username,
                    userId = player.userId
                )
            },
            syncStatus = PlaySyncStatus.PENDING
        )

        playsFlow.value = existingPlays
    }

    override suspend fun clearPlays() {
        playsFlow.value = emptyList()
        totalPlaysCount.value = 0L
        playsCountForMonth.value = emptyMap()
        recentPlays.value = emptyMap()
        uniqueGamesCount.value = 0L
    }

    override suspend fun retainByRemoteIds(remoteIds: List<Long>) {
        val existingPlays = playsFlow.value.toMutableList()
        existingPlays.removeAll { play ->
            val remoteId = (play.playId as? PlayId.Remote)?.remoteId
            remoteId != null && remoteId !in remoteIds
        }
        playsFlow.value = existingPlays
    }

    override fun observeTotalPlaysCount(): Flow<Long> {
        return totalPlaysCount
    }

    override fun observePlaysCountForMonth(
        start: Instant,
        end: Instant
    ): Flow<Long> {
        return playsCountForMonth.map { map -> map[Pair(start, end)] ?: 0L }
    }

    override fun observeRecentPlays(limit: Int): Flow<List<Play>> {
        return recentPlays.map { map -> map[limit] ?: emptyList() }
    }

    override fun observeUniqueGamesCount(): Flow<Long> {
        return uniqueGamesCount
    }

    override fun observeLocations(query: String): Flow<List<String>> {
        // Return distinct, case-preserving locations that start with the provided query (case-insensitive),
        // ordered alphabetically (case-insensitive), limited to 10 results.
        return playsFlow.map { plays ->
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
        return playsFlow.map { plays ->
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
        return playsFlow.map { plays ->
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

    /**
     * Sets the total plays count for testing.
     */
    fun setTotalPlaysCount(count: Long) {
        totalPlaysCount.value = count
    }

    /**
     * Sets the plays count for a specific month for testing.
     */
    fun setPlaysCountForMonth(start: Instant, end: Instant, count: Long) {
        val currentMap = playsCountForMonth.value.toMutableMap()
        currentMap[Pair(start, end)] = count
        playsCountForMonth.value = currentMap
    }

    /**
     * Sets recent plays for a specific limit for testing.
     */
    fun setRecentPlays(limit: Int, plays: List<Play>) {
        val currentMap = recentPlays.value.toMutableMap()
        currentMap[limit] = plays
        recentPlays.value = currentMap
    }

    /**
     * Sets the unique games count for testing.
     */
    fun setUniqueGamesCount(count: Long) {
        uniqueGamesCount.value = count
    }

    /**
     * Sets plays directly for testing.
     */
    fun setPlays(plays: List<Play>) {
        playsFlow.value = plays
    }

    fun RemotePlayDto.toPlay(localId: Long): Play {
        return Play(
            playId = PlayId.Remote(localId, remoteId),
            gameId = gameId,
            gameName = gameName,
            date = date,
            location = location,
            quantity = quantity,
            length = length,
            incomplete = incomplete,
            comments = comments,
            players = players.mapIndexed { index, playerDto -> playerDto.toPlayer(id = localId * 100 + index.toLong(), playId = localId) },
            syncStatus = PlaySyncStatus.SYNCED
        )
    }

    fun RemotePlayerDto.toPlayer(id: Long, playId: Long): Player {
        return Player(
            id = id,
            playId = playId,
            name = name,
            win = win,
            score = score,
            color = color,
            startPosition = startPosition,
            username = username,
            userId = userId
        )
    }
}
