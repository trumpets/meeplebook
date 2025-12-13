package app.meeplebook.core.plays.local

import androidx.room.withTransaction
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.entity.toEntity
import app.meeplebook.core.database.entity.toPlay
import app.meeplebook.core.plays.model.Play
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

    override fun observePlaysForGame(gameId: Int): Flow<List<Play>> {
        return playDao.observePlaysWithPlayersForGame(gameId).map { playsWithPlayers ->
            playsWithPlayers.map { it.toPlay() }
        }
    }

    override suspend fun getPlays(): List<Play> {
        return playDao.getPlaysWithPlayers().map { it.toPlay() }
    }

    override suspend fun getPlaysForGame(gameId: Int): List<Play> {
        return playDao.getPlaysWithPlayersForGame(gameId).map { it.toPlay() }
    }

    override suspend fun savePlays(plays: List<Play>) {
        database.withTransaction {
            // Delete old players for plays being updated
            plays.forEach { play ->
                playerDao.deletePlayersForPlay(play.id)
            }
            
            // Bulk insert all plays
            playDao.insertAll(plays.map { it.toEntity() })
            
            // Bulk insert all players
            val allPlayers = plays.flatMap { play ->
                play.players.map { it.toEntity() }
            }
            if (allPlayers.isNotEmpty()) {
                playerDao.insertAll(allPlayers)
            }
        }
    }

    override suspend fun savePlay(play: Play) {
        savePlays(listOf(play))
    }

    override suspend fun clearPlays() {
        database.withTransaction {
            playerDao.deleteAll()
            playDao.deleteAll()
        }
    }

    override suspend fun getTotalPlaysCount(): Int {
        return playDao.getTotalPlaysCount() ?: 0
    }

    override suspend fun getPlaysCountForMonth(start: Instant, end: Instant): Int {
        return playDao.getPlaysCountForMonth(start, end) ?: 0
    }

    override suspend fun getRecentPlays(limit: Int): List<Play> {
        return playDao.getRecentPlaysWithPlayers(limit).map { it.toPlay() }
    }
}
