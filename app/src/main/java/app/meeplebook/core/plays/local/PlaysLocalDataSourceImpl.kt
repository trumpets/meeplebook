package app.meeplebook.core.plays.local

import app.meeplebook.core.database.PlayDao
import app.meeplebook.core.database.PlayerDao
import app.meeplebook.core.database.toEntity
import app.meeplebook.core.database.toPlay
import app.meeplebook.core.database.toPlayer
import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [PlaysLocalDataSource] using Room database.
 */
class PlaysLocalDataSourceImpl @Inject constructor(
    private val playDao: PlayDao,
    private val playerDao: PlayerDao
) : PlaysLocalDataSource {

    override fun observePlays(): Flow<List<Play>> {
        return playDao.observePlays().map { playEntities ->
            playEntities.map { playEntity ->
                val players = playerDao.getPlayersForPlay(playEntity.id)
                    .map { it.toPlayer() }
                playEntity.toPlay(players)
            }
        }
    }

    override suspend fun getPlays(): List<Play> {
        return playDao.getPlays().map { playEntity ->
            val players = playerDao.getPlayersForPlay(playEntity.id)
                .map { it.toPlayer() }
            playEntity.toPlay(players)
        }
    }

    override suspend fun savePlays(plays: List<Play>) {
        plays.forEach { play ->
            playDao.insert(play.toEntity())
            playerDao.insertAll(play.players.map { it.toEntity() })
        }
    }

    override suspend fun clearPlays() {
        playerDao.deleteAll()
        playDao.deleteAll()
    }
}
