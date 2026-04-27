package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.meeplebook.core.database.entity.ActivePlayTimerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the singleton global play timer row.
 */
@Dao
interface PlayTimerDao {

    /**
     * Observes the singleton persisted timer row.
     */
    @Query("SELECT * FROM active_play_timer WHERE singletonId = 0")
    fun observeTimer(): Flow<ActivePlayTimerEntity?>

    /**
     * Reads the current singleton persisted timer row.
     */
    @Query("SELECT * FROM active_play_timer WHERE singletonId = 0")
    suspend fun getTimer(): ActivePlayTimerEntity?

    /**
     * Persists the full singleton timer row.
     */
    @Upsert
    suspend fun upsertTimer(timer: ActivePlayTimerEntity)
}
