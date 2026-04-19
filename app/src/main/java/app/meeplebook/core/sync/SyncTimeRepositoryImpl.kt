package app.meeplebook.core.sync

import app.meeplebook.core.database.dao.SyncDao
import app.meeplebook.core.database.entity.SyncStateEntity
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Room-backed implementation of [SyncTimeRepository].
 */
class SyncTimeRepositoryImpl @Inject constructor(
    private val syncDao: SyncDao
) : SyncTimeRepository {
    override fun observeSyncState(type: SyncType): Flow<SyncState> =
        syncDao.observeSyncState(type).map { entity ->
            entity?.toModel() ?: SyncState()
        }

    override fun observeLastFullSync(): Flow<Instant?> =
        combine(
            observeSyncState(SyncType.COLLECTION),
            observeSyncState(SyncType.PLAYS)
        ) { collectionState, playsState ->
            val collectionTime = collectionState.lastSyncedAt
            val playsTime = playsState.lastSyncedAt
            if (collectionTime == null || playsTime == null) {
                null
            } else {
                minOf(collectionTime, playsTime)
            }
        }

    override suspend fun markStarted(type: SyncType) {
        syncDao.markStarted(type)
    }

    override suspend fun markCompleted(type: SyncType, time: Instant) {
        syncDao.markCompleted(type, time)
    }

    override suspend fun markFailed(type: SyncType, errorMessage: String?) {
        syncDao.markFailed(type, errorMessage)
    }

    override suspend fun clearSyncTimes() {
        syncDao.clear()
    }
}

private fun SyncStateEntity.toModel(): SyncState =
    SyncState(
        isSyncing = isSyncing,
        lastSyncedAt = lastSyncedAt,
        errorMessage = errorMessage
    )
