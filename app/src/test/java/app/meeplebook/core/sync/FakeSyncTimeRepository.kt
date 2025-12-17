package app.meeplebook.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Fake implementation of [SyncTimeRepository] for testing purposes.
 */
class FakeSyncTimeRepository : SyncTimeRepository {

    private val _lastCollectionSync = MutableStateFlow<Instant?>(null)
    private val _lastPlaysSync = MutableStateFlow<Instant?>(null)
    private val _lastFullSync = MutableStateFlow<Instant?>(null)

    override fun observeLastCollectionSync(): Flow<Instant?> = _lastCollectionSync

    override fun observeLastPlaysSync(): Flow<Instant?> = _lastPlaysSync

    override fun observeLastFullSync(): Flow<Instant?> = _lastFullSync

    override suspend fun updateCollectionSyncTime(time: Instant) {
        _lastCollectionSync.value = time
    }

    override suspend fun updatePlaysSyncTime(time: Instant) {
        _lastPlaysSync.value = time
    }

    override suspend fun updateFullSyncTime(time: Instant) {
        _lastFullSync.value = time
    }

    override suspend fun clearSyncTimes() {
        _lastCollectionSync.value = null
        _lastPlaysSync.value = null
        _lastFullSync.value = null
    }

    /**
     * Gets the last collection sync time for verification.
     */
    fun getLastCollectionSync(): Instant? = _lastCollectionSync.value

    /**
     * Gets the last plays sync time for verification.
     */
    fun getLastPlaysSync(): Instant? = _lastPlaysSync.value

    /**
     * Gets the last full sync time for verification.
     */
    fun getLastFullSync(): Instant? = _lastFullSync.value
}
