package app.meeplebook.core.sync

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository for managing sync timestamps.
 */
interface SyncTimeRepository {

    /**
     * Observes the last collection sync time.
     */
    fun observeLastCollectionSync(): Flow<Instant?>

    /**
     * Observes the last plays sync time.
     */
    fun observeLastPlaysSync(): Flow<Instant?>

    /**
     * Observes the last full sync time (both collection and plays).
     */
    fun observeLastFullSync(): Flow<Instant?>

    /**
     * Updates the last collection sync time.
     */
    suspend fun updateCollectionSyncTime(time: Instant)

    /**
     * Updates the last plays sync time.
     */
    suspend fun updatePlaysSyncTime(time: Instant)

    /**
     * Updates the last full sync time.
     */
    suspend fun updateFullSyncTime(time: Instant)

    /**
     * Clears all sync times.
     */
    suspend fun clearSyncTimes()
}