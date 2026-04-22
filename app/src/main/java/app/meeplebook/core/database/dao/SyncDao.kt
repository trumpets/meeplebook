package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.meeplebook.core.database.entity.SyncStateEntity
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Room DAO for persisted sync execution state.
 */
@Dao
interface SyncDao {
    /**
     * Observes the sync state row for a single [type].
     */
    @Query("SELECT * FROM sync_states WHERE type = :type")
    fun observeSyncState(type: SyncType): Flow<SyncStateEntity?>

    /**
     * Reads the current sync state row for a single [type].
     */
    @Query("SELECT * FROM sync_states WHERE type = :type")
    suspend fun getSyncState(type: SyncType): SyncStateEntity?

    /**
     * Upserts a full sync state row.
     */
    @Upsert
    suspend fun upsertSyncState(state: SyncStateEntity)

    /**
     * Marks the sync [type] as started while preserving its last successful timestamp.
     */
    @Query(
        """
        INSERT INTO sync_states(type, isSyncing, lastSyncedAt, errorMessage)
        VALUES (:type, 1, NULL, NULL)
        ON CONFLICT(type) DO UPDATE SET
            isSyncing = 1,
            errorMessage = NULL
        """
    )
    suspend fun markStarted(type: SyncType)

    /**
     * Marks the sync [type] as completed successfully and stores its completion time.
     */
    @Query(
        """
        INSERT INTO sync_states(type, isSyncing, lastSyncedAt, errorMessage)
        VALUES (:type, 0, :time, NULL)
        ON CONFLICT(type) DO UPDATE SET
            isSyncing = 0,
            lastSyncedAt = excluded.lastSyncedAt,
            errorMessage = NULL
        """
    )
    suspend fun markCompleted(type: SyncType, time: Instant)

    /**
     * Marks the sync [type] as idle due to syncing canceled by OS.
     */
    @Query(
        """
        INSERT INTO sync_states(type, isSyncing, lastSyncedAt, errorMessage)
        VALUES (:type, 0, NULL, NULL)
        ON CONFLICT(type) DO UPDATE SET
            isSyncing = 0,
            errorMessage = NULL
        """
    )
    suspend fun markIdle(type: SyncType)

    /**
     * Marks the sync [type] as failed while preserving its last successful timestamp.
     */
    @Query(
        """
        INSERT INTO sync_states(type, isSyncing, lastSyncedAt, errorMessage)
        VALUES (:type, 0, NULL, :errorMessage)
        ON CONFLICT(type) DO UPDATE SET
            isSyncing = 0,
            errorMessage = excluded.errorMessage
        """
    )
    suspend fun markFailed(type: SyncType, errorMessage: String?)

    /**
     * Removes all persisted sync state rows.
     */
    @Query("DELETE FROM sync_states")
    suspend fun clear()
}
