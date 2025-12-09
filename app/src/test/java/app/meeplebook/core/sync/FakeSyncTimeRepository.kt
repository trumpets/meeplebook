package app.meeplebook.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

/**
 * Fake implementation of [SyncTimeRepository] for testing purposes.
 */
class FakeSyncTimeRepository : SyncTimeRepository {
    
    private val _lastCollectionSync = MutableStateFlow<LocalDateTime?>(null)
    private val _lastPlaysSync = MutableStateFlow<LocalDateTime?>(null)
    private val _lastFullSync = MutableStateFlow<LocalDateTime?>(null)
    
    override fun observeLastCollectionSync(): Flow<LocalDateTime?> = _lastCollectionSync
    
    override fun observeLastPlaysSync(): Flow<LocalDateTime?> = _lastPlaysSync
    
    override fun observeLastFullSync(): Flow<LocalDateTime?> = _lastFullSync
    
    override suspend fun updateCollectionSyncTime(time: LocalDateTime) {
        _lastCollectionSync.value = time
    }
    
    override suspend fun updatePlaysSyncTime(time: LocalDateTime) {
        _lastPlaysSync.value = time
    }
    
    override suspend fun updateFullSyncTime(time: LocalDateTime) {
        _lastFullSync.value = time
    }
    
    override suspend fun clearSyncTimes() {
        _lastCollectionSync.value = null
        _lastPlaysSync.value = null
        _lastFullSync.value = null
    }
    
    /**
     * Sets sync times directly for testing.
     */
    fun setSyncTimes(
        collectionSync: LocalDateTime? = null,
        playsSync: LocalDateTime? = null,
        fullSync: LocalDateTime? = null
    ) {
        _lastCollectionSync.value = collectionSync
        _lastPlaysSync.value = playsSync
        _lastFullSync.value = fullSync
    }
}
