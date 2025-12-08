package app.meeplebook.core.plays.local

import androidx.room.withTransaction
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.PlayDao
import app.meeplebook.core.database.PlayerDao
import app.meeplebook.core.database.toEntity
import app.meeplebook.core.database.toPlay
import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override suspend fun getPlays(): List<Play> {
        return playDao.getPlaysWithPlayers().map { it.toPlay() }
    }

    override suspend fun savePlays(plays: List<Play>) {
        database.withTransaction {
            plays.forEach { play ->
                playDao.insert(play.toEntity())
                playerDao.insertAll(play.players.map { it.toEntity() })
            }
        }
    }

    override suspend fun clearPlays() {
        database.withTransaction {
            playerDao.deleteAll()
            playDao.deleteAll()
        }
    }
}
