package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.manager.SyncManager
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Observes an app-level full-sync state derived from the collection and plays sync rows.
 *
 * The combined state reports syncing when either domain is still running, exposes a full-sync time
 * only after both domains have completed successfully, and preserves collection-first error
 * precedence so Overview status stays deterministic.
 */
class ObserveFullSyncStateUseCase @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository,
    private val syncManager: SyncManager
) {
    operator fun invoke(): Flow<SyncState> {
        return combine(
            syncManager.observeFullSyncRunning(),
            syncTimeRepository.observeSyncState(SyncType.COLLECTION),
            syncTimeRepository.observeSyncState(SyncType.PLAYS)
        ) { isSyncing, collectionState, playsState ->
            SyncState(
                isSyncing = isSyncing,
                lastSyncedAt = listOfNotNull(
                    collectionState.lastSyncedAt,
                    playsState.lastSyncedAt
                ).takeIf { it.size == 2 }?.minOrNull(),
                errorMessage = collectionState.errorMessage ?: playsState.errorMessage
            )
        }
    }
}
