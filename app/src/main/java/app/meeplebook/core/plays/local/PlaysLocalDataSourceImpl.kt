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

            // Prepare PlayEntities
            val playEntities = remotePlays.map { remotePlay ->
                existingByRemoteId[remotePlay.remoteId]?.let { existing ->
                    remotePlay.toEntity(localId = existing.localId, syncStatus = PlaySyncStatus.SYNCED)
                } ?: remotePlay.toEntity(localId = 0L, syncStatus = PlaySyncStatus.SYNCED) // new
            }

            // Upsert plays (Room generates IDs for new ones)
            playDao.upsertAll(plays = playEntities)

            // Retrieve actual localIds
            val updatedPlays = playDao.getByRemoteIds(remoteIds)
            val remoteIdToLocalId = updatedPlays.associate { it.remoteId!! to it.localId }

            // Prepare PlayerEntities
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

    override suspend fun insertPlay(playEntity: PlayEntity, playerEntities: List<PlayerEntity>) {
        database.withTransaction {
            val localPlayId = playDao.insert(playEntity)
            val playersWithFk = playerEntities.map { it.copy(playId = localPlayId) }
            playerDao.insertAll(players = playersWithFk)
        }
    }

    override suspend fun clearPlays() {
        database.withTransaction {
            playerDao.deleteAll()
            playDao.deleteAll()
        }
    }

    override suspend fun retainByRemoteIds(remoteIds: List<Long>) {
        val localRemoteIds = playDao.getRemotePlays()
            .mapNotNull { it.remoteId }

        val toDelete = localRemoteIds.filter { it !in remoteIds }

        if (toDelete.isNotEmpty()) {
            playDao.deleteByRemoteIds(remoteIds = toDelete)
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
}
