package app.meeplebook.core.sync

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository for managing sync timestamps.
 */
interface SyncTimeRepository {
    
    /**
     * Observes the last collection sync time.
     */
    fun observeLastCollectionSync(): Flow<LocalDateTime?>
    
    /**
     * Observes the last plays sync time.
     */
    fun observeLastPlaysSync(): Flow<LocalDateTime?>
    
    /**
     * Observes the last full sync time (both collection and plays).
     */
    fun observeLastFullSync(): Flow<LocalDateTime?>
    
    /**
     * Updates the last collection sync time.
     */
    suspend fun updateCollectionSyncTime(time: LocalDateTime)
    
    /**
     * Updates the last plays sync time.
     */
    suspend fun updatePlaysSyncTime(time: LocalDateTime)
    
    /**
     * Updates the last full sync time.
     */
    suspend fun updateFullSyncTime(time: LocalDateTime)
    
    /**
     * Clears all sync times.
     */
    suspend fun clearSyncTimes()
}
