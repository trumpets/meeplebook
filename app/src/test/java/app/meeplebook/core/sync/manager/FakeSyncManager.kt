package app.meeplebook.core.sync.manager

import androidx.work.Operation
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSyncManager : SyncManager {
    var pendingPlaysSyncEnqueueCount: Int = 0
        private set
    var playsSyncEnqueueCount: Int = 0
        private set
    var collectionSyncEnqueueCount: Int = 0
        private set
    var fullSyncEnqueueCount: Int = 0
        private set
    var periodicFullSyncScheduleCount: Int = 0
        private set

    private val operation = mockk<Operation>(relaxed = true)
    private val fullSyncRunning = MutableStateFlow(false)

    override fun observeFullSyncRunning(): Flow<Boolean> = fullSyncRunning.asStateFlow()

    override fun enqueuePendingPlaysSync(): Operation {
        pendingPlaysSyncEnqueueCount += 1
        return operation
    }

    override fun enqueuePlaysSync(): Operation {
        playsSyncEnqueueCount += 1
        return operation
    }

    override fun enqueueCollectionSync(): Operation {
        collectionSyncEnqueueCount += 1
        return operation
    }

    override fun enqueueFullSync(): Operation {
        fullSyncEnqueueCount += 1
        return operation
    }

    override fun schedulePeriodicFullSync(): Operation {
        periodicFullSyncScheduleCount += 1
        return operation
    }

    fun setFullSyncRunning(isRunning: Boolean) {
        fullSyncRunning.value = isRunning
    }
}
