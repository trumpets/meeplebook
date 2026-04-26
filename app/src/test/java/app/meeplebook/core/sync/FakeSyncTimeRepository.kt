package app.meeplebook.core.sync

import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Fake implementation of [SyncTimeRepository] for unit tests.
 */
class FakeSyncTimeRepository : SyncTimeRepository {

    data class Operation(
        val kind: Kind,
        val type: SyncType,
        val time: Instant? = null,
        val errorMessage: String? = null
    ) {
        enum class Kind {
            STARTED,
            COMPLETED,
            FAILED,
            CLEARED,
            IDLE
        }
    }

    private val states = SyncType.entries.associateWith { MutableStateFlow(SyncState()) }
    private val mutableOperations = mutableListOf<Operation>()

    val operations: List<Operation> get() = mutableOperations.toList()

    override fun observeSyncState(type: SyncType): Flow<SyncState> =
        states.getValue(type)

    override suspend fun markStarted(type: SyncType) {
        states.getValue(type).value = states.getValue(type).value.copy(
            isSyncing = true,
            errorMessage = null
        )
        mutableOperations += Operation(Operation.Kind.STARTED, type)
    }

    override suspend fun markIdle(type: SyncType) {
        states.getValue(type).value = states.getValue(type).value.copy(
            isSyncing = false,
            errorMessage = null
        )
        mutableOperations += Operation(Operation.Kind.IDLE, type)
    }

    override suspend fun markCompleted(type: SyncType, time: Instant) {
        states.getValue(type).value = states.getValue(type).value.copy(
            isSyncing = false,
            lastSyncedAt = time,
            errorMessage = null
        )
        mutableOperations += Operation(Operation.Kind.COMPLETED, type, time = time)
    }

    override suspend fun markFailed(type: SyncType, errorMessage: String?) {
        states.getValue(type).value = states.getValue(type).value.copy(
            isSyncing = false,
            errorMessage = errorMessage
        )
        mutableOperations += Operation(
            kind = Operation.Kind.FAILED,
            type = type,
            errorMessage = errorMessage
        )
    }

    override suspend fun clearSyncTimes() {
        SyncType.entries.forEach { type ->
            states.getValue(type).value = SyncState()
            mutableOperations += Operation(Operation.Kind.CLEARED, type)
        }
    }

    override suspend fun getSyncState(type: SyncType): SyncState = states.getValue(type).value

    fun getLastSync(type: SyncType): Instant? = states.getValue(type).value.lastSyncedAt

    suspend fun updateCollectionSyncTime(time: Instant) {
        markCompleted(SyncType.COLLECTION, time)
    }

    suspend fun updatePlaysSyncTime(time: Instant) {
        markCompleted(SyncType.PLAYS, time)
    }

    suspend fun updateFullSyncTime(time: Instant) {
        markCompleted(SyncType.COLLECTION, time)
        markCompleted(SyncType.PLAYS, time)
    }

    fun getLastCollectionSync(): Instant? = getLastSync(SyncType.COLLECTION)

    fun getLastPlaysSync(): Instant? = getLastSync(SyncType.PLAYS)

    fun getLastFullSync(): Instant? {
        val collectionTime = getLastCollectionSync()
        val playsTime = getLastPlaysSync()
        return if (collectionTime == null || playsTime == null) {
            null
        } else {
            minOf(collectionTime, playsTime)
        }
    }
}
