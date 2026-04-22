package app.meeplebook.core.plays.local

import androidx.room.withTransaction
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.database.entity.toEntity
import app.meeplebook.core.database.entity.toPlay
import app.meeplebook.core.database.projection.toPlayerIdentity
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.remote.dto.RemotePlayDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Implementation of [PlaysLocalDataSource] using Room database.
 */
class PlaysLocalDataSourceImpl @Inject constructor(
    private val database: MeepleBookDatabase,
    private val playDao: PlayDao,
    private val playerDao: PlayerDao
) : PlaysLocalDataSource {

    private companion object {
        const val SYNC_CHUNK_SIZE = 500
    }

    override fun observePlays(): Flow<List<Play>> {
        return playDao.observePlaysWithPlayers().map { playsWithPlayers ->
            playsWithPlayers.map { it.toPlay() }
        }
    }

    override fun observePlaysByGameNameOrLocation(gameNameOrLocationQuery: String): Flow<List<Play>> {
        return playDao.observePlaysWithPlayersByGameNameOrLocation(gameNameOrLocationQuery).map { playsWithPlayers ->
            playsWithPlayers.map { it.toPlay() }
        }
    }

    override fun observePlaysForGame(gameId: Long): Flow<List<Play>> {
        return playDao.observePlaysWithPlayersForGame(gameId).map { playsWithPlayers ->
            playsWithPlayers.map { it.toPlay() }
        }
    }

    override suspend fun getPlays(): List<Play> {
        return playDao.getPlaysWithPlayers().map { it.toPlay() }
    }

    override suspend fun getPlaysForGame(gameId: Long): List<Play> {
        return playDao.getPlaysWithPlayersForGame(gameId).map { it.toPlay() }
    }

    override suspend fun saveRemotePlays(remotePlays: List<RemotePlayDto>) {
        database.withTransaction {

            val remoteIds = remotePlays.map { it.remoteId }
            val existingPlays = playDao.getByRemoteIds(remoteIds)
            val existingByRemoteId = existingPlays.associateBy { it.remoteId!! }

            val playEntities = remotePlays.map { remotePlay ->
                existingByRemoteId[remotePlay.remoteId]?.let { existing ->
                    remotePlay.toEntity(localId = existing.localId, syncStatus = PlaySyncStatus.SYNCED)
                } ?: remotePlay.toEntity(localId = 0L, syncStatus = PlaySyncStatus.SYNCED)
            }

            playDao.upsertAll(plays = playEntities)

            // Retrieve actual localIds
            val updatedPlays = playDao.getByRemoteIds(remoteIds)
            val remoteIdToLocalId = updatedPlays.associate { it.remoteId!! to it.localId }

            val playerEntities = remotePlays.flatMap { remotePlay ->
                val localId = remoteIdToLocalId[remotePlay.remoteId]!!
                remotePlay.players.map { it.toEntity(playId = localId) }
            }

            // Replace players for affected plays
            val affectedLocalIds = updatedPlays.map { it.localId }
            playerDao.deletePlayersForPlays(affectedLocalIds)
            playerDao.insertAll(players = playerEntities)
        }
    }

    override suspend fun getPendingOrFailedPlays(): List<Play> {
        return playDao.getPendingOrFailedPlaysWithPlayers().map { it.toPlay() }
    }

    override suspend fun insertPlay(playEntity: PlayEntity, playerEntities: List<PlayerEntity>) {
        database.withTransaction {
            val localPlayId = playDao.insert(playEntity)
            val playersWithFk = playerEntities.map { it.copy(playId = localPlayId) }
            playerDao.insertAll(players = playersWithFk)
        }
    }

    override suspend fun markPlayAsSynced(localPlayId: Long, remotePlayId: Long) {
        playDao.updateSyncState(
            localPlayId = localPlayId,
            syncStatus = PlaySyncStatus.SYNCED,
            remoteId = remotePlayId
        )
    }

    override suspend fun markPlayAsFailed(localPlayId: Long) {
        playDao.updateSyncState(
            localPlayId = localPlayId,
            syncStatus = PlaySyncStatus.FAILED
        )
    }

    override suspend fun clearPlays() {
        database.withTransaction {
            playerDao.deleteAll()
            playDao.deleteAll()
        }
    }

    override suspend fun retainByRemoteIds(remoteIds: List<Long>) {
        val remoteIdSet = remoteIds.toHashSet()
        val toDelete = playDao.getRemotePlays()
            .mapNotNull { it.remoteId }
            .filter { it !in remoteIdSet }

        toDelete.chunked(SYNC_CHUNK_SIZE).forEach { chunk ->
            playDao.deleteByRemoteIds(remoteIds = chunk)
        }
    }

    override fun observeTotalPlaysCount(): Flow<Long> {
        return playDao.observeTotalPlaysCount()
    }

    override fun observePlaysCountForMonth(start: Instant, end: Instant): Flow<Long> {
        return playDao.observePlaysCountForMonth(start, end)
    }

    override fun observeRecentPlays(limit: Int): Flow<List<Play>> {
        return playDao.observeRecentPlaysWithPlayers(limit).map { entities ->
            entities.map { it.toPlay() }
        }
    }

    override fun observeUniqueGamesCount(): Flow<Long> {
        return playDao.observeUniqueGamesCount()
    }

    override fun observeLocations(query: String): Flow<List<String>> {
        return playDao.observeLocations(query)
    }

    override fun observeRecentLocations(): Flow<List<String>> {
        return playDao.observeRecentLocations()
    }

    override fun observePlayersByLocation(location: String): Flow<List<PlayerIdentity>> {
        return playDao.observePlayersByLocation(location).map { playerLocationProjects ->
            playerLocationProjects.map { it.toPlayerIdentity() }
        }
    }

    override fun observeColorsUsedForGame(gameId: Long): Flow<List<String>> {
        return playerDao.observeColorsUsedForGame(gameId)
    }

    override fun searchPlayersByName(query: String): Flow<List<PlayerIdentity>> {
        return playerDao.searchDistinctPlayersByName(query).map { projections ->
            projections.map { it.toPlayerIdentity() }
        }
    }

    override fun searchPlayersByUsername(query: String): Flow<List<PlayerIdentity>> {
        return playerDao.searchDistinctPlayersByUsername(query).map { projections ->
            projections.map { it.toPlayerIdentity() }
        }
    }
}
