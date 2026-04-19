package app.meeplebook.core.sync

import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository for persisted sync execution state and the derived timestamps exposed to the UI.
 */
interface SyncTimeRepository {
    /**
     * Observes the sync execution state for a single [type].
     */
    fun observeSyncState(type: SyncType): Flow<SyncState>

    /**
     * Observes the last full sync time, derived from the collection and plays sync records.
     */
    fun observeLastFullSync(): Flow<Instant?>

    /**
     * Marks the sync [type] as started.
     */
    suspend fun markStarted(type: SyncType)

    /**
     * Marks the sync [type] as completed successfully.
     */
    suspend fun markCompleted(type: SyncType, time: Instant)

    /**
     * Marks the sync [type] as failed while preserving the last successful sync time.
     */
    suspend fun markFailed(type: SyncType, errorMessage: String?)

    /**
     * Clears all persisted sync state.
     */
    suspend fun clearSyncTimes()
}
